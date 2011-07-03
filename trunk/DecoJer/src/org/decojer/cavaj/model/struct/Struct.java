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
package org.decojer.cavaj.model.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.decojer.cavaj.model.BB;

public class Struct {

	private BB follow;

	private final BB head;

	private String label;

	protected final Map<Object, List<BB>> value2members = new HashMap<Object, List<BB>>();

	private final Struct parent;

	public Struct(final BB bb) {
		assert bb != null;

		this.head = bb;
		this.parent = bb.getStruct();
		bb.setStruct(this);
	}

	public boolean addMember(final BB bb) {
		return addMember(null, bb);
	}

	public boolean addMember(final Object value, final BB bb) {
		assert bb != null;
		assert bb != this.head;

		List<BB> members = this.value2members.get(value);
		if (members == null) {
			members = new ArrayList<BB>();
			this.value2members.put(value, members);
		} else if (members.contains(bb)) {
			return false;
		}
		// TODO only possible, if IDom-Add trick with all breaks
		// assert bb.getStruct() == getParent();
		members.add(bb);
		bb.setStruct(this);
		return true;
	}

	public BB getFollow() {
		return this.follow;
	}

	public BB getHead() {
		return this.head;
	}

	public String getLabel() {
		return this.label;
	}

	public Struct getParent() {
		return this.parent;
	}

	public boolean isFollow(final BB bb) {
		return getFollow() == bb;
	}

	public boolean isHead(final BB bb) {
		return getHead() == bb;
	}

	public boolean isMember(final BB bb) {
		return isMember(null, bb);
	}

	public boolean isMember(final Object value, final BB bb) {
		final List<BB> members = this.value2members.get(value);
		return members != null && members.contains(bb);
	}

	public void setFollow(final BB bb) {
		this.follow = bb;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (getParent() != null) {
			sb.append(getParent()).append("\n\n");
		}
		sb.append("--- ").append(getClass().getSimpleName()).append(" ---");
		sb.append("\nHead: BB ").append(getHead().getPostorder());
		if (this.follow != null) {
			sb.append("  Follow: BB ").append(getFollow().getPostorder());
		}
		sb.append("\nMembers: ");
		int i = 0;
		for (final Entry<Object, List<BB>> entry : this.value2members
				.entrySet()) {
			sb.append("\n  ");
			if (i++ > 5) {
				sb.append(this.value2members.size()).append(" switches");
				break;
			}
			if (entry.getKey() != null) {
				sb.append(entry.getKey()).append(": ");
			}
			if (entry.getValue().size() > 20) {
				sb.append(entry.getValue().size()).append(" BBs");
				continue;
			}
			for (final BB bb : entry.getValue()) {
				sb.append("BB ").append(bb.getPostorder()).append("   ");
			}
		}
		return sb.toString();
	}

}