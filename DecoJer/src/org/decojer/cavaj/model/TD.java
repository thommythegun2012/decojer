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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Name;

/**
 * Type Declaration.
 * 
 * Names consist of dot-separated package names (for full name) and
 * dollar-separated type names.
 * 
 * @author André Pankraz
 */
public class TD implements BD, PD {

	private int accessFlags;

	private A[] as;

	// all body declarations: inner type/method/field declarations
	private final List<BD> bds = new ArrayList<BD>();

	private boolean deprecated;

	// parent declaration
	private PD pd;

	private String sourceFileName;

	private boolean synthetic;

	// type
	private final T t;

	// Eclipse type declaration
	private AbstractTypeDeclaration typeDeclaration; // anonymousClassDeclaration?

	private int version;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            t
	 */
	public TD(final T t) {
		assert t != null;

		this.t = t;
	}

	/**
	 * Add Eclipse body declarations.
	 * 
	 * @param bodyDeclaration
	 *            Eclipse body declaration
	 * 
	 * @return true - success
	 */
	@SuppressWarnings("unchecked")
	public boolean addBodyDeclarartion(final BodyDeclaration bodyDeclaration) {
		assert bodyDeclaration != null;

		return getTypeDeclaration().bodyDeclarations().add(bodyDeclaration);
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
	 * Get annotations.
	 * 
	 * @return annotations
	 */
	public A[] getAs() {
		return this.as;
	}

	/**
	 * Get body declarations.
	 * 
	 * @return body declarations, not null
	 */
	public List<BD> getBds() {
		return this.bds;
	}

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	public CU getCu() {
		final PD pd = getPd();
		if (pd instanceof CU) {
			return (CU) pd;
		}
		if (pd instanceof TD) {
			return ((TD) pd).getCu();
		}
		if (pd instanceof MD) {
			return ((MD) pd).getTd().getCu();
		}
		if (pd instanceof FD) {
			return ((FD) pd).getTd().getCu();
		}
		return null;
	}

	/**
	 * Get method declaration with name and signature.
	 * 
	 * @param name
	 *            method name, e.g. "test"
	 * @param descriptor
	 *            method descriptor, e.g. "()V"
	 * @return method declaration or null
	 */
	public MD getMd(final String name, final String descriptor) {
		assert name != null;
		assert descriptor != null;

		for (final BD bd : getBds()) {
			if (!(bd instanceof MD)) {
				continue;
			}
			final MD md = (MD) bd;
			if (name.equals(md.getName())
					&& descriptor.equals(md.getDescriptor())) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Get parent declaration.
	 * 
	 * @return parent declaration or null if no inner class
	 */
	public PD getPd() {
		return this.pd;
	}

	/**
	 * Get source file name (from source file attribute).
	 * 
	 * @return source file name or null
	 */
	public String getSourceFileName() {
		return this.sourceFileName;
	}

	/**
	 * Get type.
	 * 
	 * @return type
	 */
	public T getT() {
		return this.t;
	}

	/**
	 * Get Eclipse type declaration.
	 * 
	 * @return type declaration
	 */
	public AbstractTypeDeclaration getTypeDeclaration() {
		return this.typeDeclaration;
	}

	/**
	 * Get Class file version (Java2 46 ... Java7 51).
	 * 
	 * @return Class file version
	 */
	public int getVersion() {
		return this.version;
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
	 * New type name.
	 * 
	 * @param fullName
	 *            full name
	 * @return Eclipse type name
	 */
	public Name newTypeName(final String fullName) {
		assert fullName != null;

		return getCu().getTypeNameManager().newTypeName(fullName);
	}

	/**
	 * Set access flags
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public void setAccessFlags(final int accessFlags) {
		this.accessFlags = accessFlags;
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
	 * Set parent declaration.
	 * 
	 * @param pd
	 *            parent declaration
	 */
	public void setPd(final PD pd) {
		this.pd = pd;
	}

	/**
	 * Set source file name (from source file attribute).
	 * 
	 * @param sourceFileName
	 *            source file name
	 */
	public void setSourceFileName(final String sourceFileName) {
		this.sourceFileName = sourceFileName;
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
	 * Set Eclipse type declaration.
	 * 
	 * @param typeDeclaration
	 *            Eclipse type declaration
	 */
	public void setTypeDeclaration(final AbstractTypeDeclaration typeDeclaration) {
		this.typeDeclaration = typeDeclaration;
	}

	/**
	 * Set Class file version (Java2 46 ... Java7 51).
	 * 
	 * @param version
	 *            Class file version
	 */
	public void setVersion(final int version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return getT().toString();
	}

}