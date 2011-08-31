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

import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.objectweb.asm.Label;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;

/**
 * Read DEX code visitor.
 * 
 * @author André Pankraz
 */
public class ReadDexCodeVisitor implements DexCodeVisitor {

	private MD md;

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
	public void visitArrayStmt(final int opcode, final int formOrToReg,
			final int arrayReg, final int indexReg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBinopLitXStmt(final int opcode, final int distReg,
			final int srcReg, final int content) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitBinopStmt(final int opcode, final int toReg, final int r1,
			final int r2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitClassStmt(final int opcode, final int a, final int b,
			final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitClassStmt(final int opcode, final int saveTo,
			final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCmpStmt(final int opcode, final int distReg, final int bB,
			final int cC) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitConstStmt(final int opcode, final int toReg,
			final Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitEnd() {
		this.md.getCfg().postProcessVars();
	}

	@Override
	public void visitFieldStmt(final int opcode, final int fromOrToReg,
			final Field field) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitFieldStmt(final int opcode, final int fromOrToReg,
			final int objReg, final Field field) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitFillArrayStmt(final int opcode, final int aA,
			final int elemWidth, final int initLength, final Object[] values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitFilledNewArrayStmt(final int opcode, final int[] args,
			final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitJumpStmt(final int opcode, final int a, final int b,
			final Label label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitJumpStmt(final int opcode, final int reg, final Label label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitJumpStmt(final int opcode, final Label label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLabel(final Label label) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLookupSwitchStmt(final int opcode, final int aA,
			final Label label, final int[] cases, final Label[] labels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitMethodStmt(final int opcode, final int[] args,
			final Method method) {
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
	public void visitMoveStmt(final int opcode, final int toReg,
			final int fromReg) {
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
	public void visitTableSwitchStmt(final int opcode, final int aA,
			final Label label, final int first_case, final int last_case,
			final Label[] labels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTryCatch(final Label start, final Label end,
			final Label handler, final String type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitUnopStmt(final int opcode, final int toReg,
			final int fromReg) {
		// TODO Auto-generated method stub

	}

}