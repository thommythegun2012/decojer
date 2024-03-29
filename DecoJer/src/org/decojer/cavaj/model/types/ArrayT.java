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
package org.decojer.cavaj.model.types;

import java.util.Map;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.DU;

import com.google.common.collect.Maps;

/**
 * Array type.
 *
 * @author André Pankraz
 */
@Getter
public final class ArrayT extends BaseT {

	/**
	 * Component type (could be an array type too, has one dimension less).
	 */
	@Getter
	@Setter
	@Nonnull
	private T componentT;

	@Getter
	@Nonnull
	private final DU du;

	private Map<String, Object> member;

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 * @param componentT
	 *            component type
	 */
	public ArrayT(@Nonnull final DU du, @Nonnull final T componentT) {
		this.du = du;
		this.componentT = componentT;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ArrayT)) {
			return false;
		}
		return getComponentT().equals(((ArrayT) obj).getComponentT());
	}

	@Override
	public boolean eraseTo(final T t) {
		if (!t.isArray()) {
			return false;
		}
		return getComponentT().eraseTo(t.getComponentT());
	}

	@Override
	public int getDimensions() {
		int dimensions = 0;
		for (T elementT = this; elementT != null && elementT.isArray(); elementT = elementT
				.getComponentT()) {
			++dimensions;
		}
		return dimensions;
	}

	@Override
	public T getElementT() {
		T elementT = this;
		while (elementT.isArray()) {
			elementT = elementT.getComponentT();
			assert elementT != null : this;
		}
		return elementT;
	}

	@Override
	public String getFullName() {
		return getComponentT().getFullName() + "[]";
	}

	@Override
	public T[] getInterfaceTs() {
		return getDu().getArrayInterfaceTs();
	}

	@Override
	public Map<String, Object> getMember() {
		// should only be used for getM("clone", "()Ljava/lang/Object;")
		if (this.member == null) {
			this.member = Maps.newHashMap();
		}
		assert this.member != null : this;
		return this.member;
	}

	@Override
	public String getName() {
		return getComponentT().getName() + "[]";
	}

	@Override
	public T getSuperT() {
		return getDu().getObjectT();
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		if (super.isAssignableFrom(t)) {
			return true;
		}
		if (t == null || !t.isArray()) {
			return false;
		}
		final T componentT = t.getComponentT();
		assert componentT != null : this;
		if ((getComponentT().getKind() & componentT.getKind()) == 0) {
			// even though arrays are covariant in the Java language, no auto-conversion is applied
			// here and "int[] is = new byte[1]" isn't allowed in Java:
			// isAssignableFrom() usually means "is-superclass-of" in JVM function, but even though
			// "int i = short/etc." is not an allowed assignment by inheritence (it's an
			// auto-conversion) we allow it here
			return false;
		}
		return getComponentT().isAssignableFrom(t.getComponentT());
	}

	@Override
	public boolean isUnresolvable() {
		return getComponentT().isUnresolvable();
	}

}