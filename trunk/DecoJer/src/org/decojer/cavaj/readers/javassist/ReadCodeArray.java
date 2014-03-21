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
package org.decojer.cavaj.readers.javassist;

import javax.annotation.Nonnull;

/**
 * Code array reader.
 *
 * @author André Pankraz
 */
public class ReadCodeArray {

	@Nonnull
	public final byte[] code;

	public int pc;

	/**
	 * Constructor.
	 *
	 * @param code
	 *            code
	 */
	public ReadCodeArray(@Nonnull final byte[] code) {
		this.code = code;
	}

	public boolean isNext() {
		return this.pc < this.code.length;
	}

	public int readSignedByte() {
		return this.code[this.pc++];
	}

	public int readSignedInt() {
		return this.code[this.pc++] << 24 | (this.code[this.pc++] & 0xff) << 16
				| (this.code[this.pc++] & 0xff) << 8 | this.code[this.pc++] & 0xff;
	}

	public int readSignedShort() {
		return this.code[this.pc++] << 8 | this.code[this.pc++] & 0xff;
	}

	public int readUnsignedByte() {
		return this.code[this.pc++] & 0xff;
	}

	public int readUnsignedInt() {
		return (this.code[this.pc++] & 0xff) << 24 | (this.code[this.pc++] & 0xff) << 16
				| (this.code[this.pc++] & 0xff) << 8 | this.code[this.pc++] & 0xff;
	}

	public int readUnsignedShort() {
		return (this.code[this.pc++] & 0xff) << 8 | this.code[this.pc++] & 0xff;
	}

}