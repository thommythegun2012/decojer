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

import org.decojer.cavaj.model.T;

/**
 * Exception handler.
 * 
 * @author André Pankraz
 */
public final class Exc {

	private int endPc;

	private int handlerPc;

	private int startPc;

	private final T t;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type (null => catch all)
	 */
	public Exc(final T t) {
		this.t = t;
	}

	/**
	 * Get end pc.
	 * 
	 * @return end pc
	 */
	public int getEndPc() {
		return this.endPc;
	}

	/**
	 * Get handler pc.
	 * 
	 * @return handler pc
	 */
	public int getHandlerPc() {
		return this.handlerPc;
	}

	/**
	 * Get start pc.
	 * 
	 * @return start pc
	 */
	public int getStartPc() {
		return this.startPc;
	}

	/**
	 * Get type (null => catch all).
	 * 
	 * @return type (null => catch all)
	 */
	public T getT() {
		return this.t;
	}

	/**
	 * Set end pc.
	 * 
	 * @param endPc
	 *            end pc
	 */
	public void setEndPc(final int endPc) {
		this.endPc = endPc;
	}

	/**
	 * Set handler pc.
	 * 
	 * @param handlerPc
	 *            handler pc
	 */
	public void setHandlerPc(final int handlerPc) {
		this.handlerPc = handlerPc;
	}

	/**
	 * Set start pc.
	 * 
	 * @param startPc
	 *            start pc
	 */
	public void setStartPc(final int startPc) {
		this.startPc = startPc;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Exc");
		sb.append("(").append(this.startPc).append(" - ").append(this.endPc).append(") ");
		sb.append(this.t).append(": ").append(this.handlerPc);
		return sb.toString();
	}

	/**
	 * Is variable valid for pc?
	 * 
	 * @param pc
	 *            pc
	 * @return true - variable valid for pc
	 */
	public boolean validIn(final int pc) {
		// end pc is first pc _after_ (multiple byte) operation (even after final return)
		return this.startPc <= pc && pc < this.endPc;
	}

}