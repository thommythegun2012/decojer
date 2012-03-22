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

import java.util.logging.Logger;

import org.decojer.cavaj.model.code.R.Kind;

/**
 * Frame.
 * 
 * @author André Pankraz
 */
public class Frame {

	private final static Logger LOGGER = Logger.getLogger(Frame.class.getName());

	private final CFG cfg;

	private R[] rs;

	private Sub[] subs;

	/**
	 * Constructor for first frame.
	 * 
	 * @param cfg
	 *            CFG
	 */
	protected Frame(final CFG cfg) {
		this.cfg = cfg;
		this.rs = new R[getRegs()];
	}

	/**
	 * Copy constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.cfg = frame.cfg;
		this.rs = frame.rs;
		this.subs = frame.subs;
	}

	/**
	 * Clear stack registers.
	 */
	public void clear() {
		final R[] newRs = new R[this.cfg.getRegs()];
		System.arraycopy(this.rs, 0, newRs, 0, newRs.length);
		this.rs = newRs;
	}

	/**
	 * Get register (local or stack).
	 * 
	 * @param index
	 *            register index
	 * @return register (local or stack)
	 */
	public R get(final int index) {
		return this.rs[index];
	}

	/**
	 * Get local register number.
	 * 
	 * @return local register number
	 */
	public int getRegs() {
		return this.cfg.getRegs();
	}

	/**
	 * Get stack register.
	 * 
	 * @param index
	 *            stack index
	 * @return register
	 */
	public R getS(final int index) {
		assert index < getStacks() : index;

		return this.rs[getRegs() + index];
	}

	/**
	 * Get stack register number (stack size).
	 * 
	 * @return stack register number (stack size)
	 */
	public int getStacks() {
		return this.rs.length - this.cfg.getRegs();
	}

	/**
	 * Get subroutine number.
	 * 
	 * @return subroutine number
	 */
	public int getSubs() {
		return this.subs == null ? 0 : this.subs.length;
	}

	/**
	 * Peek stack register.
	 * 
	 * @return register
	 */
	public R peek() {
		if (0 == getStacks()) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.rs[this.rs.length - 1];
	}

	/**
	 * Peek stack register.
	 * 
	 * @param index
	 *            reverse stack index, last is 1
	 * @return register
	 */
	public R peek(final int index) {
		assert index <= getStacks() : index;

		return this.rs[this.rs.length - index];
	}

	/**
	 * Peek stack register (not wide).
	 * 
	 * @return stack register
	 */
	public R peekSingle() {
		final R s = peek();
		if (s.isWide()) {
			LOGGER.warning("Attempt to split long or double on the stack!");
		}
		return s;
	}

	/**
	 * Peek stack register (not wide).
	 * 
	 * @param index
	 *            reverse stack index, last is 1
	 * @return stack register
	 */
	public R peekSingle(final int index) {
		final R s = peek(index);
		if (s.isWide()) {
			LOGGER.warning("Attempt to split long or double on the stack!");
		}
		return s;
	}

	/**
	 * Pop stack register.
	 * 
	 * @return stack register
	 */
	public R pop() {
		if (getStacks() == 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		final R s = this.rs[this.rs.length - 1];
		final R[] newRs = new R[this.rs.length - 1];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length - 1);
		this.rs = newRs;
		return s;
	}

	/**
	 * Pop stack register (not wide).
	 * 
	 * @return stack register
	 */
	public R popSingle() {
		final R s = pop();
		if (s.isWide()) {
			LOGGER.warning("Attempt to split long or double on the stack!");
		}
		return s;
	}

	/**
	 * Pop subroutine from subroutine stack: Pop all subroutines till given one.
	 * 
	 * @param sub
	 *            subroutine
	 * @return true - success (found in stack, removed)
	 */
	public boolean popSub(final Sub sub) {
		if (this.subs == null) {
			return false;
		}
		for (int i = this.subs.length; i-- > 0;) {
			if (this.subs[i].equals(sub)) {
				if (i == 0) {
					this.subs = null;
				} else {
					final Sub[] newSubs = new Sub[i];
					System.arraycopy(this.subs, 0, newSubs, 0, i);
					this.subs = newSubs;
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Push stack register.
	 * 
	 * @param s
	 *            stack register
	 */
	public void push(final R s) {
		if (getStacks() >= this.cfg.getMaxStack() && this.cfg.getMaxStack() != 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		final R[] newRs = new R[this.rs.length + 1];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length);
		newRs[this.rs.length] = s;
		this.rs = newRs;
	}

	/**
	 * Push subroutine to subroutine stack.
	 * 
	 * @param sub
	 *            subroutine
	 * @return true - success (not in stack, added)
	 */
	public boolean pushSub(final Sub sub) {
		if (this.subs == null) {
			this.subs = new Sub[] { sub };
			return true;
		}
		for (int i = this.subs.length; i-- > 0;) {
			if (this.subs[i].equals(sub)) {
				return false;
			}
		}
		final Sub[] newSubs = new Sub[this.subs.length + 1];
		System.arraycopy(this.subs, 0, newSubs, 0, this.subs.length);
		newSubs[this.subs.length] = sub;
		this.subs = newSubs;
		return true;
	}

	/**
	 * Replace register for merging.
	 * 
	 * @param i
	 *            register index
	 * @param prevR
	 *            previous register
	 * @param newR
	 *            new register or null
	 * @return replaced register (oldR or mergedR or null)
	 */
	public R replaceReg(final int i, final R prevR, final R newR) {
		assert prevR != null;

		// stack value already used, no replace
		if (i >= this.rs.length) {
			return null;
		}
		final R frameR = get(i);
		if (frameR == null) {
			return null;
		}
		if (frameR != prevR && (newR != null || frameR.getKind() != Kind.MERGE)) {
			frameR.replaceIn(prevR, newR);
			return null;
		}
		set(i, newR);
		return frameR;
	}

	/**
	 * Set register (local or stack).
	 * 
	 * @param i
	 *            register index
	 * @param r
	 *            register (local or stack)
	 */
	public void set(final int i, final R r) {
		final R[] newRs = new R[this.rs.length];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length);
		newRs[i] = r;
		this.rs = newRs;
	}

	/**
	 * Set stack register.
	 * 
	 * @param index
	 *            stack index
	 * @param s
	 *            stack register
	 */
	public void setS(final int index, final R s) {
		assert index < getStacks() : index;

		set(getRegs() + index, s);
	}

	/**
	 * Get register number (local or stack).
	 * 
	 * @return register number (local or stack)
	 */
	public int size() {
		return this.rs.length;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Frame (").append(getRegs());
		if (getStacks() != 0) {
			sb.append(", ").append(getStacks());
		}
		sb.append(") ");
		for (final R r : this.rs) {
			sb.append(r).append(", ");
		}
		return sb.substring(0, sb.length() - 2);
	}

	/**
	 * Get number of wide stack registers in given stack size.
	 * 
	 * @param stacks
	 *            stack size
	 * @return number of wide stack registers
	 */
	public int wideStacks(final int stacks) {
		int wides = 0;
		for (int j = stacks, i = 1; j-- > 0; ++i) {
			if (peek(i).isWide()) {
				--j;
				++wides;
			}
		}
		return wides;
	}

}