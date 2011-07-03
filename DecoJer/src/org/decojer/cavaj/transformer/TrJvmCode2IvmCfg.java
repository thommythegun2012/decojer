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
package org.decojer.cavaj.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.DataType;
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
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWAP;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;
import org.decojer.cavaj.tool.SignatureDecompiler;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

/**
 * Transform JVM Code to IVM CFG.
 * 
 * @author André Pankraz
 */
public class TrJvmCode2IvmCfg {

	public static void transform(final CFG cfg) {
		new TrJvmCode2IvmCfg(cfg).transform();
		cfg.calculatePostorder();
	}

	public static void transform(final TD td) {
		// no parallelism! 2 shared instance variables: code and nextPc
		final List<BD> bds = td.getBds();
		for (int i = 0; i < bds.size(); ++i) {
			final BD bd = bds.get(i);
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			if (cfg == null) {
				continue;
			}
			transform(cfg);
		}
	}

	private byte[] code;

	private final CFG cfg;

	private int pc;

	private TrJvmCode2IvmCfg(final CFG cfg) {
		this.cfg = cfg;
	}

	private Type classInfoType(final ConstPool constPool, final int cpClassIndex) {
		final String classInfo = constPool.getClassInfo(cpClassIndex);
		// javassist only replaces '/' with '.', no proper array handling for
		// cp arrays: "[L<classname>;" instead of "<classname>"
		if (classInfo.indexOf('[') == -1 && classInfo.indexOf(';') == -1) {
			return getAst().newSimpleType(getTd().newTypeName(classInfo));
		}
		return new SignatureDecompiler(getTd(), classInfo, null)
				.decompileType();
	}

	private AST getAst() {
		return getCu().getAst();
	}

	private CFG getCfg() {
		return this.cfg;
	}

	private CU getCu() {
		return getTd().getCu();
	}

	private MD getMd() {
		return getCfg().getMd();
	}

	private TD getTd() {
		return getMd().getTd();
	}

	int readSignedByte() {
		return this.code[this.pc++];
	}

	int readSignedInt() {
		return this.code[this.pc++] << 24 | (this.code[this.pc++] & 0xff) << 16
				| (this.code[this.pc++] & 0xff) << 8 | this.code[this.pc++]
				& 0xff;
	}

	int readSignedShort() {
		return this.code[this.pc++] << 8 | this.code[this.pc++] & 0xff;
	}

	int readUnsignedByte() {
		return this.code[this.pc++] & 0xff;
	}

	int readUnsignedInt() {
		return (this.code[this.pc++] & 0xff) << 24
				| (this.code[this.pc++] & 0xff) << 16
				| (this.code[this.pc++] & 0xff) << 8 | this.code[this.pc++]
				& 0xff;
	}

	int readUnsignedShort() {
		return (this.code[this.pc++] & 0xff) << 8 | this.code[this.pc++] & 0xff;
	}

