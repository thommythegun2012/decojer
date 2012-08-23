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
package org.decojer.cavaj.model.types;

import lombok.Getter;

/**
 * Kind of type.
 * 
 * @author André Pankraz
 */
public enum Kind {

	INT(1 << 0, int.class),

	SHORT(1 << 1, short.class),

	BYTE(1 << 2, byte.class),

	CHAR(1 << 3, char.class),

	BOOLEAN(1 << 4, boolean.class),

	FLOAT(1 << 5, float.class),

	LONG(1 << 6, long.class),

	DOUBLE(1 << 7, double.class),

	VOID(1 << 8, void.class),

	REF(1 << 9),

	RET(1 << 10),

	LONG2(1 << 11),

	DOUBLE2(1 << 12),

	NONE(1 << 13);

	@Getter
	private final Class<?> clazz;

	@Getter
	private final int kind;

	private Kind(final int flag) {
		this(flag, null);
	}

	private Kind(final int flag, final Class<?> clazz) {
		this.clazz = clazz;
		this.kind = flag;
	}

	public String getName() {
		return this.clazz == null ? name() : this.clazz.getName();
	}

}