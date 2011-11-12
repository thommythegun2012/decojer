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

import org.decojer.cavaj.model.T.TT;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Name;

/**
 * Type Declaration.
 * 
 * Names consist of dot-separated package names (for full name) and dollar-separated type names.
 * 
 * @author André Pankraz
 */
public class TD implements BD, PD {

	private A[] as;

	// all body declarations: inner type/method/field declarations
	private final List<BD> bds = new ArrayList<BD>();

	// deprecated state (from deprecated attribute)
	private boolean deprecated;

	// is anonymous, enclosing method
	private M enclosingM;

	// is anonymous, enclosing type
	private T enclosingT;

	// member types (really contained inner classes)
	private T[] memberTs;

	// parent declaration
	private PD pd;

	// from source file attribute
	private String sourceFileName;

	// synthetic state (from synthetic attribute)
	private boolean synthetic;

	// type
	private final T t;

	// Eclipse type declaration
	private ASTNode typeDeclaration; // anonymousClassDeclaration?

	private int version;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
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
	public boolean addBodyDeclaration(final BodyDeclaration bodyDeclaration) {
		assert bodyDeclaration != null;

		if (this.typeDeclaration instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration) this.typeDeclaration).bodyDeclarations().add(
					bodyDeclaration);
		}
		if (bodyDeclaration instanceof EnumConstantDeclaration) {
			if (this.typeDeclaration instanceof EnumDeclaration) {
				return ((EnumDeclaration) this.typeDeclaration).enumConstants()
						.add(bodyDeclaration);
			}
			return false;
		}
		return ((AbstractTypeDeclaration) this.typeDeclaration).bodyDeclarations().add(
				bodyDeclaration);
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		this.pd = null;
		this.typeDeclaration = null;
		for (int i = this.bds.size(); i-- > 0;) {
			final BD bd = this.bds.get(i);
			if (bd instanceof TD) {
				((TD) bd).clear();
				this.bds.remove(i);
				continue;
			}
			if (bd instanceof FD) {
				((FD) bd).setFieldDeclaration(null);
				continue;
			}
			if (bd instanceof MD) {
				((MD) bd).setMethodDeclaration(null);
				final CFG cfg = ((MD) bd).getCfg();
				if (cfg != null) {
					cfg.clear();
				}
			}
		}
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
	 * Get enclosing method.
	 * 
	 * @return enclosing method
	 */
	public M getEnclosingM() {
		return this.enclosingM;
	}

	/**
	 * Get enclosing type.
	 * 
	 * @return enclosing type
	 */
	public T getEnclosingT() {
		return this.enclosingT;
	}

	/**
	 * Get field declaration for name.
	 * 
	 * @param name
	 *            name
	 * @return field declaration
	 */
	public FD getFd(final String name) {
		for (final BD bd : this.bds) {
			if (bd instanceof FD && name.equals(((FD) bd).getF().getName())) {
				return (FD) bd;
			}
		}
		return null;
	}

	/**
	 * Get member types (really contained inner classes).
	 * 
	 * @return member types
	 */
	public T[] getMemberTs() {
		return this.memberTs;
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
	public ASTNode getTypeDeclaration() {
		return this.typeDeclaration;
	}

	/**
	 * Get Class file version.
	 * 
	 * 1.0: 45.0, 1.1: 45.3, 1.2: 46, 1.3: 47, 1.4: 48, 5: 49, 6: 50, 7: 51
	 * 
	 * JDK 1.2 and 1.3 creates versions 1.1 if no target option given. JDK 1.4 creates 1.2 if no
	 * target option given
	 * 
	 * @return Class file version
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * Is anonymous type declaration?
	 * 
	 * @return true - is anonymous type declaration
	 */
	public boolean isAnonymous() {
		return this.typeDeclaration instanceof AnonymousClassDeclaration;
	}

	/**
	 * Is Dalvik?
	 * 
	 * @return true is Dalvik
	 */
	public boolean isDalvik() {
		return this.version == 0;
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
	 * New type name (currently only for signatures).
	 * 
	 * @param fullName
	 *            full type name
	 * @return Eclipse type name
	 */
	public Name newTypeName(final String fullName) {
		assert fullName != null;

		return getCu().getTypeNameManager().newTypeName(fullName);
	}

	/**
	 * New type name.
	 * 
	 * @param t
	 *            type
	 * @return Eclipse type name
	 */
	public Name newTypeName(final T t) {
		assert t != null;

		return getCu().getTypeNameManager().newTypeName(
				t.isMulti() ? ((TT) t).getTs()[0].getName() : t.getName());
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
	 * Set enclosing method.
	 * 
	 * @param enclosingM
	 *            enclosing method
	 */
	public void setEnclosingM(final M enclosingM) {
		this.enclosingM = enclosingM;
	}

	/**
	 * Set enclosing type.
	 * 
	 * @param enclosingT
	 *            enclosing type
	 */
	public void setEnclosingT(final T enclosingT) {
		this.enclosingT = enclosingT;
	}

	/**
	 * Set member types (really contained inner classes).
	 * 
	 * @param memberTs
	 *            member types
	 */
	public void setMemberTs(final T[] memberTs) {
		this.memberTs = memberTs;
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
	public void setTypeDeclaration(final ASTNode typeDeclaration) {
		this.typeDeclaration = typeDeclaration;
	}

	/**
	 * Set Class file version.
	 * 
	 * 1.0: 45.0, 1.1: 45.3, 1.2: 46, 1.3: 47, 1.4: 48, 5: 49, 6: 50, 7: 51
	 * 
	 * JDK 1.2 and 1.3 creates versions 1.1 if no target option given. JDK 1.4 creates 1.2 if no
	 * target option given
	 * 
	 * @param version
	 *            Class file version
	 */
	public void setVersion(final int version) {
		assert version >= 45 && version < 60 : version;

		this.version = version;
	}

	@Override
	public String toString() {
		return getT().toString();
	}

}