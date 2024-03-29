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
package org.decojer.cavaj.model.fields;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.types.T;

/**
 * Field.
 *
 * Unique identifier is: "name + descriptor"<br>
 * Even though the Java language has the field name as unique identifier, obfuscated code could
 * utilize the same name for different descriptors (see e.g. ojdbc6.jar).
 *
 * Also, an experiment:
 *
 * Field descriptor #77 Ljava/util/ArrayList;<br>
 * Signature: Ljava/util/ArrayList<Ljava/lang/Integer;>;<br>
 * public java.util.ArrayList test;
 *
 * works: getfield org.decojer.cavaj.test.DecTestFields.test : java.util.ArrayList [19]<br>
 * doesn't work: getfield org.decojer.cavaj.test.DecTestFields.test : java.util.List [19]<br>
 * throws: NoSuchFieldError extends IncompatibleClassChangeError
 *
 * @author André Pankraz
 */
public abstract class F implements Element {

	@Override
	public DU getDu() {
		return getT().getDu();
	}

	@Nonnull
	@Override
	public abstract T getT();

	/**
	 * Create field declaration for this field.
	 *
	 * @return {@code true} - success
	 */
	public abstract boolean createFd();

	/**
	 * Get value type.
	 *
	 * @return value type
	 */
	@Nonnull
	public abstract T getValueT();

	@Override
	public boolean isScala() {
		return getT().isScala();
	}

	/**
	 * Is enum?
	 *
	 * @return {@code true} - is enum
	 */
	public boolean isEnum() {
		return getAf(AF.ENUM);
	}

	public boolean isUnresolvable() {
		return true;
	}

	/**
	 * Field must be an enum (from Annotations attribute).
	 */
	public abstract void setEnum();

	/**
	 * Field must be static or dynamic (from usage, e.g. get/set).
	 *
	 * @param f
	 *            {@code true} - is static
	 */
	public abstract void setStatic(final boolean f);

	/**
	 * Set value for constant attributes. Type Integer: int, short, byte, char, boolean.
	 *
	 * @param value
	 *            value for constant attributes
	 */
	public abstract void setValue(@Nullable final Object value);

	@Override
	public String toString() {
		return getT() + "." + getName();
	}

	/**
	 * Set value type.
	 *
	 * @param valueT
	 *            value type
	 */
	public abstract void setValueT(@Nonnull final T valueT);

	/**
	 * Get value for constant attributes.
	 *
	 * Type Integer: int, short, byte, char, boolean.
	 *
	 * @return value for constant attributes
	 */
	@Nullable
	public abstract Object getValue();

}