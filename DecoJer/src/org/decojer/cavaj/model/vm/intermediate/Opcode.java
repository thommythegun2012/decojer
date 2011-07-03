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

public interface Opcode {

	int ADD = 1;

	int ALOAD = 2;

	int AND = 3;

	int ARRAYLENGTH = 4;

	int ASTORE = 5;

	int CHECKCAST = 6;

	int CMP = 7;

	int CONVERT = 8;

	int DIV = 9;

	int DUP = 10;

	int GET = 11;

	int GOTO = 12;

	int INC = 13;

	int INSTANCEOF = 14;

	int INVOKE = 15;

	int JCMP = 16;

	int JCND = 17;

	int LOAD = 18;

	int MONITOR = 19;

	int MUL = 20;

	int NEG = 21;

	int NEW = 22;

	int NEWARRAY = 23;

	int OR = 24;

	int POP = 25;

	int PUSH = 26;

	int PUT = 27;

	int REM = 28;

	int RET = 29;

	int RETURN = 30;

	int STORE = 31;

	int SUB = 32;

	int SWAP = 33;

	int SWITCH = 34;

	int THROW = 35;

	int XOR = 36;

}