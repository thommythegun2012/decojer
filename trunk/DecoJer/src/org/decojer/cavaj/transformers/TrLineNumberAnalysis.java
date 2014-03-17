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

import static org.decojer.cavaj.utils.Expressions.getOp;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Transformer: Line Number Analysis.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrLineNumberAnalysis {

	private static void analyzeLines(final Block block, final Element e) {
		for (final Statement statement : (List<Statement>) block.statements()) {
			final Op op = getOp(statement);
			if (op == null) {
				// LOGGER.warning("Op is null for: '" + statement);
				// TODO can happen for while(true) {...}

				// TODO what is with sub nodes? really decend here? simply fill?
				continue;
			}
			// LOGGER.info("LINE: " + op.getLine() + " / " + bd);
		}
	}

	private static void analyzeLines(final T t) {
		for (final Element e : t.getDeclarations()) {
			if (e instanceof T) {
				analyzeLines((T) e);
				continue;
			}
			if (e instanceof F) {
				final Object fieldDeclaration = e.getAstNode();
				if (fieldDeclaration == null) {
					continue;
				}
				if (fieldDeclaration instanceof FieldDeclaration) {
					for (final VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) ((FieldDeclaration) fieldDeclaration)
							.fragments()) {
						final Expression initializer = fragment.getInitializer();
						if (initializer == null) {
							continue;
						}
						final Op op = getOp(initializer);
						if (op == null) {
							continue;
						}
						// LOGGER.info("LINE: " + op.getLine() + " / " + bd);
					}
				} else if (fieldDeclaration instanceof EnumConstantDeclaration) {
					// TODO
				} else {
					log.warn("Unknown field ASTNode type '" + fieldDeclaration.getClass() + "'!");
					continue;
				}
				continue;
			}
			if (e instanceof M) {
				final Object methodDeclaration = e.getAstNode();
				if (methodDeclaration == null) {
					continue;
				}
				Block block;
				if (methodDeclaration instanceof MethodDeclaration) {
					block = ((MethodDeclaration) methodDeclaration).getBody();
				} else if (methodDeclaration instanceof Initializer) {
					block = ((Initializer) methodDeclaration).getBody();
				} else if (methodDeclaration instanceof AnnotationTypeMemberDeclaration) {
					continue;
				} else {
					log.warn("Unknown method ASTNode type '" + methodDeclaration.getClass() + "'!");
					continue;
				}
				if (block != null) {
					analyzeLines(block, e);
				}
				continue;
			}
		}
	}

	/**
	 * Transform compilation unit.
	 *
	 * @param cu
	 *            compilation unit
	 */
	public static void transform(final CU cu) {
		for (final Element e : cu.getDeclarations()) {
			analyzeLines((T) e);
		}
	}

}