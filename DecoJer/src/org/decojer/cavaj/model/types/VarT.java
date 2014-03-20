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
 * Type variable.
 *
 * This type is used as type argument, but other types can be type arguments too, e.g. a ClassT or
 * an extension by WildcardT.
 *
 * Is not used in declaration like ParamT but is used for referencing it, should be resolved to a
 * ParamT, but can only be done lazy.
 *
 * @author André Pankraz
 */
public class VarT extends BaseT {

	/**
	 * Enclosing type context.
	 */
	@Getter
	private final Object context;

	@Getter
	@Nonnull
	private final String name;

	@Getter
	private T resolvedT;

	/**
	 * Constructor.
	 *
	 * @param name
	 *            type name
	 * @param context
	 *            enclosing type context
	 */
	public VarT(@Nonnull final String name, final Object context) {
		this.name = name;
		this.context = context;
	}

	@Override
	public boolean eraseTo(final T t) {
		if (getResolvedT() != null) {
			return getResolvedT().equals(t);
		}
		this.resolvedT = t;
		return true;
	}

	@Override
	public T[] getInterfaceTs() {
		return this.resolvedT == null ? INTERFACES_NONE : this.resolvedT.getInterfaceTs();
	}

	@Override
	public T getSuperT() {
		return this.resolvedT == null ? null : this.resolvedT.getSuperT();
	}

}