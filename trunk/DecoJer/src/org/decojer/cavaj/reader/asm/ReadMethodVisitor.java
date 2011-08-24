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
import org.decojer.cavaj.model.vm.intermediate.DataType;
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

	private int line;

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
			this.line = 0;

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
				this.line, DataType.T_INT, var, increment));
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
			this.operations.add(new ADD(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new ALOAD(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new AND(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new ASTORE(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new CMP(this.operations.size(), opcode,
					this.line, type, iValue));
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
			this.operations.add(new CONVERT(this.operations.size(), opcode,
					this.line, type, iValue));
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
			this.operations.add(new DIV(this.operations.size(), opcode,
					this.line, type));
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
			if (type < 0) {
				type = MONITOR.T_EXIT;
			}
			this.operations.add(new MONITOR(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new MUL(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new NEG(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new OR(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new POP(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new PUSH(this.operations.size(), opcode,
					this.line, type, oValue));
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
			this.operations.add(new REM(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new RETURN(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new SHL(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new SHR(this.operations.size(), opcode,
					this.line, type, opcode == Opcodes.IUSHR
							|| opcode == Opcodes.LUSHR));
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
			this.operations.add(new SUB(this.operations.size(), opcode,
					this.line, type));
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
			type = DataType.T_INT;
			// fall through
		case Opcodes.LXOR: {
			if (type < 0) {
				type = DataType.T_LONG;
			}
			this.operations.add(new XOR(this.operations.size(), opcode,
					this.line, type));
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
			this.operations.add(new PUSH(this.operations.size(), opcode,
					this.line, type, operand));
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
		int type = -1;
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
			type = DataType.T_AREF;
			iValue = CompareType.T_EQ;
			// fall through
		case Opcodes.IF_ACMPNE:
			if (type < 0) {
				type = DataType.T_AREF;
				iValue = CompareType.T_NE;
			}
			// fall through
		case Opcodes.IF_ICMPEQ:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_EQ;
			}
			// fall through
		case Opcodes.IF_ICMPGE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_GE;
			}
			// fall through
		case Opcodes.IF_ICMPGT:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_GT;
			}
			// fall through
		case Opcodes.IF_ICMPLE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_LE;
			}
			// fall through
		case Opcodes.IF_ICMPLT:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_LT;
			}
			// fall through
		case Opcodes.IF_ICMPNE:
			if (type < 0) {
				type = DataType.T_INT;
				iValue = CompareType.T_NE;
			}
			{
				final JCMP op = new JCMP(this.operations.size(), opcode,
						this.line, type, iValue);
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
		case Opcodes.IFGE:
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
			{
				final JCND op = new JCND(this.operations.size(), opcode,
						this.line, type, iValue);
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
				final int[] keyTargets = op.getKeyPcs();
				for (int i = keyTargets.length; i-- > 0;) {
					if (labelUnknownIndex == keyTargets[i]) {
						keyTargets[i] = this.operations.size();
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
		int type = -1;
		Object oValue = null;

		/********
		 * PUSH *
		 ********/
		if (cst instanceof Type) {
			type = DataType.T_CLASS;
			oValue = this.du.getDescT(((Type) cst).getDescriptor());
		} else {
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
			oValue = cst;
		}
		this.operations.add(new PUSH(this.operations.size(), Opcodes.LDC,
				this.line, type, oValue));
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
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
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
		final int[] keyTargets = new int[labels.length];
		for (int i = labels.length; i-- > 0;) {
			keyTargets[i] = labelIndex = getLabelIndex(labels[i]);
			if (labelIndex < 0) {
				getLabelUnresolved(labels[i]).add(op);
			}
		}
		op.setKeys(keys);
		op.setKeyPcs(keyTargets);
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
		op.setKeys(keys);
		op.setKeyPcs(keyTargets);
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
			this.operations.add(new LOAD(this.operations.size(), opcode,
					this.line, type, var));
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
			this.operations.add(new STORE(this.operations.size(), opcode,
					this.line, type, var));
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