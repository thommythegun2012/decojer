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
package org.decojer.cavaj.model.code;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.T;

/**
 * Edge for CFG.
 * 
 * @author André Pankraz
 */
public final class E {

	@Getter
	@Setter
	private BB end;

	@Getter
	@Setter
	private BB start;

	@Getter
	private final Object value;

	/**
	 * Constructor.
	 * 
	 * @param start
	 *            start basic block
	 * @param end
	 *            end basic block
	 * @param value
	 *            value
	 */
	public E(final BB start, final BB end, final Object value) {
		this.start = start;
		this.end = end;
		this.value = value;
	}

	/**
	 * Is back edge?
	 * 
	 * @return true - is back edge
	 */
	public boolean isBack() {
		// equal: check self back edge too
		return this.start.getPostorder() <= this.end.getPostorder();
	}

	/**
	 * Is catch?
	 * 
	 * @return true - is catch
	 */
	public boolean isCatch() {
		return this.value instanceof T[];
	}

	/**
	 * Is conditional?
	 * 
	 * @return true - is conditional
	 */
	public boolean isCond() {
		return this.value instanceof Boolean;
	}

	/**
	 * Is switch case?
	 * 
	 * @return true - is switch case
	 */
	public boolean isSwitch() {
		return this.value instanceof Integer[];
	}

}