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
 * Operation 'CAST'.
 * 
 * @author André Pankraz
 */
public class CAST extends TypedOp {

	@Getter
	private final T toT;

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
	 * @param toT
	 *            to type
	 */
	public CAST(final int pc, final int opcode, final int line, final T t, final T toT) {
		super(pc, opcode, line, t);

		assert toT != null;

		this.toT = toT;
	}

	@Override
	public int getInStackSize() {
		return getT().isWide() ? 2 : 1;
	}

	@Override
	public Optype getOptype() {
		return Optype.CAST;
	}

}