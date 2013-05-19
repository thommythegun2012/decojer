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

	@Getter
	// for VarT lazy resolving:
	@Setter(value = AccessLevel.PROTECTED)
	private T rawT;

	protected ModT(final String name, final T rawT) {
		super(name);
		this.rawT = rawT;
	}

	@Override
	public boolean eraseTo(final T t) {
		return getRawT().eraseTo(t);
	}

	@Override
	public DU getDu() {
		return getRawT().getDu();
	}

	@Override
	public T[] getInterfaceTs() {
		return getRawT().getInterfaceTs();
	}

	@Override
	public int getKind() {
		return getRawT().getKind();
	}

	@Override
	public T getSuperT() {
		return getRawT().getSuperT();
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		return getRawT().isAssignableFrom(t);
	}

	@Override
	public boolean isInterface() {
		return getRawT().isInterface();
	}

	@Override
	public boolean isPrimitive() {
		return getRawT().isPrimitive();
	}

	@Override
	public boolean isRef() {
		return getRawT().isRef();
	}

	@Override
	public boolean isUnresolvable() {
		return getRawT().isUnresolvable();
	}

	@Override
	public void setInterface(final boolean f) {
		if (getRawT() != null) {
			getRawT().setInterface(f);
		}
	}

}