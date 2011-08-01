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

/**
 * Method.
 * 
 * @author André Pankraz
 */
public class M {

	private final String descriptor;

	private final String name;

	private String[] paramNames;

	private final T[] paramTs;

	private final T returnT;

	private String signature;

	private final T t;

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
	public M(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.descriptor = descriptor;
		this.returnT = null;
		this.paramTs = null;
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
	 * @param throwsTs
	 *            throws types
	 * @param signature
	 *            signature
	 */
	public M(final T t, final String name, final String descriptor,
			final T returnT, final T[] paramTs) {
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
	 * Get descriptor.
	 * 
	 * @return descriptor
	 */
	public String getDescriptor() {
		return this.descriptor;
	}

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get parameter name for index.
	 * 
	 * @param i
	 *            index
	 * @return parameter name
	 */
	public String getParamName(final int i) {
		if (this.paramNames == null || i >= this.paramNames.length
				|| this.paramNames[i] == null) {
			return "arg" + i;
		}
		return this.paramNames[i];
	}

	/**
	 * Get parameter types.
	 * 
	 * @return parameter types
	 */
	public T[] getParamTs() {
		return this.paramTs;
	}

	/**
	 * Get return type.
	 * 
	 * @return return type
	 */
	public T getReturnT() {
		return this.returnT;
	}

	/**
	 * Get signature.
	 * 
	 * @return signature or null
	 */
	public String getSignature() {
		return this.signature;
	}

	/**
	 * Get type.
	 * 
	 * @return type
	 */
	public T getT() {
		return this.t;
	}

	/**
	 * Get throws types.
	 * 
	 * @return throws types
	 */
	public T[] getThrowsTs() {
		return this.throwsTs;
	}

	/**
	 * Set parameter names.
	 * 
	 * @param paramNames
	 *            parameter names
	 */
	public void setParamNames(final String[] paramNames) {
		this.paramNames = paramNames;
	}

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public void setSignature(final String signature) {
		this.signature = signature;
	}

	/**
	 * Set throws types.
	 * 
	 * @param throwsTs
	 *            throws types
	 */
	public void setThrowsTs(final T[] throwsTs) {
		this.throwsTs = throwsTs;
	}

}