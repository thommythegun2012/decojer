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
import java.util.logging.Logger;

import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.POP;
import org.decojer.cavaj.model.code.ops.STORE;
import org.jf.dexlib.Code.Format.Instruction11x;
import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.dexbacked.DexBackedMethodImplementation;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;

import com.google.common.collect.Lists;

/**
 * Read method implementation.
 * 
 * @author André Pankraz
 */
public class ReadMethodImplementation {

	private final static Logger LOGGER = Logger.getLogger(ReadMethodImplementation.class.getName());

	private MD md;

	final List<Op> ops = Lists.newArrayList();

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

		final CFG cfg = new CFG(md, implementation.getRegisterCount(), 0);
		md.setCfg(cfg);

		for (final DebugItem debugItem : implementation.getDebugItems()) {
			switch (debugItem.getDebugItemType()) {
			case DebugItemType.ADVANCE_LINE:
			case DebugItemType.ADVANCE_PC:
			case DebugItemType.END_LOCAL:
			case DebugItemType.END_SEQUENCE:
			case DebugItemType.EPILOGUE_BEGIN:
			case DebugItemType.LINE_NUMBER:
			case DebugItemType.PROLOGUE_END:
			case DebugItemType.RESTART_LOCAL:
			case DebugItemType.SET_SOURCE_FILE:
			case DebugItemType.START_LOCAL:
			case DebugItemType.START_LOCAL_EXTENDED:
			default:
				LOGGER.warning("Unknown debug item type '" + debugItem.getDebugItemType() + "'!");
			}
		}

		// invoke(range) or fill-new-array result type
		T moveInvokeResultT = null;

		final int vmpc = 0;
		for (final Instruction instruction : implementation.getInstructions()) {

			// visit vmpc later, not for automatically generated follow POP!
			T t = null;

			final int line = -1; // TODO this.readDebugInfo.getLine(vmpc);
			final short opcode = instruction.getOpcode().value;

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
		}
	}

	private void visitVmpc(final int vmpc, final Instruction instruction) {
		// TODO
	}

}