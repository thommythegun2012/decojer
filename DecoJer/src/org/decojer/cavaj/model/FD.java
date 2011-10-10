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

import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;

/**
 * Field declaration.
 * 
 * @author André Pankraz
 */
public class FD implements BD, PD {

	private A[] as;

	// deprecated state (from deprecated attribute)
	private boolean deprecated;

	private final F f;

	private BodyDeclaration fieldDeclaration;

	private Expression initializer;

	// synthetic state (from synthetic attribute)
	private boolean synthetic;

	private final TD td;

	// value, type Integer: int, short, byte, char, boolean
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

	/**
	 * Get annotations.
	 * 
	 * @return annotations
	 */
	public A[] getAs() {
		return this.as;
	}

	/**
	 * Get field.
	 * 
	 * @return field
	 */
	public F getF() {
		return this.f;
	}

	/**
	 * Get Eclipse field declaration.
	 * 
	 * @return Eclipse field declaration
	 */
	public BodyDeclaration getFieldDeclaration() {
		return this.fieldDeclaration;
	}

	/**
	 * Get initializer expression.
	 * 
	 * @return initializer expression
	 */
	public Expression getInitializer() {
		return this.initializer;
	}

	/**
	 * Get type declaration.
	 * 
	 * @return type declaration, not null
	 */
	public TD getTd() {
		return this.td;
	}

	/**
	 * Get value, type Integer: int, short, byte, char, boolean.
	 * 
	 * @return value
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Get deprecated state (from deprecated attribute).
	 * 
	 * @return true - deprecated
	 */
	public boolean isDeprecated() {
		return this.deprecated;
	}

	/**
	 * Get synthetic state (from synthetic attribute).
	 * 
	 * @return true - synthetic
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * Set annotations.
	 * 
	 * @param as
	 *            annotations
	 */
	public void setAs(final A[] as) {
		this.as = as;
	}

	/**
	 * Set deprecated state (from deprecated attribute).
	 * 
	 * @param deprecated
	 *            true - deprecated
	 */
	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	/**
	 * Set Eclipse field declaration.
	 * 
	 * (FieldDeclaration or EnumConstantDeclaration)
	 * 
	 * @param fieldDeclaration
	 *            Eclipse field declaration
	 */
	public void setFieldDeclaration(final BodyDeclaration fieldDeclaration) {
		this.fieldDeclaration = fieldDeclaration;
	}

	/**
	 * Set synthetic state (from synthetic attribute).
	 * 
	 * @param synthetic
	 *            true - synthetic
	 */
	public void setSynthetic(final boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * Set value.
	 * 
	 * @param value
	 *            value
	 */
	public void setValue(final Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getF().toString();
	}

}