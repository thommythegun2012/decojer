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
 * Field.
 * 
 * @author André Pankraz
 */
public class F {

	private int accessFlags;

	private final T fieldT;

	private final String name;

	private String signature;

	private final T t;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * 
	 */
	protected F(final T t, final String name, final T fieldT) {
		assert t != null;
		assert name != null;
		assert fieldT != null;

		this.t = t;
		this.name = name;
		this.fieldT = fieldT;
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean checkAf(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Get access flags.
	 * 
	 * @return access flags
	 */
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/**
	 * Get field type.
	 * 
	 * @return field type
	 */
	public T getFieldT() {
		return this.fieldT;
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
	 * Mark as enum.
	 */
	public void markEnum() {
		this.accessFlags = AF.PUBLIC.getValue() | AF.STATIC.getValue()
				| AF.FINAL.getValue() | AF.ENUM.getValue();
	}

	/**
	 * Set access flags.
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public void setAccessFlags(final int accessFlags) {
		this.accessFlags = accessFlags;
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

}