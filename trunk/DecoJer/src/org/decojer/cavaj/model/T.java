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
 * Type.
 * 
 * @author André Pankraz
 */
public class T {

	private T[] argTs;

	private String descriptor;

	private final DU du;

	private T[] interfaceTs;

	private final String name;

	private T superT;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            name
	 */
	protected T(final DU du, final String name) {
		this.du = du;
		this.name = name;
	}

	/**
	 * Get argument types.
	 * 
	 * @return argument types
	 */
	public T[] getArgTs() {
		if (this.interfaceTs == null) {
			init();
		}
		return this.argTs;
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
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return this.du;
	}

	/**
	 * Get interface types.
	 * 
	 * @return interface types
	 */
	public T[] getInterfaceTs() {
		if (this.interfaceTs == null) {
			init();
		}
		return this.interfaceTs;
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
	 * Get super type.
	 * 
	 * @return super type
	 */
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