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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.decojer.cavaj.model.code.BB;

import com.google.common.collect.Lists;

/**
 * Block struct, as labeled block for forward GOTOs or as variable boundary.
 *
 * @author André Pankraz
 */
public class Block extends Struct {

	/**
	 * Constructor.
	 *
	 * @param head
	 *            catch head BB
	 */
	public Block(@Nonnull final BB head) {
		super(head);
	}

	/**
	 * Constructor.
	 *
	 * @param childStruct
	 *            child struct
	 */
	public Block(@Nonnull final Struct childStruct) {
		super(childStruct.getHead(), childStruct.getParent());
		childStruct.setParent(this);

		final ArrayList<BB> members = Lists.newArrayList();
		for (final Map.Entry<Object, List<BB>> value2membersEntry : childStruct.value2members
				.entrySet()) {
			final List<BB> bbs = value2membersEntry.getValue();
			assert bbs != null;
			// don't copy values here, mostly for block
			// don't use addMember(), which would change the bb-struct
			members.addAll(bbs);
		}
		this.value2members.put(null, members);
	}

}