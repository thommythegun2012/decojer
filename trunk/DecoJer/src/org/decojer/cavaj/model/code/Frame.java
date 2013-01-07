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

/**
 * Frame.
 * 
 * @author André Pankraz
 */
public final class Frame {

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
	 * Get register number (locals).
	 * 
	 * @return register number (locals)
	 */
	public int getRegs() {
		return this.cfg.getRegs();
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
	 * Get stack register number (stack size).
	 * 
	 * @return stack register number (stack size)
	 */
	public int getTop() {
		return this.rs.length - this.cfg.getRegs();
	}

	/**
	 * Is stack empty?
	 * 
	 * @return {@code true} - stack is empty
	 */
	public boolean isStackEmpty() {
		return getTop() <= 0;
	}

	/**
	 * Load register (local or stack).
	 * 
	 * @param i
	 *            register index
	 * @return register (local or stack)
	 */
	public R load(final int i) {
		// stack allowed too: assert i < this.cfg.getRegs();

		return this.rs[i];
	}

	private void log(final String message) {
		LOGGER.warning(this.cfg.getMd() + ": " + message);
	}

	/**
	 * Peek stack register.
	 * 
	 * @return register
	 */
	public R peek() {
		assert !isStackEmpty();

		return this.rs[this.rs.length - 1];
	}

	/**
	 * Peek stack register.
	 * 
	 * @param i
	 *            reverse stack index
	 * @return register
	 */
	public R peek(final int i) {
		assert i < getTop();

		return this.rs[this.rs.length - i - 1];
	}

	public R peekSub(final int callerTop, final int subPc) {
		// JSR already visited, reuse Sub
		if (getTop() != callerTop + 1) {
			log("Wrong JSR Sub merge! Subroutine stack size different.");
			return null;
		}
		final R subR = peek();
		// currently not possible, register start pc is at operation pc, not the real store pc
		// if (subR.getPc() != subPc) {
		// LOGGER.warning("Wrong JSR Sub merge! Subroutine register has wrong start PC.");
		// return null;
		// }
		// now check if RET in Sub already visited
		if (!(subR.getValue() instanceof Sub)) {
			log("Wrong JSR Sub merge! Subroutine stack has wrong peek.");
			return null;
		}
		final Sub sub = (Sub) subR.getValue();
		if (sub.getPc() != subPc) {
			log("Wrong JSR Sub merge! Subroutine has wrong start PC.");
			return null;
		}
		if (this.subs[this.subs.length - 1] != sub) {
			log("Wrong JSR Sub merge! Subroutine register incompatible to subroutine stack.");
		}
		return subR;
	}

	/**
	 * Pop stack register.
	 * 
	 * @return stack register
	 */
	public R pop() {
		assert !isStackEmpty();

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
	 * @return {@code true} - success (found in stack, removed)
	 */
	public boolean popSub(final Sub sub) {
		if (this.subs != null) {
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
		}
		log("Illegal return from subroutine! Not in subroutine stack: " + sub);
		return false;
	}

	/**
	 * Push stack register.
	 * 
	 * @param s
	 *            stack register
	 * @return pushed register (for fluent API)
	 */
	public R push(final R s) {
		assert getTop() < this.cfg.getMaxStack() || this.cfg.getMaxStack() == 0;

		final R[] newRs = new R[this.rs.length + 1];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length);
		newRs[this.rs.length] = s;
		this.rs = newRs;
		return s;
	}

	/**
	 * Push subroutine to subroutine stack.
	 * 
	 * @param sub
	 *            subroutine
	 * @return {@code true} - success (not in stack, added)
	 */
	public boolean pushSub(final Sub sub) {
		if (this.subs == null) {
			this.subs = new Sub[] { sub };
			return true;
		}
		for (int i = this.subs.length; i-- > 0;) {
			if (this.subs[i].equals(sub)) {
				log("Recursive call to jsr entry!");
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
	 * @return replaced register (prevR or mergedR or null)
	 */
	public R replaceReg(final int i, final R prevR, final R newR) {
		assert prevR != null;

		// stack value already used, no replace
		if (i >= this.rs.length) {
			return null;
		}
		final R frameR = load(i);
		if (prevR == frameR) {
			store(i, newR);
			return prevR;
		}
		// this is wrong...stop replacing if different register reached!
		// if (newR == null) {
		// TODO currently only possible for exceptions, link later when really visited?!
		// assert frameR.getKind() == Kind.MERGE : frameR.getKind();

		// store(i, null);
		// return frameR; // now replace merge register with null
		// }
		if (frameR != null) {
			frameR.replaceIn(prevR, newR);
		}
		return null;
	}

	/**
	 * Get register number (local or stack).
	 * 
	 * @return register number (local or stack)
	 */
	public int size() {
		return this.rs.length;
	}

	/**
	 * Store register (local or stack).
	 * 
	 * @param i
	 *            register index
	 * @param r
	 *            register (local or stack)
	 * @return stored register (for fluent API)
	 */
	public R store(final int i, final R r) {
		// stack allowed too: assert i < this.cfg.getRegs();

		final R[] newRs = new R[this.rs.length];
		System.arraycopy(this.rs, 0, newRs, 0, this.rs.length);
		newRs[i] = r;
		this.rs = newRs;

		return r;
	}

	/**
	 * Store stack register.
	 * 
	 * @param i
	 *            stack index
	 * @param s
	 *            stack register
	 */
	public void storeS(final int i, final R s) {
		assert getTop() > i : i;

		store(getRegs() + i, s);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Frame (").append(getRegs());
		if (!isStackEmpty()) {
			sb.append(", ").append(getTop());
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
		for (int j = stacks, i = 0; j-- > 0; ++i) {
			if (peek(i).getT().isWide()) {
				--j;
				++wides;
			}
		}
		return wides;
	}

}