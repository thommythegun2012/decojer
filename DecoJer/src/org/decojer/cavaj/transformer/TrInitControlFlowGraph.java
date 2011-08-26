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

import java.util.List;
import java.util.Stack;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;
import org.decojer.cavaj.model.vm.intermediate.operations.JCMP;
import org.decojer.cavaj.model.vm.intermediate.operations.JCND;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;

/**
 * Transform Init Control Flow Graph.
 * 
 * @author André Pankraz
 */
public class TrInitControlFlowGraph {

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
			if (cfg == null) {
				continue;
			}
			if (cfg.getOperations() == null) {
				continue; // TODO delete?
			}
			transform(cfg);
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
	 * Get target basic block. Split if necessary, but keep outgoing part same
	 * (for later adding of outging edges).
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
		final List<Operation> operations = targetBb.getOperations();

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
			final Operation vmOperation = operations.remove(0);
			splitSourceBb.addOperation(vmOperation);
			this.pc2Bbs[i] = splitSourceBb;
		}
		return targetBb;
	}

	private void transform() {
		final Operation[] operations = this.cfg.getOperations();
		this.pc2Bbs = new BB[operations.length];

		// start with this basic block, may not remain the start basic block
		// (splitting)
		BB bb = this.cfg.getStartBb();
		// remember open pcs
		final Stack<Integer> openPcs = new Stack<Integer>();

		int pc = 0;
		while (true) {
			// next open pc?
			if (pc >= operations.length) {
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
					pc = operations.length; // next open pc
					continue;
				}
				this.pc2Bbs[pc] = bb;
			}

			final Operation operation = operations[pc++];
			bb.addOperation(operation);
			switch (operation.getOpcode()) {
			case Opcode.GOTO: {
				final GOTO op = (GOTO) operation;
				pc = op.getTargetPc();
				// create new BB because we need the correct index after the
				// goto, if we simply follow the goto without a new block then
				// we have a problem to find the bb split point (operations.pc
				// might be original pc and not the operation index)
				BB nextBB = getTargetBb(pc);
				if (nextBB == null) {
					nextBB = this.cfg.newBb(pc);
				} else {
					pc = operations.length; // next open pc
				}
				bb.addSucc(nextBB, null);
				bb = nextBB;
				break;
			}
			case Opcode.JCMP: {
				final JCMP op = (JCMP) operation;
				final int targetPc = op.getTargetPc();
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
						pc = operations.length; // next open pc
					}
					bb.addSucc(nextBB, Boolean.FALSE);
					bb = nextBB;
				}
				break;
			}
			case Opcode.JCND: {
				final JCND op = (JCND) operation;
				final int targetPc = op.getTargetPc();
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
						pc = operations.length; // next open pc
					}
					bb.addSucc(nextBB, Boolean.FALSE);
					bb = nextBB;
				}
				break;
			}
			case Opcode.SWITCH: {
				final SWITCH op = (SWITCH) operation;
				/*
				 * // case pc -> values final HashMap<Integer, List<Integer>>
				 * casePc2Values = (HashMap<Integer, List<Integer>>) oValue; for
				 * (final Map.Entry<Integer, List<Integer>> casePc2ValuesEntry :
				 * casePc2Values .entrySet()) { final int caseBranch =
				 * casePc2ValuesEntry.getKey(); final List<Integer> values =
				 * casePc2ValuesEntry.getValue(); final int casePc = opPc +
				 * caseBranch; BB caseBb = this.cfg.getTargetBb(casePc, pcBbs);
				 * if (caseBb == null) { caseBb = this.cfg.newBb(casePc);
				 * pcBbs.put(casePc, caseBb); openPcs.add(casePc); }
				 * bb.addSucc(caseBb, values); }
				 */
				// next open pc
				pc = operations.length; // next open pc

				break;
			}
			case Opcode.RET:
			case Opcode.RETURN:
			case Opcode.THROW:
				pc = operations.length; // next open pc
				break;
			}
		}
		this.cfg.calculatePostorder();
	}

}