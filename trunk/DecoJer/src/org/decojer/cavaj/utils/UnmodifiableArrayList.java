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

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Unmodifiable array list wrapper.
 *
 * @param <E>
 *            Array Type
 *
 * @author André Pankraz
 */
public class UnmodifiableArrayList<E> extends AbstractList<E> {

	public static <E> List<E> fromArray(final E[] array) {
		if (array == null) {
			return Collections.<E> emptyList();
		}
		return new UnmodifiableArrayList<E>(array);
	}

	private final E[] array;

	private UnmodifiableArrayList(final E[] array) {
		this.array = array;
	}

	@Override
	public E get(final int index) {
		return this.array[index];
	}

	@Override
	public int size() {
		return this.array.length;
	}

}