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

import org.decojer.cavaj.util.Cursor;

/**
 * Method.
 * 
 * @author André Pankraz
 */
public class M {

	@Getter
	@Setter
	private int accessFlags;

	@Getter
	private final String descriptor;

	@Getter
	private final String name;

	private String[] paramNames;

	private final T[] paramTs;

	@Getter
	private final T returnT;

	@Getter
	private String signature;

	@Getter
	private T[] typeParams;

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
		this.paramTs = t.getDu().parseMethodParamTs(descriptor, c);
		this.returnT = t.getDu().parseT(descriptor, c);
	}

	@Getter
	private final T t;

	/**
	 * Throw types or null.
	 */
	@Getter
	@Setter
	private T[] throwsTs;

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
	 * Get parameter name for index.
	 * 
	 * @param i
	 *            index (starts with 0, double/long params count as 1)
	 * @return parameter name
	 */
	public String getParamName(final int i) {
		if (this.paramNames == null || i >= this.paramNames.length || this.paramNames[i] == null) {
			return "arg" + i;
		}
		return this.paramNames[i];
	}

	/**
	 * Get parameter number.
	 * 
	 * @return parameter number
	 */
	public int getParams() {
		return this.paramTs.length;
	}

	/**
	 * Get parameter type for index.
	 * 
	 * @param i
	 *            index (starts with 0, double/long params count as 1)
	 * @return parameter type
	 */
	public T getParamT(final int i) {
		return this.paramTs[i];
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
	 * Set parameter name.
	 * 
	 * @param i
	 *            index
	 * @param name
	 *            parameter name
	 */
	public void setParamName(final int i, final String name) {
		if (this.paramNames == null) {
			this.paramNames = new String[this.paramTs.length];
		}
		this.paramNames[i] = name;
	}

	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		this.signature = signature;

		final Cursor c = new Cursor();
		this.typeParams = this.t.getDu().parseTypeParams(signature, c);
		final T[] paramTs = this.t.getDu().parseMethodParamTs(signature, c);
		final T returnT = this.t.getDu().parseT(signature, c);

		if (this.returnT != returnT) {
			System.out.println("RETURN: " + returnT);
		}
	}

	@Override
	public String toString() {
		return getT() + "." + getName() + getDescriptor();
	}

}