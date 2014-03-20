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
package org.decojer.cavaj.readers.asm;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ClassReader;

/**
 * Reader from ObjectWeb ASM.
 *
 * @author André Pankraz
 */
public class AsmReader implements ClassReader {

	private final ReadClassVisitor readClassVisitor;

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public AsmReader(final DU du) {
		assert du != null;

		this.readClassVisitor = new ReadClassVisitor(du);
	}

	@Nullable
	@Override
	public T read(final InputStream is) throws IOException {
		final org.objectweb.asm.ClassReader classReader = new org.objectweb.asm.ClassReader(is);

		this.readClassVisitor.init();
		try {
			classReader.accept(this.readClassVisitor, 0);
		} catch (final ReadException e) {
			// no other measure to stop ASM in reading
			return null;
		}
		return this.readClassVisitor.getT();
	}

}