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

import java.lang.annotation.RetentionPolicy;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
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

	private A[] as;

	private final DU du;

	private MD md;

	private A[][] paramAss;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	private final ReadDexCodeVisitor readDexCodeVisitor = new ReadDexCodeVisitor();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexMethodVisitor(final DU du) {
		assert du != null;

		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
	}

	/**
	 * Init and set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 */
	public void init(final MD md) {
		this.md = md;
		this.as = null;
		this.paramAss = null;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name,
			final boolean visitable) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readAnnotationMemberVisitor.init(
				name, visitable ? RetentionPolicy.RUNTIME
						: RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public DexCodeVisitor visitCode() {
		this.readDexCodeVisitor.init(this.md);
		return this.readDexCodeVisitor;
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.md.setAs(this.as);
		}
		if (this.paramAss != null) {
			this.md.setParamAs(this.paramAss);
		}
	}

	@Override
	public DexAnnotationAble visitParamesterAnnotation(final int index) {
		return new DexAnnotationAble() {

			@Override
			public AnnotationVisitor visitAnnotation(final String name,
					final boolean visitable) {
				A[] paramAs = null;
				if (ReadDexMethodVisitor.this.paramAss == null) {
					ReadDexMethodVisitor.this.paramAss = new A[index + 1][];
				} else if (index >= ReadDexMethodVisitor.this.paramAss.length) {
					final A[][] newParamAss = new A[index + 1][];
					System.arraycopy(ReadDexMethodVisitor.this.paramAss, 0,
							newParamAss, 0,
							ReadDexMethodVisitor.this.paramAss.length);
					ReadDexMethodVisitor.this.paramAss = newParamAss;
				}
				paramAs = ReadDexMethodVisitor.this.paramAss[index];
				if (paramAs == null) {
					paramAs = new A[1];
				} else {
					final A[] newParamAs = new A[paramAs.length + 1];
					System.arraycopy(newParamAs, 0, paramAs, 0, paramAs.length);
					paramAs = newParamAs;
				}
				ReadDexMethodVisitor.this.paramAss[index] = paramAs;
				paramAs[paramAs.length - 1] = ReadDexMethodVisitor.this.readAnnotationMemberVisitor
						.init(name, visitable ? RetentionPolicy.RUNTIME
								: RetentionPolicy.CLASS);
				return ReadDexMethodVisitor.this.readAnnotationMemberVisitor;
			}

		};
	}

}