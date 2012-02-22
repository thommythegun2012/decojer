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

	private final int pc;

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
		this.pc = 0;
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
		this.pc = frame.pc;
		this.rs = frame.rs;
		this.subs = frame.subs;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	protected Frame(final int pc, final Frame frame) {
		this.cfg = frame.cfg;
		this.pc = pc;
		this.rs = frame.rs;
		this.subs = frame.subs;
	}

	/**
	 * Clear stack.
	 */
	public void clearStack() {
		final R[] newRs = new R[this.cfg.getRegs()];
		System.arraycopy(this.rs, 0, newRs, 0, newRs.length);
		this.rs = newRs;
	}

	/**
	 * Get register.
	 * 
	 * @param reg
	 *            register index
	 * @return register
	 */
	public R get(final int reg) {
		assert reg >= 0 : reg;

		return this.rs[reg];
	}

	/**
	 * Get frame pc.
	 * 
	 * @return frame pc
	 */
	public int getPc() {
		return this.pc;
	}

	/**
	 * Get register count.
	 * 
	 * @return register count
	 */
	public int getRegs() {
		return this.cfg.getRegs();
	}

	/**
	 * Get stack register.
	 * 
	 * @param i
	 *            stack index
	 * @return register
	 */
	public R getStack(final int i) {
		assert i >= 0 : i;

		if (i >= getStackSize()) {
			throw new IndexOutOfBoundsException("Stack too small!");
		}
		return this.rs[getRegs() + i];
	}

	/**
	 * Get stack size.
	 * 
	 * @return stack size
	 */
	public int getStackSize() {
		return this.rs.length - this.cfg.getRegs();
	}

	/**
	 * Get subroutine count.
	 * 
	 * @return subroutine count
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
		if (0 == getStackSize()) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.rs[this.rs.length - 1];
	}

	/**
	 * Peek stack register.
	 * 
	 * @param i
	 *            stack index, last is 1
	 * @return register
	 */
	public R peek(final int i) {
		assert i > 0;

		if (i > getStackSize()) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.rs[this.rs.length - i];
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
	 * @param i
	 *            stack index, last is 1
	 * @return stack register
	 */
	public R peekSingle(final int i) {
		final R s = peek(i);
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
		if (getStackSize() == 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		final R s = this.rs[this.rs.length - 1];
		final R[] newRs = new R[this.rs.length - 1];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length - 1);
		this.rs = newRs;
		return s;
	}

	/**
	 * Pop subroutine from subroutine stack: Pop all subroutines till given one.
	 * 
	 * @param sub
	 *            subroutine
	 * @return true - success (found in stack, removed)
	 */
	public boolean pop(final Sub sub) {
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
	 * Push stack register.
	 * 
	 * @param r
	 *            stack register
	 */
	public void push(final R r) {
		if (getStackSize() >= this.cfg.getMaxStack() && this.cfg.getMaxStack() != 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		final R[] newRs = new R[this.rs.length + 1];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length);
		newRs[this.rs.length] = r;
		this.rs = newRs;
	}

	/**
	 * Push subroutine to subroutine stack.
	 * 
	 * @param sub
	 *            subroutine
	 * @return true - success (not in stack, added)
	 */
	public boolean push(final Sub sub) {
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
	 * @param reg
	 *            register index
	 * @param oldR
	 *            old register, not null
	 * @param r
	 *            register
	 * @return replaced register (oldR or mergedR or null)
	 */
	public R replaceReg(final int reg, final R oldR, final R r) {
		assert oldR != null : oldR;

		// stack value already used, no replace
		if (reg >= this.rs.length) {
			return null;
		}
		final R frameR = get(reg);
		if (frameR == null) {
			return null;
		}
		if (frameR != oldR && (r != null || frameR.getKind() != Kind.MERGE)) {
			frameR.replaceIn(oldR, r);
			return null;
		}
		set(reg, r);
		return frameR;
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
		assert reg >= 0 : reg;

		final R[] newRs = new R[this.rs.length];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length);
		newRs[reg] = r;
		this.rs = newRs;
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
		assert i >= 0 : i;

		if (i >= getStackSize()) {
			throw new IndexOutOfBoundsException("Stack too small!");
		}
		set(getRegs() + i, r);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Frame (").append(getRegs());
		if (getStackSize() != 0) {
			sb.append(", ").append(getStackSize());
		}
		sb.append(") ");
		for (final R r : this.rs) {
			sb.append(r).append(", ");
		}
		return sb.substring(0, sb.length() - 2);
	}

}