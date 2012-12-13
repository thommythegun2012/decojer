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

import lombok.Getter;

/**
 * Access flag.
 * 
 * @author André Pankraz
 */
public enum AF {

	/**
	 * Public.
	 */
	PUBLIC(1 << 0, "public", true, true, true),
	/**
	 * Private.
	 */
	PRIVATE(1 << 1, "private", true, true, true), // inner
	/**
	 * Protected.
	 */
	PROTECTED(1 << 2, "protected", true, true, true), // inner
	/**
	 * Static.
	 */
	STATIC(1 << 3, "static", true, true, true), // inner
	/**
	 * Final.
	 */
	FINAL(1 << 4, "final", true, true, true),
	/**
	 * Super. Modern invokesuper syntax, is always set in current java.
	 */
	SUPER(1 << 5, "super", true, false, false), // not for inner
	/**
	 * Synchronized.
	 */
	SYNCHRONIZED(1 << 5, "synchronized", false, false, true),
	/**
	 * Volatile.
	 */
	VOLATILE(1 << 6, "volatile", false, true, false),
	/**
	 * Bridge.
	 */
	BRIDGE(1 << 6, "bridge", false, false, true),
	/**
	 * Transient.
	 */
	TRANSIENT(1 << 7, "transient", false, true, false),
	/**
	 * Varargs.
	 */
	VARARGS(1 << 7, "varargs", false, false, true),
	/**
	 * Native.
	 */
	NATIVE(1 << 8, "native", false, false, true),
	/**
	 * Interface.
	 */
	INTERFACE(1 << 9, "interface", true, false, false),
	/**
	 * Abstract.
	 */
	ABSTRACT(1 << 10, "abstract", true, false, true),
	/**
	 * Strictfp.
	 */
	STRICTFP(1 << 11, "strictfp", false, false, true),
	/**
	 * Synthetic.
	 */
	SYNTHETIC(1 << 12, "synthetic", true, true, true),
	/**
	 * Annotation.
	 */
	ANNOTATION(1 << 13, "annotation", true, false, false),
	/**
	 * Enum.
	 */
	ENUM(1 << 14, "enum", true, true, false),
	/**
	 * Constructor. (Dalvik only)
	 */
	CONSTRUCTOR(1 << 16, "constructor", false, false, true),
	/**
	 * Declaraed synchronized. (Dalvik only)
	 */
	DECLARED_SYNCHRONIZED(1 << 17, "declared-synchronized", false, false, true),

	/**
	 * Artificial flag 'unresolvable'.
	 */
	UNRESOLVABLE(1 << 30, "<unresolvable>", true, true, true),
	/**
	 * Artificial flag 'unresoled'.
	 */
	UNRESOLVED(1 << 31, "<unresolved>", true, true, true);

	@Getter
	private final boolean forType;

	@Getter
	private final boolean forField;

	@Getter
	private final boolean forMethod;

	@Getter
	private final String name;

	@Getter
	private final int value;

	private AF(final int value, final String name, final boolean forType, final boolean forField,
			final boolean forMethod) {
		this.value = value;
		this.name = name;
		this.forType = forType;
		this.forField = forField;
		this.forMethod = forMethod;
	}

}