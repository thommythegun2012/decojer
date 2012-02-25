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
package org.decojer.cavaj.model.code.op;

import org.decojer.cavaj.model.T;

/**
 * Operation 'CMP'.
 * 
 * @author André Pankraz
 */
public class CMP extends TypedOp {

	public static final int T_0 = 0;

	public static final int T_G = 1;

	public static final int T_L = 2;

	private final int cmpType;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param t
	 *            type
	 * @param cmpType
	 *            compare type
	 */
	public CMP(final int pc, final int opcode, final int line, final T t, final int cmpType) {
		super(pc, opcode, line, t);

		assert 0 <= cmpType && cmpType <= 2 : cmpType;

		this.cmpType = cmpType;
	}

	/**
	 * Get compare type.
	 * 
	 * @return compare type
	 */
	public int getCmpType() {
		return this.cmpType;
	}

	@Override
	public int getInStackSize() {
		return getT().isWide() ? 4 : 2;
	}

	@Override
	public Optype getOptype() {
		return Optype.CMP;
	}

}