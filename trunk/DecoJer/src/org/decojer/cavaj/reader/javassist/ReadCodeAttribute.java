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
package org.decojer.cavaj.reader.javassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javassist.bytecode.AttributeInfo;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.Opcode;
import javassist.bytecode.StackMap;
import javassist.bytecode.StackMapTable;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.op.ADD;
import org.decojer.cavaj.model.code.op.ALOAD;
import org.decojer.cavaj.model.code.op.AND;
import org.decojer.cavaj.model.code.op.ARRAYLENGTH;
import org.decojer.cavaj.model.code.op.ASTORE;
import org.decojer.cavaj.model.code.op.CAST;
import org.decojer.cavaj.model.code.op.CMP;
import org.decojer.cavaj.model.code.op.CmpType;
import org.decojer.cavaj.model.code.op.DIV;
import org.decojer.cavaj.model.code.op.DUP;
import org.decojer.cavaj.model.code.op.GET;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.INC;
import org.decojer.cavaj.model.code.op.INSTANCEOF;
import org.decojer.cavaj.model.code.op.INVOKE;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.JSR;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.MONITOR;
import org.decojer.cavaj.model.code.op.MUL;
import org.decojer.cavaj.model.code.op.NEG;
import org.decojer.cavaj.model.code.op.NEW;
import org.decojer.cavaj.model.code.op.NEWARRAY;
import org.decojer.cavaj.model.code.op.OR;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.POP;
import org.decojer.cavaj.model.code.op.PUSH;
import org.decojer.cavaj.model.code.op.PUT;
import org.decojer.cavaj.model.code.op.REM;
import org.decojer.cavaj.model.code.op.RET;
import org.decojer.cavaj.model.code.op.RETURN;
import org.decojer.cavaj.model.code.op.SHL;
import org.decojer.cavaj.model.code.op.SHR;
import org.decojer.cavaj.model.code.op.STORE;
import org.decojer.cavaj.model.code.op.SUB;
import org.decojer.cavaj.model.code.op.SWAP;
import org.decojer.cavaj.model.code.op.SWITCH;
import org.decojer.cavaj.model.code.op.THROW;
import org.decojer.cavaj.model.code.op.XOR;

/**
 * Javassist read code attribute.
 * 
 * @author André Pankraz
 */
public class ReadCodeAttribute {

	private final static Logger LOGGER = Logger.getLogger(ReadCodeAttribute.class.getName());

	private final DU du;

	final ArrayList<Op> ops = new ArrayList<Op>();

	private final HashMap<Integer, Integer> vmpc2pc = new HashMap<Integer, Integer>();

	private final HashMap<Integer, ArrayList<Object>> vmpc2unresolved = new HashMap<Integer, ArrayList<Object>>();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadCodeAttribute(final DU du) {
		assert du != null;

		this.du = du;
	}

	private int getPc(final int vmpc) {
		final Integer pc = this.vmpc2pc.get(vmpc);
		if (pc != null) {
			return pc;
		}
		final int unresolvedPc = -1 - this.vmpc2unresolved.size();
		this.vmpc2pc.put(vmpc, unresolvedPc);
		return unresolvedPc;
	}

	private ArrayList<Object> getUnresolved(final int vmpc) {
		ArrayList<Object> unresolved = this.vmpc2unresolved.get(vmpc);
		if (unresolved == null) {
			unresolved = new ArrayList<Object>();
			this.vmpc2unresolved.put(vmpc, unresolved);
		}
		return unresolved;
	}

