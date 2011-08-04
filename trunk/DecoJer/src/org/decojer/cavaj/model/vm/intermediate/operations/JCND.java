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

import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;

public class JCND extends Operation {

	private final int type;

	private final int cmpType;

	public JCND(final int opPc, final int opCode, final int opLine,
			final int type, final int cmpType) {
		super(opPc, opCode, opLine);
		this.type = type;
		this.cmpType = cmpType;
	}

	public int getCmpType() {
		return this.cmpType;
	}

	@Override
	public int getInStackSize() {
		return 1;
	}

	@Override
	public int getOpcode() {
		return Opcode.JCND;
	}

	public int getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return super.toString() + " " + CompareType.NAME[this.cmpType];
	}

}