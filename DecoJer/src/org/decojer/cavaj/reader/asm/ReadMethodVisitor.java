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
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.Exc;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.ALOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.AND;
import org.decojer.cavaj.model.vm.intermediate.operations.ARRAYLENGTH;
import org.decojer.cavaj.model.vm.intermediate.operations.ASTORE;
import org.decojer.cavaj.model.vm.intermediate.operations.CHECKCAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
import org.decojer.cavaj.model.vm.intermediate.operations.CONVERT;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.DUP;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;
import org.decojer.cavaj.model.vm.intermediate.operations.INC;
import org.decojer.cavaj.model.vm.intermediate.operations.INSTANCEOF;
import org.decojer.cavaj.model.vm.intermediate.operations.INVOKE;
import org.decojer.cavaj.model.vm.intermediate.operations.JCMP;
import org.decojer.cavaj.model.vm.intermediate.operations.JCND;
import org.decojer.cavaj.model.vm.intermediate.operations.JSR;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.MONITOR;
import org.decojer.cavaj.model.vm.intermediate.operations.MUL;
import org.decojer.cavaj.model.vm.intermediate.operations.NEG;
import org.decojer.cavaj.model.vm.intermediate.operations.NEW;
import org.decojer.cavaj.model.vm.intermediate.operations.NEWARRAY;
import org.decojer.cavaj.model.vm.intermediate.operations.OR;
import org.decojer.cavaj.model.vm.intermediate.operations.POP;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.PUT;
import org.decojer.cavaj.model.vm.intermediate.operations.REM;
import org.decojer.cavaj.model.vm.intermediate.operations.RET;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.SHL;
import org.decojer.cavaj.model.vm.intermediate.operations.SHR;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWAP;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;
import org.ow2.asm.AnnotationVisitor;
import org.ow2.asm.Attribute;
import org.ow2.asm.Handle;
import org.ow2.asm.Label;
import org.ow2.asm.MethodVisitor;
import org.ow2.asm.Opcodes;
import org.ow2.asm.Type;

/**
 * Read method visitor.
 * 
 * @author André Pankraz
 */
