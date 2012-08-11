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
package org.decojer.cavaj.model.types;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;

/**
 * Class type.
 * 
 * @author André Pankraz
 */
public class ClassT extends T {

	private final static Logger LOGGER = Logger.getLogger(ClassT.class.getName());

	private static String toString(final T superT, final T[] interfaceTs) {
		final StringBuilder sb = new StringBuilder("{");
		if (superT != null) {
			sb.append(superT.getName()).append(',');
		}
		for (final T interfaceT : interfaceTs) {
			sb.append(interfaceT.getName()).append(",");
		}
		sb.setCharAt(sb.length() - 1, '}');
		return sb.toString();
	}

	/**
	 * Access flags.
	 */
	@Setter
	private int accessFlags;

	@Getter
	private final DU du;

	/**
	 * We mix here declaring classes info and enclosing method / classes info.
	 * 
	 * @see ClassT#setEnclosingT(ClassT)
	 */
	private Object enclosing;

	@Setter
	private T[] interfaceTs;

	/**
	 * Super type.
	 */
	@Setter
	private T superT;

	@Getter
	private TD td;

	/**
	 * Type parameters. (They define the useable type variables)
	 */
	@Setter
	private T[] typeParams;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            type name
	 * @param du
	 *            decompilation unit
	 */
	public ClassT(final String name, final DU du) {
		super(name);

		assert du != null;

		this.du = du;
	}

