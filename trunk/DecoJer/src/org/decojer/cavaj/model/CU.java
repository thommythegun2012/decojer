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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.model.types.TD;
import org.decojer.cavaj.model.types.Version;
import org.decojer.cavaj.transformers.TrJvmStruct2JavaAst;
import org.decojer.cavaj.transformers.TrLineNumberAnalysis;
import org.decojer.cavaj.transformers.TrMergeAll;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Compilation unit.
 * 
 * Can contain multiple type declarations, but only one with type name equal to source file name can
 * be public.
 * 
 * @author André Pankraz
 */
public final class CU extends Container {

	private final static Logger LOGGER = Logger.getLogger(CU.class.getName());

	/**
	 * AST compilation unit.
	 */
	@Getter
	@Setter
	private CompilationUnit compilationUnit;

	private final EnumSet<DFlag> dFlags = EnumSet.of(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC); // EnumSet.noneOf(DFlag.class);

	/**
	 * Source file name (calculated).
	 */
	@Getter
	private final String sourceFileName;

	/**
	 * Constructor.
	 * 
	 * @param td
	 *            main type declaration
	 * @param sourceFileName
	 *            source file name
	 */
	public CU(final TD td, final String sourceFileName) {
		assert td != null;
		assert sourceFileName != null;

		addTd(td);
		this.sourceFileName = sourceFileName;
	}

	/**
	 * Check decompile flag.
	 * 
	 * @param dFlag
	 *            decompile flag
	 * @return {@code true} - decompile flag is active
	 */
	public boolean check(final DFlag dFlag) {
		return this.dFlags.contains(dFlag);
	}

	/**
	 * Clear all generated data after read.
	 */
	@Override
	public void clear() {
		this.compilationUnit = null;
		super.clear();
	}

	/**
	 * Create source code.
	 * 
	 * @return source code
	 */
	public String createSourceCode() {
		final Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE,
				"1." + (getT().getVersion() - Version.JVM_1.getMajor() + 1));

		final Document document = new Document();
		final TextEdit edits = this.compilationUnit.rewrite(document, options);
		try {
			edits.apply(document);
		} catch (final MalformedTreeException e) {
			throw new DecoJerException("Couldn't create source code!", e);
		} catch (final BadLocationException e) {
			throw new DecoJerException("Couldn't create source code!", e);
		}
		String source = document.get();

		final String br = TextUtilities.getDefaultLineDelimiter(document);

		final int numberOfLines = document.getNumberOfLines();
		if (numberOfLines == 4) {
			// package + 2 empty lines + 1-line class
			// TODO more checks necessary
			LOGGER.warning("Couldn't format correctly '" + this + "'! Reformatting.");
			// now try another reformat
			final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
			final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0,
					source.length(), 0, br);
			if (edit == null) {
				LOGGER.warning("Couldn't reformat source code!");
			} else {
				// can happen if no known source code version has been found
				try {
					edit.apply(document);
				} catch (final MalformedTreeException e) {
					LOGGER.log(Level.WARNING, "Couldn't reformat source code!", e);
				} catch (final BadLocationException e) {
					LOGGER.log(Level.WARNING, "Couldn't reformat source code!", e);
				}
				source = document.get();
			}
		}
		packageAnnotationBug: if (this.compilationUnit.getPackage() != null
				&& this.compilationUnit.getPackage().annotations().size() > 0) {
			// bugfix for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=361071
			// for Eclipse 4.3 still necessary
			// see TrJvmStruct2JavaAst.transform(TD)
			final int pos = source.indexOf("package ");
			if (pos < 2) {
				break packageAnnotationBug;
			}
			final char ch = source.charAt(pos - 1);
			if (Character.isWhitespace(ch)) {
				break packageAnnotationBug;
			}
			source = source.substring(0, pos) + br + source.substring(pos);
		}
		// build class decompilation comment
		final StringBuilder sb = new StringBuilder(source);
		sb.append(br).append(br).append("/*").append(br)
				.append(" * Generated by DecoJer 0.9.1, a Java-bytecode decompiler.").append(br)
				.append(" * DecoJer Copyright (C) 2009-2013 André Pankraz. All Rights Reserved.")
				.append(br).append(" *").append(br);
		if (getT().isDalvik()) {
			sb.append(" * Dalvik File");
		} else {
			final int version = getT().getVersion();
			sb.append(" * Class File Version: ").append(version).append(" (Java ");
			if (version < Version.JVM_5.getMajor()) {
				sb.append("1.");
			}
			sb.append(version - Version.JVM_1.getMajor() + 1).append(')');
		}
		sb.append(br);
		if (getT().getSourceFileName() != null) {
			sb.append(" * Source File Name: ").append(getT().getSourceFileName()).append(br);
		}
		sb.append(" */");
		return sb.toString();
	}

	/**
	 * Decompile compilation unit.
	 * 
	 * Log runtime errors in CFG and continue.
	 * 
	 * @return source code
	 */
	public String decompile() {
		return decompile(true);
	}

	/**
	 * Decompile compilation unit.
	 * 
	 * @param ignoreCfgError
	 *            {@code true} - log runtime errors in CFG and continue
	 * 
	 * @return source code
	 */
	public String decompile(final boolean ignoreCfgError) {
		for (final TD td2 : getAllTds()) {
			final T td = td2.getT();
			TrJvmStruct2JavaAst.transform(td);

			final List<Element> bds = td.getDeclarations();
			for (int j = 0; j < bds.size(); ++j) {
				final Element bd = bds.get(j);
				if (!(bd instanceof M)) {
					continue;
				}
				final CFG cfg = ((M) bd).getCfg();
				if (cfg == null || cfg.isIgnore()) {
					continue;
				}
				try {
					cfg.decompile();
				} catch (final RuntimeException e) {
					if (ignoreCfgError) {
						LOGGER.log(Level.WARNING, "Cannot transform '" + cfg + "'!", e);
					} else {
						throw e;
					}
				} catch (final Error e) {
					if (ignoreCfgError) {
						LOGGER.log(Level.WARNING, "Cannot transform '" + cfg + "'!", e);
					} else {
						throw e;
					}
				} catch (final Throwable e) {
					LOGGER.log(Level.SEVERE, "Cannot transform '" + cfg + "'!", e);
				}
			}
		}
		TrLineNumberAnalysis.transform(this);
		TrMergeAll.transform(this);
		return createSourceCode();
	}

	/**
	 * Get abstract syntax tree.
	 * 
	 * @return abstract syntax tree
	 */
	public AST getAst() {
		final AST ast = getCompilationUnit().getAST();
		assert ast != null;

		return ast;
	}

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	@Override
	public String getName() {
		return getPackageName() + "." + this.sourceFileName;
	}

	/**
	 * Get package name.
	 * 
	 * @return package name
	 */
	public String getPackageName() {
		return getT().getPackageName();
	}

	/**
	 * Get first type declaration.
	 * 
	 * @return first type declaration
	 */
	public T getT() {
		return ((TD) getBds().get(0)).getT();
	}

	@Override
	public String toString() {
		return getName();
	}

}