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
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
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

	private A[] as;

	private final DU du;

	private MD md;

	private A[][] paramAss;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	private static final boolean TODOCODE = true;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadMethodVisitor(final DU du) {
		assert du != null;

		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
	}

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
		LOGGER.warning("###### init md ###### " + md);
		this.md = md;
		this.as = null;
		this.paramAss = null;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readAnnotationMemberVisitor
				.init(desc, visible ? RetentionPolicy.RUNTIME
						: RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return new ReadAnnotationVisitor(this.du) {

			@Override
			protected void add(final String name, final Object value) {
				ReadMethodVisitor.this.md.setAnnotationDefaultValue(value);
			}

		};
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		LOGGER.warning("Unknown method attribute tag '" + attr.type
				+ "' for field info '" + this.md.getTd() + "'!");
	}

	@Override
	public void visitCode() {
		LOGGER.warning("### method visitCode ### ");
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.md.setAs(this.as);
		}
		if (this.paramAss != null) {
			this.md.setParamAss(this.paramAss);
		}
		this.md.postProcessVars();
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		if (TODOCODE) {
			LOGGER.warning("### method visitFieldInsn ### " + opcode + " : "
					+ owner + " : " + name + " : " + desc);
			// ### 178 : java/lang/System : out : Ljava/io/PrintStream;
		}
	}

	@Override
	public void visitFrame(final int type, final int nLocal,
			final Object[] local, final int nStack, final Object[] stack) {
		if (TODOCODE) {
			LOGGER.warning("### method visitFrame ### " + type + " : " + nLocal
					+ " : " + local + " : " + nStack + " : " + stack);
		}
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		if (TODOCODE) {
			LOGGER.warning("### method visitIincInsn ### " + var + " : "
					+ increment);
		}
	}

	@Override
	public void visitInsn(final int opcode) {
		if (TODOCODE) {
			LOGGER.warning("### method visitInsn ### " + opcode);
		}
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		if (TODOCODE) {
			LOGGER.warning("### method visitIntInsn ### " + opcode + " : "
					+ operand);
		}
	}

	@Override
	public void visitInvokeDynamicInsn(final String name, final String desc,
			final Handle bsm, final Object... bsmArgs) {
		if (TODOCODE) {
			LOGGER.warning("### method visitInvokeDynamicInsn ### " + name
					+ " : " + desc + " : " + bsm + " : " + bsmArgs);
		}
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		if (TODOCODE) {
			LOGGER.warning("### method visitJumpInsn ### " + opcode + " : "
					+ label);
		}
	}

	@Override
	public void visitLabel(final Label label) {
		if (TODOCODE) {
			LOGGER.warning("### method visitLabel ### " + label.getOffset());
		}
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		if (TODOCODE) {
			LOGGER.warning("### method visitLdcInsn ### " + cst);
		}
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		if (TODOCODE) {
			LOGGER.warning("### method visitLineNumber ### " + line + " : "
					+ start);
		}
	}

	@Override
	public void visitLocalVariable(final String name, final String desc,
			final String signature, final Label start, final Label end,
			final int index) {
		this.md.addVar(index, desc, signature, name, start.getOffset(),
				end.getOffset());
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		if (TODOCODE) {
			LOGGER.warning("### method visitLookupSwitchInsn ### " + dflt
					+ " : " + keys + " : " + labels);
		}
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		// is called before visitEnd
		// init CFG with start BB
		final CFG cfg = new CFG(this.md, maxLocals, maxStack);
		this.md.setCFG(cfg);

		cfg.calculatePostorder();
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc) {
		if (TODOCODE) {
			LOGGER.warning("### method visitMethodInsn ### " + opcode + " : "
					+ owner + " : " + name + " : " + desc);
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		if (TODOCODE) {
			LOGGER.warning("### method visitMultiANewArrayInsn ### " + desc
					+ " : " + dims);
		}
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(final int parameter,
			final String desc, final boolean visible) {
		A[] paramAs = null;
		if (this.paramAss == null) {
			this.paramAss = new A[parameter + 1][];
		} else if (parameter >= this.paramAss.length) {
			final A[][] newParamAss = new A[parameter + 1][];
			System.arraycopy(this.paramAss, 0, newParamAss, 0,
					this.paramAss.length);
			this.paramAss = newParamAss;
		} else {
			paramAs = this.paramAss[parameter];
		}
		if (paramAs == null) {
			paramAs = new A[1];
		} else {
			final A[] newParamAs = new A[paramAs.length + 1];
			System.arraycopy(newParamAs, 0, paramAs, 0, paramAs.length);
			paramAs = newParamAs;
		}
		this.paramAss[parameter] = paramAs;
		paramAs[paramAs.length - 1] = this.readAnnotationMemberVisitor
				.init(desc, visible ? RetentionPolicy.RUNTIME
						: RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max,
			final Label dflt, final Label... labels) {
		if (TODOCODE) {
			LOGGER.warning("### method visitTableSwitchInsn ### " + min + " : "
					+ max + " : " + dflt + " : " + labels);
		}
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		// type: java/lang/Exception
		final T catchT = type == null ? null : this.du.getT(type.replace('/',
				'.'));
		this.md.addExc(catchT, start.getOffset(), end.getOffset(),
				handler.getOffset());
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		if (TODOCODE) {
			LOGGER.warning("### method visitTypeInsn ### " + opcode + " : "
					+ type);
		}
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		if (TODOCODE) {
			LOGGER.warning("### method visitVarInsn ### " + opcode + " : "
					+ var);
		}
	}

}