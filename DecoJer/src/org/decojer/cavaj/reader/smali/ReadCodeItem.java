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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

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
import org.decojer.cavaj.model.code.op.FILLARRAY;
import org.decojer.cavaj.model.code.op.GET;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.INSTANCEOF;
import org.decojer.cavaj.model.code.op.INVOKE;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
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
import org.decojer.cavaj.model.code.op.RETURN;
import org.decojer.cavaj.model.code.op.SHL;
import org.decojer.cavaj.model.code.op.SHR;
import org.decojer.cavaj.model.code.op.STORE;
import org.decojer.cavaj.model.code.op.SUB;
import org.decojer.cavaj.model.code.op.SWITCH;
import org.decojer.cavaj.model.code.op.THROW;
import org.decojer.cavaj.model.code.op.XOR;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.CodeItem.EncodedTypeAddrPair;
import org.jf.dexlib.CodeItem.TryItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.ArrayDataPseudoInstruction;
import org.jf.dexlib.Code.Format.ArrayDataPseudoInstruction.ArrayElement;
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
import org.jf.dexlib.Code.Format.Instruction3rc;
import org.jf.dexlib.Code.Format.Instruction51l;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;

/**
 * Smali read code item.
 * 
 * @author André Pankraz
 */
public class ReadCodeItem {

	private final static Logger LOGGER = Logger.getLogger(ReadCodeItem.class.getName());

	private final DU du;

	final ArrayList<Op> ops = new ArrayList<Op>();

	private final HashMap<Integer, Integer> vmpc2pc = new HashMap<Integer, Integer>();

	private final HashMap<Integer, ArrayList<Object>> vmpc2unresolved = new HashMap<Integer, ArrayList<Object>>();

