/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.vm.intermediate.Frame;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.ALOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.AND;
import org.decojer.cavaj.model.vm.intermediate.operations.ARRAYLENGTH;
import org.decojer.cavaj.model.vm.intermediate.operations.ASTORE;
import org.decojer.cavaj.model.vm.intermediate.operations.CAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.DUP;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;
import org.decojer.cavaj.model.vm.intermediate.operations.INC;
import org.decojer.cavaj.model.vm.intermediate.operations.INSTANCEOF;
import org.decojer.cavaj.model.vm.intermediate.operations.INVOKE;
import org.decojer.cavaj.model.vm.intermediate.operations.JCMP;
import org.decojer.cavaj.model.vm.intermediate.operations.JCND;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.MONITOR;
import org.decojer.cavaj.model.vm.intermediate.operations.MUL;
import org.decojer.cavaj.model.vm.intermediate.operations.NEG;
import org.decojer.cavaj.model.vm.intermediate.operations.NEW;
import org.decojer.cavaj.model.vm.intermediate.operations.NEWARRAY;
import org.decojer.cavaj.model.vm.intermediate.operations.OR;
import org.decojer.cavaj.model.vm.intermediate.operations.POP;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.PUT;
import org.decojer.cavaj.model.vm.intermediate.operations.REM;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.SHL;
import org.decojer.cavaj.model.vm.intermediate.operations.SHR;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWAP;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;

/**
 * Transform Data Flow Analysis.
 * 
 * @author Andr� Pankraz
 */
public class TrDataFlowAnalysis {

