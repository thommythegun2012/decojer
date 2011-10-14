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

public enum Opcode {

	ADD(2),

	ALOAD(2),

	AND(2),

	ARRAYLENGTH(1),

	ASTORE(3),

	CAST(1),

	CMP(2),

	DIV(2),

	DUP(-1),

	FILLARRAY(1),

	GET(-1),

	GOTO(0),

	INC(0),

	INSTANCEOF(1),

	INVOKE(-1),

	JCMP(2),

	JCND(1),

	JSR(0),

	LOAD(0),

	MONITOR(1),

	MUL(2),

	NEG(1),

	NEW(0),

	NEWARRAY(-1),

	OR(2),

	POP(-1),

	PUSH(0),

	PUT(-1),

	REM(2),

	RET(1),

	RETURN(-1),

	SHL(2),

	SHR(2),

	STORE(1),

	SUB(2),

	SWAP(2),

	SWITCH(1),

	THROW(1),

	XOR(2);

	private final int inStackSize;

	/**
	 * Constructor.
	 * 
	 */
	private Opcode(final int inStackSize) {
		this.inStackSize = inStackSize;
	}

	/**
	 * Get input stack size.
	 * 
	 * @return input stack size
	 */
	public int getInStackSize() {
		return this.inStackSize;
	}

}