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
package org.decojer.cavaj.model.code.structs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.types.T;

/**
 * Catch struct.
 *
 * @author André Pankraz
 */
public class Catch extends Struct {

	public static final T[] FINALLY_TS = new T[] {};

	/**
	 * Constructor.
	 *
	 * @param head
	 *            catch head BB
	 */
	public Catch(@Nonnull final BB head) {
		super(head);
	}

	/**
	 * Get catch types if this catch struct has the given BB as handler node.
	 *
	 * @param bb
	 *            BB
	 * @return catch types if this catch struct has the given BB as handler node or {@code null}
	 */
	@Nullable
	public T[] getHandlerCatchTypes(@Nullable final BB bb) {
		final Object value = findValueWhereFirstMemberIs(bb);
		return value instanceof T[] ? (T[]) findValueWhereFirstMemberIs(bb) : null;
	}

	/**
	 * Has this catch struct the given BB as handler node for given types?
	 *
	 * @param types
	 *            catch types
	 * @param bb
	 *            BB
	 * @return {@code true} - this catch struct has the given BB as handler node for given types
	 */
	public boolean hasHandler(@Nonnull final T[] types, @Nullable final BB bb) {
		return getFirstMember(types) == bb;
	}

}