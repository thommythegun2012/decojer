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

import org.decojer.cavaj.model.types.T;

/**
 * Exception handler.
 * 
 * @author André Pankraz
 */
@Getter
@Setter
public final class Exc {

	private int endPc;

	private int handlerPc;

	private int startPc;

	private T t; // null -> catch all

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type (null => catch all)
	 */
	public Exc(final T t) {
		this.t = t;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Exc");
		sb.append("(").append(this.startPc).append(" - ").append(this.endPc).append(") ");
		sb.append(this.t).append(": ").append(this.handlerPc);
		final String ret = sb.toString();
		assert ret != null : "cannot be null";
		return ret;
	}

	/**
	 * Is variable valid for pc?
	 * 
	 * @param pc
	 *            pc
	 * @return {@code true} - variable valid for pc
	 */
	public boolean validIn(final int pc) {
		// JVM Spec: "The start_pc is inclusive and end_pc is exclusive":
		// end pc is first pc _after_ (multiple byte) operation (even after final return);
		// one exception are final RETURNS in try!!!
		return this.startPc <= pc && pc < this.endPc;
	}

}