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
package org.decojer.cavaj.model.code.struct;

import java.util.List;
import java.util.Map.Entry;

import org.decojer.cavaj.model.code.BB;

public class Switch extends Struct {

	public static final int SWITCH = 1;

	public static final int SWITCH_DEFAULT = 2;

	public static final String[] TYPE_NAME = { "<UNKNOWN>", "SWITCH", "SWITCH_DEFAULT" };

	private int type;

	public Switch(final BB head) {
		super(head);
	}

	public int getType() {
		return this.type;
	}

	public boolean isCase(final BB bb) {
		for (final Entry<Object, List<BB>> entry : this.value2members.entrySet()) {
			final List<BB> value = entry.getValue();
			if (value.size() == 0) {
				continue;
			}
			if (value.get(0) == bb) {
				return true;
			}
		}
		return false;
	}

	public void setType(final int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\nType: " + TYPE_NAME[getType()]);
		return sb.toString();
	}

}