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

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.R.Kind;
import org.decojer.cavaj.model.code.Sub;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.op.ADD;
import org.decojer.cavaj.model.code.op.ALOAD;
import org.decojer.cavaj.model.code.op.AND;
import org.decojer.cavaj.model.code.op.ARRAYLENGTH;
import org.decojer.cavaj.model.code.op.ASTORE;
import org.decojer.cavaj.model.code.op.CAST;
import org.decojer.cavaj.model.code.op.CMP;
import org.decojer.cavaj.model.code.op.DIV;
import org.decojer.cavaj.model.code.op.DUP;
import org.decojer.cavaj.model.code.op.FILLARRAY;
import org.decojer.cavaj.model.code.op.GET;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.INC;
import org.decojer.cavaj.model.code.op.INSTANCEOF;
import org.decojer.cavaj.model.code.op.INVOKE;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.JSR;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.MONITOR;
import org.decojer.cavaj.model.code.op.MUL;
import org.decojer.cavaj.model.code.op.NEG;
import org.decojer.cavaj.model.code.op.NEW;
import org.decojer.cavaj.model.code.op.NEWARRAY;
import org.decojer.cavaj.model.code.op.OR;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.POP;
import org.decojer.cavaj.model.code.op.PUSH;
import org.decojer.cavaj.model.code.op.PUT;
import org.decojer.cavaj.model.code.op.REM;
import org.decojer.cavaj.model.code.op.RET;
import org.decojer.cavaj.model.code.op.RETURN;
import org.decojer.cavaj.model.code.op.SHL;
import org.decojer.cavaj.model.code.op.SHR;
import org.decojer.cavaj.model.code.op.STORE;
import org.decojer.cavaj.model.code.op.SUB;
import org.decojer.cavaj.model.code.op.SWAP;
import org.decojer.cavaj.model.code.op.SWITCH;
import org.decojer.cavaj.model.code.op.THROW;
import org.decojer.cavaj.model.code.op.XOR;

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
		final T mergeT = R.merge(r1, r2);
		r1.mergeTo(mergeT);
		r2.mergeTo(mergeT);
		if (pushT != T.VOID) {
			this.frame.push(pushT == null ? new R(r1.getT(), Kind.CONST, r1) : new R(pushT,
					Kind.CONST));
		}
	}

	private R get(final int i, final T t) {
		final R r = this.frame.get(i);
		r.mergeTo(t == T.INT ? T.AINT : t);
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

		// split basic block, new incoming block, adapt basic block pcs,
		// it's necessary to preserve the outgoing block for back edges to same BB!!!
		final BB split = newBb(bb.getOpPc());

		bb.moveIns(split);
		split.setSucc(bb);
		final List<Op> ops = bb.getOps();
		while (!ops.isEmpty() && ops.get(0).getPc() != pc) {
			final Op op = ops.remove(0);
			split.addOp(op);
			this.pc2bbs[op.getPc()] = split;
		}
		bb.setOpPc(pc);
		return bb;
	}

	private boolean isWide(final Op op) {
		final R r = this.cfg.getInFrame(op).peek();
		return r == null ? false : r.getT().isWide();
	}

	private void merge(final int pc) {
		final Frame oldFrame = this.cfg.getFrame(pc);
		if (oldFrame == null) {
			this.cfg.setFrame(pc, this.frame);
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
			// TODO what if we encounter alive new merge?
			final R mergeR = oldR == null ? r : new R(R.merge(oldR, r), Kind.MERGE, oldR, r);
			replace(this.pc2bbs[pc], reg, oldR, mergeR);
		}
		for (int i = this.frame.getStackSize(); i-- > 0;) {
			final R r = this.frame.getStack(i);
			if (r == null) {
				LOGGER.warning("Stack register is null!");
				continue;
			}
			final R oldR = oldFrame.getStack(i);
			if (r == oldR) {
				continue;
			}
			if (oldR == null) {
				LOGGER.warning("Stack register is null!");
				continue;
			}
			final T mergeT = R.merge(oldR, r);
			oldR.mergeTo(mergeT);
			r.mergeTo(mergeT);
			final R mergeR = new R(mergeT, Kind.MERGE, oldR, r);
			replace(this.pc2bbs[pc], this.cfg.getRegs() + i, oldR, mergeR);
		}
	}

	private void mergeExc(final Exc[] excs) {
		if (excs == null) {
			return;
		}
		for (final Exc exc : excs) {
			if (exc.validIn(0/* TODO this.pc */)) {
				this.frame.clearStack();
				// null is <any> (finally) -> Throwable
				push(exc.getT() == null ? this.cfg.getMd().getM().getT().getDu()
						.getT(Throwable.class) : exc.getT());
				merge(exc.getHandlerPc());
			}
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
		r.mergeTo(t == T.INT ? T.AINT : t);
		return r;
	}

	private R push(final T t) {
		final R r = new R(t, Kind.CONST);
		this.frame.push(r);
		return r;
	}

	private void replace(final BB bb, final int reg, final R oldR, final R r) {
		for (final Op op : bb.getOps()) {
			final Frame oldFrame = this.cfg.getInFrame(op);
			final R oldFrameR = oldFrame.get(reg);
			if (oldFrameR != oldR) {
				// ins/outs

				// oldR can be null...but add to ins?

				return;
			}
			oldFrame.set(reg, r);
		}
		for (final E out : bb.getOuts()) {
			replace(out.getEnd(), reg, oldR, r);
		}
	}

	private void transform() {
		final MD md = this.cfg.getMd();
		final DU du = md.getM().getT().getDu();
		this.isIgnoreExceptions = md.getTd().getCu().check(DFlag.IGNORE_EXCEPTIONS);

		final Op[] ops = this.cfg.getOps();
		this.cfg.initFrames(createInitialFrame());
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
			this.frame = new Frame(this.cfg.getFrame(pc));
			final Op op = ops[pc++];
			bb.addOp(op);

			switch (op.getOptype()) {
			case ADD: {
				final ADD cop = (ADD) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case ALOAD: {
				final ALOAD cop = (ALOAD) op;
				pop(T.INT); // index
				pop(T.AREF); // array
				push(cop.getT()); // value
				break;
			}
			case AND: {
				final AND cop = (AND) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case ARRAYLENGTH: {
				assert op instanceof ARRAYLENGTH;

				pop(T.AREF); // array
				push(T.INT); // length
				break;
			}
			case ASTORE: {
				final ASTORE cop = (ASTORE) op;
				pop(cop.getT()); // value
				pop(T.INT); // index
				pop(T.AREF); // array
				break;
			}
			case CAST: {
				final CAST cop = (CAST) op;
				pop(cop.getT());
				push(cop.getToT());
				break;
			}
			case CMP: {
				final CMP cop = (CMP) op;
				evalBinaryMath(cop.getT(), T.INT);
				break;
			}
			case DIV: {
				final DIV cop = (DIV) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case DUP: {
				// TODO convert to MOVE?!
				final DUP cop = (DUP) op;
				switch (cop.getDupType()) {
				case DUP.T_DUP2:
					// Duplicate the top one or two operand stack values
					// ..., value2, value1 => ..., value2, value1, value2, value1
					// wide:
					// ..., value => ..., value, value
					if (!isWide(cop)) {
						final R e1 = this.frame.pop();
						final R e2 = this.frame.pop();
						this.frame.push(e2);
						this.frame.push(e1);
						this.frame.push(e2);
						this.frame.push(e1);
						break;
					}
					// fall through for wide
				case DUP.T_DUP:
					// Duplicate the top operand stack value
					this.frame.push(this.frame.peek());
					break;
				case DUP.T_DUP2_X1:
					// Duplicate the top one or two operand stack values and insert two or three
					// values down
					// ..., value3, value2, value1 => ..., value2, value1, value3, value2,
					// value1
					// wide:
					// ..., value2, value1 => ..., value1, value2, value1
					if (!isWide(cop)) {
						final R e1 = this.frame.pop();
						final R e2 = this.frame.pop();
						final R e3 = this.frame.pop();
						this.frame.push(e2);
						this.frame.push(e1);
						this.frame.push(e3);
						this.frame.push(e2);
						this.frame.push(e1);
						break;
					}
					// fall through for wide
				case DUP.T_DUP_X1: {
					// Duplicate the top operand stack value and insert two values down
					final R e1 = this.frame.pop();
					final R e2 = this.frame.pop();
					this.frame.push(e1);
					this.frame.push(e2);
					this.frame.push(e1);
					break;
				}
				case DUP.T_DUP2_X2:
					// Duplicate the top one or two operand stack values and insert two, three,
					// or
					// four values down
					// ..., value4, value3, value2, value1 => ..., value2, value1, value4,
					// value3,
					// value2, value1
					// wide:
					// ..., value3, value2, value1 => ..., value1, value3, value2, value1
					if (!isWide(cop)) {
						final R e1 = this.frame.pop();
						final R e2 = this.frame.pop();
						final R e3 = this.frame.pop();
						final R e4 = this.frame.pop();
						this.frame.push(e2);
						this.frame.push(e1);
						this.frame.push(e4);
						this.frame.push(e3);
						this.frame.push(e2);
						this.frame.push(e1);
						break;
					}
					// fall through for wide
				case DUP.T_DUP_X2: {
					// Duplicate the top operand stack value and insert two or three values down
					final R e1 = this.frame.pop();
					final R e2 = this.frame.pop();
					final R e3 = this.frame.pop();
					this.frame.push(e1);
					this.frame.push(e3);
					this.frame.push(e2);
					this.frame.push(e1);
					break;
				}
				default:
					LOGGER.warning("Unknown dup type '" + cop.getDupType() + "'!");
				}
				break;
			}
			case FILLARRAY: {
				assert op instanceof FILLARRAY;

				pop(T.AREF);
				break;
			}
			case GET: {
				final GET cop = (GET) op;
				final F f = cop.getF();
				if (!f.check(AF.STATIC)) {
					pop(f.getT());
				}
				push(f.getValueT());
				break;
			}
			case GOTO: {
				final GOTO cop = (GOTO) op;
				// follow without new BB, lazy splitting, at target PC other catches possible!
				pc = cop.getTargetPc();
				break;
			}
			case INC: {
				assert op instanceof INC;

				// TODO reduce bool
				break;
			}
			case INSTANCEOF: {
				assert op instanceof INSTANCEOF;

				pop(T.AREF);
				// operation contains check-type as argument, not important here
				push(T.BOOLEAN);
				break;
			}
			case INVOKE: {
				final INVOKE cop = (INVOKE) op;
				final M m = cop.getM();
				for (int i = m.getParamTs().length; i-- > 0;) {
					// implicit conversation
					// m(int) also accepts byte, short and char in Java (no bool)
					// m(short) also accepts byte in Java
					T paramT = m.getParamTs()[i];
					if (paramT == T.INT) {
						paramT = T.IINT;
					} else if (paramT == T.SHORT) {
						paramT = T.multi(T.SHORT, T.BYTE);
					}
					pop(paramT);
				}
				if (!m.check(AF.STATIC)) {
					pop(m.getT());
				}
				if (m.getReturnT() != T.VOID) {
					push(m.getReturnT());
				}
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				bb.setCondSuccs(getTarget(pc), getTarget(cop.getTargetPc()));
				evalBinaryMath(cop.getT(), T.VOID);
				merge(pc);
				merge(cop.getTargetPc());
				pc = ops.length; // next open pc
				continue;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				bb.setCondSuccs(getTarget(pc), getTarget(cop.getTargetPc()));
				pop(cop.getT());
				merge(pc);
				merge(cop.getTargetPc());
				pc = ops.length; // next open pc
				continue;
			}
			case JSR: {
				// Spec, JSR/RET is stack-like:
				// http://docs.oracle.com/javase/7/specs/jvms/JVMS-JavaSE7.pdf
				final JSR cop = (JSR) op;
				/*
				 * if (this.pc2sub == null) { this.pc2sub = new HashMap<Integer, Sub>();
				 * this.pc2sub.put(cop.getTargetPc(), new Sub(cop)); } else { Sub sub =
				 * this.pc2sub.get(cop.getTargetPc()); if (sub == null) {
				 * this.pc2sub.put(cop.getTargetPc(), sub = new Sub(cop)); } else { sub.addJsr(cop);
				 * } } bb.setSucc(getTarget(cop.getTargetPc()));
				 */
				// use target address instead of jsr follow because of merge:
				this.frame.push(new R(T.RETURN_ADDRESS, cop.getTargetPc(), Kind.CONST));
				merge(cop.getTargetPc());
				pc = ops.length; // next open pc
				continue;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;
				final R r = get(cop.getReg(), cop.getT());
				// no previous for stack
				this.frame.push(new R(r.getT(), r.getValue(), Kind.MOVE, r));
				break;
			}
			case MONITOR: {
				assert op instanceof MONITOR;

				pop(T.AREF);
				break;
			}
			case MUL: {
				final MUL cop = (MUL) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case NEG: {
				final NEG cop = (NEG) op;
				final R r = pop(cop.getT());
				push(r.getT());
				break;
			}
			case NEW: {
				final NEW cop = (NEW) op;
				push(cop.getT());
				break;
			}
			case NEWARRAY: {
				final NEWARRAY cop = (NEWARRAY) op;
				for (int i = cop.getDimensions(); i-- > 0;) {
					pop(T.INT);
				}
				push(this.cfg.getMd().getM().getT().getDu()
						.getArrayT(cop.getT(), cop.getDimensions()));
				break;
			}
			case OR: {
				final OR cop = (OR) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case POP: {
				final POP cop = (POP) op;
				switch (cop.getPopType()) {
				case POP.T_POP2:
					// Pop the top one or two operand stack values
					// ..., value2, value1 => ...
					// wide:
					// ..., value => ...
					if (!isWide(cop)) {
						this.frame.pop();
						this.frame.pop();
						break;
					}
					// fall through for wide
				case POP.T_POP:
					// Pop the top operand stack value
					this.frame.pop();
					break;
				default:
					LOGGER.warning("Unknown pop type '" + cop.getPopType() + "'!");
				}
				break;
			}
			case PUSH: {
				final PUSH cop = (PUSH) op;
				// no previous for stack
				this.frame.push(new R(cop.getT(), cop.getValue(), Kind.CONST));
				// TODO check BYTE / SHORT value ranges!
				break;
			}
			case PUT: {
				final PUT cop = (PUT) op;
				final F f = cop.getF();
				pop(f.getValueT());
				if (!f.check(AF.STATIC)) {
					pop(f.getT());
				}
				break;
			}
			case REM: {
				final REM cop = (REM) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case RET: {
				final RET cop = (RET) op;

				get(cop.getReg(), T.RETURN_ADDRESS);
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
			case SHL: {
				final SHL cop = (SHL) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case SHR: {
				final SHR cop = (SHR) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case STORE: {
				final STORE cop = (STORE) op;
				final R r = pop(cop.getT());
				final R prevR = this.frame.get(cop.getReg());
				// TODO incompatible types? remove prevR
				if (prevR == null) {
					this.frame.set(cop.getReg(), new R(r.getT(), r.getValue(), Kind.MOVE, r));
				} else {
					this.frame
							.set(cop.getReg(), new R(r.getT(), r.getValue(), Kind.MOVE, r, prevR));
				}
				break;
			}
			case SUB: {
				final SUB cop = (SUB) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case SWAP: {
				// Swap the top two operand stack values
				// ..., value2, value1 ..., value1, value2
				// wide: not supported on JVM!
				assert op instanceof SWAP;

				final R e1 = this.frame.pop();
				final R e2 = this.frame.pop();
				this.frame.push(e1);
				this.frame.push(e2);
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

				pop(T.INT);
				merge(cop.getDefaultPc());
				for (final int casePc : cop.getCasePcs()) {
					merge(casePc);
				}
				pc = ops.length; // next open pc
				continue;
			}
			case THROW:
				assert op instanceof THROW;

				pop(du.getT(Throwable.class)); // reduce only
				pc = ops.length; // next open pc
				continue;
			case XOR: {
				final XOR cop = (XOR) op;
				evalBinaryMath(cop.getT());
				break;
			}
			default:
				LOGGER.warning("Operation '" + op + "' not handled!");
			}
			merge(pc);
		}
		this.cfg.calculatePostorder();
	}

}