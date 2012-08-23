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
package org.decojer.cavaj.model.code.ops;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Operation 'STORE'.
 * 
 * @author André Pankraz
 */
public class STORE extends TypedOp {

	@Getter
	private final int reg;

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
	 * @param reg
	 *            register
	 */
	public STORE(final int pc, final int opcode, final int line, final T t, final int reg) {
		super(pc, opcode, line, t);

		assert reg >= 0 : reg;

		this.reg = reg;
	}

	@Override
	public int getInStackSize() {
		return getT().getStackSize();
	}

	@Override
	public Optype getOptype() {
		return Optype.STORE;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getReg();
	}

}