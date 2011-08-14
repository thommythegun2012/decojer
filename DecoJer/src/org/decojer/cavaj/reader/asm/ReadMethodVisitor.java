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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.Var;
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

	private final ArrayList<A> as = new ArrayList<A>();

	private final DU du;

	private MD md;

	private A[][] paramAss;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	final Map<Integer, List<Var>> reg2vars = new HashMap<Integer, List<Var>>();

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
		this.as.clear();
		this.reg2vars.clear();
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		this.as.add(this.readAnnotationMemberVisitor.init(desc,
				visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS));
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
		// TODO identical to JavassistReader.readLocalVariables
		if (this.reg2vars.size() > 0) {
			final M m = this.md.getM();
			final T[] paramTs = m.getParamTs();
			final String[] paramNames = new String[paramTs.length];
			int reg = 0;
			if (!m.checkAf(AF.STATIC)) {
				this.reg2vars.remove(reg++);
				// check this?
			}
			for (int i = 0; i < paramTs.length; ++i) {
				final List<Var> vars = this.reg2vars.remove(reg++);
				if (vars == null) {
					// could happen, e.g. synthetic methods, inner <init>
					// with outer type param
					continue;
				}
				if (vars.size() != 1) {
					LOGGER.warning("Variable size for method parameter register '"
							+ reg + "' not equal 1!");
					continue;
				}
				final Var var = vars.get(0);
				if (var.getStartPc() != 0) {
					LOGGER.warning("Variable start for method parameter register '"
							+ reg + "' not 0!");
					continue;
				}
				if (var.getTs().size() != 1) {
					LOGGER.warning("Variable type for method parameter register '"
							+ reg + "' not unique!");
					continue;
				}
				final T paramT = var.getTs().iterator().next();
				if (paramT != paramTs[i]) {
					LOGGER.warning("Variable type for method parameter register '"
							+ reg + "' not equal!");
					continue;
				}
				if (paramT == T.LONG || paramT == T.DOUBLE) {
					++reg;
				}
				paramNames[i] = var.getName();
			}
			m.setParamNames(paramNames);
		}
		if (this.paramAss != null) {
			this.md.setParamAs(this.paramAss);
		}
		if (this.reg2vars.size() > 0) {
			this.md.setReg2vars(this.reg2vars);
		}
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
		List<Var> vars = this.reg2vars.get(index);
		if (vars == null) {
			vars = new ArrayList<Var>();
			this.reg2vars.put(index, vars);
		}
		final Var var = new Var(this.du.getDescT(desc));
		var.setName(name);
		final int startPc = start.getOffset();
		final int endPc = end.getOffset();
		var.setStartPc(startPc);
		var.setEndPc(endPc);
		vars.add(var);
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
			paramAs = this.paramAss[parameter] = new A[1];
		} else if (parameter >= this.paramAss.length) {
			final A[][] newParamAss = new A[parameter + 1][];
			System.arraycopy(this.paramAss, 0, newParamAss, 0,
					this.paramAss.length);
			this.paramAss = newParamAss;
			paramAs = this.paramAss[parameter] = new A[1];
		} else {
			paramAs = new A[this.paramAss[parameter].length + 1];
			System.arraycopy(this.paramAss[parameter], 0, paramAs, 0,
					this.paramAss[parameter].length);
		}
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
		if (TODOCODE) {
			LOGGER.warning("### method visitTryCatchBlock ### " + start + " : "
					+ end + " : " + handler + " : " + type);
		}
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