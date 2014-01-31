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
 * Qualified type. This is used for references with parameterization and annotations of enclosing
 * classes. We cannot set it in the unmodified class type enclosing info, because it can differ
 * between references.
 * 
 * @author André Pankraz
 */
public class QualifiedT extends ModT {

	/**
	 * Type qualifier, is like enclosing type in references.
	 */
	@Getter
	private final T qualifierT;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param qualifierT
	 *            type qualifier
	 */
	public QualifiedT(final T t, final T qualifierT) {
		super(t); // the qualified t is the raw t, because we inherit its properties

		assert t.getName().startsWith(qualifierT.getName());

		this.qualifierT = qualifierT;
	}

}