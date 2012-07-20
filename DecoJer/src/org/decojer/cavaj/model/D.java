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

/**
 * Declaration.
 * 
 * @author André Pankraz
 */
public abstract class D {

	private final static Logger LOGGER = Logger.getLogger(BD.class.getName());

	/**
	 * All body declarations: inner type / method / field declarations.
	 */
	@Getter
	private final List<BD> bds = new ArrayList<BD>();

	/**
	 * Add body declaration.
	 * 
	 * @param bd
	 *            bode declaration
	 */
	protected void addBd(final BD bd) {
		if (bd.parent != null) {
			if (bd.parent != this) {
				LOGGER.warning("Cannot change parent declaration for '" + bd + "' from '"
						+ bd.parent + "' to '" + this + "'!");
			}
			return;
		}
		bd.parent = this;
		this.bds.add(bd);
	}

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void addTd(final TD td) {
		addBd(td);
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		for (final BD bd : this.bds) {
			bd.clear();
		}
	}

	public abstract String getName();

}