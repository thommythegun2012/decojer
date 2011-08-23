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
import java.util.logging.Logger;

import javassist.bytecode.Opcode;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.DataType;
import org.decojer.cavaj.model.vm.intermediate.Operation;
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
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;
import org.ow2.asm.AnnotationVisitor;
import org.ow2.asm.Attribute;
import org.ow2.asm.Handle;
import org.ow2.asm.Label;
import org.ow2.asm.MethodVisitor;
import org.ow2.asm.Opcodes;

/**
 * Read method visitor.
 * 
 * @author André Pankraz
 */
public class ReadMethodVisitor implements MethodVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadMethodVisitor.class.getName());

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

	private HashMap<Label, Integer> label2index;

	private HashMap<Label, ArrayList<?>> label2unresolved;

	private MD md;

	private ArrayList<Operation> operations;

	private int opLine;

	private int index;

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

	private void addOperation(final Operation operation) {
		this.operations.add(operation);
		++this.index;
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
		this.operations = new ArrayList<Operation>();
		this.label2index = new HashMap<Label, Integer>();
		this.label2unresolved = new HashMap<Label, ArrayList<?>>();
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
			addOperation(new GET(this.index, opcode, this.opLine, f));
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
			addOperation(new PUT(this.index, opcode, this.opLine, f));
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
		addOperation(new INC(this.index, Opcodes.IINC, this.opLine,
				DataType.T_INT, var, increment));
	}

	@Override
	public void visitInsn(final int opcode) {
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
			type = DataType.T_DOUBLE;
			// fall through
		case Opcodes.FADD:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IADD:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LADD:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new ADD(this.index, opcode, this.opLine, type));
			break;
		/*********
		 * ALOAD *
		 *********/
		case Opcodes.AALOAD:
			type = DataType.T_AREF;
			// fall through
		case Opcodes.BALOAD:
			if (type < 0) {
				type = DataType.T_BOOLEAN;
			}
			// fall through
		case Opcodes.CALOAD:
			if (type < 0) {
				type = DataType.T_CHAR;
			}
			// fall through
		case Opcodes.DALOAD:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FALOAD:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IALOAD:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LALOAD:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			// fall through
		case Opcodes.SALOAD:
			if (type < 0) {
				type = DataType.T_SHORT;
			}
			addOperation(new ALOAD(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * AND *
		 *******/
		case Opcodes.IAND:
			type = DataType.T_INT;
			// fall through
		case Opcodes.LAND:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new AND(this.index, opcode, this.opLine, type));
			break;
		/***************
		 * ARRAYLENGTH *
		 ***************/
		case Opcodes.ARRAYLENGTH:
			addOperation(new ARRAYLENGTH(this.index, opcode, this.opLine));
			break;
		/**********
		 * ASTORE *
		 **********/
		case Opcodes.AASTORE:
			type = DataType.T_AREF;
			// fall through
		case Opcodes.BASTORE:
			if (type < 0) {
				type = DataType.T_BOOLEAN;
			}
			// fall through
		case Opcodes.CASTORE:
			if (type < 0) {
				type = DataType.T_CHAR;
			}
			// fall through
		case Opcodes.DASTORE:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FASTORE:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IASTORE:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LASTORE:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			// fall through
		case Opcodes.SASTORE:
			if (type < 0) {
				type = DataType.T_SHORT;
			}
			addOperation(new ASTORE(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * CMP *
		 *******/
		case Opcodes.DCMPG:
			type = DataType.T_DOUBLE;
			iValue = CMP.T_G;
			// fall through
		case Opcodes.DCMPL:
			if (type < 0) {
				type = DataType.T_DOUBLE;
				iValue = CMP.T_L;
			}
			// fall through
		case Opcodes.FCMPG:
			if (type < 0) {
				type = DataType.T_FLOAT;
				iValue = CMP.T_G;
			}
			// fall through
		case Opcodes.FCMPL:
			if (type < 0) {
				type = DataType.T_FLOAT;
				iValue = CMP.T_L;
			}
			// fall through
		case Opcodes.LCMP:
			if (type < 0) {
				type = DataType.T_LONG;
				iValue = CMP.T_0;
			}
			addOperation(new CMP(this.index, opcode, this.opLine, type, iValue));
			break;
		/***********
		 * CONVERT *
		 ***********/
		case Opcodes.D2F:
			type = DataType.T_DOUBLE;
			iValue = DataType.T_FLOAT;
			// fall through
		case Opcodes.D2I:
			if (type < 0) {
				type = DataType.T_DOUBLE;
				iValue = DataType.T_INT;
			}
			// fall through
		case Opcodes.D2L:
			if (type < 0) {
				type = DataType.T_DOUBLE;
				iValue = DataType.T_LONG;
			}
			// fall through
		case Opcodes.F2D:
			if (type < 0) {
				type = DataType.T_FLOAT;
				iValue = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.F2I:
			if (type < 0) {
				type = DataType.T_FLOAT;
				iValue = DataType.T_INT;
			}
			// fall through
		case Opcodes.F2L:
			if (type < 0) {
				type = DataType.T_FLOAT;
				iValue = DataType.T_LONG;
			}
			// fall through
		case Opcodes.I2B:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = DataType.T_BYTE;
			}
			// fall through
		case Opcodes.I2C:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = DataType.T_CHAR;
			}
			// fall through
		case Opcodes.I2D:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.I2F:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.I2L:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = DataType.T_LONG;
			}
			// fall through
		case Opcodes.I2S:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = DataType.T_SHORT;
			}
			// fall through
		case Opcodes.L2D:
			if (type < 0) {
				type = DataType.T_LONG;
				iValue = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.L2F:
			if (type < 0) {
				type = DataType.T_LONG;
				iValue = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.L2I:
			if (type < 0) {
				type = DataType.T_LONG;
				iValue = DataType.T_INT;
			}
			addOperation(new CONVERT(this.index, opcode, this.opLine, type,
					iValue));
			break;
		/*******
		 * DIV *
		 *******/
		case Opcodes.DDIV:
			type = DataType.T_DOUBLE;
			// fall through
		case Opcodes.FDIV:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IDIV:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LDIV:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new DIV(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * DUP *
		 *******/
		case Opcodes.DUP:
			type = DUP.T_DUP;
			// fall through
		case Opcodes.DUP_X1:
			if (type < 0) {
				type = DUP.T_DUP_X1;
			}
			// fall through
		case Opcodes.DUP_X2:
			if (type < 0) {
				type = DUP.T_DUP_X2;
			}
			// fall through
		case Opcodes.DUP2:
			if (type < 0) {
				type = DUP.T_DUP2;
			}
			// fall through
		case Opcodes.DUP2_X1:
			if (type < 0) {
				type = DUP.T_DUP2_X1;
			}
			// fall through
		case Opcodes.DUP2_X2:
			if (type < 0) {
				type = DUP.T_DUP2_X2;
			}
			addOperation(new DUP(this.index, opcode, this.opLine, type));
			break;
		/***********
		 * MONITOR *
		 ***********/
		case Opcodes.MONITORENTER:
			type = MONITOR.T_ENTER;
			// fall through
		case Opcodes.MONITOREXIT:
			if (type < 0) {
				type = MONITOR.T_EXIT;
			}
			addOperation(new MONITOR(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * MUL *
		 *******/
		case Opcodes.DMUL:
			type = DataType.T_DOUBLE;
			// fall through
		case Opcodes.FMUL:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IMUL:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LMUL:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new MUL(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * NEG *
		 *******/
		case Opcodes.DNEG:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FNEG:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.INEG:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LNEG:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new NEG(this.index, opcode, this.opLine, type));
			break;
		/******
		 * OR *
		 ******/
		case Opcodes.IOR:
			type = DataType.T_INT;
			// fall through
		case Opcodes.LOR:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new OR(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * POP *
		 *******/
		case Opcodes.POP:
			type = POP.T_POP;
			// fall through
		case Opcodes.POP2:
			if (type < 0) {
				type = POP.T_POP2;
			}
			addOperation(new POP(this.index, opcode, this.opLine, type));
			break;
		/********
		 * PUSH *
		 ********/
		case Opcodes.ACONST_NULL:
		case Opcodes.DCONST_0:
			if (type < 0) {
				type = DataType.T_DOUBLE;
				oValue = 0D;
			}
			// fall through
		case Opcodes.FCONST_0:
			if (type < 0) {
				type = DataType.T_FLOAT;
				oValue = 0;
			}
			// fall through
		case Opcodes.ICONST_0:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = 0;
			}
			// fall through
		case Opcodes.LCONST_0:
			if (type < 0) {
				type = DataType.T_LONG;
				oValue = 0L;
			}
			// fall through
		case Opcodes.DCONST_1:
			if (type < 0) {
				type = DataType.T_DOUBLE;
				oValue = 1D;
			}
			// fall through
		case Opcodes.FCONST_1:
			if (type < 0) {
				type = DataType.T_FLOAT;
				oValue = 1;
			}
			// fall through
		case Opcodes.ICONST_1:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = 1;
			}
			// fall through
		case Opcodes.LCONST_1:
			if (type < 0) {
				type = DataType.T_LONG;
				oValue = 1L;
			}
			// fall through
		case Opcodes.FCONST_2:
			if (type < 0) {
				type = DataType.T_FLOAT;
				oValue = 2;
			}
			// fall through
		case Opcodes.ICONST_2:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = 2;
			}
			// fall through
		case Opcodes.ICONST_3:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = 3;
			}
			// fall through
		case Opcodes.ICONST_4:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = 4;
			}
			// fall through
		case Opcodes.ICONST_5:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = 5;
			}
			// fall through
		case Opcodes.ICONST_M1:
			if (type < 0) {
				type = DataType.T_INT;
				oValue = -1;
			}
			addOperation(new PUSH(this.index, opcode, this.opLine, type, oValue));
			break;
		/*******
		 * REM *
		 *******/
		case Opcodes.DREM:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FREM:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IREM:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LREM:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new REM(this.index, opcode, this.opLine, type));
			break;
		/**********
		 * RETURN *
		 **********/
		case Opcodes.ARETURN:
			type = DataType.T_AREF;
			// fall through
		case Opcodes.DRETURN:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FRETURN:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.IRETURN:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LRETURN:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			// fall through
		case Opcodes.RETURN:
			if (type < 0) {
				type = DataType.T_VOID;
			}
			addOperation(new RETURN(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * SHL *
		 *******/
		case Opcodes.ISHL:
			type = DataType.T_INT;
			// fall through
		case Opcodes.LSHL:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new SHL(this.index, opcode, this.opLine, type));
			break;
		/*******
		 * SHR *
		 *******/
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
			type = DataType.T_INT;
			// fall through
		case Opcodes.LSHR:
		case Opcodes.LUSHR:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new SHR(this.index, opcode, this.opLine, type,
					opcode == Opcodes.IUSHR || opcode == Opcodes.LUSHR));
			break;
		/*******
		 * SUB *
		 *******/
		case Opcodes.DSUB:
			type = DataType.T_DOUBLE;
			// fall through
		case Opcodes.FSUB:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.ISUB:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LSUB:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new SUB(this.index, opcode, this.opLine, type));
			break;
		/********
		 * SWAP *
		 ********/
		case Opcodes.SWAP:
			addOperation(new SWAP(this.index, opcode, this.opLine));
			break;
		/*********
		 * THROW *
		 *********/
		case Opcodes.ATHROW:
			addOperation(new THROW(this.index, opcode, this.opLine));
			break;
		/*******
		 * XOR *
		 *******/
		case Opcodes.IXOR:
			type = DataType.T_INT;
			// fall through
		case Opcodes.LXOR: {
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new XOR(this.index, opcode, this.opLine, type));
			break;
		}
		default:
			LOGGER.warning("Unknown insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		int type = -1;

		switch (opcode) {
		/********
		 * PUSH *
		 ********/
		case Opcodes.BIPUSH:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.SIPUSH:
			if (type < 0) {
				type = DataType.T_INT;
			}
			addOperation(new PUSH(this.index, opcode, this.opLine, type,
					operand));
			break;
		case Opcodes.NEWARRAY: {
			final String typeName = new String[] { null, null, null, null,
					boolean.class.getName(), char.class.getName(),
					float.class.getName(), double.class.getName(),
					byte.class.getName(), short.class.getName(),
					int.class.getName(), long.class.getName() }[operand];
			addOperation(new NEWARRAY(this.index, opcode, this.opLine,
					this.du.getT(typeName), 1));
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
		int type = -1;
		int iValue = Integer.MIN_VALUE;

		final Integer index = this.label2index.get(label);
		if (index == null) {
			ArrayList<?> unresolved = this.label2unresolved.get(label);
			if (unresolved == null) {
				unresolved = new ArrayList<Object>();
				this.label2unresolved.put(label, unresolved);
			}
		}

		switch (opcode) {
		/********
		 * GOTO *
		 ********/
		case Opcodes.GOTO:
			addOperation(new GOTO(this.index, opcode, this.opLine));
			break;
		/********
		 * JCMP *
		 ********/
		case Opcode.IF_ACMPEQ:
			type = DataType.T_AREF;
			iValue = CompareType.T_EQ;
			// fall through
		case Opcode.IF_ACMPNE:
			if (type < 0) {
				type = DataType.T_AREF;
				iValue = CompareType.T_NE;
			}
			// fall through
		case Opcode.IF_ICMPEQ:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_EQ;
			}
			// fall through
		case Opcode.IF_ICMPGE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_GE;
			}
			// fall through
		case Opcode.IF_ICMPGT:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_GT;
			}
			// fall through
		case Opcode.IF_ICMPLE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_LE;
			}
			// fall through
		case Opcode.IF_ICMPLT:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_LT;
			}
			// fall through
		case Opcode.IF_ICMPNE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_NE;
			}
			addOperation(new JCMP(this.index, opcode, this.opLine, type, iValue));
			break;
		/********
		 * JCND *
		 ********/
		case Opcodes.IFNULL:
			type = DataType.T_AREF;
			iValue = CompareType.T_EQ;
			// fall through
		case Opcodes.IFNONNULL:
			if (type < 0) {
				type = DataType.T_AREF;
				iValue = CompareType.T_NE;
			}
			// fall through
		case Opcodes.IFEQ:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_EQ;
			}
			// fall through
		case Opcode.IFGE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_GE;
			}
			// fall through
		case Opcodes.IFGT:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_GT;
			}
			// fall through
		case Opcodes.IFLE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_LE;
			}
			// fall through
		case Opcodes.IFLT:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_LT;
			}
			// fall through
		case Opcodes.IFNE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_NE;
			}
			addOperation(new JCND(this.index, opcode, this.opLine, type, iValue));
			break;
		/*******
		 * JSR *
		 *******/
		case Opcodes.JSR:
			// TODO
			System.out.println("### JSR: " + iValue + " : " + label);
			break;
		default:
			LOGGER.warning("Unknown jump insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitLabel(final Label label) {
		final Integer index = this.label2index.put(label, this.index);
		if (index != null) {
			LOGGER.warning("Label '" + label
					+ "' is not unique, has old opPc '" + index + "'!");
		}
		for (final Object o : this.label2unresolved.get(label)) {
			if (o instanceof GOTO) {
				((GOTO) o).setTargetPc(index);
			}
		}
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		int type = -1;

		/********
		 * PUSH *
		 ********/
		if (cst instanceof Double) {
			type = DataType.T_DOUBLE;
		} else if (cst instanceof Float) {
			type = DataType.T_FLOAT;
		} else if (cst instanceof Integer) {
			type = DataType.T_INT;
		} else if (cst instanceof Long) {
			type = DataType.T_LONG;
		} else if (cst instanceof String) {
			type = DataType.T_STRING;
		} else {
			LOGGER.warning("Unknown ldc insn cst '" + cst + "'!");
		}
		addOperation(new PUSH(this.index, Opcodes.LDC, this.opLine, type, cst));
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		if (TODOCODE) {
			LOGGER.warning("### method visitLineNumber ### " + line + " : "
					+ start);
		}
		this.opLine = line;
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
		// java/io/PrintStream : println : (Ljava/lang/String;)V
		// [Lorg/decojer/cavaj/test/jdk5/DecTestEnums; : clone :
		// ()Ljava/lang/Object;
		switch (opcode) {
		/**********
		 * INVOKE *
		 **********/
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKESPECIAL:
			// constructor or supermethod callout
		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.INVOKESTATIC: {
			final T invokeT = readType(owner, this.du);
			final M invokeM = invokeT.getM(name, desc);
			if (opcode == Opcodes.INVOKEINTERFACE) {
				invokeM.markAf(AF.STATIC);
			}
			if (opcode == Opcodes.INVOKESTATIC) {
				invokeM.markAf(AF.STATIC);
			}
			addOperation(new INVOKE(this.index, opcode, this.opLine, invokeM,
					opcode == Opcodes.INVOKESPECIAL));
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
		addOperation(new NEWARRAY(this.index, Opcodes.MULTIANEWARRAY,
				this.opLine, this.du.getDescT(desc), dims));
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
		// type: java/lang/StringBuilder, [[I
		final T t = readType(type, this.du);

		switch (opcode) {
		/**************
		 * CHECKCAST *
		 **************/
		case Opcodes.CHECKCAST:
			addOperation(new CHECKCAST(this.index, opcode, this.opLine, t));
			break;
		/**************
		 * INSTANCEOF *
		 **************/
		case Opcodes.INSTANCEOF:
			addOperation(new INSTANCEOF(this.index, opcode, this.opLine, t));
			break;
		/*******
		 * NEW *
		 *******/
		case Opcodes.NEW:
			addOperation(new NEW(this.index, opcode, this.opLine, t));
			break;
		/************
		 * NEWARRAY *
		 ************/
		case Opcodes.ANEWARRAY:
			addOperation(new NEWARRAY(this.index, opcode, this.opLine, t, 1));
			break;
		default:
			LOGGER.warning("Unknown var insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		int type = -1;

		switch (opcode) {
		/********
		 * LOAD *
		 ********/
		case Opcodes.ALOAD:
			type = DataType.T_AREF;
			// fall through
		case Opcodes.DLOAD:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FLOAD:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.ILOAD:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LLOAD:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new LOAD(this.index, opcode, this.opLine, type, var));
			break;
		/*********
		 * STORE *
		 *********/
		case Opcodes.ASTORE:
			type = DataType.T_AREF;
			// fall through
		case Opcodes.DSTORE:
			if (type < 0) {
				type = DataType.T_DOUBLE;
			}
			// fall through
		case Opcodes.FSTORE:
			if (type < 0) {
				type = DataType.T_FLOAT;
			}
			// fall through
		case Opcodes.ISTORE:
			if (type < 0) {
				type = DataType.T_INT;
			}
			// fall through
		case Opcodes.LSTORE:
			if (type < 0) {
				type = DataType.T_LONG;
			}
			addOperation(new STORE(this.index, opcode, this.opLine, type, var));
			break;
		/*******
		 * RET *
		 *******/
		case Opcodes.RET: {
			addOperation(new RET(this.index, opcode, this.opLine, var));
			break;
		}
		default:
			LOGGER.warning("Unknown var insn opcode '" + opcode + "'!");
		}
	}

}