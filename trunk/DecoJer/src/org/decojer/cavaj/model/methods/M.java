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
package org.decojer.cavaj.model.methods;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.types.T;

/**
 * Method.
 *
 * @author André Pankraz
 */
public abstract class M implements Element {

	public static final String CONSTRUCTOR_NAME = "<init>";

	public static final String INITIALIZER_NAME = "<clinit>";

	/**
	 * Create method declaration for this method.
	 *
	 * @return {@code true} - success
	 */
	public abstract boolean createMd();

	/**
	 * Get annotation default value.
	 *
	 * @return annotation default value
	 */
	public abstract Object getAnnotationDefaultValue();

	/**
	 * Get control flow graph.
	 *
	 * @return control flow graph
	 */
	public abstract CFG getCfg();

	/**
	 * Get descriptor. Unique in owner context.
	 *
	 * @return descriptor
	 */
	public abstract String getDescriptor();

	@Override
	public DU getDu() {
		final T t = getT();
		return t == null ? null : t.getDu();
	}

	/**
	 * Get method parameter annotations.
	 *
	 * @return method parameter annotations
	 */
	@Nullable
	public abstract A[][] getParamAss();

	/**
	 * Get parameter name for index.
	 *
	 * Dalvik provides this information directly, the JVM indirect via the local variable table.
	 * Could also be extracted from JavaDoc etc.
	 *
	 * @param i
	 *            index (starts with 0, double/long params count as 1)
	 * @return parameter name
	 */
	@Nonnull
	public abstract String getParamName(final int i);

	/**
	 * get parameter types.
	 *
	 * @return parameter types
	 */
	@Nonnull
	public abstract T[] getParamTs();

	/**
	 * Get receiver-type (this) for none-static methods.
	 *
	 * @return receiver-type
	 */
	@Nullable
	public abstract T getReceiverT();

	/**
	 * Get return type.
	 *
	 * @return return type
	 */
	@Nonnull
	public abstract T getReturnT();

	/**
	 * Get owner type, {@code null} for dynamic.
	 *
	 * @return owner type
	 */
	@Nullable
	public abstract T getT();

	/**
	 * Get throws types.
	 *
	 * @return throws types
	 */
	@Nonnull
	public abstract T[] getThrowsTs();

	/**
	 * Get type parameters.
	 *
	 * @return type parameters
	 */
	@Nonnull
	public abstract T[] getTypeParams();

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
	 * Is initializer?
	 *
	 * @return {@code true} - is constructor
	 */
	public boolean isInitializer() {
		return false;
	}

	/**
	 * Is method with final varargs parameter?
	 *
	 * @return {@code true} - is method with final varargs parameter
	 */
	public abstract boolean isVarargs();

	/**
	 * Set annotation default value..
	 *
	 * @param annotationDefaultValue
	 *            annotation default value
	 */
	public abstract void setAnnotationDefaultValue(@Nullable final Object annotationDefaultValue);

	/**
	 * Set control flow graph.
	 *
	 * @param cfg
	 *            control flow graph
	 */
	public abstract void setCfg(final CFG cfg);

	/**
	 * Set method parameter annotations.
	 *
	 * @param paramAss
	 *            method parameter annotations
	 */
	public abstract void setParamAss(final A[][] paramAss);

	/**
	 * Set parameter name.
	 *
	 * Dalvik provides this information directly, the JVM indirect via the local variable table.
	 * Could also be extracted from JavaDoc etc.
	 *
	 * @param i
	 *            index
	 * @param name
	 *            parameter name
	 */
	public abstract void setParamName(final int i, final String name);

	/**
	 * Set qualifier type for qualified method.
	 *
	 * For annotation application.
	 *
	 * @param qualifierT
	 *            qualifier type for qualified method
	 */
	public void setQualifierT(@Nonnull final T qualifierT) {
		assert false; // overwrite in QualifiedM
	}

	/**
	 * Set receiver type (this) for none-static methods.
	 *
	 * @param receiverT
	 *            receiver type
	 * @return {@code true} - success
	 */
	public abstract boolean setReceiverT(@Nullable final T receiverT);

	/**
	 * Set return type.
	 *
	 * @param returnT
	 *            return type
	 */
	public void setReturnT(@Nonnull final T returnT) {
		assert false; // overwrite in ClassM
	}

	/**
	 * Method must be static or dynamic (from usage, e.g. invoke).
	 *
	 * @param f
	 *            {@code true} - is static
	 */
	public abstract void setStatic(final boolean f);

	/**
	 * Set throws types.
	 *
	 * @param throwsTs
	 *            throws types
	 */
	public abstract void setThrowsTs(@Nullable final T[] throwsTs);

	/**
	 * Get signature.
	 *
	 * @return signature
	 */
	@Nullable
	public abstract String getSignature();

}