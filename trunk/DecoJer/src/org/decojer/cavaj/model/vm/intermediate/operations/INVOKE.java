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

import java.util.List;

import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.eclipse.jdt.core.dom.Type;

public class INVOKE extends Operation {

	public static final int T_INTERFACE = 0;

	public static final int T_SPECIAL = 1;

	public static final int T_STATIC = 2;

	public static final int T_VIRTUAL = 3;

	private final int functionType;

	private final String methodrefClassName;

	private final String methodrefName;

	private final List<Type> methodParameterTypes;

	private final Type returnType;

	public INVOKE(final int opPc, final int opcodeJvm, final int lineNumber,
			final int functionType, final String methodrefClassName,
			final String methodrefName, final List<Type> methodParameterTypes,
			final Type returnType) {
		super(opPc, opcodeJvm, lineNumber);
		this.functionType = functionType;
		this.methodrefClassName = methodrefClassName;
		this.methodrefName = methodrefName;
		this.methodParameterTypes = methodParameterTypes;
		this.returnType = returnType;
	}

	public int getFunctionType() {
		return this.functionType;
	}

	@Override
	public int getInStackSize() {
		return (this.functionType == T_STATIC ? 0 : 1)
				+ this.methodParameterTypes.size();
	}

	public List<Type> getMethodParameterTypes() {
		return this.methodParameterTypes;
	}

	public String getMethodrefClassName() {
		return this.methodrefClassName;
	}

	public String getMethodrefName() {
		return this.methodrefName;
	}

	@Override
	public int getOpcode() {
		return Opcode.INVOKE;
	}

	public Type getReturnType() {
		return this.returnType;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getMethodrefName();
	}

}