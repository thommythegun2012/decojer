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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.R.Kind;
import org.decojer.cavaj.model.code.Sub;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.op.GET;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.INVOKE;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.JSR;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.PUSH;
import org.decojer.cavaj.model.code.op.RET;
import org.decojer.cavaj.model.code.op.RETURN;
import org.decojer.cavaj.model.code.op.STORE;
import org.decojer.cavaj.model.code.op.SWITCH;
import org.decojer.cavaj.model.code.op.THROW;

/**
 * Transform Data Flow Analysis and building Control Flow Graph.
 * 
 * @author André Pankraz
 */
public final class TrDataFlowAnalysis2Cfg {

	private final static Logger LOGGER = Logger.getLogger(TrDataFlowAnalysis2Cfg.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrDataFlowAnalysis2Cfg(cfg).transform();
	}

	private final CFG cfg;

	/**
	 * Current frame.
	 */
	private Frame frame;

	private Frame[] frames;

	private boolean isIgnoreExceptions;

	/**
	 * Remember open PCs.
	 */
	private final LinkedList<Integer> openPcs = new LinkedList<Integer>();

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2bbs;

	private Map<Integer, Sub> pc2sub;

	private TrDataFlowAnalysis2Cfg(final CFG cfg) {
		this.cfg = cfg;
	}

	private Frame createInitialFrame() {
		final Frame frame = new Frame(this.cfg.getRegs());
		for (int reg = frame.getRegs(); reg-- > 0;) {
			final V v = this.cfg.getDebugV(reg, 0);
			if (v != null) {
				frame.set(reg, new R(v.getT(), Kind.MERGE));
			}
		}
		return frame;
	}

	private void evalBinaryMath(final T t) {
		evalBinaryMath(t, null);
	}

	private void evalBinaryMath(final T t, final T pushT) {
		final R r2 = pop(t);
		final R r1 = pop(t);

		if (r1.getT().isReference()) {
			// (J)CMP EQ / NE
			assert pushT == T.VOID;

			return;
		}
		final T mergedT = r1.getT().merge(r2.getT());
		// TODO reduce...
		// r1.cmpSetT(mergedT);
		// r2.cmpSetT(mergedT);
		if (pushT != T.VOID) {
			this.frame.push(pushT == null ? new R(r1.getT(), Kind.PUSH, r1) : new R(pushT,
					Kind.PUSH));
		}
	}

