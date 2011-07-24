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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map.Entry;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.PF;
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
		return new CU(startTd);
	}

	/**
	 * Create package fragment from package class file provider.
	 * 
	 * @param packageClassFileProvider
	 *            package class file provider
	 * @return package fragment
	 */
	public static PF createPf(
			final PackageClassStreamProvider packageClassFileProvider) {
		return new PF(packageClassFileProvider);
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
	 * Decompile class file.
	 * 
	 * @param path
	 *            path to class file
	 * @return source code
	 */
	public static String decompile(final String path) {
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
		final PF pf = createPf(new PackageClassStreamProvider(path));
		final String typeName = typeFileName.substring(0,
				typeFileName.length() - 6);
		final CU cu = createCu(pf.getTd(typeName));
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

	public static void main(final String[] args) {
		final PackageClassStreamProvider packageClassStreamProvider = new PackageClassStreamProvider(
				null);
		try {
			packageClassStreamProvider
					.addClassStream(
							"DecTestBooleanOperators",
							new DataInputStream(
									new FileInputStream(
											new File(
													"D:/Data/Decomp/workspace/DecoJerTest/bin/org/decojer/cavaj/test/jdk5/DecTestParametrizedMethods.class"))));
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final PF pf = createPf(packageClassStreamProvider);
		final Entry<String, TD> next = pf.getTds().entrySet().iterator().next();
		final CU cu = createCu(next.getValue());
		System.out.println(decompile(cu));

		// System.out
		// .println(decompile("E:/Decomp/workspace/DecoJerTest/bin_jdk1.6.0_26/org/decojer/cavaj/test/DecTestBooleanOperators.class"));
	}

	private DecoJer() {
		// don't initialize
	}

}