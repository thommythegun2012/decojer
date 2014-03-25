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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ReadVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.common.collect.Lists;

/**
 * ASM read annotation visitor.
 *
 * @author André Pankraz
 */
@Slf4j
public abstract class ReadAnnotationVisitor extends AnnotationVisitor implements ReadVisitor {

	@Getter
	@Nonnull
	private final ReadVisitor parentVisitor;

	/**
	 * Constructor.
	 *
	 * @param parentVisitor
	 *            parent visitor
	 */
	public ReadAnnotationVisitor(@Nonnull final ReadVisitor parentVisitor) {
		super(Opcodes.ASM5);
		this.parentVisitor = parentVisitor;
	}

	/**
	 * Ann annotation value.
	 *
	 * @param name
	 *            value name, {@code null} for default value
	 * @param value
	 *            value
	 */
	protected abstract void add(@Nullable final String name, @Nullable final Object value);

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
		if (value instanceof Type) {
			final String className = ((Type) value).getClassName();
			if (className != null) {
				add(name, getDu().getT(className));
				return;
			}
		}
		add(name, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name, final String desc) {
		final ReadAnnotationMemberVisitor readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(
				this);
		add(name, readAnnotationMemberVisitor.init(desc, null));
		return readAnnotationMemberVisitor;
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		return new ReadAnnotationVisitor(this) {

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
		final T ownerT = getDu().getDescT(desc);
		if (ownerT == null) {
			log.warn(getT() + ": Cannot read enumeration value descriptor '" + desc + "'!");
			return;
		}
		if (value == null || desc == null) {
			log.warn(getT() + ": Cannot read null enumeration value for '" + ownerT + "'!");
			return;
		}
		final F f = ownerT.getF(value, desc);
		f.setEnum();
		add(name, f);
	}

}