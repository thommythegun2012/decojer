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

	private final int locals;

	private int top;

	private V[] vs;

	/**
	 * Constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.locals = frame.locals;
		this.top = frame.top;
		this.vs = new V[this.locals + this.top];
		System.arraycopy(frame.vs, 0, this.vs, 0, this.vs.length);
	}

	/**
	 * Constructor.
	 * 
	 * @param locals
	 *            locals
	 */
	public Frame(final int locals) {
		this.locals = locals;
		this.vs = new V[locals];
	}

	/**
	 * Get local variable.
	 * 
	 * @param i
	 *            index
	 * @return variable
	 */
	public V get(final int i) {
		return this.vs[i];
	}

	/**
	 * Get locals.
	 * 
	 * @return locals
	 */
	public int getLocals() {
		return this.locals;
	}

	/**
	 * Get stack variable.
	 * 
	 * @param i
	 *            index
	 * @return variable
	 */
	public V getStack(final int i) {
		return this.vs[this.locals + i];
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
	 * Merge this frame with given frame (target types).
	 * 
	 * @param calculatedFrame
	 *            frame (contains target types)
	 * @return true - changed (this)
	 */
	public boolean merge(final Frame calculatedFrame) {
		boolean changed = false;
		for (int reg = this.locals; reg-- > 0;) {
			final V v = calculatedFrame.vs[reg];
			if (v == null) {
				continue;
			}
			if (this.vs[reg] == null) {
				this.vs[reg] = v;
				changed = true;
				continue;
			}
			changed |= this.vs[reg].merge(v.getT());
		}
		for (int i = this.top; i-- > 0;) {
			final V targetVar = this.vs[this.locals + i];
			final V calculatedVar = calculatedFrame.vs[this.locals + i];
			if (targetVar == calculatedVar) {
				continue;
			}

			// take new calculated variable, override propagation
			this.vs[this.locals + i] = calculatedVar;
			// TODO replace targetVar.getStartPc() - stack and requeue
			changed |= calculatedVar.merge(targetVar.getT());
		}
		return changed;
	}

	/**
	 * Peek stack variable.
	 * 
	 * @return variable
	 */
	public V peek() {
		if (this.top < 1) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.vs[this.locals + this.top - 1];
	}

	/**
	 * Pop stack variable.
	 * 
	 * @return variable
	 */
	public V pop() {
		if (this.top < 1) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.vs[this.locals + --this.top];
	}

	/**
	 * Push stack variable.
	 * 
	 * @param v
	 *            variable
	 */
	public void push(final V v) {
		if (this.locals + this.top >= this.vs.length) {
			final V[] newValues = new V[this.locals + this.top + 1];
			System.arraycopy(this.vs, 0, newValues, 0, this.locals + this.top);
			this.vs = newValues;
		}
		this.vs[this.locals + this.top++] = v;
	}

	/**
	 * Set register variable.
	 * 
	 * @param i
	 *            index
	 * @param v
	 *            variable
	 */
	public void set(final int i, final V v) {
		this.vs[i] = v;
	}

}