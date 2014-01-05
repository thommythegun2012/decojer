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

import org.decojer.cavaj.model.T;

/**
 * Parameterized type.
 * 
 * @since JVM 5
 * 
 * @author André Pankraz
 */
public final class ParamT extends ModT {

	/**
	 * Type argument.
	 * 
	 * We don't follow the often used {@code WildcardType} paradigma. Wildcards are only allowed in
	 * the context of parameterized types and aren't useable as standalone Types.
	 * 
	 * @author André Pankraz
	 */
	@Getter
	public static final class TypeArg {

		public enum Kind {

			MATCH,

			UNBOUND,

			SUBCLASS_OF,

			SUPER_OF

		}

		public static TypeArg subclassOf(final T t) {
			assert t != null;

			return new TypeArg(t, Kind.SUBCLASS_OF);
		}

		public static TypeArg superOf(final T t) {
			assert t != null;

			return new TypeArg(t, Kind.SUPER_OF);
		}

		private final Kind kind;

		// cannot be final because of later adding type arguments
		private final T t;

		public TypeArg() {
			this(null, Kind.UNBOUND);
		}

		public TypeArg(final T t) {
			this(t, Kind.MATCH);

			assert t != null;
		}

		public TypeArg(final T t, final Kind kind) {
			this.t = t;
			this.kind = kind;
		}

		@Override
		public String toString() {
			switch (this.kind) {
			case MATCH:
				return this.t.toString();
			case SUBCLASS_OF:
				return "? extends " + this.t;
			case SUPER_OF:
				return "? super " + this.t;
			case UNBOUND:
				return "?";
			}
			return "???";
		}

	}

	/**
	 * Type arguments for matching type parameters.
	 */
	@Getter
	private final TypeArg[] typeArgs;

	/**
	 * Constructor.
	 * 
	 * @param genericT
	 *            generic type with matching type parameters
	 * @param typeArgs
	 *            type arguments for matching type parameters
	 */
	public ParamT(final T genericT, final TypeArg[] typeArgs) {
		super(genericT.getName(), genericT);
		// we have to use the raw name here, not name<typeArgs>, else many enclosing-dependant stuff
		// will not work, like getT() for enclosed, getSimpleName() etc.,
		// cannot cache this anyway because of type variables

		this.typeArgs = typeArgs;
	}

	/**
	 * Get generic type.
	 * 
	 * @return generic type
	 */
	public T getGenericT() {
		return getRawT();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(getGenericT().toString()).append('<');
		for (final TypeArg typeArg : getTypeArgs()) {
			sb.append(typeArg).append(',');
		}
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();
	}

}