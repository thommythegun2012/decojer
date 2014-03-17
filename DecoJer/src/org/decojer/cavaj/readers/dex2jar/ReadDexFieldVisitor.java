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
package org.decojer.cavaj.readers.dex2jar;

import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nullable;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;

import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;
import com.googlecode.dex2jar.visitors.DexFieldVisitor;

/**
 * Dex2jar read field visitor.
 *
 * @author André Pankraz
 */
public class ReadDexFieldVisitor implements DexFieldVisitor {

	@Nullable
	private A[] as;

	private F f;

	private final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor;

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexFieldVisitor(final DU du) {
		this.readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(du);
	}

	/**
	 * Init and set field.
	 *
	 * @param f
	 *            field
	 */
	public void init(final F f) {
		this.f = f;
		this.as = null;
	}

	@Override
	public DexAnnotationVisitor visitAnnotation(final String name, final boolean visible) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readDexAnnotationMemberVisitor.init(name,
				visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS);
		return this.readDexAnnotationMemberVisitor;
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.f.setAs(this.as);
		}
	}

}