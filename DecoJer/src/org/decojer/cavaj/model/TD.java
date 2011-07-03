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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.ClassFile;

import org.decojer.DecoJerException;
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

	// all body declarations: inner type/method/field declarations
	private final List<BD> bds = new ArrayList<BD>();

	private final ClassFile classFile;

	// parent declaration
	private PD pd;

	// package fragment
	private final PF pf;

	// Eclipse type declaration
	private AbstractTypeDeclaration typeDeclaration; // anonymousClassDeclaration?

	/**
	 * Constructor.
	 * 
	 * @param pf
	 *            package fragment
	 * @param classStream
	 *            class stream
	 */
	public TD(final PF pf, final DataInputStream classStream) {
		assert pf != null;
		assert classStream != null;

		this.pf = pf;

		// Javassist is a shortcut for now, later parse directly, in any case
		// completely consume stream in this constructor and remember all data!
		// TODO check if Javassist's dual licence MPL / LPGL is compatible with
		// our AGPLv3?
		try {
			this.classFile = new ClassFile(classStream);
		} catch (final IOException e) {
			throw new DecoJerException(
					"Couldn't read class file from class stream!", e);
		}
		// Nevertheless, it isn't possible to create Eclipse AST objects here!
		// Can only be after Compilation Unit creation.
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
	 * Get body declarations.
	 * 
	 * @return body declarations, not null
	 */
	public List<BD> getBds() {
		return this.bds;
	}

	/**
	 * Get class file.
	 * 
	 * @return class file, not null
	 */
	public ClassFile getClassFile() {
		return this.classFile;
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
	 * Get full name, e.g. "decojer.model.TD$Inner".
	 * 
	 * @return full name
	 */
	public String getFullName() {
		return getClassFile().getName();
	}

	/**
	 * Get inner name, without '$'.
	 * 
	 * @return inner name
	 */
	public String getIName() {
		final String fullName = getName();
		final int pos = fullName.lastIndexOf('$');
		final String name = pos == -1 ? fullName : fullName.substring(pos + 1);

		try {
			Integer.parseInt(name);
			return "T$" + name;
		} catch (final NumberFormatException e) {
			return name;
		}
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
	public MD getMD(final String name, final String descriptor) {
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
	 * Get name, with '$', e.g. "TD$Inner".
	 * 
	 * @return name, not null
	 */
	public String getName() {
		final String name = getFullName();
		final int pos = name.lastIndexOf('.');
		return pos == -1 ? name : name.substring(pos + 1);
	}

	/**
	 * Get package name (from class file).
	 * 
	 * @return package name, not null
	 */
	public String getPackageName() {
		final String name = getFullName();
		final int pos = name.lastIndexOf('.');
		return pos == -1 ? "" : name.substring(0, pos);
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
	 * Get package fragment.
	 * 
	 * @return package fragment, not null
	 */
	public PF getPf() {
		return this.pf;
	}

	/**
	 * Get source file name from source file attribute.
	 * 
	 * @return source file name or null
	 */
	public String getSourceFileName() {
		return this.classFile.getSourceFile();
	}

	/**
	 * Get Eclipse type declaration.
	 * 
	 * @return type declaration
	 */
	public AbstractTypeDeclaration getTypeDeclaration() {
		return this.typeDeclaration;
	}

	public boolean isJdk6() {
		return getClassFile().getMajorVersion() == ClassFile.JAVA_6;
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
	 * Set parent declaration.
	 * 
	 * @param pd
	 *            parent declaration
	 */
	public void setPd(final PD pd) {
		this.pd = pd;
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

	@Override
	public String toString() {
		return getFullName();
	}

}