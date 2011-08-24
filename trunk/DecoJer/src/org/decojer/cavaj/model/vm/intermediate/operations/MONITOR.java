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

public class MONITOR extends Operation {

	public static final int T_ENTER = 0;

	public static final int T_EXIT = 1;

	private final int monitorType;

	public MONITOR(final int pc, final int code, final int line,
			final int monitorType) {
		super(pc, code, line);
		this.monitorType = monitorType;
	}

	@Override
	public int getInStackSize() {
		return 1;
	}

	public int getMonitorType() {
		return this.monitorType;
	}

	@Override
	public int getOpcode() {
		return Opcode.MONITOR;
	}

	@Override
	public String toString() {
		return super.toString() + " "
				+ new String[] { "ENTER", "EXIT" }[this.monitorType];
	}

}