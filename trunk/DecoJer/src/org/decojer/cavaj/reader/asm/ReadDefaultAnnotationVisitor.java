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

import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

/**
 * Read default annotation visitor.
 * 
 * @author André Pankraz
 */
public class ReadDefaultAnnotationVisitor implements AnnotationVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadDefaultAnnotationVisitor.class.getName());

	private final ReadMethodVisitor readMethodVisitor;

	/**
	 * Constructor.
	 * 
	 * @param readMethodVisitor
	 *            read method visitor
	 */
	public ReadDefaultAnnotationVisitor(
			final ReadMethodVisitor readMethodVisitor) {
		this.readMethodVisitor = readMethodVisitor;
	}

	private void checkName(final String name) {
		if (name == null) {
			return;
		}
		LOGGER.warning("Default annotation should have null name but has '"
				+ name + "'!");
	}

	@Override
	public void visit(final String name, final Object value) {
		checkName(name);
		if (value instanceof Type) {
			; // TODO type stuff
			return;
		}

		LOGGER.warning("###### default visit ### " + value + " :C: "
				+ value.getClass());
		this.readMethodVisitor.getMd().setAnnotationDefaultValue(value);
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
		LOGGER.warning("###### default visitArray ### ");
		return new ReadAnnotationVisitor();
	}

	@Override
	public void visitEnd() {
		// nothing
	}

	@Override
	public void visitEnum(final String name, final String desc,
			final String value) {
		checkName(name);
		LOGGER.warning("###### default visitEnum ### " + desc + " : " + value);
	}

}