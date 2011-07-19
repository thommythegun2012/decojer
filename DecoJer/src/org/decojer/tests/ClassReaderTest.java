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
package org.decojer.tests;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javassist.bytecode.ClassFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodHandle;
import org.objectweb.asm.MethodVisitor;

/**
 * 
 * @author André Pankraz
 */
public class ClassReaderTest {

	public static void decompileClass(final InputStream is,
			final OutputStream os) throws IOException {
		final ClassReader classReader = new ClassReader(is);
		classReader.accept(new ClassVisitor() {

			@Override
			public void visit(final int version, final int access,
					final String name, final String signature,
					final String superName, final String[] interfaces) {
				// TODO Auto-generated method stub

			}

			@Override
			public AnnotationVisitor visitAnnotation(final String desc,
					final boolean visible) {
				// TODO Auto-generated method stub
				return new AnnotationVisitor() {

					@Override
					public void visit(final String name, final Object value) {
						// TODO Auto-generated method stub

					}

					@Override
					public AnnotationVisitor visitAnnotation(final String name,
							final String desc) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public AnnotationVisitor visitArray(final String name) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void visitEnd() {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitEnum(final String name, final String desc,
							final String value) {
						// TODO Auto-generated method stub

					}
				};
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
					final String desc, final String signature,
					final Object value) {
				// TODO Auto-generated method stub
				return new FieldVisitor() {

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
				};
			}

			@Override
			public void visitInnerClass(final String name,
					final String outerName, final String innerName,
					final int access) {
				// TODO Auto-generated method stub

			}

			@Override
			public MethodVisitor visitMethod(final int access,
					final String name, final String desc,
					final String signature, final String[] exceptions) {
				// System.out.println("  visitMethod: " + name);
				return new MethodVisitor() {

					@Override
					public AnnotationVisitor visitAnnotation(final String desc,
							final boolean visible) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public AnnotationVisitor visitAnnotationDefault() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void visitAttribute(final Attribute attr) {
						// TODO Auto-generated method stub
						System.out.println("Attr: " + attr);
					}

					@Override
					public void visitCode() {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitEnd() {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitFieldInsn(final int opcode,
							final String owner, final String name,
							final String desc) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitFrame(final int type, final int nLocal,
							final Object[] local, final int nStack,
							final Object[] stack) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitIincInsn(final int var, final int increment) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitInsn(final int opcode) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitIntInsn(final int opcode, final int operand) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitInvokeDynamicInsn(final String arg0,
							final String arg1, final MethodHandle arg2,
							final Object... arg3) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitJumpInsn(final int opcode,
							final Label label) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitLabel(final Label label) {
						// TODO Auto-generated method stub
						// System.out.println("Label: " + label);
					}

					@Override
					public void visitLdcInsn(final Object cst) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitLineNumber(final int line,
							final Label start) {
						// TODO Auto-generated method stub
						// System.out.println("Label: " + line + " : " + start);
					}

					@Override
					public void visitLocalVariable(final String name,
							final String desc, final String signature,
							final Label start, final Label end, final int index) {
						// TODO Auto-generated method stub
						// System.out.println("LocalVar: " + name + " : " + desc
						// + " : " + signature + " : " + start + " : "
						// + end + " : " + index);
					}

					@Override
					public void visitLookupSwitchInsn(final Label dflt,
							final int[] keys, final Label[] labels) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitMaxs(final int maxStack,
							final int maxLocals) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitMethodInsn(final int opcode,
							final String owner, final String name,
							final String desc) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitMultiANewArrayInsn(final String desc,
							final int dims) {
						// TODO Auto-generated method stub

					}

					@Override
					public AnnotationVisitor visitParameterAnnotation(
							final int parameter, final String desc,
							final boolean visible) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void visitTableSwitchInsn(final int min,
							final int max, final Label dflt,
							final Label[] labels) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitTryCatchBlock(final Label start,
							final Label end, final Label handler,
							final String type) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitTypeInsn(final int opcode,
							final String type) {
						// TODO Auto-generated method stub

					}

					@Override
					public void visitVarInsn(final int opcode, final int var) {
						// TODO Auto-generated method stub

					}
				};
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
		}, 0);
	}

	public static void main(final String[] args) throws FileNotFoundException,
			IOException {
		for (int i = 1000; i-- > 0;) {
			new ClassFile(
					new DataInputStream(
							new FileInputStream(
									new File(
											"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/DecTestBooleanOperators.class"))));
			decompileClass(
					new FileInputStream(
							new File(
									"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/DecTestBooleanOperators.class")),
					null);
		}
		final long millis = System.currentTimeMillis();
		for (int i = 1000; i-- > 0;) {
			if (false) {
				// 560 ms
				new ClassFile(
						new DataInputStream(
								new FileInputStream(
										new File(
												"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/DecTestBooleanOperators.class"))));
			} else {
				// 60 ms
				decompileClass(
						new FileInputStream(
								new File(
										"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/DecTestBooleanOperators.class")),
						null);
			}
		}
		System.out.println("TEST: " + (System.currentTimeMillis() - millis));
	}
}
