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
package org.decojer.cavaj.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

/**
 * Body declaration.
 * 
 * @author André Pankraz
 */
public abstract class BD implements PD {

	private final static Logger LOGGER = Logger.getLogger(BD.class.getName());

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * All body declarations: inner type / method / field declarations.
	 */
	@Getter
	private final List<BD> bds = new ArrayList<BD>();

	/**
	 * Parent declaration.
	 */
	@Getter
	protected PD pd;

	/**
	 * Add body declaration.
	 * 
	 * @param bd
	 *            bode declaration
	 */
	public void addBd(final BD bd) {
		if (bd.pd != null) {
			if (bd.pd != this) {
				LOGGER.warning("Cannot change parent declaration for '" + bd + "' from '" + bd.pd
						+ "' to '" + this + "'!");
			}
			return;
		}
		bd.pd = this;
		this.bds.add(bd);
	}

	/**
	 * Clear all generated data after read.
	 */
	void clear() {
		for (final BD bd : this.bds) {
			bd.clear();
		}
	}

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	public CU getCu() {
		if (this.pd instanceof CU) {
			return (CU) this.pd;
		}
		if (this.pd instanceof BD) {
			return ((BD) this.pd).getCu();
		}
		return null;
	}

}