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
 * Operation.
 * 
 * @author André Pankraz
 */
public abstract class Operation {

	private final int opcode;

	private final int line;

	private final int pc;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            original pc
	 * @param opcode
	 *            original operation code
	 * @param line
	 *            line number
	 */
	public Operation(final int pc, final int opcode, final int line) {
		this.pc = pc;
		this.opcode = opcode;
		this.line = line;
	}

	/**
	 * Get input stack size.
	 * 
	 * @return input stack size
	 */
	public int getInStackSize() {
		return getOptype().getInStackSize();
	}

	/**
	 * Get operation line number (if debug info available).
	 * 
	 * @return operation line number
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * Get (if possible original JVM or Dalvik) operation code.
	 * 
	 * @return operation code
	 */
	public int getOpcode() {
		return this.opcode;
	}

	/**
	 * Get operation code.
	 * 
	 * @return operation code
	 */
	public abstract Optype getOptype();

	/**
	 * Get (if possible original) pc.
	 * 
	 * @return pc
	 */
	public int getPc() {
		return this.pc;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}