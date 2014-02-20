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
 * Method.
 * 
 * @author André Pankraz
 */
public abstract class M {

	public static final String CONSTRUCTOR_NAME = "<init>";

	public static final String INITIALIZER_NAME = "<clinit>";

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return {@code true} - is access flag
	 */
	public abstract boolean check(final AF af);

	/**
	 * Create method declaration for this method.
	 * 
	 * @return method declaration
	 */
	public abstract MD createMd();

	/**
	 * Get descriptor. Unique in owner context.
	 * 
	 * @return descriptor
	 */
	public abstract String getDescriptor();

	/**
	 * Get method declaration.
	 * 
	 * @return method declaration
	 */
	public abstract MD getMd();

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	public abstract String getName();

	/**
	 * get parameter types.
	 * 
	 * @return parameter types
	 */
	public abstract T[] getParamTs();

	/**
	 * Get receiver-type (this) for none-static methods.
	 * 
	 * @return receiver-type
	 */
	public abstract T getReceiverT();

	/**
	 * Get return type.
	 * 
	 * @return return type
	 */
	public abstract T getReturnT();

	/**
	 * Get owner type.
	 * 
	 * @return owner type
	 */
	public abstract T getT();

	/**
	 * Is constructor?
	 * 
	 * @return {@code true} - is constructor
	 */
	public abstract boolean isConstructor();

	/**
	 * Is dynamic?
	 * 
	 * @return {@code true} - is dynamic
	 */
	public abstract boolean isDynamic();

	/**
	 * Is static method?
	 * 
	 * @return {@code true} - is static method
	 */
	public abstract boolean isStatic();

	/**
	 * Is synthetic method?
	 * 
	 * @return {@code true} - is synthetic method
	 */
	public abstract boolean isSynthetic();

	/**
	 * Is method with final varargs parameter?
	 * 
	 * @return {@code true} - is method with final varargs parameter
	 */
	public abstract boolean isVarargs();

	/**
	 * Set raw method for modified method.
	 * 
	 * For type annotation application.
	 * 
	 * @param rawM
	 *            raw method for modified method
	 */
	public void setRawM(final M rawM) {
		assert false;
	}

	/**
	 * Method must be static or dynamic (from usage, e.g. invoke).
	 * 
	 * @param f
	 *            {@code true} - is static
	 */
	public abstract void setStatic(final boolean f);

	/**
	 * Set owner type (for applying type annotations).
	 * 
	 * @param t
	 *            owner type
	 */
	public void setT(final T t) {
		assert false; // overwrite in QualifiedM
	}

}