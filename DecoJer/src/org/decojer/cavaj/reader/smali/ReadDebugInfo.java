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

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.V;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Debug.DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate;

/**
 * Smali read debug info.
 * 
 * @author André Pankraz
 */
public class ReadDebugInfo extends ProcessDecodedDebugInstructionDelegate {

	private final static Logger LOGGER = Logger.getLogger(ReadDebugInfo.class.getName());

	private final DU du;

	private final HashMap<Integer, Integer> opLines = new HashMap<Integer, Integer>();

	private final HashMap<Integer, ArrayList<V>> reg2vs = new HashMap<Integer, ArrayList<V>>();

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

	/**
	 * Get line for VM PC.
	 * 
	 * @param vmpc
	 *            VM PC
	 * @return line
	 */
	public int getLine(final int vmpc) {
		// assume that the lines where ordered on read
		for (int i = vmpc; i >= 0; --i) {
			final Integer line = this.opLines.get(i);
			if (line != null) {
				return line;
			}
		}
		return -1;
	}

	/**
	 * Get register to variables map.
	 * 
	 * @return register to variables map
	 */
	public HashMap<Integer, ArrayList<V>> getReg2vs() {
		return this.reg2vs;
	}

	/**
	 * Init and visit.
	 * 
	 * @param md
	 *            method declaration
	 * @param debugInfoItem
	 *            Smail debug info item
	 */
	public void initAndVisit(final MD md, final DebugInfoItem debugInfoItem) {
		this.opLines.clear();
		this.reg2vs.clear();

		// must read debug info before operations because of line numbers
		if (debugInfoItem == null) {
			return;
		}

		final M m = md.getM();
		final StringIdItem[] parameterNames = debugInfoItem.getParameterNames();
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
		DebugInstructionIterator.DecodeInstructions(debugInfoItem, md.getCfg().getRegs(), this);
	}

	@Override
	public void ProcessEndLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*ProcessEndLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N"
				+ length + " r" + registerNum + " : " + name + " : " + type + " : " + signature);

		final ArrayList<V> vs = this.reg2vs.get(registerNum);
		if (vs == null) {
			LOGGER.warning("ProcessEndLocal '" + registerNum + "' without any ProcessStartLocal!");
			return;
		}
		assert vs.size() != 0;

		final V v = vs.get(vs.size() - 1);
		final int[] pcs = v.getPcs();
		assert pcs != null;
		assert pcs.length >= 2;

		if (pcs[pcs.length - 1] != -1) {
			LOGGER.warning("ProcessEndLocal '" + registerNum + "' without ProcessStartLocal!");
			return;
		}
		pcs[pcs.length - 1] = codeAddress;
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

		final ArrayList<V> vs = this.reg2vs.get(registerNum);
		if (vs == null) {
			LOGGER.warning("ProcessRestartLocal '" + registerNum
					+ "' without any ProcessStartLocal!");
			return;
		}
		assert vs.size() != 0;

		final V v = vs.get(vs.size() - 1);
		final int[] pcs = v.getPcs();
		assert pcs != null;
		assert pcs.length >= 2;

		if (pcs[pcs.length - 1] == -1) {
			LOGGER.warning("ProcessRestartLocal '" + registerNum + "' without ProcessEndLocal!");
			return;
		}
		v.addPcs(codeAddress, -1);
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

		final T vT = this.du.getDescT(type.getTypeDescriptor());
		if (signature != null) {
			vT.setSignature(signature.getStringValue());
		}
		final V v = new V(vT, name.getStringValue(), codeAddress, -1);

		ArrayList<V> vs = this.reg2vs.get(registerNum);
		if (vs == null) {
			vs = new ArrayList<V>();
			this.reg2vs.put(registerNum, vs);
		}
		vs.add(v);
	}

}