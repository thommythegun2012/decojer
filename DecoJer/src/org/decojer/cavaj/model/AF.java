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

/**
 * Access flag.
 * 
 * @author André Pankraz
 */
public enum AF {

	/**
	 * Public.
	 */
	PUBLIC(0x1, "public", true, true, true),
	/**
	 * Private.
	 */
	PRIVATE(0x2, "private", true, true, true),
	/**
	 * Protected.
	 */
	PROTECTED(0x4, "protected", true, true, true),
	/**
	 * Static.
	 */
	STATIC(0x8, "static", true, true, true),
	/**
	 * Final.
	 */
	FINAL(0x10, "final", true, true, true),
	/**
	 * Synchronized.
	 */
	SYNCHRONIZED(0x20, "synchronized", false, false, true),
	/**
	 * Volatile.
	 */
	VOLATILE(0x40, "volatile", false, true, false),
	/**
	 * Bridge.
	 */
	BRIDGE(0x40, "bridge", false, false, true),
	/**
	 * Transient.
	 */
	TRANSIENT(0x80, "transient", false, true, false),
	/**
	 * Varargs.
	 */
	VARARGS(0x80, "varargs", false, false, true),
	/**
	 * Native.
	 */
	NATIVE(0x100, "native", false, false, true),
	/**
	 * Interface.
	 */
	INTERFACE(0x200, "interface", true, false, false),
	/**
	 * Abstract.
	 */
	ABSTRACT(0x400, "abstract", true, false, true),
	/**
	 * Strictfp.
	 */
	STRICTFP(0x800, "strictfp", false, false, true),
	/**
	 * Synthetic.
	 */
	SYNTHETIC(0x1000, "synthetic", true, true, true),
	/**
	 * Annotation.
	 */
	ANNOTATION(0x2000, "annotation", true, false, false),
	/**
	 * Enum.
	 */
	ENUM(0x4000, "enum", true, true, false),
	/**
	 * Constructor. (Dalvik only)
	 */
	CONSTRUCTOR(0x10000, "constructor", false, false, true),
	/**
	 * Declaraed synchronized. (Dalvik only)
	 */
	DECLARED_SYNCHRONIZED(0x20000, "declared-synchronized", false, false, true);

	private final boolean forType;

	private final boolean forField;

	private final boolean forMethod;

	private final String name;

	private final int value;

	private AF(final int value, final String name, final boolean forType,
			final boolean forField, final boolean forMethod) {
		this.value = value;
		this.name = name;
		this.forType = forType;
		this.forField = forField;
		this.forMethod = forMethod;
	}

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get value.
	 * 
	 * @return value
	 */
	public int getValue() {
		return this.value;
	}

	/**
	 * Is for field?
	 * 
	 * @return true - is for field
	 */
	public boolean isForField() {
		return this.forField;
	}

	/**
	 * Is for method?
	 * 
	 * @return true - is for method
	 */
	public boolean isForMethod() {
		return this.forMethod;
	}

	/**
	 * Is for type?
	 * 
	 * @return true - is for type
	 */
	public boolean isForType() {
		return this.forType;
	}

}