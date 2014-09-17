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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.code.BB;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Struct.
 *
 * @author André Pankraz
 */
@Slf4j
public class Struct {

	@Getter
	@Nullable
	private BB follow;

	@Getter
	@Nonnull
	private final BB head;

	@Getter
	@Setter
	@Nullable
	private String label;

	@Nonnull
	protected final Map<Object, List<BB>> value2members = Maps.newHashMap();

	@Getter
	// can change for new outer break-block
	@Setter(AccessLevel.PROTECTED)
	@Nullable
	private Struct parent;

	/**
	 * Constructor.
	 *
	 * @param head
	 *            struct head
	 */
	public Struct(@Nonnull final BB head) {
		this(head, head.getStruct());
		head.setStruct(this);
	}

	protected Struct(@Nonnull final BB head, @Nullable final Struct parent) {
		this.head = head;
		assert this != parent;
		setParent(parent);
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
	public boolean addMember(@Nullable final Object value, @Nonnull final BB bb) {
		assert bb != this.head : "Cannot add head as struct member for: " + bb;

		List<BB> members = this.value2members.get(value);
		if (members == null) {
			members = Lists.newArrayList();
			this.value2members.put(value, members);
		}
		if (members.contains(bb)) {
			return false;
		}
		final Struct parent = getParent();
		if (parent instanceof Loop) {
			parent.addMember(null, bb);
		}
		members.add(bb);
		bb.setStruct(this);
		return true;
	}

	/**
	 * Add struct members for value (not head).
	 *
	 * @param value
	 *            value
	 * @param bbs
	 *            struct members for value
	 */
	public void addMembers(@Nullable final Object value, @Nonnull final Collection<BB> bbs) {
		// TODO could be made faster if necessary when directly implemented
		for (final BB bb : bbs) {
			assert bb != null;
			addMember(value, bb);
		}
	}

	/**
	 * Get default label name, like "loop" or "switch".
	 *
	 * @return default label name
	 */
	public String getDefaultLabelName() {
		return getClass().getSimpleName().toLowerCase();
	}

	/**
	 * Get struct members for value, changeable list!
	 *
	 * @param value
	 *            value
	 * @return struct members, changeable list
	 */
	@Nullable
	public List<BB> getMembers(@Nullable final Object value) {
		final List<BB> members = this.value2members.get(value);
		if (members == null) {
			return null;
		}
		final List<BB> unmodifiableMembers = Collections.unmodifiableList(members);
		assert unmodifiableMembers != null;
		return unmodifiableMembers;
	}

	/**
	 * His this struct the given BB as target for break?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - this struct has the given BB as target for break
	 */
	public boolean hasBreakTarget(@Nullable final BB bb) {
		return hasFollow(bb);
	}

	/**
	 * His this loop struct the given BB as target for continue?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - this loop struct has the given BB as target for continue
	 */
	public boolean hasContinueTarget(@Nullable final BB bb) {
		return false;
	}

	/**
	 * Has this struct the given BB as follow?
	 *
	 * Only such nodes are potential break targets.
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - this struct has the given BB as follow
	 */
	public boolean hasFollow(@Nullable final BB bb) {
		return getFollow() == bb;
	}

	/**
	 * Has this struct the given BB as head?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - this struct has the given BB as head
	 */
	public boolean hasHead(@Nullable final BB bb) {
		return getHead() == bb;
	}

	/**
	 * Has this struct the given BB as member (includes struct head and loop last)?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - this struct has the given BB as member
	 */
	public boolean hasMember(@Nullable final BB bb) {
		if (hasHead(bb)) {
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
	 * Has this struct the given BB as member for given value?
	 *
	 * @param value
	 *            value
	 * @param bb
	 *            BB
	 * @return {@code true} - this struct has the given BB as member for given value
	 */
	public boolean hasMember(@Nullable final Object value, @Nullable final BB bb) {
		final List<BB> members = this.value2members.get(value);
		return members != null && members.contains(bb);
	}

	/**
	 * Is this struct per default breakable?
	 *
	 * At the same time Loops and Switches even need breaks to escape these structures (beside last
	 * case in Switch).
	 *
	 * @return {@code true} - is per default breakable
	 */
	public boolean isDefaultBreakable() {
		return false; // overwritten in loop/switch
	}

	/**
	 * Set follow.
	 *
	 * @param bb
	 *            follow
	 */
	public void setFollow(@Nonnull final BB bb) {
		assert !bb.isCatchHandler() : "catch handler cannot be a follow";
		// a direct back link at the end of a loop is not a valid follow, have to handle this
		// differently or we will loop into loop create statements twice,
		// dismiss such settings silently for now, else have to handle it at many places
		if (bb.getPostorder() >= this.head.getPostorder()) {
			this.follow = null;
			return;
		}
		// if parent struct exists and doesn't has BB as member, check existend follow!
		final Struct parent = getParent();
		if (parent != null && !parent.hasMember(bb)) {
			if (!parent.hasFollow(bb)) {
				if (parent.getFollow() == null) {
					parent.setFollow(bb);
				} else if (parent instanceof Loop || parent instanceof Block) {
					// if a loop contains a sub-struct that exits the loop
					parent.addMember(null, bb);
				} else {
					log.warn("Cannot change follow to BB" + bb.getPc() + " for struct:\n" + this);
					assert bb.isSubHead() : "Cannot change follow to BB" + bb.getPc()
							+ " for struct:\n" + this;
				}
			}
		}
		this.follow = bb;
	}

	@Override
	public String toString() {
		// calculate prefix from parent indentation level
		String parentStr;
		String prefix;
		final Struct parent = getParent();
		if (parent == null) {
			parentStr = null;
			prefix = "";
		} else {
			parentStr = parent.toString();
			prefix = "  ";
			for (int i = 0; i < parentStr.length(); ++i) {
				if (parentStr.charAt(i) != ' ') {
					prefix += Strings.repeat(" ", i);
					break;
				}
			}
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(prefix).append("--- ").append(getClass().getSimpleName()).append(" ---\n");
		final String label = getLabel();
		if (label != null) {
			sb.append(prefix).append("Label: ").append(label).append('\n');
		}
		sb.append(prefix).append("Head: BB ").append(getHead().getPc());
		final BB follow = getFollow();
		if (follow != null) {
			sb.append("  Follow: BB ").append(follow.getPc());
		}
		sb.append('\n').append(prefix).append("Members: ");
		int i = 0;
		for (final Entry<Object, List<BB>> entry : this.value2members.entrySet()) {
			sb.append("\n  ").append(prefix);
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
				sb.append("BB ").append(bb.getPc()).append("   ");
			}
		}
		final String special = toStringSpecial(prefix);
		if (special != null) {
			sb.append('\n').append(special);
		}
		if (parentStr != null) {
			sb.append('\n').append(parentStr);
		}
		return sb.toString();
	}

	@SuppressWarnings("unused")
	protected String toStringSpecial(final String prefix) {
		return null;
	}

}