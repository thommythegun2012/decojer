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

	private final DU du;

	private T[] interfaceTs;

	private final String name;

	private String signature;

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
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return this.du;
	}

	/**
	 * Get inner name.
	 * 
	 * @return inner name
	 */
	public String getIName() {
		final String pName = getPName();
		final int pos = pName.lastIndexOf('$');
		if (pos == -1) {
			return pName;
		}
		try {
			final int parseInt = Integer.parseInt(pName.substring(pos + 1));
			return "I_" + parseInt;
		} catch (final NumberFormatException e) {
			return pName.substring(pos + 1);
		}
	}

	/**
	 * Get interface types.
	 * 
	 * @return interface types
	 */
	public T[] getInterfaceTs() {
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
	 * Get package name.
	 * 
	 * @return package name
	 */
	public String getPackageName() {
		final int pos = getName().lastIndexOf('.');
		return pos == -1 ? "" : getName().substring(0, pos);
	}

	/**
	 * Get primary name.
	 * 
	 * @return primary name
	 */
	public String getPName() {
		final int pos = getName().lastIndexOf('.');
		return pos == -1 ? getName() : getName().substring(pos + 1);
	}

	/**
	 * Get signature.
	 * 
	 * @return signature
	 */
	public String getSignature() {
		return this.signature;
	}

	/**
	 * Get super type.
	 * 
	 * @return super type
	 */
	public T getSuperT() {
		return this.superT;
	}

	/**
	 * Set interface types.
	 * 
	 * @param interfaceTs
	 *            interface types
	 */
	public void setInterfaceTs(final T[] interfaceTs) {
		this.interfaceTs = interfaceTs;
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
	 * Set super type.
	 * 
	 * @param superT
	 *            super type
	 */
	public void setSuperT(final T superT) {
		this.superT = superT;
	}

	@Override
	public String toString() {
		return getName();
	}

}