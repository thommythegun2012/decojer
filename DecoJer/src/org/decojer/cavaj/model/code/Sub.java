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
package org.decojer.cavaj.model.code;

import java.util.HashSet;
import java.util.Set;

import org.decojer.cavaj.model.code.op.JSR;

/**
 * Subroutine.
 * 
 * @author André Pankraz
 */
public class Sub {

	final Set<JSR> jsrs;

	/**
	 * Constructor.
	 * 
	 * @param jsr
	 *            JSR operation
	 */
	public Sub(final JSR jsr) {
		this.jsrs = new HashSet<JSR>(2);
		this.jsrs.add(jsr);
	}

	/**
	 * Add JSR operation.
	 * 
	 * @param jsr
	 *            JSR operation
	 */
	public void addJsr(final JSR jsr) {
		this.jsrs.add(jsr);
	}

	/**
	 * Get JSR operations.
	 * 
	 * @return JSR operations
	 */
	public Set<JSR> getJsrs() {
		return this.jsrs;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Sub: ");
		sb.append(this.jsrs.iterator().next());
		return sb.toString();
	}

}