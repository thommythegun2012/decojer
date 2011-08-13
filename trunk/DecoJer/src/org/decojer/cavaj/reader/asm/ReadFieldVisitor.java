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

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.T;
import org.ow2.asm.AnnotationVisitor;
import org.ow2.asm.Attribute;
import org.ow2.asm.FieldVisitor;

/**
 * Read field visitor.
 * 
 * @author André Pankraz
 */
public class ReadFieldVisitor implements FieldVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadFieldVisitor.class.getName());

	private final ArrayList<A> as = new ArrayList<A>();

	private final DU du;

	private FD fd;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadFieldVisitor(final DU du) {
		assert du != null;

		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
	}

	/**
	 * Get field declaration.
	 * 
	 * @return field declaration
	 */
	public FD getFd() {
		return this.fd;
	}

	/**
	 * Init and set field declaration.
	 * 
	 * @param fd
	 *            field declaration
	 */
	public void init(final FD fd) {
		this.fd = fd;
		this.as.clear();
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		final T t = this.du.getDescT(desc);
		final A a = new A(t, visible ? RetentionPolicy.RUNTIME
				: RetentionPolicy.CLASS);
		this.as.add(a);

		this.readAnnotationMemberVisitor.init(a);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		LOGGER.warning("Unknown field attribute tag '" + attr.type
				+ "' for field info '" + this.fd.getTd() + "'!");
	}

	@Override
	public void visitEnd() {
		// nothing
	}

}