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
package org.decojer.cavaj.readers.smali2;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.Exc;
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
import org.decojer.cavaj.model.code.ops.FILLARRAY;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.INSTANCEOF;
import org.decojer.cavaj.model.code.ops.INVOKE;
import org.decojer.cavaj.model.code.ops.JCMP;
import org.decojer.cavaj.model.code.ops.JCND;
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
import org.decojer.cavaj.model.code.ops.RETURN;
import org.decojer.cavaj.model.code.ops.SHL;
import org.decojer.cavaj.model.code.ops.SHR;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.code.ops.SUB;
import org.decojer.cavaj.model.code.ops.SWITCH;
import org.decojer.cavaj.model.code.ops.THROW;
import org.decojer.cavaj.model.code.ops.XOR;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.readers.Smali2Reader;
import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.DexBackedExceptionHandler;
import org.jf.dexlib2.dexbacked.DexBackedMethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedTryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.LineNumber;
import org.jf.dexlib2.iface.debug.SetSourceFile;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.SwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction12x;
import org.jf.dexlib2.iface.instruction.formats.Instruction20t;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21lh;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22b;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22s;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22x;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction30t;
import org.jf.dexlib2.iface.instruction.formats.Instruction31c;
import org.jf.dexlib2.iface.instruction.formats.Instruction31i;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import org.jf.dexlib2.iface.instruction.formats.Instruction32x;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;
import org.jf.dexlib2.iface.instruction.formats.Instruction51l;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Read method implementation.
 * 
 * @author André Pankraz
 */
public class ReadMethodImplementation {

	private final static Logger LOGGER = Logger.getLogger(ReadMethodImplementation.class.getName());

	private MD md;

	private final Map<Integer, Integer> opLines = Maps.newHashMap();

	final List<Op> ops = Lists.newArrayList();

	private final Map<Integer, Integer> vmpc2pc = Maps.newHashMap();

	private final Map<Integer, List<Object>> vmpc2unresolved = Maps.newHashMap();

	private DU getDu() {
		return this.md.getTd().getDu();
	}

