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

/**
 * Operation.
 * 
 * The operation code is the VM Code (Class / Dalvik, so far it's possible with reader abstraction).
 * Line numbers are only available if debug info given. The PC is the operation index, not the VM PC
 * (that is not available with Label-based readers). But the original PC / read order is preserved!
 * 
 * @author André Pankraz
 */
public abstract class Op {

	final int line;

	private final int opcode;

	private final int pc;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number, -1 for no numbers
	 */
	public Op(final int pc, final int opcode, final int line) {
		assert pc >= 0 : pc;
		assert opcode >= 0 && opcode <= 255 : opcode;
		assert line >= -1 : line;

		this.pc = pc;
		this.opcode = opcode;
		this.line = line;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Op && this.pc == ((Op) obj).pc;
	}

	/**
	 * Get input stack size.
	 * 
	 * @return input stack size
	 */
	public abstract int getInStackSize();

	/**
	 * Get line number.
	 * 
	 * @return line number, -1 for no numbers
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * Get operation code.
	 * 
	 * @return operation code
	 */
	public int getOpcode() {
		return this.opcode;
	}

	/**
	 * Get operation type.
	 * 
	 * @return operation type
	 */
	public abstract Optype getOptype();

	/**
	 * Get pc.
	 * 
	 * @return pc
	 */
	public int getPc() {
		return this.pc;
	}

	@Override
	public int hashCode() {
		return this.pc;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}