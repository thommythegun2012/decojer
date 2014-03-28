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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ReadVisitor;

import com.googlecode.dex2jar.visitors.DexAnnotationAble;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;

/**
 * Dex2jar read method visitor.
 *
 * @author André Pankraz
 */
@Slf4j
public class ReadDexMethodVisitor implements DexMethodVisitor, ReadVisitor {

	private A[] as;

	@Getter(AccessLevel.PROTECTED)
	private M m;

	private A[][] paramAss;

	@Getter
	@Nonnull
	private final ReadDexClassVisitor parentVisitor;

	@Nonnull
	private final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor;

	@Nonnull
	private final ReadDexCodeVisitor readDexCodeVisitor;

	/**
	 * Constructor.
	 *
	 * @param parentVisitor
	 *            parent visitor
	 */
	public ReadDexMethodVisitor(@Nonnull final ReadDexClassVisitor parentVisitor) {
		this.parentVisitor = parentVisitor;
		this.readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(this);
		this.readDexCodeVisitor = new ReadDexCodeVisitor(this);
	}

	@Override
	public DU getDu() {
		return getParentVisitor().getDu();
	}

	@Override
	public T getT() {
		return getParentVisitor().getT();
	}

	/**
	 * Init and set method.
	 *
	 * @param m
	 *            method
	 */
	public void init(@Nonnull final M m) {
		this.m = m;
		this.as = null;
		this.paramAss = null;
	}

	@Override
	public DexAnnotationVisitor visitAnnotation(final String name, final boolean visible) {
		final T aT = getDu().getDescT(name);
		if (aT == null) {
			log.warn(getM() + ": Cannot read annotation descriptor '" + name + "'!");
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
	public DexCodeVisitor visitCode() {
		assert this.m != null;
		this.readDexCodeVisitor.init(this.m);
		return this.readDexCodeVisitor;
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.m.setAs(this.as);
		}
		if (this.paramAss != null) {
			this.m.setParamAss(this.paramAss);
		}
	}

	@Override
	public DexAnnotationAble visitParameterAnnotation(final int index) {
		return new DexAnnotationAble() {

			@Override
			public DexAnnotationVisitor visitAnnotation(final String name, final boolean visible) {
				final T aT = getDu().getDescT(name);
				if (aT == null) {
					log.warn("Cannot read annotation descriptor '" + name + "'!");
					return null;
				}
				final A a = new A(aT, visible);
				A[] paramAs = null;
				if (ReadDexMethodVisitor.this.paramAss == null) {
					ReadDexMethodVisitor.this.paramAss = new A[index + 1][];
				} else if (index >= ReadDexMethodVisitor.this.paramAss.length) {
					final A[][] newParamAss = new A[index + 1][];
					System.arraycopy(ReadDexMethodVisitor.this.paramAss, 0, newParamAss, 0,
							ReadDexMethodVisitor.this.paramAss.length);
					ReadDexMethodVisitor.this.paramAss = newParamAss;
				} else {
					paramAs = ReadDexMethodVisitor.this.paramAss[index];
				}
				if (paramAs == null) {
					paramAs = new A[1];
				} else {
					final A[] newParamAs = new A[paramAs.length + 1];
					System.arraycopy(newParamAs, 0, paramAs, 0, paramAs.length);
					paramAs = newParamAs;
				}
				ReadDexMethodVisitor.this.paramAss[index] = paramAs;
				paramAs[paramAs.length - 1] = a;
				return ReadDexMethodVisitor.this.readDexAnnotationMemberVisitor.init(a);
			}

		};
	}

}