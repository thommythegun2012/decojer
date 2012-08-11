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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.ClassT;
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
	private MD md;

	@Getter
	private final String name;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private T[] paramTs;

	@Getter
	private final T t;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private T returnT;

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
		this.paramTs = getT().getDu().parseMethodParamTs(descriptor, c);
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
	 * Create method declaration for this method.
	 * 
	 * @return method declaration
	 */
	public MD createMd() {
		assert this.md == null;

		this.md = new MD(this);
		((ClassT) this.t).getTd().addBd(this.md);
		return this.md;
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

	@Override
	public String toString() {
		return this.t + "." + this.name + this.descriptor;
	}

}