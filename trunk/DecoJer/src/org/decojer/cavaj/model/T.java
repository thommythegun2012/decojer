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

/**
 * Type.
 * 
 * @author André Pankraz
 */
public class T {

	public static class TT extends T {

		public T[] ts;

		protected TT(final T... ts) {
			super("<multi>");
			this.ts = ts;
		}

	}

	/**
	 * Any reference type.
	 */
	public static T AREF = new T("<aref>");
	/**
	 * Primitive type boolean.
	 */
	public static T BOOLEAN = new T(boolean.class.getName());
	/**
	 * Primitive type byte.
	 */
	public static T BYTE = new T(byte.class.getName());
	/**
	 * Primitive type char.
	 */
	public static T CHAR = new T(char.class.getName());
	/**
	 * Primitive type double.
	 */
	public static T DOUBLE = new T(double.class.getName());
	/**
	 * Primitive type float.
	 */
	public static T FLOAT = new T(float.class.getName());
	/**
	 * Primitive type int.
	 */
	public static T INT = new T(int.class.getName());
	/**
	 * Primitive type long.
	 */
	public static T LONG = new T(long.class.getName());
	/**
	 * Primitive type short.
	 */
	public static T SHORT = new T(short.class.getName());
	/**
	 * Artificial type 'uninit'.
	 */
	public static T UNINIT = new T("<uninit>");

	/**
	 * Primitive type void.
	 */
	public static T VOID = new T(void.class.getName());

	public static TT multi(final T... ts) {
		return new TT(ts);
	}

	private int accessFlags;

	private int dim;

	private final DU du;

	private final HashMap<String, F> fs = new HashMap<String, F>();

	private T[] interfaceTs;

	private final HashMap<String, M> ms = new HashMap<String, M>();

	private final String name;

	private String signature;

	private T superT;

	protected T(final DU du, final String name) {
		assert du != null;
		assert name != null;

		this.du = du;
		this.name = name;
	}

	private T(final String name) {
		assert name != null;

		this.du = null; // primitive
		this.name = name;
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
	 * Get base type.
	 * 
	 * @return base type
	 */
	public T getBaseT() {
		if (isArray()) {
			return this.superT;
		}
		return this;
	}

	/**
	 * Get descriptor length.
	 * 
	 * @return descriptor length
	 */
	public int getDescriptorLength() {
		if (isPrimitive()) {
			return 1;
		}
		if (isArray()) {
			return getBaseT().getDescriptorLength() + this.dim;
		}
		return getName().length() + 2; // L...;
	}

	/**
	 * Get dimension.
	 * 
	 * @return dimension
	 */
	public int getDim() {
		return this.dim;
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
	 * Get field.
	 * 
	 * @param name
	 *            name
	 * @param valueT
	 *            value type
	 * @return field
	 */
	public F getF(final String name, final T valueT) {
		F f = this.fs.get(name);
		if (f == null) {
			f = new F(this, name, valueT);
			this.fs.put(name, f);
		}
		return f;
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
		return Character.isJavaIdentifierStart(pName.charAt(pos + 1)) ? pName
				.substring(pos + 1) : "I_" + pName.substring(pos + 1);
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
	 * Get method.
	 * 
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 * @return method
	 */
	public M getM(final String name, final String descriptor) {
		M m = this.ms.get(name + descriptor);
		if (m == null) {
			m = new M(this, name, descriptor);
			this.ms.put(name + descriptor, m);
		}
		return m;
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
		if (isArray()) {
			return this.du.getT(Object.class.getName());
		}
		return this.superT;
	}

	/**
	 * Is array?
	 * 
	 * @return true - is array
	 */
	public boolean isArray() {
		return this.dim > 0;
	}

	/**
	 * Is primitive?
	 * 
	 * @return true - is primitive
	 */
	public boolean isPrimitive() {
		return this.du == null;
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
	 * Set access flags
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public void setAccessFlags(final int accessFlags) {
		this.accessFlags = accessFlags;
	}

	/**
	 * Set dimension.
	 * 
	 * @param dim
	 *            dimension
	 */
	public void setDim(final int dim) {
		this.dim = dim;
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