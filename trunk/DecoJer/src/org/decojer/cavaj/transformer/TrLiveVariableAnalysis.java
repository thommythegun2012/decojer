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

import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.INC;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.RETURN;
import org.decojer.cavaj.model.code.op.STORE;
import org.decojer.cavaj.model.code.op.SWITCH;
import org.decojer.cavaj.model.code.op.THROW;

/**
 * Transform Live Variable Analysis.
 * 
 * http://en.wikipedia.org/wiki/Liveness_analysis
 * 
 * @author André Pankraz
 */
public class TrLiveVariableAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrDataFlowAnalysis.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrLiveVariableAnalysis(cfg).transform();
	}

	private final CFG cfg;

	private V[][] vss;

	private TrLiveVariableAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private void analyze(final BB bb) {
		final List<Op> ops = bb.getOps();
		// first get live_out for BB, only last statement in BB can be
		// control flow statement, union of all successor live_in

		// but: check BB exception catches in each operation and union

		for (int pc = ops.size(); pc-- > 0;) {
			final Op op = ops.get(pc);

			switch (op.getOptype()) {
			case GOTO: {
				final GOTO cop = (GOTO) op;
				union(pc, cop.getTargetPc());
				continue;
			}
			case INC: {
				assert op instanceof INC;

				// TODO
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				union(pc, cop.getTargetPc());
				break;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				union(pc, cop.getTargetPc());
				break;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;
				union(pc, pc + 1);
				gen(pc, cop.getReg(), cop.getT());
				continue;
			}
			case RETURN: {
				assert op instanceof RETURN;

				continue;
			}
			case STORE: {
				final STORE cop = (STORE) op;
				kill(pc, cop.getReg());
				break;
			}
			case SWITCH: {
				final SWITCH cop = (SWITCH) op;
				union(pc, cop.getDefaultPc());
				for (final int casePc : cop.getCasePcs()) {
					union(pc, casePc);
				}
				continue;
			}
			case THROW: {
				assert op instanceof THROW;

				continue;
			}
			}
			union(pc, pc + 1);
		}
	}

	private void gen(final int pc, final int reg, final T t) {
		final V[] vs = this.vss[reg];
		if (vs == null) {
			this.vss[reg] = new V[1];
			this.vss[reg][0] = new V(t, null, pc, pc);
			return;
		}
		// TODO remember read number or pcs?
		for (final V v : vs) {

		}
	}

	private void kill(final int pc, final int reg) {

	}

	private void transform() {
		this.vss = new V[this.cfg.getMaxLocals()][];

		for (final BB bb : this.cfg.getPostorderedBbs()) {
			analyze(bb);
		}
	}

	private void union(final int pc, final int succPc) {

	}

}