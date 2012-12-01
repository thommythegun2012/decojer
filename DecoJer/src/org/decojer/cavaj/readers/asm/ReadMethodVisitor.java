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

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.ops.ADD;
import org.decojer.cavaj.model.code.ops.ALOAD;
import org.decojer.cavaj.model.code.ops.AND;
import org.decojer.cavaj.model.code.ops.ARRAYLENGTH;
import org.decojer.cavaj.model.code.ops.ASTORE;
import org.decojer.cavaj.model.code.ops.CAST;
import org.decojer.cavaj.model.code.ops.CMP;
import org.decojer.cavaj.model.code.ops.CmpType;
import org.decojer.cavaj.model.code.ops.DIV;
import org.decojer.cavaj.model.code.ops.DUP;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.INC;
import org.decojer.cavaj.model.code.ops.INSTANCEOF;
import org.decojer.cavaj.model.code.ops.INVOKE;
import org.decojer.cavaj.model.code.ops.JCMP;
import org.decojer.cavaj.model.code.ops.JCND;
import org.decojer.cavaj.model.code.ops.JSR;
import org.decojer.cavaj.model.code.ops.LOAD;
import org.decojer.cavaj.model.code.ops.MONITOR;
import org.decojer.cavaj.model.code.ops.MUL;
import org.decojer.cavaj.model.code.ops.NEG;
import org.decojer.cavaj.model.code.ops.NEW;
import org.decojer.cavaj.model.code.ops.NEWARRAY;
import org.decojer.cavaj.model.code.ops.OR;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.POP;
import org.decojer.cavaj.model.code.ops.PUSH;
import org.decojer.cavaj.model.code.ops.PUT;
import org.decojer.cavaj.model.code.ops.REM;
import org.decojer.cavaj.model.code.ops.RET;
import org.decojer.cavaj.model.code.ops.RETURN;
import org.decojer.cavaj.model.code.ops.SHL;
import org.decojer.cavaj.model.code.ops.SHR;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.code.ops.SUB;
import org.decojer.cavaj.model.code.ops.SWAP;
import org.decojer.cavaj.model.code.ops.SWITCH;
import org.decojer.cavaj.model.code.ops.THROW;
import org.decojer.cavaj.model.code.ops.XOR;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.Cursor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ASM read method visitor.
 * 
 * @author André Pankraz
 */
public class ReadMethodVisitor extends MethodVisitor {

	private final static Logger LOGGER = Logger.getLogger(ReadMethodVisitor.class.getName());

	private static final boolean TODOCODE = true;

	private A[] as;

	private final DU du;

	private final ArrayList<Exc> excs = new ArrayList<Exc>();

	private final HashMap<Label, Integer> label2pc = new HashMap<Label, Integer>();

	private final HashMap<Label, ArrayList<Object>> label2unresolved = new HashMap<Label, ArrayList<Object>>();

	private int line = -1;

	private int maxLocals;

	private int maxStack;

	private MD md;

	private final ArrayList<Op> ops = new ArrayList<Op>();

