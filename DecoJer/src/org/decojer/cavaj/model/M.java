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

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.Cursor;

/**
 * Method.
 * 
 * @author André Pankraz
 */
public class M {

	public static final String CONSTRUCTOR_NAME = "<init>";

	public static final String INITIALIZER_NAME = "<clinit>";

	@Setter
	private int accessFlags;

	@Getter
	private final String descriptor;

	@Getter
	private MD md;

	@Getter
	private final String name;

	@Getter
	@Setter
	private T[] paramTs;

	@Getter
	@Setter
	private T returnT;

	@Getter
	private T t;

	/**
	 * Constructor for dynamic method.
	 * 
	 * @param du
	 *            DU
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	protected M(final DU du, final String name, final String descriptor) {
		assert name != null;
		assert descriptor != null;

		this.t = null; // dynamic method
		this.name = name;
		this.descriptor = descriptor;

		final Cursor c = new Cursor();
		this.paramTs = du.parseMethodParamTs(descriptor, c, this);
		this.returnT = du.parseT(descriptor, c, this);

		setStatic(true); // dynamic callsite resolution, never reference on stack
	}

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	protected M(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.descriptor = descriptor;

		final Cursor c = new Cursor();
		this.paramTs = t.getDu().parseMethodParamTs(descriptor, c, this);
		this.returnT = t.getDu().parseT(descriptor, c, this);
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return {@code true} - is access flag
	 */
	public boolean check(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Create method declaration for this method.
	 * 
	 * @return method declaration
	 */
	public MD createMd() {
		assert this.md == null;

		this.md = new MD(this);
		this.t.getTd().addBd(this.md);
		return this.md;
	}

	/**
	 * Get receiver-type (this) for none-static methods.
	 * 
	 * @return receiver-type
	 * 
	 * @see M#setReceiverT(T)
	 */
	public T getReceiverT() {
		return this.t instanceof ClassT ? null : this.t;
	}

	/**
	 * Is constructor?
	 * 
	 * @return {@code true} - is constructor
	 */
	public boolean isConstructor() {
		return CONSTRUCTOR_NAME.equals(getName());
	}

	/**
	 * Is deprecated method, marked via Javadoc @deprecated?
	 * 
	 * @return {@code true} - is deprecated method
	 */
	public boolean isDeprecated() {
		return check(AF.DEPRECATED);
	}

	/**
	 * Is dynamic?
	 * 
	 * @return {@code true} - is dynamic
	 */
	public boolean isDynamic() {
		return this.t == null;
	}

	/**
	 * Is initializer?
	 * 
	 * @return {@code true} - is constructor
	 */
	public boolean isInitializer() {
		return INITIALIZER_NAME.equals(getName());
	}

	/**
	 * Is static method?
	 * 
	 * @return {@code true} - is static method
	 */
	public boolean isStatic() {
		return check(AF.STATIC);
	}

	/**
	 * Is synthetic method?
	 * 
	 * @return {@code true} - is synthetic method
	 */
	public boolean isSynthetic() {
		return check(AF.SYNTHETIC);
	}

	/**
	 * Is method with final varargs parameter?
	 * 
	 * @return {@code true} - is method with final varargs parameter
	 */
	public boolean isVarargs() {
		return check(AF.VARARGS);
	}

	/**
	 * Method must be deprecated (from Deprecated attribute, marked via Javadoc @deprecate).
	 */
	public void setDeprecated() {
		this.accessFlags |= AF.DEPRECATED.getValue();
	}

	/**
	 * Set receiver type (this) for none-static methods.
	 * 
	 * We reuse the owner type here, because this is a very rarely used feature, where we don't want
	 * to add memory per method.
	 * 
	 * @param receiverT
	 *            receiver type
	 * @return {@code true} - success
	 */
	public boolean setReceiverT(final T receiverT) {
		if (isStatic() || isDynamic()) {
			return false;
		}
		if (!getT().equals(receiverT)) {
			return false;
		}
		this.t = receiverT;
		return true;
	}

	/**
	 * Method must be static or dynamic (from usage, e.g. invoke).
	 * 
	 * @param f
	 *            {@code true} - is static
	 */
	public void setStatic(final boolean f) {
		if (f) {
			// static also possible in interface since JVM 8
			if ((this.accessFlags & AF.STATIC.getValue()) != 0) {
				return;
			}
			assert (this.accessFlags & AF.STATIC_ASSERTED.getValue()) == 0;

			this.accessFlags |= AF.STATIC.getValue() | AF.STATIC_ASSERTED.getValue();
			return;
		}
		assert (this.accessFlags & AF.STATIC.getValue()) == 0;

		this.accessFlags |= AF.STATIC_ASSERTED.getValue();
		return;
	}

	/**
	 * Method must be synthetic (from synthetic attribute).
	 */
	public void setSynthetic() {
		this.accessFlags |= AF.SYNTHETIC.getValue();
	}

	@Override
	public String toString() {
		return this.t + "." + this.name + this.descriptor;
	}

}