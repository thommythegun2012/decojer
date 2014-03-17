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
package org.decojer.cavaj.model.types;

import java.util.List;

import javax.annotation.Nullable;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Element;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Base type.
 * 
 * @author André Pankraz
 */
public abstract class BaseT extends T {

	@Override
	public void clear() {
		// nothing
	}

	@Override
	public boolean createTd() {
		return false;
	}

	@Nullable
	@Override
	public Object getAstNode() {
		return null;
	}

	@Override
	public CU getCu() {
		return null;
	}

	@Nullable
	@Override
	public Element getDeclarationForNode(final ASTNode node) {
		return null;
	}

	@Nullable
	@Override
	public Element getDeclarationOwner() {
		return null;
	}

	@Override
	public List<Element> getDeclarations() {
		return null;
	}

	@Override
	public boolean isAtLeast(final Version version) {
		return true;
	}

	@Override
	public boolean isBelow(final Version version) {
		return false;
	}

	@Override
	public boolean isDalvik() {
		return false;
	}

	@Override
	public boolean isScala() {
		return false;
	}

	@Override
	public void resolve() {
		assert false;
	}

	@Override
	public void setAs(final A[] as) {
		assert false;
	}

	@Override
	public void setAstNode(@Nullable final Object astNode) {
		assert false;
	}

	@Override
	public void setScala() {
		assert false;
	}

	@Override
	public void setSignature(final String signature) {
		assert false;
	}

	@Override
	public void setSourceFileName(final String sourceFileName) {
		assert false;
	}

	@Override
	public void setVersion(final int version) {
		assert false;
	}

}