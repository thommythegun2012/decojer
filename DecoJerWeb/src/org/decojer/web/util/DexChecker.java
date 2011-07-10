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

import java.io.IOException;

import org.decojer.web.stream.StatDexVisitor;

import com.googlecode.dex2jar.reader.DexFileReader;

public class DexChecker {

	private final byte[] content;

	private DexFileReader dexFileReader;

	private StatDexVisitor statDexVisitor;

	public DexChecker(final byte[] content) throws IOException {
		this.content = content;
		check();
	}

	private void check() throws IOException {
		if (this.dexFileReader != null) {
			return;
		}
		this.dexFileReader = new DexFileReader(this.content);

		this.statDexVisitor = new StatDexVisitor();
		this.dexFileReader.accept(this.statDexVisitor);
	}

	public int getClasses() {
		return this.statDexVisitor.classes;
	}

	public byte[] getContent() {
		return this.content;
	}

}