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

import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.fields.FD;
import org.decojer.cavaj.model.methods.MD;
import org.decojer.cavaj.model.types.TD;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
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

	private static boolean addBodyDeclaration(final TD td, final BodyDeclaration bodyDeclaration) {
		if (bodyDeclaration == null) {
			return false;
		}
		if (td.getTypeDeclaration() instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration) td.getTypeDeclaration()).bodyDeclarations().add(
					bodyDeclaration);
		}
		if (bodyDeclaration instanceof EnumConstantDeclaration) {
			if (td.getTypeDeclaration() instanceof EnumDeclaration) {
				return ((EnumDeclaration) td.getTypeDeclaration()).enumConstants().add(
						bodyDeclaration);
			}
			return false;
		}
		return ((AbstractTypeDeclaration) td.getTypeDeclaration()).bodyDeclarations().add(
				bodyDeclaration);
	}

	private static int countConstructors(final TD td) {
		int constructors = 0;
		for (final BD bd : td.getBds()) {
			if (bd instanceof MD && ((MD) bd).isConstructor()) {
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
		for (final BD bd : cu.getBds()) {
			final TD td = (TD) bd;
			if (td.isAnonymous() && td.getEnclosingTd() != null) {
				continue;
			}
			final ASTNode typeDeclaration = td.getTypeDeclaration();
			// no package-info.java (typeDeclaration == null)
			if (typeDeclaration instanceof AbstractTypeDeclaration) {
				cu.getCompilationUnit().types().add(typeDeclaration);
			}
			transform(td);
		}
	}

	private static void transform(final TD td) {
		// multiple constructors? => no omissable default constructor
		final int constructors = countConstructors(td);
		for (final BD bd : td.getBds()) {
			if (bd instanceof TD) {
				if (!((TD) bd).isAnonymous()) {
					addBodyDeclaration(td, (AbstractTypeDeclaration) ((TD) bd).getTypeDeclaration());
				}
				transform((TD) bd);
				continue;
			}
			if (bd instanceof FD) {
				addBodyDeclaration(td, ((FD) bd).getFieldDeclaration());
				for (final BD innerTd : bd.getBds()) {
					transform((TD) innerTd);
				}
				continue;
			}
			if (bd instanceof MD) {
				final MD md = (MD) bd;
				for (final BD innerTd : md.getBds()) {
					if (!((TD) innerTd).isAnonymous()) {
						final ASTNode typeDeclaration = ((TD) innerTd).getTypeDeclaration();
						if (typeDeclaration != null) {
							md.getCfg()
									.getBlock()
									.statements()
									.add(typeDeclaration.getAST().newTypeDeclarationStatement(
											(AbstractTypeDeclaration) typeDeclaration));
						}
					}
					transform((TD) innerTd);
				}
				final BodyDeclaration methodDeclaration = md.getMethodDeclaration();
				if (methodDeclaration instanceof MethodDeclaration
						&& ((MethodDeclaration) methodDeclaration).isConstructor()) {
					if (td.getTypeDeclaration() instanceof AnonymousClassDeclaration) {
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
				} else if (methodDeclaration instanceof Initializer /* md.isInitializer() is true */) {
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