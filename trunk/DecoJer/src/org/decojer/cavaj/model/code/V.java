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

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.T;

/**
 * Variable.
 * 
 * @author André Pankraz
 */
public class V {

	@Getter
	@Setter
	private String name;

	/**
	 * Start and end PCs.
	 */
	@Getter
	private int[] pcs;

	@Getter
	private T t;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param startPc
	 *            start pc
	 * @param endPc
	 *            end pc
	 */
	public V(final T t, final String name, final int startPc, final int endPc) {
		assert t != null;
		// assert startPc >= 0 : startPc;
		// assert endPc == -1 || endPc >= startPc : endPc;

		this.t = t;
		this.name = name;
		this.pcs = new int[] { startPc, endPc };
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
		this.pcs = v.pcs; // TODO copy?
	}

	/**
	 * Add pc range.
	 * 
	 * @param startPc
	 *            start pc
	 * @param endPc
	 *            end pc
	 */
	public void addPcs(final int startPc, final int endPc) {
		assert startPc >= 0 : startPc;
		assert endPc == -1 || endPc >= startPc : endPc;

		int p = this.pcs.length;
		final int[] pcs = new int[p + 2];
		System.arraycopy(this.pcs, 0, pcs, 0, p);
		pcs[p++] = startPc;
		pcs[p] = endPc;
		this.pcs = pcs;
	}

	/**
	 * Set type.
	 * 
	 * @param t
	 *            type
	 * @return true - changed
	 */
	public boolean cmpSetT(final T t) {
		if (this.t == t) {
			return false;
		}
		this.t = t;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("V: ");
		if (this.t != null) {
			sb.append("(");
			for (int i = 0;;) {
				sb.append(this.pcs[i++]).append(" - ").append(this.pcs[i++]);
				if (i == this.pcs.length) {
					break;
				}
				sb.append(", ");
			}
			sb.append(") ");

			sb.append(this.name).append(": ");
		}
		sb.append(this.t).append(" ");
		return sb.toString();
	}

	/**
	 * Is variable valid for pc?
	 * 
	 * @param pc
	 *            pc
	 * @return true - variable valid for pc
	 */
	public boolean validIn(final int pc) {
		for (int i = 0; i < this.pcs.length;) {
			// end pc is first pc _after_ (multiple byte) operation (even after final return)
			if (this.pcs[i++] <= pc && pc < this.pcs[i++]) {
				return true;
			}
		}
		return false;
	}

}