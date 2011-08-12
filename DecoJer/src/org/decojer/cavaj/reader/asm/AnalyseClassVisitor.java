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

import org.decojer.cavaj.model.type.Type;
import org.ow2.asm.AnnotationVisitor;
import org.ow2.asm.Attribute;
import org.ow2.asm.ClassVisitor;
import org.ow2.asm.FieldVisitor;
import org.ow2.asm.MethodVisitor;

/**
 * Analyse class visitor.
 * 
 * @author André Pankraz
 */
public class AnalyseClassVisitor implements ClassVisitor {

	private Type type;

	/**
	 * Get type.
	 * 
	 * @return type
	 */
	public Type getType() {
		return this.type;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		// visit:
		// com/thoughtworks/xstream/mapper/AnnotationMapper$UnprocessedTypesSet
		// : Ljava/util/LinkedHashSet<Ljava/lang/Class<*>;>; :
		// java/util/LinkedHashSet : [Ljava.lang.String;@1b9a2fd
		this.type = new Type(name, signature);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitEnd() {
		// TODO Auto-generated method stub

	}

	@Override
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void visitInnerClass(final String name, final String outerName,
			final String innerName, final int access) {
		// TODO Auto-generated method stub

	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void visitOuterClass(final String owner, final String name,
			final String desc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitSource(final String source, final String debug) {
		// TODO Auto-generated method stub

	}

}