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

import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Field declaration.
 * 
 * @author André Pankraz
 */
public final class FD implements BD, PD {

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Deprecated state (from deprecated attribute).
	 */
	@Getter
	@Setter
	private boolean deprecated;

	/**
	 * Field.
	 */
	@Getter
	private final F f;

	/**
	 * AST field declaration.
	 */
	@Getter
	@Setter
	private BodyDeclaration fieldDeclaration;

	/**
	 * Synthetic state (from synthetic attribute).
	 */
	@Getter
	@Setter
	private boolean synthetic;

	/**
	 * Owner type declaration.
	 */
	@Getter
	private final TD td;

	/**
	 * Value for constant attributes or <code>null</code>. Type Integer: int, short, byte, char,
	 * boolean.
	 */
	@Getter
	@Setter
	private Object value;

	/**
	 * Constructor.
	 * 
	 * @param f
	 *            field
	 * @param td
	 *            type declaration
	 */
	public FD(final F f, final TD td) {
		assert f != null;
		assert td != null;

		this.f = f;
		this.td = td;
	}

	@Override
	public String toString() {
		return getF().toString();
	}

}