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

import lombok.Getter;

import org.decojer.cavaj.model.types.BaseT;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.Kind;
import org.decojer.cavaj.model.types.ParamT;

/**
 * Type.
 * 
 * @author André Pankraz
 */
public abstract class T {

	private static final Map<Integer, T> KIND_2_TS = new HashMap<Integer, T>();

	/**
	 * Multitype READ target-multitype => reduced (left) multitype (according to Java-conversions),
	 * e.g.:<br>
	 * __s_ read ___i => __s_<br>
	 * __s_ read __si => __s_<br>
	 * __si read __s_ => __s_<br>
	 * __s_ read _b__ => ____<br>
	 * _bsi read c__i => _bsi
	 */
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
	 * Artificial type 'none'.
	 */
	public static T NONE = getT(Kind.NONE);
	/*
	 * Artificial type 'unresolvable'.
	 */
	public static T UNRESOLVABLE = getT(Kind.UNRESOLVABLE);

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

	public static final T[] INTERFACES_NONE = new T[0];

	public static final T[] TYPE_PARAMS_NONE = new T[0];

	public static T getDalvikIntT(final int value) {
		int kinds = T.FLOAT.getKind();
		if (value == 0 || value == 1) {
			kinds |= T.BOOLEAN.getKind();
		}
		if (Character.MIN_VALUE <= value && value <= Character.MAX_VALUE) {
			kinds |= T.CHAR.getKind();
		}
		if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
			kinds |= T.BYTE.getKind();
		} else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
			kinds |= T.SHORT.getKind();
		} else {
			kinds |= T.INT.getKind();
		}
		return getT(kinds);
	}

	public static BaseT getJvmIntT(final int value) {
		int kinds = 0;
		if (value == 0 || value == 1) {
			kinds |= T.BOOLEAN.getKind();
		}
		if (Character.MIN_VALUE <= value && value <= Character.MAX_VALUE) {
			kinds |= T.CHAR.getKind();
		}
		if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
			kinds |= T.BYTE.getKind();
		} else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
			kinds |= T.SHORT.getKind();
		} else {
			kinds |= T.INT.getKind();
		}
		return getT(kinds);
	}

	private static BaseT getT(final int kinds) {
		if (kinds == 0) {
			return null;
		}
		BaseT t = (BaseT) KIND_2_TS.get(kinds);
		if (t != null) {
			return t;
		}
		final StringBuilder sb = new StringBuilder("{");
		for (final Kind k : Kind.values()) {
			if ((kinds & k.getKind()) != 0) {
				sb.append(k.getName()).append(",");
			}
		}
		t = new BaseT(sb.substring(0, sb.length() - 1) + "}", kinds);
		KIND_2_TS.put(kinds, t);
		return t;
	}

	private static BaseT getT(final Kind kind) {
		BaseT t = (BaseT) KIND_2_TS.get(kind.getKind());
		if (t != null) {
			return t;
		}
		t = new BaseT(kind.getName(), kind.getKind());
		KIND_2_TS.put(kind.getKind(), t);
		return t;
	}

	private static BaseT getT(final Kind... kinds) {
		// don't use types as input, restrict to kind-types
		int flags = 0;
		for (final Kind k : kinds) {
			flags |= k.getKind();
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
		final int kind = joinKinds(t1.getKind(), t2.getKind());
		if ((kind & Kind.REF.getKind()) == 0) {
			return getT(kind);
		}

		assert t1.getDu() != null;

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
				} else {
					ts.add(interfaceT);
				}
			}
		}

		if (interfaceTs.isEmpty()) {
			return superT;
		}
		if (interfaceTs.size() == 1 && (superT == null || superT.isObject())) {
			return interfaceTs.get(0);
		}
		return new ClassT(superT, interfaceTs.toArray(new T[interfaceTs.size()]));
	}

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
		final int kind = t1.getKind() | t2.getKind();
		if ((kind & Kind.REF.getKind()) == 0) {
			return getT(kind);
		}

		if (t1.getDu() == null) {
			return t2;
		}
		if (t2.getDu() == null) {
			return t1;
		}
		// TODO...bottom merge ref
		return t1;
	}

	/**
	 * Type name.
	 * 
	 * Names consist of '.'-separated package names (for full name) and '$'-separated type names
	 * (but '$' is also a valid Java name char!)
	 * 
	 * Valid name chars contain also connecting characters and other, e.g.:
	 * 
	 * $ _ ¢ £ ¤ ¥ ؋ ৲ ৳ ৻ ૱ ௹ ฿ ៛ ‿ ⁀ ⁔ ₠ ₡ ₢ ₣ ₤ ₥ ₦ ₧ ₨ ₩ ₪ ₫ € ₭ ₮ ₯ ₰ ₱ ₲ ₳ ₴ ₵ ₶ ₷ ₸ ₹ ꠸ ﷼ ︳ ︴
	 * ﹍ ﹎ ﹏ ﹩ ＄ ＿ ￠ ￡ ￥ ￦
	 */
	@Getter
	private final String name;

	final HashMap<String, Object> member = new HashMap<String, Object>();

	protected T(final String name) {
		this.name = name;
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
		return null;
	}

	/**
	 * Get enclosing method (including constructor).
	 * 
	 * @return enclosing method
	 * 
	 * @see ClassT#setEnclosingT(T)
	 * @see Class#getEnclosingMethod()
	 * @see Class#getEnclosingConstructor()
	 */
	public M getEnclosingM() {
		return null; // overwrite in ClassT
	}

	/**
	 * Get enclosing type.
	 * 
	 * @return enclosing type
	 * 
	 * @see ClassT#setEnclosingT(T)
	 * @see Class#getEnclosingClass()
	 */
	public ClassT getEnclosingT() {
		return null; // overwrite in ClassT
	}

	/**
	 * Get field.<br>
	 * Unique identifier is: "name + descriptor" ({@link F})
	 * 
	 * @param name
	 *            field name
	 * @param valueT
	 *            field value type
	 * @return field
	 */
	public F getF(final String name, final T valueT) {
		// Unique identifier is: "name + descriptor" ({@link F})
		final String handle = name + ":" + valueT.getName();
		F f = (F) this.member.get(handle);
		if (f == null) {
			f = new F(this, name, valueT);
			this.member.put(handle, f);
		}
		return f;
	}

	/**
	 * Get inner name. Can derive for JVM > 5 from type names (compatibility rules), but not before.<br>
	 * <br>
	 * According to JLS3 "Binary Compatibility" (13.1) the binary name of non-package classes (not
	 * top level) is the binary name of the immediately enclosing class followed by a '$' followed
	 * by:<br>
	 * (for nested and inner classes): the simple name.<br>
	 * (for local classes): 1 or more digits followed by the simple name.<br>
	 * (for anonymous classes): 1 or more digits.<br>
	 * <br>
	 * 
	 * JVM 5: {@code org.decojer.cavaj.test.DecTestInner$$Inner1$$$$_Inner$1$1AInner2}<br>
	 * Before JVM 5: {@code org.decojer.cavaj.test.DecTestInner$$1$AInner2}
	 * 
	 * @return inner name
	 * @see Class#getSimpleName()
	 */
	public String getInnerName() {
		return null; // overwrite in ClassT
	}

	/**
	 * Get interface types.
	 * 
	 * @return interface types, not {@code null}
	 * @see Class#getInterfaces()
	 */
	public T[] getInterfaceTs() {
		return INTERFACES_NONE;
	}

	/**
	 * Get kind.
	 * 
	 * @return kind
	 */
	public int getKind() {
		return Kind.REF.getKind(); // only base types can be different, overwrite in BaseT
	}

	/**
	 * Get method.<br>
	 * Unique identifier is: "name + descriptor"
	 * 
	 * @param name
	 *            method name
	 * @param descriptor
	 *            method descriptor
	 * @return method
	 */
	public M getM(final String name, final String descriptor) {
		final String handle = name + descriptor;
		M m = (M) this.member.get(handle);
		if (m == null) {
			m = new M(this, name, descriptor);
			this.member.put(handle, m);
		}
		return m;
	}

	/**
	 * Get package name.
	 * 
	 * @return package name or {@code null} for no package
	 */
	public String getPackageName() {
		final int pos = getName().lastIndexOf('.');
		return pos == -1 ? null : getName().substring(0, pos);
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
	 * Get simple name, like appearing in Java source code.
	 * 
	 * Works for all Java versions, not just for JVM >= 5 like {@code Class.getSimpleName()}.
	 * 
	 * @return simple name
	 * @see Class#getSimpleName()
	 * @see T#getInnerName()
	 */
	public String getSimpleName() {
		// The original Class-Function doesn't work for JVM < 5 because the naming rules changed,
		// different solution here with inner name info
		final String innerName = getInnerName();
		if (innerName == null) {
			return getPName();
		}
		return innerName;
	}

	/**
	 * Get super type.
	 * 
	 * @return super type, can be {@code null} for {@code Object} and primitives
	 * @see Class#getSuperclass()
	 */
	public T getSuperT() {
		return null;
	}

	/**
	 * Get type declaration.
	 * 
	 * @return type declaration
	 */
	public TD getTd() {
		return null; // overwrite in ClassT
	}

	/**
	 * Get type parameters.
	 * 
	 * @return type parameters, not {@code null}
	 * @see Class#getTypeParameters()
	 */
	public T[] getTypeParams() {
		return TYPE_PARAMS_NONE;
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
				if ((getKind() & t.getKind()) == 0) {
					return false;
				}
			}
			return true;
		}
		return this == ts[0];
	}

	/**
	 * Returns {@code true} if and only if the underlying class is an anonymous class.
	 * 
	 * @return {@code true} if and only if this class is an anonymous class.
	 * @see Class#isAnonymousClass()
	 */
	public boolean isAnonymous() {
		return "".equals(getSimpleName());
	}

	/**
	 * Is Array Type?
	 * 
	 * @return true - is Array Type
	 */
	public boolean isArray() {
		return false; // overwrite in ArrayT
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
		final int kind = readKinds(t.getKind(), getKind());
		if ((kind & Kind.REF.getKind()) == 0) {
			return kind != 0;
		}
		// assignableFrom(T.REF) is true, null is T.REF!
		// may be better to check for null const in R instead of this general answer
		if (null == getDu() || is(Object.class) || t.getDu() == null) {
			return true;
		}
		// raise step by step in hierarchy...lazy fetch unknown super
		final LinkedList<T> ts = new LinkedList<T>();
		ts.add(t);
		while (!ts.isEmpty()) {
			final T iT = ts.pollFirst();
			final T superT = iT.getSuperT();
			if (superT == this || superT instanceof ParamT
					&& ((ParamT) superT).getGenericT() == this) {
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
		return false;
	}

	public boolean isInterface() {
		return false;
	}

	/**
	 * Is multi type?
	 * 
	 * @return true - is multi type
	 */
	public boolean isMulti() {
		return false; // only base types can be multi types, overwrite in BaseT
	}

	/**
	 * Is Object type?
	 * 
	 * @return true - is Object type
	 */
	public boolean isObject() {
		return false; // only Class Types can be Object type, overwrite in TD
	}

	/**
	 * Is primitive?
	 * 
	 * @return true - is primitive
	 */
	public boolean isPrimitive() {
		return false; // only Base Types can be Primitives, overwrite in BaseT
	}

	/**
	 * Is reference type (includes array type, parameterized type)?
	 * 
	 * @return true - is reference type
	 */
	public boolean isRef() {
		return true; // only base types can be none-refs, overwrite BaseT
	}

	/**
	 * Is resolveable?
	 * 
	 * @return true - is resolveable
	 */
	public boolean isResolvable() {
		return true; // only class types can be unresolveable, overwrite TD
	}

	/**
	 * Is wide type?
	 * 
	 * @return true - is wide type
	 */
	public boolean isWide() {
		return false; // only base types can be wide, overwrite BaseT
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
		final int kind = readKinds(getKind(), t.getKind());
		if ((kind & Kind.REF.getKind()) == 0) {
			return getT(kind);
		}

		if (t.isAssignableFrom(this)) {
			return this;
		}
		return null;
	}

	@Override
	public String toString() {
		// getSimpleName() not possible, potentially needs unresolved attributes, e.g. enclosing
		return this.name;
	}

}