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
package org.decojer.cavaj.readers.smali;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import lombok.Getter;

import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.utils.Cursor;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Debug.DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Smali read debug info.
 * 
 * @author André Pankraz
 */
public class ReadDebugInfo extends ProcessDecodedDebugInstructionDelegate {

	private final static Logger LOGGER = Logger.getLogger(ReadDebugInfo.class.getName());

	private final static boolean DEBUG = false;

	private MD md;

	private final Map<Integer, Integer> opLines = Maps.newHashMap();

	@Getter
	private final Map<Integer, List<V>> reg2vs = Maps.newHashMap();

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
	 * Init and visit.
	 * 
	 * @param md
	 *            method declaration
	 * @param debugInfoItem
	 *            Smail debug info item
	 */
	public void initAndVisit(final MD md, final DebugInfoItem debugInfoItem) {
		this.md = md;

		this.opLines.clear();
		this.reg2vs.clear();

		// must read debug info before operations because of line numbers
		if (debugInfoItem == null) {
			return;
		}

		final StringIdItem[] parameterNames = debugInfoItem.getParameterNames();
		if (parameterNames != null && parameterNames.length > 0) {
			for (int i = parameterNames.length; i-- > 0;) {
				if (parameterNames[i] == null) {
					// could happen, e.g. synthetic methods, inner constructor with outer type param
					continue;
				}
				md.setParamName(i, parameterNames[i].getStringValue());
			}
		}
		if (DEBUG) {
			System.out.println("****DecodeDebugInstructions: " + md);
		}
		DebugInstructionIterator.DecodeInstructions(debugInfoItem, md.getCfg().getRegs(), this);
	}

	private void log(final String message) {
		LOGGER.warning(this.md + ": " + message);
	}

	@Override
	public void ProcessEndLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type, final StringIdItem signature) {
		// log("*EndLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N" + length + " r"
		// + registerNum + " : " + name + " : " + type + " : " + signature);

		final List<V> vs = this.reg2vs.get(registerNum);
		if (vs == null) {
			log("EndLocal '" + registerNum + "' without any StartLocal!");
			return;
		}
		assert vs.size() != 0;

		final V v = vs.get(vs.size() - 1);
		final int[] pcs = v.getPcs();
		assert pcs != null;
		assert pcs.length >= 2;

		if (pcs[pcs.length - 1] != -1) {
			log("EndLocal '" + registerNum + "' without StartLocal!");
			return;
		}
		pcs[pcs.length - 1] = codeAddress;
	}

	@Override
	public void ProcessLineEmit(final int codeAddress, final int line) {
		this.opLines.put(codeAddress, line);
		// log("*EmitLine: P" + codeAddress + " l" + line);
	}

	@Override
	public void ProcessRestartLocal(final int codeAddress, final int length, final int registerNum,
			final StringIdItem name, final TypeIdItem type, final StringIdItem signature) {
		// log("*RestartLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N" + length + " r"
		// + registerNum + " : " + name + " : " + type + " : " + signature);

		final List<V> vs = this.reg2vs.get(registerNum);
		if (vs == null) {
			log("RestartLocal '" + registerNum + "' without any StartLocal!");
			return;
		}
		assert vs.size() != 0;

		final V v = vs.get(vs.size() - 1);
		final int[] pcs = v.getPcs();
		assert pcs != null;
		assert pcs.length >= 2;

		if (pcs[pcs.length - 1] == -1) {
			log("RestartLocal '" + registerNum + "' without EndLocal!");
			return;
		}
		v.addPcs(codeAddress, -1);
	}

	@Override
	public void ProcessSetEpilogueBegin(final int codeAddress) {
		log("Unknown stuff: SetEpilogueBegin: " + codeAddress);
	}

	@Override
	public void ProcessSetFile(final int codeAddress, final int length, final StringIdItem name) {
		log("Unknown stuff: SetFile: " + codeAddress + " : " + length + " : " + name);
	}

	@Override
	public void ProcessSetPrologueEnd(final int codeAddress) {
		if (codeAddress != 0) {
			log("Unknown stuff: SetPrologueEnd: " + codeAddress);
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
		// log("*StartLocal: P" + codeAddress + " l" + getLine(codeAddress) + " N" + length + " r"
		// + registerNum + " : " + name + " : " + type + " : " + signature);
		assert length > 0; // have no idea what this is

		T vT = this.md.getTd().getDu().getDescT(type.getTypeDescriptor());
		if (signature != null) {
			final T sigT = this.md.getTd().getDu()
					.parseT(signature.getStringValue(), new Cursor(), this.md.getM());
			if (!sigT.eraseTo(vT)) {
				LOGGER.info("Cannot reduce signature '" + signature.getStringValue()
						+ "' to type '" + vT + "' for method (local variable '" + name + "') "
						+ this.md);
			} else {
				vT = sigT;
			}
		}
		final V v = new V(vT, name.getStringValue(), codeAddress, -1);

		List<V> vs = this.reg2vs.get(registerNum);
		if (vs == null) {
			vs = Lists.newArrayList();
			this.reg2vs.put(registerNum, vs);
		}
		vs.add(v);
	}

}