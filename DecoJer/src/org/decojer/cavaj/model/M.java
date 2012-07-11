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

import java.util.ArrayList;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.utils.Cursor;

/**
 * Method.
 * 
 * @author André Pankraz
 */
public class M {

	private final static Logger LOGGER = Logger.getLogger(M.class.getName());

	@Setter
	private int accessFlags;

	@Getter
	private final String descriptor;

	@Getter
	private final String name;

	private String[] paramNames;

	@Getter
	private T[] paramTs;

	@Getter
	private T returnT;

	@Getter
	private String signature;

	@Getter
	private final T t;

	/**
	 * Throws Types or <code>null</code>.
	 */
	@Getter
	@Setter
	private T[] throwsTs;

	@Getter
	private T[] typeParams;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	protected M(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.descriptor = descriptor;

		final Cursor c = new Cursor();
		this.paramTs = parseMethodParamTs(descriptor, c);
		this.returnT = t.getDu().parseT(descriptor, c);
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean check(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Get parameter name for index.
	 * 
	 * @param i
	 *            index (starts with 0, double/long params count as 1)
	 * @return parameter name
	 */
	public String getParamName(final int i) {
		if (this.paramNames == null || i >= this.paramNames.length || this.paramNames[i] == null) {
			return "arg" + i;
		}
		return this.paramNames[i];
	}

	/**
	 * Get parameter number.
	 * 
	 * @return parameter number
	 */
	public int getParams() {
		return this.paramTs.length;
	}

	/**
	 * Get parameter type for index.
	 * 
	 * @param i
	 *            index (starts with 0, double/long params count as 1)
	 * @return parameter type
	 */
	public T getParamT(final int i) {
		return this.paramTs[i];
	}

	/**
	 * Mark access flag.
	 * 
	 * @param af
	 *            access flag
	 */
	public void markAf(final AF af) {
		this.accessFlags |= af.getValue();
	}

	/**
	 * Parse method parameter types from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return method parameter types
	 */
	private T[] parseMethodParamTs(final String s, final Cursor c) {
		assert s.charAt(c.pos) == '(';

		++c.pos;
		final ArrayList<T> ts = new ArrayList<T>();
		while (s.charAt(c.pos) != ')') {
			ts.add(this.t.getDu().parseT(s, c));
		}
		++c.pos;
		return ts.toArray(new T[ts.size()]);
	}

	/**
	 * Parse Throw Types from Signature.
	 * 
	 * @param s
	 *            Signature
	 * @param c
	 *            Cursor
	 * @return Throw Types or <code>null</code>
	 */
	private T[] parseThrowsTs(final String s, final Cursor c) {
		if (c.pos >= s.length() || s.charAt(c.pos) != '^') {
			return null;
		}
		final ArrayList<T> ts = new ArrayList<T>();
		do {
			++c.pos;
			ts.add(this.t.getDu().parseT(s, c));
		} while (c.pos < s.length() && s.charAt(c.pos) == '^');
		return ts.toArray(new T[ts.size()]);
	}

	/**
	 * Set parameter name.
	 * 
	 * @param i
	 *            index
	 * @param name
	 *            parameter name
	 */
	public void setParamName(final int i, final String name) {
		if (this.paramNames == null) {
			this.paramNames = new String[this.paramTs.length];
		}
		this.paramNames[i] = name;
	}

	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		this.signature = signature;

		final Cursor c = new Cursor();
		this.typeParams = this.t.getDu().parseTypeParams(signature, c);

		// TODO more checks for following overrides:
		final T[] paramTs = parseMethodParamTs(signature, c);
		if (paramTs.length != 0) {
			if (this.paramTs.length != paramTs.length) {
				// can happen with Sun JVM:
				// see org.decojer.cavaj.test.jdk2.DecTestInnerS.Inner1.Inner11.1.InnerMethod
				// or org.decojer.cavaj.test.jdk5.DecTestEnumStatus
				// Signature since JDK 5 exists but doesn't contain synthetic parameters,
				// e.g. outer context for methods in inner classes: (I)V instead of (Lthis;_I_II)V
				// or enum constructor parameters arg0: String, arg1: int

				// ignore for now? Eclipse Compiler doesn't generate this information
				LOGGER.info("Not matching Signature '" + signature + "' for Method " + this);
			} else {
				this.paramTs = paramTs;
			}
		}
		final T returnT = this.t.getDu().parseT(signature, c);
		if (returnT != null) {
			this.returnT = returnT;
		}
		final T[] throwsTs = parseThrowsTs(signature, c);
		if (throwsTs != null) {
			this.throwsTs = throwsTs;
		}
	}

	@Override
	public String toString() {
		return getT() + "." + getName() + getDescriptor();
	}

}