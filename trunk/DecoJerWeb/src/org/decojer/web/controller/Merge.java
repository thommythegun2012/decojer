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
package org.decojer.web.controller;

import lombok.Getter;
import lombok.Setter;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;

/**
 * Merge.
 * 
 * @author André Pankraz
 */
public class Merge {

	@Getter
	@Setter
	public String type1;

	@Getter
	@Setter
	public String type2;

	public String getMergedType() {
		if (this.type1 == null || this.type2 == null) {
			return "";
		}
		final DU du = DecoJer.createDu();
		try {
			final T t1 = du.getT(this.type1);
			final T t2 = du.getT(this.type2);
			return "Merged type: " + t1.merge(t2).toString();
		} catch (final Exception e) {
			e.printStackTrace();
			return "Error: " + e.getMessage();
		}
	}

}