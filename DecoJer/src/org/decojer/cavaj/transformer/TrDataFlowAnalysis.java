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

import java.util.Arrays;
import java.util.List;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.data.Frame;

/**
 * Transform Data Flow Analysis.
 * 
 * @author André Pankraz
 */
public class TrDataFlowAnalysis {

	public static void transform(final CFG cfg) {
		new TrDataFlowAnalysis(cfg).transform();
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

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private void transform() {
		final MD md = this.cfg.getMd();
		final M m = md.getM();
		final TD td = md.getTd();
		final T t = td.getT();

		// init first frame
		final Frame frame = new Frame();
		frame.registerTs = new T[this.cfg.getRegisterCount()];
		frame.varNames = new String[this.cfg.getRegisterCount()];
		final T[] paramTs = m.getParamTs();
		if (td.getVersion() == 0) {
			// Dalvik
			int locals = this.cfg.getRegisterCount() - paramTs.length;
			if (!m.checkAf(AF.STATIC)) {
				frame.registerTs[locals - 1] = t;
				frame.varNames[locals - 1] = "this";
				--locals;
			}
			Arrays.fill(frame.registerTs, 0, locals, T.UNINIT);
			System.arraycopy(paramTs, 0, frame.registerTs, locals,
					paramTs.length);
			for (int i = paramTs.length; i-- > 0;) {
				frame.varNames[locals + i] = m.getParamName(i);
			}
		} else {
			// JVM
			if (!m.checkAf(AF.STATIC)) {
				frame.registerTs[0] = t;
				frame.varNames[0] = "this";
			}
			for (int i = 0, j = 0; i < paramTs.length; ++i, ++j) {
				final T paramT = paramTs[i];
				frame.registerTs[j] = paramT;
				frame.varNames[j] = m.getParamName(i);
				// wide values need 2 registers, srsly?
				if (paramT == T.LONG || paramT == T.DOUBLE) {
					++j;
					// TODO better mark as unuseable?
					frame.registerTs[j] = T.UNINIT;
				}
			}
		}

		final List<BB> postorderedBBs = this.cfg.getPostorderedBbs();
		for (int postorder = postorderedBBs.size(); postorder-- > 0;) {
			final BB basicBlock = postorderedBBs.get(postorder);
		}
	}

}