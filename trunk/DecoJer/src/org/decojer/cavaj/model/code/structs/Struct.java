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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.code.BB;

import com.google.common.collect.Maps;

/**
 * Struct.
 *
 * @author André Pankraz
 */
public class Struct {

	@Getter
	@Nullable
	private BB follow;

	@Getter
	@Nonnull
	private final BB head;

	@Getter
	@Setter
	private String label;

	protected final Map<Object, List<BB>> value2members = Maps.newHashMap();

	@Getter
	private final Struct parent;

	/**
	 * Constructor.
	 *
	 * @param bb
	 *            struct head
	 */
	public Struct(@Nonnull final BB bb) {
		this.parent = bb.getStruct();
		assert this != this.parent : bb.getCfg();

		this.head = bb;
		bb.setStruct(this);
	}

	/**
	 * Add struct member for value (not head).
	 *
	 * @param value
	 *            value
	 * @param bb
	 *            struct member for value
	 * @return {@code true} - added
	 */
	public boolean addMember(final Object value, final BB bb) {
		assert bb != this.head : bb.getCfg() + ": Cannot add head as struct member for: " + bb;

		return getMembers(value).add(bb);
	}

	/**
	 * Add struct members for value (not head).
	 *
	 * @param value
	 *            value
	 * @param bbs
	 *            struct members for value
	 */
	public void addMembers(final Object value, final Collection<BB> bbs) {
		for (final BB bb : bbs) {
			addMember(value, bb);
		}
	}

	/**
	 * Get struct members for value, changeable list!
	 *
	 * @param value
	 *            value
	 * @return struct members, changeable list
	 */
	@Nonnull
	public List<BB> getMembers(final Object value) {
		List<BB> members = this.value2members.get(value);
		if (members == null) {
			members = new ArrayList<BB>() {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean add(final BB bb) {
					if (bb.getStruct() == Struct.this) {
						// TODO sync assert 0 == 1 : bb;
						return false;
					}
					bb.setStruct(Struct.this);
					return super.add(bb);
				}

			};
			this.value2members.put(value, members);
		}
		return members;
	}

	/**
	 * Is BB a branching statement node (pre / endless loop head for continue, struct follow)?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is a branching statement node
	 */
	public boolean isBranching(final BB bb) {
		if (isBreakTarget(bb)) {
			return true;
		}
		if (this.parent instanceof Loop) {
			// scenario: isn't conditional follow, could still be a loop head
			return this.parent.isBranching(bb);
		}
		return false;
	}

	/**
	 * Is BB target for struct break?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is target for struct break
	 */
	public boolean isBreakTarget(final BB bb) {
		return isFollow(bb);
	}

	/**
	 * Is BB struct follow?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is struct follow
	 */
	public boolean isFollow(final BB bb) {
		return getFollow() == bb;
	}

	/**
	 * Is BB struct head?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is struct head
	 */
	public boolean isHead(final BB bb) {
		return getHead() == bb;
	}

	/**
	 * Is BB struct member (includes struct head and loop last)?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is struct member
	 */
	public boolean isMember(final BB bb) {
		if (isHead(bb)) {
			return true;
		}
		for (final Map.Entry<Object, List<BB>> members : this.value2members.entrySet()) {
			if (members.getValue().contains(bb)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is BB struct member for value?
	 *
	 * @param value
	 *            value
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is struct member for value
	 */
	public boolean isMember(final Object value, final BB bb) {
		final List<BB> members = this.value2members.get(value);
		return members != null && members.contains(bb);
	}

	/**
	 * Set follow.
	 *
	 * @param bb
	 *            follow
	 */
	public void setFollow(final BB bb) {
		// a direct back link at the end of a loop is not a valid follow, have to handle this
		// differently or we will loop into loop create statements twice,
		// dismiss such settings silently for now, else have to handle it at many places
		if (bb == null || bb.getPostorder() >= this.head.getPostorder()) {
			this.follow = null;
		} else {
			this.follow = bb;
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (this.parent != null) {
			sb.append(this.parent).append("\n\n");
		}
		sb.append("--- ").append(getClass().getSimpleName()).append(" ---");
		sb.append("\nHead: BB ").append(getHead().getPostorder());
		final BB follow = getFollow();
		if (follow != null) {
			sb.append("  Follow: BB ").append(follow.getPostorder());
		}
		sb.append("\nMembers: ");
		int i = 0;
		for (final Entry<Object, List<BB>> entry : this.value2members.entrySet()) {
			sb.append("\n  ");
			if (i++ > 5) {
				sb.append(this.value2members.size()).append(" switches");
				break;
			}
			if (entry.getKey() != null) {
				sb.append(
						entry.getKey() instanceof Object[] ? Arrays.toString((Object[]) entry
								.getKey()) : entry.getKey()).append(": ");
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