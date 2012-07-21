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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;

/**
 * Transformer: Analyze inner classes and create compilation units.
 * 
 * @author André Pankraz
 */
public class TrInnerClassesAnalysis {

	private static TD findOuterTd(final TD td) {
		final String name = td.getName();
		final int pos = name.lastIndexOf('$');
		return td.getDu().getTd(name.substring(0, pos));
	}

	private static List<TD> findTopTds(final DU du) {
		final List<TD> tds = new ArrayList<TD>();
		for (final TD td : du.getTds()) {
			if (td.getName().lastIndexOf('$') >= 0) {
				// is inner name, check direct parent
				final TD outerTd = findOuterTd(td);
				if (outerTd != null) {
					outerTd.addTd(td);
					// parent checked earlier or later
					continue;
				}
				// no matching parent read till now...live with that
				tds.add(td);
				continue;
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
		return packageName == null ? sourceFileName : packageName + "/" + sourceFileName;
	}

	public static void transform(final DU du) {
		final List<TD> topTds = findTopTds(du);
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
		du.setCus(cus);
	}

}