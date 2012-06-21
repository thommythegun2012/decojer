/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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
package org.decojer.cavaj.model.type;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Parameterized Type.
 * 
 * @author Andr� Pankraz
 */
@Getter
public final class ParamT extends T {

	/**
	 * Type Argument.
	 * 
	 * We don't follow the often used <code>WildcardType</code> paradigma. Wildcards are only
	 * allowed in the context of Parameterized Types and aren't useable as standalone Types.
	 * 
	 * @author Andr� Pankraz
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

		private final T t;

		public TypeArg() {
			this(null, Kind.UNBOUND);
		}

		public TypeArg(final T t) {
			this(t, Kind.MATCH);

			assert t != null;
		}

		private TypeArg(final T t, final Kind kind) {
			this.t = t;
			this.kind = kind;
		}

		@Override
		public String toString() {
			switch (this.kind) {
			case MATCH:
				return this.t.toString();
			case SUBCLASS_OF:
				return "? extends " + this.t.toString();
			case SUPER_OF:
				return "? super " + this.t.toString();
			case UNBOUND:
				return "?";
			}
			return "???";
		}

	}

	private static String toString(final T genericT, final TypeArg[] typeArgs) {
		final StringBuilder sb = new StringBuilder(genericT.getName()).append('<');
		for (final TypeArg typeArg : typeArgs) {
			sb.append(typeArg).append(',');
		}
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();
	}

	/**
	 * Generic Type with matching Type Parameters.
	 */
	private final T genericT;

	/**
	 * Type Arguments for matching Type Parameters.
	 */
	private final TypeArg[] typeArgs;

	/**
	 * Constructor.
	 * 
	 * @param genericT
	 *            Generic Type with matching Type Parameters
	 * @param typeArgs
	 *            Type Arguments for matching Type Parameters
	 */
	public ParamT(final T genericT, final TypeArg[] typeArgs) {
		super(genericT.getDu(), toString(genericT, typeArgs));

		this.genericT = genericT;
		this.typeArgs = typeArgs;
	}

}