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
package org.decojer.cavaj.model;

import java.lang.annotation.RetentionPolicy;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.Getter;

import org.decojer.cavaj.model.types.T;

import com.google.common.collect.Maps;

/**
 * Annotation.
 *
 * @author André Pankraz
 */
public final class A {

	private final LinkedHashMap<String, Object> members = Maps.newLinkedHashMap();

	@Getter
	private final RetentionPolicy retentionPolicy;

	@Getter
	@Nonnull
	private final T t;

	/**
	 * Constructor.
	 *
	 * @param t
	 *            type
	 * @param retentionPolicy
	 *            retention policy
	 */
	public A(@Nonnull final T t, final RetentionPolicy retentionPolicy) {
		this.t = t;
		this.retentionPolicy = retentionPolicy;
	}

	/**
	 * Add member.
	 *
	 * @param name
	 *            name
	 * @param value
	 *            value
	 */
	public void addMember(final String name, final Object value) {
		this.members.put(name, value);
	}

	/**
	 * Get member value.
	 *
	 * @param name
	 *            name
	 * @return value
	 */
	public Object getMember(final String name) {
		return this.members.get(name);
	}

	/**
	 * Get members.
	 *
	 * @return members
	 */
	public Set<Entry<String, Object>> getMembers() {
		return this.members.entrySet();
	}

	/**
	 * Get member value.
	 *
	 * @return value
	 */
	public Object getValueMember() {
		return this.members.get("value");
	}

	@Override
	public String toString() {
		if (getMembers().isEmpty()) {
			return getT().getName();
		}
		final StringBuilder sb = new StringBuilder(getT().getName()).append("(");
		for (final Entry<String, Object> member : getMembers()) {
			sb.append(member.getKey()).append("=").append(member.getValue()).append(",");
		}
		sb.setCharAt(sb.length() - 1, ')');
		return sb.toString();
	}

}