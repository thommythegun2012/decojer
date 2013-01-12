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

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;

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

	private A[] as;

	private final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor;

	private final ReadDexFieldVisitor readDexFieldVisitor;

	private final ReadDexMethodVisitor readDexMethodVisitor;

	private TD td;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexClassVisitor(final DU du) {
		assert du != null;

		this.readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(du);
		this.readDexFieldVisitor = new ReadDexFieldVisitor(du);
		this.readDexMethodVisitor = new ReadDexMethodVisitor(du);
	}

	/**
	 * Init and set type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void init(final TD td) {
		this.td = td;
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
			this.td.setAs(this.as);
		}
		this.td.resolve();
	}

	@Override
	public DexFieldVisitor visitField(final int accessFlags, final Field field, final Object value) {
		final FD fd = this.td.createFd(field.getName(), field.getType());

		fd.setAccessFlags(accessFlags);
		// TODO signature in annotation

		fd.setValue(value);

		this.readDexFieldVisitor.init(fd);
		return this.readDexFieldVisitor;
	}

	@Override
	public DexMethodVisitor visitMethod(final int accessFlags, final Method method) {
		final MD md = this.td.createMd(method.getName(), method.getDesc());

		md.setAccessFlags(accessFlags);
		// TODO throws in annotation

		this.readDexMethodVisitor.init(md);
		return this.readDexMethodVisitor;
	}

	@Override
	public void visitSource(final String file) {
		this.td.setSourceFileName(file);
	}

}