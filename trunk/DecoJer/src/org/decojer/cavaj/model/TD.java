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

import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Lists;

/**
 * Type declaration. This includes Java class and interface declarations.
 * 
 * Names consist of dot-separated package names (for full name) and dollar-separated type names (but
 * dollar is a valid Java name char!)
 * 
 * @author André Pankraz
 */
public final class TD extends BD {

	private final static Logger LOGGER = Logger.getLogger(TD.class.getName());

	/**
	 * Source file name (from source file attribute).
	 */
	@Getter
	@Setter
	private String sourceFileName;

	@Getter
	private final ClassT t;

	/**
	 * AST type declaration.
	 */
	@Getter
	@Setter
	private ASTNode typeDeclaration;

	/**
	 * Class file version.
	 * 
	 * 1.0: 45.0, 1.1: 45.3, 1.2: 46, 1.3: 47, 1.4: 48, 5: 49, 6: 50, 7: 51, 8: 52
	 * 
	 * JDK 1.2 and 1.3 creates versions 1.1 if no target option given. JDK 1.4 creates 1.2 if no
	 * target option given.
	 */
	@Getter
	@Setter
	private int version;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            class type
	 */
	public TD(final ClassT t) {
		assert t != null;

		this.t = t;
	}

	@Override
	public boolean check(final AF af) {
		return getT().check(af);
	}

	/**
	 * Clear all generated data after read.
	 */
	@Override
	public void clear() {
		this.typeDeclaration = null;
		super.clear();
	}

	/**
	 * Create field declaration.
	 * 
	 * @param name
	 *            field name
	 * @param descriptor
	 *            field descriptor
	 * @return field declaration
	 */
	public FD createFd(final String name, final String descriptor) {
		return getT().getF(name, descriptor).createFd();
	}

	/**
	 * Create method declaration.
	 * 
	 * @param name
	 *            method name
	 * @param descriptor
	 *            method descriptor
	 * @return method declaration
	 */
	public MD createMd(final String name, final String descriptor) {
		return getT().getM(name, descriptor).createMd();
	}

	/**
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return getT().getDu();
	}

	/**
	 * Get enclosing type declaration.
	 * 
	 * @return enclosing type declaration
	 * 
	 * @see ClassT#setEnclosingT(T)
	 * @see Class#getEnclosingClass()
	 */
	public TD getEnclosingTd() {
		final T enclosingT = getT().getEnclosingT();
		return enclosingT == null ? null : enclosingT.getTd();
	}

	/**
	 * Get interface types.
	 * 
	 * @return interface types, not {@code null}
	 * @see T#getInterfaceTs()
	 */
	public T[] getInterfaceTs() {
		return getT().getInterfaceTs();
	}

	/**
	 * Get name.
	 * 
	 * @return name
	 * @see T#getName()
	 */
	@Override
	public String getName() {
		return getT().getName();
	}

	/**
	 * Get package name.
	 * 
	 * @return package name or {@code null} for no package
	 * @see T#getPackageName()
	 */
	public String getPackageName() {
		return getT().getPackageName();
	}

	/**
	 * Get primary name.
	 * 
	 * @return primary name
	 * @see T#getPName()
	 */
	public String getPName() {
		return getT().getPName();
	}

	/**
	 * Get inner name.
	 * 
	 * @return inner name
	 * @see T#getSimpleName()
	 */
	public String getSimpleName() {
		return getT().getSimpleName();
	}

	/**
	 * Get super type.
	 * 
	 * @return super type
	 * @see T#getSuperT()
	 */
	public T getSuperT() {
		return getT().getSuperT();
	}

	/**
	 * Get type parameters.
	 * 
	 * @return type parameters, not {@code null}
	 * @see T#getTypeParams()
	 */
	public T[] getTypeParams() {
		return getT().getTypeParams();
	}

	/**
	 * Returns {@code true} if and only if the underlying class is an anonymous class.<br>
	 * <br>
	 * This is heavily dependant from {@link T#getEnclosingT()}, which can be {@code null} for
	 * obfuscate code! Hence this is not a reliable answer.
	 * 
	 * @return {@code true} if and only if this class is an anonymous class.
	 * @see T#isAnonymous()
	 */
	public boolean isAnonymous() {
		return getT().isAnonymous();
	}

