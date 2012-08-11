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

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;

/**
 * Array type.
 * 
 * @author André Pankraz
 */
@Getter
public final class ArrayT extends T {

	/**
	 * Component type (could be an array type too, has one dimension less).
	 */
	@Getter
	private final T componentT;

	@Getter
	private final DU du;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 * @param componentT
	 *            component type
	 */
	public ArrayT(final DU du, final T componentT) {
		super(componentT.getName() + "[]");

		this.componentT = componentT;
		this.du = du;
	}

	@Override
	public T[] getInterfaceTs() {
		return getDu().getArrayInterfaceTs();
	}

	@Override
	public T getSuperT() {
		return getDu().getT(Object.class);
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		// for t == REF, faster solution without this?!
		if (super.isAssignableFrom(t)) {
			return true;
		}
		return getComponentT().isAssignableFrom(t.getComponentT()); // assign from null is false
	}

}