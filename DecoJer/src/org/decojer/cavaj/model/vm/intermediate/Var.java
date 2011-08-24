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
package org.decojer.cavaj.model.vm.intermediate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.decojer.cavaj.model.T;

/**
 * Variable.
 * 
 * @author André Pankraz
 */
public class Var {

	/**
	 * Merge variables.
	 * 
	 * @param vars
	 *            variables
	 * @return variable
	 */
	public static Var merge(final Var... vars) {
		final HashSet<T> ts = new HashSet<T>();
		ts.addAll(vars[0].ts);
		return new Var(ts);
	}

	private int endPc;

	private String name;

	private int startPc;

	private Set<T> ts = new HashSet<T>();

	/**
	 * Constructor.
	 * 
	 * @param ts
	 *            types
	 */
	public Var(final Set<T> ts) {
		this.ts = ts;
	}

	/**
	 * Constructor.
	 * 
	 * @param ts
	 *            types
	 */
	public Var(final T... ts) {
		this.ts.addAll(Arrays.asList(ts));
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
	 * Get types.
	 * 
	 * @return types
	 */
	public Set<T> getTs() {
		return this.ts;
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
		final StringBuilder sb = new StringBuilder("Var");
		sb.append("(").append(this.startPc).append(" - ").append(this.endPc)
				.append(") ");
		sb.append(this.name).append(": ");
		for (final T t : this.ts) {
			sb.append(t).append(" ");
		}
		return sb.toString();
	}

}