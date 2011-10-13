/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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

import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Debug.DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate;

/**
 * Read Dalvik Debug Info.
 * 
 * @author Andr� Pankraz
 */
public class ReadDebugInfo extends ProcessDecodedDebugInstructionDelegate {

	private final static Logger LOGGER = Logger.getLogger(ReadDebugInfo.class.getName());

	private CFG cfg;

	private final DU du;

	private final HashMap<Integer, Integer> opLines = new HashMap<Integer, Integer>();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDebugInfo(final DU du) {
		assert du != null;

		this.du = du;
	}

	public int getLine(final int codeAddress) {
		for (int i = codeAddress; i >= 0; --i) {
			final Integer line = this.opLines.get(i);
			if (line != null) {
				return line;
			}
		}
		return -1;
	}

	/**
	 * Get operation lines.
	 * 
	 * @return operation lines
	 */
	public HashMap<Integer, Integer> getOpLines() {
		return this.opLines;
	}

	public void initAndVisit(final MD md, final DebugInfoItem debugInfo) {
		this.opLines.clear();

		// must read debug info before operations because of line numbers
		if (debugInfo == null) {
			return;
		}

		this.cfg = md.getCfg();

		final M m = md.getM();
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
		DebugInstructionIterator.DecodeInstructions(debugInfo, md.getCfg().getMaxRegs(), this);
	}

	@Override
	public void ProcessEndLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*ProcessEndLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N"
				+ length + " r" + registerNum + " : " + name + " : " + type + " : " + signature);

		final Var var = this.cfg.getVar(registerNum, codeAddress);
		if (var == null) {
			LOGGER.warning("ProcessEndLocal '" + registerNum + "' without ProcessStartLocal!");
			return;
		}
		var.setEndPc(codeAddress);
	}

	@Override
	public void ProcessLineEmit(final int codeAddress, final int line) {
		this.opLines.put(codeAddress, line);
	}

	@Override
	public void ProcessRestartLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*RestartLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N"
				+ length + " r" + registerNum + " : " + name + " : " + type + " : " + signature);
		// TODO whats that?
	}

	@Override
	public void ProcessSetEpilogueBegin(final int codeAddress) {
		LOGGER.warning("Unknown stuff: ProcessSetEpilogueBegin: " + codeAddress);
	}

	@Override
	public void ProcessSetFile(final int codeAddress, final int length, final StringIdItem name) {
		LOGGER.warning("Unknown stuff: ProcessSetFile: " + codeAddress + " : " + length + " : "
				+ name);
	}

	@Override
	public void ProcessSetPrologueEnd(final int codeAddress) {
		if (codeAddress != 0) {
			LOGGER.warning("Unknown stuff: ProcessSetPrologueEnd: " + codeAddress);
		}
	}

	@Override
	public void ProcessStartLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type) {
		startLocal(codeAddress, length, registerNum, name, type, null);
	}

	@Override
	public void ProcessStartLocalExtended(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name, final TypeIdItem type,
			final StringIdItem signature) {
		startLocal(codeAddress, length, registerNum, name, type, signature);
	}

	private void startLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*startLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N"
				+ length + " r" + registerNum + " : " + name + " : " + type + " : " + signature);
		final T varT = this.du.getDescT(type.getTypeDescriptor());
		if (signature != null) {
			varT.setSignature(signature.getStringValue());
		}
		final Var var = new Var(varT);
		var.setName(name.getStringValue());

		var.setStartPc(codeAddress);

		this.cfg.addVar(registerNum, var);
	}

}