	@SuppressWarnings("unchecked")
	private void transform() {
		this.cfg.init();

		final ConstPool constPool = this.cfg.getConstPool();
		this.code = this.cfg.getCode();

		// setup loop

		// start with pc = 0
		this.pc = 0;
		// start with this basic block, may not remain the start basic block
		// (splitting)
		BB bb = this.cfg.getStartBb();
		// remember visited pcs via BBNode
		final Map<Integer, BB> pcBbs = new HashMap<Integer, BB>();
		// remember open pcs
		final Stack<Integer> openPcs = new Stack<Integer>();

		// wide operation following?
		// one of: iload, fload, aload, lload, dload, istore, fstore, astore,
		// lstore, dstore, or ret
		boolean wide = false;

		while (true) {
			// next open pc?
			if (this.pc >= this.code.length) {
				if (openPcs.isEmpty()) {
					break;
				}
				this.pc = openPcs.pop();
				bb = pcBbs.get(this.pc);
			} else {
				// next pc allready in flow?
				final BB nextBB = this.cfg.getTargetBb(this.pc, pcBbs);
				if (nextBB != null) {
					bb.addSucc(nextBB, null);
					this.pc = this.code.length; // next open pc
					continue;
				}
				pcBbs.put(this.pc, bb);
			}
			final int opPc = this.pc;
			final int opCode = readUnsignedByte();
			final int opLine = this.cfg.lineNumberAttribute == null ? -1
					: this.cfg.lineNumberAttribute.toLineNumber(opPc);

			int type = -1;
			int iValue = Integer.MIN_VALUE;
			Object oValue = null;

			switch (opCode) {
			case Opcode.NOP:
				// nothing to do, ignore
				break;
			/*******
			 * ADD *
			 *******/
			case Opcode.DADD:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FADD:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IADD:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LADD:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new ADD(opPc, opCode, opLine, type));
				break;
			/*********
			 * ALOAD *
			 *********/
			case Opcode.AALOAD:
				type = DataType.T_AREF;
				// fall through
			case Opcode.BALOAD:
				if (type < 0) {
					type = DataType.T_BOOLEAN;
				}
				// fall through
			case Opcode.CALOAD:
				if (type < 0) {
					type = DataType.T_CHAR;
				}
				// fall through
			case Opcode.DALOAD:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FALOAD:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IALOAD:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LALOAD:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// fall through
			case Opcode.SALOAD:
				if (type < 0) {
					type = DataType.T_SHORT;
				}
				bb.addOperation(new ALOAD(opPc, opCode, opLine, type));
				break;
			/*******
			 * AND *
			 *******/
			case Opcode.IAND:
				type = DataType.T_INT;
				// fall through
			case Opcode.LAND:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new AND(opPc, opCode, opLine, type));
				break;
			/***************
			 * ARRAYLENGTH *
			 ***************/
			case Opcode.ARRAYLENGTH:
				bb.addOperation(new ARRAYLENGTH(opPc, opCode, opLine));
				break;
			/**********
			 * ASTORE *
			 **********/
			case Opcode.AASTORE:
				type = DataType.T_AREF;
				// fall through
			case Opcode.BASTORE:
				if (type < 0) {
					type = DataType.T_BOOLEAN;
				}
				// fall through
			case Opcode.CASTORE:
				if (type < 0) {
					type = DataType.T_CHAR;
				}
				// fall through
			case Opcode.DASTORE:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FASTORE:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IASTORE:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LASTORE:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// fall through
			case Opcode.SASTORE:
				if (type < 0) {
					type = DataType.T_SHORT;
				}
				bb.addOperation(new ASTORE(opPc, opCode, opLine, type));
				break;
			/***********
			 * CONVERT *
			 ***********/
			case Opcode.D2F:
				type = DataType.T_DOUBLE;
				iValue = DataType.T_FLOAT;
				// fall through
			case Opcode.D2I:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = DataType.T_INT;
				}
				// fall through
			case Opcode.D2L:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = DataType.T_LONG;
				}
				// fall through
			case Opcode.F2D:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.F2I:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = DataType.T_INT;
				}
				// fall through
			case Opcode.F2L:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = DataType.T_LONG;
				}
				// fall through
			case Opcode.I2B:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_BYTE;
				}
				// fall through
			case Opcode.I2C:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_CHAR;
				}
				// fall through
			case Opcode.I2D:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.I2F:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.I2L:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_LONG;
				}
				// fall through
			case Opcode.I2S:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_SHORT;
				}
				// fall through
			case Opcode.L2D:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.L2F:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.L2I:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = DataType.T_INT;
				}
				bb.addOperation(new CONVERT(opPc, opCode, opLine, type, iValue));
				break;
			/*******
			 * CMP *
			 *******/
			case Opcode.DCMPG:
				type = DataType.T_DOUBLE;
				iValue = CMP.T_G;
				// fall through
			case Opcode.DCMPL:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = CMP.T_L;
				}
				// fall through
			case Opcode.FCMPG:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = CMP.T_G;
				}
				// fall through
			case Opcode.FCMPL:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = CMP.T_L;
				}
				// fall through
			case Opcode.LCMP:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = CMP.T_0;
				}
				bb.addOperation(new CMP(opPc, opCode, opLine, type, iValue));
				break;
			/*******
			 * DIV *
			 *******/
			case Opcode.DDIV:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FDIV:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IDIV:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LDIV:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new DIV(opPc, opCode, opLine, type));
				break;
			/*******
			 * DUP *
			 *******/
			case Opcode.DUP:
				type = DUP.T_DUP;
				// fall through
			case Opcode.DUP_X1:
				if (type < 0) {
					type = DUP.T_DUP_X1;
				}
				// fall through
			case Opcode.DUP_X2:
				if (type < 0) {
					type = DUP.T_DUP_X2;
				}
				// fall through
			case Opcode.DUP2:
				if (type < 0) {
					type = DUP.T_DUP2;
				}
				// fall through
			case Opcode.DUP2_X1:
				if (type < 0) {
					type = DUP.T_DUP2_X1;
				}
				// fall through
			case Opcode.DUP2_X2:
				if (type < 0) {
					type = DUP.T_DUP2_X2;
				}
				bb.addOperation(new DUP(opPc, opCode, opLine, type));
				break;
			/*******
			 * GET *
			 *******/
			case Opcode.GETFIELD:
				type = GET.T_DYNAMIC;
				// fall through
			case Opcode.GETSTATIC:
				if (type < 0) {
					type = GET.T_STATIC;
				}
				{
					final int cpFieldIndex = readUnsignedShort();
					final String fieldrefClassName = constPool
							.getFieldrefClassName(cpFieldIndex);
					final String fieldrefName = constPool
							.getFieldrefName(cpFieldIndex);
					final String fieldrefType = constPool
							.getFieldrefType(cpFieldIndex);
					bb.addOperation(new GET(opPc, opCode, opLine, type,
							fieldrefClassName, fieldrefName, fieldrefType));
				}
				break;
			/********
			 * GOTO *
			 ********/
			case Opcode.GOTO:
				type = 0;
				iValue = readSignedShort();
				// fall through
			case Opcode.GOTO_W:
				if (type < 0) {
					iValue = readSignedInt();
				}
				// not really necessary, but important for
				// 1) correct opPc blocks
				// 2) line numbers
				bb.addOperation(new GOTO(opPc, opCode, opLine));
				this.pc = opPc + iValue;
				break;
			/*******
			 * INC *
			 *******/
			case Opcode.IINC: {
				final int varIndex = readUnsignedByte();
				final int constValue = readUnsignedByte();
				bb.addOperation(new INC(opPc, opCode, opLine, DataType.T_INT,
						varIndex, constValue));
			}
				break;
			/**********
			 * INVOKE *
			 **********/
			case Opcode.INVOKEINTERFACE: {
				type = INVOKE.T_INTERFACE;
				final int cpMethodIndex = readUnsignedShort();
				final int count = readUnsignedByte();
				final int reserved = readUnsignedByte();
				final String methodrefClassName = constPool
						.getInterfaceMethodrefClassName(cpMethodIndex);
				final String methodrefName = constPool
						.getInterfaceMethodrefName(cpMethodIndex);
				final String methodrefType = constPool
						.getInterfaceMethodrefType(cpMethodIndex);
				final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(
						getTd(), methodrefType, null);
				final List<Type> methodParameterTypes = signatureDecompiler
						.decompileMethodParameterTypes();
				final Type returnType = signatureDecompiler.decompileType();
				bb.addOperation(new INVOKE(opPc, opCode, opLine, type,
						methodrefClassName, methodrefName,
						methodParameterTypes, returnType));
			}
				break;
			case Opcode.INVOKESPECIAL:
				type = INVOKE.T_SPECIAL;
				// fall through
			case Opcode.INVOKESTATIC:
				if (type < 0) {
					type = INVOKE.T_STATIC;
				}
				// fall through
			case Opcode.INVOKEVIRTUAL:
				if (type < 0) {
					type = INVOKE.T_VIRTUAL;
				}
				{
					final int cpMethodIndex = readUnsignedShort();
					final String methodrefClassName = constPool
							.getMethodrefClassName(cpMethodIndex);
					final String methodrefName = constPool
							.getMethodrefName(cpMethodIndex);
					final String methodrefType = constPool
							.getMethodrefType(cpMethodIndex);
					final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(
							getTd(), methodrefType, null);
					final List<Type> methodParameterTypes = signatureDecompiler
							.decompileMethodParameterTypes();
					final Type returnType = signatureDecompiler.decompileType();
					bb.addOperation(new INVOKE(opPc, opCode, opLine, type,
							methodrefClassName, methodrefName,
							methodParameterTypes, returnType));
				}
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
				{
					final int branch = readSignedShort();
					bb.addOperation(new JCMP(opPc, opCode, opLine, type, iValue));
					final int targetPc = opPc + branch;
					if (targetPc == this.pc) {
						System.out.println("### BRANCH_IFCMP (Empty): "
								+ targetPc);
					} else {
						BB targetBB = this.cfg.getTargetBb(targetPc, pcBbs);
						if (targetBB == null) {
							targetBB = this.cfg.newBb(targetPc);
							pcBbs.put(targetPc, targetBB);
							openPcs.add(targetPc);
						}
						bb.addSucc(targetBB, Boolean.TRUE);
						BB nextBB = this.cfg.getTargetBb(this.pc, pcBbs);
						if (nextBB == null) {
							nextBB = this.cfg.newBb(this.pc);
						} else {
							this.pc = this.code.length; // next open pc
						}
						bb.addSucc(nextBB, Boolean.FALSE);
						bb = nextBB;
					}
				}
				break;
			/********
			 * JCND *
			 ********/
			case Opcode.IFNULL:
				type = DataType.T_AREF;
				iValue = CompareType.T_EQ;
				// fall through
			case Opcode.IFNONNULL:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = CompareType.T_NE;
				}
				// fall through
			case Opcode.IFEQ:
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
			case Opcode.IFGT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case Opcode.IFLE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case Opcode.IFLT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case Opcode.IFNE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_NE;
				}
				{
					final int branch = readSignedShort();
					bb.addOperation(new JCND(opPc, opCode, opLine, type, iValue));
					final int targetPc = opPc + branch;
					if (targetPc == this.pc) {
						System.out
								.println("### BRANCH_IF (Empty): " + targetPc);
					} else {
						BB targetBB = this.cfg.getTargetBb(targetPc, pcBbs);
						if (targetBB == null) {
							targetBB = this.cfg.newBb(targetPc);
							pcBbs.put(targetPc, targetBB);
							openPcs.add(targetPc);
						}
						bb.addSucc(targetBB, Boolean.TRUE);
						BB nextBB = this.cfg.getTargetBb(this.pc, pcBbs);
						if (nextBB == null) {
							nextBB = this.cfg.newBb(this.pc);
						} else {
							this.pc = this.code.length; // next open pc
						}
						bb.addSucc(nextBB, Boolean.FALSE);
						bb = nextBB;
					}
				}
				break;
			/*******
			 * JSR *
			 *******/
			case Opcode.JSR:
				type = 0;
				iValue = readUnsignedShort();
				// fall through
			case Opcode.JSR_W:
				if (type < 0) {
					iValue = readUnsignedInt();
				}
				// TODO
				System.out.println("### JSR: " + iValue + " : "
						+ (opPc + iValue));
				break;
			/********
			 * LOAD *
			 ********/
			case Opcode.ALOAD:
				type = DataType.T_AREF;
				// fall through
			case Opcode.DLOAD:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FLOAD:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.ILOAD:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LLOAD:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// for all above
				iValue = wide ? readUnsignedShort() : readUnsignedByte();
				// fall through
			case Opcode.ALOAD_0:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 0;
				}
				// fall through
			case Opcode.DLOAD_0:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 0;
				}
				// fall through
			case Opcode.FLOAD_0:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 0;
				}
				// fall through
			case Opcode.ILOAD_0:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 0;
				}
				// fall through
			case Opcode.LLOAD_0:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 0;
				}
				// fall through
			case Opcode.ALOAD_1:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 1;
				}
				// fall through
			case Opcode.DLOAD_1:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 1;
				}
				// fall through
			case Opcode.FLOAD_1:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 1;
				}
				// fall through
			case Opcode.ILOAD_1:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 1;
				}
				// fall through
			case Opcode.LLOAD_1:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 1;
				}
				// fall through
			case Opcode.ALOAD_2:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 2;
				}
				// fall through
			case Opcode.DLOAD_2:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 2;
				}
				// fall through
			case Opcode.FLOAD_2:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 2;
				}
				// fall through
			case Opcode.ILOAD_2:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 2;
				}
				// fall through
			case Opcode.LLOAD_2:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 2;
				}
				// fall through
			case Opcode.ALOAD_3:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 3;
				}
				// fall through
			case Opcode.DLOAD_3:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 3;
				}
				// fall through
			case Opcode.FLOAD_3:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 3;
				}
				// fall through
			case Opcode.ILOAD_3:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 3;
				}
				// fall through
			case Opcode.LLOAD_3:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 3;
				}
				bb.addOperation(new LOAD(opPc, opCode, opLine, type, iValue));
				break;
			/*******
			 * NEW *
			 *******/
			case Opcode.NEW: {
				final int cpClassIndex = readUnsignedShort();
				bb.addOperation(new NEW(opPc, opCode, opLine, classInfoType(
						constPool, cpClassIndex)));
			}
				break;
			/************
			 * NEWARRAY *
			 ************/
			case Opcode.ANEWARRAY: {
				final int cpClassIndex = readUnsignedShort();
				bb.addOperation(new NEWARRAY(opPc, opCode, opLine,
						classInfoType(constPool, cpClassIndex), 1));
			}
				break;
			case Opcode.NEWARRAY: {
				type = readUnsignedByte();
				final PrimitiveType.Code typeCode = new PrimitiveType.Code[] {
						null, null, null, null, PrimitiveType.BOOLEAN,
						PrimitiveType.CHAR, PrimitiveType.FLOAT,
						PrimitiveType.DOUBLE, PrimitiveType.BYTE,
						PrimitiveType.SHORT, PrimitiveType.INT,
						PrimitiveType.LONG }[type];
				bb.addOperation(new NEWARRAY(opPc, opCode, opLine, getAst()
						.newPrimitiveType(typeCode), 1));
			}
				break;
			case Opcode.MULTIANEWARRAY: {
				final int cpClassIndex = readUnsignedShort();
				final int dimensions = readUnsignedByte();
				bb.addOperation(new NEWARRAY(opPc, opCode, opLine,
						classInfoType(constPool, cpClassIndex), dimensions));
			}
				break;
			/********
			 * PUSH *
			 ********/
			case Opcode.ACONST_NULL:
				type = DataType.T_AREF;
				oValue = null;
				// fall through
			case Opcode.BIPUSH:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = readSignedByte();
				}
				// fall through
			case Opcode.SIPUSH:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = readSignedShort();
				}
				// fall through
			case Opcode.LDC:
				if (type < 0) {
					final int ldcValueIndex = readUnsignedByte();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						type = DataType.T_CLASS;
						oValue = classInfoType(constPool, ldcValueIndex);
						break;
					case ConstPool.CONST_Double:
						// Double / Long only with LDC2_W, but is OK here too
						type = DataType.T_DOUBLE;
						// fall through
					case ConstPool.CONST_Float:
						if (type < 0) {
							type = DataType.T_FLOAT;
						}
						// fall through
					case ConstPool.CONST_Integer:
						if (type < 0) {
							type = DataType.T_INT;
						}
						// fall through
					case ConstPool.CONST_Long:
						// Double / Long only with LDC2_W, but is OK here too
						if (type < 0) {
							type = DataType.T_LONG;
						}
						// fall through
					case ConstPool.CONST_String:
						if (type < 0) {
							type = DataType.T_STRING;
						}
						oValue = constPool.getLdcValue(ldcValueIndex);
						break;
					default:
						throw new RuntimeException("Unknown Const Pool Tag "
								+ tag + " for LDC!");
					}
				}
				// fall through
			case Opcode.LDC_W:
				// fall through
			case Opcode.LDC2_W:
				if (type < 0) {
					final int ldcValueIndex = readUnsignedShort();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						type = DataType.T_CLASS;
						oValue = classInfoType(constPool, ldcValueIndex);
						break;
					case ConstPool.CONST_Double:
						type = DataType.T_DOUBLE;
						// fall through
					case ConstPool.CONST_Float:
						if (type < 0) {
							type = DataType.T_FLOAT;
						}
						// fall through
					case ConstPool.CONST_Integer:
						if (type < 0) {
							type = DataType.T_INT;
						}
						// fall through
					case ConstPool.CONST_Long:
						if (type < 0) {
							type = DataType.T_LONG;
						}
						// fall through
					case ConstPool.CONST_String:
						if (type < 0) {
							type = DataType.T_STRING;
						}
						oValue = constPool.getLdcValue(ldcValueIndex);
						break;
					default:
						throw new RuntimeException("Unknown Const Pool Tag "
								+ tag + " for LDC!");
					}
				}
				// fall through
			case Opcode.DCONST_0:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					oValue = 0D;
				}
				// fall through
			case Opcode.FCONST_0:
				if (type < 0) {
					type = DataType.T_FLOAT;
					oValue = 0;
				}
				// fall through
			case Opcode.ICONST_0:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 0;
				}
				// fall through
			case Opcode.LCONST_0:
				if (type < 0) {
					type = DataType.T_LONG;
					oValue = 0L;
				}
				// fall through
			case Opcode.DCONST_1:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					oValue = 1D;
				}
				// fall through
			case Opcode.FCONST_1:
				if (type < 0) {
					type = DataType.T_FLOAT;
					oValue = 1;
				}
				// fall through
			case Opcode.ICONST_1:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 1;
				}
				// fall through
			case Opcode.LCONST_1:
				if (type < 0) {
					type = DataType.T_LONG;
					oValue = 1L;
				}
				// fall through
			case Opcode.FCONST_2:
				if (type < 0) {
					type = DataType.T_FLOAT;
					oValue = 2;
				}
				// fall through
			case Opcode.ICONST_2:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 2;
				}
				// fall through
			case Opcode.ICONST_3:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 3;
				}
				// fall through
			case Opcode.ICONST_4:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 4;
				}
				// fall through
			case Opcode.ICONST_5:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 5;
				}
				// fall through
			case Opcode.ICONST_M1:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = -1;
				}
				bb.addOperation(new PUSH(opPc, opCode, opLine, type, oValue));
				break;
			/*******
			 * PUT *
			 *******/
			case Opcode.PUTFIELD:
				type = PUT.T_DYNAMIC;
				// fall through
			case Opcode.PUTSTATIC:
				if (type < 0) {
					type = PUT.T_STATIC;
				}
				{
					final int cpFieldIndex = readUnsignedShort();
					final String fieldrefClassName = constPool
							.getFieldrefClassName(cpFieldIndex);
					final String fieldrefName = constPool
							.getFieldrefName(cpFieldIndex);
					final String fieldrefType = constPool
							.getFieldrefType(cpFieldIndex);
					bb.addOperation(new PUT(opPc, opCode, opLine, type,
							fieldrefClassName, fieldrefName, fieldrefType));
				}
				break;
			/*******
			 * RET *
			 *******/
			case Opcode.RET: {
				final int varIndex = wide ? readUnsignedShort()
						: readUnsignedByte();
				bb.addOperation(new RET(opPc, opCode, opLine, varIndex));
				this.pc = this.code.length; // next open pc
			}
				break;
			/**********
			 * RETURN *
			 **********/
			case Opcode.ARETURN:
				type = DataType.T_AREF;
				// fall through
			case Opcode.DRETURN:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FRETURN:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IRETURN:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LRETURN:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// fall through
			case Opcode.RETURN:
				if (type < 0) {
					type = DataType.T_VOID;
				}
				bb.addOperation(new RETURN(opPc, opCode, opLine, type));
				this.pc = this.code.length; // next open pc
				break;
			/*********
			 * STORE *
			 *********/
			case Opcode.ASTORE:
				type = DataType.T_AREF;
				// fall through
			case Opcode.DSTORE:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FSTORE:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.ISTORE:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LSTORE:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// for all above
				iValue = wide ? readUnsignedShort() : readUnsignedByte();
				// fall through
			case Opcode.ASTORE_0:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 0;
				}
				// fall through
			case Opcode.DSTORE_0:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 0;
				}
				// fall through
			case Opcode.FSTORE_0:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 0;
				}
				// fall through
			case Opcode.ISTORE_0:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 0;
				}
				// fall through
			case Opcode.LSTORE_0:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 0;
				}
				// fall through
			case Opcode.ASTORE_1:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 1;
				}
				// fall through
			case Opcode.DSTORE_1:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 1;
				}
				// fall through
			case Opcode.FSTORE_1:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 1;
				}
				// fall through
			case Opcode.ISTORE_1:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 1;
				}
				// fall through
			case Opcode.LSTORE_1:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 1;
				}
				// fall through
			case Opcode.ASTORE_2:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 2;
				}
				// fall through
			case Opcode.DSTORE_2:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 2;
				}
				// fall through
			case Opcode.FSTORE_2:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 2;
				}
				// fall through
			case Opcode.ISTORE_2:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 2;
				}
				// fall through
			case Opcode.LSTORE_2:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 2;
				}
				// fall through
			case Opcode.ASTORE_3:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 3;
				}
				// fall through
			case Opcode.DSTORE_3:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 3;
				}
				// fall through
			case Opcode.FSTORE_3:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 3;
				}
				// fall through
			case Opcode.ISTORE_3:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 3;
				}
				// fall through
			case Opcode.LSTORE_3:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 3;
				}
				bb.addOperation(new STORE(opPc, opCode, opLine, type, iValue));
				break;
			/*********
			 * THROW *
			 *********/
			case Opcode.ATHROW:
				bb.addOperation(new THROW(opPc, opCode, opLine));
				// next open pc
				this.pc = this.code.length; // next open pc
				break;
			/**************
			 * CHECKCCAST *
			 **************/
			case Opcode.CHECKCAST: {
				final int cpClassIndex = readUnsignedShort();
				// cp arrays: "[L<classname>;" instead of "<classname>"!!!
				bb.addOperation(new CHECKCAST(opPc, opCode, opLine,
						classInfoType(constPool, cpClassIndex)));
			}
				break;
			/*******
			 * MUL *
			 *******/
			case Opcode.DMUL:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FMUL:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IMUL:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LMUL:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new MUL(opPc, opCode, opLine, type));
				break;
			/*******
			 * NEG *
			 *******/
			case Opcode.DNEG:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FNEG:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.INEG:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LNEG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new NEG(opPc, opCode, opLine, type));
				break;
			/*******
			 * REM *
			 *******/
			case Opcode.DREM:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FREM:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IREM:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LREM:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new REM(opPc, opCode, opLine, type));
				break;
			/*******
			 * SUB *
			 *******/
			case Opcode.DSUB:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FSUB:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.ISUB:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LSUB:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new SUB(opPc, opCode, opLine, type));
				break;
			// *** INSTANCEOF ***
			case Opcode.INSTANCEOF: {
				final int cpClassIndex = readUnsignedShort();
				bb.addOperation(new INSTANCEOF(opPc, opCode, opLine,
						classInfoType(constPool, cpClassIndex)));
			}
				break;
			/******
			 * OR *
			 ******/
			case Opcode.IOR:
				type = DataType.T_INT;
				// fall through
			case Opcode.LOR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new OR(opPc, opCode, opLine, type));
				break;
			case Opcode.ISHL:
				// fall through
			case Opcode.LSHL:
				break;
			case Opcode.ISHR:
				// fall through
			case Opcode.LSHR:
				break;
			case Opcode.IUSHR:
				// fall through
			case Opcode.LUSHR:
				break;
			/*******
			 * XOR *
			 *******/
			case Opcode.IXOR:
				type = DataType.T_INT;
				// fall through
			case Opcode.LXOR: {
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new XOR(opPc, opCode, opLine, type));
			}
				break;
			/**********
			 * SWITCH *
			 **********/
			case Opcode.LOOKUPSWITCH: {
				// align
				if (this.pc % 4 > 0) {
					this.pc += 4 - this.pc % 4;
				}
				// defaultBranch
				final int defaultBranch = readUnsignedInt();
				// map entries number
				final int npairs = readUnsignedInt();
				// case pc -> values
				final HashMap<Integer, List<Integer>> casePc2Values = new HashMap<Integer, List<Integer>>();
				for (int i = 0; i < npairs; ++i) {
					final int caseValue = readUnsignedInt();
					final int caseBranch = readUnsignedInt();
					List<Integer> values = casePc2Values.get(caseBranch);
					if (values == null) {
						values = new ArrayList<Integer>();
						casePc2Values.put(caseBranch, values);
					}
					values.add(caseValue);
				}
				// add default branch, can overlay with other cases, even JDK 6
				// doesn't optimize this
				List<Integer> values = casePc2Values.get(defaultBranch);
				if (values == null) {
					values = new ArrayList<Integer>();
					casePc2Values.put(defaultBranch, values);
				}
				values.add(null);
				oValue = casePc2Values;
			}
			// fall through
			case Opcode.TABLESWITCH: {
				if (oValue == null) {
					// align
					if (this.pc % 4 > 0) {
						this.pc += 4 - this.pc % 4;
					}
					// defaultBranch
					final int defaultBranch = readUnsignedInt();
					// map key boundaries
					final int caseLow = readUnsignedInt();
					final int caseHigh = readUnsignedInt();
					// case pc -> values
					final HashMap<Integer, List<Integer>> casePc2Values = new HashMap<Integer, List<Integer>>();
					for (int caseValue = caseLow; caseValue <= caseHigh; ++caseValue) {
						final int caseBranch = readUnsignedInt();
						List<Integer> values = casePc2Values.get(caseBranch);
						if (values == null) {
							values = new ArrayList<Integer>();
							casePc2Values.put(caseBranch, values);
						}
						values.add(caseValue);
					}
					// add default branch, can overlay with other cases, even
					// JDK 6
					// doesn't optimize this
					List<Integer> values = casePc2Values.get(defaultBranch);
					if (values == null) {
						values = new ArrayList<Integer>();
						casePc2Values.put(defaultBranch, values);
					}
					values.add(null);
					oValue = casePc2Values;
				}
				// case pc -> values
				final HashMap<Integer, List<Integer>> casePc2Values = (HashMap<Integer, List<Integer>>) oValue;
				for (final Map.Entry<Integer, List<Integer>> casePc2ValuesEntry : casePc2Values
						.entrySet()) {
					final int caseBranch = casePc2ValuesEntry.getKey();
					final List<Integer> values = casePc2ValuesEntry.getValue();
					final int casePc = opPc + caseBranch;

					BB caseBb = this.cfg.getTargetBb(casePc, pcBbs);
					if (caseBb == null) {
						caseBb = this.cfg.newBb(casePc);
						pcBbs.put(casePc, caseBb);
						openPcs.add(casePc);
					}
					bb.addSucc(caseBb, values);
				}
				bb.addOperation(new SWITCH(opPc, opCode, opLine));
				// next open pc
				this.pc = this.code.length; // next open pc
			}
				break;
			/***********
			 * MONITOR *
			 ***********/
			case Opcode.MONITORENTER:
				type = MONITOR.T_ENTER;
				// fall through
			case Opcode.MONITOREXIT:
				if (type < 0) {
					type = MONITOR.T_EXIT;
				}
				bb.addOperation(new MONITOR(opPc, opCode, opLine, type));
				break;
			/*******
			 * POP *
			 *******/
			case Opcode.POP:
				type = POP.T_POP;
				// fall through
			case Opcode.POP2:
				if (type < 0) {
					type = POP.T_POP2;
				}
				bb.addOperation(new POP(opPc, opCode, opLine, type));
				break;
			/********
			 * SWAP *
			 ********/
			case Opcode.SWAP:
				bb.addOperation(new SWAP(opPc, opCode, opLine));
				break;
			/*******
			 * WIDE *
			 *******/
			case Opcode.WIDE:
				wide = true;
				// just for once! reset wide after switch
				continue;
			default:
				throw new RuntimeException("Unknown jvm operation code '"
						+ opCode + "'!");
			}
			// reset wide
			wide = false;
		}
	}

}