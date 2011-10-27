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
package org.decojer.cavaj.model.code;

import org.decojer.cavaj.model.T;

/**
 * Variable.
 * 
 * @author André Pankraz
 */
public class V {

	private int endPc;

	private String name;

	private int startPc;

	private T t;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 */
	public V(final T t) {
		assert t != null;

		this.t = t;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param v
	 *            variable
	 */
	public V(final V v) {
		assert v != null;

		this.t = v.t;
		this.name = v.name;
		this.startPc = v.startPc;
		this.endPc = v.endPc;
	}

	/**
	 * Get emd pc.
	 * 
	 * @return end pc
	 */
	public int getEndPc() {
		return this.endPc;
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
	 * Get start pc.
	 * 
	 * @return start pc
	 */
	public int getStartPc() {
		return this.startPc;
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
	 * Merge this type with given type (target type). Could produce multi-types.
	 * 
	 * @param t
	 *            type
	 * @return true - changed (this)
	 */
	public boolean merge(final T t) {
		final T mergedT = this.t.merge(t);
		if (this.t == mergedT) {
			return false;
		}
		this.t = mergedT;
		return true;
	}

	/**
	 * Merge this type to given type (target type). Can only reduce multi-types (if overridden
	 * method called).
	 * 
	 * @param t
	 *            type
	 * @return true - changed (this)
	 */
	public boolean mergeTo(final T t) {
		final T mergedT = this.t.mergeTo(t);
		if (this.t == mergedT) {
			return false;
		}
		this.t = mergedT;
		return true;
	}

	/**
	 * Set end pc.
	 * 
	 * @param endPc
	 *            end pc
	 */
	public void setEndPc(final int endPc) {
		this.endPc = endPc;
	}

	/**
	 * Set name.
	 * 
	 * @param name
	 *            name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Set start pc.
	 * 
	 * @param startPc
	 *            start pc
	 */
	public void setStartPc(final int startPc) {
		this.startPc = startPc;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("V: ");
		if (this.t != null) {
			sb.append("(").append(this.startPc).append(" - ").append(this.endPc).append(") ")
					.append(this.name).append(": ");
		}
		sb.append(this.t).append(" ");
		return sb.toString();
	}

}