	public ClassT(final T superT, final T[] interfaceTs) {
		super(toString(superT, interfaceTs));

		this.du = superT.getDu();
		this.superT = superT;
		this.interfaceTs = interfaceTs;
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean check(final AF af) {
		if (this.accessFlags == 0) {
			resolve();
		}
		return (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Create type declaration for this type.
	 * 
	 * @return type declaration
	 */
	public TD createTd() {
		assert this.td == null;

		this.td = new TD(this);
		return this.td;
	}

	private Object getEnclosing() {
		if (this.enclosing == null) {
			resolve();
		}
		return this.enclosing == NONE ? null : this.enclosing;
	}

	/**
	 * Get enclosing method (including constructor).
	 * 
	 * @return enclosing method
	 * 
	 * @see ClassT#setEnclosingT(ClassT)
	 * @see Class#getEnclosingMethod()
	 * @see Class#getEnclosingConstructor()
	 */
	public M getEnclosingM() {
		final Object enclosing = getEnclosing();
		return enclosing instanceof M ? (M) enclosing : null;
	}

	@Override
	public ClassT getEnclosingT() {
		final Object enclosing = getEnclosing();
		// like Class#getEnclosingClass()
		if (enclosing instanceof M) {
			return (ClassT) ((M) enclosing).getT();
		}
		return enclosing instanceof ClassT ? (ClassT) enclosing : null;
	}

	@Override
	public T[] getInterfaceTs() {
		if (this.interfaceTs == null) {
			resolve();
		}
		return this.interfaceTs;
	}

	@Override
	public int getKind() {
		return Kind.REF.getKind();
	}

	@Override
	public T getSuperT() {
		if (this.superT == null) {
			resolve();
		}
		// can be null, e.g. for Object.class
		return this.superT == NONE ? null : this.superT;
	}

	@Override
	public T[] getTypeParams() {
		if (this.typeParams == null) {
			resolve();
		}
		return this.typeParams;
	}

	@Override
	public boolean isInterface() {
		return check(AF.INTERFACE);
	}

	@Override
	public boolean isObject() {
		return Object.class.getName().equals(getName());
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
	 * Is unresolveable?
	 * 
	 * @return true - is unresolveable
	 */
	@Override
	public boolean resolve() {
		if ((this.accessFlags & AF.UNRESOLVEABLE.getValue()) != 0) {
			return false;
		}
		// try simple class loading, may be we are lucky ;)
		// TODO later ask DecoJer-online and local type cache with context info
		try {
			final Class<?> clazz = getClass().getClassLoader().loadClass(getName());
			this.accessFlags = clazz.getModifiers();

			final Class<?> superclass = clazz.getSuperclass();
			if (superclass != null) {
				this.superT = getDu().getT(superclass.getName());
			}

			final Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces.length > 0) {
				final T[] interfaceTs = new T[interfaces.length];
				for (int i = interfaces.length; i-- > 0;) {
					interfaceTs[i] = getDu().getT(interfaces[i].getName());
				}
				this.interfaceTs = interfaceTs;
			}

			final TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
			if (typeParameters.length > 0) {
				final T[] typeParams = new T[typeParameters.length];
				for (int i = typeParameters.length; i-- > 0;) {
					typeParams[i] = getDu().getT(typeParameters[i].getName());
				}
				this.typeParams = typeParams;
			}

			final Method enclosingMethod = clazz.getEnclosingMethod();
			if (enclosingMethod != null) {
				final Class<?> declaringClass = enclosingMethod.getDeclaringClass();
				final T methodT = this.du.getT(declaringClass);
				// TODO difficult...have only generic types here, not original descriptor
				this.enclosing = methodT.getM(enclosingMethod.getName(), "<TODO>");
			}
			final Class<?> enclosingClass = clazz.getEnclosingClass();
			if (enclosingClass != null) {
				this.enclosing = this.du.getT(enclosingClass);
			}
			return true;
		} catch (final ClassNotFoundException e) {
			markAf(AF.UNRESOLVEABLE);
			this.interfaceTs = INTERFACES_NONE;
			LOGGER.warning("Couldn't load type : " + getName());
			return false;
		} finally {
			resolveFill();
		}
	}

	public void resolveFill() {
		if (this.superT == null) {
			this.superT = NONE; // Object.class has no super!
		}
		if (this.interfaceTs == null) {
			this.interfaceTs = INTERFACES_NONE;
		}
		if (this.typeParams == null) {
			this.typeParams = TYPE_PARAMS_NONE;
		}
		if (this.enclosing == null) {
			this.enclosing = NONE;
		}
	}

	/**
	 * Set enclosing method (since JVM 5).
	 * 
	 * @param m
	 *            method
	 * 
	 * @see ClassT#setEnclosingT(ClassT)
	 */
	public void setEnclosingM(final M m) {
		if (this.enclosing != null) {
			if (this.enclosing != m) {
				LOGGER.warning("Enclosing method cannot be changed from '" + this.enclosing
						+ "' to '" + m + "'!");
			}
			return;
		}
		this.enclosing = m;
	}

	/**
	 * Set enclosing class type (since JRE 5).
	 * 
	 * There are five kinds of classes (or interfaces):<br>
	 * 
	 * a) Top level classes<br>
	 * b) Nested classes (static member classes)<br>
	 * c) Inner classes (non-static member classes)<br>
	 * d) Local classes (named classes declared within a method)<br>
	 * e) Anonymous classes<br>
	 * 
	 * JVM Spec 4.8.6: A class must have an EnclosingMethod attribute if and only if it is a local
	 * class or an anonymous class.<br>
	 * 
	 * We mix declaring classes info and enclosing method / classes attribut info.<br>
	 * 
	 * JRE 5 has enclosing method attribute for local/anonymous, outer info only for declaring outer<br>
	 * JRE < 5 has no enclosing method attribute and:<br>
	 * JRE 1.1 has normal outer info for anonymous/local, like declaring for JRE 5,<br>
	 * JRE 1.2 .. 1.4 has no outer info at all!!!
	 * 
	 * We cannot ignore this information and rely on naming rules, because the separator '$' is a
	 * valid character in none-inner type names.
	 * 
	 * @param t
	 *            class type
	 * 
	 * @see Class#getEnclosingClass()
	 */
	public void setEnclosingT(final ClassT t) {
		if (this.enclosing != null) {
			if (this.enclosing != t) {
				LOGGER.warning("Enclosing type cannot be changed from '" + this.enclosing
						+ "' to '" + t + "'!");
			}
			return;
		}
		this.enclosing = t;
	}

	/**
	 * Set inner info.
	 * 
	 * @param name
	 *            inner name
	 * @param accessFlags
	 *            inner access flags
	 */
	public void setInnerInfo(final String name, final int accessFlags) {
		// this inner access flags have exclusively following modifiers: PROTECTED, PRIVATE, STATIC,
		// but not: SUPER
		this.accessFlags = accessFlags | this.accessFlags & AF.SUPER.getValue();
		// don't really need this info (@see T#getSimpleName()):
		// According to JLS3 "Binary Compatibility" (13.1) the binary
		// name of non-package classes (not top level) is the binary
		// name of the immediately enclosing class followed by a '$' followed by:
		// (for nested and inner classes): the simple name.
		// (for local classes): 1 or more digits followed by the simple name.
		// (for anonymous classes): 1 or more digits.
		if (this.enclosing != null) {
			final String simpleName = getSimpleName();
			if (name == null && simpleName.isEmpty() || simpleName.equals(name)) {
				return;
			}
			LOGGER.warning("Inner name '" + name + "' is different from enclosing info '"
					+ getSimpleName() + "'!");
		}
	}

}