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
import org.decojer.cavaj.model.vm.intermediate.operations.CAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
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
import org.ow2.asm.Opcodes;

/**
 * Read code attribute.
 * 
 * @author André Pankraz
 */
public class ReadCodeAttribute {

	private final static Logger LOGGER = Logger
			.getLogger(ReadCodeAttribute.class.getName());

	private final DU du;

	private MD md;

	final ArrayList<Operation> operations = new ArrayList<Operation>();

	private final HashMap<Integer, Integer> pc2index = new HashMap<Integer, Integer>();

	private final HashMap<Integer, ArrayList<Object>> pc2unresolved = new HashMap<Integer, ArrayList<Object>>();

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

	private int getPcIndex(final int pc) {
		final Integer index = this.pc2index.get(pc);
		if (index != null) {
			return index;
		}
		final int unresolvedIndex = -1 - this.pc2unresolved.size();
		this.pc2index.put(pc, unresolvedIndex);
		return unresolvedIndex;
	}

	private ArrayList<Object> getPcUnresolved(final int pc) {
		ArrayList<Object> unresolved = this.pc2unresolved.get(pc);
		if (unresolved == null) {
			unresolved = new ArrayList<Object>();
			this.pc2unresolved.put(pc, unresolved);
		}
		return unresolved;
	}

