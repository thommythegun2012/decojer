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
 * Register.
 * 
 * @author André Pankraz
 */
public class R {

	public enum Kind {

		MERGE, // merge ins

		PUSH, // no in

		STORE // previous r and new source r

	}

	private R[] ins;

	private final Kind kind;

	private R[] outs;

	private T t;

	private final Object value;

	public R(final T t, final Kind kind, final R... ins) {
		this(t, null, kind, ins);
	}

	public R(final T t, final Object value, final Kind kind, final R... ins) {
		this.t = t;
		this.value = value;
		this.kind = kind;
		if (ins != null) {
			this.ins = ins;
			for (final R in : ins) {
				final R[] inOuts = in.outs;
				if (in.outs == null) {
					in.outs = new R[] { this };
				} else {
					in.outs = new R[inOuts.length + 1];
					System.arraycopy(inOuts, 0, in.outs, 0, inOuts.length);
					in.outs[inOuts.length] = this;
				}
			}
		}
	}

	public T getT() {
		return this.t;
	}

	public void setT(final T t) {
		this.t = t;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("R: ");
		sb.append(this.t);
		return sb.toString();
	}

}