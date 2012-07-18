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
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.transformers.TrCalculatePostorder;
import org.decojer.cavaj.transformers.TrCfg2JavaControlFlowStmts;
import org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts;
import org.decojer.cavaj.transformers.TrControlFlowAnalysis;
import org.decojer.cavaj.transformers.TrDataFlowAnalysis;
import org.decojer.cavaj.transformers.TrJvmStruct2JavaAst;
import org.decojer.cavaj.transformers.TrMergeAll;
import org.decojer.cavaj.utils.TypeNameManager;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Compilation unit. Can contain multiple type declarations, but only one with type name equal to
 * source file name can be public.
 * 
 * @author André Pankraz
 */
public final class CU implements PD {

	private final static Logger LOGGER = Logger.getLogger(CU.class.getName());

	/**
	 * AST compilation unit.
	 */
	@Getter
	@Setter
	private CompilationUnit compilationUnit;

	private final EnumSet<DFlag> dFlags = EnumSet.noneOf(DFlag.class);

	private final TD mainTd;

	/**
	 * Source file name.
	 */
	@Getter
	@Setter
	private String sourceFileName;

	/**
	 * Sub type declarations.
	 */
	@Getter
	private final List<TD> tds = new ArrayList<TD>(2);

	@Getter
	private final TypeNameManager typeNameManager = new TypeNameManager(this);

	/**
	 * Constructor.
	 * 
	 * @param mainTd
	 *            main type declaration
	 */
	public CU(final TD mainTd) {
		assert mainTd != null;

		addTd(mainTd);
		this.mainTd = mainTd;
		getTypeNameManager().setPackageName(mainTd.getPackageName());
	}

	/**
	 * Add type declaration and add all parents.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void addTd(final TD td) {
		if (td.pd != null) {
			if (td.pd != this) {
				LOGGER.warning("Cannot change parent declaration for '" + td + "' from '" + td.pd
						+ "' to '" + this + "'!");
			}
			return;
		}
		td.pd = this;
		this.tds.add(td);
	}

	/**
	 * Check decompile flag.
	 * 
	 * @param dFlag
	 *            decompile flag
	 * @return true - decompile flag is active
	 */
	public boolean check(final DFlag dFlag) {
		return this.dFlags.contains(dFlag);
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		this.compilationUnit = null;
		for (final TD td : getTds()) {
			td.clear();
		}
	}

	/**
	 * Create source code.
	 * 
	 * @return source code
	 */
	public String createSourceCode() {
		final Document document = new Document();
		final TextEdit edits = this.compilationUnit.rewrite(document, null);
		try {
			edits.apply(document);
		} catch (final MalformedTreeException e) {
			throw new DecoJerException("Couldn't create source code!", e);
		} catch (final BadLocationException e) {
			throw new DecoJerException("Couldn't create source code!", e);
		}
		String sourceCode = document.get();

		packageAnnotationBug: if (this.compilationUnit.getPackage() != null
				&& this.compilationUnit.getPackage().annotations().size() > 0) {
			// bugfix for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=361071
			// for Eclipse 4.2 still necessary
			// see TrJvmStruct2JavaAst.transform(TD)
			final int pos = sourceCode.indexOf("package ");
			if (pos < 2) {
				break packageAnnotationBug;
			}
			final char ch = sourceCode.charAt(pos - 1);
			if (Character.isWhitespace(ch)) {
				break packageAnnotationBug;
			}
			sourceCode = sourceCode.substring(0, pos - 1) + "\n" + sourceCode.substring(pos);
		}
		// build class decompilation comment
		final StringBuilder sb = new StringBuilder(sourceCode);
		sb.append("\n\n/*\n").append(" * Generated by DecoJer ").append(0.9)
				.append(", a Java-bytecode decompiler.\n")
				.append(" * DecoJer Copyright (C) 2009-2011 André Pankraz. All Rights Reserved.\n")
				.append(" *\n");
		final int version = this.mainTd.getVersion();
		if (version == 0) {
			sb.append(" * Dalvik File");
		} else {
			sb.append(" * Class File Version: ").append(version).append(" (Java ");
			if (version <= 48) {
				sb.append("1.");
			}
			sb.append(version - 44).append(')');
		}
		sb.append('\n');
		if (this.mainTd.getSourceFileName() != null) {
			sb.append(" * Source File Name: ").append(this.mainTd.getSourceFileName()).append('\n');
		}
		sb.append(" */");
		return sb.toString();
	}

	/**
	 * Decompile compilation unit.
	 * 
	 * @return source code
	 */
	public String decompile() {
		for (final TD td : this.tds) {
			TrJvmStruct2JavaAst.transform(td);

			final List<BD> bds = td.getBds();
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
		TrMergeAll.transform(this);

		if (check(DFlag.START_TD_ONLY)) {
			setSourceFileName(this.mainTd.getPName() + ".java");
		} else {
			final List<TD> rootTds = getTds();
			final TD td = rootTds.get(0);
			// if (td.getSourceFileName() != null) {
			// cu.setSourceFileName(td.getSourceFileName());
			setSourceFileName(td.getPName() + ".java");
		}
		return createSourceCode();
	}

	/**
	 * Get abstract syntax tree.
	 * 
	 * @return abstract syntax tree
	 */
	public AST getAst() {
		return getCompilationUnit().getAST();
	}

	/**
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return this.mainTd.getDu();
	}

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	public String getName() {
		return this.mainTd.getName();
	}

	/**
	 * Get package name.
	 * 
	 * @return package name
	 */
	public String getPackageName() {
		return this.mainTd.getPackageName();
	}

	/**
	 * Get type declaration with name or full name.
	 * 
	 * @param name
	 *            name or full name
	 * @return type declaration or <code>null</code>
	 */
	public TD getTd(final String name) {
		String n = name;
		if (n.indexOf('.') == -1) {
			n = this.mainTd.getPackageName() + '.' + n;
		}
		for (final TD td : getTds()) {
			if (n.equals(td.getName())) {
				return td;
			}
		}
		return null;
	}

}