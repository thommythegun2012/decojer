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

import org.decojer.cavaj.model.MD;
import org.objectweb.asm.AnnotationVisitor;

import com.googlecode.dex2jar.visitors.DexAnnotationAble;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;

/**
 * Read DEX method visitor.
 * 
 * @author André Pankraz
 */
public class ReadDexMethodVisitor implements DexMethodVisitor {

	private MD md;

	/**
	 * Get method declaration.
	 * 
	 * @return method declaration
	 */
	public MD getMd() {
		return this.md;
	}

	/**
	 * Set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 */
	public void setMd(final MD md) {
		this.md = md;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name,
			final boolean visitable) {
		return new ReadDexAnnotationVisitor();
	}

	@Override
	public DexCodeVisitor visitCode() {
		return null;
	}

	@Override
	public void visitEnd() {
	}

	@Override
	public DexAnnotationAble visitParamesterAnnotation(final int index) {
		return null;
	}

}