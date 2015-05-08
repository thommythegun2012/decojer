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
package org.decojer.cavaj.utils;

import java.util.Comparator;

import org.decojer.cavaj.model.code.E;

/**
 * Line comperator for edges.
 *
 * @author André Pankraz
 */
public class ELineComperator implements Comparator<E> {

	// since JVM 7...GAE not
	private static int compare(final int x, final int y) {
		return x < y ? -1 : x == y ? 0 : 1;
	}

	@Override
	public int compare(final E e1, final E e2) {
		if (e1.isCatch() ^ e2.isCatch()) {
			return e2.isCatch() ? 1 : -1;
		}
		// don't change order for out lines that are before the in line
		final int startLine = e1.getStart().getLine();
		final int endLine1 = e1.getEnd().getLine();
		final int endLine2 = e2.getEnd().getLine();
		if (startLine > endLine1 || startLine > endLine2) {
			return 0;
		}
		return compare(endLine1, endLine2);
	}

}