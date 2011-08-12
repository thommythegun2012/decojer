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
package org.decojer.cavaj.reader.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.ow2.asm.AnnotationVisitor;
import org.ow2.asm.Type;

/**
 * Read default annotation visitor.
 * 
 * @author André Pankraz
 */
public class ReadDefaultAnnotationVisitor implements AnnotationVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadDefaultAnnotationVisitor.class.getName());

	private MD md;

	private Object value;

	private void checkName(final String name) {
		if (name == null) {
			return;
		}
		LOGGER.warning("Default annotation should have null name but has '"
				+ name + "'!");
	}

	private DU getDu() {
		return this.md.getM().getT().getDu();
	}

	/**
	 * Init and set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 */
	public void init(final MD md) {
		this.md = md;
		this.value = null;
	}

	@Override
	public void visit(final String name, final Object value) {
		checkName(name);
		if (value instanceof Type) {
			this.value = getDu().getT(((Type) value).getClassName());
			return;
		}
		this.value = value;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name,
			final String desc) {
		checkName(name);
		LOGGER.warning("###### default visitAnnotation ### " + desc);
		return new ReadAnnotationVisitor();
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		checkName(name);
		return new AnnotationVisitor() {

			private final List<Object> values = new ArrayList<Object>();

			@Override
			public void visit(final String name, final Object value) {
				checkName(name);
				if (value instanceof Type) {
					final T t = getDu().getT(((Type) value).getClassName());
					this.values.add(t);
					return;
				}
				this.values.add(value);
			}

			@Override
			public AnnotationVisitor visitAnnotation(final String name,
					final String desc) {
				checkName(name);
				return null;
			}

			@Override
			public AnnotationVisitor visitArray(final String name) {
				LOGGER.warning("Onle 1-dimensional arrays allowed as default values!");
				return null;
			}

			@Override
			public void visitEnd() {
				ReadDefaultAnnotationVisitor.this.value = this.values.toArray();
			}

			@Override
			public void visitEnum(final String name, final String desc,
					final String value) {
				checkName(name);
			}
		};
	}

	@Override
	public void visitEnd() {
		if (this.value != null) {
			this.md.setAnnotationDefaultValue(this.value);
		}
	}

	@Override
	public void visitEnum(final String name, final String desc,
			final String value) {
		checkName(name);
		// desc: Ljava/lang/Thread$State;
		// value: BLOCKED
		final T t = getDu().getDescT(desc);
		final F f = t.getF(value, t);
		f.markAf(AF.ENUM);
		this.value = f;
	}

}