	private A[][] paramAss;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	private final HashMap<Integer, ArrayList<V>> reg2vs = new HashMap<Integer, ArrayList<V>>();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadMethodVisitor(final DU du) {
		super(Opcodes.ASM4);

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

	private int getPc(final Label label) {
		assert label != null;

		final Integer pc = this.label2pc.get(label);
		if (pc != null) {
			return pc;
		}
		final int unresolvedPc = -1 - this.label2unresolved.size();
		this.label2pc.put(label, unresolvedPc);
		return unresolvedPc;
	}

	private ArrayList<Object> getUnresolved(final Label label) {
		assert label != null;

		ArrayList<Object> unresolved = this.label2unresolved.get(label);
		if (unresolved == null) {
			unresolved = new ArrayList<Object>();
			this.label2unresolved.put(label, unresolved);
		}
		return unresolved;
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
		LOGGER.warning("Unknown method attribute tag '" + attr.type + "' for field info '"
				+ this.md.getTd() + "'!");
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
		if (this.ops.size() > 0) {
			final CFG cfg = new CFG(this.md, this.maxLocals, this.maxStack);
			this.md.setCfg(cfg);

			cfg.setOps(this.ops.toArray(new Op[this.ops.size()]));
			this.ops.clear();
			this.label2pc.clear();
			this.label2unresolved.clear();
			this.line = -1;

			if (this.excs.size() > 0) {
				cfg.setExcs(this.excs.toArray(new Exc[this.excs.size()]));
				this.excs.clear();
			}
			if (this.reg2vs.size() > 0) {
				for (final Entry<Integer, ArrayList<V>> entry : this.reg2vs.entrySet()) {
					final int reg = entry.getKey();
					for (final V var : entry.getValue()) {
						cfg.addVar(reg, var);
					}
				}
				this.reg2vs.clear();
			}
			cfg.postProcessVars();
		}
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name,
			final String desc) {
		// ### 178 : java/lang/System : out : Ljava/io/PrintStream;
		switch (opcode) {
		/*******
		 * GET *
		 *******/
		case Opcodes.GETFIELD:
		case Opcodes.GETSTATIC: {
			final T ownerT = this.du.getT(owner);
			final F f = ownerT.getF(name, this.du.getDescT(desc));
			if (opcode == Opcodes.GETSTATIC) {
				f.markAf(AF.STATIC);
			}
			this.ops.add(new GET(this.ops.size(), opcode, this.line, f));
			return;
		}
		/*******
		 * PUT *
		 *******/
		case Opcodes.PUTFIELD:
		case Opcodes.PUTSTATIC: {
			final T ownerT = this.du.getT(owner);
			final F f = ownerT.getF(name, this.du.getDescT(desc));
			if (opcode == Opcodes.PUTSTATIC) {
				f.markAf(AF.STATIC);
			}
			this.ops.add(new PUT(this.ops.size(), opcode, this.line, f));
			return;
		}
		default:
			LOGGER.warning("Unknown field insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitFrame(final int type, final int nLocal, final Object[] local,
			final int nStack, final Object[] stack) {
		// LOGGER.info("### method visitFrame ### " + type + " : " + nLocal
		// + " : " + local + " : " + nStack + " : " + stack);
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		/*******
		 * INC *
		 *******/
		this.ops.add(new INC(this.ops.size(), Opcodes.IINC, this.line, T.INT, var, increment));
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
			this.ops.add(new ADD(this.ops.size(), opcode, this.line, t));
			break;
		/*********
		 * ALOAD *
		 *********/
		case Opcodes.AALOAD:
			t = T.REF;
			// fall through
		case Opcodes.BALOAD:
			if (t == null) {
				t = T.SMALL;
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
			this.ops.add(new ALOAD(this.ops.size(), opcode, this.line, t));
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
			this.ops.add(new AND(this.ops.size(), opcode, this.line, t));
			break;
		/***************
		 * ARRAYLENGTH *
		 ***************/
		case Opcodes.ARRAYLENGTH:
			this.ops.add(new ARRAYLENGTH(this.ops.size(), opcode, this.line));
			break;
		/**********
		 * ASTORE *
		 **********/
		case Opcodes.AASTORE:
			t = T.REF;
			// fall through
		case Opcodes.BASTORE:
			if (t == null) {
				t = T.SMALL;
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
			this.ops.add(new ASTORE(this.ops.size(), opcode, this.line, t));
			break;
		/********
		 * CAST *
		 ********/
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
			this.ops.add(new CAST(this.ops.size(), opcode, this.line, t, (T) oValue));
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
			this.ops.add(new CMP(this.ops.size(), opcode, this.line, t, iValue));
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
			this.ops.add(new DIV(this.ops.size(), opcode, this.line, t));
			break;
		/*******
		 * DUP *
		 *******/
		case Opcodes.DUP:
			oValue = DUP.Kind.DUP;
			// fall through
		case Opcodes.DUP_X1:
			if (oValue == null) {
				oValue = DUP.Kind.DUP_X1;
			}
			// fall through
		case Opcodes.DUP_X2:
			if (oValue == null) {
				oValue = DUP.Kind.DUP_X2;
			}
			// fall through
		case Opcodes.DUP2:
			if (oValue == null) {
				oValue = DUP.Kind.DUP2;
			}
			// fall through
		case Opcodes.DUP2_X1:
			if (oValue == null) {
				oValue = DUP.Kind.DUP2_X1;
			}
			// fall through
		case Opcodes.DUP2_X2:
			if (oValue == null) {
				oValue = DUP.Kind.DUP2_X2;
			}
			this.ops.add(new DUP(this.ops.size(), opcode, this.line, (DUP.Kind) oValue));
			break;
		/***********
		 * MONITOR *
		 ***********/
		case Opcodes.MONITORENTER:
			type = MONITOR.T_ENTER;
			// fall through
		case Opcodes.MONITOREXIT:
			if (type == -1) {
				type = MONITOR.T_EXIT;
			}
			this.ops.add(new MONITOR(this.ops.size(), opcode, this.line, type));
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
			this.ops.add(new MUL(this.ops.size(), opcode, this.line, t));
			break;
		/*******
		 * NEG *
		 *******/
		case Opcodes.DNEG:
			t = T.DOUBLE;
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
			this.ops.add(new NEG(this.ops.size(), opcode, this.line, t));
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
			this.ops.add(new OR(this.ops.size(), opcode, this.line, t));
			break;
		/*******
		 * POP *
		 *******/
		case Opcodes.POP:
			oValue = POP.Kind.POP;
			// fall through
		case Opcodes.POP2:
			if (oValue == null) {
				oValue = POP.Kind.POP;
			}
			this.ops.add(new POP(this.ops.size(), opcode, this.line, (POP.Kind) oValue));
			break;
		/********
		 * PUSH *
		 ********/
		case Opcodes.ACONST_NULL:
			t = T.REF;
			// fall through
		case Opcodes.DCONST_0:
			if (t == null) {
				oValue = 0D;
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FCONST_0:
			if (t == null) {
				oValue = 0F;
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.ICONST_0:
			if (t == null) {
				oValue = 0;
				t = T.getJvmIntT(0);
			}
			// fall through
		case Opcodes.LCONST_0:
			if (t == null) {
				oValue = 0L;
				t = T.LONG;
			}
			// fall through
		case Opcodes.DCONST_1:
			if (t == null) {
				oValue = 1D;
				t = T.DOUBLE;
			}
			// fall through
		case Opcodes.FCONST_1:
			if (t == null) {
				oValue = 1F;
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.ICONST_1:
			if (t == null) {
				oValue = 1;
				t = T.getJvmIntT(1);
			}
			// fall through
		case Opcodes.LCONST_1:
			if (t == null) {
				oValue = 1L;
				t = T.LONG;
			}
			// fall through
		case Opcodes.FCONST_2:
			if (t == null) {
				oValue = 2F;
				t = T.FLOAT;
			}
			// fall through
		case Opcodes.ICONST_2:
			if (t == null) {
				oValue = 2;
				t = T.getJvmIntT(2);
			}
			// fall through
		case Opcodes.ICONST_3:
			if (t == null) {
				oValue = 3;
				t = T.getJvmIntT(3);
			}
			// fall through
		case Opcodes.ICONST_4:
			if (t == null) {
				oValue = 4;
				t = T.getJvmIntT(4);
			}
			// fall through
		case Opcodes.ICONST_5:
			if (t == null) {
				oValue = 5;
				t = T.getJvmIntT(5);
			}
			// fall through
		case Opcodes.ICONST_M1:
			if (t == null) {
				oValue = -1;
				t = T.getJvmIntT(-1);
			}
			this.ops.add(new PUSH(this.ops.size(), opcode, this.line, t, oValue));
			break;
		/*******
		 * REM *
		 *******/
		case Opcodes.DREM:
			t = T.DOUBLE;
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
			this.ops.add(new REM(this.ops.size(), opcode, this.line, t));
			break;
		/**********
		 * RETURN *
		 **********/
		case Opcodes.ARETURN:
			t = T.REF;
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
				t = T.AINT;
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
			// cannot check assignable here...variable types could be unresolved
			this.ops.add(new RETURN(this.ops.size(), opcode, this.line, t));
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
			this.ops.add(new SHL(this.ops.size(), opcode, this.line, t, T.INT));
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
			this.ops.add(new SHR(this.ops.size(), opcode, this.line, t, T.INT,
					opcode == Opcodes.IUSHR || opcode == Opcodes.LUSHR));
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
			this.ops.add(new SUB(this.ops.size(), opcode, this.line, t));
			break;
		/********
		 * SWAP *
		 ********/
		case Opcodes.SWAP:
			this.ops.add(new SWAP(this.ops.size(), opcode, this.line));
			break;
		/*********
		 * THROW *
		 *********/
		case Opcodes.ATHROW:
			this.ops.add(new THROW(this.ops.size(), opcode, this.line));
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
			this.ops.add(new XOR(this.ops.size(), opcode, this.line, t));
			break;
		}
		default:
			LOGGER.warning("Unknown insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		switch (opcode) {
		/********
		 * PUSH *
		 ********/
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			this.ops.add(new PUSH(this.ops.size(), opcode, this.line, T.getJvmIntT(operand),
					operand));
			break;
		/************
		 * NEWARRAY *
		 ************/
		case Opcodes.NEWARRAY:
			this.ops.add(new NEWARRAY(this.ops.size(), opcode, this.line, T.TYPES[operand], 1));
			break;
		default:
			LOGGER.warning("Unknown int insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,
			final Object... bsmArgs) {
		if (TODOCODE) {
			LOGGER.warning("### method visitInvokeDynamicInsn ### " + name + " : " + desc + " : "
					+ bsm + " : " + bsmArgs);
		}
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		T t = null;
		Object oValue = null;

		final int targetPc = getPc(label);

		switch (opcode) {
		/********
		 * GOTO *
		 ********/
		case Opcodes.GOTO: {
			final GOTO op = new GOTO(this.ops.size(), opcode, this.line);
			this.ops.add(op);
			op.setTargetPc(targetPc);
			if (targetPc < 0) {
				getUnresolved(label).add(op);
			}
			break;
		}
		/********
		 * JCMP *
		 ********/
		case Opcodes.IF_ACMPEQ:
			t = T.REF;
			oValue = CmpType.T_EQ;
			// fall through
		case Opcodes.IF_ACMPNE:
			if (t == null) {
				t = T.REF;
				oValue = CmpType.T_NE;
			}
			// fall through
		case Opcodes.IF_ICMPEQ:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_EQ;
			}
			// fall through
		case Opcodes.IF_ICMPGE:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_GE;
			}
			// fall through
		case Opcodes.IF_ICMPGT:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_GT;
			}
			// fall through
		case Opcodes.IF_ICMPLE:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_LE;
			}
			// fall through
		case Opcodes.IF_ICMPLT:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_LT;
			}
			// fall through
		case Opcodes.IF_ICMPNE:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_NE;
			}
			{
				final JCMP op = new JCMP(this.ops.size(), opcode, this.line, t, (CmpType) oValue);
				this.ops.add(op);
				op.setTargetPc(targetPc);
				if (targetPc < 0) {
					getUnresolved(label).add(op);
				}
			}
			break;
		/********
		 * JCND *
		 ********/
		case Opcodes.IFNULL:
			t = T.REF;
			oValue = CmpType.T_EQ;
			// fall through
		case Opcodes.IFNONNULL:
			if (t == null) {
				t = T.REF;
				oValue = CmpType.T_NE;
			}
			// fall through
		case Opcodes.IFEQ:
			if (t == null) {
				t = T.AINT; // boolean too
				oValue = CmpType.T_EQ;
			}
			// fall through
		case Opcodes.IFGE:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_GE;
			}
			// fall through
		case Opcodes.IFGT:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_GT;
			}
			// fall through
		case Opcodes.IFLE:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_LE;
			}
			// fall through
		case Opcodes.IFLT:
			if (t == null) {
				t = T.INT;
				oValue = CmpType.T_LT;
			}
			// fall through
		case Opcodes.IFNE:
			if (t == null) {
				t = T.AINT; // boolean too
				oValue = CmpType.T_NE;
			}
			{
				final JCND op = new JCND(this.ops.size(), opcode, this.line, t, (CmpType) oValue);
				this.ops.add(op);
				op.setTargetPc(targetPc);
				if (targetPc < 0) {
					getUnresolved(label).add(op);
				}
			}
			break;
		/*******
		 * JSR *
		 *******/
		case Opcodes.JSR: {
			final JSR op = new JSR(this.ops.size(), opcode, this.line);
			this.ops.add(op);
			op.setTargetPc(targetPc);
			if (targetPc < 0) {
				getUnresolved(label).add(op);
			}
			break;
		}
		default:
			LOGGER.warning("Unknown jump insn opcode '" + opcode + "'!");
		}
	}

