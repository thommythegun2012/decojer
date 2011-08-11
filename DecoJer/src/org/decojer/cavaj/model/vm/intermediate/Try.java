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
package org.decojer.cavaj.model.vm.intermediate;

import java.util.HashMap;
import java.util.Map;

import org.decojer.cavaj.model.T;

/**
 * Try.
 * 
 * @author André Pankraz
 */
public class Try {

	private final int endPc;

	private final int startPc;

	private final HashMap<T, Integer> catches = new HashMap<T, Integer>();

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param startPc
	 *            start pc
	 * @param endPc
	 *            end pc
	 * @param handlerOpPc
	 *            handler pc
	 */
	public Try(final int startPc, final int endPc) {
		this.startPc = startPc;
		this.endPc = endPc;
	}

	/**
	 * Get catches.
	 * 
	 * @return catches
	 */
	public HashMap<T, Integer> getCatches() {
		return this.catches;
	}

	/**
	 * Get end pc.
	 * 
	 * @return end pc
	 */
	public int getEndPc() {
		return this.endPc;
	}

	/**
	 * Get start pc.
	 * 
	 * @return start pc
	 */
	public int getStartPc() {
		return this.startPc;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Try");
		sb.append("(").append(this.startPc).append(" - ").append(this.endPc)
				.append(") ");
		for (final Map.Entry<T, Integer> catchh : this.catches.entrySet()) {
			sb.append(catchh.getKey()).append(": ").append(catchh.getValue())
					.append(" ");
		}
		return sb.toString();
	}

}