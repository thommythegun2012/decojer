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
package org.decojer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.transformer.TrControlFlowAnalysis;
import org.decojer.cavaj.transformer.TrDataFlowAnalysis;
import org.decojer.cavaj.transformer.TrIvmCfg2JavaExprStmts;
import org.decojer.cavaj.transformer.TrJvmCode2IvmCfg;
import org.decojer.cavaj.transformer.TrJvmStruct2JavaAst;
import org.decojer.cavaj.transformer.TrMergeAll;
import org.decojer.cavaj.transformer.TrQualifiedNames2Imports;
import org.decojer.cavaj.transformer.TrRemoveEmptyConstructor;
import org.decojer.cavaj.transformer.TrStructCfg2JavaControlFlowStmts;

/**
 * DecoJer.
 * 
 * @author André Pankraz
 */
public class DecoJer {

	/**
	 * Create compilation unit.
	 * 
	 * @param startTd
	 *            start type declaration
	 * @return compilation unit
	 */
	public static CU createCu(final TD startTd) {
		assert startTd != null;

		return new CU(startTd);
	}

	/**
	 * Create decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public static DU createDu() {
		return new DU();
	}

	/**
	 * Decompile compilation unit.
	 * 
	 * @param cu
	 *            compilation unit
	 * @return source code
	 */
	public static String decompile(final CU cu) {
		if (cu == null) {
			throw new DecoJerException("Compilation unit must not be null!");
		}

		if (!cu.addTd(cu.getStartTd())) {
			// cannot add startTd with parents
			cu.startTdOnly();
		}

		final List<TD> tds = cu.getAllTds();
		for (int i = 0; i < tds.size(); ++i) {
			final TD td = tds.get(i);

			TrJvmStruct2JavaAst.transform(td); // could add tds
			TrJvmCode2IvmCfg.transform(td); // could add tds

			TrDataFlowAnalysis.transform(td);
			TrIvmCfg2JavaExprStmts.transform(td);

			TrControlFlowAnalysis.transform(td);
			TrStructCfg2JavaControlFlowStmts.transform(td);

			TrRemoveEmptyConstructor.transform(td);
		}
		// TODO
		// catch errors and in case of errors, do it again for startTd only,
		// if all is OK, add main type siblings
		// if all is OK and source attribute is OK, add source code siblings

		TrMergeAll.transform(cu);
		TrQualifiedNames2Imports.transform(cu);

		return cu.createSourceCode();
	}

	/**
	 * Decompile single class file.
	 * 
	 * @param path
	 *            path to class file
	 * @return source code
	 * @throws IOException
	 *             read exception
	 */
	public static String decompile(final String path) throws IOException {
		if (path == null) {
			throw new DecoJerException("Path must not be null!");
		}
		final File file = new File(path);
		if (!file.exists()) {
			throw new DecoJerException("File not found: " + path);
		}
		if (file.isDirectory()) {
			throw new DecoJerException("Directory path not supported: " + path);
		}
		final String typeFileName = file.getName();
		if (!typeFileName.endsWith(".class")) {
			throw new DecoJerException("Must be a path to a class file: "
					+ path);
		}
		final DU du = createDu();
		final TD td = du.read(path);
		final CU cu = createCu(td);
		return decompile(cu);
	}

	/**
	 * Decompile files (class file / archive / directory) and write source codes
	 * into derived files (source file / archive / directory).
	 * 
	 * @param path
	 *            path to class file / archive / directory, e.g.
	 *            D:/.../[filename].jar!/.../...$[typename].class
	 */
	public static void decompileAll(final String path) {
		// later...
	}

	public static void main(final String[] args) throws IOException {
		final DU du = createDu();
		switch (2) {
		case 0:
			System.out
					.println(decompile("D:/Data/Decomp/workspace/DecoJerTest/bin/org/decojer/cavaj/test/DecTestBooleanOperators.class"));
			break;
		case 1: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar");
			final CU cu = createCu(du
					.getTd("org.decojer.cavaj.test.DecTestBooleanOperators"));
			System.out.println(decompile(cu));
			break;
		}
		case 2: {
			final TD td = du
					.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar!/org/decojer/cavaj/test/DecTestBooleanOperators.class");
			final CU cu = createCu(td);
			System.out.println(decompile(cu));
			break;
		}
		case 3: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex");
			final CU cu = createCu(du
					.getTd("org.decojer.cavaj.test.DecTestBooleanOperators"));
			System.out.println(decompile(cu));
			break;
		}
		}
	}

	private DecoJer() {
		// don't initialize
	}

}