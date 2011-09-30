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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Type.
 * 
 * @author André Pankraz
 */
public class T {

	/**
	 * Multi-type.
	 * 
	 * @author André Pankraz
	 */
	public static class TT extends T {

		private static String concat(final T[] ts) {
			final StringBuilder sb = new StringBuilder("<");
			for (final T t : ts) {
				sb.append(t).append(" ");
			}
			sb.append(">");
			return sb.toString();
		}

		private final T[] ts;

		protected TT(final T... ts) {
			super(concat(ts));
			this.ts = ts;
		}

		/**
		 * Get types.
		 * 
		 * @return types
		 */
		public T[] getTs() {
			return this.ts;
		}

		@Override
		public T merge(final T t) {
			if (t == this || t == null) {
				return this;
			}
			final LinkedHashSet<T> merged = new LinkedHashSet<T>();
			for (final T st : this.ts) {
				final T mt = t.merge(st); // t might be multi too
				if (mt == T.BOGUS) {
					continue;
				}
				if (mt instanceof TT) {
					for (final T smt : ((TT) mt).getTs()) {
						merged.add(smt);
					}
					continue;
				}
				merged.add(mt);
			}
			final int s = merged.size();
			if (s == 0) {
				return T.BOGUS;
			}
			if (s == 1) {
				return merged.iterator().next();
			}
			// TODO equals old?
			return new TT(merged.toArray(new T[s]));
		}

		@Override
		public T mergeTo(final T t) {
			return merge(t);
		}

	}

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
	 * Primitive type void.
	 */
	public static T VOID = new T(void.class.getName());
	/**
	 * Artificial type 'any reference'.
	 */
	public static T AREF = new T("<aref>");
	/**
	 * Artificial type 'uninit'.
	 */
	public static T UNINIT = new T("<uninit>");
	/**
	 * Artificial type 'bogus'.
	 */
	public static T BOGUS = new T("<bogus>");
	/**
	 * Multi-type 'any int (32 bit)'.
	 */
	public static T AINT = multi(BOOLEAN, CHAR, BYTE, SHORT, INT);
	/**
	 * Multi-type 'dalvik int (32 bit)', includes float.
	 */
	public static T DINT = multi(BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT);
	/**
	 * Multi-type 'any wide (64 bit)'.
	 */
	public static T WIDE = multi(DOUBLE, LONG);

	private static final T[] NO_INTERFACES = new T[0];

	/**
	 * Create multi-type.
	 * 
	 * @param ts
	 *            types
	 * @return multi-type
	 */
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
		if (this.interfaceTs == null) {
			if (isArray()) {
				// TODO could set / cache this in du...
				final T[] interfaceTs = new T[2];
				interfaceTs[0] = this.du.getT(Cloneable.class.getName());
				interfaceTs[1] = this.du.getT(Serializable.class.getName());
				return interfaceTs;
			}
			init();
		}
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
		if (this.superT == null) {
			if (Object.class.getName().equals(getName())) {
				return null;
			}
			init();
		} else if (isArray()) {
			return this.du.getT(Object.class.getName());
		}
		return this.superT;
	}

	private void init() {
		if (this.interfaceTs == NO_INTERFACES) {
			return;
		}
		// TODO for now...better together with setSuper...
		if (this.superT != null) {
			this.interfaceTs = NO_INTERFACES;
			return;
		}
		// try simple class loading, may be we are lucky ;)
		// later ask DecoJer-online and local type cache with context info
		try {
			final Class<?> clazz = getClass().getClassLoader().loadClass(
					getName());
			this.accessFlags = clazz.getModifiers();
			final Class<?> superclass = clazz.getSuperclass();
			if (superclass != null) {
				this.superT = this.du.getT(superclass.getName());
			}
			final Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces.length == 0) {
				this.interfaceTs = NO_INTERFACES;
				return;
			}
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = this.du.getT(interfaces[i].getName());
			}
			this.interfaceTs = interfaceTs;
		} catch (final ClassNotFoundException e) {
			System.out.println("Couldn't load type : " + this);
			this.interfaceTs = NO_INTERFACES;
		}
		return;
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
		// TODO improve lame code
		return this == T.BOOLEAN || this == T.BYTE || this == T.CHAR
				|| this == T.DOUBLE || this == T.FLOAT || this == T.INT
				|| this == T.LONG || this == T.SHORT;
	}

	/**
	 * Is reference?
	 * 
	 * @return true - is reference
	 */
	public boolean isReference() {
		return this == T.AREF || this.du != null;
	}

	/**
	 * Is wide type?
	 * 
	 * @return true - is wide type
	 */
	public boolean isWide() {
		return this == T.DOUBLE || this == T.LONG;
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
	 * Merge type.
	 * 
	 * @param t
	 *            type
	 * @return merged type
	 */
	public T merge(final T t) {
		if (t == this || t == null) {
			return this;
		}
		// has priority, TT doesn't know merged super states
		if (t instanceof TT) {
			return t.merge(this);
		}
		if (!isReference() || !t.isReference()) {
			// unequal primitives or special types cannot be equal
			return T.BOGUS;
		}
		if (this == T.AREF && t.isReference()) {
			return t;
		}
		if (t == T.AREF && isReference()) {
			return this;
		}
		System.out.println("Merge: " + this + " <-> " + t);
		return this;
	}

	/**
	 * Assign to type: Assign instances from this type to given (multi-)type,
	 * reduce multis...
	 * 
	 * Like merge, but doesn't create new multi-types through common super type
	 * search.
	 * 
	 * @param t
	 *            type
	 * @return t (reduced) type (or BOGUS for bytecode error)
	 */
	public T mergeTo(final T t) {
		if (t == this || t == null) {
			return this;
		}
		// has priority, TT doesn't know merged super states
		if (t instanceof TT) {
			return t.mergeTo(this);
		}
		if (!isReference() || !t.isReference()) {
			// unequal primitives or special types cannot be equal
			return T.BOGUS;
		}
		if (this == T.AREF && t.isReference()) {
			return t;
		}
		if (t == T.AREF && isReference()) {
			return this;
		}

		if (t.isArray()) {
			if (!isArray()) {
				return T.BOGUS;
			}
			if (getBaseT().subtypeOf(t.getBaseT())) {
				return this;
			}
		}
		if (subtypeOf(t)) {
			return this;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("AssignTo: " + this + " -> " + t + " (Super: " + getSuperT());
		if (getInterfaceTs() != null) {
			for (final T it : getInterfaceTs()) {
				sb.append(", ").append(it.toString());
			}
		}
		sb.append(")");
		System.out.println(sb.toString());
		return this;
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

	private boolean subtypeOf(final T t) {
		if (Object.class.getName().equals(t.getName())) {
			return true;
		}
		// check one hierarchie up first
		final T superT = getSuperT();
		if (superT == t) {
			return true;
		}
		final T[] interfaceTs = getInterfaceTs();
		if (interfaceTs != null) {
			for (final T interfaceT : interfaceTs) {
				if (interfaceT == t) {
					return true;
				}
			}
		}
		// now check further up
		if (superT != null && superT.subtypeOf(t)) {
			return true;
		}
		if (interfaceTs != null) {
			for (final T interfaceT : interfaceTs) {
				if (interfaceT.subtypeOf(t)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getName();
	}

}