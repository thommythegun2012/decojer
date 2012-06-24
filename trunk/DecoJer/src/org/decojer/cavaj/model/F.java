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
package org.decojer.cavaj.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Field.
 * 
 * @author André Pankraz
 */
public class F {

	@Getter
	@Setter
	private int accessFlags;

	@Getter
	private final String name;

	@Getter
	@Setter
	private String signature;

	@Getter
	private final TD t;

	/**
	 * Value type.
	 */
	@Getter
	private final T valueT;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param valueT
	 *            value type
	 */
	protected F(final TD t, final String name, final T valueT) {
		assert t != null;
		assert name != null;
		assert valueT != null;

		this.t = t;
		this.name = name;
		this.valueT = valueT;
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean check(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Mark access flag.
	 * 
	 * @param af
	 *            access flag
	 */
	public void markAf(final AF af) {
		// TODO many more checks
		if (af == AF.ENUM) {
			this.accessFlags = AF.PUBLIC.getValue() | AF.STATIC.getValue() | AF.FINAL.getValue()
					| AF.ENUM.getValue();
			return;
		}
		this.accessFlags |= af.getValue();
	}

	@Override
	public String toString() {
		return getT() + "." + getName();
	}

}