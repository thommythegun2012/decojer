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
package org.decojer.cavaj.reader.smali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

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
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.ALOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.AND;
import org.decojer.cavaj.model.vm.intermediate.operations.ARRAYLENGTH;
import org.decojer.cavaj.model.vm.intermediate.operations.ASTORE;
import org.decojer.cavaj.model.vm.intermediate.operations.CHECKCAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;
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
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.PUT;
import org.decojer.cavaj.model.vm.intermediate.operations.REM;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.SHL;
import org.decojer.cavaj.model.vm.intermediate.operations.SHR;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.CodeItem.EncodedTypeAddrPair;
import org.jf.dexlib.CodeItem.TryItem;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.Instruction10t;
import org.jf.dexlib.Code.Format.Instruction11n;
import org.jf.dexlib.Code.Format.Instruction11x;
import org.jf.dexlib.Code.Format.Instruction12x;
import org.jf.dexlib.Code.Format.Instruction20t;
import org.jf.dexlib.Code.Format.Instruction21c;
import org.jf.dexlib.Code.Format.Instruction21h;
import org.jf.dexlib.Code.Format.Instruction21s;
import org.jf.dexlib.Code.Format.Instruction21t;
import org.jf.dexlib.Code.Format.Instruction22b;
import org.jf.dexlib.Code.Format.Instruction22c;
import org.jf.dexlib.Code.Format.Instruction22s;
import org.jf.dexlib.Code.Format.Instruction22t;
import org.jf.dexlib.Code.Format.Instruction22x;
import org.jf.dexlib.Code.Format.Instruction23x;
import org.jf.dexlib.Code.Format.Instruction30t;
import org.jf.dexlib.Code.Format.Instruction31c;
import org.jf.dexlib.Code.Format.Instruction31i;
import org.jf.dexlib.Code.Format.Instruction31t;
import org.jf.dexlib.Code.Format.Instruction32x;
import org.jf.dexlib.Code.Format.Instruction35c;
import org.jf.dexlib.Code.Format.Instruction51l;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;
import org.jf.dexlib.Debug.DebugInstructionIterator;

/**
 * Read code item.
 * 
 * @author André Pankraz
 */
public class ReadCodeItem {

