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

import java.util.HashMap;
import java.util.Map;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;

/**
 * Transformer: Identify and create compilation units.
 * 
 * @author André Pankraz
 */
public class TrCreateCompilationUnits {

	public static void transform(final DU du) {
		final Map<String, TD> units = new HashMap<String, TD>();
		for (final TD td : du.getTds()) {
			System.out.println("TD Source: " + td.getSourceFileName());
			final String packageName = td.getPackageName();
		}
		du.setCus(null);
	}

}