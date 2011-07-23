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

import java.util.HashMap;
import java.util.Map;

/**
 * Type.
 * 
 * @author André Pankraz
 */
public class T {

	private static final Map<String, T> ts = new HashMap<String, T>();

	/**
	 * Init type.
	 * 
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 * @return type
	 */
	public static T initT(final String name, final String descriptor) {
		final T t = new T(name);
		t.setDescriptor(descriptor);
		return t;
	}

	private String descriptor;

	private T[] interfaceTs;

	private final String name;

	private T superT;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            name
	 */
	protected T(final String name) {
		this.name = name;
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

	public T getSuperT() {
		if (this.interfaceTs == null) {
			init();
		}
		return this.superT;
	}

	private void init() {
		// parse and init

	}

	/**
	 * Set descriptor.
	 * 
	 * @param descriptor
	 *            descriptor
	 */
	public void setDescriptor(final String descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public String toString() {
		return getName() + ": " + getDescriptor();
	}

}