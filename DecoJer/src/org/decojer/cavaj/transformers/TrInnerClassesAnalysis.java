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

	public static void transform(final DU du) {
		final List<CU> cus = new ArrayList<CU>();
		final Map<String, CU> sourceFileName2cu = new HashMap<String, CU>();
		for (final TD td : du.getTds()) {
			final String name = td.getName();
			final int pos = name.lastIndexOf('$');
			// TODO inner/outer info
			if (pos >= 0) {
				// is inner name, check direct parent
				final TD pd = du.getTd(name.substring(0, pos));
				if (pd != null) {
					pd.addTd(td);
					// parent checked earlier or later
					continue;
				}
				// no matching parent read till now...live with that, create cu, no source check
				cus.add(new CU(td));
				continue;
			}
			final String sourceFileName = td.getSourceFileName();
			if (sourceFileName != null) {
				final CU cu = sourceFileName2cu.get(sourceFileName);
				if (cu != null) {
					cu.addTd(td);
					continue;
				}
			}
			final CU cu = new CU(td);
			if (sourceFileName != null) {
				sourceFileName2cu.put(sourceFileName, cu);
			}
			cus.add(cu);
		}
		du.setCus(cus);
	}

}