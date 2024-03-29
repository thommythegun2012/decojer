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
import javax.annotation.Nullable;

import lombok.Getter;

import org.decojer.cavaj.model.types.T;

/**
 * Operation 'PUSH'.
 *
 * @author André Pankraz
 */
public class PUSH extends TypedOp {

	@Getter
	@Nullable
	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param t
	 *            type
	 * @param value
	 *            value
	 */
	public PUSH(final int pc, final int opcode, final int line, @Nonnull final T t,
			@Nullable final Object value) {
		super(pc, opcode, line, t);

		this.value = value; // can be null
	}

	@Override
	public int getInStackSize() {
		return 0;
	}

	@Override
	public Optype getOptype() {
		return Optype.PUSH;
	}

	@Override
	public String toString() {
		final Object value = getValue();
		if (!(value instanceof String)) {
			return super.toString() + " " + value;
		}
		if (((String) value).length() <= 5) {
			return super.toString() + " \"" + value + "\"";
		}
		return super.toString() + " \"" + ((String) value).substring(0, 4) + "...\"";
	}

}