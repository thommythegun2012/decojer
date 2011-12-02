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
package org.decojer.cavaj.util;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.DFlag;
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

	private AST getAST() {
		return this.cu.getAst();
	}

	/**
	 * New type name.
	 * 
	 * @param name
	 *            type name
	 * @return Eclipse type name
	 */
	public Name newTypeName(final String name) {
		assert name != null;

		String javaName;
		if (this.packagePrefix != null && name.startsWith(this.packagePrefix)
				&& name.indexOf('.', this.packagePrefix.length()) == -1) {
			if (!this.cu.check(DFlag.START_TD_ONLY)) {
				// add TD to CU - if main type name part equal to any main TD in CU,
				// anonymous inner classes need extra handling
				final TD td = this.cu.getStartTd().getT().getDu().getTd(name);
				if (td != null && td.getCu() == null) {
					final int pos = name.indexOf('$');
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
			javaName = name.substring(this.packagePrefix.length());
		}
		if (name.startsWith(JAVA_LANG) && name.indexOf('.', JAVA_LANG.length()) == -1) {
			// immediately remove this package prefix, save resources,
			// not newSimpleName, e.g. java.lang.Thread$State -> Thread.State
			javaName = name.substring(JAVA_LANG.length());
		} else {
			javaName = name;
		}
		try {
			return getAST().newName(replace(javaName));
		} catch (final IllegalArgumentException e) {
			// could contain illegal keywords
			return getAST().newName(javaName);
		}
	}

	private String replace(final String str) {
		int pos2 = str.indexOf('$');
		if (pos2 == -1) {
			return str;
		}
		int pos1 = 0;
		final StringBuilder sb = new StringBuilder();
		while (pos2 != -1 && pos2 + 1 < str.length()) {
			sb.append(str.substring(pos1, pos2));
			if (Character.isJavaIdentifierStart(str.charAt(pos2 + 1))) {
				sb.append('.');
			} else {
				sb.append('$');
			}
			pos1 = pos2 + 1;
			pos2 = str.indexOf('$', pos1);
		}
		sb.append(str.substring(pos1));
		return sb.toString();
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