	/**
	 * Init and visit.
	 * 
	 * @param md
	 *            method declaration
	 * @param codeAttribute
	 *            Javassist code attribute
	 */
	@SuppressWarnings("unchecked")
	public void initAndVisit(final MD md, final CodeAttribute codeAttribute) {
		this.ops.clear();
		this.vmpc2pc.clear();
		this.vmpc2unresolved.clear();

		// read all code attributes
		LineNumberAttribute lineNumberAttribute = null;
		// contains names and descriptors
		LocalVariableAttribute localVariableAttribute = null;
		// contains signatures, names are same
		LocalVariableAttribute localVariableTypeAttribute = null;
		StackMap stackMap = null;
		StackMapTable stackMapTable = null;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) codeAttribute
				.getAttributes()) {
			final String attributeTag = attributeInfo.getName();
			if (LineNumberAttribute.tag.equals(attributeTag)) {
				lineNumberAttribute = (LineNumberAttribute) attributeInfo;
			} else if (LocalVariableAttribute.tag.equals(attributeTag)) {
				localVariableAttribute = (LocalVariableAttribute) attributeInfo;
			} else if (LocalVariableAttribute.typeTag.equals(attributeTag)) {
				localVariableTypeAttribute = (LocalVariableAttribute) attributeInfo;
			} else if (StackMap.tag.equals(attributeTag)) {
				stackMap = (StackMap) attributeInfo;
			} else if (StackMapTable.tag.equals(attributeTag)) {
				stackMapTable = (StackMapTable) attributeInfo;
			} else {
				LOGGER.warning("Unknown code attribute tag '" + attributeTag + "' in '" + md + "'!");
			}
		}
		LOGGER.fine("Stack info: " + stackMap + " : " + stackMapTable);

		final CFG cfg = new CFG(md, codeAttribute.getMaxLocals(), codeAttribute.getMaxStack());
		md.setCFG(cfg);

		final CodeReader codeReader = new CodeReader(codeAttribute.getCode());
		final ConstPool constPool = codeAttribute.getConstPool();

		// wide operation following? one of:
		// iload, fload, aload, lload, dload,
		// istore, fstore, astore, lstore, dstore,
		// ret, iinc
		boolean wide = false;

		while (codeReader.isNext()) {
			final int vmpc = codeReader.pc;

			visitVmpc(vmpc);
			final int line = lineNumberAttribute == null ? -1 : lineNumberAttribute
					.toLineNumber(vmpc);
			final int opcode = codeReader.readUnsignedByte();

			T t = null;
			int type = -1;
			int iValue = Integer.MIN_VALUE;
			Object oValue = null;

			switch (opcode) {
			/*******
			 * ADD *
			 *******/
			case Opcode.DADD:
				t = T.DOUBLE;
				// fall through
			case Opcode.FADD:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IADD:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LADD:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new ADD(this.ops.size(), opcode, line, t));
				break;
			/*********
			 * ALOAD *
			 *********/
			case Opcode.AALOAD:
				t = T.AREF;
				// fall through
			case Opcode.BALOAD:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case Opcode.CALOAD:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case Opcode.DALOAD:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FALOAD:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IALOAD:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LALOAD:
				if (t == null) {
					t = T.LONG;
				}
				// fall through
			case Opcode.SALOAD:
				if (t == null) {
					t = T.SHORT;
				}
				this.ops.add(new ALOAD(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * AND *
			 *******/
			case Opcode.IAND:
				t = T.INT;
				// fall through
			case Opcode.LAND:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new AND(this.ops.size(), opcode, line, t));
				break;
			/***************
			 * ARRAYLENGTH *
			 ***************/
			case Opcode.ARRAYLENGTH:
				this.ops.add(new ARRAYLENGTH(this.ops.size(), opcode, line));
				break;
			/**********
			 * ASTORE *
			 **********/
			case Opcode.AASTORE:
				t = T.AREF;
				// fall through
			case Opcode.BASTORE:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case Opcode.CASTORE:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case Opcode.DASTORE:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FASTORE:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IASTORE:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LASTORE:
				if (t == null) {
					t = T.LONG;
				}
				// fall through
			case Opcode.SASTORE:
				if (t == null) {
					t = T.SHORT;
				}
				this.ops.add(new ASTORE(this.ops.size(), opcode, line, t));
				break;
			/********
			 * CAST *
			 ********/
			case Opcode.CHECKCAST:
				t = T.AREF;
				oValue = this.du.getT(constPool.getClassInfo(codeReader.readUnsignedShort()));
				// fall through
			case Opcode.D2F:
				if (t == null) {
					t = T.DOUBLE;
					oValue = T.FLOAT;
				}
				// fall through
			case Opcode.D2I:
				if (t == null) {
					t = T.DOUBLE;
					oValue = T.INT;
				}
				// fall through
			case Opcode.D2L:
				if (t == null) {
					t = T.DOUBLE;
					oValue = T.LONG;
				}
				// fall through
			case Opcode.F2D:
				if (t == null) {
					t = T.FLOAT;
					oValue = T.DOUBLE;
				}
				// fall through
			case Opcode.F2I:
				if (t == null) {
					t = T.FLOAT;
					oValue = T.INT;
				}
				// fall through
			case Opcode.F2L:
				if (t == null) {
					t = T.FLOAT;
					oValue = T.LONG;
				}
				// fall through
			case Opcode.I2B:
				if (t == null) {
					t = T.INT;
					oValue = T.BYTE;
				}
				// fall through
			case Opcode.I2C:
				if (t == null) {
					t = T.INT;
					oValue = T.CHAR;
				}
				// fall through
			case Opcode.I2D:
				if (t == null) {
					t = T.INT;
					oValue = T.DOUBLE;
				}
				// fall through
			case Opcode.I2F:
				if (t == null) {
					t = T.INT;
					oValue = T.FLOAT;
				}
				// fall through
			case Opcode.I2L:
				if (t == null) {
					t = T.INT;
					oValue = T.LONG;
				}
				// fall through
			case Opcode.I2S:
				if (t == null) {
					t = T.INT;
					oValue = T.SHORT;
				}
				// fall through
			case Opcode.L2D:
				if (t == null) {
					t = T.LONG;
					oValue = T.DOUBLE;
				}
				// fall through
			case Opcode.L2F:
				if (t == null) {
					t = T.LONG;
					oValue = T.FLOAT;
				}
				// fall through
			case Opcode.L2I:
				if (t == null) {
					t = T.LONG;
					oValue = T.INT;
				}
				this.ops.add(new CAST(this.ops.size(), opcode, line, t, (T) oValue));
				break;
			/*******
			 * CMP *
			 *******/
			case Opcode.DCMPG:
				t = T.DOUBLE;
				iValue = CMP.T_G;
				// fall through
			case Opcode.DCMPL:
				if (t == null) {
					t = T.DOUBLE;
					iValue = CMP.T_L;
				}
				// fall through
			case Opcode.FCMPG:
				if (t == null) {
					t = T.FLOAT;
					iValue = CMP.T_G;
				}
				// fall through
			case Opcode.FCMPL:
				if (t == null) {
					t = T.FLOAT;
					iValue = CMP.T_L;
				}
				// fall through
			case Opcode.LCMP:
				if (t == null) {
					t = T.LONG;
					iValue = CMP.T_0;
				}
				this.ops.add(new CMP(this.ops.size(), opcode, line, t, iValue));
				break;
			/*******
			 * DIV *
			 *******/
			case Opcode.DDIV:
				t = T.DOUBLE;
				// fall through
			case Opcode.FDIV:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IDIV:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LDIV:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new DIV(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * DUP *
			 *******/
			case Opcode.DUP:
				oValue = DUP.Kind.DUP;
				// fall through
			case Opcode.DUP_X1:
				if (oValue == null) {
					oValue = DUP.Kind.DUP_X1;
				}
				// fall through
			case Opcode.DUP_X2:
				if (oValue == null) {
					oValue = DUP.Kind.DUP_X2;
				}
				// fall through
			case Opcode.DUP2:
				if (oValue == null) {
					oValue = DUP.Kind.DUP2;
				}
				// fall through
			case Opcode.DUP2_X1:
				if (oValue == null) {
					oValue = DUP.Kind.DUP2_X1;
				}
				// fall through
			case Opcode.DUP2_X2:
				if (oValue == null) {
					oValue = DUP.Kind.DUP2_X2;
				}
				this.ops.add(new DUP(this.ops.size(), opcode, line, (DUP.Kind) oValue));
				break;
			/*******
			 * GET *
			 *******/
			case Opcode.GETFIELD:
			case Opcode.GETSTATIC: {
				final int fieldIndex = codeReader.readUnsignedShort();

				t = this.du.getT(constPool.getFieldrefClassName(fieldIndex));
				final F f = t.getF(constPool.getFieldrefName(fieldIndex),
						this.du.getDescT(constPool.getFieldrefType(fieldIndex)));
				if (opcode == Opcode.GETSTATIC) {
					f.markAf(AF.STATIC);
				}
				this.ops.add(new GET(this.ops.size(), opcode, line, f));
				break;
			}
			/********
			 * GOTO *
			 ********/
			case Opcode.GOTO:
				type = 0;
				iValue = codeReader.readSignedShort();
				// fall through
			case Opcode.GOTO_W:
				if (type == -1) {
					iValue = codeReader.readSignedInt();
				}
				{
					final GOTO op = new GOTO(this.ops.size(), opcode, line);
					this.ops.add(op);
					final int targetVmpc = vmpc + iValue;
					final int targetPc = getPc(targetVmpc);
					op.setTargetPc(targetPc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				break;
			/*******
			 * INC *
			 *******/
			case Opcode.IINC: {
				final int varIndex = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
				final int constValue = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
				this.ops.add(new INC(this.ops.size(), opcode, line, T.INT, varIndex, constValue));
				break;
			}
			/**************
			 * INSTANCEOF *
			 **************/
			case Opcode.INSTANCEOF:
				this.ops.add(new INSTANCEOF(this.ops.size(), opcode, line, this.du.getT(constPool
						.getClassInfo(codeReader.readUnsignedShort()))));
				break;
			/**********
			 * INVOKE *
			 **********/
			case Opcode.INVOKEINTERFACE: {
				// interface method callout
				final int methodIndex = codeReader.readUnsignedShort();
				codeReader.readUnsignedByte(); // count, unused
				codeReader.readUnsignedByte(); // reserved, unused

				t = this.du.getT(constPool.getInterfaceMethodrefClassName(methodIndex));
				t.markAf(AF.INTERFACE);
				final M m = t.getM(constPool.getInterfaceMethodrefName(methodIndex),
						constPool.getInterfaceMethodrefType(methodIndex));
				this.ops.add(new INVOKE(this.ops.size(), opcode, line, m, false));
				break;
			}
			case Opcode.INVOKESPECIAL:
				// constructor or supermethod callout
			case Opcode.INVOKEVIRTUAL:
			case Opcode.INVOKESTATIC: {
				final int cpMethodIndex = codeReader.readUnsignedShort();

				t = this.du.getT(constPool.getMethodrefClassName(cpMethodIndex));
				final M m = t.getM(constPool.getMethodrefName(cpMethodIndex),
						constPool.getMethodrefType(cpMethodIndex));
				if (opcode == Opcode.INVOKESTATIC) {
					m.markAf(AF.STATIC);
				}
				this.ops.add(new INVOKE(this.ops.size(), opcode, line, m,
						opcode == Opcode.INVOKESPECIAL));
				break;
			}
			/********
			 * JCMP *
			 ********/
			case Opcode.IF_ACMPEQ:
				t = T.AREF;
				oValue = CmpType.T_EQ;
				// fall through
			case Opcode.IF_ACMPNE:
				if (t == null) {
					t = T.AREF;
					oValue = CmpType.T_NE;
				}
				// fall through
			case Opcode.IF_ICMPEQ:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_EQ;
				}
				// fall through
			case Opcode.IF_ICMPGE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GE;
				}
				// fall through
			case Opcode.IF_ICMPGT:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GT;
				}
				// fall through
			case Opcode.IF_ICMPLE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LE;
				}
				// fall through
			case Opcode.IF_ICMPLT:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LT;
				}
				// fall through
			case Opcode.IF_ICMPNE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_NE;
				}
				{
					final JCMP op = new JCMP(this.ops.size(), opcode, line, t, (CmpType) oValue);
					this.ops.add(op);
					final int targetVmpc = vmpc + codeReader.readSignedShort();
					final int targetPc = getPc(targetVmpc);
					op.setTargetPc(targetPc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				break;
			/********
			 * JCND *
			 ********/
			case Opcode.IFNULL:
				t = T.AREF;
				oValue = CmpType.T_EQ;
				// fall through
			case Opcode.IFNONNULL:
				if (t == null) {
					t = T.AREF;
					oValue = CmpType.T_NE;
				}
				// fall through
			case Opcode.IFEQ:
				if (t == null) {
					t = T.RINT; // boolean too
					oValue = CmpType.T_EQ;
				}
				// fall through
			case Opcode.IFGE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GE;
				}
				// fall through
			case Opcode.IFGT:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GT;
				}
				// fall through
			case Opcode.IFLE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LE;
				}
				// fall through
			case Opcode.IFLT:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LT;
				}
				// fall through
			case Opcode.IFNE:
				if (t == null) {
					t = T.RINT; // boolean too
					oValue = CmpType.T_NE;
				}
				{
					final JCND op = new JCND(this.ops.size(), opcode, line, t, (CmpType) oValue);
					this.ops.add(op);
					final int targetVmpc = vmpc + codeReader.readSignedShort();
					final int targetPc = getPc(targetVmpc);
					op.setTargetPc(targetPc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				break;
			/*******
			 * JSR *
			 *******/
			case Opcode.JSR:
				type = 0;
				iValue = codeReader.readUnsignedShort();
				// fall through
			case Opcode.JSR_W:
				if (type == -1) {
					iValue = codeReader.readUnsignedInt();
				}
				{
					final JSR op = new JSR(this.ops.size(), opcode, line);
					this.ops.add(op);
					final int targetVmpc = vmpc + iValue;
					final int targetPc = getPc(targetVmpc);
					op.setTargetPc(targetPc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				break;
			/********
			 * LOAD *
			 ********/
			case Opcode.ALOAD:
				t = T.AREF;
				// fall through
			case Opcode.DLOAD:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FLOAD:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.ILOAD:
				if (t == null) {
					t = T.RINT;
				}
				// fall through
			case Opcode.LLOAD:
				if (t == null) {
					t = T.LONG;
				}
				iValue = wide ? codeReader.readUnsignedShort() : codeReader.readUnsignedByte();
				// fall through
			case Opcode.ALOAD_0:
				if (t == null) {
					t = T.AREF;
					iValue = 0;
				}
				// fall through
			case Opcode.DLOAD_0:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 0;
				}
				// fall through
			case Opcode.FLOAD_0:
				if (t == null) {
					t = T.FLOAT;
					iValue = 0;
				}
				// fall through
			case Opcode.ILOAD_0:
				if (t == null) {
					t = T.RINT;
					iValue = 0;
				}
				// fall through
			case Opcode.LLOAD_0:
				if (t == null) {
					t = T.LONG;
					iValue = 0;
				}
				// fall through
			case Opcode.ALOAD_1:
				if (t == null) {
					t = T.AREF;
					iValue = 1;
				}
				// fall through
			case Opcode.DLOAD_1:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 1;
				}
				// fall through
			case Opcode.FLOAD_1:
				if (t == null) {
					t = T.FLOAT;
					iValue = 1;
				}
				// fall through
			case Opcode.ILOAD_1:
				if (t == null) {
					t = T.RINT;
					iValue = 1;
				}
				// fall through
			case Opcode.LLOAD_1:
				if (t == null) {
					t = T.LONG;
					iValue = 1;
				}
				// fall through
			case Opcode.ALOAD_2:
				if (t == null) {
					t = T.AREF;
					iValue = 2;
				}
				// fall through
			case Opcode.DLOAD_2:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 2;
				}
				// fall through
			case Opcode.FLOAD_2:
				if (t == null) {
					t = T.FLOAT;
					iValue = 2;
				}
				// fall through
			case Opcode.ILOAD_2:
				if (t == null) {
					t = T.RINT;
					iValue = 2;
				}
				// fall through
			case Opcode.LLOAD_2:
				if (t == null) {
					t = T.LONG;
					iValue = 2;
				}
				// fall through
			case Opcode.ALOAD_3:
				if (t == null) {
					t = T.AREF;
					iValue = 3;
				}
				// fall through
			case Opcode.DLOAD_3:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 3;
				}
				// fall through
			case Opcode.FLOAD_3:
				if (t == null) {
					t = T.FLOAT;
					iValue = 3;
				}
				// fall through
			case Opcode.ILOAD_3:
				if (t == null) {
					t = T.RINT;
					iValue = 3;
				}
				// fall through
			case Opcode.LLOAD_3: {
				if (t == null) {
					t = T.LONG;
					iValue = 3;
				}
				this.ops.add(new LOAD(this.ops.size(), opcode, line, t, iValue));
				break;
			}
			/***********
			 * MONITOR *
			 ***********/
			case Opcode.MONITORENTER:
				type = MONITOR.T_ENTER;
				// fall through
			case Opcode.MONITOREXIT:
				if (type == -1) {
					type = MONITOR.T_EXIT;
				}
				this.ops.add(new MONITOR(this.ops.size(), opcode, line, type));
				break;
			/*******
			 * MUL *
			 *******/
			case Opcode.DMUL:
				t = T.DOUBLE;
				// fall through
			case Opcode.FMUL:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IMUL:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LMUL:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new MUL(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * NEG *
			 *******/
			case Opcode.DNEG:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FNEG:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.INEG:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LNEG:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new NEG(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * NEW *
			 *******/
			case Opcode.NEW:
				this.ops.add(new NEW(this.ops.size(), opcode, line, this.du.getT(constPool
						.getClassInfo(codeReader.readUnsignedShort()))));
				break;
			/************
			 * NEWARRAY *
			 ************/
			case Opcode.ANEWARRAY:
				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, this.du.getT(constPool
						.getClassInfo(codeReader.readUnsignedShort())), 1));
				break;
			case Opcode.NEWARRAY: {
				type = codeReader.readUnsignedByte();
				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, T.TYPES[type], 1));
				break;
			}
			case Opcode.MULTIANEWARRAY: {
				final int classIndex = codeReader.readUnsignedShort();
				final int dimensions = codeReader.readUnsignedByte();
				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, this.du.getT(constPool
						.getClassInfo(classIndex)), dimensions));
				break;
			}
			/*******
			 * NOP *
			 *******/
			case Opcode.NOP:
				// ignore
				break;
			/******
			 * OR *
			 ******/
			case Opcode.IOR:
				t = T.INT;
				// fall through
			case Opcode.LOR:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new OR(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * POP *
			 *******/
			case Opcode.POP:
				oValue = POP.Kind.POP;
				// fall through
			case Opcode.POP2:
				if (oValue == null) {
					oValue = POP.Kind.POP2;
				}
				this.ops.add(new POP(this.ops.size(), opcode, line, (POP.Kind) oValue));
				break;
			/********
			 * PUSH *
			 ********/
			case Opcode.ACONST_NULL:
				t = T.AREF;
				// fall through
			case Opcode.BIPUSH:
				if (t == null) {
					t = T.AINT;
					oValue = codeReader.readSignedByte();
				}
				// fall through
			case Opcode.SIPUSH:
				if (t == null) {
					t = T.AINT;
					oValue = codeReader.readSignedShort();
				}
				// fall through
			case Opcode.LDC:
				if (t == null) {
					final int ldcValueIndex = codeReader.readUnsignedByte();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						t = this.du.getT(Class.class);
						oValue = this.du.getT(constPool.getClassInfo(ldcValueIndex));
						break;
					case ConstPool.CONST_Double:
						// Double / Long only with LDC2_W, but is OK here too
						t = T.DOUBLE;
						// fall through
					case ConstPool.CONST_Float:
						if (t == null) {
							t = T.FLOAT;
						}
						// fall through
					case ConstPool.CONST_Integer:
						if (t == null) {
							t = T.AINT;
						}
						// fall through
					case ConstPool.CONST_Long:
						// Double / Long only with LDC2_W, but is OK here too
						if (t == null) {
							t = T.LONG;
						}
						// fall through
					case ConstPool.CONST_String:
						if (t == null) {
							t = this.du.getT(String.class);
						}
						oValue = constPool.getLdcValue(ldcValueIndex);
						break;
					default:
						throw new RuntimeException("Unknown Const Pool Tag " + tag + " for LDC!");
					}
				}
				// fall through
			case Opcode.LDC_W:
				// fall through
			case Opcode.LDC2_W:
				if (t == null) {
					final int ldcValueIndex = codeReader.readUnsignedShort();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						t = this.du.getT(Class.class);
						oValue = this.du.getT(constPool.getClassInfo(ldcValueIndex));
						break;
					case ConstPool.CONST_Double:
						t = T.DOUBLE;
						// fall through
					case ConstPool.CONST_Float:
						if (t == null) {
							t = T.FLOAT;
						}
						// fall through
					case ConstPool.CONST_Integer:
						if (t == null) {
							t = T.AINT;
						}
						// fall through
					case ConstPool.CONST_Long:
						if (t == null) {
							t = T.LONG;
						}
						// fall through
					case ConstPool.CONST_String:
						if (t == null) {
							t = this.du.getT(String.class);
						}
						oValue = constPool.getLdcValue(ldcValueIndex);
						break;
					default:
						throw new RuntimeException("Unknown Const Pool Tag " + tag + " for LDC!");
					}
				}
				// fall through
			case Opcode.DCONST_0:
				if (t == null) {
					t = T.DOUBLE;
					oValue = 0D;
				}
				// fall through
			case Opcode.FCONST_0:
				if (t == null) {
					t = T.FLOAT;
					oValue = 0F;
				}
				// fall through
			case Opcode.ICONST_0:
				if (t == null) {
					t = T.AINT;
					oValue = 0;
				}
				// fall through
			case Opcode.LCONST_0:
				if (t == null) {
					t = T.LONG;
					oValue = 0L;
				}
				// fall through
			case Opcode.DCONST_1:
				if (t == null) {
					t = T.DOUBLE;
					oValue = 1D;
				}
				// fall through
			case Opcode.FCONST_1:
				if (t == null) {
					t = T.FLOAT;
					oValue = 1F;
				}
				// fall through
			case Opcode.ICONST_1:
				if (t == null) {
					t = T.AINT;
					oValue = 1;
				}
				// fall through
			case Opcode.LCONST_1:
				if (t == null) {
					t = T.LONG;
					oValue = 1L;
				}
				// fall through
			case Opcode.FCONST_2:
				if (t == null) {
					t = T.FLOAT;
					oValue = 2F;
				}
				// fall through
			case Opcode.ICONST_2:
				if (t == null) {
					t = T.AINT;
					oValue = 2;
				}
				// fall through
			case Opcode.ICONST_3:
				if (t == null) {
					t = T.AINT;
					oValue = 3;
				}
				// fall through
			case Opcode.ICONST_4:
				if (t == null) {
					t = T.AINT;
					oValue = 4;
				}
				// fall through
			case Opcode.ICONST_5:
				if (t == null) {
					t = T.AINT;
					oValue = 5;
				}
				// fall through
			case Opcode.ICONST_M1:
				if (t == null) {
					t = T.AINT;
					oValue = -1;
				}
				this.ops.add(new PUSH(this.ops.size(), opcode, line, t, oValue));
				break;
			/*******
			 * PUT *
			 *******/
			case Opcode.PUTFIELD:
			case Opcode.PUTSTATIC: {
				final int fieldIndex = codeReader.readUnsignedShort();

				t = this.du.getT(constPool.getFieldrefClassName(fieldIndex));
				final F f = t.getF(constPool.getFieldrefName(fieldIndex),
						this.du.getDescT(constPool.getFieldrefType(fieldIndex)));
				if (opcode == Opcode.PUTSTATIC) {
					f.markAf(AF.STATIC);
				}
				this.ops.add(new PUT(this.ops.size(), opcode, line, f));
				break;
			}
			/*******
			 * REM *
			 *******/
			case Opcode.DREM:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FREM:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IREM:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LREM:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new REM(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * RET *
			 *******/
			case Opcode.RET: {
				final int varIndex = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
				this.ops.add(new RET(this.ops.size(), opcode, line, varIndex));
				break;
			}
			/**********
			 * RETURN *
			 **********/
			case Opcode.ARETURN:
				t = T.AREF;
				// fall through
			case Opcode.DRETURN:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FRETURN:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.IRETURN:
				if (t == null) {
					t = T.RINT;
				}
				// fall through
			case Opcode.LRETURN:
				if (t == null) {
					t = T.LONG;
				}
				// fall through
			case Opcode.RETURN:
				if (t == null) {
					t = T.VOID;
				}
				this.ops.add(new RETURN(this.ops.size(), opcode, line, t));
				break;
			/*********
			 * STORE *
			 *********/
			case Opcode.ASTORE:
				t = T.AREF;
				// fall through
			case Opcode.DSTORE:
				if (t == null) {
					t = T.DOUBLE;
				}
				// fall through
			case Opcode.FSTORE:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.ISTORE:
				if (t == null) {
					t = T.RINT;
				}
				// fall through
			case Opcode.LSTORE:
				if (t == null) {
					t = T.LONG;
				}
				iValue = wide ? codeReader.readUnsignedShort() : codeReader.readUnsignedByte();
				// fall through
			case Opcode.ASTORE_0:
				if (t == null) {
					t = T.AREF;
					iValue = 0;
				}
				// fall through
			case Opcode.DSTORE_0:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 0;
				}
				// fall through
			case Opcode.FSTORE_0:
				if (t == null) {
					t = T.FLOAT;
					iValue = 0;
				}
				// fall through
			case Opcode.ISTORE_0:
				if (t == null) {
					t = T.RINT;
					iValue = 0;
				}
				// fall through
			case Opcode.LSTORE_0:
				if (t == null) {
					t = T.LONG;
					iValue = 0;
				}
				// fall through
			case Opcode.ASTORE_1:
				if (t == null) {
					t = T.AREF;
					iValue = 1;
				}
				// fall through
			case Opcode.DSTORE_1:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 1;
				}
				// fall through
			case Opcode.FSTORE_1:
				if (t == null) {
					t = T.FLOAT;
					iValue = 1;
				}
				// fall through
			case Opcode.ISTORE_1:
				if (t == null) {
					t = T.RINT;
					iValue = 1;
				}
				// fall through
			case Opcode.LSTORE_1:
				if (t == null) {
					t = T.LONG;
					iValue = 1;
				}
				// fall through
			case Opcode.ASTORE_2:
				if (t == null) {
					t = T.AREF;
					iValue = 2;
				}
				// fall through
			case Opcode.DSTORE_2:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 2;
				}
				// fall through
			case Opcode.FSTORE_2:
				if (t == null) {
					t = T.FLOAT;
					iValue = 2;
				}
				// fall through
			case Opcode.ISTORE_2:
				if (t == null) {
					t = T.RINT;
					iValue = 2;
				}
				// fall through
			case Opcode.LSTORE_2:
				if (t == null) {
					t = T.LONG;
					iValue = 2;
				}
				// fall through
			case Opcode.ASTORE_3:
				if (t == null) {
					t = T.AREF;
					iValue = 3;
				}
				// fall through
			case Opcode.DSTORE_3:
				if (t == null) {
					t = T.DOUBLE;
					iValue = 3;
				}
				// fall through
			case Opcode.FSTORE_3:
				if (t == null) {
					t = T.FLOAT;
					iValue = 3;
				}
				// fall through
			case Opcode.ISTORE_3:
				if (t == null) {
					t = T.RINT;
					iValue = 3;
				}
				// fall through
			case Opcode.LSTORE_3: {
				if (t == null) {
					t = T.LONG;
					iValue = 3;
				}
				this.ops.add(new STORE(this.ops.size(), opcode, line, t, iValue));
				break;
			}
			/*******
			 * SHL *
			 *******/
			case Opcode.ISHL:
				t = T.INT;
				// fall through
			case Opcode.LSHL:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new SHL(this.ops.size(), opcode, line, t));
				break;
			/*******
			 * SHR *
			 *******/
			case Opcode.ISHR:
			case Opcode.IUSHR:
				t = T.INT;
				// fall through
			case Opcode.LSHR:
			case Opcode.LUSHR:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new SHR(this.ops.size(), opcode, line, t, opcode == Opcode.IUSHR
						|| opcode == Opcode.LUSHR));
				break;
			/*******
			 * SUB *
			 *******/
			case Opcode.DSUB:
				t = T.DOUBLE;
				// fall through
			case Opcode.FSUB:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case Opcode.ISUB:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case Opcode.LSUB:
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new SUB(this.ops.size(), opcode, line, t));
				break;
			/********
			 * SWAP *
			 ********/
			case Opcode.SWAP:
				this.ops.add(new SWAP(this.ops.size(), opcode, line));
				break;
			/**********
			 * SWITCH *
			 **********/
			case Opcode.LOOKUPSWITCH: {
				// align
				if (codeReader.pc % 4 > 0) {
					codeReader.pc += 4 - codeReader.pc % 4;
				}
				final SWITCH op = new SWITCH(this.ops.size(), Opcode.LOOKUPSWITCH, line);
				this.ops.add(op);
				// default
				int targetVmpc = vmpc + codeReader.readUnsignedInt();
				int targetPc = getPc(targetVmpc);
				op.setDefaultPc(targetPc);
				if (targetPc < 0) {
					getUnresolved(targetVmpc).add(op);
				}

				// map entries number
				final int npairs = codeReader.readUnsignedInt();

				final int[] caseKeys = new int[npairs];
				final int[] casePcs = new int[npairs];

				for (int i = 0; i < npairs; ++i) {
					caseKeys[i] = codeReader.readUnsignedInt();
					targetVmpc = vmpc + codeReader.readUnsignedInt();
					casePcs[i] = targetPc = getPc(targetVmpc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				op.setCaseKeys(caseKeys);
				op.setCasePcs(casePcs);
				break;
			}
			case Opcode.TABLESWITCH: {
				// align
				if (codeReader.pc % 4 > 0) {
					codeReader.pc += 4 - codeReader.pc % 4;
				}
				final SWITCH op = new SWITCH(this.ops.size(), Opcode.TABLESWITCH, line);
				this.ops.add(op);
				// default
				int targetVmpc = vmpc + codeReader.readUnsignedInt();
				int targetPc = getPc(targetVmpc);
				op.setDefaultPc(targetPc);
				if (targetPc < 0) {
					getUnresolved(targetVmpc).add(op);
				}

				// map key boundaries
				final int caseLow = codeReader.readUnsignedInt();
				final int caseHigh = codeReader.readUnsignedInt();

				final int[] caseKeys = new int[caseHigh - caseLow + 1];
				final int[] casePcs = new int[caseHigh - caseLow + 1];

				for (int i = 0, caseValue = caseLow; caseValue <= caseHigh; ++caseValue, ++i) {
					caseKeys[i] = caseValue;
					targetVmpc = vmpc + codeReader.readUnsignedInt();
					casePcs[i] = targetPc = getPc(targetVmpc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				op.setCaseKeys(caseKeys);
				op.setCasePcs(casePcs);
				break;
			}
			/*********
			 * THROW *
			 *********/
			case Opcode.ATHROW:
				this.ops.add(new THROW(this.ops.size(), opcode, line));
				break;
			/*******
			 * XOR *
			 *******/
			case Opcode.IXOR:
				t = T.INT;
				// fall through
			case Opcode.LXOR: {
				if (t == null) {
					t = T.LONG;
				}
				this.ops.add(new XOR(this.ops.size(), opcode, line, t));
				break;
			}
			/*******
			 * WIDE *
			 *******/
			case Opcode.WIDE:
				wide = true;
				// just for once! reset wide after switch
				continue;
			default:
				throw new RuntimeException("Unknown jvm operation code '" + opcode + "'!");
			}
			// reset wide
			wide = false;
		}
		visitVmpc(codeReader.pc);
		cfg.setOps(this.ops.toArray(new Op[this.ops.size()]));

		final ExceptionTable exceptionTable = codeAttribute.getExceptionTable();
		if (exceptionTable != null) {
			final ArrayList<Exc> excs = new ArrayList<Exc>();
			// preserve order
			final int exceptionTableSize = exceptionTable.size();
			for (int i = 0; i < exceptionTableSize; ++i) {
				final String catchName = constPool.getClassInfo(exceptionTable.catchType(i));
				// no array possible, name is OK here
				final T catchT = catchName == null ? null : this.du.getT(catchName);
				final Exc exc = new Exc(catchT);

				exc.setStartPc(this.vmpc2pc.get(exceptionTable.startPc(i)));
				exc.setEndPc(this.vmpc2pc.get(exceptionTable.endPc(i)));
				exc.setHandlerPc(this.vmpc2pc.get(exceptionTable.handlerPc(i)));

				excs.add(exc);
			}
			if (excs.size() > 0) {
				cfg.setExcs(excs.toArray(new Exc[excs.size()]));
			}
		}
		readLocalVariables(cfg, localVariableAttribute, localVariableTypeAttribute);
	}

	private void readLocalVariables(final CFG cfg,
			final LocalVariableAttribute localVariableAttribute,
			final LocalVariableAttribute localVariableTypeAttribute) {
		final HashMap<Integer, ArrayList<V>> reg2vs = new HashMap<Integer, ArrayList<V>>();
		if (localVariableAttribute != null) {
			// preserve order
			final int tableLength = localVariableAttribute.tableLength();
			for (int i = 0; i < tableLength; ++i) {
				final T vT = cfg.getDu().getDescT(localVariableAttribute.descriptor(i));
				final V v = new V(vT, localVariableAttribute.variableName(i),
						this.vmpc2pc.get(localVariableAttribute.startPc(i)),
						this.vmpc2pc.get(localVariableAttribute.startPc(i)
								+ localVariableAttribute.codeLength(i)));

				final int index = localVariableAttribute.index(i);

				ArrayList<V> vs = reg2vs.get(index);
				if (vs == null) {
					vs = new ArrayList<V>();
					reg2vs.put(index, vs);
				}
				vs.add(v);
			}
			if (reg2vs.size() > 0) {
				for (final Entry<Integer, ArrayList<V>> entry : reg2vs.entrySet()) {
					final int reg = entry.getKey();
					for (final V var : entry.getValue()) {
						cfg.addVar(reg, var);
					}
				}
			}
		}
		if (localVariableTypeAttribute != null) {
			// preserve order
			final int tableLength = localVariableTypeAttribute.tableLength();
			for (int i = 0; i < tableLength; ++i) {
				final V v = cfg.getDebugV(localVariableTypeAttribute.index(i),
						this.vmpc2pc.get(localVariableTypeAttribute.startPc(i)));
				if (v == null) {
					LOGGER.warning("Local variable type attribute '"
							+ localVariableTypeAttribute.index(i)
							+ "' without local variable attribute!");
					continue;
				}
				v.getT().setSignature(localVariableTypeAttribute.signature(i));
			}
		}
		cfg.postProcessVars();
	}

	private void visitVmpc(final int vmpc) {
		final Integer pc = this.vmpc2pc.put(vmpc, this.ops.size());
		if (pc == null) {
			// fresh new label, never referenced before
			return;
		}
		if (pc > 0) {
			// visited before but is known?!
			LOGGER.warning("VM PC '" + vmpc + "' is not unique, has old PC '" + this.ops.size()
					+ "'!");
			return;
		}
		// unknown and has forward reference
		for (final Object o : this.vmpc2unresolved.get(vmpc)) {
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
				final int[] keyTargets = op.getCasePcs();
				for (int i = keyTargets.length; i-- > 0;) {
					if (pc == keyTargets[i]) {
						keyTargets[i] = this.ops.size();
					}
				}
				continue;
			}
			// cannot happen for Exc / Var here
		}
	}

}