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

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
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

	private boolean isIgnoreExceptions;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2Bbs;

	/**
	 * Remember open PCs.
	 */
	final LinkedList<Integer> openPcs = new LinkedList<Integer>();

	private TrInitControlFlowGraph(final CFG cfg) {
		this.cfg = cfg;
	}

	/**
	 * Get target BB for PC. Split or create new if necessary.
	 * 
	 * @param pc
	 *            target PC
	 * @return target BB
	 */
	private BB getTarget(final int pc) {
		final BB bb = this.pc2Bbs[pc]; // get BB for target PC
		if (bb == null) {
			// PC not processed yet
			this.openPcs.add(pc);
			return newBb(pc);
		}
		// found BB has target PC as first PC => return BB, no split necessary
		if (bb.getOpPc() == pc) {
			return bb;
		}

		// split basic block, new incoming block, adapt basic block pcs
		final BB split = newBb(pc);
		// first preserve previous successors...
		bb.moveOuts(split);
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

	private BB newBb(final int pc) {
		final BB bb = this.cfg.newBb(pc);
		this.pc2Bbs[pc] = bb;
		if (!this.isIgnoreExceptions) {
			final Exc[] excs = this.cfg.getExcs();
			if (excs == null) {
				return bb;
			}
			// build sorted map: unique handler pc -> matching handler types
			final TreeMap<Integer, List<T>> handlerPc2type = new TreeMap<Integer, List<T>>();
			for (final Exc exc : this.cfg.getExcs()) {
				if (!exc.validIn(pc)) {
					continue;
				}
				final int handlerPc = exc.getHandlerPc();
				List<T> types = handlerPc2type.get(handlerPc);
				if (types == null) {
					types = new ArrayList<T>();
					handlerPc2type.put(handlerPc, types);
				}
				types.add(exc.getT());
			}
			// now add successors
			for (final Map.Entry<Integer, List<T>> handlerPc2typeEntry : handlerPc2type.entrySet()) {
				final List<T> types = handlerPc2typeEntry.getValue();
				bb.addCatchSucc(types.toArray(new T[types.size()]),
						getTarget(handlerPc2typeEntry.getKey()));
			}
		}
		return bb;
	}

	private void transform() {
		this.isIgnoreExceptions = this.cfg.getMd().getTd().getCu().check(DFlag.IGNORE_EXCEPTIONS);

		final Op[] ops = this.cfg.getOps();
		this.pc2Bbs = new BB[ops.length];

		// start with PC 0 and new BB
		this.openPcs.add(0);
		int pc = ops.length;
		BB bb = newBb(0);
		this.cfg.setStartBb(bb);

		while (true) {
			if (pc >= ops.length) {
				// next open pc?
				if (this.openPcs.isEmpty()) {
					break;
				}
				pc = this.openPcs.removeFirst();
				bb = this.pc2Bbs[pc];
			} else if (this.pc2Bbs[pc] != null) {
				bb.setSucc(getTarget(pc));
				pc = ops.length; // next open pc
				continue;
			} else {
				this.pc2Bbs[pc] = bb;
			}
			if (!this.isIgnoreExceptions) {
				// exception block changes in none-empty BB? -> split necessary!
				if (!bb.getOps().isEmpty() && this.cfg.getExcs() != null) {
					for (final Exc exc : this.cfg.getExcs()) {
						if (exc.validIn(pc)) {
							if (exc.validIn(bb.getOpPc())) {
								// exception is valid - has been valid at BB entry -> OK
								continue;
							}
						} else {
							if (!exc.validIn(bb.getOpPc())) {
								// exception isn't valid - hasn't bean valid at BB entry -> OK
								continue;
							}
						}
						// at least one exception has changed, now split
						final BB succ = newBb(pc);
						bb.setSucc(succ);
						bb = succ;
						break;
					}
				}
			}
			final Op op = ops[pc++];
			bb.addOp(op);
			switch (op.getOptype()) {
			case GOTO: {
				final GOTO cop = (GOTO) op;
				// follow without new BB, lazy splitting, at target PC other catches possible!
				pc = cop.getTargetPc();
				continue;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				bb.setCondSuccs(getTarget(pc), getTarget(cop.getTargetPc()));
				pc = ops.length; // next open pc
				continue;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				bb.setCondSuccs(getTarget(pc), getTarget(cop.getTargetPc()));
				pc = ops.length; // next open pc
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
				for (final Map.Entry<Integer, List<Integer>> casePc2keysEntry : casePc2keys
						.entrySet()) {
					keys = casePc2keysEntry.getValue();
					bb.addSwitchSucc(keys.toArray(new Integer[keys.size()]),
							getTarget(casePc2keysEntry.getKey()));
				}
				pc = ops.length; // next open pc
				continue;
			}
			case RET: // TODO back-edge?
			case RETURN:
			case THROW:
				pc = ops.length; // next open pc
			}
		}
		this.cfg.calculatePostorder();
	}

}