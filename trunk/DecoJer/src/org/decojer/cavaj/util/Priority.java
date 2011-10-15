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
package org.decojer.cavaj.util;

/**
 * Expression priority.
 * 
 * @author André Pankraz
 */
public enum Priority {

	LITERAL(0),

	ARRAY_INDEX(1),

	METHOD_CALL(1),

	MEMBER_ACCESS(1),

	PREFIX_OR_POSTFIX(2),

	TYPE_CAST(2),

	MULT_DIV(3),

	ADD_SUB(4),

	SHIFT(5),

	LESS_OR_GREATER(6),

	INSTANCEOF(6),

	EQUALS(7),

	AND(8),

	XOR(9),

	OR(10),

	CONDITIONAL_AND(11),

	CONDITIONAL_OR(12),

	CONDITIONAL(13),

	ASSIGNMENT(14);

	private final int priority;

	/**
	 * Constructor.
	 * 
	 */
	private Priority(final int priority) {
		this.priority = priority;
	}

	/**
	 * Get priority.
	 * 
	 * @return priority
	 */
	public int getPriority() {
		return this.priority;
	}

}