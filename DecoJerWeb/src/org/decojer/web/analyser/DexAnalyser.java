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
package org.decojer.web.analyser;

import java.io.IOException;
import java.io.InputStream;

import org.decojer.web.util.IOUtils;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFileVisitor;

public class DexAnalyser {

	public static DexInfo analyse(final InputStream is) throws IOException {
		final DexFileReader dexFileReader = new DexFileReader(
				IOUtils.toBytes(is));
		final DexInfo dexInfo = new DexInfo();
		dexFileReader.accept(new DexFileVisitor() {

			@Override
			public DexClassVisitor visit(final int access_flags,
					final String className, final String superClass,
					final String... interfaceNames) {
				// attention: all type names already with L...;
				final TypeInfo typeInfo = new TypeInfo();
				typeInfo.setName(className);
				typeInfo.setSuperName(superClass);
				typeInfo.setInterfaces(interfaceNames);
				dexInfo.typeInfos.add(typeInfo);
				return null;
			}

			@Override
			public void visitEnd() {
				// nothing, only 1 time called, not per above visit()!
			}
		});
		return dexInfo;
	}
}