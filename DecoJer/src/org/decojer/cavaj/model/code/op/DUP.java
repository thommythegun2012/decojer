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
 * Operation 'DUP'.
 * 
 * @author André Pankraz
 */
public class DUP extends Op {

	/**
	 * Dup kind.
	 * 
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * Duplicate the top operand stack value.
		 */
		DUP,

		/**
		 * Duplicate the top operand stack value and insert two values down.
		 */
		DUP_X1,

		/**
		 * Duplicate the top operand stack value and insert two or three values down.
		 */
		DUP_X2,

		/**
		 * Duplicate the top one or two operand stack values:<br>
		 * <code>..., value2, value1 => ..., value2,
		 * value1, value2, value1</code><br>
		 * wide:<br>
		 * <code>..., value => ..., value, value</code>
		 */
		DUP2,

		/**
		 * Duplicate the top one or two operand stack values and insert two or three values down:<br>
		 * <code>..., value3, value2, value1 => ..., value2, value1, value3, value2, value1</code><br>
		 * wide:<br>
		 * <code>..., value2, value1 => ..., value1, value2, value1</code>
		 */
		DUP2_X1,

		/**
		 * Duplicate the top one or two operand stack values and insert two, three, or four values
		 * down:<br>
		 * <code>..., value4, value3, value2, value1 => ..., value2, value1, value4,
		 * value3, value2, value1</code><br>
		 * wide:<br>
		 * <code>..., value3, value2, value1 => ..., value1, value3,
		 * value2, value1</code>
		 */
		DUP2_X2

	}

	private final Kind kind;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param kind
	 *            dup kind
	 */
	public DUP(final int pc, final int opcode, final int line, final Kind kind) {
		super(pc, opcode, line);
		this.kind = kind;
	}

	@Override
	public int getInStackSize() {
		// TODO? should be 2, 3, 4 for not-wide stack arguments, but we cannot know here...
		// return new int[] { 1, 2, 3, 1, 2, 3 }[this.dupType];
		// TODO change stack to 2 for wide!!!
		return 1;
	}

	/**
	 * Get dup type.
	 * 
	 * @return dup type
	 */
	public Kind getKind() {
		return this.kind;
	}

	@Override
	public Optype getOptype() {
		return Optype.DUP;
	}

	@Override
	public String toString() {
		return this.kind.toString();
	}

}