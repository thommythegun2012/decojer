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
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BB;
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
import org.decojer.cavaj.model.vm.intermediate.operations.CHECKCAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
import org.decojer.cavaj.model.vm.intermediate.operations.CONVERT;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.DUP;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
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
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWAP;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;

/**
 * Transform Data Flow Analysis.
 * 
 * @author André Pankraz
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

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private Frame createMethodFrame() {
		final Frame frame = new Frame();
		frame.stack = new Stack<Var>();
		frame.vars = new Var[this.cfg.getMaxRegs()];
		for (int i = frame.vars.length; i-- > 0;) {
			frame.vars[i] = this.cfg.getVar(i, 0);
		}
		return frame;
	}

	private CFG getCfg() {
		return this.cfg;
	}

	private Frame propagateFrames(final BB bb, final Frame frame) {
		Frame opFrame = frame;
		int nextOpPc = bb.getOpPc();
		for (final Operation operation : bb.getOperations()) {
			this.frames[nextOpPc++] = opFrame;
			switch (operation.getOpcode()) {
			case Opcode.ADD: {
				final ADD op = (ADD) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.ALOAD: {
				final ALOAD op = (ALOAD) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(new Var(op.getT()));
				break;
			}
			case Opcode.AND: {
				final AND op = (AND) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.ARRAYLENGTH: {
				final ARRAYLENGTH op = (ARRAYLENGTH) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.push(new Var(T.INT));
				break;
			}
			case Opcode.ASTORE: {
				final ASTORE op = (ASTORE) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.pop();
				opFrame.stack.pop();
				break;
			}
			case Opcode.CHECKCAST: {
				final CHECKCAST op = (CHECKCAST) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.push(new Var(op.getT()));
				break;
			}
			case Opcode.CMP: {
				final CMP op = (CMP) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.pop();
				opFrame.stack.push(new Var(T.INT));
				break;
			}
			case Opcode.CONVERT: {
				final CONVERT op = (CONVERT) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.push(new Var(op.getToT()));
				break;
			}
			case Opcode.DIV: {
				final DIV op = (DIV) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.DUP: {
				final DUP op = (DUP) operation;
				opFrame = new Frame(opFrame);
				switch (op.getDupType()) {
				case DUP.T_DUP:
					opFrame.stack.push(opFrame.stack.peek());
					break;
				case DUP.T_DUP_X1: {
					final Var e1 = opFrame.stack.pop();
					final Var e2 = opFrame.stack.pop();
					opFrame.stack.push(e1);
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					break;
				}
				case DUP.T_DUP_X2: {
					final Var e1 = opFrame.stack.pop();
					final Var e2 = opFrame.stack.pop();
					final Var e3 = opFrame.stack.pop();
					opFrame.stack.push(e1);
					opFrame.stack.push(e3);
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					break;
				}
				case DUP.T_DUP2: {
					final Var e1 = opFrame.stack.pop();
					final Var e2 = opFrame.stack.pop();
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					break;
				}
				case DUP.T_DUP2_X1: {
					final Var e1 = opFrame.stack.pop();
					final Var e2 = opFrame.stack.pop();
					final Var e3 = opFrame.stack.pop();
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					opFrame.stack.push(e3);
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					break;
				}
				case DUP.T_DUP2_X2: {
					final Var e1 = opFrame.stack.pop();
					final Var e2 = opFrame.stack.pop();
					final Var e3 = opFrame.stack.pop();
					final Var e4 = opFrame.stack.pop();
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
					opFrame.stack.push(e4);
					opFrame.stack.push(e3);
					opFrame.stack.push(e2);
					opFrame.stack.push(e1);
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
				opFrame = new Frame(opFrame);
				final F f = op.getF();
				if (!f.checkAf(AF.STATIC)) {
					opFrame.stack.pop();
				}
				opFrame.stack.push(new Var(f.getValueT()));
				break;
			}
			case Opcode.INC: {
				final INC op = (INC) operation;
				// no Bool!
				break;
			}
			case Opcode.INSTANCEOF: {
				final INSTANCEOF op = (INSTANCEOF) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.push(new Var(T.BOOLEAN));
				break;
			}
			case Opcode.INVOKE: {
				final INVOKE op = (INVOKE) operation;
				opFrame = new Frame(opFrame);
				final M m = op.getM();
				if (!m.checkAf(AF.STATIC)) {
					opFrame.stack.pop();
				}
				for (int i = m.getParamTs().length; i-- > 0;) {
					opFrame.stack.pop();
				}
				if (m.getReturnT() != T.VOID) {
					opFrame.stack.push(new Var(m.getReturnT()));
				}
				break;
			}
			case Opcode.JCMP: {
				final JCMP op = (JCMP) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				opFrame.stack.pop();
				break;
			}
			case Opcode.JCND: {
				final JCND op = (JCND) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				break;
			}
			case Opcode.LOAD: {
				final LOAD op = (LOAD) operation;
				opFrame = new Frame(opFrame);
				final Var var = opFrame.vars[op.getVarIndex()];
				var.merge(op.getT());
				opFrame.stack.push(var);
				break;
			}
			case Opcode.MONITOR: {
				final MONITOR op = (MONITOR) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				break;
			}
			case Opcode.MUL: {
				final MUL op = (MUL) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
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
				opFrame.stack.push(new Var(op.getT()));
				break;
			}
			case Opcode.NEWARRAY: {
				final NEWARRAY op = (NEWARRAY) operation;
				opFrame.stack.pop(); // no BOOL
				opFrame.stack.push(new Var(op.getT())); // add dimension!
				break;
			}
			case Opcode.OR: {
				final OR op = (OR) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.POP: {
				final POP op = (POP) operation;
				opFrame = new Frame(opFrame);
				switch (op.getPopType()) {
				case POP.T_POP: {
					opFrame.stack.pop();
					break;
				}
				case POP.T_POP2: {
					opFrame.stack.pop();
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
				opFrame = new Frame(opFrame);
				opFrame.stack.push(new Var(op.getT()));
				break;
			}
			case Opcode.PUT: {
				final PUT op = (PUT) operation;
				final F f = op.getF();
				opFrame.stack.pop();
				if (!f.checkAf(AF.STATIC)) {
					opFrame.stack.pop();
				}
				break;
			}
			case Opcode.REM: {
				final REM op = (REM) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.RETURN: {
				final RETURN op = (RETURN) operation;
				if (op.getT() != T.VOID) {
					opFrame = new Frame(opFrame);
					opFrame.stack.pop();
				}
				break;
			}
			case Opcode.STORE: {
				final STORE op = (STORE) operation;
				opFrame = new Frame(opFrame);
				final Var pop = opFrame.stack.pop();

				final int reg = op.getVarIndex();
				final Var var = this.cfg.getVar(reg, nextOpPc);

				opFrame.vars[op.getVarIndex()] = var != null ? var : pop;
				break;
			}
			case Opcode.SUB: {
				final SUB op = (SUB) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.SWAP: {
				final SWAP op = (SWAP) operation;
				opFrame = new Frame(opFrame);
				final Var e1 = opFrame.stack.pop();
				final Var e2 = opFrame.stack.pop();
				opFrame.stack.push(e1);
				opFrame.stack.push(e2);
				break;
			}
			case Opcode.SWITCH: {
				final SWITCH op = (SWITCH) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				break;
			}
			case Opcode.THROW: {
				final THROW op = (THROW) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.pop();
				break;
			}
			case Opcode.XOR: {
				final XOR op = (XOR) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			default:
				if (operation.getInStackSize() > 0) {
					LOGGER.warning("Operation '" + operation
							+ "' with stacksize not handled!");
				}
			}
		}
		return opFrame;
	}

	private void transform() {
		Frame frame = createMethodFrame();
		this.frames = new Frame[this.cfg.getOperations().length];

		final List<BB> bbs = this.cfg.getPostorderedBbs();
		for (int postorder = bbs.size(); postorder-- > 0;) {
			final BB bb = bbs.get(postorder);
			frame = propagateFrames(bb, frame);
		}
		this.cfg.setFrames(this.frames);
	}

}