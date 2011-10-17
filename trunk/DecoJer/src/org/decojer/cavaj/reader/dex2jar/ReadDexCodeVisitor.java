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
package org.decojer.cavaj.reader.dex2jar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.Var;
import org.decojer.cavaj.model.code.op.FILLARRAY;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.JSR;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.SWITCH;

import com.googlecode.dex2jar.DexLabel;
import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;

/**
 * Dex2jar code visitor.
 * 
 * @author André Pankraz
 */
public class ReadDexCodeVisitor implements DexCodeVisitor {

	private final static Logger LOGGER = Logger.getLogger(ReadDexCodeVisitor.class.getName());

	private final DU du;

	private final ArrayList<Exc> excs = new ArrayList<Exc>();

	private final HashMap<DexLabel, Integer> label2pc = new HashMap<DexLabel, Integer>();

	private final HashMap<DexLabel, ArrayList<Object>> label2unresolved = new HashMap<DexLabel, ArrayList<Object>>();

	private int line = -1;

	private int maxLocals;

	private MD md;

	private final ArrayList<Op> ops = new ArrayList<Op>();

	private final HashMap<Integer, ArrayList<Var>> reg2vars = new HashMap<Integer, ArrayList<Var>>();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexCodeVisitor(final DU du) {
		assert du != null;

		this.du = du;
	}

	private int getPc(final DexLabel label) {
		assert label != null;

		final Integer pc = this.label2pc.get(label);
		if (pc != null) {
			return pc;
		}
		final int unresolvedPc = -1 - this.label2unresolved.size();
		this.label2pc.put(label, unresolvedPc);
		return unresolvedPc;
	}

	private ArrayList<Object> getUnresolved(final DexLabel label) {
		assert label != null;

		ArrayList<Object> unresolved = this.label2unresolved.get(label);
		if (unresolved == null) {
			unresolved = new ArrayList<Object>();
			this.label2unresolved.put(label, unresolved);
		}
		return unresolved;
	}

	/**
	 * Init and set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 */
	public void init(final MD md) {
		this.md = md;
	}

	@Override
	public void visitArguments(final int total, final int[] args) {
		this.maxLocals = total;
		// set varss
	}

