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

	private R[] rs;

	private int top;

	/**
	 * Copy constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.regs = frame.regs;
		this.top = frame.top;
		this.rs = new R[this.regs + this.top];
		System.arraycopy(frame.rs, 0, this.rs, 0, this.rs.length);
	}

	/**
	 * Constructor.
	 * 
	 * @param regs
	 *            register count (max locals)
	 */
	public Frame(final int regs) {
		this.regs = regs;
		this.rs = new R[regs];
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
		System.arraycopy(frame.rs, 0, this.rs, 0, this.rs.length);
	}

	/**
	 * Get register.
	 * 
	 * @param reg
	 *            register
	 * @return register
	 */
	public R get(final int reg) {
		return this.rs[reg];
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
	 * Get stack register.
	 * 
	 * @param i
	 *            stack index
	 * @return register
	 */
	public R getStack(final int i) {
		return this.rs[this.regs + i];
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
	 * Peek stack register.
	 * 
	 * @return register
	 */
	public R peek() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.rs[this.regs + this.top - 1];
	}

	/**
	 * Pop stack register.
	 * 
	 * @return stack register
	 */
	public R pop() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.rs[this.regs + --this.top];
	}

	/**
	 * Push stack register.
	 * 
	 * @param r
	 *            stack register
	 */
	public void push(final R r) {
		if (this.regs + this.top >= this.rs.length) {
			final R[] newRs = new R[this.regs + this.top + 1];
			System.arraycopy(this.rs, 0, newRs, 0, this.regs + this.top);
			this.rs = newRs;
		}
		this.rs[this.regs + this.top++] = r;
	}

	/**
	 * Set register.
	 * 
	 * @param reg
	 *            register index
	 * @param r
	 *            register
	 */
	public void set(final int reg, final R r) {
		this.rs[reg] = r;
	}

	/**
	 * Set stack register.
	 * 
	 * @param i
	 *            stack index
	 * @param r
	 *            register
	 */
	public void setStack(final int i, final R r) {
		this.rs[this.regs + i] = r;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Frame (").append(this.regs);
		if (this.top != 0) {
			sb.append(", ").append(this.top);
		}
		sb.append(") ");
		for (int i = 0; i < this.rs.length; ++i) {
			sb.append(this.rs[i]).append(", ");
		}
		return sb.substring(0, sb.length() - 2);
	}

}