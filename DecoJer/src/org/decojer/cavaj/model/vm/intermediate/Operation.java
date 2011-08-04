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
package org.decojer.cavaj.model.vm.intermediate;

public abstract class Operation {

	private final int opCode;

	private final int opLine;

	private final int opPc;

	public Operation(final int opPc, final int opCode, final int opLine) {
		this.opPc = opPc;
		this.opCode = opCode;
		this.opLine = opLine;
	}

	@Override
	public boolean equals(final Object ob) {
		return ob instanceof Operation
				&& getOpPc() == ((Operation) ob).getOpPc(); // super.equals(arg0);
	}

	public abstract int getInStackSize();

	public abstract int getOpcode();

	public int getOpCode() {
		return this.opCode;
	}

	public int getOpLine() {
		return this.opLine;
	}

	public int getOpPc() {
		return this.opPc;
	}

	@Override
	public int hashCode() {
		return getOpPc(); // super.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}