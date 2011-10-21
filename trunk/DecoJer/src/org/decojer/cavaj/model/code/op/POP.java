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

/**
 * Operation 'POP'.
 * 
 * @author André Pankraz
 */
public class POP extends Op {

	public static final int T_POP = 0;

	public static final int T_POP2 = 1;

	private final int popType;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param popType
	 *            pop type
	 */
	public POP(final int pc, final int opcode, final int line, final int popType) {
		super(pc, opcode, line);
		this.popType = popType;
	}

	@Override
	public int getInStackSize() {
		// TODO? should be 2 for not-wide stack arguments, but we cannot know here...
		return 1;
	}

	@Override
	public Optype getOptype() {
		return Optype.POP;
	}

	/**
	 * Get pop type.
	 * 
	 * @return pop type
	 */
	public int getPopType() {
		return this.popType;
	}

}