	private final static Logger LOGGER = Logger.getLogger(ReadCodeItem.class
			.getName());

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
	public ReadCodeItem(final DU du) {
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
	 * @param codeItem
	 *            code item
	 */
	public void initAndVisit(final MD md, final CodeItem codeItem) {
		this.md = md;

		this.operations.clear();
		this.pc2index.clear();
		this.pc2unresolved.clear();

		final M m = md.getM();

		final CFG cfg = new CFG(md, codeItem.getRegisterCount(), 0);
		md.setCFG(cfg);

		HashMap<Integer, Integer> opLines = null;

		// must read debug info before operations because of line numbers
		final DebugInfoItem debugInfo = codeItem.getDebugInfo();
		if (debugInfo != null) {
			final StringIdItem[] parameterNames = debugInfo.getParameterNames();
			if (parameterNames != null && parameterNames.length > 0) {
				for (int i = parameterNames.length; i-- > 0;) {
					if (parameterNames[i] == null) {
						// could happen, e.g. synthetic methods, inner <init>
						// with outer type param
						continue;
					}
					m.setParamName(i, parameterNames[i].getStringValue());
				}
			}
			final ReadDebugInfo readDebugInfo = new ReadDebugInfo(cfg);
			DebugInstructionIterator.DecodeInstructions(debugInfo,
					codeItem.getRegisterCount(), readDebugInfo);
			opLines = readDebugInfo.getOpLines();
		}
		cfg.postProcessVars();

		final Instruction[] instructions = codeItem.getInstructions();

		// 2 free to use work register, 3 parameter
		// static: (5 register)
		// work_register1...work_register_2...param1...param2...param3
		// dynamic: (6 register)
		// work_register1...work_register_2...this...param1...param2...param3

		for (int opPc = 0, line = -1, i = 0; i < instructions.length; ++i) {
			final Instruction instruction = instructions[i];

			visitPc(opPc, instruction);

			final int code = instruction.opcode.value;
			if (opLines != null && opLines.containsKey(opPc)) {
				// opLine remains constant with increasing opPc till new info is
				// available
				line = opLines.get(opPc);
			}

			final int pc = this.operations.size();

			T t = null;
			int type = -1;
			int iValue = 0;
			Object oValue = null;

			switch (instruction.opcode) {
			/*******
			 * ADD *
			 *******/
			case ADD_DOUBLE:
				t = T.DOUBLE;
				// fall through
			case ADD_FLOAT:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case ADD_INT:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case ADD_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B + C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new ADD(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case ADD_DOUBLE_2ADDR:
				t = T.DOUBLE;
				// fall through
			case ADD_FLOAT_2ADDR:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case ADD_INT_2ADDR:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case ADD_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A += B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new ADD(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case ADD_INT_LIT8: {
				// A = B + literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new ADD(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case ADD_INT_LIT16: {
				// A = B + literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new ADD(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/*********
			 * ALOAD *
			 *********/
			case AGET:
				t = T.INT;
				// fall through
			case AGET_BOOLEAN:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case AGET_BYTE:
				if (t == null) {
					t = T.BYTE;
				}
				// fall through
			case AGET_CHAR:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case AGET_OBJECT:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case AGET_SHORT:
				if (t == null) {
					t = T.SHORT;
				}
				// fall through
			case AGET_WIDE:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B[C]
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, T.AREF, instr
							.getRegisterB())); // TODO array type?
					this.operations.add(new LOAD(pc, code, line, T.INT, instr
							.getRegisterC()));

					this.operations.add(new ALOAD(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			/*******
			 * AND *
			 *******/
			case AND_INT:
				t = T.INT;
				// fall through
			case AND_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B & C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new AND(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case AND_INT_2ADDR:
				t = T.INT;
				// fall through
			case AND_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A &= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new AND(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case AND_INT_LIT8: {
				// A = B & literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new AND(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case AND_INT_LIT16: {
				// A = B & literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new AND(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/***************
			 * ARRAYLENGTH *
			 ***************/
			case ARRAY_LENGTH: {
				// A = B.length
				final Instruction12x instr = (Instruction12x) instruction;

				this.operations.add(new LOAD(pc, code, line, T.AREF, instr
						.getRegisterB()));

				this.operations.add(new ARRAYLENGTH(pc, code, line));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/**********
			 * ASTORE *
			 **********/
			case APUT:
				t = T.INT;
				// fall through
			case APUT_BOOLEAN:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case APUT_BYTE:
				if (t == null) {
					t = T.BYTE;
				}
				// fall through
			case APUT_CHAR:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case APUT_OBJECT:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case APUT_SHORT:
				if (t == null) {
					t = T.SHORT;
				}
				// fall through
			case APUT_WIDE:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// B[C] = A
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, T.AREF, instr
							.getRegisterB())); // TODO array type?
					this.operations.add(new LOAD(pc, code, line, T.INT, instr
							.getRegisterC()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));

					this.operations.add(new ASTORE(pc, code, line, t));
				}
				break;
			/**************
			 * CHECKCAST *
			 **************/
			case CHECK_CAST: {
				// A := (typeIdItem) A
				final Instruction21c instr = (Instruction21c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem())
						.getTypeDescriptor());

				this.operations.add(new LOAD(pc, code, line, T.AREF, instr
						.getRegisterA()));

				this.operations.add(new CHECKCAST(pc, code, line, t));

				this.operations.add(new STORE(pc, code, line, t, instr
						.getRegisterA()));
				break;
			}
			/*******
			 * CMP *
			 *******/
			case CMPG_DOUBLE:
				t = T.DOUBLE;
				iValue = CMP.T_G;
				// fall through
			case CMPG_FLOAT:
				if (t == null) {
					t = T.FLOAT;
					iValue = CMP.T_G;
				}
				// fall through
			case CMPL_DOUBLE:
				if (t == null) {
					t = T.DOUBLE;
					iValue = CMP.T_L;
				}
				// fall through
			case CMPL_FLOAT:
				if (t == null) {
					t = T.FLOAT;
					iValue = CMP.T_L;
				}
				// fall through
			case CMP_LONG:
				if (t == null) {
					t = T.LONG;
					iValue = CMP.T_0;
				}
				{
					// A = B CMP C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new CMP(pc, code, line, t, iValue));

					this.operations.add(new STORE(pc, code, line, T.INT, instr
							.getRegisterA()));
				}
				break;
			/*******
			 * DIV *
			 *******/
			case DIV_DOUBLE:
				t = T.DOUBLE;
				// fall through
			case DIV_FLOAT:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case DIV_INT:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case DIV_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B / C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new DIV(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case DIV_DOUBLE_2ADDR:
				t = T.DOUBLE;
				// fall through
			case DIV_FLOAT_2ADDR:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case DIV_INT_2ADDR:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case DIV_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A /= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new DIV(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case DIV_INT_LIT8: {
				// A = B / literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new DIV(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case DIV_INT_LIT16: {
				// A = B / literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new DIV(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/*******
			 * GET *
			 *******/
			case IGET:
			case IGET_VOLATILE:
				t = T.INT;
				// fall through
			case IGET_BOOLEAN:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case IGET_BYTE:
				if (t == null) {
					t = T.BYTE;
				}
				// fall through
			case IGET_CHAR:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case IGET_OBJECT:
			case IGET_OBJECT_VOLATILE:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case IGET_SHORT:
				if (t == null) {
					t = T.SHORT;
				}
				// fall through
			case IGET_WIDE:
			case IGET_WIDE_VOLATILE:
				if (t == null) {
					t = T.WIDE;
				}
				// fall through
				{
					// A = B.fieldIdItem
					final Instruction22c instr = (Instruction22c) instruction;

					final FieldIdItem fieldIdItem = (FieldIdItem) instr
							.getReferencedItem();
					final T ownerT = this.du.getDescT(fieldIdItem
							.getContainingClass().getTypeDescriptor());
					final T fieldT = this.du.getDescT(fieldIdItem
							.getFieldType().getTypeDescriptor());
					final F f = ownerT.getF(fieldIdItem.getFieldName()
							.getStringValue(), fieldT);

					if (t != T.AREF && !t.equals(fieldT)) {
						LOGGER.warning("IGET TODO Must check compatibility here? T '"
								+ t + "' not of '" + fieldT + "'!");
					}

					this.operations.add(new LOAD(pc, code, line, ownerT, instr
							.getRegisterB()));

					this.operations.add(new GET(pc, code, line, f));

					this.operations.add(new STORE(pc, code, line, fieldT, instr
							.getRegisterA()));
				}
				break;
			case SGET:
			case SGET_VOLATILE:
				t = T.INT;
				// fall through
			case SGET_BOOLEAN:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case SGET_BYTE:
				if (t == null) {
					t = T.BYTE;
				}
				// fall through
			case SGET_CHAR:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case SGET_OBJECT:
			case SGET_OBJECT_VOLATILE:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case SGET_SHORT:
				if (t == null) {
					t = T.SHORT;
				}
				// fall through
			case SGET_WIDE:
			case SGET_WIDE_VOLATILE:
				if (t == null) {
					t = T.WIDE;
				}
				// fall through
				{
					// A = fieldIdItem
					final Instruction21c instr = (Instruction21c) instruction;

					final FieldIdItem fieldIdItem = (FieldIdItem) instr
							.getReferencedItem();
					final T ownerT = this.du.getDescT(fieldIdItem
							.getContainingClass().getTypeDescriptor());
					final T fieldT = this.du.getDescT(fieldIdItem
							.getFieldType().getTypeDescriptor());
					final F f = ownerT.getF(fieldIdItem.getFieldName()
							.getStringValue(), fieldT);
					f.markAf(AF.STATIC);

					if (t != T.AREF && !t.equals(fieldT)) {
						LOGGER.warning("SGET TODO Must check compatibility here? T '"
								+ t + "' not of '" + fieldT + "'!");
					}

					this.operations.add(new GET(pc, code, line, f));

					this.operations.add(new STORE(pc, code, line, fieldT, instr
							.getRegisterA()));
				}
				break;
			/********
			 * GOTO *
			 ********/
			case GOTO: {
				final Instruction10t instr = (Instruction10t) instruction;

				t = T.VOID;
				iValue = instr.getTargetAddressOffset();
			}
			// fall through
			case GOTO_16:
				if (t == null) {
					final Instruction20t instr = (Instruction20t) instruction;

					t = T.VOID;
					iValue = instr.getTargetAddressOffset();
				}
				// fall through
			case GOTO_32:
				if (t == null) {
					final Instruction30t instr = (Instruction30t) instruction;

					t = T.VOID;
					instr.getTargetAddressOffset();
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
			/********
			 * JCMP *
			 ********/
			case IF_EQ:
				t = T.INT;
				iValue = CompareType.T_EQ;
				// fall through
			case IF_GE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case IF_GT:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case IF_LE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case IF_LT:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case IF_NE:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_NE;
				}
				{
					// IF A cond B JMP offset
					final Instruction22t instr = (Instruction22t) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					final JCMP op = new JCMP(pc, code, line, t, iValue);
					final int targetPc = opPc + instr.getTargetAddressOffset();
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
			case IF_EQZ:
				t = T.INT;
				iValue = CompareType.T_EQ;
				// fall through
			case IF_GEZ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case IF_GTZ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case IF_LEZ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case IF_LTZ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case IF_NEZ:
				if (t == null) {
					t = T.INT;
					iValue = CompareType.T_NE;
				}
				{
					// IF A cond 0 JMP offset
					final Instruction21t instr = (Instruction21t) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));

					final JCND op = new JCND(pc, code, line, t, iValue);
					final int targetPc = opPc + instr.getTargetAddressOffset();
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
				}
				break;
			case INT_TO_BYTE:
			case INT_TO_CHAR:
			case INT_TO_DOUBLE:
			case INT_TO_FLOAT:
			case INT_TO_LONG:
			case INT_TO_SHORT: {
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			/**********
			 * INVOKE *
			 **********/
			case INVOKE_DIRECT:
				// constructor or supermethod callout
			case INVOKE_INTERFACE:
			case INVOKE_STATIC:
			case INVOKE_VIRTUAL: {
				final Instruction35c instr = (Instruction35c) instruction;

				final MethodIdItem methodIdItem = (MethodIdItem) instr
						.getReferencedItem();
				final T ownerT = this.du.getDescT(methodIdItem
						.getContainingClass().getTypeDescriptor());
				if (instruction.opcode == Opcode.INVOKE_INTERFACE) {
					ownerT.markAf(AF.INTERFACE);
				}
				final M invokeM = ownerT.getM(methodIdItem.getMethodName()
						.getStringValue(), methodIdItem.getPrototype()
						.getPrototypeString());
				T[] paramTs = invokeM.getParamTs();
				if (instruction.opcode == Opcode.INVOKE_STATIC) {
					invokeM.markAf(AF.STATIC);
				} else {
					final T[] virtualParamTs = new T[paramTs.length + 1];
					System.arraycopy(paramTs, 0, virtualParamTs, 1,
							paramTs.length);
					virtualParamTs[0] = ownerT;
					paramTs = virtualParamTs;
				}
				if (instr.getRegCount() > 0) {
					this.operations.add(new LOAD(pc, code, line, paramTs[0],
							instr.getRegisterD()));
				}
				if (instr.getRegCount() > 1) {
					this.operations.add(new LOAD(pc, code, line, paramTs[1],
							instr.getRegisterE()));
				}
				if (instr.getRegCount() > 2) {
					this.operations.add(new LOAD(pc, code, line, paramTs[2],
							instr.getRegisterF()));
				}
				if (instr.getRegCount() > 3) {
					this.operations.add(new LOAD(pc, code, line, paramTs[3],
							instr.getRegisterG()));
				}
				if (instr.getRegCount() > 4) {
					this.operations.add(new LOAD(pc, code, line, paramTs[4],
							instr.getRegisterA()));
				}
				this.operations.add(new INVOKE(pc, code, line, invokeM,
						instruction.opcode == Opcode.INVOKE_DIRECT));
				break;
			}
			/***********
			 * MONITOR *
			 ***********/
			case MONITOR_ENTER:
				type = MONITOR.T_ENTER;
				// fall through
			case MONITOR_EXIT:
				if (t == null) {
					type = MONITOR.T_EXIT;
				}
				{
					// synchronized A
					final Instruction11x instr = (Instruction11x) instruction;

					this.operations.add(new LOAD(pc, code, line, T.AREF, instr
							.getRegisterA()));

					this.operations.add(new MONITOR(pc, code, line, type));
				}
				break;
			/********
			 * MOVE *
			 ********/
			case MOVE:
				t = T.INT; // TODO multi
				// fall through
			case MOVE_OBJECT:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case MOVE_WIDE:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case MOVE_16:
				t = T.INT; // TODO multi
				// fall through
			case MOVE_OBJECT_16:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case MOVE_WIDE_16:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B
					final Instruction32x instr = (Instruction32x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case MOVE_FROM16:
				t = T.INT; // TODO multi
				// fall through
			case MOVE_OBJECT_FROM16:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case MOVE_WIDE_FROM16:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B
					final Instruction22x instr = (Instruction22x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case MOVE_EXCEPTION:
				if (t == null) {
					t = this.du.getT(Throwable.class);
				}
				// fall through
			case MOVE_RESULT:
				t = T.INT; // TODO multi
				// fall through
			case MOVE_RESULT_OBJECT:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case MOVE_RESULT_WIDE:
				// TODO doesn't follow a method? => POP
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = resultRegister
					final Instruction11x instr = (Instruction11x) instruction;

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			/*******
			 * MUL *
			 *******/
			case MUL_DOUBLE:
				t = T.DOUBLE;
				// fall through
			case MUL_FLOAT:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case MUL_INT:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case MUL_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B * C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new MUL(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case MUL_DOUBLE_2ADDR:
				t = T.DOUBLE;
				// fall through
			case MUL_FLOAT_2ADDR:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case MUL_INT_2ADDR:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case MUL_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A *= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new MUL(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case MUL_INT_LIT8: {
				// A = B * literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new MUL(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case MUL_INT_LIT16: {
				// A = B * literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new MUL(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/*******
			 * NEG *
			 *******/
			case NEG_DOUBLE:
				t = T.DOUBLE;
				// fall through
			case NEG_FLOAT:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case NEG_INT:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case NEG_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = -B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new NEG(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			/*******
			 * NEW *
			 *******/
			case NEW_INSTANCE: {
				// A = new typeIdItem
				final Instruction21c instr = (Instruction21c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem())
						.getTypeDescriptor());

				this.operations.add(new NEW(pc, code, line, t));

				this.operations.add(new STORE(pc, code, line, t, instr
						.getRegisterA()));
				break;
			}
			/************
			 * NEWARRAY *
			 ************/
			case NEW_ARRAY: {
				// A = new referencedItem[B]
				final Instruction22c instr = (Instruction22c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem())
						.getTypeDescriptor());
				// contains dimensions via [

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));

				this.operations.add(new NEWARRAY(pc, code, line, t.getBaseT(),
						t.getDim()));

				this.operations.add(new STORE(pc, code, line, t, instr
						.getRegisterA()));
				break;
			}
			/*******
			 * NOP *
			 *******/
			case NOP:
				// nothing
				break;
			/*******
			 * NOT *
			 *******/
			case NOT_INT:
				t = T.INT;
				// fall through
			case NOT_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = ~B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new PUSH(pc, code, line, t, -1));

					// simulate with A ^ -1
					this.operations.add(new XOR(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			/*******
			 * OR *
			 *******/
			case OR_INT:
				t = T.INT;
				// fall through
			case OR_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B | C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new OR(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case OR_INT_2ADDR:
				t = T.INT;
				// fall through
			case OR_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A |= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new OR(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case OR_INT_LIT8: {
				// A = B | literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new OR(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case OR_INT_LIT16: {
				// A = B | literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new OR(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/********
			 * PUSH *
			 ********/
			case CONST_4: {
				// A = literal
				final Instruction11n instr = (Instruction11n) instruction;

				t = T.AINT;
				iValue = instr.getRegisterA();
				oValue = (int) instr.getLiteral();
			}
			// fall through
			case CONST_16:
				if (t == null) {
					// A = literal
					final Instruction21s instr = (Instruction21s) instruction;

					t = T.AINT;
					iValue = instr.getRegisterA();
					oValue = (int) instr.getLiteral();
				}
				// fall through
			case CONST_HIGH16:
				if (t == null) {
					// A = literal
					final Instruction21h instr = (Instruction21h) instruction;

					t = T.multi(T.INT, T.FLOAT);
					iValue = instr.getRegisterA();
					oValue = (int) instr.getLiteral() << 16;
				}
				// fall through
			case CONST: /* 32 */
				if (t == null) {
					// A = literal
					final Instruction31i instr = (Instruction31i) instruction;

					t = T.multi(T.INT, T.FLOAT);
					iValue = instr.getRegisterA();
					oValue = (int) instr.getLiteral();
				}
				// fall through
			case CONST_WIDE_16:
				if (t == null) {
					// A = literal
					final Instruction21s instr = (Instruction21s) instruction;

					t = T.WIDE;
					iValue = instr.getRegisterA();
					oValue = instr.getLiteral();
				}
				// fall through
			case CONST_WIDE_HIGH16:
				if (t == null) {
					// A = literal
					final Instruction21h instr = (Instruction21h) instruction;

					t = T.WIDE;
					iValue = instr.getRegisterA();
					oValue = instr.getLiteral() << 48;
				}
				// fall through
			case CONST_WIDE_32:
				if (t == null) {
					// A = literal
					final Instruction31i instr = (Instruction31i) instruction;

					t = T.WIDE;
					iValue = instr.getRegisterA();
					oValue = instr.getLiteral();
				}
				// fall through
			case CONST_WIDE: /* _64 */
				if (t == null) {
					// A = literal
					final Instruction51l instr = (Instruction51l) instruction;

					t = T.WIDE;
					iValue = instr.getRegisterA();
					oValue = instr.getLiteral();
				}
				// fall through
			case CONST_CLASS:
				if (t == null) {
					// A = literal
					final Instruction21c instr = (Instruction21c) instruction;

					t = this.du.getT(Class.class);
					iValue = instr.getRegisterA();
					oValue = this.du.getDescT(((TypeIdItem) instr
							.getReferencedItem()).getTypeDescriptor());
				}
				// fall through
			case CONST_STRING:
				if (t == null) {
					// A = literal
					final Instruction21c instr = (Instruction21c) instruction;

					t = this.du.getT(String.class);
					iValue = instr.getRegisterA();
					oValue = ((StringIdItem) instr.getReferencedItem())
							.getStringValue();
				}
			case CONST_STRING_JUMBO:
				if (t == null) {
					// A = literal
					final Instruction31c instr = (Instruction31c) instruction;

					t = this.du.getT(String.class);
					iValue = instr.getRegisterA();
					oValue = ((StringIdItem) instr.getReferencedItem())
							.getStringValue();
				}
				{
					this.operations.add(new PUSH(pc, code, line, t, oValue));

					this.operations.add(new STORE(pc, code, line, t, iValue));
				}
				break;
			/*******
			 * PUT *
			 *******/
			case IPUT:
			case IPUT_VOLATILE:
				t = T.INT;
				// fall through
			case IPUT_BOOLEAN:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case IPUT_BYTE:
				if (t == null) {
					t = T.BYTE;
				}
				// fall through
			case IPUT_CHAR:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case IPUT_OBJECT:
			case IPUT_OBJECT_VOLATILE:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case IPUT_SHORT:
				if (t == null) {
					t = T.SHORT;
				}
				// fall through
			case IPUT_WIDE:
			case IPUT_WIDE_VOLATILE:
				if (t == null) {
					t = T.WIDE;
				}
				// case IPUT_OBJECT_QUICK:
				// case IPUT_QUICK:
				// case IPUT_WIDE_QUICK:
				{
					// B.fieldIdItem = A
					final Instruction22c instr = (Instruction22c) instruction;

					final FieldIdItem fieldIdItem = (FieldIdItem) instr
							.getReferencedItem();
					final T ownerT = this.du.getDescT(fieldIdItem
							.getContainingClass().getTypeDescriptor());
					final T fieldT = this.du.getDescT(fieldIdItem
							.getFieldType().getTypeDescriptor());
					final F f = ownerT.getF(fieldIdItem.getFieldName()
							.getStringValue(), fieldT);

					if (t != T.AREF && !t.equals(fieldT)) {
						LOGGER.warning("IPUT TODO Must check compatibility here? T '"
								+ t + "' not of '" + fieldT + "'!");
					}

					this.operations.add(new LOAD(pc, code, line, ownerT, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, fieldT, instr
							.getRegisterA()));

					this.operations.add(new PUT(pc, code, line, f));
				}
				break;
			case SPUT:
			case SPUT_VOLATILE:
				t = T.INT;
				// fall through
			case SPUT_BOOLEAN:
				if (t == null) {
					t = T.BOOLEAN;
				}
				// fall through
			case SPUT_BYTE:
				if (t == null) {
					t = T.BYTE;
				}
				// fall through
			case SPUT_CHAR:
				if (t == null) {
					t = T.CHAR;
				}
				// fall through
			case SPUT_OBJECT:
			case SPUT_OBJECT_VOLATILE:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case SPUT_SHORT:
				if (t == null) {
					t = T.SHORT;
				}
				// fall through
			case SPUT_WIDE:
			case SPUT_WIDE_VOLATILE:
				if (t == null) {
					t = T.WIDE;
				}
				// case IPUT_OBJECT_QUICK:
				// case IPUT_QUICK:
				// case IPUT_WIDE_QUICK:
				{
					// fieldIdItem = A
					final Instruction21c instr = (Instruction21c) instruction;

					final FieldIdItem fieldIdItem = (FieldIdItem) instr
							.getReferencedItem();
					final T ownerT = this.du.getDescT(fieldIdItem
							.getContainingClass().getTypeDescriptor());
					final T fieldT = this.du.getDescT(fieldIdItem
							.getFieldType().getTypeDescriptor());
					final F f = ownerT.getF(fieldIdItem.getFieldName()
							.getStringValue(), fieldT);
					f.markAf(AF.STATIC);

					if (t != T.AREF && !t.equals(fieldT)) {
						LOGGER.warning("SPUT TODO Must check compatibility here? T '"
								+ t + "' not of '" + fieldT + "'!");
					}

					this.operations.add(new LOAD(pc, code, line, fieldT, instr
							.getRegisterA()));

					this.operations.add(new PUT(pc, code, line, f));
				}
				break;
			/*******
			 * REM *
			 *******/
			case REM_DOUBLE:
				t = T.DOUBLE;
				// fall through
			case REM_FLOAT:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case REM_INT:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case REM_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A := B % C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new REM(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case REM_DOUBLE_2ADDR:
				t = T.DOUBLE;
				// fall through
			case REM_FLOAT_2ADDR:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case REM_INT_2ADDR:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case REM_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A %= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new REM(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case REM_INT_LIT8: {
				// A = B % literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new REM(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case REM_INT_LIT16: {
				// A = B % literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new REM(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/**********
			 * RETURN *
			 **********/
			case RETURN:
				t = T.INT; // TODO Multi simple
				// fall through
			case RETURN_OBJECT:
				if (t == null) {
					t = T.AREF;
				}
				// fall through
			case RETURN_WIDE:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// return A
					final Instruction11x instr = (Instruction11x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));

					this.operations.add(new RETURN(pc, code, line, t));
					break;
				}
			case RETURN_VOID: {
				this.operations.add(new RETURN(pc, code, line, T.VOID));
				break;
			}
			/*******
			 * SHL *
			 *******/
			case SHL_INT:
				t = T.INT;
				// fall through
			case SHL_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A := B << C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new SHL(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case SHL_INT_2ADDR:
				t = T.INT;
				// fall through
			case SHL_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A <<= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new SHL(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case SHL_INT_LIT8: {
				// A = B << literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new SHL(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/*******
			 * SHR *
			 *******/
			case SHR_INT:
				t = T.INT;
				// fall through
			case SHR_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B >> C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new SHR(pc, code, line, t, false));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case SHR_INT_2ADDR:
				t = T.INT;
				// fall through
			case SHR_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A >>= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new SHR(pc, code, line, t, false));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case SHR_INT_LIT8: {
				// A = B >> literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new SHR(pc, code, line, T.INT, false));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case USHR_INT:
				t = T.INT;
				// fall through
			case USHR_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B >>> C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new SHR(pc, code, line, t, true));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case USHR_INT_2ADDR:
				t = T.INT;
				// fall through
			case USHR_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A >>>= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new SHR(pc, code, line, t, true));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case USHR_INT_LIT8: {
				// A = B >>> literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new SHR(pc, code, line, T.INT, true));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			/*******
			 * SUB *
			 *******/
			case SUB_DOUBLE:
				t = T.DOUBLE;
				// fall through
			case SUB_FLOAT:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case SUB_INT:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case SUB_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B - C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new SUB(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case SUB_DOUBLE_2ADDR:
				t = T.DOUBLE;
				// fall through
			case SUB_FLOAT_2ADDR:
				if (t == null) {
					t = T.FLOAT;
				}
				// fall through
			case SUB_INT_2ADDR:
				if (t == null) {
					t = T.INT;
				}
				// fall through
			case SUB_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A -= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new SUB(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			/**********
			 * SWITCH *
			 **********/
			case PACKED_SWITCH:
			case SPARSE_SWITCH: {
				// switch(A)
				final Instruction31t instr = (Instruction31t) instruction;

				final SWITCH op = new SWITCH(pc, code, line);
				op.setDefaultPc(this.operations.size() + 1);

				final int targetPc = opPc + instr.getTargetAddressOffset();
				final int pcIndex = getPcIndex(targetPc);
				if (pcIndex < 0) {
					getPcUnresolved(targetPc).add(op);
				} else {
					LOGGER.warning("Switch pseudo operation must have forward target!");
				}
				this.operations.add(op);
				break;
			}
			/*********
			 * THROW *
			 *********/
			case THROW: {
				// throw A
				final Instruction11x instr = (Instruction11x) instruction;

				t = this.du.getT(Throwable.class);

				this.operations.add(new LOAD(pc, code, line, t, instr
						.getRegisterA()));

				this.operations.add(new THROW(pc, code, line));
				break;
			}
			/*******
			 * XOR *
			 *******/
			case XOR_INT:
				t = T.INT;
				// fall through
			case XOR_LONG:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A = B ^ C
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterC()));

					this.operations.add(new XOR(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case XOR_INT_2ADDR:
				t = T.INT;
				// fall through
			case XOR_LONG_2ADDR:
				if (t == null) {
					t = T.LONG;
				}
				{
					// A ^= B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterA()));
					this.operations.add(new LOAD(pc, code, line, t, instr
							.getRegisterB()));

					this.operations.add(new XOR(pc, code, line, t));

					this.operations.add(new STORE(pc, code, line, t, instr
							.getRegisterA()));
				}
				break;
			case XOR_INT_LIT8: {
				// A = B ^ literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new XOR(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			case XOR_INT_LIT16: {
				// A = B ^ literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(pc, code, line, T.INT, instr
						.getRegisterB()));
				this.operations.add(new PUSH(pc, code, line, T.INT, (int) instr
						.getLiteral()));

				this.operations.add(new XOR(pc, code, line, T.INT));

				this.operations.add(new STORE(pc, code, line, T.INT, instr
						.getRegisterA()));
				break;
			}
			default:
				throw new RuntimeException("Unknown jvm operation code '0x"
						+ Integer.toHexString(code) + "'!");
			}
			opPc += instruction.getSize(opPc);
		}
		cfg.setOperations(this.operations.toArray(new Operation[this.operations
				.size()]));

		final TryItem[] tryItems = codeItem.getTries();
		if (tryItems != null && tryItems.length > 0) {
			final ArrayList<Exc> excs = new ArrayList<Exc>();
			// preserve order
			for (int i = 0; i < tryItems.length; ++i) {
				final TryItem tryItem = tryItems[i];
				for (final EncodedTypeAddrPair handler : tryItem.encodedCatchHandler.handlers) {
					final T catchT = this.du.getDescT(handler.exceptionType
							.getTypeDescriptor());
					final Exc exc = new Exc(catchT);
					exc.setStartPc(tryItem.getStartCodeAddress());
					exc.setEndPc(tryItem.getStartCodeAddress()
							+ tryItem.getTryLength());
					exc.setHandlerPc(handler.getHandlerAddress());
					excs.add(exc);
				}
				if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() != -1) {
					final Exc exc = new Exc(null);
					exc.setStartPc(tryItem.getStartCodeAddress());
					exc.setEndPc(tryItem.getStartCodeAddress()
							+ tryItem.getTryLength());
					exc.setHandlerPc(tryItem.encodedCatchHandler
							.getCatchAllHandlerAddress());
					excs.add(exc);
				}
			}
			cfg.setExcs(excs.toArray(new Exc[excs.size()]));
		}
	}

	private void visitPc(final int pc, final Instruction instruction) {
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
		// final int labelUnknownIndex = pcIndex;
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

				if (instruction instanceof PackedSwitchDataPseudoInstruction) {
					final PackedSwitchDataPseudoInstruction instr = (PackedSwitchDataPseudoInstruction) instruction;
					final int firstKey = instr.getFirstKey();
					// absolute original pcs
					final int[] targets = instr.getTargets();

					final int[] caseKeys = new int[targets.length];
					final int[] casePcs = new int[targets.length];
					for (int t = 0; t < targets.length; ++t) {
						caseKeys[t] = firstKey + t;
						casePcs[t] = getPcIndex(targets[t]);
						if (casePcs[t] < 0) {
							getPcUnresolved(targets[t]).add(op);
						}
					}
					op.setCaseKeys(caseKeys);
					op.setCasePcs(casePcs);
					continue;
				}
				if (instruction instanceof SparseSwitchDataPseudoInstruction) {
					final SparseSwitchDataPseudoInstruction instr = (SparseSwitchDataPseudoInstruction) instruction;
					final int[] keys = instr.getKeys();
					// absolute original pcs
					final int[] targets = instr.getTargets();

					final int[] caseKeys = new int[targets.length];
					final int[] casePcs = new int[targets.length];
					for (int t = 0; t < targets.length; ++t) {
						caseKeys[t] = keys[t];
						casePcs[t] = getPcIndex(targets[t]);
						if (casePcs[t] < 0) {
							getPcUnresolved(targets[t]).add(op);
						}
					}
					op.setCaseKeys(caseKeys);
					op.setCasePcs(casePcs);
					continue;
				}
				LOGGER.warning("Unresolved switch target isn't a SwitchDataPseudoInstruction!");
				continue;
			}
			// cannot happen for Exc / Var here
		}
	}

}