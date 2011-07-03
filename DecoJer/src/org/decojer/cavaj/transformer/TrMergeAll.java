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

import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.eclipse.jdt.core.dom.BodyDeclaration;

public class TrMergeAll {

	public static void transform(final CU cu) {
		final List<TD> tds = cu.getTds();
		for (final TD td : tds) {
			cu.addTypeDeclaration(td.getTypeDeclaration());
			transform(td);
		}
	}

	private static void transform(final TD td) {
		final List<BD> bds = td.getBds();
		for (final BD bd : bds) {
			if (bd instanceof TD) {
				td.addBodyDeclarartion(((TD) bd).getTypeDeclaration());
				transform((TD) bd);
				continue;
			}
			if (bd instanceof FD) {
				final BodyDeclaration fieldDeclaration = ((FD) bd)
						.getFieldDeclaration();
				if (fieldDeclaration != null) {
					// e.g. enum constant declarations? TODO later add here,
					// because of line number sort?
					td.addBodyDeclarartion(fieldDeclaration);
				}
				continue;
			}
			if (bd instanceof MD) {
				final BodyDeclaration methodDeclaration = ((MD) bd)
						.getMethodDeclaration();
				if (methodDeclaration != null) {
					// e.g. bridge methods?
					td.addBodyDeclarartion(methodDeclaration);
				}
				continue;
			}
		}
	}

}