	private R get(final int i, final T t) {
		final R r = this.frame.get(i);
		// v.cmpSetT(v.getT().mergeTo(t));
		return r;
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

	private void merge(final int pc) {
		final Frame oldFrame = this.frames[pc];
		if (oldFrame == null) {
			// copy frame, could be cond or switch with multiple merge-targets
			this.frames[pc] = new Frame(this.frame);
			return;
		}
		for (int reg = this.frame.getRegs(); reg-- > 0;) {
			final R r = this.frame.get(reg);
			if (r == null) {
				continue;
			}
			final R oldR = oldFrame.get(reg);
			if (r == oldR) {
				continue;
			}
			if (oldR == null) {
				oldFrame.set(reg, oldR);
				continue;
			}
			final T t = oldR.getT().merge(r.getT());
			final R mergeR = new R(t, Kind.MERGE, oldR, r);
			// TODO forward replace in frames, replace ins/outs
			oldFrame.set(reg, mergeR);
		}
		for (int i = this.frame.getStackSize(); i-- > 0;) {
			final R r = this.frame.getStack(i);
			if (r == null) {
				System.out.println("SUCKER");
				continue;
			}
			final R oldR = oldFrame.getStack(i);
			if (r == oldR) {
				continue;
			}
			if (oldR == null) {
				System.out.println("SUCKER2");
				continue;
			}
			final T t = oldR.getT().merge(r.getT());
			oldR.setT(t);
			r.setT(t);
			final R mergeR = new R(t, Kind.MERGE, oldR, r);
			// TODO forward replace in frames, replace ins/outs
			oldFrame.setStack(i, mergeR);
		}
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

	private R pop(final T t) {
		final R r = this.frame.pop();
		// v.cmpSetT(v.getT().mergeTo(t));
		return r;
	}

	private void transform() {
		final MD md = this.cfg.getMd();
		final DU du = md.getM().getT().getDu();
		this.isIgnoreExceptions = md.getTd().getCu().check(DFlag.IGNORE_EXCEPTIONS);

		final Op[] ops = this.cfg.getOps();
		this.frames = new Frame[ops.length];
		this.frames[0] = createInitialFrame();
		this.cfg.setFrames(this.frames); // init early for analysis in Eclipse view
		this.pc2bbs = new BB[ops.length];

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
			if (this.frames[pc] != null) {
				this.frame = new Frame(this.frames[pc]);
			}
			final Op op = ops[pc++];
			bb.addOp(op);

			// TODO bug with back loops to same block, split must not change this BB!!!
			// Resolve with new Merge/CFG-Analyzer
			switch (op.getOptype()) {
			case GET: {
				final GET cop = (GET) op;
				final F f = cop.getF();
				if (!f.check(AF.STATIC)) {
					pop(f.getT());
				}
				this.frame.push(new R(f.getValueT(), Kind.PUSH));
				break;
			}
			case GOTO: {
				final GOTO cop = (GOTO) op;
				// follow without new BB, lazy splitting, at target PC other catches possible!
				pc = cop.getTargetPc();
				break;
			}
			case INVOKE: {
				final INVOKE cop = (INVOKE) op;
				final M m = cop.getM();
				for (int i = m.getParamTs().length; i-- > 0;) {
					// m(int) also accepts byte, short and char
					pop(m.getParamTs()[i] == T.INT ? T.IINT : m.getParamTs()[i]);
				}
				if (!m.check(AF.STATIC)) {
					pop(m.getT());
				}
				if (m.getReturnT() != T.VOID) {
					this.frame.push(new R(m.getReturnT(), Kind.PUSH));
				}
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				evalBinaryMath(cop.getT(), T.VOID);
				merge(pc);
				merge(cop.getTargetPc());
				bb.setCondSuccs(getTarget(pc), getTarget(cop.getTargetPc()));
				pc = ops.length; // next open pc
				continue;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				pop(cop.getT());
				merge(pc);
				merge(cop.getTargetPc());
				bb.setCondSuccs(getTarget(pc), getTarget(cop.getTargetPc()));
				pc = ops.length; // next open pc
				continue;
			}
			case JSR: {
				// Spec, JSR/RET is stack-like:
				// http://docs.oracle.com/javase/7/specs/jvms/JVMS-JavaSE7.pdf
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
			case LOAD: {
				final LOAD cop = (LOAD) op;
				final R r = get(cop.getReg(), cop.getT());
				this.frame.push(new R(r.getT(), Kind.PUSH, r));
				break;
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
			case PUSH: {
				final PUSH cop = (PUSH) op;
				this.frame.push(new R(cop.getT(), cop.getValue(), Kind.PUSH));
				break;
			}
			case RET: {
				final RET cop = (RET) op;

				// TODO

				pc = ops.length; // next open pc
				continue;
			}
			case RETURN: {
				assert op instanceof RETURN;

				// don't need op type here, could check, but why should we...
				final T returnT = md.getM().getReturnT();
				if (returnT != T.VOID) {
					pop(returnT); // reduce only
				}
				pc = ops.length; // next open pc
				continue;
			}
			case STORE: {
				final STORE cop = (STORE) op;
				final R r = pop(cop.getT());
				final R varR = this.frame.get(cop.getReg()); // potential var let
				// TODO incompatible types? remove varR
				if (varR == null) {
					this.frame.set(cop.getReg(), new R(r.getT(), Kind.STORE, r));
				} else {
					this.frame.set(cop.getReg(), new R(r.getT(), Kind.STORE, r, varR));
				}
				break;
			}
			case THROW:
				assert op instanceof THROW;

				pop(du.getT(Throwable.class)); // reduce only
				pc = ops.length; // next open pc
				continue;
			}
			merge(pc);
		}
		this.cfg.calculatePostorder();
	}

}