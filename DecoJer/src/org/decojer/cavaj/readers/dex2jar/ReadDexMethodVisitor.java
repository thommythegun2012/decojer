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
import org.decojer.cavaj.model.methods.M;

import com.googlecode.dex2jar.visitors.DexAnnotationAble;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;
import com.googlecode.dex2jar.visitors.DexCodeVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;

/**
 * Dex2jar read method visitor.
 * 
 * @author André Pankraz
 */
public class ReadDexMethodVisitor implements DexMethodVisitor {

	@Nullable
	private A[] as;

	private M m;

	@Nullable
	private A[][] paramAss;

	private final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor;

	private final ReadDexCodeVisitor readDexCodeVisitor;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexMethodVisitor(final DU du) {
		assert du != null;

		this.readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(du);
		this.readDexCodeVisitor = new ReadDexCodeVisitor(du);
	}

	/**
	 * Init and set method.
	 * 
	 * @param m
	 *            method
	 */
	public void init(final M m) {
		this.m = m;
		this.as = null;
		this.paramAss = null;
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
	public DexCodeVisitor visitCode() {
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
				paramAs[paramAs.length - 1] = ReadDexMethodVisitor.this.readDexAnnotationMemberVisitor
						.init(name, visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS);
				return ReadDexMethodVisitor.this.readDexAnnotationMemberVisitor;
			}

		};
	}

}