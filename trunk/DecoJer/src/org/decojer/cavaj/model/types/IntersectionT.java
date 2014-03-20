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
 * Intersection type.
 *
 * @see T#intersect(T, T)
 *
 * @author André Pankraz
 */
public class IntersectionT extends BaseT {

	/**
	 * Interface types.
	 */
	@Getter
	private final T[] interfaceTs;

	/**
	 * Super type.
	 */
	@Getter
	private final T superT;

	/**
	 * Constructor for anonymous multi class type.
	 *
	 * @param superT
	 *            super type
	 * @param interfaceTs
	 *            interface types
	 */
	public IntersectionT(final T superT, final T... interfaceTs) {
		this.superT = superT;
		this.interfaceTs = interfaceTs;
	}

	@Override
	public String getName() {
		final StringBuilder sb = new StringBuilder("{");
		if (this.superT != null) {
			sb.append(this.superT.getName()).append(',');
		}
		for (final T interfaceT : this.interfaceTs) {
			sb.append(interfaceT.getName()).append(",");
		}
		sb.setCharAt(sb.length() - 1, '}');
		final String ret = sb.toString();
		assert ret != null;
		return ret;
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		final T superT = getSuperT();
		if (superT != null && !superT.isAssignableFrom(t)) {
			return false;
		}
		for (final T interfactT : getInterfaceTs()) {
			if (!interfactT.isAssignableFrom(t)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isIntersection() {
		return true;
	}

}