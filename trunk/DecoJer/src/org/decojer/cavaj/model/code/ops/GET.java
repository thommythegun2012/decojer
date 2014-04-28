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
package org.decojer.cavaj.model.code.ops;

import javax.annotation.Nonnull;

import lombok.Getter;

import org.decojer.cavaj.model.fields.F;

/**
 * Operation 'GET'.
 *
 * @author André Pankraz
 */
public class GET extends Op {

	@Getter
	@Nonnull
	private final F f;

	/**
	 * Constructor.
	 *
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param f
	 *            field
	 */
	public GET(final int pc, final int opcode, final int line, @Nonnull final F f) {
		super(pc, opcode, line);
		// for all variants valid: any superfield possible for static / instance
		this.f = f;
	}

	@Override
	public int getInStackSize() {
		return getF().isStatic() ? 0 : getF().getT().getStackSize();
	}

	@Override
	public Optype getOptype() {
		return Optype.GET;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		final F f = getF();
		if (f.isStatic()) {
			sb.append('S');
		}
		sb.append(' ').append(f.getT().getPName()).append('.').append(f.getName());
		final String ret = sb.toString();
		assert ret != null;
		return ret;
	}

}