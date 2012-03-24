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
import java.util.Set;

import lombok.Getter;

/**
 * Annotation.
 * 
 * @author André Pankraz
 */
public class A {

	private final LinkedHashMap<String, Object> members = new LinkedHashMap<String, Object>();

	@Getter
	private final RetentionPolicy retentionPolicy;

	@Getter
	private final T t;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param retentionPolicy
	 *            retention policy
	 */
	public A(final T t, final RetentionPolicy retentionPolicy) {
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
	 * Get member names.
	 * 
	 * @return member names
	 */
	public Set<String> getMemberNames() {
		return this.members.keySet();
	}

	/**
	 * Get member value.
	 * 
	 * @return value
	 */
	public Object getMemberValue() {
		return this.members.get("value");
	}

	/**
	 * Get member value.
	 * 
	 * @param name
	 *            name
	 * @return value
	 */
	public Object getMemberValue(final String name) {
		return this.members.get(name);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(getT().getName());
		for (final String name : getMemberNames()) {
			sb.append("  ").append(name).append("=").append(getMemberValue(name));
		}
		return sb.toString();
	}

}