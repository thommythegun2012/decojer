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

import org.decojer.cavaj.model.A;

/**
 * Annotated type.
 *
 * @since JVM 8, see JSR 308
 *
 * @author André Pankraz
 */
public class AnnotatedT extends ExtendedT {

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
	@Nonnull
	public AnnotatedT(@Nonnull final T t, @Nonnull final A[] as) {
		super(t);
		// we have to use the raw name here, not @annotations name, else many enclosing-dependant
		// stuff will not work, like getT() for enclosed, getSimpleName() etc.,
		// cannot cache this anyway because of lazy application of type annotations

		assert as.length > 0;

		this.as = as;
	}

	@Override
	public String getFullName() {
		final StringBuilder sb = new StringBuilder();
		for (final A a : this.as) {
			sb.append('@').append(a).append(' ');
		}
		final String ret = sb.append(getRawT().getFullName()).toString();
		assert ret != null;
		return ret;
	}

	@Override
	public boolean isAnnotated() {
		return true;
	}

	@Override
	public void setComponentT(final T componentT) {
		getRawT().setComponentT(componentT);
	}

	@Override
	public void setInterfaceTs(final T[] interfaceTs) {
		getRawT().setInterfaceTs(interfaceTs);
	}

	@Override
	public void setSuperT(final T t) {
		getRawT().setSuperT(t);
	}

}