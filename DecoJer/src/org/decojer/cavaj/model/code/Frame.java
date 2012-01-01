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

/**
 * Frame.
 * 
 * @author André Pankraz
 */
public class Frame {

	private final int regs;

	private int top;

	private V[] vs;

	/**
	 * Copy constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.regs = frame.regs;
		this.top = frame.top;
		this.vs = new V[this.regs + this.top];
		System.arraycopy(frame.vs, 0, this.vs, 0, this.vs.length);
	}

	/**
	 * Constructor.
	 * 
	 * @param regs
	 *            register count (max locals)
	 */
	public Frame(final int regs) {
		this.regs = regs;
		this.vs = new V[regs];
	}

	/**
	 * Clear stack.
	 */
	public void clearStack() {
		this.top = 0;
	}

	/**
	 * Quick copy values from similar (previous) frame.
	 * 
	 * @param frame
	 *            similar (previous) frame
	 */
	public void copy(final Frame frame) {
		this.top = frame.top;
		System.arraycopy(frame.vs, 0, this.vs, 0, this.vs.length);
	}

	/**
	 * Get register variable.
	 * 
	 * @param reg
	 *            register
	 * @return variable
	 */
	public V get(final int reg) {
		return this.vs[reg];
	}

	/**
	 * Get register count.
	 * 
	 * @return register count
	 */
	public int getRegs() {
		return this.regs;
	}

	/**
	 * Get stack variable.
	 * 
	 * @param i
	 *            index
	 * @return variable
	 */
	public V getStack(final int i) {
		return this.vs[this.regs + i];
	}

	/**
	 * Get stack size.
	 * 
	 * @return stack size
	 */
	public int getStackSize() {
		return this.top;
	}

	/**
	 * Peek stack variable.
	 * 
	 * @return variable
	 */
	public V peek() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.vs[this.regs + this.top - 1];
	}

	/**
	 * Pop stack variable.
	 * 
	 * @return variable
	 */
	public V pop() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.vs[this.regs + --this.top];
	}

	/**
	 * Push stack variable.
	 * 
	 * @param v
	 *            variable
	 */
	public void push(final V v) {
		if (this.regs + this.top >= this.vs.length) {
			final V[] newVs = new V[this.regs + this.top + 1];
			System.arraycopy(this.vs, 0, newVs, 0, this.regs + this.top);
			this.vs = newVs;
		}
		this.vs[this.regs + this.top++] = v;
	}

	/**
	 * Set register variable.
	 * 
	 * @param reg
	 *            register
	 * @param v
	 *            variable
	 */
	public void set(final int reg, final V v) {
		this.vs[reg] = v;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Frame (").append(this.regs);
		if (this.top != 0) {
			sb.append(", ").append(this.top);
		}
		sb.append(") ");
		for (int i = 0; i < this.vs.length; ++i) {
			sb.append(this.vs[i]).append(", ");
		}
		return sb.substring(0, sb.length() - 2);
	}

}