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

import java.util.List;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ReadVisitor;

import com.google.common.collect.Lists;
import com.googlecode.dex2jar.DexType;
import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.visitors.DexAnnotationVisitor;

/**
 * Dex2jar read annotation visitor.
 *
 * @author André Pankraz
 */
@Slf4j
public abstract class ReadDexAnnotationVisitor implements DexAnnotationVisitor, ReadVisitor {

	@Getter
	@Nonnull
	private final ReadVisitor parentVisitor;

	/**
	 * Constructor.
	 *
	 * @param parentVisitor
	 *            parent visitor
	 */
	public ReadDexAnnotationVisitor(@Nonnull final ReadVisitor parentVisitor) {
		this.parentVisitor = parentVisitor;
	}

	protected abstract void add(final String name, final Object value);

	@Override
	public DU getDu() {
		return getParentVisitor().getDu();
	}

	@Override
	public T getT() {
		return getParentVisitor().getT();
	}

	@Override
	public void visit(final String name, final Object value) {
		if (value instanceof Field) {
			log.warn("Visit field value '" + name
					+ "' should be visitEnum! (bug in dex-reader-1.1)");
			visitEnum(name, ((Field) value).getType(), ((Field) value).getName());
			return;
		}
		if (value instanceof DexType) {
			add(name, getDu().getDescT(((DexType) value).toString()));
			return;
		}
		add(name, value);
	}

	@Override
	public DexAnnotationVisitor visitAnnotation(final String name, final String desc) {
		final ReadDexAnnotationMemberVisitor readDexAnnotationMemberVisitor = new ReadDexAnnotationMemberVisitor(
				this);
		final T aT = getDu().getDescT(desc);
		if (aT == null) {
			log.warn("Cannot read annotation descriptor '" + desc + "'!");
			return null;
		}
		final A a = new A(aT, null);
		add(name, a);
		return readDexAnnotationMemberVisitor.init(a);
	}

	@Override
	public DexAnnotationVisitor visitArray(final String name) {
		return new ReadDexAnnotationVisitor(this) {

			private final List<Object> values = Lists.newArrayList();

			@Override
			protected void add(final String name, final Object value) {
				this.values.add(value);
			}

			@Override
			public void visitEnd() {
				ReadDexAnnotationVisitor.this.add(name,
						this.values.toArray(new Object[this.values.size()]));
			}

		};
	}

	@Override
	public void visitEnd() {
		// nothing
	}

	@Override
	public void visitEnum(final String name, final String desc, final String value) {
		final T ownerT = getDu().getDescT(desc);
		if (ownerT == null || value == null || desc == null) {
			return;
		}
		final F f = ownerT.getF(value, desc);
		f.setEnum();
		add(name, f);
	}

}