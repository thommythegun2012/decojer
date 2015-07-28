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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;

/**
 * Container declaration.
 *
 * @author André Pankraz
 */
public abstract class CD {

	/**
	 * AST node.
	 */
	@Getter
	@Setter
	@Nullable
	private Object astNode;

	/**
	 * Child declarations.
	 */
	@Getter
	private final List<Element> declarations = Lists.newArrayListWithCapacity(0);

	/**
	 * Clear all decompile infos, e.g. AST nodes.
	 */
	public void clear() {
		setAstNode(null);
		for (final Element declaration : getDeclarations()) {
			declaration.clear();
		}
	}

	/**
	 * Get declaration for AST node.
	 *
	 * @param node
	 *            AST node
	 * @return declaration
	 */
	@Nullable
	public Element getDeclarationForNode(@Nonnull final ASTNode node) {
		for (final Element bd : getDeclarations()) {
			// could also work with polymorphism here...but why pollute subclasses with helper
			if (bd instanceof F) {
				if (((F) bd).getAstNode() == node) {
					return bd;
				}
			} else if (bd instanceof M) {
				if (((M) bd).getAstNode() == node) {
					return bd;
				}
			} else if (bd instanceof T) {
				if (((T) bd).getAstNode() == node) {
					return bd;
				}
			}
			final Element retBd = bd.getDeclarationForNode(node);
			if (retBd != null) {
				return retBd;
			}
		}
		return null;
	}

}