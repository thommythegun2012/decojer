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
package org.decojer.cavaj.model.util;

import java.util.HashMap;
import java.util.Map;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.TD;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Name;

/**
 * Type name manager.
 * 
 * Full names consist of dot-separated package names and dollar-separated type names.
 * 
 * @author André Pankraz
 */
public class TypeNameManager {

	private static final String JAVA_LANG = "java.lang.";

	private final CU cu;

	private String packagePrefix;

	private final Map<String, Integer> packagesName2number = new HashMap<String, Integer>();

	/**
	 * Constructor.
	 * 
	 * @param cu
	 *            compilation unit
	 */
	public TypeNameManager(final CU cu) {
		assert cu != null;

		this.cu = cu;
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		this.packagesName2number.clear();
	}

	private AST getAST() {
		return this.cu.getAst();
	}

	/**
	 * New type name.
	 * 
	 * @param fullName
	 *            full type name
	 * @return Eclipse type name
	 */
	public Name newTypeName(final String fullName) {
		assert fullName != null;

		if (this.packagePrefix != null && fullName.startsWith(this.packagePrefix)) {
			// immediately remove this package prefix, save resources
			int pos = fullName.indexOf('.', this.packagePrefix.length());
			if (pos == -1) {
				// if not in sub package
				final String name = fullName.substring(this.packagePrefix.length());
				Integer number = this.packagesName2number.get(name);
				number = number == null ? 1 : number + 1;
				this.packagesName2number.put(name, number);

				if (!this.cu.isStartTdOnly()) {
					// add TD to CU if main type name equal to any main TD in CU
					final TD td = this.cu.getStartTd().getT().getDu().getTd(name);
					if (td != null && td.getCu() == null) {
						pos = name.indexOf('$');
						if (pos != -1) {
							final String mName = name.substring(0, pos);
							for (final TD _td : this.cu.getTds()) {
								if (mName.equals(_td.getT().getName())) {
									this.cu.addTd(td);
									break;
								}
							}
						}
					}
				}
				// TODO replace $ with ., but correctly handle anonymous inner
				// classes first, <init> not synthetic!
				return getAST().newSimpleName(name);
			}
		}
		if (fullName.startsWith(JAVA_LANG)) {
			// immediately remove this package prefix, save resources
			final int pos = fullName.indexOf('.', JAVA_LANG.length());
			if (pos == -1) {
				// if not in sub package
				return getAST().newSimpleName(fullName.substring(JAVA_LANG.length()));
			}
		}
		// TODO build package histogram
		// TODO replace $ with ., but correctly handle anonymous inner
		// classes first
		return getAST().newName(fullName);
	}

	/**
	 * Set package name.
	 * 
	 * @param packageName
	 *            package name
	 */
	public void setPackageName(final String packageName) {
		this.packagePrefix = packageName + ".";
	}

}