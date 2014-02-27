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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.TD;

/**
 * Declaration.
 * 
 * @author André Pankraz
 */
public abstract class ED extends D {

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	@Getter
	@Setter
	private Object astNode;

	/**
	 * Parent declaration.
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter
	private D parent;

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	@Override
	public void addTd(final TD td) {
		addBd(td);
	}

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
		if (this.parent instanceof CU) {
			return (CU) this.parent;
		}
		if (this.parent instanceof ED) {
			return ((ED) this.parent).getCu();
		}
		return null;
	}

	public Element getDeclarationOwner() {
		return getParent() == null ? null : getParent().getElement();
	}

	public void setDeclarationOwner(final Element declarationOwner) {
		if (getParent() != null) {
			getParent().getBds().remove(this);
		}
	}

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public abstract void setSignature(final String signature);

}