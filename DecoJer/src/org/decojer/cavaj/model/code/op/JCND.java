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
 * Operation 'JCND'.
 * 
 * @author André Pankraz
 */
public class JCND extends TypedOp {

	private final CmpType cmpType;

	private int targetPc;

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
	public JCND(final int pc, final int opcode, final int line, final T t, final CmpType cmpType) {
		super(pc, opcode, line, t);

		assert cmpType != null;

		this.cmpType = cmpType;
	}

	/**
	 * Get compare type.
	 * 
	 * @return compare type
	 */
	public CmpType getCmpType() {
		return this.cmpType;
	}

	@Override
	public int getInStackSize() {
		assert !getT().isWide();

		return 1;
	}

	@Override
	public Optype getOptype() {
		return Optype.JCND;
	}

	/**
	 * Get target pc.
	 * 
	 * @return target pc
	 */
	public int getTargetPc() {
		// pc can temporarily be negative because of reading unknown labels
		// assert targetPc >= 0 : targetPc;

		return this.targetPc;
	}

	/**
	 * Set target pc.
	 * 
	 * @param targetPc
	 *            target pc
	 */
	public void setTargetPc(final int targetPc) {
		// pc can temporarily be negative because of reading unknown labels
		// assert targetPc >= 0 : targetPc;

		this.targetPc = targetPc;
	}

	@Override
	public String toString() {
		return super.toString() + " " + this.cmpType + " " + this.targetPc;
	}

}