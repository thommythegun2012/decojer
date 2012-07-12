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

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.utils.Cursor;

/**
 * Method.
 * 
 * @author André Pankraz
 */
public class M {

	@Setter
	private int accessFlags;

	@Getter
	private final String descriptor;

	@Getter
	private final String name;

	@Getter
	T[] paramTs;

	@Getter
	private final T t;

	@Getter
	T returnT;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	protected M(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.descriptor = descriptor;

		final Cursor c = new Cursor();
		this.paramTs = parseMethodParamTs(descriptor, c);
		this.returnT = getT().getDu().parseT(descriptor, c);
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
		this.accessFlags |= af.getValue();
	}

	/**
	 * Parse method parameter types from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return method parameter types
	 */
	T[] parseMethodParamTs(final String s, final Cursor c) {
		assert s.charAt(c.pos) == '(';

		++c.pos;
		final ArrayList<T> ts = new ArrayList<T>();
		while (s.charAt(c.pos) != ')') {
			ts.add(getT().getDu().parseT(s, c));
		}
		++c.pos;
		return ts.toArray(new T[ts.size()]);
	}

	@Override
	public String toString() {
		return getT() + "." + getName() + getDescriptor();
	}

}