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

import javassist.bytecode.FieldInfo;

import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;

/**
 * Field declaration.
 * 
 * @author André Pankraz
 */
public class FD implements BD, PD {

	private BodyDeclaration fieldDeclaration;

	private final FieldInfo fieldInfo;

	private Expression initializer;

	private final TD td;

	/**
	 * Constructor.
	 * 
	 * @param td
	 *            type declaration
	 * @param fieldInfo
	 *            field info
	 */
	public FD(final TD td, final FieldInfo fieldInfo) {
		assert td != null;
		assert fieldInfo != null;

		this.td = td;
		this.fieldInfo = fieldInfo;
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
	 * Get field info.
	 * 
	 * @return field info
	 */
	public FieldInfo getFieldInfo() {
		return this.fieldInfo;
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
	 * Set Eclipse field declaration.
	 * 
	 * (FieldDeclaration or EnumConstantDeclaration)
	 * 
	 * @param fieldDeclaration
	 *            Eclipse field declaration
	 */
	public void setFieldDeclaration(final BodyDeclaration fieldDeclaration) {
		assert fieldDeclaration != null;

		this.fieldDeclaration = fieldDeclaration;
	}

}