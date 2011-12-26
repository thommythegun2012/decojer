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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.Sub;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.JSR;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.RET;
import org.decojer.cavaj.model.code.op.STORE;
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

	private boolean isIgnoreExceptions;

	/**
	 * Remember open PCs.
	 */
	private final LinkedList<Integer> openPcs = new LinkedList<Integer>();

	/**
	 * Remember open RETs.
	 */
	private LinkedList<RET> openRets;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2bbs;

	private Map<Integer, Sub> pc2sub;

	private TrInitControlFlowGraph(final CFG cfg) {
		this.cfg = cfg;
	}

	private boolean checkOpenRets() {
		if (this.openRets == null) {
			return false;
		}
		for (final RET ret : this.openRets) {
			final BB subLast = this.pc2bbs[ret.getPc()];
			// RET register should contain an address that follows the calling JSR instruction,
			// JSR pushes this address onto the stack and calls the subroutine BB,
			// normally the first subroutine instruction is a STORE
			final Sub sub = findSub(ret.getReg(), subLast, new HashSet<BB>());
			for (final JSR jsr : sub.getJsrs()) {
				subLast.setSucc(getTarget(jsr.getPc() + 1));
			}
		}
		this.openRets.clear();
		return !this.openPcs.isEmpty();
	}

	private Sub findSub(final int reg, final BB bb, final Set<BB> traversed) {
		for (final Op op : bb.getOps()) {
			if (!(op instanceof STORE) || ((STORE) op).getReg() != reg) {
				continue;
			}
			final Sub sub = this.pc2sub.get(bb.getOpPc());
			if (sub != null) {
				if (op.getPc() != bb.getOpPc()) {
					LOGGER.warning("Subroutine for reg '" + reg
							+ "' found, but STORE isn't first operation: " + sub);
				}
				return sub;
			}
			LOGGER.warning("Couldn't find subroutine for STORE: " + op);
			return null;
		}
		traversed.add(bb);
		for (final E in : bb.getIns()) {
			if (traversed.contains(in.getStart())) {
				continue;
			}
			final Sub sub = findSub(reg, in.getStart(), traversed);
			if (sub != null) {
				return sub;
			}
		}
		return null;
	}

	/**
	 * Get target BB for PC. Split or create new if necessary.
	 * 
	 * @param pc
	 *            target PC
	 * @return target BB
	 */
	private BB getTarget(final int pc) {
		final BB bb = this.pc2bbs[pc]; // get BB for target PC
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
			this.pc2bbs[op.getPc()] = split;
		}

		return split;
	}

	private BB newBb(final int pc) {
		final BB bb = this.cfg.newBb(pc);
		this.pc2bbs[pc] = bb;
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
		this.pc2bbs = new BB[ops.length];

		// start with PC 0 and new BB
		this.openPcs.add(0);
		int pc = ops.length;
		BB bb = newBb(0);
		this.cfg.setStartBb(bb);

		while (true) {
			if (pc >= ops.length) {
				// next open pc?
				if (this.openPcs.isEmpty() && !checkOpenRets()) {
					break;
				}
				pc = this.openPcs.removeFirst();
				bb = this.pc2bbs[pc];
			} else if (this.pc2bbs[pc] != null) {
				bb.setSucc(getTarget(pc));
				pc = ops.length; // next open pc
				continue;
			} else {
				this.pc2bbs[pc] = bb;
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
			case JSR: {
				// Spec, JSR/RET is stack-like:
				// http://docs.oracle.com/javase/7/specs/jvms/JVMS-JavaSE7.pdf

				// • No return address (a value of type returnAddress) may be loaded from a
				// local variable.
				// • The instruction following each jsr or jsr_w instruction may be returned to only
				// by a single ret instruction.
				// • No jsr or jsr_w instruction that is returned to may be used to recursively call
				// a
				// subroutine if that subroutine is already present in the subroutine call chain.
				// (Subroutines can be nested when using try-finally constructs from within a
				// finally clause.)
				// • Each instance of type returnAddress can be returned to at most once. If a ret
				// instruction returns to a point in the subroutine call chain above the ret
				// instruction
				// corresponding to a given instance of type returnAddress, then that
				// instance can never be used as a return address.

				// Verifying code that contains a finally clause is complicated. The basic idea
				// is the following:
				// • Each instruction keeps track of the list of jsr targets needed to reach that
				// instruction. For most code, this list is empty. For instructions inside code for
				// the finally clause, it is of length one. For multiply nested finally code
				// (extremely rare!), it may be longer than one.
				// • For each instruction and each jsr needed to reach that instruction, a bit
				// vector
				// is maintained of all local variables accessed or modified since the execution of
				// the jsr instruction.
				// • When executing the ret instruction, which implements a return from a
				// subroutine,
				// there must be only one possible subroutine from which the instruction can
				// be returning. Two different subroutines cannot “merge” their execution to a
				// single ret instruction.
				// • To perform the data-flow analysis on a ret instruction, a special procedure is
				// used. Since the verifier knows the subroutine from which the instruction must
				// be returning, it can find all the jsr instructions that call the subroutine and
				// merge the state of the operand stack and local variable array at the time
				// of the ret instruction into the operand stack and local variable array of the
				// instructions following the jsr. Merging uses a special set of values for local
				// variables:
				// - For any local variable that the bit vector (constructed above) indicates has
				// been accessed or modified by the subroutine, use the type of the local variable
				// at the time of the ret.
				// - For other local variables, use the type of the local variable before the jsr
				// instruction.
				final JSR cop = (JSR) op;
				if (this.pc2sub == null) {
					this.pc2sub = new HashMap<Integer, Sub>();
					this.pc2sub.put(cop.getTargetPc(), new Sub(cop));
				} else {
					Sub sub = this.pc2sub.get(cop.getTargetPc());
					if (sub == null) {
						this.pc2sub.put(cop.getTargetPc(), sub = new Sub(cop));
					} else {
						sub.addJsr(cop);
					}
				}
				bb.setSucc(getTarget(cop.getTargetPc()));
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
			case RET: {
				final RET cop = (RET) op;
				// remember because we don't know if we have all JSR pathes right now
				if (this.openRets == null) {
					this.openRets = new LinkedList<RET>();
				}
				this.openRets.add(cop);
				pc = ops.length; // next open pc
				continue;
			}
			case RETURN:
			case THROW:
				pc = ops.length; // next open pc
			}
		}
		this.cfg.calculatePostorder();
	}

}