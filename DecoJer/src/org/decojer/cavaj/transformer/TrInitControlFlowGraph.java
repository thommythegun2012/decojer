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
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
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
public class TrInitControlFlowGraph {

	private final static Logger LOGGER = Logger.getLogger(TrInitControlFlowGraph.class.getName());

	public static void transform(final CFG cfg) {
		new TrInitControlFlowGraph(cfg).transform();
	}

	public static void transform(final TD td) {
		final List<BD> bds = td.getBds();
		for (int i = 0; i < bds.size(); ++i) {
			final BD bd = bds.get(i);
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			if (cfg == null || cfg.isIgnore()) {
				continue;
			}
			try {
				transform(cfg);
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Cannot transform '" + cfg.getMd() + "'!", e);
				cfg.setError(true);
			}
		}
	}

	private final CFG cfg;

	/**
	 * Remember visited pcs and related basic block.
	 */
	private BB[] pc2Bbs;

	private TrInitControlFlowGraph(final CFG cfg) {
		this.cfg = cfg;
	}

	/**
	 * Get target basic block. Split if necessary, but keep outgoing part same (for later adding of
	 * outging edges).
	 * 
	 * @param pc
	 *            target pc
	 * @return target basic block
	 */
	private BB getTargetBb(final int pc) {
		// operation with pc must be in this basic block
		final BB targetBb = this.pc2Bbs[pc];
		// no basic block found for pc yet
		if (targetBb == null) {
			return null;
		}

		final int bbPc = targetBb.getOpPc();
		final List<Op> ops = targetBb.getOps();

		// first operation in basic block has target pc, return basic block,
		// no split necessary
		if (pc == bbPc) {
			return targetBb;
		}
		// split basic block, new incoming block, adapt basic block pcs
		final BB splitSourceBb = this.cfg.newBb(bbPc);
		targetBb.setOpPc(pc);
		if (this.cfg.getStartBb() == targetBb) {
			this.cfg.setStartBb(splitSourceBb);
		}
		// first preserve predecessors...
		targetBb.movePredBbs(splitSourceBb);
		// ...then add connection
		splitSourceBb.addSucc(targetBb, null);

		// move operations, change pc map
		for (int i = pc; i-- > bbPc;) {
			splitSourceBb.addOp(ops.remove(0));
			this.pc2Bbs[i] = splitSourceBb;
		}
		return targetBb;
	}

	private void transform() {
		// this.cfg.clear(); don't kill body here...
		// set start BB, may change through splitting
		this.cfg.setStartBb(this.cfg.newBb(0));

		final Op[] ops = this.cfg.getOps();
		this.pc2Bbs = new BB[ops.length];

		// start with this basic block, may not remain the start basic block
		// (splitting)
		BB bb = this.cfg.getStartBb();
		// remember open pcs
		final Stack<Integer> openPcs = new Stack<Integer>();

		int pc = 0;
		while (true) {
			// next open pc?
			if (pc >= ops.length) {
				if (openPcs.isEmpty()) {
					break;
				}
				pc = openPcs.pop();
				bb = this.pc2Bbs[pc];
			} else {
				// next pc allready in flow?
				final BB nextBB = getTargetBb(pc);
				if (nextBB != null) {
					bb.addSucc(nextBB, null);
					pc = ops.length; // next open pc
					continue;
				}
				this.pc2Bbs[pc] = bb;
			}

			final Op op = ops[pc++];
			bb.addOp(op);
			switch (op.getOptype()) {
			case GOTO: {
				final GOTO cop = (GOTO) op;
				pc = cop.getTargetPc();
				// create new BB because we need the correct index after the
				// goto, if we simply follow the goto without a new block then
				// we have a problem to find the bb split point (operations.pc
				// might be original pc and not the operation index)
				BB nextBB = getTargetBb(pc);
				if (nextBB == null) {
					nextBB = this.cfg.newBb(pc);
				} else {
					pc = ops.length; // next open pc
				}
				bb.addSucc(nextBB, null);
				bb = nextBB;
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				final int targetPc = cop.getTargetPc();
				if (targetPc == pc) {
					System.out.println("### BRANCH_IFCMP (Empty): " + targetPc);
				} else {
					BB targetBB = getTargetBb(targetPc);
					if (targetBB == null) {
						targetBB = this.cfg.newBb(targetPc);
						this.pc2Bbs[targetPc] = targetBB;
						openPcs.add(targetPc);
					}
					bb.addSucc(targetBB, Boolean.TRUE);
					BB nextBB = getTargetBb(pc);
					if (nextBB == null) {
						nextBB = this.cfg.newBb(pc);
					} else {
						pc = ops.length; // next open pc
					}
					bb.addSucc(nextBB, Boolean.FALSE);
					bb = nextBB;
				}
				break;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				final int targetPc = cop.getTargetPc();
				if (targetPc == pc) {
					System.out.println("### BRANCH_IF (Empty): " + targetPc);
				} else {
					BB targetBB = getTargetBb(targetPc);
					if (targetBB == null) {
						targetBB = this.cfg.newBb(targetPc);
						this.pc2Bbs[targetPc] = targetBB;
						openPcs.add(targetPc);
					}
					bb.addSucc(targetBB, Boolean.TRUE);
					BB nextBB = getTargetBb(pc);
					if (nextBB == null) {
						nextBB = this.cfg.newBb(pc);
					} else {
						pc = ops.length; // next open pc
					}
					bb.addSucc(nextBB, Boolean.FALSE);
					bb = nextBB;
				}
				break;
			}
			case SWITCH: {
				final SWITCH cop = (SWITCH) op;

				// build map: unique case pc -> case keys
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
				// add default branch, can overlay with other cases, even JDK 6
				// doesn't optimize this
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

					BB caseBb = getTargetBb(casePc);
					if (caseBb == null) {
						caseBb = this.cfg.newBb(casePc);
						this.pc2Bbs[casePc] = caseBb;
						openPcs.add(casePc);
					}
					bb.addSucc(caseBb, keys);
				}
				pc = ops.length; // next open pc
				break;
			}
			case RET:
			case RETURN:
			case THROW:
				pc = ops.length; // next open pc
				break;
			}
		}
		this.cfg.calculatePostorder();
	}

}