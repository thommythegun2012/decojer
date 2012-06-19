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
package org.decojer.cavaj.model.type;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Parameterized type.
 * 
 * @author André Pankraz
 */
@Getter
public class ParamT extends T {

	@Getter
	public static class TypeArg {

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

		public TypeArg(final T t, final Kind kind) {
			this.t = t;
			this.kind = kind;
		}

	}

	/**
	 * Generic object type with matching type parameters.
	 */
	private final T genericT;

	/**
	 * Type arguments for type parameters.
	 */
	private final TypeArg[] typeArgs;

	/**
	 * Constructor.
	 * 
	 * @param genericT
	 *            generic object type with type parameters
	 * @param typeArgs
	 *            type arguments for type parameters
	 */
	public ParamT(final T genericT, final TypeArg[] typeArgs) {
		super(genericT.getDu(), genericT.getName() + "_G_"); // TODO

		this.genericT = genericT;
		this.typeArgs = typeArgs;
	}

}