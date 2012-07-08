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
package org.decojer.cavaj.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.reader.dex2jar.ReadDexFileVisitor;

import com.googlecode.dex2jar.reader.DexFileReader;

/**
 * Reader from Dex2Jar.
 * 
 * @author André Pankraz
 */
public class Dex2jarReader implements DexReader {

	private final ReadDexFileVisitor readDexFileVisitor;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public Dex2jarReader(final DU du) {
		assert du != null;

		this.readDexFileVisitor = new ReadDexFileVisitor(du);
	}

	@Override
	public List<TD> read(final InputStream is, final String selector) throws IOException {
		final DexFileReader dexFileReader = new DexFileReader(is);

		this.readDexFileVisitor.init(selector);
		dexFileReader.accept(this.readDexFileVisitor);

		return this.readDexFileVisitor.getTds();
	}

}