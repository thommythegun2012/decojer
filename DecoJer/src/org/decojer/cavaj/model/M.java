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

import org.decojer.DecoJerException;

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
	@Setter
	private String signature;

	@Getter
	private final T t;

	/**
	 * Throw types or null.
	 */
	@Getter
	@Setter
	private T[] throwsTs;

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

		if (descriptor.charAt(0) != '(') {
			throw new DecoJerException("Method descriptor must start with '('!");
		}
		final int endPos = descriptor.indexOf(')', 1);
		if (endPos == -1) {
			throw new DecoJerException("Method descriptor must contain ')'!");
		}
		final ArrayList<T> paramTs = new ArrayList<T>();
		final DU du = t.getDu();
		for (int pos = 1; pos < endPos;) {
			final T paramT = du.getDescT(descriptor.substring(pos));
			pos += paramT.getDescriptorLength();
			paramTs.add(paramT);
		}
		this.paramTs = paramTs.toArray(new T[paramTs.size()]);
		this.returnT = du.getDescT(descriptor.substring(endPos + 1));
	}

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 * @param returnT
	 *            return type
	 * @param paramTs
	 *            parameter types
	 */
	public M(final T t, final String name, final String descriptor, final T returnT,
			final T[] paramTs) {
		assert t != null;
		assert name != null;
		assert descriptor != null;
		assert returnT != null;
		assert paramTs != null;

		this.t = t;
		this.name = name;
		this.descriptor = descriptor;
		this.returnT = returnT;
		this.paramTs = paramTs;
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

	@Override
	public String toString() {
		return getT() + "." + getName() + getDescriptor();
	}

}