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

import org.decojer.web.stream.StatClassVisitor;
import org.objectweb.asm.ClassReader;

public class ClassChecker {

	private ClassReader classReader;

	private final byte[] content;

	private StatClassVisitor statClassVisitor;

	public ClassChecker(final byte[] content) throws IOException {
		this.content = content;
		check();
	}

	private void check() throws IOException {
		if (this.classReader != null) {
			return;
		}
		this.classReader = new ClassReader(this.content);

		this.statClassVisitor = new StatClassVisitor();
		this.classReader.accept(this.statClassVisitor, ClassReader.SKIP_FRAMES);
	}

	public byte[] getContent() {
		return this.content;
	}

	public String getName() {
		return this.statClassVisitor.name;
	}

	public String getSignature() {
		return this.statClassVisitor.signature;
	}

}