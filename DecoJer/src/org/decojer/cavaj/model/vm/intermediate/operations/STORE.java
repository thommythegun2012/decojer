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

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;

public class STORE extends Operation {

	private final int type;

	private final int varIndex;

	private final String varName;

	private final T varT;

	public STORE(final int opPc, final int opcodeJvm, final int lineNumber,
			final int type, final int varIndex, final String varName,
			final T varT) {
		super(opPc, opcodeJvm, lineNumber);
		this.type = type;
		this.varIndex = varIndex;
		this.varName = varName;
		this.varT = varT;
	}

	@Override
	public int getInStackSize() {
		return 1;
	}

	@Override
	public int getOpcode() {
		return Opcode.STORE;
	}

	public int getType() {
		return this.type;
	}

	public int getVarIndex() {
		return this.varIndex;
	}

	public String getVarName() {
		return this.varName;
	}

	public T getVarT() {
		return this.varT;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getVarIndex();
	}

}