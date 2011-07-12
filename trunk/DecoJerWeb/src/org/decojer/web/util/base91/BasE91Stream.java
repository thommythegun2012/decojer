/*
 * basE91 command line front-end
 *
 * Copyright (c) 2000-2006 Joachim Henke
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of Joachim Henke nor the names of his contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.decojer.web.util.base91;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class BasE91Stream {

	private static void decode(final InputStream is, final OutputStream os) {
		int s;
		final byte[] ibuf = new byte[65536];
		final byte[] obuf = new byte[57344];
		final BasE91 b91 = new BasE91();
		try {
			while ((s = is.read(ibuf)) > 0) {
				s = b91.decode(ibuf, s, obuf);
				os.write(obuf, 0, s);
			}
			s = b91.decEnd(obuf);
			os.write(obuf, 0, s);
		} catch (final Exception e) {
			System.err.println(e);
		}
	}

	private static void encode(final InputStream is, final OutputStream os) {
		int s;
		final byte[] ibuf = new byte[53248];
		final byte[] obuf = new byte[65536];
		final BasE91 b91 = new BasE91();
		try {
			while ((s = is.read(ibuf)) > 0) {
				s = b91.encode(ibuf, s, obuf);
				os.write(obuf, 0, s);
			}
			s = b91.encEnd(obuf);
			os.write(obuf, 0, s);
		} catch (final Exception e) {
			System.err.println(e);
		}
	}

	private static void encodeWrap(final InputStream is, final OutputStream os) {
		int i, s;
		int n = 0;
		final byte[] ibuf = new byte[53248];
		final byte[] obuf = new byte[65536];
		final char[] line = new char[76];
		final BasE91 b91 = new BasE91();
		try {
			final PrintStream ps = new PrintStream(os, false, "US-ASCII");
			while ((s = is.read(ibuf)) > 0) {
				s = b91.encode(ibuf, s, obuf);
				for (i = 0; i < s; ++i) {
					line[n++] = (char) obuf[i];
					if (n == 76) {
						ps.println(line);
						n = 0;
					}
				}
			}
			s = b91.encEnd(obuf);
			for (i = 0; i < s; ++i) {
				line[n++] = (char) obuf[i];
				if (n == 76) {
					ps.println(line);
					n = 0;
				}
			}
			if (n > 0) {
				ps.println(new String(line, 0, n));
			}
		} catch (final Exception e) {
			System.err.println(e);
		}
	}
}