	@Override
	public void visitArrayStmt(final int opcode, final int formOrToReg, final int arrayReg,
			final int indexReg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBinopLitXStmt(final int opcode, final int distReg, final int srcReg,
			final int content) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBinopStmt(final int opcode, final int toReg, final int r1, final int r2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitClassStmt(final int opcode, final int a, final int b, final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitClassStmt(final int opcode, final int saveTo, final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCmpStmt(final int opcode, final int distReg, final int bB, final int cC) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitConstStmt(final int opcode, final int toReg, final Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitEnd() {
		if (this.ops.size() > 0) {
			final CFG cfg = new CFG(this.md, this.maxLocals, 0);
			this.md.setCFG(cfg);

			cfg.setOps(this.ops.toArray(new Op[this.ops.size()]));
			this.ops.clear();
			this.label2pc.clear();
			this.label2unresolved.clear();
			this.line = -1;

			if (this.excs.size() > 0) {
				cfg.setExcs(this.excs.toArray(new Exc[this.excs.size()]));
				this.excs.clear();
			}
			if (this.reg2vars.size() > 0) {
				for (final Entry<Integer, ArrayList<Var>> entry : this.reg2vars.entrySet()) {
					final int reg = entry.getKey();
					for (final Var var : entry.getValue()) {
						cfg.addVar(reg, var);
					}
				}
				this.reg2vars.clear();
			}
			cfg.postProcessVars();
		}
	}

	@Override
	public void visitFieldStmt(final int opcode, final int fromOrToReg, final Field field) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitFieldStmt(final int opcode, final int fromOrToReg, final int objReg,
			final Field field) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitFillArrayStmt(final int opcode, final int aA, final int elemWidth,
			final int initLength, final Object[] values) {
		this.ops.add(new LOAD(this.ops.size(), opcode, this.line, T.AREF, aA));

		final FILLARRAY op = new FILLARRAY(this.ops.size(), opcode, this.line);
		this.ops.add(op);
		op.setValues(values);
	}

	@Override
	public void visitFilledNewArrayStmt(final int opcode, final int[] args, final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitJumpStmt(final int opcode, final DexLabel label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitJumpStmt(final int opcode, final int reg, final DexLabel label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitJumpStmt(final int opcode, final int a, final int b, final DexLabel label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLabel(final DexLabel label) {
		final Integer pc = this.label2pc.put(label, this.ops.size());
		if (pc == null) {
			// fresh new label, never referenced before
			return;
		}
		if (pc > 0) {
			// visited before but is known?!
			LOGGER.warning("Label '" + label + "' is not unique, has old PC '" + this.ops.size()
					+ "'!");
			return;
		}
		// unknown and has forward reference
		for (final Object o : this.label2unresolved.get(label)) {
			if (o instanceof GOTO) {
				((GOTO) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof JCMP) {
				((JCMP) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof JCND) {
				((JCND) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof JSR) {
				((JSR) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof SWITCH) {
				final SWITCH op = (SWITCH) o;
				if (pc == op.getDefaultPc()) {
					op.setDefaultPc(this.ops.size());
				}
				final int[] casePcs = op.getCasePcs();
				for (int i = casePcs.length; i-- > 0;) {
					if (pc == casePcs[i]) {
						casePcs[i] = this.ops.size();
					}
				}
				continue;
			}
			if (o instanceof Exc) {
				final Exc op = (Exc) o;
				if (pc == op.getStartPc()) {
					op.setStartPc(this.ops.size());
				}
				if (pc == op.getEndPc()) {
					op.setEndPc(this.ops.size());
				}
				if (pc == op.getHandlerPc()) {
					op.setHandlerPc(this.ops.size());
				}
			}
			if (o instanceof Var) {
				final Var op = (Var) o;
				if (pc == op.getStartPc()) {
					op.setStartPc(this.ops.size());
				}
				if (pc == op.getEndPc()) {
					op.setEndPc(this.ops.size());
				}
			}
		}
	}

	@Override
	public void visitLineNumber(final int line, final DexLabel label) {
		// BUG in Dex2jar: visitLineNumber before visitLabel...not really helpful,
		// should be ordered?! check! TODO could handle this in a more dynamic way

		// final int pc = getPc(label);
		// if (pc < 0) {
		// LOGGER.warning("Line number '" + line + "' start label '" + label + "' unknown yet?");
		// }
		this.line = line;
	}

	@Override
	public void visitLocalVariable(final String name, final String type, final String signature,
			final DexLabel start, final DexLabel end, final int reg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLookupSwitchStmt(final int opcode, final int aA, final DexLabel label,
			final int[] cases, final DexLabel[] labels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMethodStmt(final int opcode, final int[] args, final Method method) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMonitorStmt(final int opcode, final int reg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMoveStmt(final int opcode, final int toReg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMoveStmt(final int opcode, final int toReg, final int fromReg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitReturnStmt(final int opcode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitReturnStmt(final int opcode, final int reg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTableSwitchStmt(final int opcode, final int aA, final DexLabel label,
			final int first_case, final int last_case, final DexLabel[] labels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTryCatch(final DexLabel start, final DexLabel end, final DexLabel handler,
			final String type) {
		// type: Ljava/lang/Exception;
		final T catchT = type == null ? null : this.du.getDescT(type);
		final Exc exc = new Exc(catchT);

		int pc = getPc(start);
		exc.setStartPc(pc);
		if (pc < 0) {
			getUnresolved(start).add(exc);
		}
		pc = getPc(end);
		exc.setEndPc(pc);
		if (pc < 0) {
			getUnresolved(end).add(exc);
		}
		pc = getPc(handler);
		exc.setHandlerPc(pc);
		if (pc < 0) {
			getUnresolved(handler).add(exc);
		}

		this.excs.add(exc);
	}

	@Override
	public void visitUnopStmt(final int opcode, final int toReg, final int fromReg) {
		// TODO Auto-generated method stub

	}

}