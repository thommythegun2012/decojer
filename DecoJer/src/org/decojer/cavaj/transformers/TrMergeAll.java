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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.CFG;
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

	private static boolean addBodyDeclaration(final T t, @Nullable final Object bodyDeclaration) {
		if (bodyDeclaration == null) {
			return false;
		}
		final Object astNode = t.getAstNode();
		if (astNode == null) {
			return false;
		}
		if (astNode instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration) astNode).bodyDeclarations().add(bodyDeclaration);
		}
		if (bodyDeclaration instanceof EnumConstantDeclaration) {
			if (astNode instanceof EnumDeclaration) {
				return ((EnumDeclaration) astNode).enumConstants().add(bodyDeclaration);
			}
			return false;
		}
		return ((AbstractTypeDeclaration) astNode).bodyDeclarations().add(bodyDeclaration);
	}

	private static int countConstructors(final T t) {
		int constructors = 0;
		for (final Element declaration : t.getDeclarations()) {
			if (declaration instanceof M && ((M) declaration).isConstructor()) {
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
	public static void transform(@Nonnull final CU cu) {
		for (final Element declaration : cu.getDeclarations()) {
			final T t = (T) declaration;
			if (t.isAnonymous()) {
				final T enclosingT = t.getEnclosingT();
				if (enclosingT != null && enclosingT.isDeclaration()) {
					continue;
				}
			}
			final Object typeDeclaration = t.getAstNode();
			// no package-info.java (typeDeclaration == null)
			if (typeDeclaration instanceof AbstractTypeDeclaration) {
				cu.getCompilationUnit().types().add(typeDeclaration);
			}
			transform(t);
		}
	}

	private static void transform(final T t) {
		// multiple constructors? => no omissable default constructor
		final int constructors = countConstructors(t);
		for (final Element declaration : t.getDeclarations()) {
			if (declaration instanceof T) {
				if (!((T) declaration).isAnonymous()) {
					addBodyDeclaration(t, declaration.getAstNode());
				}
				transform((T) declaration);
				continue;
			}
			if (declaration instanceof F) {
				addBodyDeclaration(t, declaration.getAstNode());
				for (final Element innerDeclaration : declaration.getDeclarations()) {
					transform((T) innerDeclaration);
				}
				continue;
			}
			if (declaration instanceof M) {
				final M m = (M) declaration;
				for (final Element innerDeclaration : m.getDeclarations()) {
					if (!((T) innerDeclaration).isAnonymous()) {
						final ASTNode typeDeclaration = (ASTNode) ((T) innerDeclaration)
								.getAstNode();
						final CFG cfg = m.getCfg();
						if (cfg != null && typeDeclaration != null) {
							cfg.getBlock()
							.statements()
							.add(typeDeclaration.getAST().newTypeDeclarationStatement(
									(AbstractTypeDeclaration) typeDeclaration));
						}
					}
					transform((T) innerDeclaration);
				}
				final Object methodDeclaration = m.getAstNode();
				if (methodDeclaration instanceof MethodDeclaration
						&& ((MethodDeclaration) methodDeclaration).isConstructor()) {
					if (t.getAstNode() instanceof AnonymousClassDeclaration) {
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
				addBodyDeclaration(t, methodDeclaration);
				continue;
			}
		}
	}

}