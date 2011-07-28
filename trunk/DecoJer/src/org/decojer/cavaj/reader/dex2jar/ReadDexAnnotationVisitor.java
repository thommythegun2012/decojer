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

import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;

/**
 * Read DEX annotation visitor.
 * 
 * @author André Pankraz
 */
public class ReadDexAnnotationVisitor implements AnnotationVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadDexAnnotationVisitor.class.getName());

	@Override
	public void visit(final String name, final Object value) {
		LOGGER.warning("### annotation visit ### " + name + " : " + value
				+ " :C: " + value.getClass());
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name,
			final String desc) {
		LOGGER.warning("### annotation visitAnnotation ### " + name + " : "
				+ desc);
		return new ReadDexAnnotationVisitor();
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		LOGGER.warning("### annotation visitArray ### " + name);
		return new ReadDexAnnotationVisitor();
	}

	@Override
	public void visitEnd() {
		// LOGGER.warning("### annotation visitEnd ### ");
	}

	@Override
	public void visitEnum(final String name, final String desc,
			final String value) {
		LOGGER.warning("### annotation visitEnum ### " + name + " : " + desc
				+ " : " + value);
	}

}