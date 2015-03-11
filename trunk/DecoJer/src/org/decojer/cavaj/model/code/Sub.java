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

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.code.ops.RET;

/**
 * Subroutine.
 *
 * @author André Pankraz
 */
public final class Sub {

	@Getter
	@Setter
	private int pc;

	/**
	 * RET operation.
	 */
	@Getter
	@Setter
	private RET ret;

	/**
	 * Constructor.
	 *
	 * @param pc
	 *            pc
	 */
	public Sub(final int pc) {
		this.pc = pc;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Sub)) {
			return false;
		}
		final Sub sub = (Sub) obj;
		return this.pc == sub.pc;
	}

	@Override
	public int hashCode() {
		return this.pc;
	}

	@Override
	public String toString() {
		return "Sub" + getPc();
	}

}