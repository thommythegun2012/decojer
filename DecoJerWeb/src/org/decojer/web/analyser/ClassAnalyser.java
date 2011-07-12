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
package org.decojer.web.analyser;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassAnalyser {

	public static TypeInfo analyse(final byte[] bytes) {
		final TypeInfo typeInfo = new TypeInfo();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(new ClassVisitor() {

			@Override
			public void visit(final int version, final int access,
					final String name, final String signature,
					final String superName, final String[] interfaces) {
				typeInfo.name = name;
				if (signature == null) {
					final StringBuilder sb = new StringBuilder("L");
					sb.append(superName);
					sb.append(";");
					for (int i = 0; i < interfaces.length; ++i) {
						sb.append("L").append(interfaces[i]).append(";");
					}
					typeInfo.signature = sb.toString();
				} else {
					typeInfo.signature = signature;
				}
				typeInfo.superName = superName;
			}

			@Override
			public AnnotationVisitor visitAnnotation(final String desc,
					final boolean visible) {
				return null;
			}

			@Override
			public void visitAttribute(final Attribute attr) {
			}

			@Override
			public void visitEnd() {
			}

			@Override
			public FieldVisitor visitField(final int access, final String name,
					final String desc, final String signature,
					final Object value) {
				return null;
			}

			@Override
			public void visitInnerClass(final String name,
					final String outerName, final String innerName,
					final int access) {
			}

			@Override
			public MethodVisitor visitMethod(final int access,
					final String name, final String desc,
					final String signature, final String[] exceptions) {
				return null;
			}

			@Override
			public void visitOuterClass(final String owner, final String name,
					final String desc) {
			}

			@Override
			public void visitSource(final String source, final String debug) {
			}
		}, ClassReader.SKIP_FRAMES);
		return typeInfo;
	}
}