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
package org.decojer.cavaj.readers.asm;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.types.T;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.common.collect.Lists;

/**
 * ASM read annotation visitor.
 *
 * @author André Pankraz
 */
public abstract class ReadAnnotationVisitor extends AnnotationVisitor {

	@Nonnull
	protected final DU du;

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public ReadAnnotationVisitor(@Nonnull final DU du) {
		super(Opcodes.ASM5);
		this.du = du;
	}

	protected abstract void add(final String name, @Nullable final Object value);

	@Override
	public void visit(final String name, final Object value) {
		if (value instanceof Type) {
			add(name, this.du.getT(((Type) value).getClassName()));
			return;
		}
		add(name, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name, final String desc) {
		final ReadAnnotationMemberVisitor readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(
				this.du);
		add(name, readAnnotationMemberVisitor.init(desc, null));
		return readAnnotationMemberVisitor;
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		return new ReadAnnotationVisitor(this.du) {

			private final List<Object> values = Lists.newArrayList();

			@Override
			protected void add(final String name, final Object value) {
				this.values.add(value);
			}

			@Override
			public void visitEnd() {
				ReadAnnotationVisitor.this.add(name,
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
		final T ownerT = this.du.getDescT(desc);
		final F f = ownerT.getF(value, desc);
		f.setEnum();
		add(name, f);
	}

}