	private final ReadDebugInfo readDebugInfo;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadCodeItem(final DU du) {
		assert du != null;

		this.du = du;
		this.readDebugInfo = new ReadDebugInfo(du);
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
	 * @param codeItem
	 *            smali code item
	 */
	public void initAndVisit(final MD md, final CodeItem codeItem) {
		this.ops.clear();
		this.vmpc2pc.clear();
		this.vmpc2unresolved.clear();

		final CFG cfg = new CFG(md, codeItem.getRegisterCount(), 0);
		md.setCFG(cfg);

		// read debug info here, need lines early, but handle read vars after code
		this.readDebugInfo.initAndVisit(md, codeItem.getDebugInfo());

		final Instruction[] instructions = codeItem.getInstructions();

		// 2 free to use work register, 3 parameter
		// static: (5 register)
		// work_register1...work_register_2...param1...param2...param3
		// dynamic: (6 register)
		// work_register1...work_register_2...this...param1...param2...param3

		// invoke(range) or fill-new-array result type
		T moveInvokeResultT = null;

		Instruction instruction;
		int vmpc = 0;
		for (int i = 0, line = -1, opcode = -1; i < instructions.length; ++i, vmpc += instruction
				.getSize(vmpc)) {
			instruction = instructions[i];

			// visit vmpc later, not for automatically generated follow POP!
			T t = null;

			switch (instruction.opcode) {
			case MOVE_RESULT:
				// Move the single-word non-object result of the most recent invoke-kind into the
				// indicated register. This must be done as the instruction immediately after an
				// invoke-kind whose (single-word, non-object) result is not to be ignored;
				// anywhere else is invalid.
				t = T.SINGLE;
				// fall through
			case MOVE_RESULT_OBJECT:
				// Move the object result of the most recent invoke-kind into the indicated
				// register. This must be done as the instruction immediately after an invoke-kind
				// or filled-new-array whose (object) result is not to be ignored; anywhere else
				// is invalid.
				if (t == null) {
					t = T.REF;
				}
				// fall through
			case MOVE_RESULT_WIDE:
				// Move the double-word result of the most recent invoke-kind into the indicated
				// register pair. This must be done as the instruction immediately after an
				// invoke-kind whose (double-word) result is not to be ignored; anywhere else is
				// invalid.
				if (t == null) {
					t = T.WIDE;
				}
				{
					// regular instruction, visit and remember vmpc here
					visitVmpc(vmpc, instruction);

					// A = resultRegister
					final Instruction11x instr = (Instruction11x) instruction;

					if (moveInvokeResultT == null) {
						LOGGER.warning("Move result without previous result type!");
						moveInvokeResultT = T.REF;
					}

					this.ops.add(new STORE(this.ops.size(), opcode, line, moveInvokeResultT, instr
							.getRegisterA()));

					moveInvokeResultT = null;
					// next instruction, done for this round
					continue;
				}
			default:
				if (moveInvokeResultT != null) {
					moveInvokeResultT = null;

					// automatically generated follow instruction, don't visit and remember vmpc!!!

					// no POP2 with current wide handling
					this.ops.add(new POP(this.ops.size(), opcode, line, POP.Kind.POP));
				}
			}

			visitVmpc(vmpc, instruction);
			line = this.readDebugInfo.getLine(vmpc);
			opcode = instruction.opcode.value;

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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new ADD(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new ADD(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case ADD_INT_LIT8: {
				// A = B + literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new ADD(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case ADD_INT_LIT16: {
				// A = B + literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new ADD(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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
					t = T.REF;
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

					// TODO array type?
					this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr
							.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr
							.getRegisterC()));

					this.ops.add(new ALOAD(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new AND(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new AND(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case AND_INT_LIT8: {
				// A = B & literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new AND(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case AND_INT_LIT16: {
				// A = B & literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new AND(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			/***************
			 * ARRAYLENGTH *
			 ***************/
			case ARRAY_LENGTH: {
				// A = B.length
				final Instruction12x instr = (Instruction12x) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr.getRegisterB()));

				this.ops.add(new ARRAYLENGTH(this.ops.size(), opcode, line));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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
					t = T.REF;
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

					// TODO array type?
					this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr
							.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr
							.getRegisterC()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));

					this.ops.add(new ASTORE(this.ops.size(), opcode, line, t));
				}
				break;
			/********
			 * CAST *
			 ********/
			case CHECK_CAST: {
				// A = (typeIdItem) A
				final Instruction21c instr = (Instruction21c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem()).getTypeDescriptor());

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr.getRegisterA()));

				this.ops.add(new CAST(this.ops.size(), opcode, line, T.REF, t));

				this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				break;
			}
			case DOUBLE_TO_FLOAT:
				t = T.DOUBLE;
				oValue = T.FLOAT;
				// fall through
			case DOUBLE_TO_INT:
				if (t == null) {
					t = T.DOUBLE;
					oValue = T.INT;
				}
				// fall through
			case DOUBLE_TO_LONG:
				if (t == null) {
					t = T.DOUBLE;
					oValue = T.LONG;
				}
				// fall through
			case INT_TO_BYTE:
				if (t == null) {
					t = T.INT;
					oValue = T.BYTE;
				}
				// fall through
			case INT_TO_CHAR:
				if (t == null) {
					t = T.INT;
					oValue = T.CHAR;
				}
				// fall through
			case INT_TO_DOUBLE:
				if (t == null) {
					t = T.INT;
					oValue = T.DOUBLE;
				}
				// fall through
			case INT_TO_FLOAT:
				if (t == null) {
					t = T.INT;
					oValue = T.FLOAT;
				}
				// fall through
			case INT_TO_LONG:
				if (t == null) {
					t = T.INT;
					oValue = T.LONG;
				}
				// fall through
			case INT_TO_SHORT:
				if (t == null) {
					t = T.INT;
					oValue = T.SHORT;
				}
				// fall through
			case FLOAT_TO_DOUBLE:
				if (t == null) {
					t = T.FLOAT;
					oValue = T.DOUBLE;
				}
				// fall through
			case FLOAT_TO_INT:
				if (t == null) {
					t = T.FLOAT;
					oValue = T.INT;
				}
				// fall through
			case FLOAT_TO_LONG:
				if (t == null) {
					t = T.FLOAT;
					oValue = T.LONG;
				}
				// fall through
			case LONG_TO_DOUBLE:
				if (t == null) {
					t = T.LONG;
					oValue = T.DOUBLE;
				}
				// fall through
			case LONG_TO_FLOAT:
				if (t == null) {
					t = T.LONG;
					oValue = T.FLOAT;
				}
				// fall through
			case LONG_TO_INT:
				if (t == null) {
					t = T.LONG;
					oValue = T.INT;
				}
				{
					// B = (totype) A
					final Instruction12x instr = (Instruction12x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));

					this.ops.add(new CAST(this.ops.size(), opcode, line, t, (T) oValue));

					this.ops.add(new STORE(this.ops.size(), opcode, line, (T) oValue, instr
							.getRegisterA()));
				}
				break;
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new CMP(this.ops.size(), opcode, line, t, iValue));

