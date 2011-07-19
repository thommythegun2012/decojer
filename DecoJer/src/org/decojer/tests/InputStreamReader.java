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
package org.decojer.tests;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javassist.bytecode.ClassFile;

/**
 * 
 * @author André Pankraz
 */
public class InputStreamReader {

	private static int _MAGIC_NUMBER_MAX_LENGTH = 4;

	private static byte[] MAGIC_NUMBER = new byte[_MAGIC_NUMBER_MAX_LENGTH];

	private static final byte[] MAGIC_NUMBER_DEX_CLASS = { (byte) 0x64,
			(byte) 0x65, (byte) 0x78, (byte) 0x0A };

	private static final byte[] MAGIC_NUMBER_JAVA_CLASS = { (byte) 0xCA,
			(byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

	private static final byte[] MAGIC_NUMBER_ZIP = { (byte) 0x50, (byte) 0x4B,
			(byte) 0x03, (byte) 0x04 };

	public static String toHexString(final byte[] bytes) {
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'a', 'b', 'c', 'd', 'e', 'f' };
		final char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v / 16];
			hexChars[j * 2 + 1] = hexArray[v % 16];
		}
		return new String(hexChars);
	}

	private final MessageDigest messageDigest;

	/**
	 * @throws NoSuchAlgorithmException
	 * 
	 */
	public InputStreamReader() throws NoSuchAlgorithmException {
		this.messageDigest = MessageDigest.getInstance("MD5");
	}

	public void visitStream(final InputStream is) throws IOException {
		final int read = is.read(MAGIC_NUMBER, 0, _MAGIC_NUMBER_MAX_LENGTH);
		if (read < _MAGIC_NUMBER_MAX_LENGTH) {
			System.out.println("  MN too small: " + read);
			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_JAVA_CLASS, MAGIC_NUMBER)) {
			System.out.println("  Java!");

			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(MAGIC_NUMBER, 0, _MAGIC_NUMBER_MAX_LENGTH);

			final DigestInputStream dis = new DigestInputStream(pis,
					this.messageDigest);

			if (false) {
				final DataInputStream dip = new DataInputStream(dis);
				final ClassFile classFile = new ClassFile(dip);
				// System.out.println("  Classfile: " + classFile.getName() +
				// "  "
				// + toHexString(this.messageDigest.digest()));
			} else {
				ClassReaderTest.decompileClass(dis, null);
			}

			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_DEX_CLASS, MAGIC_NUMBER)) {
			System.out.println("  Dex!");

			// use http://code.google.com/p/dex2jar/

			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_ZIP, MAGIC_NUMBER)) {
			System.out.println("  Zip!");

			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(MAGIC_NUMBER, 0, _MAGIC_NUMBER_MAX_LENGTH);
			final ZipInputStream zip = new ZipInputStream(pis);

			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				System.out.println("Entry: " + zipEntry.getName());

				visitStream(zip);
			}
			return;
		}
		System.out.println("  Unknown!");
		return;
	}
}