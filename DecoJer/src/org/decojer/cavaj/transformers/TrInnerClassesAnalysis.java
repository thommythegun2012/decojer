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
package org.decojer.cavaj.transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.ops.NEW;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.types.ClassT;

/**
 * Transformer: Analyze inner classes and create compilation units.
 * 
 * @author André Pankraz
 */
public class TrInnerClassesAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrInnerClassesAnalysis.class.getName());

	private static void findEnclosingMethods(final Collection<T> ts) {
		for (final T t : ts) {
			final TD td = t.getTd();
			if (td == null) {
				continue;
			}
			for (final BD bd : td.getBds()) {
				if (!(bd instanceof MD)) {
					continue;
				}
				final MD enclosingMd = (MD) bd;
				final CFG cfg = enclosingMd.getCfg();
				if (cfg == null) {
					continue;
				}
				final Op[] ops = cfg.getOps();
				if (ops == null) {
					continue;
				}
				for (final Op op : ops) {
					if (!(op instanceof NEW)) {
						continue;
					}
					final T newT = ((NEW) op).getT();
					if (!newT.isAnonymous()) {
						continue;
					}
					if (!(newT instanceof ClassT)) {
						continue;
					}
					final M enclosingM = newT.getEnclosingM();
					if (enclosingM != null) {
						// TODO check if equal
					}
					final T enclosingT = newT.getEnclosingT();
					if (enclosingT != null) {
						// TODO check if equal
					}
					// TODO repair T or set TD?
					enclosingMd.addTd(td); // TODO better setEnclosingMd() ???
				}
			}
		}
	}

	private static List<TD> findTopTds(final Collection<T> ts) {
		final List<TD> tds = new ArrayList<TD>();
		// separate all read tds, not just selected tds
		for (final T t : ts) {
			final TD td = t.getTd();
			if (td == null) {
				continue;
			}
			// Inner name is not necessary anymore since JRE 5, see T#getInnerName(), but we
			// validate the new "Binary Compatibility" rules here.
			if (td.getVersion() >= 48 && t.getEnclosingT() != null) {
				final String innerName = t.getInnerName();
				final String simpleName = t.getSimpleClassName();
				if (innerName == null && !simpleName.isEmpty() || innerName != null
						&& !innerName.equals(simpleName)) {
					LOGGER.warning("Inner name '" + innerName
							+ "' is different from enclosing info '" + simpleName + "'!");
				}
			}

			// first check enclosing method, potentially deeper nested than in type
			final M enclosingM = t.getEnclosingM();
			if (enclosingM != null) {
				final MD enclosingMd = enclosingM.getMd();
				if (enclosingMd != null) {
					enclosingMd.addTd(td);
					continue;
				}
			}
			final ClassT enclosingT = t.getEnclosingT();
			if (enclosingT != null) {
				final TD enclosingTd = enclosingT.getTd();
				if (enclosingTd != null) {
					enclosingTd.addTd(td);
					continue;
				}
			}
			tds.add(td);
		}
		return tds;
	}

	private static String getSourceId(final TD mainTd) {
		final String sourceFileName = mainTd.getSourceFileName();
		if (sourceFileName == null) {
			return null;
		}
		final String packageName = mainTd.getPackageName();
		return packageName == null ? sourceFileName : packageName + "." + sourceFileName;
	}

	public static void transform(final DU du) {
		final Collection<T> ts = du.getTs();

		findEnclosingMethods(ts);
		final List<TD> topTds = findTopTds(ts);

		final List<CU> cus = new ArrayList<CU>();
		final Map<String, CU> sourceId2cu = new HashMap<String, CU>();
		for (final TD topTd : topTds) {
			final String sourceId = getSourceId(topTd);
			if (sourceId != null) {
				final CU cu = sourceId2cu.get(sourceId);
				if (cu != null) {
					cu.addTd(topTd);
					continue;
				}
			}
			final String sourceFileName = sourceId != null ? topTd.getSourceFileName() : topTd
					.getPName() + ".java";
			final CU cu = new CU(topTd, sourceFileName);
			if (sourceId != null) {
				sourceId2cu.put(sourceId, cu);
			}
			cus.add(cu);
		}
		// not very optimized...but it works for now...
		final List<CU> selectedCus = new ArrayList<CU>();
		for (final CU cu : cus) {
			for (final TD td : cu.getAllTds()) {
				if (du.getSelectedTds().contains(td)) {
					selectedCus.add(cu);
					break;
				}
			}
		}
		du.setCus(selectedCus);
	}

}