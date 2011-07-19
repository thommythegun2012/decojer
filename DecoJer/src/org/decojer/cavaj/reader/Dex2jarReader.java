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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.decojer.cavaj.model.type.Types;
import org.decojer.cavaj.reader.dex2jar.AnalyseDexFileVisitor;

import com.googlecode.dex2jar.reader.DexFileReader;

/**
 * @author André Pankraz
 */
public class Dex2jarReader {

	public static Types analyse(final InputStream is) throws IOException {
		final DexFileReader dexFileReader = new DexFileReader(is);
		final AnalyseDexFileVisitor analyseDexFileVisitor = new AnalyseDexFileVisitor();
		dexFileReader.accept(analyseDexFileVisitor);
		return analyseDexFileVisitor.getTypes();
	}

	public static void main(final String[] args) throws IOException {
		final FileInputStream is = new FileInputStream(
				"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex");
		final Types types = analyse(is);
		System.out.println("Ana: " + types.getTypes().size());
	}

}