	@Override
	public void visitLabel(final Label label) {
		final Integer pc = this.label2pc.put(label, this.ops.size());
		if (pc == null) {
			// fresh new label, never referenced before
			return;
		}
		if (pc > 0) {
			// visited before but is known?!
			LOGGER.warning("Label '" + label + "' is not unique, has old PC '" + this.ops.size()
					+ "'!");
			return;
		}
		// unknown and has forward reference
		for (final Object o : this.label2unresolved.get(label)) {
			if (o instanceof GOTO) {
				((GOTO) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof JCMP) {
				((JCMP) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof JCND) {
				((JCND) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof JSR) {
				((JSR) o).setTargetPc(this.ops.size());
				continue;
			}
			if (o instanceof SWITCH) {
				final SWITCH op = (SWITCH) o;
				if (pc == op.getDefaultPc()) {
					op.setDefaultPc(this.ops.size());
				}
				final int[] casePcs = op.getCasePcs();
				for (int i = casePcs.length; i-- > 0;) {
					if (pc == casePcs[i]) {
						casePcs[i] = this.ops.size();
					}
				}
				continue;
			}
			if (o instanceof Exc) {
				final Exc op = (Exc) o;
				if (pc == op.getStartPc()) {
					op.setStartPc(this.ops.size());
				}
				if (pc == op.getEndPc()) {
					op.setEndPc(this.ops.size());
				}
				if (pc == op.getHandlerPc()) {
					op.setHandlerPc(this.ops.size());
				}
			}
			if (o instanceof V) {
				final int[] pcs = ((V) o).getPcs();
				for (int i = pcs.length; i-- > 0;) {
					if (pc == pcs[i]) {
						pcs[i] = this.ops.size();
					}
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
			oValue = this.du.getDescT(((Type) cst).getDescriptor());
			t = this.du.getT(Class.class);
		} else {
			oValue = cst;
			if (cst instanceof Double) {
				t = T.DOUBLE;
			} else if (cst instanceof Float) {
				t = T.FLOAT;
			} else if (cst instanceof Integer) {
				t = T.getJvmIntT((Integer) oValue);
			} else if (cst instanceof Long) {
				t = T.LONG;
			} else if (cst instanceof String) {
				t = this.du.getT(String.class);
			} else {
				LOGGER.warning("Unknown ldc insn cst '" + cst + "'!");
			}
		}
		this.ops.add(new PUSH(this.ops.size(), Opcodes.LDC, this.line, t, oValue));
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		final int pc = getPc(start);
		if (pc < 0) {
			LOGGER.warning("Line number '" + line + "' start label '" + start + "' unknown yet?");
		}
		this.line = line;
	}

	@Override
	public void visitLocalVariable(final String name, final String desc, final String signature,
			final Label start, final Label end, final int index) {
		T vT = this.du.getDescT(desc);
		if (signature != null) {
			final T sigT = this.du.parseT(signature, new Cursor(), this.md.getM());
			if (!sigT.eraseTo(vT)) {
				LOGGER.info("Cannot reduce signature '" + signature + "' to type '" + vT
						+ "' for method (local variable '" + name + "') " + this.md);
			} else {
				vT = sigT;
			}
		}
		final int startPc = getPc(start);
		final int endPc = getPc(end);

		final V v = new V(vT, name, startPc, endPc);

		if (startPc < 0) {
			getUnresolved(start).add(v);
		}
		if (endPc < 0) {
			getUnresolved(end).add(v);
		}

		ArrayList<V> vs = this.reg2vs.get(index);
		if (vs == null) {
			vs = new ArrayList<V>();
			this.reg2vs.put(index, vs);
		}
		vs.add(v);
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] caseKeys, final Label[] labels) {
		final SWITCH op = new SWITCH(this.ops.size(), Opcodes.LOOKUPSWITCH, this.line);
		this.ops.add(op);
		// default
		int targetPc = getPc(dflt);
		op.setDefaultPc(targetPc);
		if (targetPc < 0) {
			getUnresolved(dflt).add(op);
		}
		// keys
		final int[] casePcs = new int[labels.length];
		for (int i = labels.length; i-- > 0;) {
			casePcs[i] = targetPc = getPc(labels[i]);
			if (targetPc < 0) {
				getUnresolved(labels[i]).add(op);
			}
		}
		op.setCaseKeys(caseKeys);
		op.setCasePcs(casePcs);
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name,
			final String desc) {
		switch (opcode) {
		/**********
		 * INVOKE *
		 **********/
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKESPECIAL:
			// Constructor or supermethod (any super) or private method callout.
		case Opcodes.INVOKESTATIC:
		case Opcodes.INVOKEVIRTUAL: {
			final T ownerT = this.du.getT(owner);
			final M invokeM = ownerT.getM(name, desc);
			if (opcode == Opcodes.INVOKEINTERFACE) {
				((ClassT) ownerT).markAf(AF.INTERFACE);
			}
			if (opcode == Opcodes.INVOKESTATIC) {
				invokeM.markAf(AF.STATIC);
			}
			this.ops.add(new INVOKE(this.ops.size(), opcode, this.line, invokeM,
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
		this.ops.add(new NEWARRAY(this.ops.size(), Opcodes.MULTIANEWARRAY, this.line, this.du
				.getDescT(desc), dims));
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc,
			final boolean visible) {
		A[] paramAs = null;
		if (this.paramAss == null) {
			this.paramAss = new A[parameter + 1][];
		} else if (parameter >= this.paramAss.length) {
			final A[][] newParamAss = new A[parameter + 1][];
			System.arraycopy(this.paramAss, 0, newParamAss, 0, this.paramAss.length);
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
		paramAs[paramAs.length - 1] = this.readAnnotationMemberVisitor.init(desc,
				visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max, final Label dflt,
			final Label... labels) {
		final SWITCH op = new SWITCH(this.ops.size(), Opcodes.TABLESWITCH, this.line);
		this.ops.add(op);
		// default
		int targetPc = getPc(dflt);
		op.setDefaultPc(targetPc);
		if (targetPc < 0) {
			getUnresolved(dflt).add(op);
		}
		// keys
		final int[] keys = new int[labels.length];
		final int[] keyTargets = new int[labels.length];
		for (int i = labels.length; i-- > 0;) {
			keys[i] = min + i;
			targetPc = getPc(labels[i]);
			keyTargets[i] = targetPc;
			if (targetPc < 0) {
				getUnresolved(labels[i]).add(op);
			}
		}
		op.setCaseKeys(keys);
		op.setCasePcs(keyTargets);
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end, final Label handler,
			final String type) {
		// type: java/lang/Exception
		final T catchT = type == null ? null : this.du.getT(type);
		final Exc exc = new Exc(catchT);

		int pc = getPc(start);
		exc.setStartPc(pc);
		if (pc < 0) {
			getUnresolved(start).add(exc);
		}
		pc = getPc(end);
		exc.setEndPc(pc);
		if (pc < 0) {
			getUnresolved(end).add(exc);
		}
		pc = getPc(handler);
		exc.setHandlerPc(pc);
		if (pc < 0) {
			getUnresolved(handler).add(exc);
		}

		this.excs.add(exc);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		final T t = this.du.getT(type);

		switch (opcode) {
		/********
		 * CAST *
		 ********/
		case Opcodes.CHECKCAST:
			this.ops.add(new CAST(this.ops.size(), opcode, this.line, T.REF, t));
			break;
		/**************
		 * INSTANCEOF *
		 **************/
		case Opcodes.INSTANCEOF:
			this.ops.add(new INSTANCEOF(this.ops.size(), opcode, this.line, t));
			break;
		/*******
		 * NEW *
		 *******/
		case Opcodes.NEW:
			this.ops.add(new NEW(this.ops.size(), opcode, this.line, t));
			break;
		/************
		 * NEWARRAY *
		 ************/
		case Opcodes.ANEWARRAY:
			this.ops.add(new NEWARRAY(this.ops.size(), opcode, this.line, t, 1));
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
			t = T.REF;
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
				t = T.AINT;
			}
			// fall through
		case Opcodes.LLOAD:
			if (t == null) {
				t = T.LONG;
			}
			this.ops.add(new LOAD(this.ops.size(), opcode, this.line, t, var));
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
				t = T.AINT;
			}
			// fall through
		case Opcodes.LSTORE:
			if (t == null) {
				t = T.LONG;
			}
			this.ops.add(new STORE(this.ops.size(), opcode, this.line, t, var));
			break;
		/*******
		 * RET *
		 *******/
		case Opcodes.RET: {
			this.ops.add(new RET(this.ops.size(), opcode, this.line, var));
			break;
		}
		default:
			LOGGER.warning("Unknown var insn opcode '" + opcode + "'!");
		}
	}

}