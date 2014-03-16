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
import lombok.Setter;

/**
 * Wildcard type.
 * 
 * This type is used as type argument, but other types can be type arguments too.
 * 
 * @author André Pankraz
 */
public class WildcardT extends BaseT {

	@Getter
	private final boolean subclassOf;

	@Getter
	@Setter
	private T boundT;

	/**
	 * Constructor.
	 * 
	 * @param boundT
	 *            bound type
	 * @param subclass
	 *            is subclass (extends)
	 */
	public WildcardT(final T boundT, final boolean subclass) {
		setBoundT(boundT);
		this.subclassOf = subclass;
	}

	@Override
	public T[] getInterfaceTs() {
		if (isSubclassOf()) {
			final T boundT = getBoundT();
			assert boundT != null;
			if (boundT.isInterface()) {
				return new T[] { boundT };
			}
		}
		return null;
	}

	@Override
	public String getName() {
		final T boundT = getBoundT();
		if (boundT == null) {
			return "?";
		}
		if (isSubclassOf()) {
			return "? extends " + boundT.getFullName();
		}
		return "? super " + boundT.getFullName();
	}

	@Override
	public T getSuperT() {
		if (isSubclassOf()) {
			final T boundT = getBoundT();
			assert boundT != null;
			if (!boundT.isInterface()) {
				return boundT;
			}
		}
		return null;
	}

	@Override
	public boolean isWildcard() {
		return true;
	}

}