					this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new DIV(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new DIV(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case DIV_INT_LIT8: {
				// A = B / literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new DIV(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case DIV_INT_LIT16: {
				// A = B / literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new DIV(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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
					t = T.REF;
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

					final FieldIdItem fieldIdItem = (FieldIdItem) instr.getReferencedItem();

					final T valueT = this.du.getDescT(fieldIdItem.getFieldType()
							.getTypeDescriptor());
					if (t != T.REF && !t.equals(valueT)) {
						LOGGER.warning("IGET TODO Must check compatibility here? T '" + t
								+ "' not of '" + valueT + "'!");
					}

					t = this.du.getDescT(fieldIdItem.getContainingClass().getTypeDescriptor());
					final F f = t.getF(fieldIdItem.getFieldName().getStringValue(), valueT);

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new GET(this.ops.size(), opcode, line, f));

					this.ops.add(new STORE(this.ops.size(), opcode, line, valueT, instr
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
					t = T.REF;
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

					final FieldIdItem fieldIdItem = (FieldIdItem) instr.getReferencedItem();

					final T valueT = this.du.getDescT(fieldIdItem.getFieldType()
							.getTypeDescriptor());
					if (t != T.REF && !t.equals(valueT)) {
						LOGGER.warning("SGET TODO Must check compatibility here? T '" + t
								+ "' not of '" + valueT + "'!");
					}

					t = this.du.getDescT(fieldIdItem.getContainingClass().getTypeDescriptor());
					final F f = t.getF(fieldIdItem.getFieldName().getStringValue(), valueT);
					f.markAf(AF.STATIC);

					this.ops.add(new GET(this.ops.size(), opcode, line, f));

					this.ops.add(new STORE(this.ops.size(), opcode, line, valueT, instr
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
			/**************
			 * INSTANCEOF *
			 **************/
			case INSTANCE_OF: {
				// A = B instanceof referencedItem
				final Instruction22c instr = (Instruction22c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem()).getTypeDescriptor());

				// not t, is unknown, result can be false
				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr.getRegisterB()));

				this.ops.add(new INSTANCEOF(this.ops.size(), opcode, line, t));

				// hmmm, "spec" only says none-zero result, multitype?
				this.ops.add(new STORE(this.ops.size(), opcode, line, T.BOOLEAN, instr
						.getRegisterA()));
				break;
			}
			/********
			 * JCMP *
			 ********/
			case IF_EQ:
				t = T.INT;
				oValue = CmpType.T_EQ;
				// fall through
			case IF_GE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GE;
				}
				// fall through
			case IF_GT:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GT;
				}
				// fall through
			case IF_LE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LE;
				}
				// fall through
			case IF_LT:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LT;
				}
				// fall through
			case IF_NE:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_NE;
				}
				{
					// IF A cond B JMP offset
					final Instruction22t instr = (Instruction22t) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					final JCMP op = new JCMP(this.ops.size(), opcode, line, t, (CmpType) oValue);
					this.ops.add(op);
					final int targetVmpc = vmpc + instr.getTargetAddressOffset();
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
			case IF_EQZ:
				t = T.AINT; // boolean too, float separate
				oValue = CmpType.T_EQ;
				// fall through
			case IF_GEZ:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GE;
				}
				// fall through
			case IF_GTZ:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_GT;
				}
				// fall through
			case IF_LEZ:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LE;
				}
				// fall through
			case IF_LTZ:
				if (t == null) {
					t = T.INT;
					oValue = CmpType.T_LT;
				}
				// fall through
			case IF_NEZ:
				if (t == null) {
					t = T.AINT; // boolean too
					oValue = CmpType.T_NE;
				}
				{
					// IF A cond 0 JMP offset
					final Instruction21t instr = (Instruction21t) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));

					final JCND op = new JCND(this.ops.size(), opcode, line, t, (CmpType) oValue);
					this.ops.add(op);
					final int targetVmpc = vmpc + instr.getTargetAddressOffset();
					final int targetPc = getPc(targetVmpc);
					op.setTargetPc(targetPc);
					if (targetPc < 0) {
						getUnresolved(targetVmpc).add(op);
					}
				}
				break;
			/**********
			 * INVOKE *
			 **********/
			case INVOKE_DIRECT:
				// e.g. constructor callout
			case INVOKE_INTERFACE:
			case INVOKE_STATIC:
			case INVOKE_SUPER:
			case INVOKE_VIRTUAL: {
				final Instruction35c instr = (Instruction35c) instruction;

				final int[] regs = new int[instr.getRegCount()];
				if (instr.getRegCount() > 0) {
					regs[0] = instr.getRegisterD();
				}
				if (instr.getRegCount() > 1) {
					regs[1] = instr.getRegisterE();
				}
				if (instr.getRegCount() > 2) {
					regs[2] = instr.getRegisterF();
				}
				if (instr.getRegCount() > 3) {
					regs[3] = instr.getRegisterG();
				}
				if (instr.getRegCount() > 4) {
					regs[4] = instr.getRegisterA();
				}
				int reg = 0;

				final MethodIdItem methodIdItem = (MethodIdItem) instr.getReferencedItem();
				t = this.du.getDescT(methodIdItem.getContainingClass().getTypeDescriptor());
				if (instruction.opcode == Opcode.INVOKE_INTERFACE) {
					t.markAf(AF.INTERFACE);
				}
				final M invokeM = t.getM(methodIdItem.getMethodName().getStringValue(),
						methodIdItem.getPrototype().getPrototypeString());
				if (instruction.opcode == Opcode.INVOKE_STATIC) {
					invokeM.markAf(AF.STATIC);
				} else {
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, regs[reg++]));
				}

				for (int j = 0; j < invokeM.getParams(); ++j) {
					final T paramT = invokeM.getParamT(j);
					this.ops.add(new LOAD(this.ops.size(), opcode, line, paramT, regs[reg++]));
					if (paramT.isWide()) {
						++reg;
					}
				}

				this.ops.add(new INVOKE(this.ops.size(), opcode, line, invokeM,
						instruction.opcode == Opcode.INVOKE_DIRECT));
				if (invokeM.getReturnT() != T.VOID) {
					moveInvokeResultT = invokeM.getReturnT();
				}
				break;
			}
			case INVOKE_DIRECT_RANGE:
				// e.g. constructor callout
			case INVOKE_INTERFACE_RANGE:
			case INVOKE_STATIC_RANGE:
			case INVOKE_SUPER_RANGE:
			case INVOKE_VIRTUAL_RANGE: {
				final Instruction3rc instr = (Instruction3rc) instruction;

				int reg = instr.getStartRegister();

				final MethodIdItem methodIdItem = (MethodIdItem) instr.getReferencedItem();
				t = this.du.getDescT(methodIdItem.getContainingClass().getTypeDescriptor());
				if (instruction.opcode == Opcode.INVOKE_INTERFACE_RANGE) {
					t.markAf(AF.INTERFACE);
				}
				final M invokeM = t.getM(methodIdItem.getMethodName().getStringValue(),
						methodIdItem.getPrototype().getPrototypeString());
				if (instruction.opcode == Opcode.INVOKE_STATIC_RANGE) {
					invokeM.markAf(AF.STATIC);
				} else {
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, reg++));
				}

				for (int j = 0; j < invokeM.getParams(); ++j) {
					final T paramT = invokeM.getParamT(j);
					this.ops.add(new LOAD(this.ops.size(), opcode, line, paramT, reg++));
					if (paramT.isWide()) {
						++reg;
					}
				}

				this.ops.add(new INVOKE(this.ops.size(), opcode, line, invokeM,
						instruction.opcode == Opcode.INVOKE_DIRECT_RANGE));
				if (invokeM.getReturnT() != T.VOID) {
					moveInvokeResultT = invokeM.getReturnT();
				}
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr
							.getRegisterA()));

					this.ops.add(new MONITOR(this.ops.size(), opcode, line, type));
				}
				break;
			/********
			 * MOVE *
			 ********/
			case MOVE:
				t = T.SINGLE;
				// fall through
			case MOVE_OBJECT:
				if (t == null) {
					t = T.REF;
				}
				// fall through
			case MOVE_WIDE:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B
					final Instruction12x instr = (Instruction12x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case MOVE_16:
				t = T.SINGLE;
				// fall through
			case MOVE_OBJECT_16:
				if (t == null) {
					t = T.REF;
				}
				// fall through
			case MOVE_WIDE_16:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B
					final Instruction32x instr = (Instruction32x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case MOVE_FROM16:
				t = T.SINGLE;
				// fall through
			case MOVE_OBJECT_FROM16:
				if (t == null) {
					t = T.REF;
				}
				// fall through
			case MOVE_WIDE_FROM16:
				if (t == null) {
					t = T.WIDE;
				}
				{
					// A = B
					final Instruction22x instr = (Instruction22x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case MOVE_EXCEPTION: {
				// Save a just-caught exception into the given register. This should be the first
				// instruction of any exception handler whose caught exception is not to be
				// ignored, and this instruction may only ever occur as the first instruction of an
				// exception handler; anywhere else is invalid.

				// TODO

				// A = resultRegister
				final Instruction11x instr = (Instruction11x) instruction;

				this.ops.add(new STORE(this.ops.size(), opcode, line,
						this.du.getT(Throwable.class), instr.getRegisterA()));
				break;
			}
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new MUL(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new MUL(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case MUL_INT_LIT8: {
				// A = B * literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new MUL(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case MUL_INT_LIT16: {
				// A = B * literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new MUL(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new NEG(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			/*******
			 * NEW *
			 *******/
			case NEW_INSTANCE: {
				// A = new typeIdItem
				final Instruction21c instr = (Instruction21c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem()).getTypeDescriptor());

				this.ops.add(new NEW(this.ops.size(), opcode, line, t));

				this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				break;
			}
			/************
			 * NEWARRAY *
			 ************/
			case NEW_ARRAY: {
				// A = new referencedItem[B]
				final Instruction22c instr = (Instruction22c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem()).getTypeDescriptor());
				// contains dimensions via [

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));

				// not t.getDim() for NEWARRAY! reduce t by 1 dimension
				// => int[][] intArray = new int[10][];
				final T elemT = this.du.getT(t.getName().substring(0, t.getName().length() - 2));
				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, elemT, 1));

				this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				break;
			}
			case FILL_ARRAY_DATA: {
				// fill_array_data(A) -> target
				final Instruction31t instr = (Instruction31t) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr.getRegisterA()));

				final FILLARRAY op = new FILLARRAY(this.ops.size(), opcode, line);
				this.ops.add(op);
				final int targetVmpc = vmpc + instr.getTargetAddressOffset();
				final int targetPc = getPc(targetVmpc);
				if (targetPc < 0) {
					getUnresolved(targetVmpc).add(op);
				} else {
					LOGGER.warning("Array pseudo operation must have forward target!");
				}
				break;
			}
			case FILLED_NEW_ARRAY: {
				// A = new referencedItem[] {D, E, F, G, A}
				final Instruction35c instr = (Instruction35c) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem()).getTypeDescriptor());
				// contains dimensions via [

				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr.getRegCount()));

				// not t.getDim() for NEWARRAY! reduce t by 1 dimension
				// => int[][] intArray = new int[10][];
				final T elemT = this.du.getT(t.getName().substring(0, t.getName().length() - 2));
				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, elemT, 1));

				final Object[] regs = new Object[instr.getRegCount()];
				if (instr.getRegCount() > 0) {
					regs[0] = instr.getRegisterD();
				}
				if (instr.getRegCount() > 1) {
					regs[1] = instr.getRegisterE();
				}
				if (instr.getRegCount() > 2) {
					regs[2] = instr.getRegisterF();
				}
				if (instr.getRegCount() > 3) {
					regs[3] = instr.getRegisterG();
				}
				if (instr.getRegCount() > 4) {
					regs[4] = instr.getRegisterA();
				}

				this.ops.add(new DUP(this.ops.size(), opcode, line, DUP.Kind.DUP));

				final FILLARRAY op = new FILLARRAY(this.ops.size(), opcode, line);
				this.ops.add(op);
				op.setValues(regs);

				moveInvokeResultT = t;
				break;
			}
			case FILLED_NEW_ARRAY_RANGE: {
				// A = new referencedItem[] {D, E, F, G, A}
				final Instruction3rc instr = (Instruction3rc) instruction;

				t = this.du.getDescT(((TypeIdItem) instr.getReferencedItem()).getTypeDescriptor());
				// contains dimensions via [

				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr.getRegCount()));

				// not t.getDim() for NEWARRAY! reduce t by 1 dimension
				// => int[][] intArray = new int[10][];
				final T elemT = this.du.getT(t.getName().substring(0, t.getName().length() - 2));
				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, elemT, 1));

				final Object[] regs = new Object[instr.getRegCount()];
				for (int reg = instr.getStartRegister(), j = 0; j < instr.getRegCount(); ++reg, ++j) {
					regs[j] = reg; // TODO wide?
				}

				this.ops.add(new DUP(this.ops.size(), opcode, line, DUP.Kind.DUP));

				final FILLARRAY op = new FILLARRAY(this.ops.size(), opcode, line);
				this.ops.add(op);
				op.setValues(regs);

				moveInvokeResultT = t;
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new PUSH(this.ops.size(), opcode, line, t, -1));

					// simulate with A ^ -1
					this.ops.add(new XOR(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new OR(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new OR(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case OR_INT_LIT8: {
				// A = B | literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new OR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case OR_INT_LIT16: {
				// A = B | literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new OR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			/********
			 * PUSH *
			 ********/
			case CONST_4: {
				// A = literal
				final Instruction11n instr = (Instruction11n) instruction;

				oValue = iValue = (int) instr.getLiteral();
				t = T.getDalvikIntT(iValue);
				iValue = instr.getRegisterA();
			}
			// fall through
			case CONST_16:
				if (t == null) {
					// A = literal
					final Instruction21s instr = (Instruction21s) instruction;

					oValue = iValue = (int) instr.getLiteral();
					t = T.getDalvikIntT(iValue);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_HIGH16:
				if (t == null) {
					// A = literal
					final Instruction21h instr = (Instruction21h) instruction;

					oValue = iValue = (int) instr.getLiteral() << 16;
					t = T.getDalvikIntT(iValue);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST: /* 32 */
				if (t == null) {
					// A = literal
					final Instruction31i instr = (Instruction31i) instruction;

					oValue = iValue = (int) instr.getLiteral();
					t = T.getDalvikIntT(iValue);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE_16:
				if (t == null) {
					// A = literal
					final Instruction21s instr = (Instruction21s) instruction;

					oValue = instr.getLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE_HIGH16:
				if (t == null) {
					// A = literal
					final Instruction21h instr = (Instruction21h) instruction;

					oValue = instr.getLiteral() << 48;
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE_32:
				if (t == null) {
					// A = literal
					final Instruction31i instr = (Instruction31i) instruction;

					oValue = instr.getLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE: /* _64 */
				if (t == null) {
					// A = literal
					final Instruction51l instr = (Instruction51l) instruction;

					oValue = instr.getLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_CLASS:
				if (t == null) {
					// A = literal
					final Instruction21c instr = (Instruction21c) instruction;

					oValue = this.du.getDescT(((TypeIdItem) instr.getReferencedItem())
							.getTypeDescriptor());
					t = this.du.getT(Class.class);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_STRING:
				if (t == null) {
					// A = literal
					final Instruction21c instr = (Instruction21c) instruction;

					oValue = ((StringIdItem) instr.getReferencedItem()).getStringValue();
					t = this.du.getT(String.class);
					iValue = instr.getRegisterA();
				}
			case CONST_STRING_JUMBO:
				if (t == null) {
					// A = literal
					final Instruction31c instr = (Instruction31c) instruction;

					oValue = ((StringIdItem) instr.getReferencedItem()).getStringValue();
					t = this.du.getT(String.class);
					iValue = instr.getRegisterA();
				}
				{
					this.ops.add(new PUSH(this.ops.size(), opcode, line, t, oValue));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, iValue));
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
					t = T.REF;
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

					final FieldIdItem fieldIdItem = (FieldIdItem) instr.getReferencedItem();

					final T valueT = this.du.getDescT(fieldIdItem.getFieldType()
							.getTypeDescriptor());
					if (t != T.REF && !t.equals(valueT)) {
						LOGGER.warning("IPUT TODO Must check compatibility here? T '" + t
								+ "' not of '" + valueT + "'!");
					}

					t = this.du.getDescT(fieldIdItem.getContainingClass().getTypeDescriptor());
					final F f = t.getF(fieldIdItem.getFieldName().getStringValue(), valueT);

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, valueT, instr
							.getRegisterA()));

					this.ops.add(new PUT(this.ops.size(), opcode, line, f));
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
					t = T.REF;
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

					final FieldIdItem fieldIdItem = (FieldIdItem) instr.getReferencedItem();

					final T valueT = this.du.getDescT(fieldIdItem.getFieldType()
							.getTypeDescriptor());
					if (t != T.REF && !t.equals(valueT)) {
						LOGGER.warning("SPUT TODO Must check compatibility here? T '" + t
								+ "' not of '" + valueT + "'!");
					}

					t = this.du.getDescT(fieldIdItem.getContainingClass().getTypeDescriptor());
					final F f = t.getF(fieldIdItem.getFieldName().getStringValue(), valueT);
					f.markAf(AF.STATIC);

					this.ops.add(new LOAD(this.ops.size(), opcode, line, valueT, instr
							.getRegisterA()));

					this.ops.add(new PUT(this.ops.size(), opcode, line, f));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new REM(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new REM(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case REM_INT_LIT8: {
				// A = B % literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new REM(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case REM_INT_LIT16: {
				// A = B % literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new REM(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			/**********
			 * RETURN *
			 **********/
			case RETURN:
				t = T.SINGLE;
				// fall through
			case RETURN_OBJECT:
				if (t == null) {
					t = T.REF;
				}
				// fall through
			case RETURN_WIDE:
				if (t == null) {
					t = T.WIDE;
				}
				{
					if (!t.isAssignableFrom(md.getM().getReturnT())) {
						LOGGER.warning("Incompatible operation return type '" + t
								+ "' for method return type '" + md.getM().getReturnT() + "'!");
					}
					t = md.getM().getReturnT();

					// return A
					final Instruction11x instr = (Instruction11x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));

					this.ops.add(new RETURN(this.ops.size(), opcode, line, t));
				}
				break;
			case RETURN_VOID: {
				this.ops.add(new RETURN(this.ops.size(), opcode, line, T.VOID));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new SHL(this.ops.size(), opcode, line, t, T.INT));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new SHL(this.ops.size(), opcode, line, t, T.INT));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case SHL_INT_LIT8: {
				// A = B << literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new SHL(this.ops.size(), opcode, line, T.INT, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new SHR(this.ops.size(), opcode, line, t, T.INT, false));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new SHR(this.ops.size(), opcode, line, t, T.INT, false));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case SHR_INT_LIT8: {
				// A = B >> literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new SHR(this.ops.size(), opcode, line, T.INT, T.INT, false));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new SHR(this.ops.size(), opcode, line, t, T.INT, true));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new SHR(this.ops.size(), opcode, line, t, T.INT, true));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case USHR_INT_LIT8: {
				// A = B >>> literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new SHR(this.ops.size(), opcode, line, T.INT, T.INT, true));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new SUB(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new SUB(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			/**********
			 * SWITCH *
			 **********/
			case PACKED_SWITCH:
			case SPARSE_SWITCH: {
				// switch(A)
				final Instruction31t instr = (Instruction31t) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));

				final SWITCH op = new SWITCH(this.ops.size(), opcode, line);
				this.ops.add(op);
				op.setDefaultPc(this.ops.size());
				final int targetVmpc = vmpc + instr.getTargetAddressOffset();
				final int targetPc = getPc(targetVmpc);
				if (targetPc < 0) {
					getUnresolved(targetVmpc).add(op);
				} else {
					LOGGER.warning("Switch pseudo operation must have forward target!");
				}
				break;
			}
			/*********
			 * THROW *
			 *********/
			case THROW: {
				// throw A
				final Instruction11x instr = (Instruction11x) instruction;

				t = this.du.getT(Throwable.class);

				this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));

				this.ops.add(new THROW(this.ops.size(), opcode, line));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterC()));

					this.ops.add(new XOR(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					this.ops.add(new XOR(this.ops.size(), opcode, line, t));

					this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				}
				break;
			case XOR_INT_LIT8: {
				// A = B ^ literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new XOR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case XOR_INT_LIT16: {
				// A = B ^ literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, (int) instr
						.getLiteral()));

				this.ops.add(new XOR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			default:
				throw new RuntimeException("Unknown jvm operation code '0x"
						+ Integer.toHexString(opcode & 0xff) + "'!");
			}
		}
		visitVmpc(vmpc, null);
		cfg.setOps(this.ops.toArray(new Op[this.ops.size()]));

		final TryItem[] tryItems = codeItem.getTries();
		if (tryItems != null && tryItems.length > 0) {
			final ArrayList<Exc> excs = new ArrayList<Exc>();
			// preserve order
			for (int i = 0; i < tryItems.length; ++i) {
				final TryItem tryItem = tryItems[i];
				for (final EncodedTypeAddrPair handler : tryItem.encodedCatchHandler.handlers) {
					final Exc exc = new Exc(this.du.getDescT(handler.exceptionType
							.getTypeDescriptor()));
					exc.setStartPc(this.vmpc2pc.get(tryItem.getStartCodeAddress()));
					exc.setEndPc(this.vmpc2pc.get(tryItem.getStartCodeAddress()
							+ tryItem.getTryLength()));
					exc.setHandlerPc(this.vmpc2pc.get(handler.getHandlerAddress()));
					excs.add(exc);
				}
				if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() != -1) {
					final Exc exc = new Exc(null);
					exc.setStartPc(this.vmpc2pc.get(tryItem.getStartCodeAddress()));
					exc.setEndPc(this.vmpc2pc.get(tryItem.getStartCodeAddress()
							+ tryItem.getTryLength()));
					exc.setHandlerPc(this.vmpc2pc.get(tryItem.encodedCatchHandler
							.getCatchAllHandlerAddress()));
					excs.add(exc);
				}
			}
			cfg.setExcs(excs.toArray(new Exc[excs.size()]));
		}
		readLocalVariables(cfg, this.readDebugInfo);
	}

	private void readLocalVariables(final CFG cfg, final ReadDebugInfo readDebugInfo) {
		final HashMap<Integer, ArrayList<V>> reg2vs = readDebugInfo.getReg2vs();
		for (final Entry<Integer, ArrayList<V>> reg2v : reg2vs.entrySet()) {
			final int reg = reg2v.getKey();
			for (final V v : reg2v.getValue()) {
				final int[] pcs = v.getPcs();
				for (int i = pcs.length; i-- > 0;) {
					if (pcs[i] == -1) {
						// dalvik doesn't encode end pc if locals preserve till method end
						pcs[i] = this.ops.size();
						continue;
					}
					int vmpc = this.vmpc2pc.get(pcs[i]);
					// TODO really necessary???
					// find end, must find because multiple ops could be created
					for (int j = 0; j < 10; ++j) {
						final Integer pc = this.vmpc2pc.get(++vmpc);
						if (pc != null) {
							pcs[i] = pc - 1;
							break;
						}
					}
				}
				cfg.addVar(reg, v);
			}
		}
		cfg.postProcessVars();
	}

	private void visitVmpc(final int vmpc, final Instruction instruction) {
		final Integer pc = this.vmpc2pc.put(vmpc, this.ops.size());
		if (pc == null) {
			// fresh new label, never referenced before
			return;
		}
		if (pc > 0) {
			// visited before, possible with NOP / pseudo operations
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
			if (o instanceof SWITCH) {
				final SWITCH op = (SWITCH) o;

				// hack to get VM PC from PC...argl... _not_ cool
				final int switchPc = op.getPc() - 1;
				int switchVmpc = -1;
				for (final Map.Entry<Integer, Integer> entry : this.vmpc2pc.entrySet()) {
					if (entry.getValue().intValue() == switchPc) {
						switchVmpc = entry.getKey();
						break;
					}
				}

				if (instruction instanceof PackedSwitchDataPseudoInstruction) {
					final PackedSwitchDataPseudoInstruction instr = (PackedSwitchDataPseudoInstruction) instruction;
					final int firstKey = instr.getFirstKey();
					// offsets to switch VM PC
					final int[] targets = instr.getTargets();

					final int[] caseKeys = new int[targets.length];
					final int[] casePcs = new int[targets.length];
					for (int t = 0; t < targets.length; ++t) {
						caseKeys[t] = firstKey + t;
						casePcs[t] = getPc(switchVmpc + targets[t]);
						if (casePcs[t] < 0) {
							getUnresolved(targets[t]).add(op);
						}
					}
					op.setCaseKeys(caseKeys);
					op.setCasePcs(casePcs);
					continue;
				}
				if (instruction instanceof SparseSwitchDataPseudoInstruction) {
					final SparseSwitchDataPseudoInstruction instr = (SparseSwitchDataPseudoInstruction) instruction;
					final int[] keys = instr.getKeys();
					// offsets to switch VM PC
					final int[] targets = instr.getTargets();

					final int[] caseKeys = new int[targets.length];
					final int[] casePcs = new int[targets.length];
					for (int t = 0; t < targets.length; ++t) {
						caseKeys[t] = keys[t];
						casePcs[t] = getPc(switchVmpc + targets[t]);
						if (casePcs[t] < 0) {
							getUnresolved(targets[t]).add(op);
						}
					}
					op.setCaseKeys(caseKeys);
					op.setCasePcs(casePcs);
					continue;
				}
				LOGGER.warning("Unresolved switch target isn't a SwitchDataPseudoInstruction!");
				continue;
			}
			if (o instanceof FILLARRAY) {
				final FILLARRAY op = (FILLARRAY) o;

				if (instruction instanceof ArrayDataPseudoInstruction) {
					final ArrayDataPseudoInstruction instr = (ArrayDataPseudoInstruction) instruction;
					final Object[] values = new Object[instr.getElementCount()];
					final Iterator<ArrayElement> elements = instr.getElements();
					for (int i = 0; elements.hasNext(); ++i) {
						final ArrayElement element = elements.next();
						// strange API, b[] is same for elements, bi changes
						final byte[] b = element.buffer;
						final int bi = element.bufferIndex;
						switch (element.elementWidth) {
						case 1:
							values[i] = b[bi];
							continue;
						case 2:
							values[i] = (short) ((b[bi + 1] & 0xFF) << 8 | b[bi] & 0xFF);
							continue;
						case 4:
							values[i] = (b[bi + 3] & 0xFF) << 24 | (b[bi + 2] & 0xFF) << 16
									| (b[bi + 1] & 0xFF) << 8 | b[bi] & 0xFF;
							continue;
						case 8:
							values[i] = ((long) b[bi + 7] & 0xFF) << 56
									| ((long) b[bi + 6] & 0xFF) << 48
									| ((long) b[bi + 5] & 0xFF) << 40
									| ((long) b[bi + 4] & 0xFF) << 32
									| ((long) b[bi + 3] & 0xFF) << 24
									| ((long) b[bi + 2] & 0xFF) << 16
									| ((long) b[bi + 1] & 0xFF) << 8 | (long) b[bi] & 0xFF;
							continue;
						default:
							LOGGER.warning("Unknown fill array element length '"
									+ element.elementWidth + "'!");
						}
					}
					op.setValues(values);
					continue;
				}
			}
			// cannot happen for Exc / Var here
		}
	}
}