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

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFileVisitor;

public class DexAnalyser {

	public static DexInfo analyse(final byte[] bytes) {
		final DexInfo dexInfo = new DexInfo();
		final DexFileReader dexFileReader = new DexFileReader(bytes);
		dexFileReader.accept(new DexFileVisitor() {

			private TypeInfo typeInfo;

			@Override
			public DexClassVisitor visit(final int access_flags,
					final String className, final String superClass,
					final String... interfaceNames) {
				this.typeInfo = new TypeInfo();
				this.typeInfo.name = className;
				final StringBuilder sb = new StringBuilder("L");
				sb.append(superClass);
				sb.append(";");
				for (int i = 0; i < interfaceNames.length; ++i) {
					sb.append("L").append(interfaceNames[i]).append(";");
				}
				this.typeInfo.signature = sb.toString();
				this.typeInfo.superName = superClass;
				return null;
			}

			@Override
			public void visitEnd() {
				if (this.typeInfo != null) {
					dexInfo.typeInfos.add(this.typeInfo);
				}
				this.typeInfo = null;
			}
		});
		return dexInfo;
	}
}