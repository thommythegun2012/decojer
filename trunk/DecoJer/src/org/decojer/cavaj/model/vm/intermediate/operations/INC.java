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

public class INC extends Operation {

	private final int constValue;

	private final T t;

	private final int varIndex;

	public INC(final int pc, final int code, final int line, final T t,
			final int varIndex, final int constValue) {
		super(pc, code, line);
		this.t = t;
		this.varIndex = varIndex;
		this.constValue = constValue;
	}

	public int getConstValue() {
		return this.constValue;
	}

	@Override
	public int getInStackSize() {
		return 0;
	}

	@Override
	public int getOpcode() {
		return Opcode.INC;
	}

	public T getT() {
		return this.t;
	}

	public int getVarIndex() {
		return this.varIndex;
	}

}