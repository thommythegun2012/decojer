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

import java.util.Map;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Primitive type (normal primitives and artificial / internal VM types).
 * 
 * @author André Pankraz
 */
public class PrimitiveT extends T {

	@Getter
	private final String name;

	@Getter
	private final int kind;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            type name
	 * @param kind
	 *            type kind
	 */
	public PrimitiveT(final String name, final int kind) {
		assert name != null;

		this.name = name;
		this.kind = kind;
	}

	@Override
	public T[] getInterfaceTs() {
		return INTERFACES_NONE;
	}

	@Override
	public Map<String, Object> getMember() {
		return null;
	}

	@Override
	public int getStackSize() {
		if (this.kind == Kind.VOID.getKind()) {
			return 0;
		}
		return isWide() ? 2 : 1;
	}

	@Override
	public T getSuperT() {
		return null;
	}

	@Override
	public boolean isMulti() {
		int nr = getKind() - (getKind() >> 1 & 0x55555555);
		nr = (nr & 0x33333333) + (nr >> 2 & 0x33333333);
		nr = (nr + (nr >> 4) & 0x0F0F0F0F) * 0x01010101 >> 24;
		assert nr > 0;

		return nr > 1;
	}

	@Override
	public boolean isPrimitive() {
		// not always {@code true} - consider REF/RET multitypes
		return (getKind() & PRIMITIVE.getKind()) != 0;
	}

	@Override
	public boolean isRef() {
		// not always false - consider REF/RET multitypes
		return (getKind() & REF.getKind()) != 0;
	}

	@Override
	public boolean isUnresolvable() {
		return false;
	}

	/**
	 * Is wide type?
	 * 
	 * @return {@code true} - is wide type
	 */
	@Override
	public boolean isWide() {
		return (getKind() & WIDE.getKind()) != 0;
	}

}