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

import org.decojer.cavaj.model.T;

/**
 * @author André Pankraz
 */
public class WildcardT extends ModT {

	public static WildcardT matches() {
		return new WildcardT(null, false);
	}

	public static WildcardT subclassOf(final T t) {
		assert t != null;

		return new WildcardT(t, true);
	}

	public static WildcardT superOf(final T t) {
		assert t != null;

		return new WildcardT(t, false);
	}

	@Getter
	private final boolean subclassOf;

	/**
	 * Constructor.
	 * 
	 * @param boundT
	 *            bound type
	 * @param subclass
	 *            is subclass (extends)
	 */
	protected WildcardT(final T boundT, final boolean subclass) {
		super(boundT);

		this.subclassOf = subclass;
	}

	@Override
	public T getBoundT() {
		return getRawT();
	}

	@Override
	public String getFullName() {
		if (getBoundT() == null) {
			return "?";
		}
		if (isSubclassOf()) {
			return "? extends " + getBoundT().getFullName();
		}
		return "? super " + getBoundT().getFullName();
	}

	@Override
	public boolean isWildcard() {
		return true;
	}

	@Override
	public void setBoundT(final T boundT) {
		setRawT(boundT);
	}

}