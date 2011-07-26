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

/**
 * Method declaration.
 * 
 * @author André Pankraz
 */
public class MD implements BD, PD {

	private final int accessFlags;

	private Object annotationDefaultValue;

	private CFG cfg;

	private boolean deprecated;

	private final String descriptor;

	private final String[] exceptions;

	private A[] invisibleAs;

	private A[][] invisibleParamAs;

	private BodyDeclaration methodDeclaration;

	private final String name;

	private final String signature;

	private boolean synthetic;

	private final TD td;

	private A[] visibleAs;

	private A[][] visibleParamAs;

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
	 * @param exceptions
	 *            exceptions
	 */
	public MD(final TD td, final int accessFlags, final String name,
			final String descriptor, final String signature,
			final String[] exceptions) {
		assert td != null;
		assert name != null;
		assert descriptor != null;

		this.td = td;
		this.accessFlags = accessFlags;
		this.name = name;
		this.descriptor = descriptor;
		this.signature = signature;
		this.exceptions = exceptions;
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
	 * Get annotation default value.
	 * 
	 * @return annotation default value
	 */
	public Object getAnnotationDefaultValue() {
		return this.annotationDefaultValue;
	}

	/**
	 * Get control flow graph.
	 * 
	 * @return control flow graph or null
	 */
	public CFG getCfg() {
		return this.cfg;
	}

	/**
	 * Get method descriptor.
	 * 
	 * @return method descriptor
	 */
	public String getDescriptor() {
		return this.descriptor;
	}

	/**
	 * Get exceptions.
	 * 
	 * @return exceptions
	 */
	public String[] getExceptions() {
		return this.exceptions;
	}

	public A[] getInvisibleAs() {
		return this.invisibleAs;
	}

	public A[][] getInvisibleParamAs() {
		return this.invisibleParamAs;
	}

	/**
	 * Get Eclipse method declaration.
	 * 
	 * @return Eclipse method declaration
	 */
	public BodyDeclaration getMethodDeclaration() {
		return this.methodDeclaration;
	}

	/**
	 * Get method name.
	 * 
	 * @return method name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get method signature.
	 * 
	 * @return method signature
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

	public A[] getVisibleAs() {
		return this.visibleAs;
	}

	public A[][] getVisibleParamAs() {
		return this.visibleParamAs;
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
	 * Set annotation default value.
	 * 
	 * @param annotationDefaultValue
	 *            annotation default value
	 */
	public void setAnnotationDefaultValue(final Object annotationDefaultValue) {
		this.annotationDefaultValue = annotationDefaultValue;
	}

	/**
	 * Set control flow graph.
	 * 
	 * @param cfg
	 *            control flow graph
	 */
	public void setCFG(final CFG cfg) {
		this.cfg = cfg;
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

	public void setInvisibleAs(final A[] invisibleAs) {
		this.invisibleAs = invisibleAs;
	}

	public void setInvisibleParamAs(final A[][] invisibleParamAs) {
		this.invisibleParamAs = invisibleParamAs;
	}

	/**
	 * Set Eclipse method declaration.
	 * 
	 * @param methodDeclaration
	 *            Eclipse method declaration
	 */
	public void setMethodDeclaration(final BodyDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
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

	public void setVisibleAs(final A[] visibleAs) {
		this.visibleAs = visibleAs;
	}

	public void setVisibleParamAs(final A[][] visibleParamAs) {
		this.visibleParamAs = visibleParamAs;
	}

	@Override
	public String toString() {
		return getTd().toString() + '.' + getSignature();
	}

}