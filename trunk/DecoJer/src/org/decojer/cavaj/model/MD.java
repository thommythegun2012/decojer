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

import org.decojer.cavaj.model.code.CFG;
import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Method Declaration.
 * 
 * @author André Pankraz
 */
public final class MD implements BD, PD {

	/**
	 * Annotation Default Value.
	 */
	@Getter
	@Setter
	private Object annotationDefaultValue;

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Control Flow Graph.
	 */
	@Getter
	@Setter
	private CFG cfg;

	/**
	 * Deprecated State (from Deprecated Attribute).
	 */
	@Getter
	@Setter
	private boolean deprecated;

	/**
	 * Method.
	 */
	@Getter
	private final M m;

	/**
	 * AST Method Declaration.
	 */
	@Getter
	@Setter
	private BodyDeclaration methodDeclaration;

	/**
	 * Method Parameter Annotations.
	 */
	@Getter
	@Setter
	private A[][] paramAss;

	/**
	 * Synthetic State (from Synthetic Attribute)
	 */
	@Getter
	@Setter
	private boolean synthetic;

	/**
	 * Owner Type Declaration.
	 */
	@Getter
	private final TD td;

	/**
	 * Constructor.
	 * 
	 * @param m
	 *            method
	 * @param td
	 *            type declaration
	 */
	public MD(final M m, final TD td) {
		assert m != null;
		assert td != null;

		this.m = m;
		this.td = td;
	}

	@Override
	public String toString() {
		return getM().toString();
	}

}