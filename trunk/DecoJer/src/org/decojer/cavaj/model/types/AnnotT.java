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

import lombok.Getter;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.T;

/**
 * Annotated type.
 * 
 * @since JVM 8, see JSR 308
 * 
 * @author André Pankraz
 */
public class AnnotT extends T {

	private static String toString(final T t, final A[] as) {
		final StringBuilder sb = new StringBuilder(t.getName());
		for (final A a : as) {
			sb.append('@').append(a).append(' ');
		}
		return sb.substring(0, sb.length() - 1);
	}

	/**
	 * Type.
	 */
	@Getter
	private final T t;

	/**
	 * Type annotations.
	 */
	@Getter
	private final A[] as;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param as
	 *            type annotations
	 */
	public AnnotT(final T t, final A[] as) {
		super(toString(t, as));

		this.t = t;
		this.as = as;
	}

}