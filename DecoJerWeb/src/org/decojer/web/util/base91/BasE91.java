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

public class BasE91 {

	private final byte[] dectab;

	private int ebq, en, dbq, dn, dv;

	public final byte[] enctab;

	public BasE91() {
		int i;
		final String ts = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&()*+,./:;<=>?@[]^_`{|}~\"";
		this.enctab = ts.getBytes();
		this.dectab = new byte[256];
		for (i = 0; i < 256; ++i) {
			this.dectab[i] = -1;
		}
		for (i = 0; i < 91; ++i) {
			this.dectab[this.enctab[i]] = (byte) i;
		}
		encReset();
		decReset();
	}

	public int decEnd(final byte[] ob) {
		int c = 0;
		if (this.dv != -1) {
			ob[c++] = (byte) (this.dbq | this.dv << this.dn);
		}
		decReset();
		return c;
	}

	public int decode(final byte[] ib, final int n, final byte[] ob) {
		int i, c = 0;
		for (i = 0; i < n; ++i) {
			if (this.dectab[ib[i]] == -1) {
				continue;
			}
			if (this.dv == -1) {
				this.dv = this.dectab[ib[i]];
			} else {
				this.dv += this.dectab[ib[i]] * 91;
				this.dbq |= this.dv << this.dn;
				this.dn += (this.dv & 8191) > 88 ? 13 : 14;
				do {
					ob[c++] = (byte) this.dbq;
					this.dbq >>= 8;
					this.dn -= 8;
				} while (this.dn > 7);
				this.dv = -1;
			}
		}
		return c;
	}

	public void decReset() {
		this.dbq = 0;
		this.dn = 0;
		this.dv = -1;
	}

	public int encEnd(final byte[] ob) {
		int c = 0;
		if (this.en > 0) {
			ob[c++] = this.enctab[this.ebq % 91];
			if (this.en > 7 || this.ebq > 90) {
				ob[c++] = this.enctab[this.ebq / 91];
			}
		}
		encReset();
		return c;
	}

	public int encode(final byte[] ib, final int n, final byte[] ob) {
		int i, c = 0;
		for (i = 0; i < n; ++i) {
			this.ebq |= (ib[i] & 255) << this.en;
			this.en += 8;
			if (this.en > 13) {
				int ev = this.ebq & 8191;
				if (ev > 88) {
					this.ebq >>= 13;
					this.en -= 13;
				} else {
					ev = this.ebq & 16383;
					this.ebq >>= 14;
					this.en -= 14;
				}
				ob[c++] = this.enctab[ev % 91];
				ob[c++] = this.enctab[ev / 91];
			}
		}
		return c;
	}

	public void encReset() {
		this.ebq = 0;
		this.en = 0;
	}
}