	/**
	 * Get line for VM PC.
	 * 
	 * @param vmpc
	 *            VM PC
	 * @return line
	 */
	private int getLine(final int vmpc) {
		// assume that the lines where ordered on read
		for (int i = vmpc; i >= 0; --i) {
			final Integer line = this.opLines.get(i);
			if (line != null) {
				return line;
			}
		}
		return -1;
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

	private List<Object> getUnresolved(final int vmpc) {
		List<Object> unresolved = this.vmpc2unresolved.get(vmpc);
		if (unresolved == null) {
			unresolved = Lists.newArrayList();
			this.vmpc2unresolved.put(vmpc, unresolved);
		}
		return unresolved;
	}

	/**
	 * Init and visit.
	 * 
	 * @param md
	 *            method declaration
	 * @param implementation
	 *            method implementation
	 */
	public void initAndVisit(final MD md, final DexBackedMethodImplementation implementation) {
		if (implementation == null) {
			return;
		}
		this.md = md;

		this.ops.clear();
		this.opLines.clear();
		this.vmpc2pc.clear();
		this.vmpc2unresolved.clear();

		final CFG cfg = new CFG(md, implementation.getRegisterCount(), 0);
		md.setCfg(cfg);

		for (final DebugItem debugItem : implementation.getDebugItems()) {
			final int codeAddress = debugItem.getCodeAddress();
			switch (debugItem.getDebugItemType()) {
			case DebugItemType.ADVANCE_LINE:
				break;
			case DebugItemType.ADVANCE_PC:
				break;
			case DebugItemType.END_LOCAL:
				break;
			case DebugItemType.END_SEQUENCE:
				break;
			case DebugItemType.EPILOGUE_BEGIN:
				break;
			case DebugItemType.LINE_NUMBER:
				this.opLines.put(codeAddress, ((LineNumber) debugItem).getLineNumber());
				break;
			case DebugItemType.PROLOGUE_END:
				break;
			case DebugItemType.RESTART_LOCAL:
				break;
			case DebugItemType.SET_SOURCE_FILE:
				log("Unknown stuff: SetFile: " + codeAddress + " : "
						+ ((SetSourceFile) debugItem).getSourceFile());
				break;
			case DebugItemType.START_LOCAL:
				break;
			case DebugItemType.START_LOCAL_EXTENDED:
				break;
			default:
				log("Unknown debug item type '" + debugItem.getDebugItemType() + "'!");
			}
		}

		// invoke(range) or fill-new-array result type
		T moveInvokeResultT = null;

		int vmpc = 0, line = -1, opcode = -1;
		for (final Instruction instruction : implementation.getInstructions()) {

			// visit vmpc later, not for automatically generated follow POP!
			T t = null;

			switch (instruction.getOpcode()) {
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
						log("Move result without previous result type!");
						moveInvokeResultT = T.REF;
					}

					this.ops.add(new STORE(this.ops.size(), opcode, line, moveInvokeResultT, instr
							.getRegisterA()));

					moveInvokeResultT = null;
					// next instruction, done for this round
					vmpc += instruction.getCodeUnits();
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
			line = getLine(vmpc);
			opcode = instruction.getOpcode().value;

			int iValue = 0;
			Object oValue = null;

			switch (instruction.getOpcode()) {
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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new ADD(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case ADD_INT_LIT16: {
				// A = B + literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new ADD(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			/*********
			 * ALOAD *
			 *********/
			case AGET:
				t = T.SINGLE; // int & float
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, getDu().getArrayT(t),
							instr.getRegisterB()));
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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new AND(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case AND_INT_LIT16: {
				// A = B & literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

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
				t = T.SINGLE; // int & float
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

					this.ops.add(new LOAD(this.ops.size(), opcode, line, getDu().getArrayT(t),
							instr.getRegisterB()));
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

				t = getDu().getDescT(((TypeReference) instr.getReference()).getType());

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
					// A = (totype) B
					final Instruction12x instr = (Instruction12x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new DIV(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case DIV_INT_LIT16: {
				// A = B / literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new DIV(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			/*******
			 * GET *
			 *******/
			case IGET:
			case IGET_VOLATILE:
				t = T.SINGLE; // int & float
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

					final FieldReference fieldReference = (FieldReference) instr.getReference();

					final T ownerT = getDu().getDescT(fieldReference.getDefiningClass());
					final F f = ownerT.getF(fieldReference.getName(), fieldReference.getType());
					f.setStatic(false);

					assert t.isAssignableFrom(f.getValueT());

					this.ops.add(new LOAD(this.ops.size(), opcode, line, ownerT, instr
							.getRegisterB()));

					this.ops.add(new GET(this.ops.size(), opcode, line, f));

					this.ops.add(new STORE(this.ops.size(), opcode, line, f.getValueT(), instr
							.getRegisterA()));
				}
				break;
			case SGET:
			case SGET_VOLATILE:
				t = T.SINGLE; // int & float
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

					final FieldReference fieldReference = (FieldReference) instr.getReference();

					final T ownerT = getDu().getDescT(fieldReference.getDefiningClass());
					final F f = ownerT.getF(fieldReference.getName(), fieldReference.getType());
					f.setStatic(true);

					assert t.isAssignableFrom(f.getValueT());

					this.ops.add(new GET(this.ops.size(), opcode, line, f));

					this.ops.add(new STORE(this.ops.size(), opcode, line, f.getValueT(), instr
							.getRegisterA()));
				}
				break;
			/********
			 * GOTO *
			 ********/
			case GOTO: {
				final Instruction10t instr = (Instruction10t) instruction;

				t = T.VOID;
				iValue = instr.getCodeOffset();
			}
			// fall through
			case GOTO_16:
				if (t == null) {
					final Instruction20t instr = (Instruction20t) instruction;

					t = T.VOID;
					iValue = instr.getCodeOffset();
				}
				// fall through
			case GOTO_32:
				if (t == null) {
					final Instruction30t instr = (Instruction30t) instruction;

					t = T.VOID;
					instr.getCodeOffset();
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

				t = getDu().getDescT(((TypeReference) instr.getReference()).getType());

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
			// all IF_???: floats via CMP?_FLOAT
			case IF_EQ:
				t = T.AINTREF; // boolean and refcheck too
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
					t = T.AINTREF; // boolean and refcheck too
					oValue = CmpType.T_NE;
				}
				{
					// IF A cond B JMP offset
					final Instruction22t instr = (Instruction22t) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterB()));

					final JCMP op = new JCMP(this.ops.size(), opcode, line, t, (CmpType) oValue);
					this.ops.add(op);
					final int targetVmpc = vmpc + instr.getCodeOffset();
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
			// all IF_???: floats via CMP?_FLOAT
			case IF_EQZ:
				t = T.AINTREF; // boolean and nullcheck too
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
					t = T.AINTREF; // boolean and nullcheck too
					oValue = CmpType.T_NE;
				}
				{
					// IF A cond 0 JMP offset
					final Instruction21t instr = (Instruction21t) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, t, instr.getRegisterA()));

					final JCND op = new JCND(this.ops.size(), opcode, line, t, (CmpType) oValue);
					this.ops.add(op);
					final int targetVmpc = vmpc + instr.getCodeOffset();
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
				// Constructor or supermethod (any super) or private method callout.
			case INVOKE_INTERFACE:
			case INVOKE_STATIC:
			case INVOKE_SUPER:
			case INVOKE_VIRTUAL: {
				final Instruction35c instr = (Instruction35c) instruction;

				final int registerCount = instr.getRegisterCount();
				final int[] regs = new int[registerCount];
				if (registerCount > 0) {
					regs[0] = instr.getRegisterD();
				}
				if (registerCount > 1) {
					regs[1] = instr.getRegisterE();
				}
				if (registerCount > 2) {
					regs[2] = instr.getRegisterF();
				}
				if (registerCount > 3) {
					regs[3] = instr.getRegisterG();
				}
				if (registerCount > 4) {
					regs[4] = instr.getRegisterC(); /* A -> C? */
				}
				int reg = 0;

				final MethodReference methodReference = (MethodReference) instr.getReference();
				final T ownerT = getDu().getDescT(methodReference.getDefiningClass());
				ownerT.setInterface(instruction.getOpcode() == Opcode.INVOKE_INTERFACE);
				final M m = ownerT.getM(methodReference.getName(),
						Smali2Reader.desc(methodReference));
				m.setStatic(instruction.getOpcode() == Opcode.INVOKE_STATIC);
				if (instruction.getOpcode() != Opcode.INVOKE_STATIC) {
					this.ops.add(new LOAD(this.ops.size(), opcode, line, ownerT, regs[reg++]));
				}

				for (final T paramT : m.getParamTs()) {
					this.ops.add(new LOAD(this.ops.size(), opcode, line, paramT, regs[reg++]));
					if (paramT.isWide()) {
						++reg;
					}
				}

				this.ops.add(new INVOKE(this.ops.size(), opcode, line, m,
						instruction.getOpcode() == Opcode.INVOKE_DIRECT));
				if (m.getReturnT() != T.VOID) {
					moveInvokeResultT = m.getReturnT();
				}
				break;
			}
			case INVOKE_DIRECT_RANGE:
				// Constructor or supermethod (any super) or private method callout.
			case INVOKE_INTERFACE_RANGE:
			case INVOKE_STATIC_RANGE:
			case INVOKE_SUPER_RANGE:
			case INVOKE_VIRTUAL_RANGE: {
				final Instruction3rc instr = (Instruction3rc) instruction;

				int reg = instr.getStartRegister();

				final MethodReference methodReference = (MethodReference) instr.getReference();
				final T ownerT = getDu().getDescT(methodReference.getDefiningClass());
				((ClassT) ownerT)
						.setInterface(instruction.getOpcode() == Opcode.INVOKE_INTERFACE_RANGE);
				final M m = ownerT.getM(methodReference.getName(),
						Smali2Reader.desc(methodReference));
				m.setStatic(instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE);
				if (instruction.getOpcode() != Opcode.INVOKE_STATIC_RANGE) {
					this.ops.add(new LOAD(this.ops.size(), opcode, line, ownerT, reg++));
				}

				for (final T paramT : m.getParamTs()) {
					this.ops.add(new LOAD(this.ops.size(), opcode, line, paramT, reg++));
					if (paramT.isWide()) {
						++reg;
					}
				}

				this.ops.add(new INVOKE(this.ops.size(), opcode, line, m,
						instruction.getOpcode() == Opcode.INVOKE_DIRECT_RANGE));
				if (m.getReturnT() != T.VOID) {
					moveInvokeResultT = m.getReturnT();
				}
				break;
			}
			/***********
			 * MONITOR *
			 ***********/
			case MONITOR_ENTER:
				oValue = MONITOR.Kind.ENTER;
				// fall through
			case MONITOR_EXIT:
				if (oValue == null) {
					oValue = MONITOR.Kind.EXIT;
				}
				{
					// synchronized A
					final Instruction11x instr = (Instruction11x) instruction;

					this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr
							.getRegisterA()));

					this.ops.add(new MONITOR(this.ops.size(), opcode, line, (MONITOR.Kind) oValue));
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
						getDu().getT(Throwable.class), instr.getRegisterA()));
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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new MUL(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case MUL_INT_LIT16: {
				// A = B * literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

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

				t = getDu().getDescT(((TypeReference) instr.getReference()).getType());

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

				t = getDu().getDescT(((TypeReference) instr.getReference()).getType());
				// contains dimensions via [

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));

				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, t, 1));

				this.ops.add(new STORE(this.ops.size(), opcode, line, t, instr.getRegisterA()));
				break;
			}
			case FILL_ARRAY_DATA: {
				// fill_array_data(A) -> target
				final Instruction31t instr = (Instruction31t) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.REF, instr.getRegisterA()));

				final FILLARRAY op = new FILLARRAY(this.ops.size(), opcode, line);
				this.ops.add(op);

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.REF, instr.getRegisterA()));

				final int targetVmpc = vmpc + instr.getCodeOffset();
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

				t = getDu().getDescT(((TypeReference) instr.getReference()).getType());
				// contains dimensions via [
				final int registerCount = instr.getRegisterCount();

				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, registerCount));

				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, t, 1));

				final Object[] regs = new Object[registerCount];
				if (registerCount > 0) {
					regs[0] = instr.getRegisterD();
				}
				if (registerCount > 1) {
					regs[1] = instr.getRegisterE();
				}
				if (registerCount > 2) {
					regs[2] = instr.getRegisterF();
				}
				if (registerCount > 3) {
					regs[3] = instr.getRegisterG();
				}
				if (registerCount > 4) {
					regs[4] = instr.getRegisterC(); // TODO A -> C?
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

				t = getDu().getDescT(((TypeReference) instr.getReference()).getType());
				// contains dimensions via [
				final int registerCount = instr.getRegisterCount();

				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, registerCount));

				this.ops.add(new NEWARRAY(this.ops.size(), opcode, line, t, 1));

				final Object[] regs = new Object[registerCount];
				for (int reg = instr.getStartRegister(), j = 0; j < registerCount; ++reg, ++j) {
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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new OR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case OR_INT_LIT16: {
				// A = B | literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

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

				oValue = iValue = instr.getNarrowLiteral();
				t = T.getDalvikIntT(iValue);
				iValue = instr.getRegisterA();
			}
			// fall through
			case CONST_16:
				if (t == null) {
					// A = literal
					final Instruction21s instr = (Instruction21s) instruction;

					oValue = iValue = instr.getNarrowLiteral();
					t = T.getDalvikIntT(iValue);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_HIGH16:
				if (t == null) {
					// A = literal
					final Instruction21ih instr = (Instruction21ih) instruction;

					oValue = iValue = instr.getNarrowLiteral();
					t = T.getDalvikIntT(iValue);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST: /* 32 */
				if (t == null) {
					// A = literal
					final Instruction31i instr = (Instruction31i) instruction;

					oValue = iValue = instr.getNarrowLiteral();
					t = T.getDalvikIntT(iValue);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE_16:
				if (t == null) {
					// A = literal
					final Instruction21s instr = (Instruction21s) instruction;

					oValue = instr.getWideLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE_HIGH16:
				if (t == null) {
					// A = literal
					final Instruction21lh instr = (Instruction21lh) instruction;

					oValue = instr.getWideLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE_32:
				if (t == null) {
					// A = literal
					final Instruction31i instr = (Instruction31i) instruction;

					oValue = instr.getWideLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_WIDE: /* _64 */
				if (t == null) {
					// A = literal
					final Instruction51l instr = (Instruction51l) instruction;

					oValue = instr.getWideLiteral();
					t = T.WIDE;
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_CLASS:
				if (t == null) {
					// A = literal
					final Instruction21c instr = (Instruction21c) instruction;

					oValue = getDu().getDescT(((TypeReference) instr.getReference()).getType());
					t = getDu().getT(Class.class);
					iValue = instr.getRegisterA();
				}
				// fall through
			case CONST_STRING:
				if (t == null) {
					// A = literal
					final Instruction21c instr = (Instruction21c) instruction;

					oValue = ((StringReference) instr.getReference()).getString();
					t = getDu().getT(String.class);
					iValue = instr.getRegisterA();
				}
			case CONST_STRING_JUMBO:
				if (t == null) {
					// A = literal
					final Instruction31c instr = (Instruction31c) instruction;

					oValue = ((StringReference) instr.getReference()).getString();
					t = getDu().getT(String.class);
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
				t = T.SINGLE; // int & float
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

					final FieldReference fieldReference = (FieldReference) instr.getReference();

					final T ownerT = getDu().getDescT(fieldReference.getDefiningClass());
					final F f = ownerT.getF(fieldReference.getName(), fieldReference.getType());
					f.setStatic(false);

					assert f.getValueT().isAssignableFrom(t);

					this.ops.add(new LOAD(this.ops.size(), opcode, line, ownerT, instr
							.getRegisterB()));
					this.ops.add(new LOAD(this.ops.size(), opcode, line, f.getValueT(), instr
							.getRegisterA()));

					this.ops.add(new PUT(this.ops.size(), opcode, line, f));
				}
				break;
			case SPUT:
			case SPUT_VOLATILE:
				t = T.SINGLE; // int & float
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

					final FieldReference fieldReference = (FieldReference) instr.getReference();

					final T ownerT = getDu().getDescT(fieldReference.getDefiningClass());
					final F f = ownerT.getF(fieldReference.getName(), fieldReference.getType());
					f.setStatic(true);

					assert f.getValueT().isAssignableFrom(t);

					this.ops.add(new LOAD(this.ops.size(), opcode, line, f.getValueT(), instr
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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new REM(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case REM_INT_LIT16: {
				// A = B % literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new SHR(this.ops.size(), opcode, line, T.INT, T.INT, true));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			/********
			 * RSUB *
			 ********/
			case RSUB_INT: {
				// A = literal - B
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));
				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));

				this.ops.add(new SUB(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
			}
				break;
			case RSUB_INT_LIT8: {
				// A = literal - B
				final Instruction22b instr = (Instruction22b) instruction;

				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));
				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));

				this.ops.add(new SUB(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
			}
				break;
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
				final int targetVmpc = vmpc + instr.getCodeOffset();
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

				t = getDu().getT(Throwable.class);

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
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new XOR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case XOR_INT_LIT16: {
				// A = B ^ literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.ops.add(new LOAD(this.ops.size(), opcode, line, T.INT, instr.getRegisterB()));
				this.ops.add(new PUSH(this.ops.size(), opcode, line, T.INT, instr
						.getNarrowLiteral()));

				this.ops.add(new XOR(this.ops.size(), opcode, line, T.INT));

				this.ops.add(new STORE(this.ops.size(), opcode, line, T.INT, instr.getRegisterA()));
				break;
			}
			case ARRAY_PAYLOAD:
			case PACKED_SWITCH_PAYLOAD:
			case SPARSE_SWITCH_PAYLOAD:
				// pseudo operations, that are handled in visitVmpc()
				break;
			default:
				throw new RuntimeException("Unknown jvm operation '" + instruction.getOpcode()
						+ "'!");
			}
			vmpc += instruction.getCodeUnits();
		}
		visitVmpc(vmpc, null);
		cfg.setOps(this.ops.toArray(new Op[this.ops.size()]));

		final List<? extends DexBackedTryBlock> tryBlocks = implementation.getTryBlocks();
		if (!tryBlocks.isEmpty()) {
			final List<Exc> excs = Lists.newArrayList();
			// preserve order
			for (final DexBackedTryBlock tryBlock : tryBlocks) {
				for (final DexBackedExceptionHandler handler : tryBlock.getExceptionHandlers()) {
					final String exceptionType = handler.getExceptionType();
					final Exc exc = new Exc(exceptionType == null ? null : getDu().getDescT(
							exceptionType));
					exc.setStartPc(this.vmpc2pc.get(tryBlock.getStartCodeAddress()));
					exc.setEndPc(this.vmpc2pc.get(tryBlock.getStartCodeAddress()
							+ tryBlock.getCodeUnitCount()));
					exc.setHandlerPc(this.vmpc2pc.get(handler.getHandlerCodeAddress()));
					excs.add(exc);
				}
			}
			cfg.setExcs(excs.toArray(new Exc[excs.size()]));
		}
		// TODO
	}

	private void log(final String message) {
		LOGGER.warning(this.md + ": " + message);
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
			if (o instanceof FILLARRAY) {
				final FILLARRAY op = (FILLARRAY) o;

				if (instruction instanceof ArrayPayload) {
					final ArrayPayload instr = (ArrayPayload) instruction;
					final List<Number> arrayElements = instr.getArrayElements();

					final Object[] values = new Object[arrayElements.size()];
					for (int i = values.length; i-- > 0;) {
						values[i] = arrayElements.get(i);
					}
					op.setValues(values);
					continue;
				}
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

				if (instruction instanceof SwitchPayload) {
					final SwitchPayload instr = (SwitchPayload) instruction;
					final List<? extends SwitchElement> switchElements = instr.getSwitchElements();

					final int[] caseKeys = new int[switchElements.size()];
					final int[] casePcs = new int[caseKeys.length];
					for (int i = caseKeys.length; i-- > 0;) {
						final SwitchElement switchElement = switchElements.get(i);
						caseKeys[i] = switchElement.getKey();
						casePcs[i] = getPc(switchVmpc + switchElement.getOffset());
						if (casePcs[i] < 0) {
							getUnresolved(switchElement.getOffset()).add(op);
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