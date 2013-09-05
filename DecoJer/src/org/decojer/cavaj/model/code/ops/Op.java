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

import org.decojer.cavaj.model.code.Frame;

/**
 * Operation.
 * 
 * The operation code is the VM Code (Class / Dalvik, so far it's possible with reader abstraction).
 * Line numbers are only available if debug info given. The PC is the operation index, not the VM PC
 * (that is not available with Label-based readers). But the original PC / read order is preserved!
 * 
 * @author André Pankraz
 */
@Getter
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
	 * Get input stack size where wide registers are count as one.
	 * 
	 * @param frame
	 *            operation frame
	 * @return input stack size
	 */
	public int getInStackSize(final Frame frame) {
		return getInStackSize() - frame.wideStacks(getInStackSize());
	}

	/**
	 * Get operation type.
	 * 
	 * @return operation type
	 */
	public abstract Optype getOptype();

	@Override
	public int hashCode() {
		return this.pc;
	}

	/**
	 * Is line information available?
	 * 
	 * @return {@code true} - line information is available
	 */
	public boolean isLineInfo() {
		return this.line >= 0;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}