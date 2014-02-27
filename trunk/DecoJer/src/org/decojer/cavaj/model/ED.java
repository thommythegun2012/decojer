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

import lombok.Getter;
import lombok.Setter;

/**
 * Declaration.
 * 
 * @author André Pankraz
 */
public abstract class ED extends D {

	/**
	 * Annotations or {@code null}.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Declaration owner or {@code null}.
	 */
	@Getter
	private Container declarationOwner;

	@Override
	public void clear() {
		setAstNode(null);
	}

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	public CU getCu() {
		final Container declarationOwner = getDeclarationOwner();
		if (declarationOwner instanceof CU) {
			return (CU) declarationOwner;
		}
		if (declarationOwner instanceof Element) {
			return ((Element) declarationOwner).getCu();
		}
		return null;
	}

	/**
	 * Set declaration owner.
	 * 
	 * @param declarationOwner
	 *            declaration owner
	 */
	public void setDeclarationOwner(final Container declarationOwner) {
		final Element element = getElement();
		final Container previousDeclarationOwner = getDeclarationOwner();
		if (previousDeclarationOwner != null) {
			previousDeclarationOwner.getDeclarations().remove(element);
		}
		declarationOwner.getDeclarations().add(element);
		this.declarationOwner = declarationOwner;
	}

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public abstract void setSignature(final String signature);

}