public class ReadMethodVisitor implements MethodVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadMethodVisitor.class.getName());

	private static final boolean TODOCODE = true;

	private static T readType(final String classInfo, final DU du) {
		if (classInfo == null) {
			return null;
		}
		// strange behaviour for classinfo:
		// arrays: normal descriptor:
		// [[I, [Ljava/lang/String;
		if (classInfo.charAt(0) == '[') {
			return du.getDescT(classInfo);
		}
		// no arrays - class name (but with '/'):
		// java/lang/StringBuilder
		return du.getT(classInfo.replace('/', '.'));
	}

	private A[] as;

	private final DU du;

	private final ArrayList<Exc> excs = new ArrayList<Exc>();

	// operation index or temporary unknown index
	private final HashMap<Label, Integer> label2index = new HashMap<Label, Integer>();

	private final HashMap<Label, ArrayList<Object>> label2unresolved = new HashMap<Label, ArrayList<Object>>();

	private int line = -1;

	private int maxLocals;

	private int maxStack;

	private MD md;

	private final ArrayList<Operation> operations = new ArrayList<Operation>();

	private A[][] paramAss;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	private final HashMap<Integer, ArrayList<Var>> reg2vars = new HashMap<Integer, ArrayList<Var>>();

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

	private int getLabelIndex(final Label label) {
		assert label != null;

		final Integer index = this.label2index.get(label);
		if (index != null) {
			return index;
		}
		final int unresolvedIndex = -1 - this.label2unresolved.size();
		this.label2index.put(label, unresolvedIndex);
		return unresolvedIndex;
	}

	private ArrayList<Object> getLabelUnresolved(final Label label) {
		assert label != null;

		ArrayList<Object> unresolved = this.label2unresolved.get(label);
		if (unresolved == null) {
			unresolved = new ArrayList<Object>();
			this.label2unresolved.put(label, unresolved);
		}
		return unresolved;
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
		this.md = md;
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
		// OK
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.md.setAs(this.as);
			this.as = null;
		}
		if (this.paramAss != null) {
			this.md.setParamAss(this.paramAss);
			this.paramAss = null;
		}
		if (this.operations.size() > 0) {
			final CFG cfg = new CFG(this.md, this.maxLocals, this.maxStack);
			this.md.setCFG(cfg);

			cfg.setOperations(this.operations
					.toArray(new Operation[this.operations.size()]));
			this.operations.clear();
			this.label2index.clear();
			this.label2unresolved.clear();
			this.line = -1;

			if (this.excs.size() > 0) {
				cfg.setExcs(this.excs.toArray(new Exc[this.excs.size()]));
				this.excs.clear();
			}
			if (this.reg2vars.size() > 0) {
				for (final Entry<Integer, ArrayList<Var>> entry : this.reg2vars
						.entrySet()) {
					final int reg = entry.getKey();
					for (final Var var : entry.getValue()) {
						this.md.addVar(reg, var);
					}
				}
				this.reg2vars.clear();
			}
			this.md.postProcessVars();
		}
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		// ### 178 : java/lang/System : out : Ljava/io/PrintStream;
		switch (opcode) {
		/*******
		 * GET *
		 *******/
		case Opcodes.GETFIELD:
		case Opcodes.GETSTATIC: {
			final T ownerT = this.du.getT(owner.replace('/', '.'));
			final T t = this.du.getDescT(desc);
			final F f = ownerT.getF(name, t);
			if (opcode == Opcodes.GETSTATIC) {
				f.markAf(AF.STATIC);
			}
			this.operations.add(new GET(this.operations.size(), opcode,
					this.line, f));
			return;
		}
		/*******
		 * PUT *
		 *******/
		case Opcodes.PUTFIELD:
		case Opcodes.PUTSTATIC: {
			final T ownerT = this.du.getT(owner.replace('/', '.'));
			final T t = this.du.getDescT(desc);
			final F f = ownerT.getF(name, t);
			if (opcode == Opcodes.PUTSTATIC) {
				f.markAf(AF.STATIC);
			}
			this.operations.add(new PUT(this.operations.size(), opcode,
					this.line, f));
			return;
		}
		default:
			LOGGER.warning("Unknown field insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitFrame(final int type, final int nLocal,
			final Object[] local, final int nStack, final Object[] stack) {
		// LOGGER.info("### method visitFrame ### " + type + " : " + nLocal
		// + " : " + local + " : " + nStack + " : " + stack);
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		/*******
		 * INC *
		 *******/
		this.operations.add(new INC(this.operations.size(), Opcodes.IINC,
				this.line, T.INT, var, increment));
	}

	@Override
	public void visitInsn(final int opcode) {
		T t = null;
		int type = -1;
		int iValue = Integer.MIN_VALUE;
		Object oValue = null;

		switch (opcode) {
		case Opcodes.NOP:
			// nothing to do, ignore
			break;
		/*******
		 * ADD *
		 *******/
		case Opcodes.DADD:
			t = T.DOUBLE;
			// fall through
		case Opcodes.FADD:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IADD:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LADD:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new ADD(this.operations.size(), opcode,
					this.line, t));
			break;
		/*********
		 * ALOAD *
		 *********/
		case Opcodes.AALOAD:
			t = T.AREF;
			// fall through
		case Opcodes.BALOAD:
			if (t == null) {
				t = T.BOOLEAN;
			}
			// fall through
		case Opcodes.CALOAD:
			if (t == null) {
				t = T.CHAR;
			}
			// fall through
		case Opcodes.DALOAD:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FALOAD:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IALOAD:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LALOAD:
			if (t == null) {
				t = T.LONG;
			}
			// fall through
		case Opcodes.SALOAD:
			if (t == null) {
				t = T.SHORT;
			}
			this.operations.add(new ALOAD(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * AND *
		 *******/
		case Opcodes.IAND:
			t = T.INT;
			// fall through
		case Opcodes.LAND:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new AND(this.operations.size(), opcode,
					this.line, t));
			break;
		/***************
		 * ARRAYLENGTH *
		 ***************/
		case Opcodes.ARRAYLENGTH:
			this.operations.add(new ARRAYLENGTH(this.operations.size(), opcode,
					this.line));
			break;
		/**********
		 * ASTORE *
		 **********/
		case Opcodes.AASTORE:
			t = T.AREF;
			// fall through
		case Opcodes.BASTORE:
			if (t == null) {
				t = T.BOOLEAN;
			}
			// fall through
		case Opcodes.CASTORE:
			if (t == null) {
				t = T.CHAR;
			}
			// fall through
		case Opcodes.DASTORE:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FASTORE:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IASTORE:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LASTORE:
			if (t == null) {
				t = T.LONG;
			}
			// fall through
		case Opcodes.SASTORE:
			if (t == null) {
				t = T.SHORT;
			}
			this.operations.add(new ASTORE(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * CMP *
		 *******/
		case Opcodes.DCMPG:
			t = T.DOUBLE;
			iValue = CMP.T_G;
			// fall through
		case Opcodes.DCMPL:
			if (t == null) {
				t = T.DOUBLE;
				iValue = CMP.T_L;
			}
			// fall through
		case Opcodes.FCMPG:
			if (t == null) {
				t = T.FLOAT;
				iValue = CMP.T_G;
			}
			// fall through
		case Opcodes.FCMPL:
			if (t == null) {
				t = T.FLOAT;
				iValue = CMP.T_L;
			}
			// fall through
		case Opcodes.LCMP:
			if (t == null) {
				t = T.LONG;
				iValue = CMP.T_0;
			}
			this.operations.add(new CMP(this.operations.size(), opcode,
					this.line, t, iValue));
			break;
		/***********
		 * CONVERT *
		 ***********/
		case Opcodes.D2F:
			t = T.DOUBLE;
			oValue = T.FLOAT;
			// fall through
		case Opcodes.D2I:
			if (t == null) {
				t = T.DOUBLE;
				oValue = T.INT;
			}
			// fall through
		case Opcodes.D2L:
			if (t == null) {
				t = T.DOUBLE;
				oValue = T.LONG;
			}
			// fall through
		case Opcodes.F2D:
			if (t == null) {
				t = T.FLOAT;
				oValue = T.DOUBLE;
			}
			// fall through
		case Opcodes.F2I:
			if (t == null) {
				t = T.FLOAT;
				oValue = T.INT;
			}
			// fall through
		case Opcodes.F2L:
			if (t == null) {
				t = T.FLOAT;
				oValue = T.LONG;
			}
			// fall through
		case Opcodes.I2B:
			if (t == null) {
				t = T.INT;
				oValue = T.BYTE;
			}
			// fall through
		case Opcodes.I2C:
			if (t == null) {
				t = T.INT;
				oValue = T.CHAR;
			}
			// fall through
		case Opcodes.I2D:
			if (t == null) {
				t = T.INT;
				oValue = T.DOUBLE;
			}
			// fall through
		case Opcodes.I2F:
			if (t == null) {
				t = T.INT;
				oValue = T.FLOAT;
			}
			// fall through
		case Opcodes.I2L:
			if (t == null) {
				t = T.INT;
				oValue = T.LONG;
			}
			// fall through
		case Opcodes.I2S:
			if (t == null) {
				t = T.INT;
				oValue = T.SHORT;
			}
			// fall through
		case Opcodes.L2D:
			if (t == null) {
				t = T.LONG;
				oValue = T.DOUBLE;
			}
			// fall through
		case Opcodes.L2F:
			if (t == null) {
				t = T.LONG;
				oValue = T.FLOAT;
			}
			// fall through
		case Opcodes.L2I:
			if (t == null) {
				t = T.LONG;
				oValue = T.INT;
			}
			this.operations.add(new CONVERT(this.operations.size(), opcode,
					this.line, t, (T) oValue));
			break;
		/*******
		 * DIV *
		 *******/
		case Opcodes.DDIV:
			t = T.DOUBLE;
			// fall through
		case Opcodes.FDIV:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IDIV:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LDIV:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new DIV(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * DUP *
		 *******/
		case Opcodes.DUP:
			type = DUP.T_DUP;
			// fall through
		case Opcodes.DUP_X1:
			if (t == null) {
				type = DUP.T_DUP_X1;
			}
			// fall through
		case Opcodes.DUP_X2:
			if (t == null) {
				type = DUP.T_DUP_X2;
			}
			// fall through
		case Opcodes.DUP2:
			if (t == null) {
				type = DUP.T_DUP2;
			}
			// fall through
		case Opcodes.DUP2_X1:
			if (t == null) {
				type = DUP.T_DUP2_X1;
			}
			// fall through
		case Opcodes.DUP2_X2:
			if (t == null) {
				type = DUP.T_DUP2_X2;
			}
			this.operations.add(new DUP(this.operations.size(), opcode,
					this.line, type));
			break;
		/***********
		 * MONITOR *
		 ***********/
		case Opcodes.MONITORENTER:
			type = MONITOR.T_ENTER;
			// fall through
		case Opcodes.MONITOREXIT:
			if (t == null) {
				type = MONITOR.T_EXIT;
			}
			this.operations.add(new MONITOR(this.operations.size(), opcode,
					this.line, type));
			break;
		/*******
		 * MUL *
		 *******/
		case Opcodes.DMUL:
			t = T.DOUBLE;
			// fall through
		case Opcodes.FMUL:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IMUL:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LMUL:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new MUL(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * NEG *
		 *******/
		case Opcodes.DNEG:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FNEG:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.INEG:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LNEG:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new NEG(this.operations.size(), opcode,
					this.line, t));
			break;
		/******
		 * OR *
		 ******/
		case Opcodes.IOR:
			t = T.INT;
			// fall through
		case Opcodes.LOR:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new OR(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * POP *
		 *******/
		case Opcodes.POP:
			type = POP.T_POP;
			// fall through
		case Opcodes.POP2:
			if (t == null) {
				type = POP.T_POP2;
			}
			this.operations.add(new POP(this.operations.size(), opcode,
					this.line, type));
			break;
		/********
		 * PUSH *
		 ********/
		case Opcodes.ACONST_NULL:
			t = T.AREF;
			// fall through
		case Opcodes.DCONST_0:
			if (t == null) {
				t = T.DOUBLE;
				oValue = 0D;
			}
			// fall through
		case Opcodes.FCONST_0:
			if (t == null) {
				t = T.FLOAT;
				oValue = 0;
			}
			// fall through
		case Opcodes.ICONST_0:
			if (t == null) {
				t = T.INT;
				oValue = 0;
			}
			// fall through
		case Opcodes.LCONST_0:
			if (t == null) {
				t = T.LONG;
				oValue = 0L;
			}
			// fall through
		case Opcodes.DCONST_1:
			if (t == null) {
				t = T.DOUBLE;
				oValue = 1D;
			}
			// fall through
		case Opcodes.FCONST_1:
			if (t == null) {
				t = T.FLOAT;
				oValue = 1;
			}
			// fall through
		case Opcodes.ICONST_1:
			if (t == null) {
				t = T.INT;
				oValue = 1;
			}
			// fall through
		case Opcodes.LCONST_1:
			if (t == null) {
				t = T.LONG;
				oValue = 1L;
			}
			// fall through
		case Opcodes.FCONST_2:
			if (t == null) {
				t = T.FLOAT;
				oValue = 2;
			}
			// fall through
		case Opcodes.ICONST_2:
			if (t == null) {
				t = T.INT;
				oValue = 2;
			}
			// fall through
		case Opcodes.ICONST_3:
			if (t == null) {
				t = T.INT;
				oValue = 3;
			}
			// fall through
		case Opcodes.ICONST_4:
			if (t == null) {
				t = T.INT;
				oValue = 4;
			}
			// fall through
		case Opcodes.ICONST_5:
			if (t == null) {
				t = T.INT;
				oValue = 5;
			}
			// fall through
		case Opcodes.ICONST_M1:
			if (t == null) {
				t = T.INT;
				oValue = -1;
			}
			this.operations.add(new PUSH(this.operations.size(), opcode,
					this.line, t, oValue));
			break;
		/*******
		 * REM *
		 *******/
		case Opcodes.DREM:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FREM:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IREM:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LREM:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new REM(this.operations.size(), opcode,
					this.line, t));
			break;
		/**********
		 * RETURN *
		 **********/
		case Opcodes.ARETURN:
			t = T.AREF;
			// fall through
		case Opcodes.DRETURN:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FRETURN:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.IRETURN:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LRETURN:
			if (t == null) {
				t = T.LONG;
			}
			// fall through
		case Opcodes.RETURN:
			if (t == null) {
				t = T.VOID;
			}
			this.operations.add(new RETURN(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * SHL *
		 *******/
		case Opcodes.ISHL:
			t = T.INT;
			// fall through
		case Opcodes.LSHL:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new SHL(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * SHR *
		 *******/
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
			t = T.INT;
			// fall through
		case Opcodes.LSHR:
		case Opcodes.LUSHR:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new SHR(this.operations.size(), opcode,
					this.line, t, opcode == Opcodes.IUSHR
							|| opcode == Opcodes.LUSHR));
			break;
		/*******
		 * SUB *
		 *******/
		case Opcodes.DSUB:
			t = T.DOUBLE;
			// fall through
		case Opcodes.FSUB:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.ISUB:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LSUB:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new SUB(this.operations.size(), opcode,
					this.line, t));
			break;
		/********
		 * SWAP *
		 ********/
		case Opcodes.SWAP:
			this.operations.add(new SWAP(this.operations.size(), opcode,
					this.line));
			break;
		/*********
		 * THROW *
		 *********/
		case Opcodes.ATHROW:
			this.operations.add(new THROW(this.operations.size(), opcode,
					this.line));
			break;
		/*******
		 * XOR *
		 *******/
		case Opcodes.IXOR:
			t = T.INT;
			// fall through
		case Opcodes.LXOR: {
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new XOR(this.operations.size(), opcode,
					this.line, t));
			break;
		}
		default:
			LOGGER.warning("Unknown insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		T t = null;

		switch (opcode) {
		/********
		 * PUSH *
		 ********/
		case Opcodes.BIPUSH:
			t = T.INT;
			// fall through
		case Opcodes.SIPUSH:
			if (t == null) {
				t = T.INT;
			}
			this.operations.add(new PUSH(this.operations.size(), opcode,
					this.line, t, operand));
			break;
		case Opcodes.NEWARRAY: {
			final String typeName = new String[] { null, null, null, null,
					boolean.class.getName(), char.class.getName(),
					float.class.getName(), double.class.getName(),
					byte.class.getName(), short.class.getName(),
					int.class.getName(), long.class.getName() }[operand];
			this.operations.add(new NEWARRAY(this.operations.size(), opcode,
					this.line, this.du.getT(typeName), 1));
			break;
		}
		default:
			LOGGER.warning("Unknown int insn opcode '" + opcode + "'!");
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
		T t = null;
		int iValue = Integer.MIN_VALUE;

		final int labelIndex = getLabelIndex(label);

		switch (opcode) {
		/********
		 * GOTO *
		 ********/
		case Opcodes.GOTO: {
			final GOTO op = new GOTO(this.operations.size(), opcode, this.line);
			op.setTargetPc(labelIndex);
			if (labelIndex < 0) {
				getLabelUnresolved(label).add(op);
			}
			this.operations.add(op);
			break;
		}
		/********
		 * JCMP *
		 ********/
		case Opcodes.IF_ACMPEQ:
			t = T.AREF;
			iValue = CompareType.T_EQ;
			// fall through
		case Opcodes.IF_ACMPNE:
			if (t == null) {
				t = T.AREF;
				iValue = CompareType.T_NE;
			}
			// fall through
		case Opcodes.IF_ICMPEQ:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_EQ;
			}
			// fall through
		case Opcodes.IF_ICMPGE:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_GE;
			}
			// fall through
		case Opcodes.IF_ICMPGT:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_GT;
			}
			// fall through
		case Opcodes.IF_ICMPLE:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_LE;
			}
			// fall through
		case Opcodes.IF_ICMPLT:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_LT;
			}
			// fall through
		case Opcodes.IF_ICMPNE:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_NE;
			}
			{
				final JCMP op = new JCMP(this.operations.size(), opcode,
						this.line, t, iValue);
				op.setTargetPc(labelIndex);
				if (labelIndex < 0) {
					getLabelUnresolved(label).add(op);
				}
				this.operations.add(op);
			}
			break;
		/********
		 * JCND *
		 ********/
		case Opcodes.IFNULL:
			t = T.AREF;
			iValue = CompareType.T_EQ;
			// fall through
		case Opcodes.IFNONNULL:
			if (t == null) {
				t = T.AREF;
				iValue = CompareType.T_NE;
			}
			// fall through
		case Opcodes.IFEQ:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_EQ;
			}
			// fall through
		case Opcodes.IFGE:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_GE;
			}
			// fall through
		case Opcodes.IFGT:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_GT;
			}
			// fall through
		case Opcodes.IFLE:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_LE;
			}
			// fall through
		case Opcodes.IFLT:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_LT;
			}
			// fall through
		case Opcodes.IFNE:
			if (t == null) {
				t = T.INT;
				iValue = CompareType.T_NE;
			}
			{
				final JCND op = new JCND(this.operations.size(), opcode,
						this.line, t, iValue);
				op.setTargetPc(labelIndex);
				if (labelIndex < 0) {
					getLabelUnresolved(label).add(op);
				}
				this.operations.add(op);
			}
			break;
		/*******
		 * JSR *
		 *******/
		case Opcodes.JSR: {
			final JSR op = new JSR(this.operations.size(), opcode, this.line);
			op.setTargetPc(labelIndex);
			if (labelIndex < 0) {
				getLabelUnresolved(label).add(op);
			}
			this.operations.add(op);
			break;
		}
		default:
			LOGGER.warning("Unknown jump insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitLabel(final Label label) {
		final Integer labelIndex = this.label2index.put(label,
				this.operations.size());
		if (labelIndex == null) {
			// fresh new label, never referenced before
			return;
		}
		if (labelIndex > 0) {
			// visited before but is known?!
			LOGGER.warning("Label '" + label
					+ "' is not unique, has old opPc '"
					+ this.operations.size() + "'!");
			return;
		}
		final int labelUnknownIndex = labelIndex;
		// unknown and has forward reference
		for (final Object o : this.label2unresolved.get(label)) {
			if (o instanceof GOTO) {
				((GOTO) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof JCMP) {
				((JCMP) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof JCND) {
				((JCND) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof JSR) {
				((JSR) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof SWITCH) {
				final SWITCH op = (SWITCH) o;
				if (labelUnknownIndex == op.getDefaultPc()) {
					op.setDefaultPc(this.operations.size());
				}
				final int[] casePcs = op.getCasePcs();
				for (int i = casePcs.length; i-- > 0;) {
					if (labelUnknownIndex == casePcs[i]) {
						casePcs[i] = this.operations.size();
					}
				}
				continue;
			}
			if (o instanceof Exc) {
				final Exc op = (Exc) o;
				if (labelUnknownIndex == op.getStartPc()) {
					op.setStartPc(this.operations.size());
				}
				if (labelUnknownIndex == op.getEndPc()) {
					op.setEndPc(this.operations.size());
				}
				if (labelUnknownIndex == op.getHandlerPc()) {
					op.setHandlerPc(this.operations.size());
				}
			}
			if (o instanceof Var) {
				final Var op = (Var) o;
				if (labelUnknownIndex == op.getStartPc()) {
					op.setStartPc(this.operations.size());
				}
				if (labelUnknownIndex == op.getEndPc()) {
					op.setEndPc(this.operations.size());
				}
			}
		}
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		T t = null;
		Object oValue = null;

		/********
		 * PUSH *
		 ********/
		if (cst instanceof Type) {
			t = this.du.getT(Class.class);
			oValue = this.du.getDescT(((Type) cst).getDescriptor());
		} else {
			if (cst instanceof Double) {
				t = T.DOUBLE;
			} else if (cst instanceof Float) {
				t = T.FLOAT;
			} else if (cst instanceof Integer) {
				t = T.INT;
			} else if (cst instanceof Long) {
				t = T.LONG;
			} else if (cst instanceof String) {
				t = this.du.getT(String.class);
			} else {
				LOGGER.warning("Unknown ldc insn cst '" + cst + "'!");
			}
			oValue = cst;
		}
		this.operations.add(new PUSH(this.operations.size(), Opcodes.LDC,
				this.line, t, oValue));
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		final int labelIndex = getLabelIndex(start);
		if (labelIndex < 0) {
			LOGGER.warning("Line number '" + line + "' start label '" + start
					+ "' unknown yet?");
		}
		this.line = line;
	}

	@Override
	public void visitLocalVariable(final String name, final String desc,
			final String signature, final Label start, final Label end,
			final int index) {
		final T varT = this.du.getDescT(desc);
		if (signature != null) {
			varT.setSignature(signature);
		}
		final Var var = new Var(varT);
		var.setName(name);

		int labelIndex = getLabelIndex(start);
		var.setStartPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(start).add(var);
		}
		labelIndex = getLabelIndex(end);
		var.setEndPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(end).add(var);
		}

		ArrayList<Var> vars = this.reg2vars.get(index);
		if (vars == null) {
			vars = new ArrayList<Var>();
			this.reg2vars.put(index, vars);
		}
		vars.add(var);
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] caseKeys,
			final Label[] labels) {
		final SWITCH op = new SWITCH(this.operations.size(),
				Opcodes.LOOKUPSWITCH, this.line);
		// default
		int labelIndex = getLabelIndex(dflt);
		op.setDefaultPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(dflt).add(op);
		}
		// keys
		final int[] casePcs = new int[labels.length];
		for (int i = labels.length; i-- > 0;) {
			casePcs[i] = labelIndex = getLabelIndex(labels[i]);
			if (labelIndex < 0) {
				getLabelUnresolved(labels[i]).add(op);
			}
		}
		op.setCaseKeys(caseKeys);
		op.setCasePcs(casePcs);
		this.operations.add(op);
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc) {
		// java/io/PrintStream : println : (Ljava/lang/String;)V
		// [Lorg/decojer/cavaj/test/jdk5/DecTestEnums; : clone :
		// ()Ljava/lang/Object;
		switch (opcode) {
		/**********
		 * INVOKE *
		 **********/
		case Opcodes.INVOKEINTERFACE:
			// interface method callout
		case Opcodes.INVOKESPECIAL:
			// constructor or supermethod callout
		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.INVOKESTATIC: {
			final T invokeT = readType(owner, this.du);
			final M invokeM = invokeT.getM(name, desc);
			if (opcode == Opcodes.INVOKEINTERFACE) {
				invokeT.markAf(AF.INTERFACE);
			}
			if (opcode == Opcodes.INVOKESTATIC) {
				invokeM.markAf(AF.STATIC);
			}
			this.operations.add(new INVOKE(this.operations.size(), opcode,
					this.line, invokeM, opcode == Opcodes.INVOKESPECIAL));
			break;
		}
		default:
			LOGGER.warning("Unknown method insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		/************
		 * NEWARRAY *
		 ************/
		this.operations
				.add(new NEWARRAY(this.operations.size(),
						Opcodes.MULTIANEWARRAY, this.line, this.du
								.getDescT(desc), dims));
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
		final SWITCH op = new SWITCH(this.operations.size(),
				Opcodes.TABLESWITCH, this.line);
		// default
		int labelIndex = getLabelIndex(dflt);
		op.setDefaultPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(dflt).add(op);
		}
		// keys
		final int[] keys = new int[labels.length];
		final int[] keyTargets = new int[labels.length];
		for (int i = labels.length; i-- > 0;) {
			keys[i] = min + i;
			labelIndex = getLabelIndex(labels[i]);
			keyTargets[i] = labelIndex;
			if (labelIndex < 0) {
				getLabelUnresolved(labels[i]).add(op);
			}
		}
		op.setCaseKeys(keys);
		op.setCasePcs(keyTargets);
		this.operations.add(op);
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		// type: java/lang/Exception
		final T catchT = type == null ? null : this.du.getT(type.replace('/',
				'.'));
		final Exc exc = new Exc(catchT);

		int labelIndex = getLabelIndex(start);
		exc.setStartPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(start).add(exc);
		}
		labelIndex = getLabelIndex(end);
		exc.setEndPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(end).add(exc);
		}
		labelIndex = getLabelIndex(handler);
		exc.setHandlerPc(labelIndex);
		if (labelIndex < 0) {
			getLabelUnresolved(handler).add(exc);
		}

		this.excs.add(exc);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		// type: java/lang/StringBuilder, [[I
		final T t = readType(type, this.du);

		switch (opcode) {
		/**************
		 * CHECKCAST *
		 **************/
		case Opcodes.CHECKCAST:
			this.operations.add(new CHECKCAST(this.operations.size(), opcode,
					this.line, t));
			break;
		/**************
		 * INSTANCEOF *
		 **************/
		case Opcodes.INSTANCEOF:
			this.operations.add(new INSTANCEOF(this.operations.size(), opcode,
					this.line, t));
			break;
		/*******
		 * NEW *
		 *******/
		case Opcodes.NEW:
			this.operations.add(new NEW(this.operations.size(), opcode,
					this.line, t));
			break;
		/************
		 * NEWARRAY *
		 ************/
		case Opcodes.ANEWARRAY:
			this.operations.add(new NEWARRAY(this.operations.size(), opcode,
					this.line, t, 1));
			break;
		default:
			LOGGER.warning("Unknown var insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		T t = null;

		switch (opcode) {
		/********
		 * LOAD *
		 ********/
		case Opcodes.ALOAD:
			t = T.AREF;
			// fall through
		case Opcodes.DLOAD:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FLOAD:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.ILOAD:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LLOAD:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new LOAD(this.operations.size(), opcode,
					this.line, t, var));
			break;
		/*********
		 * STORE *
		 *********/
		case Opcodes.ASTORE:
			t = T.AREF;
			// fall through
		case Opcodes.DSTORE:
			if (t == null) {
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FSTORE:
			if (t == null) {
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.ISTORE:
			if (t == null) {
				t = T.INT;
			}
			// fall through
		case Opcodes.LSTORE:
			if (t == null) {
				t = T.LONG;
			}
			this.operations.add(new STORE(this.operations.size(), opcode,
					this.line, t, var));
			break;
		/*******
		 * RET *
		 *******/
		case Opcodes.RET: {
			this.operations.add(new RET(this.operations.size(), opcode,
					this.line, var));
			break;
		}
		default:
			LOGGER.warning("Unknown var insn opcode '" + opcode + "'!");
		}
	}

}