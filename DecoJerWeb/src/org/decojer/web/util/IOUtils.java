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
package org.decojer.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.github.libxjava.io.Base91;

/**
 * @author André Pankraz
 */
public class IOUtils {

	private static final Base91 BASE91 = new Base91(); // synchronized self-resetting, OK

	private static final Charset CHARSET = Charset.forName("US-ASCII");

	private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f' };

	public static byte[] base91Decode(final String str) {
		return BASE91.decode(str.getBytes(CHARSET));
	}

	public static String base91Encode(final byte[] bytes) {
		return new String(BASE91.encode(bytes), CHARSET);
	}

	public static int copy(final InputStream is, final OutputStream os) throws IOException {
		final byte[] buffer = new byte[4096];
		long count = 0;
		int n = 0;
		while (-1 != (n = is.read(buffer))) {
			os.write(buffer, 0, n);
			count += n;
		}
		return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
	}

	public static byte[] hexDecode(final String hex) {
		int j = hex.length();
		int i = j >> 1;
		final byte[] bytes = new byte[i];
		char v;
		while (i > 0) {
			v = hex.charAt(--j);
			bytes[--i] = (byte) (v - (v >= 'a' ? 'a' : '0'));
			v = hex.charAt(--j);
			bytes[i] |= (byte) (v - (v >= 'a' ? 'a' : '0')) << 4;
		}
		return bytes;
	}

	public static String hexEncode(final byte[] bytes) {
		int i = bytes.length;
		int t = i << 1;
		final char[] hex = new char[t];
		byte v;
		while (i > 0) {
			v = bytes[--i];
			hex[--t] = HEX_CHARS[v & 0x0F];
			hex[--t] = HEX_CHARS[v >> 4 & 0x0F];
		}
		return new String(hex);
	}

	public static void main(final String[] args) {
		final String test = "bmm)5FZG)g%oKX;3;1PIAAAAAA7?JB";
		try {
			final byte[] base91Decode = base91Decode(test);
			final byte[] md5bytes = new byte[16];
			System.arraycopy(base91Decode, 0, md5bytes, 0, 16);
			final String md5 = hexEncode(md5bytes);
			final long size = new DataInputStream(new ByteArrayInputStream(base91Decode, 16, 8))
					.readLong();
			System.out.println("TEST: " + md5 + " : " + size);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		// http://worker.decojer.appspot.com/admin/cleanup
	}

	public static byte[] md5(final byte[] content) {
		try {
			final MessageDigest m = MessageDigest.getInstance("MD5");
			return m.digest(content);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("Message digest algorithm 'MD5' unknown!", e);
		}
	}

	public static byte[] toBytes(final InputStream is) throws IOException {
		final ByteArrayOutputStream os = new ByteArrayOutputStream(32486);
		copy(is, os);
		return os.toByteArray();
	}

	public static String toKey(final byte[] md5Hash, final long size) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.write(md5Hash);
			boolean write = false;
			for (int i = 56; i >= 0; i -= 8) {
				final byte v = (byte) (size >>> i);
				if (v != 0) {
					write = true;
				}
				if (write) {
					dos.writeByte(v);
				}
			}
			dos.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return base91Encode(bos.toByteArray());
	}

	public static String toKey(final String md5Hash, final long size) {
		return toKey(hexDecode(md5Hash), size);
	}

}