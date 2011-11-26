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
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.Frame;
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
 * Transform Data Flow Analysis.
 * 
 * @author André Pankraz
 */
public final class TrDataFlowAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrDataFlowAnalysis.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrDataFlowAnalysis(cfg).transform();
	}

	private boolean changed;

	private final CFG cfg;

	private Frame[] frames;

	private Frame frame;

	private int pc;

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private void analyze(final BB bb) {
		for (final Op op : bb.getOps()) {
			final int pc = op.getPc();
			// first we copy the current in frame for the operation, then we
			// calculate the type-changes through the operation and then we
			// merge this calculated frame-types to the out frame

			// shallow copy of calculation frame
			final Frame frame = new Frame(this.frames[pc]);
			this.frame = frame;
			this.pc = pc;

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
				final DUP cop = (DUP) op;
				switch (cop.getDupType()) {
				case DUP.T_DUP2:
					// Duplicate the top one or two operand stack values
					// ..., value2, value1 => ..., value2, value1, value2, value1
					// wide:
					// ..., value => ..., value, value
					if (!isWide(cop)) {
						final V e1 = frame.pop();
						final V e2 = frame.pop();
						frame.push(e2);
						frame.push(e1);
						frame.push(e2);
						frame.push(e1);
						break;
					}
					// fall through for wide
				case DUP.T_DUP:
					// Duplicate the top operand stack value
					frame.push(frame.peek());
					break;
				case DUP.T_DUP2_X1:
					// Duplicate the top one or two operand stack values and insert two or three
					// values down
					// ..., value3, value2, value1 => ..., value2, value1, value3, value2,
					// value1
					// wide:
					// ..., value2, value1 => ..., value1, value2, value1
					if (!isWide(cop)) {
						final V e1 = frame.pop();
						final V e2 = frame.pop();
						final V e3 = frame.pop();
						frame.push(e2);
						frame.push(e1);
						frame.push(e3);
						frame.push(e2);
						frame.push(e1);
						break;
					}
					// fall through for wide
				case DUP.T_DUP_X1: {
					// Duplicate the top operand stack value and insert two values down
					final V e1 = frame.pop();
					final V e2 = frame.pop();
					frame.push(e1);
					frame.push(e2);
					frame.push(e1);
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
						final V e1 = frame.pop();
						final V e2 = frame.pop();
						final V e3 = frame.pop();
						final V e4 = frame.pop();
						frame.push(e2);
						frame.push(e1);
						frame.push(e4);
						frame.push(e3);
						frame.push(e2);
						frame.push(e1);
						break;
					}
					// fall through for wide
				case DUP.T_DUP_X2: {
					// Duplicate the top operand stack value and insert two or three values down
					final V e1 = frame.pop();
					final V e2 = frame.pop();
					final V e3 = frame.pop();
					frame.push(e1);
					frame.push(e3);
					frame.push(e2);
					frame.push(e1);
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
				if (!f.checkAf(AF.STATIC)) {
					pop(f.getT());
				}
				push(f.getValueT());
				break;
			}
			case GOTO: {
				final GOTO cop = (GOTO) op;
				merge(cop.getTargetPc());
				continue;
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
					// m(int) also accepts byte, short and char
					pop(m.getParamTs()[i] == T.INT ? T.IINT : m.getParamTs()[i]);
				}
				if (!m.checkAf(AF.STATIC)) {
					pop(m.getT());
				}
				if (m.getReturnT() != T.VOID) {
					push(m.getReturnT());
				}
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				evalBinaryMath(cop.getT(), T.VOID);
				merge(cop.getTargetPc());
				break;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				pop(cop.getT());
				merge(cop.getTargetPc());
				break;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;
				final V v = get(cop.getReg(), cop.getT());
				push(v); // no copy is OK
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
				final V v = pop(cop.getT());
				push(v); // OK
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
						frame.pop();
						frame.pop();
						break;
					}
					// fall through for wide
				case POP.T_POP:
					// Pop the top operand stack value
					frame.pop();
					break;
				default:
					LOGGER.warning("Unknown pop type '" + cop.getPopType() + "'!");
				}
				break;
			}
			case PUSH: {
				final PUSH cop = (PUSH) op;
				push(cop.getT());
				break;
			}
			case PUT: {
				final PUT cop = (PUT) op;
				final F f = cop.getF();
				pop(f.getValueT());
				if (!f.checkAf(AF.STATIC)) {
					pop(f.getT());
				}
				break;
			}
			case REM: {
				final REM cop = (REM) op;
				evalBinaryMath(cop.getT());
				break;
			}
			case RETURN: {
				assert op instanceof RETURN;

				// don't need op type here, could check, but why should we...
				final T returnT = this.cfg.getMd().getM().getReturnT();
				if (returnT != T.VOID) {
					pop(returnT);
				}
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
				final V v = pop(cop.getT());

				V storeV = frame.get(cop.getReg());

				if (storeV != null) {
					if (storeV.validIn(pc)) {
						final T mergedT = v.getT().merge(storeV.getT());
						this.changed |= v.cmpSetT(mergedT);
						// can happen for no debug info or temporary
						this.changed |= storeV.cmpSetT(mergedT);
						// endPc update-check because of if-condition not reasonable
						break;
					}
					// TODO could check assignable instead of merge,
					// or read in another branch? => merge
				}
				final V debugV = this.cfg.getDebugV(cop.getReg(), pc + 1);
				if (debugV != null) {
					storeV = new V(debugV);
					this.changed |= v.cmpSetT(v.getT().mergeTo(storeV.getT()));
				} else {
					// TODO could be tmp or none-debug-info?!
					storeV = new V(v.getT(), null, pc + 1, pc + 1);
				}
				set(cop.getReg(), storeV);
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

				final V e1 = frame.pop();
				final V e2 = frame.pop();
				frame.push(e1);
				frame.push(e2);
				break;
			}
			case SWITCH: {
				final SWITCH cop = (SWITCH) op;
				pop(T.INT);
				merge(cop.getDefaultPc());
				for (final int casePc : cop.getCasePcs()) {
					merge(casePc);
				}
				continue;
			}
			case THROW: {
				assert op instanceof THROW;

				pop(T.AREF); // TODO Throwable
				continue;
			}
			case XOR: {
				final XOR cop = (XOR) op;
				evalBinaryMath(cop.getT());
				break;
			}
			default:
				LOGGER.warning("Operation '" + op + "' not handled!");
			}
			merge(pc + 1);
		}
	}

	private Frame createMethodFrame() {
		final Frame frame = new Frame(this.cfg.getMaxLocals());
		for (int i = frame.getLocals(); i-- > 0;) {
			final V v = this.cfg.getDebugV(i, 0);
			if (v != null) {
				frame.set(i, new V(v));
			}
		}
		return frame;
	}

	private void evalBinaryMath(final T t) {
		evalBinaryMath(t, null);
	}

	private void evalBinaryMath(final T t, final T pushT) {
		final V v2 = pop(t);
		final V v1 = pop(t);

		if (v1.getT().isReference()) {
			// (J)CMP EQ / NE
			assert pushT == T.VOID;

			return;
		}
		final T mergedT = v1.getT().merge(v2.getT());
		this.changed |= v1.cmpSetT(mergedT);
		this.changed |= v2.cmpSetT(mergedT);
		if (pushT != T.VOID) {
			push(pushT == null ? v1.getT() : pushT);
		}
	}

	private V get(final int i, final T t) {
		final V v = this.frame.get(i);
		if (v.getPcs()[1] /* TODO */< this.pc) {
			v.getPcs()[1] = this.pc;
		}
		this.changed |= v.cmpSetT(v.getT().mergeTo(t));
		return v;
	}

	private boolean isWide(final Op op) {
		final V v = this.cfg.getInFrame(op).peek();
		if (v == null) {
			return false;
		}
		return v.getT().isWide();
	}

	private void merge(final int targetPc) {
		final Frame targetFrame = this.frames[targetPc];
		if (targetFrame == null) {
			// copy frame, could be cond or switch with multiple merge-targets
			this.frames[targetPc] = new Frame(this.frame);
			this.changed = true;
			return;
		}
		for (int i = this.frame.getLocals(); i-- > 0;) {
			final V v = this.frame.get(i);
			if (v == null) {
				// TODO could check if we run into a variable through a real branch merge?
				continue;
			}
			final V targetV = targetFrame.get(i);
			// TODO sometimes we don't like to merge, if we merge we must also add dom
			// declaration
			if (targetV == null) {
				targetFrame.set(i, v);
				this.changed = true;
				continue;
			}
			final T mergedT = targetV.getT().merge(v.getT());
			this.changed |= targetV.cmpSetT(mergedT);
			if (v.getPcs()[0] /* TODO */!= targetPc) {
				this.changed |= v.cmpSetT(mergedT);
			}
		}
		for (int i = this.frame.getStackSize(); i-- > 0;) {
			final V v = this.frame.getStack(i);
			final V targetV = targetFrame.getStack(i);
			final T mergedT = targetV.getT().merge(v.getT());
			this.changed |= targetV.cmpSetT(mergedT);
			if (v.getPcs()[0] /* TODO */!= targetPc) {
				this.changed |= v.cmpSetT(mergedT);
			}
		}
	}

	private V pop(final T t) {
		final V v = this.frame.pop();
		if (v.getPcs()[1] /* TODO */< this.pc) {
			v.getPcs()[1] = this.pc;
		}
		this.changed |= v.cmpSetT(v.getT().mergeTo(t));
		return v;
	}

	private V push(final T t) {
		// known in follow frame! no push through control flow operations
		final V v = new V(t, null, this.pc + 1, this.pc + 1);
		this.frame.push(v);
		return v;
	}

	private void push(final V v) {
		if (v.getPcs()[1] /* TODO */<= this.pc) {
			// known in follow frame! no push through control flow operations
			v.getPcs()[1] = this.pc + 1;
		}
		this.frame.push(v);
	}

	private void set(final int i, final V v) {
		if (v.getPcs()[1] /* TODO */<= this.pc) {
			v.getPcs()[1] = this.pc + 1;
		}
		this.frame.set(i, v);
	}

	private void transform() {
		this.frames = new Frame[this.cfg.getOps().length];
		this.cfg.setFrames(this.frames); // assign early for debugging...
		this.frames[0] = createMethodFrame();
		final List<BB> postorderedBbs = this.cfg.getPostorderedBbs();

		int todoHackPreventLoop = 0;

		do {
			this.changed = false;
			for (int i = postorderedBbs.size(); i-- > 0;) {
				analyze(postorderedBbs.get(i));
			}
		} while (this.changed && ++todoHackPreventLoop < 50);
	}

}