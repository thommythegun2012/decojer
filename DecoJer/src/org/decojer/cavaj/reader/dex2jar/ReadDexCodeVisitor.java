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
import java.util.logging.Logger;

import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.op.FILLARRAY;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.Op;

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

	private final HashMap<DexLabel, Integer> label2pc = new HashMap<DexLabel, Integer>();

	private final HashMap<DexLabel, ArrayList<Object>> label2unresolved = new HashMap<DexLabel, ArrayList<Object>>();

	private int line = -1;

	private MD md;

	private final ArrayList<Op> ops = new ArrayList<Op>();

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
		final CFG cfg = new CFG(this.md, total, 0);
		this.md.setCFG(cfg);
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
		this.md.getCfg().postProcessVars();
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
		op.setValues(values);
		this.ops.add(op);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLineNumber(final int line, final DexLabel label) {
		final int pc = getPc(label);
		if (pc < 0) {
			LOGGER.warning("Line number '" + line + "' start label '" + label + "' unknown yet?");
		}
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
		// TODO Auto-generated method stub

	}

	@Override
	public void visitUnopStmt(final int opcode, final int toReg, final int fromReg) {
		// TODO Auto-generated method stub

	}

}