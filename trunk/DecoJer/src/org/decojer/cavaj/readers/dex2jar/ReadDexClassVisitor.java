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

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ReadVisitor;

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
@Slf4j
public class ReadDexClassVisitor implements DexClassVisitor, ReadVisitor {

	private A[] as;

	@Nonnull
	private final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor;

	@Nonnull
	private final ReadDexFieldVisitor readDexFieldVisitor;

	@Nonnull
	@Getter
	private final ReadDexFileVisitor parentVisitor;

	@Nonnull
	private final ReadDexMethodVisitor readDexMethodVisitor;

	@Getter
	private T t;

	/**
	 * Constructor.
	 *
	 * @param parentVisitor
	 *            parent visitor
	 */
	public ReadDexClassVisitor(@Nonnull final ReadDexFileVisitor parentVisitor) {
		this.parentVisitor = parentVisitor;
		this.readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(this);
		this.readDexFieldVisitor = new ReadDexFieldVisitor(this);
		this.readDexMethodVisitor = new ReadDexMethodVisitor(this);
	}

	@Override
	public DU getDu() {
		return getParentVisitor().getDu();
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
		final T aT = getDu().getDescT(name);
		if (aT == null) {
			log.warn(getT() + ": Cannot read annotation descriptor '" + name + "'!");
			return null;
		}
		final A a = new A(aT, visible);
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = a;
		return this.readDexAnnotationMemberVisitor.init(a);
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
		final String name = field.getName();
		final String desc = field.getType();
		if (name == null || desc == null) {
			return null;
		}
		final F f = this.t.getF(name, desc);
		f.createFd();

		f.setAccessFlags(accessFlags);
		// TODO signature in annotation

		f.setValue(value);

		this.readDexFieldVisitor.init(f);
		return this.readDexFieldVisitor;
	}

	@Override
	public DexMethodVisitor visitMethod(final int accessFlags, final Method method) {
		final String name = method.getName();
		final String desc = method.getDesc();
		if (name == null || desc == null) {
			return null;
		}
		final M m = this.t.getM(name, desc);
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