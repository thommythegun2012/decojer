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
package org.decojer.cavaj.model.type;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Array type.
 * 
 * @author André Pankraz
 */
@Getter
public class ArrayT extends T {

	/**
	 * Component type (could be an array type, has one dimension less).
	 */
	private final T componentT;

	/**
	 * Constructor.
	 * 
	 * @param componentT
	 *            component type
	 * @param arraySuperT
	 *            array super type, should be <code>Object</code>
	 * @param arrayInterfaceTs
	 *            array interface types, should be <code>Cloneable, Serializable</code>
	 */
	public ArrayT(final T componentT, final T arraySuperT, final T[] arrayInterfaceTs) {
		super(arraySuperT.getDu(), componentT.getName() + "[]");

		this.componentT = componentT;
		setSuperT(arraySuperT);
		setInterfaceTs(arrayInterfaceTs);
	}

	@Override
	public boolean isArray() {
		return true;
	}

}