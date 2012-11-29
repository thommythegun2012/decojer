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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;

/**
 * Modifying type.
 * 
 * @author André Pankraz
 */
public abstract class ModT extends T {

	@Getter(value = AccessLevel.PROTECTED)
	// for VarT lazy resolving:
	@Setter(value = AccessLevel.PROTECTED)
	private T rawT;

	protected ModT(final String name, final T rawT) {
		super(name);
		this.rawT = rawT;
	}

	@Override
	public DU getDu() {
		return this.rawT.getDu();
	}

	@Override
	public T[] getInterfaceTs() {
		return this.rawT.getInterfaceTs();
	}

	@Override
	public int getKind() {
		return this.rawT.getKind();
	}

	@Override
	public T getSuperT() {
		return this.rawT.getSuperT();
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		return this.rawT.isAssignableFrom(t);
	}

	@Override
	public boolean isInterface() {
		return this.rawT.isInterface();
	}

	@Override
	public boolean isPrimitive() {
		return this.rawT.isPrimitive();
	}

	@Override
	public boolean isRef() {
		return this.rawT.isRef();
	}

	@Override
	public boolean isResolvable() {
		return this.rawT.isResolvable();
	}

	@Override
	public boolean isSignatureFor(final T t) {
		return this.rawT.isSignatureFor(t);
	}

}