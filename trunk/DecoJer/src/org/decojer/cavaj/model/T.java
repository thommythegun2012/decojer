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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.decojer.DecoJer;

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
		public boolean is(final Class<?>... clazzes) {
			if (this.ts.length != clazzes.length) {
				return false;
			}
			loop: for (final T thisT : this.ts) {
				for (final Class<?> clazz : clazzes) {
					if (thisT.is(clazz)) {
						continue loop;
					}
				}
				return false;
			}
			return true;
		}

		@Override
		public boolean is(final T... ts) {
			if (ts.length == 1 && ts[0].isMulti()) {
				return is(((TT) ts[0]).ts);
			}
			if (this.ts.length != ts.length) {
				return false;
			}
			loop: for (final T thisT : this.ts) {
				for (final T cmpT : ts) {
					if (thisT == cmpT) {
						continue loop;
					}
				}
				return false;
			}
			return true;
		}

		@Override
		public boolean isMulti() {
			return true;
		}

		@Override
		public boolean isWide() {
			return this == T.WIDE;
		}

		@Override
		public T merge(final T t) {
			if (t == this || t == null) {
				return this;
			}
			boolean changed = false;
			final ArrayList<T> mergedTs = new ArrayList<T>();
			for (final T iT : this.ts) {
				final T mergedT = t.merge(iT); // t might be multi too
				if (mergedT != iT) {
					changed = true;
					if (mergedT == T.BOGUS) {
						continue;
					}
				}
				if (mergedT.isMulti()) {
					for (final T iMergedT : ((TT) mergedT).getTs()) {
						if (!mergedTs.contains(iMergedT)) {
							mergedTs.add(iMergedT);
						}
					}
					continue;
				}
				if (!mergedTs.contains(mergedT)) {
					mergedTs.add(mergedT);
				}
			}
			if (!changed) {
				return this;
			}
			if (mergedTs.size() == 0) {
				return T.BOGUS;
			}
			if (mergedTs.size() == 1) {
				return mergedTs.get(0);
			}
			return new TT(mergedTs.toArray(new T[mergedTs.size()]));
		}

		@Override
		public T mergeTo(final T t) {
			if (t == this || t == null) {
				return this;
			}
			boolean changed = false;
			final List<T> mergedTs = new ArrayList<T>();
			for (final T iT : this.ts) {
				final T mergedT = iT.mergeTo(t);
				if (mergedT != iT) {
					changed = true;
					if (mergedT == T.BOGUS) {
						continue;
					}
				}
				// cannot be a multi-type, result can only be iT / BOGUS or AREF could change to
				// concrete type
				assert !mergedT.isMulti();

				if (!mergedTs.contains(mergedT)) {
					mergedTs.add(mergedT);
				}
			}
			if (!changed) {
				return this;
			}
			if (mergedTs.size() == 0) {
				return T.BOGUS;
			}
			if (mergedTs.size() == 1) {
				return mergedTs.get(0);
			}
			return new TT(mergedTs.toArray(new T[mergedTs.size()]));
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
	 * Artificial type 'address' for JSR follow pc.
	 */
	public static final T ADDRESS = new T("ADDRESS");
	/**
	 * Artificial type 'any reference'.
	 */
	public static T AREF = new T("AREF");
	/**
	 * Artificial type 'uninit'.
	 */
	public static T UNINIT = new T("UNINIT");
	/**
	 * Artificial type 'uninit'.
	 */
	public static T UNRESOLVABLE = new T("UNRESOLVABLE");
	/**
	 * Artificial type 'bogus'.
	 */
	public static T BOGUS = new T("BOGUS");
	/**
	 * Multi-type 'any int (32 bit)'.
	 */
	public static TT AINT = multi(INT, SHORT, BYTE, CHAR, BOOLEAN);
	/**
	 * Multi-type 'implicit int (32 bit)'.
	 * 
	 * From VM Spec: Note that widening numeric conversions do not exist from integral types byte,
	 * char, and short to type int. As noted in §3.11.1, values of type byte, char, and short are
	 * internally widened to type int, making these conversions implicit.
	 */
	public static TT IINT = multi(INT, SHORT, BYTE, CHAR);
	/**
	 * Multi-type 'dalvik int (32 bit)', includes float.
	 */
	public static TT DINT = multi(INT, SHORT, BYTE, CHAR, BOOLEAN, FLOAT);
	/**
	 * Multi-type 'any wide (64 bit)'.
	 */
	public static TT WIDE = multi(DOUBLE, LONG);

	/**
	 * From Opcodes.T_BOOLEAN = 4 to T_LONG = 11.
	 */
	public static final T[] TYPES = new T[] { null, null, null, null, T.BOOLEAN, T.CHAR, T.FLOAT,
			T.DOUBLE, T.BYTE, T.SHORT, T.INT, T.LONG };

	private static final T[] NO_INTERFACES = new T[0];

	public static void main(final String[] args) {
		final DU du = DecoJer.createDu();
		T tm;

		assert int.class.isAssignableFrom(int.class) == du.getT(int.class).isAssignableFrom(
				du.getT(int.class));
		assert int.class.isAssignableFrom(short.class) == du.getT(int.class).isAssignableFrom(
				du.getT(short.class));
		assert Object.class.isAssignableFrom(int[].class) == du.getT(Object.class)
				.isAssignableFrom(du.getT(int[].class));

		assert Object.class.isAssignableFrom(Serializable.class) == du.getT(Object.class)
				.isAssignableFrom(du.getT(Serializable.class));
		assert Object[].class.isAssignableFrom(Serializable[].class) == du.getT(Object[].class)
				.isAssignableFrom(du.getT(Serializable[].class));
		assert Number[].class.isAssignableFrom(Integer[].class) == du.getT(Number[].class)
				.isAssignableFrom(du.getT(Integer[].class));

		// mergeTo is like subtypeOf and reduces multi-types
		tm = du.getT(Integer.class).mergeTo(du.getT(Number.class));
		assert tm.is(Integer.class) : tm;

		tm = T.multi(du.getT(Integer.class), du.getT(Double.class)).mergeTo(
				du.getT(Serializable.class));
		assert tm.is(Integer.class, Double.class) : tm;

		// merge is looking for smallest super types and creates multi-types

		// primitives
		tm = T.INT.merge(null);
		assert tm.is(T.INT);

		tm = T.LONG.merge(T.LONG);
		assert tm.is(T.LONG);

		tm = T.DOUBLE.merge(T.AREF);
		assert tm.is(T.BOGUS);

		tm = T.CHAR.merge(T.SHORT);
		assert tm.is(T.BOGUS) : tm;

		// multi and single primitive
		tm = T.AINT.merge(T.CHAR);
		assert tm.is(T.CHAR) : tm;

		tm = T.INT.merge(T.DINT);
		assert tm.is(T.INT) : tm;

		// multi primitives
		tm = T.AINT.merge(T.DINT);
		assert tm.is(T.AINT) : tm;

		tm = T.DINT.merge(T.IINT);
		assert tm.is(T.IINT) : tm;

		// primitive arrays
		tm = du.getArrayT(T.INT, 1).merge(du.getArrayT(T.INT, 1));
		assert tm.is(du.getArrayT(T.INT, 1)) : tm;

		tm = du.getArrayT(T.INT, 2).merge(du.getArrayT(T.INT, 1));
		assert tm.is(du.getT(Cloneable.class), du.getT(Serializable.class)) : tm;

		tm = du.getArrayT(T.INT, 3).merge(du.getT(Object.class));
		assert tm.is(du.getT(Object.class)) : tm;

		tm = du.getArrayT(T.INT, 4).merge(du.getT(Cloneable.class));
		assert tm.is(du.getT(Cloneable.class)) : tm;

		tm = du.getArrayT(T.INT, 5).merge(du.getT(Serializable.class));
		assert tm.is(du.getT(Serializable.class)) : tm;

		tm = du.getT(Serializable.class).merge(du.getArrayT(T.INT, 5));
		assert tm.is(du.getT(Serializable.class)) : tm;

		// AREF and Object have different behaviour in merge!
		tm = du.getArrayT(T.INT, 1).merge(T.AREF);
		assert tm.is(du.getArrayT(T.INT, 1)) : tm;

		tm = du.getArrayT(T.INT, 1).merge(du.getT(Object.class));
		assert tm.is(du.getT(Object.class)) : tm;

		tm = du.getT(Integer.class).merge(du.getT(Long.class));
		assert tm.is(Number.class, Comparable.class) : tm;

		tm = du.getT(Comparable.class).merge(du.getT(Object.class));
		assert tm.is(Object.class) : tm;

		tm = du.getT(Object.class).merge(du.getT(Serializable.class));
		assert tm.is(Object.class) : tm;

		System.out.println("HURRAY: " + tm);
	}

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

	protected T(final DU du, final T baseT, final int dim) {
		assert baseT != null;
		assert baseT.dim == 0;
		assert dim > 0;

		this.du = du;
		this.superT = baseT;
		this.dim = dim;
		final StringBuilder name = new StringBuilder(baseT.getName());
		for (int i = dim; i-- > 0;) {
			name.append("[]");
		}
		this.name = name.toString();
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
	public boolean check(final AF af) {
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
	 * Get decompilation unit or null (for primitive and special types).
	 * 
	 * @return decompilation unit or null
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
		if (pos == -1 || pos + 1 >= pName.length()) {
			return pName;
		}
		return Character.isJavaIdentifierStart(pName.charAt(pos + 1)) ? pName.substring(pos + 1)
				: "I_" + pName.substring(pos + 1);
	}

	/**
	 * Get interface types.
	 * 
	 * @return interface types
	 */
	public T[] getInterfaceTs() {
		init();
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
		init();
		if (isArray()) {
			return this.du.getT(Object.class);
		}
		return this.superT;
	}

	private void init() {
		if (this.interfaceTs != null) {
			return; // arrays return here because interface is set in du.getT()
		}
		// setSuper() in class read doesn't set interfaces if not known
		if (this.superT != null) {
			this.interfaceTs = NO_INTERFACES;
			return;
		}
		// try simple class loading, may be we are lucky ;)
		// later ask DecoJer-online and local type cache with context info
		try {
			final Class<?> clazz = getClass().getClassLoader().loadClass(getName());
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
			this.superT = T.UNRESOLVABLE;
			this.interfaceTs = NO_INTERFACES;
		}
		return;
	}

	/**
	 * Is type of class?
	 * 
	 * @param clazzes
	 *            classes
	 * @return true - type is of class
	 */
	public boolean is(final Class<?>... clazzes) {
		if (clazzes.length != 1) {
			return false;
		}
		return this.name.equals(clazzes[0].getName());
	}

	/**
	 * Is type of types?
	 * 
	 * @param ts
	 *            types
	 * @return true - type is of types
	 */
	public boolean is(final T... ts) {
		if (ts.length != 1) {
			return false;
		}
		return this == ts[0];
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
	 * Is this type instance assignable from given type instance?
	 * 
	 * @param t
	 *            type
	 * @return true - is assignable
	 * 
	 * @see Class#isAssignableFrom(Class)
	 */
	public boolean isAssignableFrom(final T t) {
		assert t != null && !isMulti() && !t.isMulti();

		// all instances are assignable to Object, even if only known by interface
		if (t == this) {
			return true;
		}
		if (!isReference() || !t.isReference()) {
			// unequal primitives or special types cannot be equal
			return false;
		}
		// references (arrays too) only from here on
		if (t == T.AREF || this == T.AREF || this.is(Object.class)) {
			return true;
		}
		if (this.dim > 0 && t.dim > 0) {
			// one is array and other is Object/Cloneable/Serializable still possible
			if (this.dim != t.dim) {
				return false;
			}
			return getBaseT().isAssignableFrom(t.getBaseT());
		}
		// raise step by step in hierarchy...lazy fetch unknown super
		final LinkedList<T> ts = new LinkedList<T>();
		ts.add(t);
		while (!ts.isEmpty()) {
			final T iT = ts.pollFirst();
			final T superT = iT.getSuperT();
			if (superT == T.UNRESOLVABLE) {
				// consider this OK for valid bytecode, wrong results possible for frame-local
				// variables without debug information
				return true;
			}
			if (superT == this) {
				return true;
			}
			if (superT != null) {
				ts.add(superT);
			}
			for (final T interfaceT : iT.getInterfaceTs()) {
				if (interfaceT == this) {
					return true;
				}
				if (interfaceT != null) {
					ts.add(interfaceT);
				}
			}
		}
		return false;
	}

	/**
	 * Is multi-type?
	 * 
	 * @return true - is multi-type
	 */
	public boolean isMulti() {
		return false;
	}

	/**
	 * Is primitive?
	 * 
	 * @return true - is primitive
	 */
	public boolean isPrimitive() {
		// TODO improve lame code
		return this == T.BOOLEAN || this == T.BYTE || this == T.CHAR || this == T.DOUBLE
				|| this == T.FLOAT || this == T.INT || this == T.LONG || this == T.SHORT;
	}

	/**
	 * Is reference (array too)?
	 * 
	 * @return true - is reference (array too)
	 */
	public boolean isReference() {
		return this == T.AREF || this.du != null; // included: dim > 0
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
		if (t.isMulti()) {
			return t.merge(this);
		}
		if (t.isAssignableFrom(this)) {
			return t == AREF ? this : t;
		}
		if (!isReference() || !t.isReference()) {
			return BOGUS;
		}
		// find common supertypes, raise in t-hierarchy till assignable from this
		final ArrayList<T> mergedTs = new ArrayList<T>();
		// raise step by step in hierarchy...lazy fetch unknown super
		final LinkedList<T> ts = new LinkedList<T>();
		ts.add(t);
		while (!ts.isEmpty()) {
			final T iT = ts.pollFirst();
			final T superT = iT.getSuperT();
			if (superT == T.UNRESOLVABLE) {
				if (mergedTs.isEmpty()) {
					// Object always a valid response, wrong results possible for frame-local
					// variables without debug information
					mergedTs.add(this.du.getT(Object.class));
				}
				continue;
			}
			if (superT != null) {
				if (superT.isAssignableFrom(this)) {
					if (!mergedTs.contains(superT)) {
						mergedTs.add(superT);
					}
				} else {
					ts.add(superT);
				}
			}
			for (final T interfaceT : iT.getInterfaceTs()) {
				if (interfaceT.isAssignableFrom(this)) {
					if (!mergedTs.contains(interfaceT)) {
						mergedTs.add(interfaceT);
					}
				} else if (interfaceT != null) {
					ts.add(interfaceT);
				}
			}
		}
		if (mergedTs.size() == 0) {
			return this.du.getT(Object.class);
		}
		if (mergedTs.size() > 1) {
			mergedTs.remove(this.du.getT(Object.class));
		}
		if (mergedTs.size() == 1) {
			return mergedTs.get(0);
		}
		return new TT(mergedTs.toArray(new T[mergedTs.size()]));
	}

	/**
	 * Merge-to type. Check if instances from this type are assignable to the given type. For single
	 * types this returns this or BOGUS. For multi-types it reduces the multi-type to all assignable
	 * types (via polymorphism function).
	 * 
	 * So unlike merge this doesn't search for common super types!
	 * 
	 * @param t
	 *            type
	 * @return t this or T.BOGUS
	 */
	public T mergeTo(final T t) {
		if (t == this || t == null) {
			return this;
		}
		if (t.isMulti()) {
			for (final T iT : ((TT) t).getTs()) {
				if (mergeTo(iT) != T.BOGUS) {
					return this;
				}
			}
			return T.BOGUS;
		}
		if (t.isAssignableFrom(this)) {
			return this == AREF ? t : this;
		}
		// this should never happen...
		if (isReference() && t.isReference()) {
			final StringBuilder sb = new StringBuilder();
			sb.append("MergeTo: " + this + " (Super: " + getSuperT());
			for (final T it : getInterfaceTs()) {
				sb.append(", ").append(it.toString());
			}
			sb.append(") -> " + t + " (Super: " + t.getSuperT());
			for (final T it : t.getInterfaceTs()) {
				sb.append(", ").append(it.toString());
			}
			sb.append(")");
			System.out.println(sb.toString());
		}
		return T.BOGUS;
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
		// inner classes always with . here...bad for us?
		this.signature = signature.replace('.', '$');
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