/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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

import javassist.bytecode.CodeAttribute;

import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Method declaration.
 * 
 * @author Andr� Pankraz
 */
public class MD implements BD, PD {

	private int accessFlags;

	private Object annotationDefaultValue;

	private A[] as;

	private CFG cfg;

	// TODO temporary
	private CodeAttribute codeAttribute;

	private boolean deprecated;

	private final M m;

	private BodyDeclaration methodDeclaration;

	private A[][] paramAs;

	private boolean synthetic;

	private final TD td;

	/**
	 * Constructor.
	 * 
	 * @param m
	 *            method
	 */
	public MD(final M m, final TD td) {
		assert m != null;
		assert td != null;

		this.m = m;
		this.td = td;
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
	 * Get annotations.
	 * 
	 * @return annotations
	 */
	public A[] getAs() {
		return this.as;
	}

	/**
	 * Get control flow graph.
	 * 
	 * @return control flow graph or null
	 */
	public CFG getCfg() {
		return this.cfg;
	}

	public CodeAttribute getCodeAttribute() {
		return this.codeAttribute;
	}

	/**
	 * Get method.
	 * 
	 * @return method
	 */
	public M getM() {
		return this.m;
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
	 * Get parameter annotations.
	 * 
	 * @return parameter annotations
	 */
	public A[][] getParamAs() {
		return this.paramAs;
	}

	/**
	 * Get type declaration.
	 * 
	 * @return type declaration
	 */
	public TD getTd() {
		return this.td;
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
	 * Set access flags.
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public void setAccessFlags(final int accessFlags) {
		this.accessFlags = accessFlags;
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
	 * Set annotations.
	 * 
	 * @param as
	 *            annotations
	 */
	public void setAs(final A[] as) {
		this.as = as;
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

	// TODO temporary
	public void setCodeAttribute(final CodeAttribute codeAttribute) {
		this.codeAttribute = codeAttribute;
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
	 * Set Eclipse method declaration.
	 * 
	 * @param methodDeclaration
	 *            Eclipse method declaration
	 */
	public void setMethodDeclaration(final BodyDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	/**
	 * Set parameter annotations.
	 * 
	 * @param paramAs
	 *            parameter annotations
	 */
	public void setParamAs(final A[][] paramAs) {
		this.paramAs = paramAs;
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

	@Override
	public String toString() {
		return getM().toString();
	}

}