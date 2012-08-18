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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.transformers.TrCalculatePostorder;
import org.decojer.cavaj.transformers.TrCfg2JavaControlFlowStmts;
import org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts;
import org.decojer.cavaj.transformers.TrControlFlowAnalysis;
import org.decojer.cavaj.transformers.TrDataFlowAnalysis;
import org.decojer.cavaj.transformers.TrJvmStruct2JavaAst;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;

import com.google.common.base.Objects;
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

	private static final String JAVA_LANG = "java.lang";

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

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean check(final AF af) {
		return this.t.check(af);
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
	 * @param valueT
	 *            field value type
	 * @return field declaration
	 */
	public FD createFd(final String name, final T valueT) {
		return this.t.getF(name, valueT).createFd();
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
		return this.t.getM(name, descriptor).createMd();
	}

	public void decompile() {
		TrJvmStruct2JavaAst.transform(this);

		final List<BD> bds = getBds();
		for (int j = 0; j < bds.size(); ++j) {
			final BD bd = bds.get(j);
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			if (cfg == null || cfg.isIgnore()) {
				continue;
			}
			try {
				TrDataFlowAnalysis.transform(cfg);
				TrCalculatePostorder.transform(cfg);

				TrCfg2JavaExpressionStmts.transform(cfg);
				TrCalculatePostorder.transform(cfg);

				TrControlFlowAnalysis.transform(cfg);
				TrCfg2JavaControlFlowStmts.transform(cfg);
			} catch (final Throwable e) {
				LOGGER.log(Level.WARNING, "Cannot transform '" + cfg + "'!", e);
			}
		}
	}

	/**
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return this.t.getDu();
	}

	/**
	 * Get interface types.
	 * 
	 * @return interface types, not {@code null}
	 * @see T#getInterfaceTs()
	 */
	public T[] getInterfaceTs() {
		return this.t.getInterfaceTs();
	}

	/**
	 * Get name.
	 * 
	 * @return name
	 * @see T#getName()
	 */
	@Override
	public String getName() {
		return this.t.getName();
	}

	/**
	 * Get package name.
	 * 
	 * @return package name or {@code null} for no package
	 * @see T#getPackageName()
	 */
	public String getPackageName() {
		return this.t.getPackageName();
	}

	/**
	 * Get primary name.
	 * 
	 * @return primary name
	 * @see T#getPName()
	 */
	public String getPName() {
		return this.t.getPName();
	}

	/**
	 * Get inner name.
	 * 
	 * @return inner name
	 * @see T#getSimpleName()
	 */
	public String getSimpleName() {
		return this.t.getSimpleName();
	}

	/**
	 * Get super type.
	 * 
	 * @return super type
	 * @see T#getSuperT()
	 */
	public T getSuperT() {
		return this.t.getSuperT();
	}

	/**
	 * Get type parameters.
	 * 
	 * @return type parameters, not {@code null}
	 * @see T#getTypeParams()
	 */
	public T[] getTypeParams() {
		return this.t.getTypeParams();
	}

	/**
	 * Returns {@code true} if and only if the underlying class is an anonymous class.
	 * 
	 * @return {@code true} if and only if this class is an anonymous class.
	 * @see T#isAnonymous()
	 */
	public boolean isAnonymous() {
		return this.t.isAnonymous();
	}

	/**
	 * Is Dalvik?
	 * 
	 * @return true - is Dalvik
	 */
	public boolean isDalvik() {
		return this.version == 0;
	}

	/**
	 * New type name.
	 * 
	 * @param t
	 *            type
	 * @return AST type name
	 */
	public Name newTypeName(final T t) {
		final AST ast = getCu().getAst();
		final String packageName = t.getPackageName();
		if (!Objects.equal(getPackageName(), t.getPackageName())) {
			if (JAVA_LANG.equals(packageName)) {
				// ignore default package
				return ast.newName(t.getPName());
			}
			// full name
			// TODO later histogram for import candidates here?
			return ast.newName(t.getName());
		}
		// ignore same package
		if (t.getEnclosingT() == null) {
			return ast.newName(t.getPName());
		}
		// convert inner classes separator '$' into '.',
		// cannot use string replace because '$' is also a regular Java type name!
		// find common name dominator and stop there, for relative inner names
		final String name = getName();
		String simpleName = t.getSimpleName();
		final ArrayList<String> names = Lists.newArrayList(simpleName.length() > 0 ? simpleName : t
				.getPName());
		T enclosingT = t.getEnclosingT();
		while (enclosingT != null && !name.startsWith(enclosingT.getName())) {
			simpleName = enclosingT.getSimpleName();
			names.add(simpleName.length() > 0 ? simpleName : enclosingT.getPName());
			enclosingT = enclosingT.getEnclosingT();
		}
		return ast.newName(names.toArray(new String[names.size()]));
	}

	/**
	 * Resolve unfilled parameters.
	 */
	public void resolve() {
		// has other name in ClassT
		this.t.resolveFill();
	}

	/**
	 * Set access flags.
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public void setAccessFlags(final int accessFlags) {
		this.t.setAccessFlags(accessFlags);
	}

	/**
	 * Set interface types.
	 * 
	 * @param interfaceTs
	 *            interface types
	 */
	public void setInterfaceTs(final T[] interfaceTs) {
		this.t.setInterfaceTs(interfaceTs);
	}

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		final Cursor c = new Cursor();
		this.t.setTypeParams(getDu().parseTypeParams(signature, c));

		// TODO more checks for following overrides:
		final T superT = getDu().parseT(signature, c);
		if (superT != null) {
			this.t.setSuperT(superT);
		}
		final ArrayList<T> interfaceTs = new ArrayList<T>();
		while (true) {
			final T interfaceT = getDu().parseT(signature, c);
			if (interfaceT == null) {
				break;
			}
			interfaceTs.add(interfaceT);
		}
		if (!interfaceTs.isEmpty()) {
			if (this.t.getInterfaceTs().length != interfaceTs.size()) {
				LOGGER.info("Not matching Signature '" + signature + "' for Type Declaration: "
						+ this);
			} else {
				this.t.setInterfaceTs(interfaceTs.toArray(new T[interfaceTs.size()]));
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
		this.t.setSuperT(superT);
	}

	@Override
	public String toString() {
		return this.t.toString();
	}

}