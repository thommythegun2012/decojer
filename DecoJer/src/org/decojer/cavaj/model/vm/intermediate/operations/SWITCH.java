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
package org.decojer.cavaj.model.vm.intermediate.operations;

import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;

public class SWITCH extends Operation {

	private int defaultTarget;

	private int[] keys;

	private int[] keyTargets;

	public SWITCH(final int opPc, final int opCode, final int opLine) {
		super(opPc, opCode, opLine);
	}

	public int getDefaultTarget() {
		return this.defaultTarget;
	}

	@Override
	public int getInStackSize() {
		return 1;
	}

	public int[] getKeys() {
		return this.keys;
	}

	public int[] getKeyTargets() {
		return this.keyTargets;
	}

	@Override
	public int getOpcode() {
		return Opcode.SWITCH;
	}

	public void setDefaultTarget(final int defaultTarget) {
		this.defaultTarget = defaultTarget;
	}

	public void setKeys(final int[] keys) {
		this.keys = keys;
	}

	public void setKeyTargets(final int[] keyTargets) {
		this.keyTargets = keyTargets;
	}

}