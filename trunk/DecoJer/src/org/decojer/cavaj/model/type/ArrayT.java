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

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;

/**
 * Array Type.
 * 
 * @author André Pankraz
 */
@Getter
public final class ArrayT extends T {

	/**
	 * Component Type (could be an Array Type too, has one dimension less).
	 */
	private final T componentT;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            Decompilation Unit
	 * @param componentT
	 *            Component Type
	 */
	public ArrayT(final DU du, final T componentT) {
		super(du, componentT.getName() + "[]");

		this.componentT = componentT;
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
	public boolean isResolveable() {
		return true;
	}

}