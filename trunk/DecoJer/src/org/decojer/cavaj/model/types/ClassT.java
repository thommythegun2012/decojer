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

	private final static String ANONYMOUS = "<ANONYMOUS>";

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

	@Getter
	private String innerName;

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
		return isResolveable() && (this.accessFlags & af.getValue()) != 0;
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

	/**
	 * Get enclosing method (including constructor).
	 * 
	 * @return enclosing method
	 * 
	 * @see Class#getEnclosingMethod()
	 * @see Class#getEnclosingConstructor()
	 */
	public M getEnclosingM() {
		return isResolveable() && this.enclosing instanceof M ? (M) this.enclosing : null;
	}

	/**
	 * Get enclosing type.
	 * 
	 * @return enclosing type
	 * 
	 * @see Class#getEnclosingClass()
	 */
	public ClassT getEnclosingT() {
		return isResolveable() && this.enclosing instanceof ClassT ? (ClassT) this.enclosing : null;
	}

	@Override
	public T[] getInterfaceTs() {
		return isResolveable() ? this.interfaceTs : T.NO_INTERFACES;
	}

	@Override
	public int getKind() {
		return Kind.REF.getKind();
	}

	@Override
	public T getSuperT() {
		return isResolveable() ? this.superT : null;
	}

	public T[] getTypeParams() {
		return isResolveable() ? this.typeParams : null;
	}

	public boolean isAnonymousClass() {
		return this.innerName == ANONYMOUS;
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
	 * Is unresolveable?
	 * 
	 * @return true - is unresolveable
	 */
	@Override
	public boolean isResolveable() {
		if (this.interfaceTs != null) {
			// don't use check(AF.UNRESOLVEABLE) -> endless loop
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
				this.superT = getDu().getT(superclass.getName());
			}

			final Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces.length == 0) {
				this.interfaceTs = NO_INTERFACES;
			} else {
				final T[] interfaceTs = new T[interfaces.length];
				for (int i = interfaces.length; i-- > 0;) {
					interfaceTs[i] = getDu().getT(interfaces[i].getName());
				}
				this.interfaceTs = interfaceTs;
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
			LOGGER.warning("Couldn't load type : " + this);
			this.interfaceTs = NO_INTERFACES;
			markAf(AF.UNRESOLVEABLE);
			return false;
		}
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
	 * We mix here declaring classes info and enclosing method / classes info.
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
	 * but JRE < 5 has no enclosing method attribute,<br>
	 * JRE 1.1 has normal outer for anonymous/local, like declaring for JRE 5,<br>
	 * JRE 1.2 .. 1.4 has no outer info at all!!!
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
		final String innerName = name == null ? ANONYMOUS : name;
		if (this.innerName != null) {
			if (!this.innerName.equals(innerName)) {
				LOGGER.warning("Inner name cannot be changed from '" + this.innerName + "' to '"
						+ name + "'!");
			}
			return;
		}
		this.innerName = innerName;
		/*
		 * if (this.accessFlags != 0) { if (this.accessFlags != accessFlags) {
		 * LOGGER.warning("Inner access flags cannot be changed from '" + this.accessFlags +
		 * "' to '" + accessFlags + "'!"); } return; } this.accessFlags = accessFlags;
		 */
	}

}