	/**
	 * Is this version at least of the given version?
	 * 
	 * @param version
	 *            version
	 * @return {@code true} - at least of given version
	 */
	public boolean isAtLeastVersion(final Version version) {
		return this.version >= version.getMajor();
	}

	/**
	 * Is this version less then given version?
	 * 
	 * @param version
	 *            version
	 * @return {@code true} - less then given version
	 */
	public boolean isBelowVersion(final Version version) {
		return this.version < version.getMajor();
	}

	/**
	 * Is Dalvik?
	 * 
	 * @return {@code true} - is Dalvik
	 */
	public boolean isDalvik() {
		return this.version == 0;
	}

	/**
	 * Is interface?
	 * 
	 * @return {@code true} - is interface
	 */
	public boolean isInterface() {
		return getT().isInterface();
	}

	/**
	 * Is Scala source code?
	 * 
	 * @return {@code true} - is Scala source code
	 */
	public boolean isScala() {
		return getSourceFileName().endsWith(".scala");
	}

	/**
	 * Parse interface types from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return interface types or {@code null}
	 */
	private T[] parseInterfaceTs(final String s, final Cursor c) {
		if (c.pos >= s.length() || s.charAt(c.pos) != 'L') {
			return null;
		}
		final List<T> ts = Lists.newArrayList();
		do {
			final T interfaceT = getT().getDu().parseT(s, c, getT());
			// not here...signature could be wrong (not bytecode checked), check erasure first
			// interfaceT.setInterface(true);
			ts.add(interfaceT);
		} while (c.pos < s.length() && s.charAt(c.pos) == 'L');
		return ts.toArray(new T[ts.size()]);
	}

	/**
	 * Resolve unfilled parameters.
	 */
	public void resolve() {
		// has other name in ClassT
		getT().resolved();
	}

	@Override
	public void setAccessFlags(final int accessFlags) {
		getT().setAccessFlags(accessFlags);
	}

	@Override
	public void setDeprecated() {
		getT().setDeprecated();
	}

	/**
	 * Set interface types.
	 * 
	 * @param interfaceTs
	 *            interface types
	 */
	public void setInterfaceTs(final T[] interfaceTs) {
		getT().setInterfaceTs(interfaceTs);
	}

	/**
	 * This should be scala code.
	 */
	public void setScala() {
		if (getSourceFileName() != null) {
			if (!isScala()) {
				LOGGER.warning("This should be a Scala source code!");
			}
			return;
		}
	}

	@Override
	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		final Cursor c = new Cursor();
		getT().setTypeParams(getDu().parseTypeParams(signature, c, getT()));

		final T superT = getDu().parseT(signature, c, getT());
		if (!superT.eraseTo(getSuperT())) {
			LOGGER.info("Cannot reduce type '" + superT + "' to super type '" + getSuperT()
					+ "' for type declaration '" + this + "' with signature: " + signature);
			return;
		}
		getT().setSuperT(superT);
		final T[] signInterfaceTs = parseInterfaceTs(signature, c);
		if (signInterfaceTs != null) {
			final T[] interfaceTs = getInterfaceTs();
			if (signInterfaceTs.length > interfaceTs.length) {
				// < can happen, e.g. scala-lift misses the final java.io.Serializable in signatures
				LOGGER.info("Cannot reduce interface types for type declaration '" + this
						+ "' with signature: " + signature);
				return;
			}
			for (int i = 0; i < signInterfaceTs.length; ++i) {
				final T interfaceT = signInterfaceTs[i];
				if (!interfaceT.eraseTo(interfaceTs[i])) {
					LOGGER.info("Cannot reduce type '" + interfaceT + "' to interface type '"
							+ interfaceTs[i] + "' for type declaration '" + this
							+ "' with signature: " + signature);
					return;
				}
				// erasure works...now we are safe to assert interface...but should be anyway
				// because erasure leads to right type, not necessary:
				// interfaceT.setInterface(true);
				assert interfaceT.isInterface();

				interfaceTs[i] = interfaceT;
			}
		}
	}

	/**
	 * Set super type.
	 * 
	 * @param superT
	 *            super type
	 */
	public void setSuperT(final T superT) {
		getT().setSuperT(superT);
	}

	@Override
	public void setSynthetic() {
		getT().setSynthetic();
	}

	@Override
	public String toString() {
		return getT().toString();
	}

}