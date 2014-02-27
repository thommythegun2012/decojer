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
package org.decojer.cavaj.transformers;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * Transformer: Merge all.
 * 
 * @author André Pankraz
 */
public final class TrMergeAll {

	private static boolean addBodyDeclaration(final T td, final Object bodyDeclaration) {
		if (bodyDeclaration == null) {
			return false;
		}
		if (td.getAstNode() instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration) td.getAstNode()).bodyDeclarations().add(
					bodyDeclaration);
		}
		if (bodyDeclaration instanceof EnumConstantDeclaration) {
			if (td.getAstNode() instanceof EnumDeclaration) {
				return ((EnumDeclaration) td.getAstNode()).enumConstants().add(bodyDeclaration);
			}
			return false;
		}
		return ((AbstractTypeDeclaration) td.getAstNode()).bodyDeclarations().add(bodyDeclaration);
	}

	private static int countConstructors(final T td) {
		int constructors = 0;
		for (final Element e : td.getDeclarations()) {
			if (e instanceof M && ((M) e).isConstructor()) {
				++constructors;
			}
		}
		return constructors;
	}

	/**
	 * Transform compilation unit.
	 * 
	 * @param cu
	 *            compilation unit
	 */
	public static void transform(final CU cu) {
		for (final Element e : cu.getDeclarations()) {
			final T td = (T) e;
			if (td.isAnonymous() && td.getEnclosingT() != null) {
				continue;
			}
			final Object typeDeclaration = td.getAstNode();
			// no package-info.java (typeDeclaration == null)
			if (typeDeclaration instanceof AbstractTypeDeclaration) {
				cu.getCompilationUnit().types().add(typeDeclaration);
			}
			transform(td);
		}
	}

	private static void transform(final T td) {
		// multiple constructors? => no omissable default constructor
		final int constructors = countConstructors(td);
		for (final Element e : td.getDeclarations()) {
			if (e instanceof T) {
				if (!((T) e).isAnonymous()) {
					addBodyDeclaration(td, e.getAstNode());
				}
				transform((T) e);
				continue;
			}
			if (e instanceof F) {
				addBodyDeclaration(td, e.getAstNode());
				for (final Element innerE : e.getDeclarations()) {
					transform((T) innerE);
				}
				continue;
			}
			if (e instanceof M) {
				final M m = (M) e;
				for (final Element innerE : m.getDeclarations()) {
					if (!((T) innerE).isAnonymous()) {
						final ASTNode typeDeclaration = (ASTNode) ((T) innerE).getAstNode();
						if (typeDeclaration != null) {
							m.getCfg()
									.getBlock()
									.statements()
									.add(typeDeclaration.getAST().newTypeDeclarationStatement(
											(AbstractTypeDeclaration) typeDeclaration));
						}
					}
					transform((T) innerE);
				}
				final Object methodDeclaration = m.getAstNode();
				if (methodDeclaration instanceof MethodDeclaration
						&& ((MethodDeclaration) methodDeclaration).isConstructor()) {
					if (td.getAstNode() instanceof AnonymousClassDeclaration) {
						// anonymous inner classes cannot have visible Java constructors
						continue;
					}
					// ignore empty default constructor
					if (constructors == 1
							&& ((MethodDeclaration) methodDeclaration).parameters().size() == 0
							&& ((MethodDeclaration) methodDeclaration).getBody().statements()
									.size() == 0) {
						continue;
					}
				} else if (methodDeclaration instanceof Initializer /* m.isInitializer() is true */) {
					if (((Initializer) methodDeclaration).getBody().statements().size() == 0) {
						continue;
					}
				}
				// e.g. bridge methods?
				addBodyDeclaration(td, methodDeclaration);
				continue;
			}
		}
	}

}