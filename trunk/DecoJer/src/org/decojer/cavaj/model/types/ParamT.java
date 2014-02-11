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
 * Type parameter.
 * 
 * @author André Pankraz
 */
public class ParamT extends T {

	@Getter
	private final String name;

	@Getter
	private final DU du;

	/**
	 * Interface types.
	 */
	private T[] interfaceTs;

	/**
	 * Super type.
	 */
	@Getter
	private T superT;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 * @param name
	 *            type name
	 */
	public ParamT(final DU du, final String name) {
		assert du != null;
		assert name != null;

		this.du = du;
		this.name = name;
	}

	@Override
	public T[] getInterfaceTs() {
		if (this.interfaceTs == null) {
			return INTERFACES_NONE;
		}
		return this.interfaceTs;
	}

	@Override
	public void setInterfaceTs(final T[] interfaceTs) {
		for (final T t : interfaceTs) {
			t.setInterface(true);
		}
		this.interfaceTs = interfaceTs;
	}

	@Override
	public void setSuperT(final T superT) {
		if (superT == null) {
			this.superT = NONE;
			return;
		}
		superT.setInterface(false);
		this.superT = superT;
	}

}