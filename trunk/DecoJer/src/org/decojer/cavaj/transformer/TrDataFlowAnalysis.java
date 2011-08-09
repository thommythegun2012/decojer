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

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.vm.intermediate.DataType;
import org.decojer.cavaj.model.vm.intermediate.Frame;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;

/**
 * Transform Data Flow Analysis.
 * 
 * @author André Pankraz
 */
public class TrDataFlowAnalysis {

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

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private T convertType(final int dataType) {
		switch (dataType) {
		case DataType.T_AREF:
			return getCfg().getMd().getTd().getT().getDu()
					.getT(Object.class.getName());
		case DataType.T_BOOLEAN:
			return T.BOOLEAN;
		case DataType.T_BYTE:
			return T.BYTE;
		case DataType.T_CHAR:
			return T.CHAR;
		case DataType.T_CLASS:
			return getCfg().getMd().getTd().getT().getDu()
					.getT(Class.class.getName());
		case DataType.T_DOUBLE:
			return T.DOUBLE;
		case DataType.T_FLOAT:
			return T.FLOAT;
		case DataType.T_INT:
			return T.INT;
		case DataType.T_LONG:
			return T.LONG;
		case DataType.T_SHORT:
			return T.SHORT;
		case DataType.T_STRING:
			return getCfg().getMd().getTd().getT().getDu()
					.getT(String.class.getName());
		case DataType.T_VOID:
			return T.VOID;
		}
		return null;
	}

	private Frame createMethodFrame() {
		final MD md = getCfg().getMd();
		final M m = md.getM();
		final TD td = md.getTd();
		final T t = td.getT();
		final Frame frame = new Frame();
		frame.stack = new Stack<Var>();
		frame.vars = new Var[this.cfg.getRegisterCount()];
		frame.varNames = new String[this.cfg.getRegisterCount()];
		final T[] paramTs = m.getParamTs();
		if (td.getVersion() == 0) {
			// Dalvik...function parameters right aligned
			int reg = this.cfg.getRegisterCount();
			for (int i = paramTs.length; i-- > 0;) {
				frame.vars[--reg] = new Var(paramTs[i]);
				frame.varNames[reg] = m.getParamName(i);
			}
			if (!m.checkAf(AF.STATIC)) {
				frame.vars[--reg] = new Var(t);
				frame.varNames[reg] = "this";
			}
			while (reg > 0) {
				frame.vars[--reg] = new Var(T.UNINIT);
				frame.varNames[reg] = "r" + reg;
			}
		} else {
			// JVM...function parameters left aligned
			int reg = 0;
			if (!m.checkAf(AF.STATIC)) {
				frame.vars[reg] = new Var(t);
				frame.varNames[reg++] = "this";
			}
			for (int i = 0; i < paramTs.length; ++i) {
				final T paramT = paramTs[i];
				frame.vars[reg] = new Var(paramT);
				frame.varNames[reg++] = m.getParamName(i);
				// wide values need 2 registers, srsly?
				if (paramT == T.LONG || paramT == T.DOUBLE) {
					// TODO better mark as unuseable?
					frame.vars[reg++] = new Var(T.UNINIT);
				}
			}
			while (reg < this.cfg.getRegisterCount()) {
				frame.vars[reg] = new Var(T.UNINIT);
				frame.varNames[reg++] = "r" + reg;
			}
		}
		return frame;
	}

	private CFG getCfg() {
		return this.cfg;
	}

	private Frame propagateFrames(final BB bb, final Frame frame) {
		Frame opFrame = frame;
		for (final Operation operation : bb.getOperations()) {
			operation.setFrame(opFrame);
			switch (operation.getOpcode()) {
			case Opcode.ADD: {
				final ADD op = (ADD) operation;
				opFrame = new Frame(opFrame);
				// check op.getType()
				opFrame.stack.push(Var.merge(opFrame.stack.pop(),
						opFrame.stack.pop()));
				break;
			}
			case Opcode.GET: {
				final GET op = (GET) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(new Var(op.getF().getValueT()));
				break;
			}
			case Opcode.LOAD: {
				final LOAD op = (LOAD) operation;
				opFrame = new Frame(opFrame);
				// check op.getType()
				final Var var = opFrame.vars[op.getVarIndex()];
				opFrame.stack.push(var);
				break;
			}
			case Opcode.PUSH: {
				final PUSH op = (PUSH) operation;
				opFrame = new Frame(opFrame);
				opFrame.stack.push(new Var(convertType(op.getType())));
				break;
			}
			case Opcode.STORE: {
				final STORE op = (STORE) operation;
				opFrame = new Frame(opFrame);
				opFrame.vars[op.getVarIndex()] = opFrame.stack.pop();
				// check follow?
				break;
			}
			}
		}
		return frame;
	}

	private void transform() {
		Frame frame = createMethodFrame();

		final List<BB> bbs = this.cfg.getPostorderedBbs();
		for (int postorder = bbs.size(); postorder-- > 0;) {
			final BB bb = bbs.get(postorder);
			frame = propagateFrames(bb, frame);
		}
	}

}