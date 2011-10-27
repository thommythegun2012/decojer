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
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
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
public class TrDataFlowAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrDataFlowAnalysis.class.getName());

	public static void transform(final CFG cfg) {
		new TrDataFlowAnalysis(cfg).transform();
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

	private Frame[] frames;

	private int pc;

	// sorted set superior to as a DFS queue here, because this algorithm has a stronger backward
	// component then the normal data flow analysis,
	// currently works with var.startPc, better would be an operation-postorder
	private final TreeSet<Integer> queue = new TreeSet<Integer>();

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
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

	private void evalBinaryMath(final Frame frame, final T t) {
		evalBinaryMath(frame, t, null);
	}

	private void evalBinaryMath(final Frame frame, final T t, final T pushT) {
		final V v2 = pop(frame, t);
		final V v1 = pop(frame, t);

		final T resultT = v1.getT().merge(v2.getT());

		if (!v1.getT().isReference()) {
			// (J)CMP EQ / NE
			if (v1.getT() != resultT) {
				v1.mergeTo(resultT);
				this.queue.add(v1.getStartPc());
			}
			if (v2.getT() != resultT) {
				v2.mergeTo(resultT);
				this.queue.add(v2.getStartPc());
			}
		}

		if (pushT != T.VOID) {
			push(frame, pushT == null ? resultT : pushT);
		}
	}

	private V get(final Frame frame, final int i, final T t) {
		final V v = frame.get(i);
		if (v.getEndPc() < this.pc) {
			v.setEndPc(this.pc);
		}
		if (v.mergeTo(t)) {
			this.queue.add(v.getStartPc());
		}
		return v;
	}

	private boolean isWide(final Op op) {
		final V v = this.cfg.getInFrame(op).peek();
		if (v == null) {
			return false;
		}
		return v.getT().isWide();
	}

	private void merge(final Frame calculatedFrame, final int targetPc) {
		final Frame targetFrame = this.frames[targetPc];
		if (targetFrame == null) {
			// copy frame, could be cond or switch with multiple merge-targets
			this.frames[targetPc] = new Frame(calculatedFrame);
		} else if (!targetFrame.merge(calculatedFrame)) {
			return;
		}
		this.queue.add(targetPc);
	}

	private V pop(final Frame frame, final T t) {
		final V v = frame.pop();
		if (v.getEndPc() < this.pc) {
			v.setEndPc(this.pc);
		}
		if (v.mergeTo(t)) {
			this.queue.add(v.getStartPc());
		}
		return v;
	}

	private V push(final Frame frame, final T t) {
		final V v = new V(t);
		// known in follow frame! no push through control flow operations
		v.setStartPc(this.pc + 1);
		v.setEndPc(this.pc + 1);
		frame.push(v);
		return v;
	}

	private void push(final Frame frame, final V v) {
		if (v.getEndPc() <= this.pc) {
			// known in follow frame! no push through control flow operations
			v.setEndPc(this.pc + 1);
		}
		frame.push(v);
	}

	private void set(final Frame frame, final int i, final V v) {
		if (v.getEndPc() <= this.pc) {
			v.setEndPc(this.pc + 1);
		}
		frame.set(i, v);
	}

	private void transform() {
		final Op[] ops = this.cfg.getOps();
		this.frames = new Frame[ops.length];
		this.cfg.setFrames(this.frames); // assign early for debugging...
		this.frames[0] = createMethodFrame();

		this.queue.clear();
		this.queue.add(0);
		while (!this.queue.isEmpty()) {
			this.pc = this.queue.pollFirst();
			// first we copy the current in frame for the operation, then we
			// calculate the type-changes through the operation and then we
			// merge this calculated frame-types to the out frame

			// IDEA why copy and change-detect copy-backs...would also work via
			// direct updates of register / stack data - much cooler

			// shallow copy of calculation frame
			final Frame frame = new Frame(this.frames[this.pc]);
			final Op op = ops[this.pc];
			switch (op.getOptype()) {
			case ADD: {
				final ADD cop = (ADD) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case ALOAD: {
				final ALOAD cop = (ALOAD) op;
				pop(frame, T.INT); // index
				pop(frame, T.AREF); // array
				push(frame, cop.getT()); // value
				break;
			}
			case AND: {
				final AND cop = (AND) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case ARRAYLENGTH: {
				assert op instanceof ARRAYLENGTH;

				pop(frame, T.AREF); // array
				push(frame, T.INT); // length
				break;
			}
			case ASTORE: {
				final ASTORE cop = (ASTORE) op;
				pop(frame, cop.getT()); // value
				pop(frame, T.INT); // index
				pop(frame, T.AREF); // array
				break;
			}
			case CAST: {
				final CAST cop = (CAST) op;
				pop(frame, cop.getT());
				push(frame, cop.getToT());
				break;
			}
			case CMP: {
				final CMP cop = (CMP) op;
				evalBinaryMath(frame, cop.getT(), T.INT);
				break;
			}
			case DIV: {
				final DIV cop = (DIV) op;
				evalBinaryMath(frame, cop.getT());
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
					// ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
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
					// Duplicate the top one or two operand stack values and insert two, three, or
					// four values down
					// ..., value4, value3, value2, value1 => ..., value2, value1, value4, value3,
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

				pop(frame, T.AREF);
				break;
			}
			case GET: {
				final GET cop = (GET) op;
				final F f = cop.getF();
				if (!f.checkAf(AF.STATIC)) {
					pop(frame, f.getT());
				}
				push(frame, f.getValueT());
				break;
			}
			case GOTO: {
				final GOTO cop = (GOTO) op;
				merge(frame, cop.getTargetPc());
				continue;
			}
			case INC: {
				assert op instanceof INC;

				// TODO reduce bool
				break;
			}
			case INSTANCEOF: {
				assert op instanceof INSTANCEOF;

				pop(frame, T.AREF);
				// operation contains check-type as argument, not important here
				push(frame, T.BOOLEAN);
				break;
			}
			case INVOKE: {
				final INVOKE cop = (INVOKE) op;
				final M m = cop.getM();
				for (int i = m.getParamTs().length; i-- > 0;) {
					// m(int) also accepts byte, short and char
					pop(frame, m.getParamTs()[i] == T.INT ? T.IINT : m.getParamTs()[i]);
				}
				if (!m.checkAf(AF.STATIC)) {
					pop(frame, m.getT());
				}
				if (m.getReturnT() != T.VOID) {
					push(frame, m.getReturnT());
				}
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				evalBinaryMath(frame, cop.getT(), T.VOID);
				merge(frame, cop.getTargetPc());
				break;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				pop(frame, cop.getT());
				merge(frame, cop.getTargetPc());
				break;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;
				final V v = get(frame, cop.getReg(), cop.getT());
				push(frame, v); // OK
				break;
			}
			case MONITOR: {
				assert op instanceof MONITOR;

				pop(frame, T.AREF);
				break;
			}
			case MUL: {
				final MUL cop = (MUL) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case NEG: {
				final NEG cop = (NEG) op;
				final V v = pop(frame, cop.getT());
				push(frame, v); // OK
				break;
			}
			case NEW: {
				final NEW cop = (NEW) op;
				push(frame, cop.getT());
				break;
			}
			case NEWARRAY: {
				final NEWARRAY cop = (NEWARRAY) op;
				String name = cop.getT().getName();
				for (int i = cop.getDimensions(); i-- > 0;) {
					pop(frame, T.INT);
					name += "[]";
				}
				push(frame, this.cfg.getMd().getM().getT().getDu().getT(name));
				break;
			}
			case OR: {
				final OR cop = (OR) op;
				evalBinaryMath(frame, cop.getT());
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
				push(frame, cop.getT());
				break;
			}
			case PUT: {
				final PUT cop = (PUT) op;
				final F f = cop.getF();
				pop(frame, f.getValueT());
				if (!f.checkAf(AF.STATIC)) {
					pop(frame, f.getT());
				}
				break;
			}
			case REM: {
				final REM cop = (REM) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case RETURN: {
				assert op instanceof RETURN;

				// don't need op type here, could check, but why should we...
				final T returnT = this.cfg.getMd().getM().getReturnT();
				if (returnT != T.VOID) {
					pop(frame, returnT);
				}
				continue;
			}
			case SHL: {
				final SHL cop = (SHL) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case SHR: {
				final SHR cop = (SHR) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case STORE: {
				final STORE cop = (STORE) op;
				final V v = pop(frame, cop.getT());

				V storeV = frame.get(cop.getReg());

				if (storeV != null) {
					if (this.pc <= storeV.getEndPc()) {
						// TODO
						if (v.merge(storeV.getT())) {
							this.queue.add(storeV.getStartPc());
						}
						if (storeV.getEndPc() <= this.pc) {
							storeV.setEndPc(this.pc + 1);
						}
						break;
					}
					// TODO could check assignable instead of merge
				}
				final V debugV = this.cfg.getDebugV(cop.getReg(), this.pc + 1);
				if (debugV != null) {
					storeV = new V(debugV);
					// TODO
					v.merge(storeV.getT());
				} else {
					storeV = new V(v.getT());
					storeV.setStartPc(this.pc + 1);
				}
				set(frame, cop.getReg(), storeV);
				break;
			}
			case SUB: {
				final SUB cop = (SUB) op;
				evalBinaryMath(frame, cop.getT());
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
				pop(frame, T.INT);
				merge(frame, cop.getDefaultPc());
				for (final int casePc : cop.getCasePcs()) {
					merge(frame, casePc);
				}
				continue;
			}
			case THROW: {
				assert op instanceof THROW;

				pop(frame, T.AREF); // TODO Throwable
				continue;
			}
			case XOR: {
				final XOR cop = (XOR) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			default:
				LOGGER.warning("Operation '" + op + "' not handled!");
			}
			merge(frame, this.pc + 1);
		}
	}

}