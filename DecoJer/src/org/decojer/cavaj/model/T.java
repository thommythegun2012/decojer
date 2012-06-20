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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.util.Cursor;

/**
 * Type.
 * 
 * @author André Pankraz
 */
public class T {

	public enum Kind {

		INT(1 << 0, int.class),

		SHORT(1 << 1, short.class),

		BYTE(1 << 2, byte.class),

		CHAR(1 << 3, char.class),

		BOOLEAN(1 << 4, boolean.class),

		FLOAT(1 << 5, float.class),

		LONG(1 << 6, long.class),

		DOUBLE(1 << 7, double.class),

		VOID(1 << 8, void.class),

		REF(1 << 9),

		RET(1 << 10),

		LONG2(1 << 11),

		DOUBLE2(1 << 12);

		@Getter
		private final Class<?> clazz;

		@Getter
		private final int kind;

		private Kind(final int flag) {
			this(flag, null);
		}

		private Kind(final int flag, final Class<?> clazz) {
			this.clazz = clazz;
			this.kind = flag;
		}

		public String getName() {
			return this.clazz == null ? name() : this.clazz.getName();
		}

	}

	private final static Logger LOGGER = Logger.getLogger(T.class.getName());

	private static final Map<Integer, T> KIND_2_TS = new HashMap<Integer, T>();

	private static int[][] READ_INT = {
	/* ___i */{ 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 },
	/* __s_ */{ 2, 2, 2, 0, 2, 2, 2, 0, 2, 2, 2, 0, 2, 2, 2 },
	/* __si */{ 3, 2, 3, 0, 3, 2, 3, 0, 3, 2, 3, 0, 3, 2, 3 },
	/* _b__ */{ 4, 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 4 },
	/* _b_i */{ 5, 4, 5, 4, 5, 4, 5, 0, 5, 4, 5, 4, 5, 4, 5 },
	/* _bs_ */{ 6, 6, 6, 4, 6, 6, 6, 0, 6, 6, 6, 4, 6, 6, 6 },
	/* _bsi */{ 7, 6, 7, 4, 7, 6, 7, 0, 7, 6, 7, 4, 7, 6, 7 },
	/* c___ */{ 8, 0, 8, 0, 8, 0, 8, 8, 8, 8, 8, 8, 8, 8, 8 },
	/* c__i */{ 9, 0, 9, 0, 9, 0, 9, 8, 9, 8, 9, 8, 9, 8, 9 },
	/* c_s_ */{ 10, 2, 10, 0, 10, 2, 10, 8, 10, 10, 10, 8, 10, 10, 10 },
	/* c_si */{ 11, 2, 11, 0, 11, 2, 11, 8, 11, 10, 11, 8, 11, 10, 11 },
	/* cb__ */{ 12, 4, 12, 4, 12, 4, 12, 8, 12, 12, 12, 12, 12, 12, 12 },
	/* cb_i */{ 13, 4, 13, 4, 13, 4, 13, 8, 13, 12, 13, 12, 13, 12, 13 },
	/* cbs_ */{ 14, 6, 14, 4, 14, 6, 14, 8, 14, 14, 14, 12, 14, 14, 14 },
	/* cbsi */{ 15, 6, 15, 4, 15, 6, 15, 8, 15, 14, 15, 12, 15, 14, 15 } };

