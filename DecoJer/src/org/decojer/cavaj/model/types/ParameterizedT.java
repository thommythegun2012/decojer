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

import javax.annotation.Nonnull;

import lombok.Getter;

/**
 * Parameterized type.
 *
 * @since JVM 5
 *
 * @author André Pankraz
 */
public final class ParameterizedT extends ExtendedT {

	/**
	 * Type arguments for matching type parameters.
	 */
	@Getter
	private final T[] typeArgs;

	/**
	 * Constructor.
	 *
	 * @param genericT
	 *            generic type with matching type parameters
	 * @param typeArgs
	 *            type arguments for matching type parameters
	 */
	public ParameterizedT(@Nonnull final T genericT, @Nonnull final T[] typeArgs) {
		super(genericT);
		// we have to use the raw name here, not name<typeArgs>, else many enclosing-dependant stuff
		// will not work, like getT() for enclosed, getSimpleName() etc.,
		// cannot cache this anyway because of type variables

		assert typeArgs.length > 0;
		assert !genericT.isAnnotated() : "Anno(Param(t, args)) is same like Param(Anno(t), args), prefer first";

		this.typeArgs = typeArgs;
	}

	@Override
	public String getFullName() {
		final StringBuilder sb = new StringBuilder(getGenericT().getFullName()).append('<');
		for (final T typeArg : getTypeArgs()) {
			sb.append(typeArg).append(',');
		}
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();
	}

	@Override
	public T getGenericT() {
		return getRawT();
	}

	@Override
	public boolean isParameterized() {
		return true;
	}

}