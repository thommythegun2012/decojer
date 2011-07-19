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
package org.decojer.cavaj.reader.dex2jar;

import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;

import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFileVisitor;

/**
 * @author André Pankraz
 */
public class AnalyseDexFileVisitor implements DexFileVisitor {

	private final Types types = new Types();

	public Types getTypes() {
		return this.types;
	}

	@Override
	public DexClassVisitor visit(final int access_flags,
			final String className, final String superClass,
			final String... interfaceNames) {
		// TODO Auto-generated method stub
		this.types.addType(new Type(className));
		return null;
	}

	@Override
	public void visitEnd() {
		// TODO Auto-generated method stub

	}

}