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
 * Type variable (like U, V in Map<U,V>, type arguments consisting of type variables).
 * 
 * @author André Pankraz
 */
public class VarT extends ModT {

	/**
	 * Enclosing type context.
	 */
	@Getter
	private final Object enclosing;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            type name
	 * @param enclosing
	 *            enclosing type context
	 */
	public VarT(final String name, final Object enclosing) {
		super(name, null);

		// TODO add this after we know what happens for:
		// Lorg/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>;
		// assert enclosing != null;

		this.enclosing = enclosing;
	}

	@Override
	public boolean isSignatureFor(final T t) {
		if (getRawT() != null) {
			return getRawT().equals(t);
		}
		setRawT(t);
		return true;
	}

	public void setReducedT(final T t) {
		setRawT(t);
	}

}