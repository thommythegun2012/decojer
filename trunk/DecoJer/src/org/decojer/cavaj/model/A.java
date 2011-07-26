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

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Annotation.
 * 
 * @author André Pankraz
 */
public class A {

	private final LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();

	private final T t;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 */
	public A(final T t) {
		this.t = t;
	}

	/**
	 * Add parameter.
	 * 
	 * @param name
	 *            name
	 * @param value
	 *            value
	 */
	public void addParameter(final String name, final Object value) {
		this.params.put(name, value);
	}

	/**
	 * Get parameter.
	 * 
	 * @param name
	 *            name
	 * @return value
	 */
	public Object getMemberValue(final String name) {
		return this.params.get(name);
	}

	/**
	 * Get parameter names.
	 * 
	 * @return parameter names
	 */
	public Set<String> getMemberNames() {
		return this.params.keySet();
	}

	/**
	 * Get type.
	 * 
	 * @return types
	 */
	public T getT() {
		return this.t;
	}

}