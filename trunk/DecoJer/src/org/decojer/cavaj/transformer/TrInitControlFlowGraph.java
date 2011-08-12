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
package org.decojer.cavaj.transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;

/**
 * Transform Init Control Flow Graph.
 * 
 * @author André Pankraz
 */
public class TrInitControlFlowGraph {

	public static void transform(final CFG cfg) {
		new TrInitControlFlowGraph(cfg).transform();
	}

	public static void transform(final TD td) {
		final List<BD> bds = td.getBds();
		for (int i = 0; i < bds.size(); ++i) {
			final BD bd = bds.get(i);
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			if (cfg == null) {
				continue;
			}
			transform(cfg);
		}
	}

	private final CFG cfg;

	private TrInitControlFlowGraph(final CFG cfg) {
		this.cfg = cfg;
	}

	private CFG getCfg() {
		return this.cfg;
	}

	private void transform() {
		final Map<Integer, BB> pcBbs = new HashMap<Integer, BB>();
		final Operation[] operations = getCfg().getOperations();
		int pc = 0;
		while (true) {
			final Operation operation = operations[pc];
			switch (operation.getOpcode()) {
			case Opcode.GOTO: {
				final GOTO op = (GOTO) operation;
				pc = op.getTargetPc();
				break;
			}
			case Opcode.JCMP: {
				final GOTO op = (GOTO) operation;
				pc = op.getTargetPc();
				break;
			}
			default:
				++pc;
			}
		}
	}

}