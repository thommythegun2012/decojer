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

import org.decojer.cavaj.model.MD;
import org.ow2.asm.AnnotationVisitor;
import org.ow2.asm.Attribute;
import org.ow2.asm.Handle;
import org.ow2.asm.Label;
import org.ow2.asm.MethodVisitor;

/**
 * Read method visitor.
 * 
 * @author André Pankraz
 */
public class ReadMethodVisitor implements MethodVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadMethodVisitor.class.getName());

	private MD md;

	private final ReadAnnotationVisitor readAnnotationVisitor = new ReadAnnotationVisitor();

	private final ReadDefaultAnnotationVisitor readDefaultAnnotationVisitor = new ReadDefaultAnnotationVisitor();

	/**
	 * Get method declaration.
	 * 
	 * @return method declaration
	 */
	public MD getMd() {
		return this.md;
	}

	/**
	 * Init and set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 */
	public void init(final MD md) {
		this.md = md;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		LOGGER.warning("### method visitAnnotation ### " + desc + " : "
				+ visible);
		this.readAnnotationVisitor.init(this.md);
		return this.readAnnotationVisitor;
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		this.readDefaultAnnotationVisitor.init(this.md);
		return this.readDefaultAnnotationVisitor;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		LOGGER.warning("Unknown method attribute tag '" + attr.type
				+ "' for field info '" + this.md.getTd() + "'!");
	}

	@Override
	public void visitCode() {
		// nothing
	}

	@Override
	public void visitEnd() {
		// nothing
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		LOGGER.warning("### method visitFieldInsn ### " + opcode + " : "
				+ owner + " : " + name + " : " + desc);
		// ### 178 : java/lang/System : out : Ljava/io/PrintStream;
	}

	@Override
	public void visitFrame(final int type, final int nLocal,
			final Object[] local, final int nStack, final Object[] stack) {
		LOGGER.warning("### method visitFrame ### " + type + " : " + nLocal
				+ " : " + local + " : " + nStack + " : " + stack);
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		LOGGER.warning("### method visitIincInsn ### " + var + " : "
				+ increment);
	}

	@Override
	public void visitInsn(final int opcode) {
		LOGGER.warning("### method visitInsn ### " + opcode);
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		LOGGER.warning("### method visitIntInsn ### " + opcode + " : "
				+ operand);
	}

	@Override
	public void visitInvokeDynamicInsn(final String name, final String desc,
			final Handle bsm, final Object... bsmArgs) {
		LOGGER.warning("### method visitInvokeDynamicInsn ### " + name + " : "
				+ desc + " : " + bsm + " : " + bsmArgs);
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		LOGGER.warning("### method visitJumpInsn ### " + opcode + " : " + label);
	}

	@Override
	public void visitLabel(final Label label) {
		LOGGER.warning("### method visitLabel ### " + label.getOffset());
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		LOGGER.warning("### method visitLdcInsn ### " + cst);
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		LOGGER.warning("### method visitLineNumber ### " + line + " : " + start);
	}

	@Override
	public void visitLocalVariable(final String name, final String desc,
			final String signature, final Label start, final Label end,
			final int index) {
		LOGGER.warning("### method visitLocalVariable ### " + name + " : "
				+ desc + " : " + signature + " : " + start + " : " + end
				+ " : " + index);
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		LOGGER.warning("### method visitLookupSwitchInsn ### " + dflt + " : "
				+ keys + " : " + labels);
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		LOGGER.warning("### method visitMaxs ### " + maxStack + " : "
				+ maxLocals);
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc) {
		LOGGER.warning("### method visitMethodInsn ### " + opcode + " : "
				+ owner + " : " + name + " : " + desc);
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		LOGGER.warning("### method visitMultiANewArrayInsn ### " + desc + " : "
				+ dims);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(final int parameter,
			final String desc, final boolean visible) {
		LOGGER.warning("### method visitParameterAnnotation ### " + parameter
				+ " : " + desc + " : " + visible);
		return new ReadAnnotationVisitor();
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max,
			final Label dflt, final Label... labels) {
		LOGGER.warning("### method visitTableSwitchInsn ### " + min + " : "
				+ max + " : " + dflt + " : " + labels);
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		LOGGER.warning("### method visitTryCatchBlock ### " + start + " : "
				+ end + " : " + handler + " : " + type);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		LOGGER.warning("### method visitTypeInsn ### " + opcode + " : " + type);
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		LOGGER.warning("### method visitVarInsn ### " + opcode + " : " + var);
	}

}