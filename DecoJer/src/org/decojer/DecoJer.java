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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.transformer.TrControlFlowAnalysis;
import org.decojer.cavaj.transformer.TrDataFlowAnalysis;
import org.decojer.cavaj.transformer.TrInitControlFlowGraph;
import org.decojer.cavaj.transformer.TrIvmCfg2JavaExprStmts;
import org.decojer.cavaj.transformer.TrJvmStruct2JavaAst;
import org.decojer.cavaj.transformer.TrMergeAll;
import org.decojer.cavaj.transformer.TrStructCfg2JavaControlFlowStmts;
import org.decojer.cavaj.util.MagicNumbers;

/**
 * DecoJer.
 * 
 * @author André Pankraz
 */
public class DecoJer {

	private final static Logger LOGGER = Logger.getLogger(DecoJer.class.getName());

	private static final Charset UTF8 = Charset.forName("utf-8");

	/**
	 * Analyze file.
	 * 
	 * @param is
	 *            input stream
	 * @return interesting artifacts
	 * @throws IOException
	 *             read exception
	 */
	public static int analyze(final InputStream is) throws IOException {
		final byte[] magicNumber = new byte[MagicNumbers.LENGTH];
		final int read = is.read(magicNumber, 0, magicNumber.length);
		if (read < magicNumber.length) {
			return 0;
		}
		if (Arrays.equals(magicNumber, MagicNumbers.CLASS)) {
			return 1;
		}
		if (Arrays.equals(magicNumber, MagicNumbers.DEX)
				|| Arrays.equals(magicNumber, MagicNumbers.ODEX)) {
			return 1;
		}
		if (Arrays.equals(magicNumber, MagicNumbers.ZIP)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			final ZipInputStream zip = new ZipInputStream(pis);
			int nr = 0;
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				nr += analyze(zip);
			}
			return nr;
		}
		return 0;
	}

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

		final DU du = cu.getStartTd().getT().getDu();

		final List<TD> tds = cu.getAllTds();
		final HashSet<TD> processedTds = new HashSet<TD>();

		boolean changed;
		do {
			changed = false;
			for (int i = 0; i < tds.size(); ++i) {
				final TD td = tds.get(i);
				if (processedTds.contains(td)) {
					continue;
				}
				TrJvmStruct2JavaAst.transform(td);

				TrInitControlFlowGraph.transform(td);
				TrDataFlowAnalysis.transform(td);

				TrIvmCfg2JavaExprStmts.transform(td);

				TrControlFlowAnalysis.transform(td);
				TrStructCfg2JavaControlFlowStmts.transform(td);

				processedTds.add(td);
			}
			// many steps here can add type declarations through lazy finding
			for (final Entry<String, TD> entry : du.getTds()) {
				if (entry.getKey().startsWith(cu.getTds().get(0).getT().getName() + "$")) {
					if (cu.addTd(entry.getValue())) {
						changed = true;
					}
				}
			}
		} while (changed);
		// TODO
		// catch errors and in case of errors, do it again for startTd only,
		// if all is OK, add main type siblings
		// if all is OK and source attribute is OK, add source code siblings

		TrMergeAll.transform(cu);

		if (cu.isStartTdOnly()) {
			cu.setSourceFileName(cu.getStartTd().getT().getPName() + ".java");
		} else {
			final List<TD> rootTds = cu.getTds();
			final TD td = rootTds.get(0);
			// if (td.getSourceFileName() != null) {
			// cu.setSourceFileName(td.getSourceFileName());
			cu.setSourceFileName(td.getT().getPName() + ".java");
		}
		return cu.createSourceCode();
	}

	/**
	 * Decompile all type declarations from decompilation unit into output stream.
	 * 
	 * @param du
	 *            decompilation unit
	 * @param os
	 *            output stream
	 * @throws IOException
	 *             read exception
	 */
	public static void decompile(final DU du, final OutputStream os) throws IOException {
		final ZipOutputStream zip = new ZipOutputStream(os);

		final Iterator<Entry<String, TD>> tdIt = du.getTds().iterator();
		while (tdIt.hasNext()) {
			final TD td = tdIt.next().getValue();
			try {
				if (td.getCu() != null) {
					continue;
				}
				final CU cu = createCu(td);
				final String source = decompile(cu);
				final String sourceFileName = cu.getSourceFileName();
				final String packageName = td.getT().getPackageName();
				String zipEntryName;
				if (packageName != null && packageName.length() != 0) {
					zipEntryName = packageName.replace('.', '/') + '/' + sourceFileName;
				} else {
					zipEntryName = sourceFileName;
				}
				final ZipEntry zipEntry = new ZipEntry(zipEntryName);
				zip.putNextEntry(zipEntry);
				zip.write(source.getBytes(UTF8));
			} catch (final Throwable t) {
				LOGGER.log(Level.WARNING, "Decompilation problems for '" + td + "'!", t);
			} finally {
				tdIt.remove(); // cannot use cu.clear() here because processed info lost then
			}
		}
		zip.finish();
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
			throw new DecoJerException("Must be a path to a class file: " + path);
		}
		final DU du = createDu();
		final TD td = du.read(path);
		final CU cu = createCu(td);
		return decompile(cu);
	}

	/**
	 * Decompile files (class file / archive / directory) and write source codes into derived files
	 * (source file / archive / directory).
	 * 
	 * @param path
	 *            path to class file / archive / directory, e.g.
	 *            D:/.../[filename].jar!/.../...$[typename].class
	 */
	public static void decompileAll(final String path) {
		// later...
	}

	/**
	 * Main test method.
	 * 
	 * @param args
	 *            args - currently unused
	 * @throws IOException
	 *             read exception
	 */
	public static void main(final String[] args) throws IOException {
		final long time = System.currentTimeMillis();
		final DU du = createDu();
		switch (4) {
		case 0:
			System.out
					.println(decompile("D:/Data/Decomp/workspace/DecoJerTest/bin/org/decojer/cavaj/test/DecTestFields.class"));
			break;
		case 1: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar");
			final CU cu = createCu(du.getTd("org.decojer.cavaj.test.DecTestBooleanOperators"));
			System.out.println(decompile(cu));
			break;
		}
		case 2: {
			final TD td = du
					.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar!/org/decojer/cavaj/test/jdk5/DecTestAnnotations.class");
			final CU cu = createCu(td);
			System.out.println(decompile(cu));
			break;
		}
		case 3: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar");
			decompile(du, new FileOutputStream(new File(
					"D:/Data/Decomp/workspace/DecoJerTest/dex/classes_source.jar")));
			break;
		}
		case 4: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/myCinema_v1.6.1.jar");
			decompile(
					du,
					new FileOutputStream(
							new File(
									"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/myCinema_v1.6.1_source.jar")));
			break;
		}
		case 5: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/org.eclipse.jdt.core_3.7.0.v_B61.jar");
			decompile(
					du,
					new FileOutputStream(
							new File(
									"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/org.eclipse.jdt.core_3.7.0.v_B61_source.jar")));
			break;
		}
		case 11: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex");
			final CU cu = createCu(du.getTd("org.decojer.cavaj.test.jdk5.DecTestMethods"));
			System.out.println(decompile(cu));
			break;
		}
		case 12: {
			final TD td = du
					.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex!/org/decojer/cavaj/test/DecTestBooleanOperators.class");
			final CU cu = createCu(td);
			System.out.println(decompile(cu));
			break;
		}
		case 13: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex");
			decompile(du, new FileOutputStream(new File(
					"D:/Data/Decomp/workspace/DecoJerTest/dex/classes_source.jar")));
			break;
		}
		case 14: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2.apk");
			decompile(
					du,
					new FileOutputStream(
							new File(
									"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2_source.jar")));
			break;
		}
		}
		System.out.println("TIME: " + (System.currentTimeMillis() - time) / 1000);
	}

	private DecoJer() {
		// don't initialize
	}

}