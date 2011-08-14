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

import java.util.HashMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Debug.DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate;

/**
 * Read Dalvik Debug Info.
 * 
 * @author André Pankraz
 */
public class ReadDebugInfo extends ProcessDecodedDebugInstructionDelegate {

	private final static Logger LOGGER = Logger.getLogger(ReadDebugInfo.class
			.getName());

	private final DU du;

	private final HashMap<Integer, Integer> opLines = new HashMap<Integer, Integer>();

	private Var[][] vars;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDebugInfo(final DU du) {
		this.du = du;
	}

	/**
	 * Get operation lines.
	 * 
	 * @return operation lines
	 */
	public HashMap<Integer, Integer> getOpLines() {
		return this.opLines;
	}

	/**
	 * Get local variables.
	 * 
	 * @return local variables
	 */
	public Var[][] getVars() {
		return this.vars;
	}

	@Override
	public void ProcessEndLocal(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		System.out
				.println("*EndLocal: I" + codeAddress + " l" + length + " r"
						+ registerNum + " : " + name + " : " + type + " : "
						+ signature);
		if (name == null) {
			// can happen, sometimes start with
			// Lorg/decojer/cavaj/test/DecTestInlineAssignments;->decIntTest(I[I)I
			// *EndLocal: I7 l2 r8 : null : null : null
			// Smali-Bug? DebugInfo-Bug?
			return;
		}
		if (this.vars == null || registerNum >= this.vars.length) {
			LOGGER.warning("ProcessEndLocal for unknown variable register '"
					+ registerNum + "'!");
		}
		final Var[] vars = this.vars[registerNum];
		for (int i = vars.length; i-- > 0;) {
			final Var var = vars[i];
			if (!var.getName().equals(name.getStringValue())) {
				continue;
			}
			// TODO check type / signature?
			if (var.getEndPc() != 0) {
				continue;
			}
			var.setEndPc(codeAddress);
		}
	}

	@Override
	public void ProcessLineEmit(final int codeAddress, final int line) {
		this.opLines.put(codeAddress, line);
	}

	@Override
	public void ProcessRestartLocal(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*RestartLocal: I" + codeAddress + " l" + length
				+ " r" + registerNum + " : " + name + " : " + type + " : "
				+ signature);
		// TODO whats that?
	}

	@Override
	public void ProcessSetEpilogueBegin(final int codeAddress) {
		LOGGER.warning("Unknown stuff: ProcessSetEpilogueBegin: " + codeAddress);
	}

	@Override
	public void ProcessSetFile(final int codeAddress, final int length,
			final StringIdItem name) {
		LOGGER.warning("Unknown stuff: ProcessSetFile: " + codeAddress + " : "
				+ length + " : " + name);
	}

	@Override
	public void ProcessSetPrologueEnd(final int codeAddress) {
		if (codeAddress != 0) {
			LOGGER.warning("Unknown stuff: ProcessSetPrologueEnd: "
					+ codeAddress);
		}
	}

	@Override
	public void ProcessStartLocal(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name,
			final TypeIdItem type) {
		System.out.println("*StartLocal: I" + codeAddress + " l" + length
				+ " r" + registerNum + " : " + name + " : " + type);
		startLocal(codeAddress, length, registerNum, name, type, null);
	}

	@Override
	public void ProcessStartLocalExtended(final int codeAddress,
			final int length, final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*StartLocalExtended: I" + codeAddress + " l"
				+ length + " r" + registerNum + " : " + name + " : " + type
				+ " : " + signature);
		startLocal(codeAddress, length, registerNum, name, type, signature);
	}

	void startLocal(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		final T varT = this.du.getDescT(type.getTypeDescriptor());
		if (signature != null) {
			varT.setSignature(signature.getStringValue());
		}
		final Var var = new Var(varT);
		var.setName(name.getStringValue());
		var.setStartPc(codeAddress);

		Var[] vars = null;
		if (this.vars == null) {
			this.vars = new Var[registerNum + 1][];
		} else if (registerNum >= this.vars.length) {
			final Var[][] newVars = new Var[registerNum + 1][];
			System.arraycopy(this.vars, 0, newVars, 0, this.vars.length);
			this.vars = newVars;
		} else {
			vars = this.vars[registerNum];
		}
		if (vars == null) {
			vars = new Var[1];
		} else {
			final Var[] newVars = new Var[vars.length + 1];
			System.arraycopy(vars, 0, newVars, 0, vars.length);
			vars = newVars;
		}
		this.vars[registerNum] = vars;
		vars[vars.length - 1] = var;
	}

}