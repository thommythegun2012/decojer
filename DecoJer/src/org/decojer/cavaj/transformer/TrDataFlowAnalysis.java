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
import org.decojer.cavaj.model.code.Var;
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
		final Frame frame = new Frame(this.cfg.getMaxRegs());
		for (int index = frame.getRegsSize(); index-- > 0;) {
			frame.setReg(index, this.cfg.getVar(index, 0));
		}
		return frame;
	}

	private void evalBinaryMath(final Frame frame, final T t) {
		evalBinaryMath(frame, t, null);
	}

	private void evalBinaryMath(final Frame frame, final T t, final T pushT) {
		final Var var2 = pop(frame, t);
		final Var var1 = pop(frame, t);

		final T resultT = var1.getT().merge(var2.getT());

		if (!var1.getT().isReference()) {
			// (J)CMP EQ / NE
			if (var1.getT() != resultT) {
				var1.mergeTo(resultT);
				this.queue.add(var1.getStartPc());
			}
			if (var2.getT() != resultT) {
				var2.mergeTo(resultT);
				this.queue.add(var2.getStartPc());
			}
		}

		if (pushT != T.VOID) {
			push(frame, pushT == null ? resultT : pushT);
		}
	}

	private Var getReg(final Frame frame, final int index, final T t) {
		final Var var = frame.getReg(index);
		if (var.getEndPc() < this.pc) {
			var.setEndPc(this.pc);
		}
		if (var.mergeTo(t)) {
			this.queue.add(var.getStartPc());
		}
		return var;
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

	private Var pop(final Frame frame, final T t) {
		final Var var = frame.pop();
		if (var.getEndPc() < this.pc) {
			var.setEndPc(this.pc);
		}
		if (var.mergeTo(t)) {
			this.queue.add(var.getStartPc());
		}
		return var;
	}

	private Var push(final Frame frame, final T t) {
		final Var var = new Var(t);
		// known in follow frame! no push through control flow operations
		var.setStartPc(this.pc + 1);
		var.setEndPc(this.pc + 1);
		frame.push(var);
		return var;
	}

	private void push(final Frame frame, final Var var) {
		if (var.getEndPc() <= this.pc) {
			// known in follow frame! no push through control flow operations
			var.setEndPc(this.pc + 1);
		}
		frame.push(var);
	}

	private void setReg(final Frame frame, final int index, final Var var) {
		frame.setReg(index, var);
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
				case DUP.T_DUP:
					frame.push(frame.peek());
					break;
				case DUP.T_DUP_X1: {
					final Var e1 = frame.pop();
					final Var e2 = frame.pop();
					frame.push(e1);
					frame.push(e2);
					frame.push(e1);
					break;
				}
				case DUP.T_DUP_X2: {
					final Var e1 = frame.pop();
					final Var e2 = frame.pop();
					final Var e3 = frame.pop();
					frame.push(e1);
					frame.push(e3);
					frame.push(e2);
					frame.push(e1);
					break;
				}
				case DUP.T_DUP2: {
					final Var e1 = frame.pop();
					final Var e2 = frame.pop();
					frame.push(e2);
					frame.push(e1);
					frame.push(e2);
					frame.push(e1);
					break;
				}
				case DUP.T_DUP2_X1: {
					final Var e1 = frame.pop();
					final Var e2 = frame.pop();
					final Var e3 = frame.pop();
					frame.push(e2);
					frame.push(e1);
					frame.push(e3);
					frame.push(e2);
					frame.push(e1);
					break;
				}
				case DUP.T_DUP2_X2: {
					final Var e1 = frame.pop();
					final Var e2 = frame.pop();
					final Var e3 = frame.pop();
					final Var e4 = frame.pop();
					frame.push(e2);
					frame.push(e1);
					frame.push(e4);
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

				// TODO check stack has array...
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
				final Var var = getReg(frame, cop.getReg(), cop.getT());
				push(frame, var); // OK
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
				final Var var = pop(frame, cop.getT());
				push(frame, var); // OK
				break;
			}
			case NEW: {
				final NEW cop = (NEW) op;
				push(frame, cop.getT());
				break;
			}
			case NEWARRAY: {
				final NEWARRAY cop = (NEWARRAY) op;
				pop(frame, T.INT); // dimension
				push(frame, T.AREF);
				// TODO to get the real type -> would have to evaluate und check
				// dimension value!!! hmmmm
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
				case POP.T_POP: {
					frame.pop();
					break;
				}
				case POP.T_POP2: {
					frame.pop();
					// should pop 2...add 2 for double/long
					break;
				}
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
				final Var pop = pop(frame, cop.getT());

				// TODO STORE.pc in DALVIK sucks now...multiple ops share pc
				final Var var = this.cfg.getVar(cop.getReg(), ops[this.pc + 1].getPc());

				if (var != null) {
					// TODO
					if (pop.merge(var.getT())) {
						this.queue.add(var.getStartPc());
					}
				}

				setReg(frame, cop.getReg(), var != null ? var : pop);
				break;
			}
			case SUB: {
				final SUB cop = (SUB) op;
				evalBinaryMath(frame, cop.getT());
				break;
			}
			case SWAP: {
				assert op instanceof SWAP;

				final Var e1 = frame.pop();
				final Var e2 = frame.pop();
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