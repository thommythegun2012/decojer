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

import javax.annotation.Nullable;

import lombok.Getter;

import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;

/**
 * Operation 'INVOKE'.
 * 
 * @author André Pankraz
 */
public class INVOKE extends Op {

	/**
	 * Is direct call?
	 * 
	 * Constructor or supermethod (any super) or private method callout.
	 * 
	 * JVM: SPECIAL, Dalvik: DIRECT.
	 */
	@Getter
	private static final Object[] EXTRA_MARKER_DIRECT = new Object[0];

	@Getter
	private final M m;

	@Nullable
	private final Object extra[];

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param m
	 *            method
	 * @param direct
	 *            direct call
	 */
	public INVOKE(final int pc, final int opcode, final int line, final M m, final boolean direct) {
		super(pc, opcode, line);

		assert m != null;
		// for all variants valid: any supermethod possible for direct / static / interface,
		// for virtual anyway

		this.m = m;
		this.extra = direct ? EXTRA_MARKER_DIRECT : null;
	}

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 * @param m
	 *            dynamic method
	 * @param bsM
	 *            bootstrap method (delivering callsite)
	 * @param bsArgs
	 *            bootstrap method arguments
	 */
	public INVOKE(final int pc, final int opcode, final int line, final M m, final M bsM,
			final Object[] bsArgs) {
		super(pc, opcode, line);

		assert m != null;
		assert m.isDynamic();
		assert bsM != null;

		this.m = m;
		this.extra = new Object[1 + bsArgs.length];
		this.extra[0] = bsM;
		System.arraycopy(bsArgs, 0, this.extra, 1, bsArgs.length);
	}

	/**
	 * Get bootstrap method arguments.
	 * 
	 * @return bootstrap method arguments
	 */
	@Nullable
	public Object[] getBsArgs() {
		final Object[] extra = this.extra;
		if (extra == null || isDirect()) {
			return null;
		}
		final Object[] ret = new Object[extra.length - 1];
		System.arraycopy(extra, 1, ret, 0, ret.length);
		return ret;
	}

	/**
	 * Get bootstrap method.
	 * 
	 * @return bootstrap method
	 */
	@Nullable
	public M getBsM() {
		final Object[] extra = this.extra;
		if (extra == null || isDirect()) {
			return null;
		}
		return (M) extra[0];
	}

	@Override
	public int getInStackSize() {
		int inStackSize = getM().isStatic() ? 0 : getM().getT().getStackSize();
		for (final T paramT : getM().getParamTs()) {
			inStackSize += paramT.getStackSize();
		}
		return inStackSize;
	}

	@Override
	public Optype getOptype() {
		return Optype.INVOKE;
	}

	/**
	 * Is direct call?
	 * 
	 * @return is direct
	 */
	public boolean isDirect() {
		return this.extra == EXTRA_MARKER_DIRECT;
	}

	@Override
	public String toString() {
		return super.toString() + " " + this.m.getName() + this.m.getDescriptor();
	}

}