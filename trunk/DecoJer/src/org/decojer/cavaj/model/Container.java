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

import javax.annotation.Nullable;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Container.
 * 
 * @author André Pankraz
 */
public interface Container {

	/**
	 * Get AST node.
	 * 
	 * @return AST node
	 */
	@Nullable
	Object getAstNode();

	/**
	 * Set AST node.
	 * 
	 * @param astNode
	 *            AST node
	 */
	void setAstNode(@Nullable final Object astNode);

	/**
	 * Clear all decompile infos, e.g. AST nodes.
	 */
	void clear();

	/**
	 * Get declaration for AST node or {@code null}.
	 * 
	 * @param node
	 *            AST node or {@code null}
	 * @return declaration
	 */
	Element getDeclarationForNode(final ASTNode node);

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	String getName();

	/**
	 * Get contained declarations.
	 * 
	 * @return declarations
	 */
	List<Element> getDeclarations();

}