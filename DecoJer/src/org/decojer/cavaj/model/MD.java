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

import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Method declaration.
 * 
 * @author André Pankraz
 */
public final class MD extends M implements BD, PD {

	private final static Logger LOGGER = Logger.getLogger(MD.class.getName());

	/**
	 * Annotation default value.
	 */
	@Getter
	@Setter
	private Object annotationDefaultValue;

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Control flow graph.
	 */
	@Getter
	@Setter
	private CFG cfg;

	/**
	 * Deprecated state (from deprecated attribute).
	 */
	@Getter
	@Setter
	private boolean deprecated;

	@Getter
	private String signature;

	/**
	 * Throws Types or <code>null</code>.
	 */
	@Getter
	@Setter
	private T[] throwsTs;

	@Getter
	private T[] typeParams;

	/**
	 * AST method declaration.
	 */
	@Getter
	@Setter
	private BodyDeclaration methodDeclaration;

	/**
	 * Method parameter annotations.
	 */
	@Getter
	@Setter
	private A[][] paramAss;

	/**
	 * Synthetic state (from synthetic attribute)
	 */
	@Getter
	@Setter
	private boolean synthetic;

	private String[] paramNames;

	/**
	 * Constructor.
	 * 
	 * @param td
	 *            owner type declaration
	 * @param name
	 *            method name
	 * @param descriptor
	 *            method descriptor
	 */
	protected MD(final TD td, final String name, final String descriptor) {
		super(td, name, descriptor);
	}

	@Override
	public void clear() {
		this.methodDeclaration = null;
		if (this.cfg != null) {
			this.cfg.clear();
		}
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
	 * Get owner type declaration.
	 * 
	 * @return owner type declaration
	 */
	public TD getTd() {
		return (TD) getT();
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
			ts.add(getT().getDu().parseT(s, c));
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
		this.typeParams = getT().getDu().parseTypeParams(signature, c);

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
		final T returnT = getT().getDu().parseT(signature, c);
		if (returnT != null) {
			this.returnT = returnT;
		}
		final T[] throwsTs = parseThrowsTs(signature, c);
		if (throwsTs != null) {
			this.throwsTs = throwsTs;
		}
	}

}