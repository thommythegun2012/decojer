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

	private final int accessFlags;

	private boolean deprecated;

	private final String descriptor;

	private BodyDeclaration fieldDeclaration;

	private Expression initializer;

	private final String name;

	private final String signature;

	private boolean synthetic;

	private final TD td;

	// value, type Integer: int, short, byte, char, boolean
	private final Object value;

	/**
	 * Constructor.
	 * 
	 * @param td
	 *            type declaration
	 * @param accessFlags
	 *            access flags
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 * @param signature
	 *            signature
	 * @param value
	 *            value
	 */
	public FD(final TD td, final int accessFlags, final String name,
			final String descriptor, final String signature, final Object value) {
		assert td != null;
		assert name != null;
		assert descriptor != null;

		this.td = td;
		this.accessFlags = accessFlags;
		this.name = name;
		this.descriptor = descriptor;
		this.signature = signature;
		this.value = value;
	}

	/**
	 * Get access flags.
	 * 
	 * @return access flags
	 */
	public int getAccessFlags() {
		return this.accessFlags;
	}

	/**
	 * Get descriptor.
	 * 
	 * @return descriptor
	 */
	public String getDescriptor() {
		return this.descriptor;
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
	 * Get name.
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get signature.
	 * 
	 * @return signature
	 */
	public String getSignature() {
		return this.signature;
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
		assert fieldDeclaration != null;

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

}