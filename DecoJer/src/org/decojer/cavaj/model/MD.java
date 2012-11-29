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
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Method declaration.
 * 
 * @author André Pankraz
 */
public final class MD extends BD {

	private final static Logger LOGGER = Logger.getLogger(MD.class.getName());

	/**
	 * Annotation default value.
	 */
	@Getter
	@Setter
	private Object annotationDefaultValue;

	/**
	 * Control flow graph.
	 */
	@Getter
	@Setter
	private CFG cfg;

	@Getter
	private final M m;

	@Getter
	private String signature;

	/**
	 * Throws types or {@code null}.
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

	private String[] paramNames;

	/**
	 * Constructor.
	 * 
	 * @param m
	 *            method
	 */
	protected MD(final M m) {
		assert m != null;

		this.m = m;
	}

	public boolean check(final AF af) {
		return this.m.check(af);
	}

	@Override
	public void clear() {
		this.methodDeclaration = null;
		if (this.cfg != null) {
			this.cfg.clear();
		}
		super.clear();
	}

	public String getDescriptor() {
		return this.m.getDescriptor();
	}

	@Override
	public String getName() {
		return this.m.getName();
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

	public T[] getParamTs() {
		return this.m.getParamTs();
	}

	public T getReturnT() {
		return this.m.getReturnT();
	}

	/**
	 * Get owner type declaration.
	 * 
	 * @return owner type declaration
	 */
	public TD getTd() {
		return ((ClassT) this.m.getT()).getTd();
	}

	/**
	 * Is constructor?
	 * 
	 * @return {@code true} - is constructor
	 * @see M#isConstructor()
	 */
	public boolean isConstructor() {
		return this.m.isConstructor();
	}

	/**
	 * Is initializer?
	 * 
	 * @return {@code true} - is initializer
	 * @see M#isInitializer()
	 */
	public boolean isInitializer() {
		return this.m.isInitializer();
	}

	/**
	 * Parse throw types from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return throw types or {@code null}
	 */
	private T[] parseThrowsTs(final String s, final Cursor c) {
		if (c.pos >= s.length() || s.charAt(c.pos) != '^') {
			return null;
		}
		final ArrayList<T> ts = new ArrayList<T>();
		do {
			++c.pos;
			ts.add(getTd().getDu().parseT(s, c, this.m));
		} while (c.pos < s.length() && s.charAt(c.pos) == '^');
		return ts.toArray(new T[ts.size()]);
	}

	public void setAccessFlags(final int accessFlags) {
		this.m.setAccessFlags(accessFlags);
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
			this.paramNames = new String[getParamTs().length];
		}
		this.paramNames[i] = name;
	}

	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		this.signature = signature;

		final Cursor c = new Cursor();
		this.typeParams = getTd().getDu().parseTypeParams(signature, c, this.m);

		final T[] paramTs = getParamTs();
		final T[] signParamTs = getTd().getDu().parseMethodParamTs(signature, c, this.m);
		if (signParamTs.length != 0) {
			if (paramTs.length != signParamTs.length) {
				// can happen with Sun JVM for constructor:
				// see org.decojer.cavaj.test.jdk2.DecTestInnerS.Inner1.Inner11.1.InnerMethod
				// or org.decojer.cavaj.test.jdk5.DecTestEnumStatus
				// Signature since JVM 5 exists but doesn't contain synthetic parameters,
				// e.g. outer context for methods in inner classes: (I)V instead of (Lthis;_I_II)V
				// or enum constructor parameters arg0: String, arg1: int
				if (!isConstructor()) {
					LOGGER.info("Cannot reduce signature '" + signature
							+ "' to types for method params: " + this);
				}
			} else {
				for (int i = 0; i < paramTs.length; ++i) {
					final T paramT = signParamTs[i];
					if (!paramT.isSignatureFor(paramTs[i])) {
						LOGGER.info("Cannot reduce signature '" + signature + "' to type '"
								+ paramTs[i] + "' for method param: " + this);
						break;
					}
					paramTs[i] = paramT;
				}
			}
		}
		final T returnT = getTd().getDu().parseT(signature, c, this.m);
		if (!returnT.isSignatureFor(getReturnT())) {
			LOGGER.info("Cannot reduce signature '" + signature + "' to type '" + getReturnT()
					+ "' for method return: " + this);
		} else {
			this.m.setReturnT(returnT);
		}
		final T[] signThrowTs = parseThrowsTs(signature, c);
		if (signThrowTs != null) {
			final T[] throwsTs = getThrowsTs();
			if (throwsTs.length != signThrowTs.length) {
				LOGGER.info("Cannot reduce signature '" + signature
						+ "' to types for method throws: " + this);
			}
			for (int i = 0; i < throwsTs.length; ++i) {
				final T throwT = signThrowTs[i];
				if (!throwT.isSignatureFor(throwsTs[i])) {
					LOGGER.info("Cannot reduce signature '" + signature + "' to type '"
							+ throwsTs[i] + "' for method throw: " + this);
					break;
				}
				throwsTs[i] = throwT;
			}
		}
	}

	@Override
	public String toString() {
		return this.m.toString();
	}

}