	/**
	 * Init and set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 * @param codeAttribute
	 *            Javassist code attribute
	 */
	@SuppressWarnings("unchecked")
	public void initAndVisit(final MD md, final CodeAttribute codeAttribute) {
		this.md = md;

		this.operations.clear();
		this.pc2index.clear();
		this.pc2unresolved.clear();

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
				LOGGER.warning("Unknown code attribute tag '" + attributeTag
						+ "' in '" + md + "'!");
			}
		}

		LOGGER.fine("Stack info: " + stackMap + " : " + stackMapTable);

		// read code
		final CodeReader codeReader = new CodeReader(codeAttribute.getCode());

		// init CFG with start BB
		final CFG cfg = new CFG(md, codeAttribute.getMaxLocals(),
				codeAttribute.getMaxStack());
		md.setCFG(cfg);

		final DU du = md.getTd().getT().getDu();
		final ConstPool constPool = codeAttribute.getConstPool();

		// wide operation following?
		// one of: iload, fload, aload, lload, dload, istore, fstore, astore,
		// lstore, dstore, or ret
		boolean wide = false;

		while (codeReader.isNext()) {
			final int opPc = codeReader.pc;

			visitPc(opPc);

			final int code = codeReader.readUnsignedByte();
			final int line = lineNumberAttribute == null ? -1
					: lineNumberAttribute.toLineNumber(opPc);

			final int pc = this.operations.size();

			T t = null;
			int type = -1;
			int iValue = Integer.MIN_VALUE;
			Object oValue = null;

			switch (code) {
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
				this.operations.add(new ADD(pc, code, line, t));
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
				this.operations.add(new ALOAD(pc, code, line, t));
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
				this.operations.add(new AND(pc, code, line, t));
				break;
			/***************
			 * ARRAYLENGTH *
			 ***************/
			case Opcode.ARRAYLENGTH:
				this.operations.add(new ARRAYLENGTH(pc, code, line));
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
				this.operations.add(new ASTORE(pc, code, line, t));
				break;
			/********
			 * CAST *
			 ********/
			case Opcode.CHECKCAST: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				t = T.AREF;
				oValue = readType(constPool.getClassInfo(cpClassIndex));
			}
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
				this.operations.add(new CAST(pc, code, line, t, (T) oValue));
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
				this.operations.add(new CMP(pc, code, line, t, iValue));
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
				this.operations.add(new DIV(pc, code, line, t));
				break;
			/*******
			 * DUP *
			 *******/
			case Opcode.DUP:
				type = DUP.T_DUP;
				// fall through
			case Opcode.DUP_X1:
				if (type == -1) {
					type = DUP.T_DUP_X1;
				}
				// fall through
			case Opcode.DUP_X2:
				if (type == -1) {
					type = DUP.T_DUP_X2;
				}
				// fall through
			case Opcode.DUP2:
				if (type == -1) {
					type = DUP.T_DUP2;
				}
				// fall through
			case Opcode.DUP2_X1:
				if (type == -1) {
					type = DUP.T_DUP2_X1;
				}
				// fall through
			case Opcode.DUP2_X2:
				if (type == -1) {
					type = DUP.T_DUP2_X2;
				}
				this.operations.add(new DUP(pc, code, line, type));
				break;
			/*******
			 * GET *
			 *******/
			case Opcode.GETFIELD:
			case Opcode.GETSTATIC: {
				final int cpFieldIndex = codeReader.readUnsignedShort();

				final T ownerT = readType(constPool
						.getFieldrefClassName(cpFieldIndex));
				t = du.getDescT(constPool.getFieldrefType(cpFieldIndex));
				final F f = ownerT.getF(
						constPool.getFieldrefName(cpFieldIndex), t);
				if (code == Opcode.GETSTATIC) {
					f.markAf(AF.STATIC);
				}
				this.operations.add(new GET(pc, code, line, f));
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
					final GOTO op = new GOTO(pc, code, line);
					final int targetPc = opPc + iValue;
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
				}
				break;
			/*******
			 * INC *
			 *******/
			case Opcode.IINC: {
				final int varIndex = codeReader.readUnsignedByte();
				final int constValue = codeReader.readUnsignedByte();
				this.operations.add(new INC(pc, code, line, T.INT, varIndex,
						constValue));
				break;
			}
			/**************
			 * INSTANCEOF *
			 **************/
			case Opcode.INSTANCEOF: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				this.operations.add(new INSTANCEOF(pc, code, line,
						readType(constPool.getClassInfo(cpClassIndex))));
				break;
			}
			/**********
			 * INVOKE *
			 **********/
			case Opcode.INVOKEINTERFACE: {
				// interface method callout
				final int cpMethodIndex = codeReader.readUnsignedShort();
				codeReader.readUnsignedByte(); // count, unused
				codeReader.readUnsignedByte(); // reserved, unused

				final T invokeT = readType(constPool
						.getInterfaceMethodrefClassName(cpMethodIndex));
				invokeT.markAf(AF.INTERFACE);
				final M invokeM = invokeT.getM(
						constPool.getInterfaceMethodrefName(cpMethodIndex),
						constPool.getInterfaceMethodrefType(cpMethodIndex));

				this.operations.add(new INVOKE(pc, code, line, invokeM, false));
				break;
			}
			case Opcode.INVOKESPECIAL:
				// constructor or supermethod callout
			case Opcode.INVOKEVIRTUAL:
			case Opcode.INVOKESTATIC: {
				final int cpMethodIndex = codeReader.readUnsignedShort();

				final T invokeT = readType(constPool
						.getMethodrefClassName(cpMethodIndex));
				final M invokeM = invokeT.getM(
						constPool.getMethodrefName(cpMethodIndex),
						constPool.getMethodrefType(cpMethodIndex));
				if (code == Opcode.INVOKESTATIC) {
					invokeM.markAf(AF.STATIC);
				}
				this.operations.add(new INVOKE(pc, code, line, invokeM,
						code == Opcode.INVOKESPECIAL));
				break;
			}
			/********
			 * JCMP *
			 ********/
			case Opcode.IF_ACMPEQ:
				t = T.AREF;
				iValue = CompareType.T_EQ;
				// fall through
			case Opcode.IF_ACMPNE:
				if (t == null) {
					t = T.AREF;
					iValue = CompareType.T_NE;
				}
				// fall through
			case Opcode.IF_ICMPEQ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_EQ;
				}
				// fall through
			case Opcode.IF_ICMPGE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case Opcode.IF_ICMPGT:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case Opcode.IF_ICMPLE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case Opcode.IF_ICMPLT:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case Opcode.IF_ICMPNE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_NE;
				}
				{
					final JCMP op = new JCMP(pc, code, line, t, iValue);
					final int targetPc = opPc + codeReader.readSignedShort();
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
				}
				break;
			/********
			 * JCND *
			 ********/
			case Opcode.IFNULL:
				t = T.AREF;
				iValue = CompareType.T_EQ;
				// fall through
			case Opcode.IFNONNULL:
				if (t == null) {
					t = T.AREF;
					iValue = CompareType.T_NE;
				}
				// fall through
			case Opcode.IFEQ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_EQ;
				}
				// fall through
			case Opcode.IFGE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case Opcode.IFGT:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case Opcode.IFLE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case Opcode.IFLT:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case Opcode.IFNE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_NE;
				}
				{
					final JCND op = new JCND(pc, code, line, t, iValue);
					final int targetPc = opPc + codeReader.readSignedShort();
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
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
					final JSR op = new JSR(pc, code, line);
					final int targetPc = opPc + iValue;
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
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
					t = T.AINT;
				}
				// fall through
			case Opcode.LLOAD:
				if (t == null) {
					t = T.LONG;
				}
				iValue = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
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
					t = T.AINT;
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
					t = T.AINT;
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
					t = T.AINT;
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
					t = T.AINT;
					iValue = 3;
				}
				// fall through
			case Opcode.LLOAD_3: {
				if (t == null) {
					t = T.LONG;
					iValue = 3;
				}
				this.operations.add(new LOAD(pc, code, line, t, iValue));
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
				this.operations.add(new MONITOR(pc, code, line, type));
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
				this.operations.add(new MUL(pc, code, line, t));
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
				this.operations.add(new NEG(pc, code, line, t));
				break;
			/*******
			 * NEW *
			 *******/
			case Opcode.NEW: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				this.operations.add(new NEW(pc, code, line, readType(constPool
						.getClassInfo(cpClassIndex))));
				break;
			}
			/************
			 * NEWARRAY *
			 ************/
			case Opcode.ANEWARRAY: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				this.operations.add(new NEWARRAY(pc, code, line,
						readType(constPool.getClassInfo(cpClassIndex)), 1));
				break;
			}
			case Opcode.NEWARRAY: {
				type = codeReader.readUnsignedByte();
				final String typeName = new String[] { null, null, null, null,
						boolean.class.getName(), char.class.getName(),
						float.class.getName(), double.class.getName(),
						byte.class.getName(), short.class.getName(),
						int.class.getName(), long.class.getName() }[type];
				this.operations.add(new NEWARRAY(pc, code, line, du
						.getT(typeName), 1));
				break;
			}
			case Opcode.MULTIANEWARRAY: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				final int dimensions = codeReader.readUnsignedByte();
				this.operations.add(new NEWARRAY(pc, code, line,
						readType(constPool.getClassInfo(cpClassIndex)),
						dimensions));
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
				this.operations.add(new OR(pc, code, line, t));
				break;
			/*******
			 * POP *
			 *******/
			case Opcode.POP:
				type = POP.T_POP;
				// fall through
			case Opcode.POP2:
				if (type == -1) {
					type = POP.T_POP2;
				}
				this.operations.add(new POP(pc, code, line, type));
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
						t = du.getT(Class.class);
						oValue = readType(constPool.getClassInfo(ldcValueIndex));
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
							t = du.getT(String.class);
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
				if (t == null) {
					final int ldcValueIndex = codeReader.readUnsignedShort();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						t = du.getT(Class.class);
						oValue = readType(constPool.getClassInfo(ldcValueIndex));
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
							t = du.getT(String.class);
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
				if (t == null) {
					t = T.DOUBLE;
					oValue = 0D;
				}
				// fall through
			case Opcode.FCONST_0:
				if (t == null) {
					t = T.FLOAT;
					oValue = 0;
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
					oValue = 1;
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
					oValue = 2;
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
				this.operations.add(new PUSH(pc, code, line, t, oValue));
				break;
			/*******
			 * PUT *
			 *******/
			case Opcode.PUTFIELD:
			case Opcode.PUTSTATIC: {
				final int cpFieldIndex = codeReader.readUnsignedShort();

				final T ownerT = readType(constPool
						.getFieldrefClassName(cpFieldIndex));
				t = du.getDescT(constPool.getFieldrefType(cpFieldIndex));
				final F f = ownerT.getF(
						constPool.getFieldrefName(cpFieldIndex), t);
				if (code == Opcode.PUTSTATIC) {
					f.markAf(AF.STATIC);
				}
				this.operations.add(new PUT(pc, code, line, f));
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
				this.operations.add(new REM(pc, code, line, t));
				break;
			/*******
			 * RET *
			 *******/
			case Opcode.RET: {
				final int varIndex = wide ? codeReader.readUnsignedShort()
						: codeReader.readUnsignedByte();
				this.operations.add(new RET(pc, code, line, varIndex));
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
					t = T.INT;
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
				this.operations.add(new RETURN(pc, code, line, t));
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
					t = T.AINT;
				}
				// fall through
			case Opcode.LSTORE:
				if (t == null) {
					t = T.LONG;
				}
				iValue = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
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
					t = T.AINT;
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
					t = T.AINT;
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
					t = T.AINT;
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
					t = T.AINT;
					iValue = 3;
				}
				// fall through
			case Opcode.LSTORE_3: {
				if (t == null) {
					t = T.LONG;
					iValue = 3;
				}
				this.operations.add(new STORE(pc, code, line, t, iValue));
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
				this.operations.add(new SHL(pc, code, line, t));
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
				this.operations.add(new SHR(pc, code, line, t,
						code == Opcode.IUSHR || code == Opcode.LUSHR));
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
				this.operations.add(new SUB(pc, code, line, t));
				break;
			/********
			 * SWAP *
			 ********/
			case Opcode.SWAP:
				this.operations.add(new SWAP(pc, code, line));
				break;
			/**********
			 * SWITCH *
			 **********/
			case Opcode.LOOKUPSWITCH: {
				// align
				if (codeReader.pc % 4 > 0) {
					codeReader.pc += 4 - codeReader.pc % 4;
				}
				final SWITCH op = new SWITCH(pc, Opcodes.LOOKUPSWITCH, line);
				// default
				int targetPc = opPc + codeReader.readUnsignedInt();
				int pcIndex = getPcIndex(targetPc);
				op.setDefaultPc(pcIndex);
				if (pcIndex < 0) {
					getPcUnresolved(targetPc).add(op);
				}

				// map entries number
				final int npairs = codeReader.readUnsignedInt();

				final int[] caseKeys = new int[npairs];
				final int[] casePcs = new int[npairs];

				for (int i = 0; i < npairs; ++i) {
					caseKeys[i] = codeReader.readUnsignedInt();
					targetPc = opPc + codeReader.readUnsignedInt();
					casePcs[i] = pcIndex = getPcIndex(targetPc);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
				}
				op.setCaseKeys(caseKeys);
				op.setCasePcs(casePcs);
				this.operations.add(op);
				break;
			}
			case Opcode.TABLESWITCH: {
				// align
				if (codeReader.pc % 4 > 0) {
					codeReader.pc += 4 - codeReader.pc % 4;
				}
				final SWITCH op = new SWITCH(pc, Opcodes.LOOKUPSWITCH, line);
				// default
				int targetPc = opPc + codeReader.readUnsignedInt();
				int pcIndex = getPcIndex(targetPc);
				op.setDefaultPc(pcIndex);
				if (pcIndex < 0) {
					getPcUnresolved(targetPc).add(op);
				}

				// map key boundaries
				final int caseLow = codeReader.readUnsignedInt();
				final int caseHigh = codeReader.readUnsignedInt();

				final int[] caseKeys = new int[caseHigh - caseLow + 1];
				final int[] casePcs = new int[caseHigh - caseLow + 1];

				for (int i = 0, caseValue = caseLow; caseValue <= caseHigh; ++caseValue, ++i) {
					caseKeys[i] = caseValue;
					targetPc = opPc + codeReader.readUnsignedInt();
					casePcs[i] = pcIndex = getPcIndex(targetPc);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
				}
				op.setCaseKeys(caseKeys);
				op.setCasePcs(casePcs);
				this.operations.add(op);
				break;
			}
			/*********
			 * THROW *
			 *********/
			case Opcode.ATHROW:
				this.operations.add(new THROW(pc, code, line));
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
				this.operations.add(new XOR(pc, code, line, t));
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
				throw new RuntimeException("Unknown jvm operation code '"
						+ code + "'!");
			}
			// reset wide
			wide = false;
		}
		visitPc(codeReader.pc);
		cfg.setOperations(this.operations.toArray(new Operation[this.operations
				.size()]));

		final ExceptionTable exceptionTable = codeAttribute.getExceptionTable();
		if (exceptionTable != null) {
			final ArrayList<Exc> excs = new ArrayList<Exc>();
			// preserve order
			final int exceptionTableSize = exceptionTable.size();
			for (int i = 0; i < exceptionTableSize; ++i) {
				final String catchName = constPool.getClassInfo(exceptionTable
						.catchType(i));
				// no array possible, name is OK here
				final T catchT = catchName == null ? null : du.getT(catchName);
				final Exc exc = new Exc(catchT);

				exc.setStartPc(this.pc2index.get(exceptionTable.startPc(i)));
				exc.setEndPc(this.pc2index.get(exceptionTable.endPc(i)));
				exc.setHandlerPc(this.pc2index.get(exceptionTable.handlerPc(i)));

				excs.add(exc);
			}
			if (excs.size() > 0) {
				cfg.setExcs(excs.toArray(new Exc[excs.size()]));
			}
		}
		readLocalVariables(localVariableAttribute, localVariableTypeAttribute);
		cfg.postProcessVars();
	}

	private void readLocalVariables(
			final LocalVariableAttribute localVariableAttribute,
			final LocalVariableAttribute localVariableTypeAttribute) {
		final CFG cfg = this.md.getCfg();
		final HashMap<Integer, ArrayList<Var>> reg2vars = new HashMap<Integer, ArrayList<Var>>();
		if (localVariableAttribute != null) {
			final DU du = this.md.getTd().getT().getDu();
			// preserve order
			final int tableLength = localVariableAttribute.tableLength();
			for (int i = 0; i < tableLength; ++i) {
				final T varT = du
						.getDescT(localVariableAttribute.descriptor(i));
				final Var var = new Var(varT);

				var.setName(localVariableAttribute.variableName(i));
				var.setStartPc(this.pc2index.get(localVariableAttribute
						.startPc(i)));
				var.setEndPc(this.pc2index.get(localVariableAttribute
						.startPc(i) + localVariableAttribute.codeLength(i)));

				final int index = localVariableAttribute.index(i);

				ArrayList<Var> vars = reg2vars.get(index);
				if (vars == null) {
					vars = new ArrayList<Var>();
					reg2vars.put(index, vars);
				}
				vars.add(var);
			}
			if (reg2vars.size() > 0) {
				for (final Entry<Integer, ArrayList<Var>> entry : reg2vars
						.entrySet()) {
					final int reg = entry.getKey();
					for (final Var var : entry.getValue()) {
						cfg.addVar(reg, var);
					}
				}
			}
		}
		if (localVariableTypeAttribute != null) {
			// preserve order
			final int tableLength = localVariableTypeAttribute.tableLength();
			for (int i = 0; i < tableLength; ++i) {
				final Var var = cfg.getVar(localVariableTypeAttribute.index(i),
						localVariableTypeAttribute.startPc(i));
				if (var == null) {
					LOGGER.warning("Local variable type attribute '"
							+ localVariableTypeAttribute.index(i)
							+ "' without local variable attribute!");
					continue;
				}
				var.getT()
						.setSignature(localVariableTypeAttribute.signature(i));
			}
		}
	}

	private T readType(final String classInfo) {
		if (classInfo == null) {
			return null;
		}
		// strange behaviour for classinfo:
		// arrays: normal descriptor (but with '.'):
		// [[I, [Ljava/lang/String;
		if (classInfo.charAt(0) == '[') {
			return this.du.getDescT(classInfo.replace('.', '/'));
		}
		// no arrays - class name:
		// org.decojer.cavaj.test.DecTestInner$1$1$1
		return this.du.getT(classInfo);
	}

	private void visitPc(final int pc) {
		final Integer pcIndex = this.pc2index.put(pc, this.operations.size());
		if (pcIndex == null) {
			// fresh new label, never referenced before
			return;
		}
		if (pcIndex > 0) {
			// visited before but is known?!
			LOGGER.warning("Pc '" + pc + "' is not unique, has old opPc '"
					+ this.operations.size() + "'!");
			return;
		}
		final int labelUnknownIndex = pcIndex;
		// unknown and has forward reference
		for (final Object o : this.pc2unresolved.get(pc)) {
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
				final int[] keyTargets = op.getCasePcs();
				for (int i = keyTargets.length; i-- > 0;) {
					if (labelUnknownIndex == keyTargets[i]) {
						keyTargets[i] = this.operations.size();
					}
				}
				continue;
			}
			// cannot happen for Exc / Var here
		}
	}

}