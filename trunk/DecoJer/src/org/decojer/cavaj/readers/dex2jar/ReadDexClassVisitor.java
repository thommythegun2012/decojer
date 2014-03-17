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
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFieldVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;

/**
 * Dex2jar read class visitor.
 *
 * @author André Pankraz
 */
public class ReadDexClassVisitor implements DexClassVisitor {

	@Nullable
	private A[] as;

	private final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor;

	private final ReadDexFieldVisitor readDexFieldVisitor;

	private final ReadDexMethodVisitor readDexMethodVisitor;

	private T t;

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexClassVisitor(final DU du) {
		this.readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(du);
		this.readDexFieldVisitor = new ReadDexFieldVisitor(du);
		this.readDexMethodVisitor = new ReadDexMethodVisitor(du);
	}

	/**
	 * Init and set type.
	 *
	 * @param t
	 *            type
	 */
	public void init(final T t) {
		this.t = t;
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
			this.t.setAs(this.as);
		}
		this.t.resolve();
	}

	@Override
	public DexFieldVisitor visitField(final int accessFlags, final Field field, final Object value) {
		final F f = this.t.getF(field.getName(), field.getType());
		f.createFd();

		f.setAccessFlags(accessFlags);
		// TODO signature in annotation

		f.setValue(value);

		this.readDexFieldVisitor.init(f);
		return this.readDexFieldVisitor;
	}

	@Override
	public DexMethodVisitor visitMethod(final int accessFlags, final Method method) {
		final M m = this.t.getM(method.getName(), method.getDesc());
		m.createMd();

		m.setAccessFlags(accessFlags);
		// TODO throws in annotation

		this.readDexMethodVisitor.init(m);
		return this.readDexMethodVisitor;
	}

	@Override
	public void visitSource(final String file) {
		this.t.setSourceFileName(file);
	}

}