	private static int[][] JOIN_INT = {
	/* ___i */{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
	/* __s_ */{ 1, 2, 3, 2, 3, 2, 2, 1, 1, 3, 3, 3, 3, 3, 3 },
	/* __si */{ 1, 3, 3, 3, 3, 3, 3, 1, 1, 3, 3, 3, 3, 3, 3 },
	/* _b__ */{ 1, 2, 3, 4, 5, 6, 1, 1, 1, 3, 3, 5, 5, 7, 7 },
	/* _b_i */{ 1, 3, 3, 5, 5, 7, 1, 1, 1, 3, 3, 5, 5, 7, 7 },
	/* _bs_ */{ 1, 2, 3, 6, 7, 7, 1, 1, 1, 3, 3, 7, 7, 7, 7 },
	/* _bsi */{ 1, 3, 3, 7, 7, 7, 7, 1, 1, 3, 3, 7, 7, 7, 7 },

	/* c___ */{ 1, 1, 1, 1, 1, 1, 1, 8, 9, 9, 9, 9, 9, 9, 9 },
	/* c__i */{ 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9 },
	/* c_s_ */{ 1, 3, 3, 3, 3, 3, 3, 9, 9, 11, 11, 11, 11, 11, 11 },
	/* c_si */{ 1, 3, 3, 3, 3, 3, 3, 9, 9, 11, 11, 11, 11, 11, 11 },
	/* cb__ */{ 1, 3, 3, 5, 5, 7, 7, 9, 9, 11, 11, 13, 13, 15, 15 },
	/* cb_i */{ 1, 3, 3, 5, 5, 7, 7, 9, 9, 11, 11, 13, 13, 15, 15 },
	/* cbs_ */{ 1, 3, 3, 7, 7, 7, 7, 9, 9, 11, 11, 15, 15, 15, 15 },
	/* cbsi */{ 1, 3, 3, 7, 7, 7, 7, 9, 9, 11, 11, 15, 15, 15, 15 } };

	/**
	 * Primitive type int.
	 */
	public static T INT = getT(Kind.INT);
	/**
	 * Primitive type short.
	 */
	public static T SHORT = getT(Kind.SHORT);
	/**
	 * Primitive type byte.
	 */
	public static T BYTE = getT(Kind.BYTE);
	/**
	 * Primitive type char.
	 */
	public static T CHAR = getT(Kind.CHAR);
	/**
	 * Primitive type boolean.
	 */
	public static T BOOLEAN = getT(Kind.BOOLEAN);
	/**
	 * Primitive type float.
	 */
	public static T FLOAT = getT(Kind.FLOAT);
	/**
	 * Primitive type long.
	 */
	public static T LONG = getT(Kind.LONG);
	/**
	 * Primitive type double.
	 */
	public static T DOUBLE = getT(Kind.DOUBLE);
	/**
	 * Primitive type void.
	 */
	public static T VOID = getT(Kind.VOID);

	/**
	 * Artificial type 'reference'.
	 */
	public static T REF = getT(Kind.REF);
	/**
	 * Artificial type 'return address' for JSR follow pc.
	 * 
	 * Spec: No return address (a value of type returnAddress) may be loaded from a local variable.
	 */
	public static final T RET = getT(Kind.RET);
	/**
	 * Artificial type long part 2.
	 */
	public static T LONG2 = getT(Kind.LONG2);
	/**
	 * Artificial type double part 2.
	 */
	public static T DOUBLE2 = getT(Kind.DOUBLE2);

	/**
	 * Multi-type 'any reference'.
	 */
	public static T AREF = getT(Kind.REF, Kind.RET);
	/**
	 * Multi-type 'any JVM int'.
	 */
	public static T AINT = getT(Kind.INT, Kind.SHORT, Kind.BYTE, Kind.CHAR, Kind.BOOLEAN);
	/**
	 * Multi-type 'primitive'.
	 */
	public static T PRIMITIVE = getT(Kind.INT, Kind.SHORT, Kind.BYTE, Kind.CHAR, Kind.BOOLEAN,
			Kind.FLOAT, Kind.LONG, Kind.DOUBLE, Kind.VOID);
	/**
	 * Multi-type 'any small (8 bit)'.
	 */
	public static T SMALL = getT(Kind.BYTE, Kind.BOOLEAN);
	/**
	 * Multi-type 'any single (32 bit)'.
	 */
	public static T SINGLE = getT(Kind.INT, Kind.SHORT, Kind.BYTE, Kind.CHAR, Kind.BOOLEAN,
			Kind.FLOAT);
	/**
	 * Multi-type 'any wide (64 bit)'.
	 */
	public static T WIDE = getT(Kind.LONG, Kind.DOUBLE);

	/**
	 * Translation from JVM Array Opcodes: T_BOOLEAN = 4 to T_LONG = 11.
	 */
	public static final T[] TYPES = new T[] { null, null, null, null, T.BOOLEAN, T.CHAR, T.FLOAT,
			T.DOUBLE, T.BYTE, T.SHORT, T.INT, T.LONG };

	private static final T[] NO_INTERFACES = new T[0];

	private static int joinKinds(final int kind1, final int kind2) {
		final int k1 = (kind1 & 0xF) - 1;
		final int k2 = (kind2 & 0xF) - 1;
		if (k1 >= 0 && k2 >= 0) {
			return JOIN_INT[k1][k2] | kind1 & kind2;
		}
		return kind1 & kind2;
	}

	private static int readKinds(final int kind1, final int kind2) {
		final int k1 = (kind1 & 0xF) - 1;
		final int k2 = (kind2 & 0xF) - 1;
		if (k1 >= 0 && k2 >= 0) {
			return READ_INT[k1][k2] | kind1 & kind2;
		}
		return kind1 & kind2;
	}

	/**
	 * Merge/union read/down/or types: Find common lower type. Use OR operation for kind.
	 * 
	 * If type is yet unknown, leave name empty.
	 * 
	 * @param t1
	 *            type 1
	 * @param t2
	 *            type 2
	 * @return merged type
	 */
	public static T union(final T t1, final T t2) {
		if (t1 == t2) {
			return t1;
		}
		if (t1 == null) {
			return t2;
		}
		if (t2 == null) {
			return t1;
		}
		final int kind = t1.kind | t2.kind;
		if ((kind & Kind.REF.kind) == 0) {
			return getT(kind);
		}

		if (t1.du == null) {
			return t2;
		}
		if (t2.du == null) {
			return t1;
		}
		// TODO...bottom merge ref
		return t1;
	}

	public static T getDalvikIntT(final int value) {
		int kinds = T.FLOAT.kind;
		if (value == 0 || value == 1) {
			kinds |= T.BOOLEAN.kind;
		}
		if (Character.MIN_VALUE <= value && value <= Character.MAX_VALUE) {
			kinds |= T.CHAR.kind;
		}
		if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
			kinds |= T.BYTE.kind;
		} else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
			kinds |= T.SHORT.kind;
		} else {
			kinds |= T.INT.kind;
		}
		return getT(kinds);
	}

	@Getter
	private final String name;

	@Getter
	private String signature;

	public static T getJvmIntT(final int value) {
		int kinds = 0;
		if (value == 0 || value == 1) {
			kinds |= T.BOOLEAN.kind;
		}
		if (Character.MIN_VALUE <= value && value <= Character.MAX_VALUE) {
			kinds |= T.CHAR.kind;
		}
		if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
			kinds |= T.BYTE.kind;
		} else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
			kinds |= T.SHORT.kind;
		} else {
			kinds |= T.INT.kind;
		}
		return getT(kinds);
	}

	private static T getT(final int kinds) {
		if (kinds == 0) {
			return null;
		}
		T t = KIND_2_TS.get(kinds);
		if (t != null) {
			return t;
		}
		final StringBuilder sb = new StringBuilder("{");
		for (final Kind k : Kind.values()) {
			if ((kinds & k.kind) != 0) {
				sb.append(k.getName()).append(",");
			}
		}
		t = new T(sb.substring(0, sb.length() - 1) + "}", kinds);
		KIND_2_TS.put(kinds, t);
		return t;
	}

	private static T getT(final Kind kind) {
		T t = KIND_2_TS.get(kind.kind);
		if (t != null) {
			return t;
		}
		t = new T(kind.getName(), kind.kind);
		KIND_2_TS.put(kind.kind, t);
		return t;
	}

	/**
	 * Super type or base type for arrays or null for none-refs and unresolveable refs.
	 */
	@Setter
	private T superT;

	@Getter
	@Setter
	private int accessFlags;

	@Setter
	private T[] interfaceTs;

	private static T getT(final Kind... kinds) {
		// don't use types as input, restrict to kind-types
		int flags = 0;
		for (final Kind k : kinds) {
			flags |= k.kind;
		}
		return getT(flags);
	}

	/**
	 * Merge/join store/up/and types: Find common super type. Use AND operation for kind - primitive
	 * multitypes: no conversion. No primitive reduction of source types, done through eventual
	 * following register read. Resulting reference type contains one class and multiple interfaces.
	 * 
	 * If type is yet unknown, leave name empty.
	 * 
	 * @param t1
	 *            type 1
	 * @param t2
	 *            type 2
	 * @return merged type
	 */
	public static T join(final T t1, final T t2) {
		if (t1 == t2) {
			return t1;
		}
		if (t1 == null || t2 == null) {
			return null;
		}
		final int kind = joinKinds(t1.kind, t2.kind);
		if ((kind & Kind.REF.kind) == 0) {
			return getT(kind);
		}

		assert t1.du != null;

		if (t1.isAssignableFrom(t2)) {
			return t1;
		}

		T superT = null;
		// find common supertypes, raise in t-hierarchy till assignable from this
		final ArrayList<T> interfaceTs = new ArrayList<T>();
		// raise step by step in hierarchy...lazy fetch unknown super
		final LinkedList<T> ts = new LinkedList<T>();
		ts.add(t1);
		while (!ts.isEmpty()) {
			final T iT = ts.pollFirst();
			if (superT == null && !iT.isInterface()) {
				superT = iT.getSuperT();
				if (superT != null) {
					if (!superT.isAssignableFrom(t2)) {
						ts.add(superT);
						superT = null;
					}
				}
			}
			for (final T interfaceT : iT.getInterfaceTs()) {
				if (interfaceT.isAssignableFrom(t2)) {
					if (!interfaceTs.contains(interfaceT)) {
						interfaceTs.add(interfaceT);
					}
				} else if (interfaceT != null) {
					ts.add(interfaceT);
				}
			}
		}

		if (interfaceTs.isEmpty()) {
			return superT;
		}
		if (interfaceTs.size() == 1 && superT.isObject()) {
			return interfaceTs.get(0);
		}
		return new T(superT, interfaceTs.toArray(new T[interfaceTs.size()]));
	}

	@Getter
	private T[] typeParams;

	private final DU du;

	private final HashMap<String, F> fs = new HashMap<String, F>();

	private final int kind;

	private final HashMap<String, M> ms = new HashMap<String, M>();

	protected T(final DU du, final String name) {
		assert du != null;
		assert name != null;

		this.du = du;
		this.kind = Kind.REF.kind;
		this.name = name;
	}

	private T(final String name, final int kind) {
		this.du = null; // primitive or internal
		this.name = name;
		this.kind = kind;
	}

	private T(final T superT, final T[] interfaceTs) {
		assert superT.du != null;
		assert superT != null;
		assert interfaceTs != null;

		this.du = superT.du;
		this.kind = Kind.REF.kind;
		this.superT = superT;
		this.interfaceTs = interfaceTs;
		final StringBuilder sb = new StringBuilder("{");
		sb.append(superT.getName()).append(",");
		for (final T t : interfaceTs) {
			sb.append(t.getName()).append(",");
		}
		this.name = sb.substring(0, sb.length() - 1) + "}";
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean check(final AF af) {
		return isResolveable() && (this.accessFlags & af.getValue()) != 0;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof T)) {
			return false;
		}
		return getName().equals(((T) obj).getName());
	}

	/**
	 * Get component type of array type (null if no array type).
	 * 
	 * @return component type of array type (null if no array type)
	 * 
	 * @see Class#isAssignableFrom(Class)
	 */
	public T getComponentT() {
		return null;
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
		if (this.name.startsWith("{")) {
			return this.name;
		}
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
		return isResolveable() ? this.interfaceTs : NO_INTERFACES;
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
		if (this.name.startsWith("{")) {
			return this.name;
		}
		final int pos = this.name.lastIndexOf('.');
		return pos == -1 ? this.name : this.name.substring(pos + 1);
	}

	/**
	 * Get super type.
	 * 
	 * @return super type
	 */
	public T getSuperT() {
		return isResolveable() ? this.superT : null;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
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
		if (isMulti()) {
			for (final T t : ts) {
				if ((this.kind & t.kind) == 0) {
					return false;
				}
			}
			return true;
		}
		return this == ts[0];
	}

	/**
	 * Is array?
	 * 
	 * @return true - is array
	 */
	public boolean isArray() {
		return false;
	}

	/**
	 * Is this type instance assignable from given type instance?
	 * 
	 * Attention: Doesn't work for primtives implicit conversion (byte 2 short 2 int, char 2 int).
	 * 
	 * @param t
	 *            type
	 * @return true - is assignable
	 * 
	 * @see Class#isAssignableFrom(Class)
	 */
	public boolean isAssignableFrom(final T t) {
		if (this == t) {
			return true;
		}
		if (t == null) {
			return false;
		}
		final int kind = readKinds(t.kind, this.kind);
		if ((kind & Kind.REF.kind) == 0) {
			return kind != 0;
		}
		// assignableFrom(T.REF) is true, null is T.REF!
		// may be better to check for null const in R instead of this general answer
		if (null == this.du || this.is(Object.class) || t.du == null) {
			return true;
		}
		// raise step by step in hierarchy...lazy fetch unknown super
		final LinkedList<T> ts = new LinkedList<T>();
		ts.add(t);
		while (!ts.isEmpty()) {
			final T iT = ts.pollFirst();
			final T superT = iT.getSuperT();
			if (this == superT) {
				return true;
			}
			if (null != superT) {
				ts.add(superT);
			}
			for (final T interfaceT : iT.getInterfaceTs()) {
				if (this == interfaceT) {
					return true;
				}
				if (null != interfaceT) {
					ts.add(interfaceT);
				}
			}
		}
		if (isArray() && t.isArray()) {
			return getComponentT().isAssignableFrom(t.getComponentT());
		}
		return false;
	}

	/**
	 * Is interface?
	 * 
	 * @return true - is interface
	 */
	public boolean isInterface() {
		return check(AF.INTERFACE);
	}

	/**
	 * Is multitype?
	 * 
	 * @return true - is multitype
	 */
	public boolean isMulti() {
		int nr = this.kind - (this.kind >> 1 & 0x55555555);
		nr = (nr & 0x33333333) + (nr >> 2 & 0x33333333);
		nr = (nr + (nr >> 4) & 0x0F0F0F0F) * 0x01010101 >> 24;

		assert nr > 0;

		return nr > 1;
	}

	/**
	 * Is object type?
	 * 
	 * @return true - is object type
	 */
	public boolean isObject() {
		return Object.class.getName().equals(this.name);
	}

	/**
	 * Is primitive?
	 * 
	 * @return true - is primitive
	 */
	public boolean isPrimitive() {
		return (this.kind & PRIMITIVE.kind) != 0;
	}

	/**
	 * Is reference (array too)?
	 * 
	 * @return true - is reference (array too)
	 */
	public boolean isRef() {
		return (this.kind & REF.kind) != 0;
	}

	/**
	 * Is unresolveable?
	 * 
	 * @return true - is unresolveable
	 */
	public boolean isResolveable() {
		if (this.interfaceTs != null) {
			return (this.accessFlags & AF.UNRESOLVEABLE.getValue()) == 0;
		}
		if (!isRef()) {
			this.interfaceTs = NO_INTERFACES;
			return true;
		}
		// setSuper() in class read doesn't set interfaces if not known
		if (this.superT != null) {
			this.interfaceTs = NO_INTERFACES;
			return true;
		}
		// try simple class loading, may be we are lucky ;)
		// TODO later ask DecoJer-online and local type cache with context info
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
			} else {
				final T[] interfaceTs = new T[interfaces.length];
				for (int i = interfaces.length; i-- > 0;) {
					interfaceTs[i] = this.du.getT(interfaces[i].getName());
				}
				this.interfaceTs = interfaceTs;
			}
			return true;
		} catch (final ClassNotFoundException e) {
			LOGGER.warning("Couldn't load type : " + this);
			this.interfaceTs = NO_INTERFACES;
			markAf(AF.UNRESOLVEABLE);
			return false;
		}
	}

	/**
	 * Is wide type?
	 * 
	 * @return true - is wide type
	 */
	public boolean isWide() {
		return (this.kind & WIDE.kind) != 0;
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
	 * Read with given type. There are 3 possible outcomes: Cannot read with given type, which
	 * returns _null_. Can read with given type and primitive multitype reduction, which returns the
	 * reduced new type. Can read without reduction, which returns unmodified _this_.
	 * 
	 * @param t
	 *            read type
	 * @return null or reduced type or this
	 */
	public T read(final T t) {
		if (this == t) {
			return this;
		}
		if (t == null) {
			return null;
		}
		final int kind = readKinds(this.kind, t.kind);
		if ((kind & Kind.REF.kind) == 0) {
			return getT(kind);
		}

		if (t.isAssignableFrom(this)) {
			return this;
		}
		return null;
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

		final Cursor c = new Cursor();
		this.typeParams = getDu().parseTypeParams(signature, c);
		final T returnT = getDu().parseT(signature, c);
	}

	@Override
	public String toString() {
		return getIName();
	}

}