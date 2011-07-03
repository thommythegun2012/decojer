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

public interface DataType {

	int T_VOID = 0;
	int T_AREF = 1;
	int T_STRING = 2;
	int T_CLASS = 3;
	// see vmspec: atype in newarray
	int T_BOOLEAN = 4;
	int T_CHAR = 5;
	int T_FLOAT = 6;
	int T_DOUBLE = 7;
	int T_BYTE = 8;
	int T_SHORT = 9;
	int T_INT = 10;
	int T_LONG = 11;

}