	private final static Logger LOGGER = Logger
			.getLogger(TrDataFlowAnalysis.class.getName());

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
			if (cfg == null) {
				continue;
			}
			transform(cfg);
		}
	}

	private final CFG cfg;

	private Frame[] frames;

	private Operation[] ops;

	private final LinkedList<Integer> queue = new LinkedList<Integer>();

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

	private CFG getCfg() {
		return this.cfg;
	}

	private void merge(final Frame frame, final int targetPc) {
		final Frame targetFrame = this.frames[targetPc];
		if (targetFrame == null) {
			this.frames[targetPc] = frame;
		} else if (!targetFrame.merge(frame)) {
			return;
		}
		this.queue.add(targetPc);
	}

	private Var pop(final T t, final Frame frame) {
		final Var var = frame.pop();
		if (var.merge(t)) {
			// TODO check should not pe necessary
			if (var.getStartPc() > 0) {
				this.queue.add(var.getStartPc());
			}
		}
		return var;
	}

	private void transform() {
		this.ops = this.cfg.getOperations();
		this.frames = new Frame[this.ops.length];
		this.frames[0] = createMethodFrame();

		this.queue.clear();
		this.queue.add(0);
		while (!this.queue.isEmpty()) {
			final int pc = this.queue.removeFirst();
			// frame will be modified and forward merged through operation
			final Frame frame = new Frame(this.frames[pc]);
			final Operation operation = this.ops[pc];
			switch (operation.getOpcode()) {
			case Opcode.ADD: {
				final ADD op = (ADD) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.ALOAD: {
				final ALOAD op = (ALOAD) operation;
				frame.push(new Var(op.getT()));
				break;
			}
			case Opcode.AND: {
				final AND op = (AND) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.ARRAYLENGTH: {
				final ARRAYLENGTH op = (ARRAYLENGTH) operation;
				frame.pop();
				frame.push(new Var(T.INT));
				break;
			}
			case Opcode.ASTORE: {
				final ASTORE op = (ASTORE) operation;
				frame.pop();
				frame.pop();
				frame.pop();
				break;
			}
			case Opcode.CAST: {
				final CAST op = (CAST) operation;
				frame.pop();
				frame.push(new Var(op.getToT()));
				break;
			}
			case Opcode.CMP: {
				final CMP op = (CMP) operation;
				frame.pop();
				frame.pop();
				frame.push(new Var(T.INT));
				break;
			}
			case Opcode.DIV: {
				final DIV op = (DIV) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.DUP: {
				final DUP op = (DUP) operation;
				switch (op.getDupType()) {
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
					LOGGER.warning("Unknown dup type '" + op.getDupType()
							+ "'!");
				}
			}
				break;
			case Opcode.GET: {
				final GET op = (GET) operation;
				final F f = op.getF();
				if (!f.checkAf(AF.STATIC)) {
					frame.pop();
				}
				frame.push(new Var(f.getValueT()));
				break;
			}
			case Opcode.GOTO: {
				final GOTO op = (GOTO) operation;
				merge(frame, op.getTargetPc());
				continue;
			}
			case Opcode.INC: {
				final INC op = (INC) operation;
				// no Bool!
				break;
			}
			case Opcode.INSTANCEOF: {
				final INSTANCEOF op = (INSTANCEOF) operation;
				frame.pop();
				frame.push(new Var(T.BOOLEAN));
				break;
			}
			case Opcode.INVOKE: {
				final INVOKE op = (INVOKE) operation;
				final M m = op.getM();
				if (!m.checkAf(AF.STATIC)) {
					frame.pop();
				}
				for (int i = m.getParamTs().length; i-- > 0;) {
					frame.pop();
				}
				if (m.getReturnT() != T.VOID) {
					frame.push(new Var(m.getReturnT()));
				}
				break;
			}
			case Opcode.JCMP: {
				final JCMP op = (JCMP) operation;
				frame.pop();
				frame.pop();
				merge(frame, op.getTargetPc());
				break;
			}
			case Opcode.JCND: {
				final JCND op = (JCND) operation;
				frame.pop();
				merge(frame, op.getTargetPc());
				break;
			}
			case Opcode.LOAD: {
				final LOAD op = (LOAD) operation;
				final Var var = frame.getReg(op.getVarIndex());
				var.merge(op.getT());
				frame.push(var);
				break;
			}
			case Opcode.MONITOR: {
				final MONITOR op = (MONITOR) operation;
				frame.pop();
				break;
			}
			case Opcode.MUL: {
				final MUL op = (MUL) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.NEG: {
				final NEG op = (NEG) operation;
				// opFrame = new Frame(opFrame);
				// no BOOL
				break;
			}
			case Opcode.NEW: {
				final NEW op = (NEW) operation;
				frame.push(new Var(op.getT()));
				break;
			}
			case Opcode.NEWARRAY: {
				final NEWARRAY op = (NEWARRAY) operation;
				frame.pop(); // no BOOL
				frame.push(new Var(op.getT())); // add dimension!
				break;
			}
			case Opcode.OR: {
				final OR op = (OR) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.POP: {
				final POP op = (POP) operation;
				switch (op.getPopType()) {
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
					LOGGER.warning("Unknown pop type '" + op.getPopType()
							+ "'!");
				}
				break;
			}
			case Opcode.PUSH: {
				final PUSH op = (PUSH) operation;
				frame.push(new Var(op.getT()));
				break;
			}
			case Opcode.PUT: {
				final PUT op = (PUT) operation;
				final F f = op.getF();
				frame.pop();
				if (!f.checkAf(AF.STATIC)) {
					frame.pop();
				}
				break;
			}
			case Opcode.REM: {
				final REM op = (REM) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.RETURN: {
				final RETURN op = (RETURN) operation;
				if (op.getT() != T.VOID) {
					frame.pop();
				}
				continue;
			}
			case Opcode.SHL: {
				final SHL op = (SHL) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.SHR: {
				final SHR op = (SHR) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.STORE: {
				final STORE op = (STORE) operation;
				final Var pop = frame.pop();

				final int reg = op.getVarIndex();
				final Var var = this.cfg.getVar(reg, pc + 1);

				if (var != null) {
					// TODO
					if (pop.merge(var.getT())) {
						this.queue.add(var.getStartPc());
					}
				}

				frame.setReg(op.getVarIndex(), var != null ? var : pop);
				break;
			}
			case Opcode.SUB: {
				final SUB op = (SUB) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			case Opcode.SWAP: {
				final SWAP op = (SWAP) operation;
				final Var e1 = frame.pop();
				final Var e2 = frame.pop();
				frame.push(e1);
				frame.push(e2);
				break;
			}
			case Opcode.SWITCH: {
				final SWITCH op = (SWITCH) operation;
				frame.pop();
				merge(frame, op.getDefaultPc());
				for (final int casePc : op.getCasePcs()) {
					merge(frame, casePc);
				}
				continue;
			}
			case Opcode.THROW: {
				final THROW op = (THROW) operation;
				frame.pop();
				continue;
			}
			case Opcode.XOR: {
				final XOR op = (XOR) operation;
				frame.push(Var.merge(frame.pop(), frame.pop()));
				break;
			}
			default:
				LOGGER.warning("Operation '" + operation + "' not handled!");
			}
			merge(frame, pc + 1);
		}

		this.cfg.setFrames(this.frames);
	}

}