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

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.T;

/**
 * Operation.
 *
 * The operation code is the VM Code (Class / Dalvik, so far it's possible with reader abstraction).
 * Line numbers are only available if debug info given. The PC is the operation index, not the VM PC
 * (that is not available with Label-based readers). But the original PC / read order is preserved!
 *
 * @author André Pankraz
 */
public abstract class TypedOp extends Op {

	@Getter
	@Setter
	private T t;

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
	 */
	public TypedOp(final int pc, final int opcode, final int line, final T t) {
		super(pc, opcode, line);
		this.t = t;
	}

}