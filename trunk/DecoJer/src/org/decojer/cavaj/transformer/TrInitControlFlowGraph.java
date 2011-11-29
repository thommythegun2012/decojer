/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  André Pankraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every Java Source Code
 * that is created using DecoJer.
 */
package org.decojer.cavaj.transformer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.SWITCH;

/**
 * Transform Init Control Flow Graph.
 * 
 * @author André Pankraz
 */
public final class TrInitControlFlowGraph {

	private final static Logger LOGGER = Logger.getLogger(TrInitControlFlowGraph.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrInitControlFlowGraph(cfg).transform();
	}

	private final CFG cfg;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2Bbs;

	private TrInitControlFlowGraph(final CFG cfg) {
		this.cfg = cfg;
	}

	/**
	 * Get target BB for PC. Split if necessary.
	 * 
	 * @param pc
	 *            target PC
	 * @return target BB
	 */
	private BB getTarget(final int pc) {
		final BB bb = this.pc2Bbs[pc]; // get BB for target PC
		if (bb == null) {
			// PC not processed yet
			return null;
		}
		// found BB has target PC as first PC => return BB, no split necessary
		if (bb.getOpPc() == pc) {
			return bb;
		}

		// split basic block, new incoming block, adapt basic block pcs
		final BB split = this.cfg.newBb(pc);
		// first preserve previous successors...
		bb.moveSuccs(split);
		// ...then set new successor
		bb.setSucc(split);

		// move operations, update PC map, first find split point...
		final List<Op> ops = bb.getOps();
		int i;
		for (i = ops.size(); i-- > 0;) {
			if (ops.get(i).getPc() == pc) {
				break;
			}
		}
		// ...now move all tail operations
		while (i < ops.size()) {
			final Op op = ops.remove(i);
			split.addOp(op);
			this.pc2Bbs[op.getPc()] = split;
		}

		return split;
	}

	private void transform() {
		final Op[] ops = this.cfg.getOps();
		this.pc2Bbs = new BB[ops.length];
		final LinkedList<Integer> openPcs = new LinkedList<Integer>(); // remember open PCs

		// start with PC 0 and new BB
		int pc = 0;
		BB bb = this.cfg.newBb(0);
		this.cfg.setStartBb(bb);

		while (true) {
			if (pc >= ops.length) {
				// next open pc?
				if (openPcs.isEmpty()) {
					break;
				}
				pc = openPcs.removeFirst();
				bb = this.pc2Bbs[pc];
			} else {
				// next pc allready in flow?
				final BB target = getTarget(pc);
				if (target != null) {
					bb.setSucc(target);
					pc = ops.length; // next open pc
					continue;
				}
				this.pc2Bbs[pc] = bb;
			}

			final Exc[] excs = this.cfg.getExcs();
			if (excs != null) {
				// PC enters or leaves exception-catch?
				final List<BB> succs = bb.getSuccs();
				final List<Object> succValues = bb.getSuccValues();

				for (final Exc exc : this.cfg.getExcs()) {
					if (exc.validIn(pc)) {
						exc.getT();
					}
				}
			}

			final Op op = ops[pc++];
			bb.addOp(op);
			switch (op.getOptype()) {
			case GOTO: {
				final GOTO cop = (GOTO) op;
				// follow without new BB, lazy splitting
				pc = cop.getTargetPc();
				continue;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				final int targetPc = cop.getTargetPc();
				if (targetPc == pc) {
					LOGGER.warning("Empty JCMP Branch: " + targetPc);
				} else {
					BB trueSucc = getTarget(targetPc);
					if (trueSucc == null) {
						trueSucc = this.cfg.newBb(targetPc);
						this.pc2Bbs[targetPc] = trueSucc;
						openPcs.add(targetPc);
					}
					BB falseSucc = getTarget(pc);
					if (falseSucc == null) {
						falseSucc = this.cfg.newBb(pc);
					} else {
						pc = ops.length; // next open pc
					}
					bb.setCondSuccs(trueSucc, falseSucc);
					bb = falseSucc;
				}
				continue;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				final int targetPc = cop.getTargetPc();
				if (targetPc == pc) {
					LOGGER.warning("Empty JCND Branch: " + targetPc);
				} else {
					BB trueSucc = getTarget(targetPc);
					if (trueSucc == null) {
						trueSucc = this.cfg.newBb(targetPc);
						this.pc2Bbs[targetPc] = trueSucc;
						openPcs.add(targetPc);
					}
					BB falseSucc = getTarget(pc);
					if (falseSucc == null) {
						falseSucc = this.cfg.newBb(pc);
					} else {
						pc = ops.length; // next open pc
					}
					bb.setCondSuccs(trueSucc, falseSucc);
					bb = falseSucc;
				}
				continue;
			}
			case SWITCH: {
				final SWITCH cop = (SWITCH) op;

				// build sorted map: unique case pc -> matching case keys
				final TreeMap<Integer, List<Integer>> casePc2keys = new TreeMap<Integer, List<Integer>>();
				List<Integer> keys;
				final int[] caseKeys = cop.getCaseKeys();
				final int[] casePcs = cop.getCasePcs();
				for (int i = 0; i < caseKeys.length; ++i) {
					final int casePc = casePcs[i];
					keys = casePc2keys.get(casePc);
					if (keys == null) {
						keys = new ArrayList<Integer>();
						casePc2keys.put(casePc, keys);
					}
					keys.add(caseKeys[i]);
				}
				// add default branch, can overlay with other cases, even JDK 6 doesn't optimize
				final int defaultPc = cop.getDefaultPc();
				keys = casePc2keys.get(defaultPc);
				if (keys == null) {
					keys = new ArrayList<Integer>();
					casePc2keys.put(defaultPc, keys);
				}
				keys.add(null);

				// now add successors
				for (final Map.Entry<Integer, List<Integer>> casePc2ValuesEntry : casePc2keys
						.entrySet()) {
					final int casePc = casePc2ValuesEntry.getKey();
					keys = casePc2ValuesEntry.getValue();

					BB caseBb = getTarget(casePc);
					if (caseBb == null) {
						caseBb = this.cfg.newBb(casePc);
						this.pc2Bbs[casePc] = caseBb;
						openPcs.add(casePc);
					}
					bb.addSwitchSucc(keys.toArray(new Integer[keys.size()]), caseBb);
				}
				pc = ops.length; // next open pc
				continue;
			}
			case RET:
			case RETURN:
			case THROW:
				pc = ops.length; // next open pc
			}
		}
		this.cfg.calculatePostorder();
	}

}