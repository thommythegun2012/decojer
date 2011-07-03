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

import javassist.bytecode.MethodInfo;

import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Method declaration.
 * 
 * @author André Pankraz
 */
public class MD implements BD, PD {

	private CFG cfg;

	private BodyDeclaration methodDeclaration;

	private final MethodInfo methodInfo;

	private final TD td;

	/**
	 * Constructor.
	 * 
	 * @param td
	 *            type declaration
	 * @param methodInfo
	 *            method info
	 */
	public MD(final TD td, final MethodInfo methodInfo) {
		assert td != null;
		assert methodInfo != null;

		this.td = td;
		this.methodInfo = methodInfo;
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
	 * Get method descriptor, e.g. "()V".
	 * 
	 * @return method descriptor
	 */
	public String getDescriptor() {
		return this.methodInfo.getDescriptor();
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
	 * Get method info.
	 * 
	 * @return method info
	 */
	public MethodInfo getMethodInfo() {
		return this.methodInfo;
	}

	/**
	 * Get method name, e.g. "test".
	 * 
	 * @return method name
	 */
	public String getName() {
		final String name = this.methodInfo.getName();
		if ("<clinit>".equals(name)) {
			return "static"; // TODO OK for Outline?
		}
		if ("<init>".equals(name)) {
			return getTd().getIName();
		}
		return name;
	}

	/**
	 * Get method signature, e.g. "test()V".
	 * 
	 * @return method signature
	 */
	public String getSignature() {
		return getName() + getDescriptor();
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
	 * Set control flow graph.
	 * 
	 * @param cfg
	 *            control flow graph
	 */
	public void setCFG(final CFG cfg) {
		this.cfg = cfg;
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

	@Override
	public String toString() {
		return getTd().toString() + '.' + getSignature();
	}

}