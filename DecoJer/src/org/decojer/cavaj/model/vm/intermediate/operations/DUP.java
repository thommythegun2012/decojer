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
package org.decojer.cavaj.model.vm.intermediate.operations;

import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;

public class DUP extends Operation {

	public static final int T_DUP = 0;

	public static final int T_DUP_X1 = 1;

	public static final int T_DUP_X2 = 2;

	public static final int T_DUP2 = 3;

	public static final int T_DUP2_X1 = 4;

	public static final int T_DUP2_X2 = 5;

	private final int dupType;

	public DUP(final int pc, final int code, final int line, final int dupType) {
		super(pc, code, line);
		this.dupType = dupType;
	}

	public int getDupType() {
		return this.dupType;
	}

	@Override
	public int getInStackSize() {
		return new int[] { 1, 2, 3, 2, 3, 4 }[this.dupType];
	}

	@Override
	public int getOpcode() {
		return Opcode.DUP;
	}

	@Override
	public String toString() {
		return super.toString()
				+ new String[] { "", "_X1", "_X2", "2", "2_X1", "2_X2" }[this.dupType];
	}

}