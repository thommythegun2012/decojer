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
 * Body declaration.
 * 
 * @author André Pankraz
 */
public abstract class BD extends D {

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Deprecated State (from Deprecated Attribute).
	 */
	@Getter
	@Setter
	private boolean deprecated;

	/**
	 * Parent declaration.
	 */
	@Getter
	protected D parent;

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

	/**
	 * Declaration must be synthetic (from synthetic declaration attribute).
	 * 
	 * Since version 49 the ACC_SYNTHETIC attribute is the preferred solution. We simply put this
	 * information into the access flags.
	 */
	public abstract void assertSynthetic();

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	public CU getCu() {
		if (this.parent instanceof CU) {
			return (CU) this.parent;
		}
		if (this.parent instanceof BD) {
			return ((BD) this.parent).getCu();
		}
		return null;
	}

}