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

import static org.decojer.cavaj.readers.asm.ReadUtils.annotateT;

import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

/**
 * ASM read field visitor.
 * 
 * @author André Pankraz
 */
public class ReadFieldVisitor extends FieldVisitor {

	private final static Logger LOGGER = Logger.getLogger(ReadFieldVisitor.class.getName());

	private A[] as;

	private F f;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadFieldVisitor(final DU du) {
		super(Opcodes.ASM5);
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
	}

	/**
	 * Get field declaration.
	 * 
	 * @return field declaration
	 */
	public F getFd() {
		return this.f;
	}

	/**
	 * Init and set field.
	 * 
	 * @param f
	 *            field
	 */
	public void init(final F f) {
		this.f = f;
		this.as = null;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readAnnotationMemberVisitor.init(desc,
				visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		LOGGER.warning(getFd() + ": Unknown field attribute tag '" + attr.type
				+ "' for field info '" + this.f.getT() + "'!");
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.f.setAs(this.as);
		}
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath,
			final String desc, final boolean visible) {
		final A a = this.readAnnotationMemberVisitor.init(desc, visible ? RetentionPolicy.RUNTIME
				: RetentionPolicy.CLASS);
		final TypeReference typeReference = new TypeReference(typeRef);
		switch (typeReference.getSort()) {
		case TypeReference.FIELD:
			getFd().setValueT(annotateT(getFd().getValueT(), a, typePath));
			break;
		default:
			LOGGER.warning(getFd() + ": Unknown type annotation ref sort '0x"
					+ Integer.toHexString(typeReference.getSort()) + "' : " + typeRef + " : "
					+ typePath + " : " + desc + " : " + visible);
		}
		return this.readAnnotationMemberVisitor;
	}

}