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
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.utils.MagicNumbers;

/**
 * DecoJer.
 * 
 * @author André Pankraz
 */
public class DecoJer {

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
	 * Create decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public static DU createDu() {
		return new DU();
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
		final DU du = createDu();
		du.read(path);
		return du.getCus().get(0).decompile();
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
		switch (5) {
		case 0:
			System.out
					.println(decompile("D:/Data/Decomp/workspace/DecoJerTest/bin/org/decojer/cavaj/test/DecTestFields.class"));
			break;
		case 1: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar");
			System.out.println(du.decompile("org.decojer.cavaj.test.DecTestBooleanOperators"));
			break;
		}
		case 2: {
			System.out
					.println(decompile("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar!/org/decojer/cavaj/test/jdk5/DecTestAnnotations.class"));
			break;
		}
		case 3: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.jar");
			du.decompileAll(new FileOutputStream(new File(
					"D:/Data/Decomp/workspace/DecoJerTest/dex/classes_source.jar")));
			break;
		}
		case 4: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/myCinema_v1.6.1.jar");
			du.decompileAll(new FileOutputStream(
					new File(
							"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/myCinema_v1.6.1_source.jar")));
			break;
		}
		case 5: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/org.eclipse.jdt.core_3.7.0.v_B61.jar");
			du.decompileAll(new FileOutputStream(
					new File(
							"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/org.eclipse.jdt.core_3.7.0.v_B61_source.jar")));
			break;
		}
		case 11: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex");
			System.out.println(du.decompile("org.decojer.cavaj.test.jdk5.DecTestMethods"));
			break;
		}
		case 12: {
			System.out
					.println(decompile("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex!/org/decojer/cavaj/test/DecTestBooleanOperators.class"));
			break;
		}
		case 13: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/dex/classes.dex");
			du.decompileAll(new FileOutputStream(new File(
					"D:/Data/Decomp/workspace/DecoJerTest/dex/classes_source.jar")));
			break;
		}
		case 14: {
			du.read("D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2.apk");
			du.decompileAll(new FileOutputStream(
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