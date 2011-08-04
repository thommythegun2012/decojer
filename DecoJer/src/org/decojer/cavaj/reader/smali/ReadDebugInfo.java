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

import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Debug.DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate;

/**
 * @author André Pankraz
 */
public class ReadDebugInfo extends ProcessDecodedDebugInstructionDelegate {

	private final static Logger LOGGER = Logger.getLogger(ReadDebugInfo.class
			.getName());

	private final HashMap<Integer, Integer> opLines = new HashMap<Integer, Integer>();

	public HashMap<Integer, Integer> getOpLines() {
		return this.opLines;
	}

	@Override
	public void ProcessEndLocal(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		System.out
				.println("*EndLocal: " + codeAddress + " : " + length + " : "
						+ registerNum + " : " + name + " : " + type + " : "
						+ signature);
	}

	@Override
	public void ProcessLineEmit(final int codeAddress, final int line) {
		this.opLines.put(codeAddress, line);
	}

	@Override
	public void ProcessRestartLocal(final int codeAddress, final int length,
			final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*RestartLocal: " + codeAddress + " : " + length
				+ " : " + registerNum + " : " + name + " : " + type + " : "
				+ signature);
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
		System.out.println("*StartLocal: " + codeAddress + " : " + length
				+ " : " + registerNum + " : " + name + " : " + type);
	}

	@Override
	public void ProcessStartLocalExtended(final int codeAddress,
			final int length, final int registerNum, final StringIdItem name,
			final TypeIdItem type, final StringIdItem signature) {
		System.out.println("*StartLocalExtended: " + codeAddress + " : "
				+ length + " : " + registerNum + " : " + name + " : " + type
				+ " : " + signature);
	}

}