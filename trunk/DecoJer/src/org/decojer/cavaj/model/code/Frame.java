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

	private Var[] values;

	/**
	 * Constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.locals = frame.locals;
		this.top = frame.top;
		this.values = new Var[this.locals + this.top];
		System.arraycopy(frame.values, 0, this.values, 0, this.values.length);
	}

	/**
	 * Constructor.
	 * 
	 * @param locals
	 *            locals
	 */
	public Frame(final int locals) {
		this.locals = locals;
		this.values = new Var[locals];
	}

	/**
	 * Get local variable.
	 * 
	 * @param index
	 *            index
	 * @return variable
	 */
	public Var get(final int index) {
		return this.values[index];
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
	 * @param index
	 *            index
	 * @return variable
	 */
	public Var getStack(final int index) {
		return this.values[this.locals + index];
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
			final Var var = calculatedFrame.values[reg];
			if (var == null) {
				continue;
			}
			if (this.values[reg] == null) {
				this.values[reg] = var;
				changed = true;
				continue;
			}
			changed |= this.values[reg].merge(var.getT());
		}
		for (int index = this.top; index-- > 0;) {
			final Var targetVar = this.values[this.locals + index];
			final Var calculatedVar = calculatedFrame.values[this.locals + index];
			if (targetVar == calculatedVar) {
				continue;
			}

			// take new calculated var, override propagation
			this.values[this.locals + index] = calculatedVar;
			// TODO replace targetVar.getStartPc() - stack and requeue
			changed |= calculatedVar.merge(targetVar.getT());
		}
		return changed;
	}

	/**
	 * Peek stack variable.
	 * 
	 * @return stack variable
	 */
	public Var peek() {
		if (this.top < 1) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.values[this.locals + this.top - 1];
	}

	/**
	 * Pop stack variable.
	 * 
	 * @return variable
	 */
	public Var pop() {
		if (this.top < 1) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.values[this.locals + --this.top];
	}

	/**
	 * Push stack variable.
	 * 
	 * @param var
	 *            variable
	 */
	public void push(final Var var) {
		if (this.locals + this.top >= this.values.length) {
			final Var[] newValues = new Var[this.locals + this.top + 1];
			System.arraycopy(this.values, 0, newValues, 0, this.locals + this.top);
			this.values = newValues;
		}
		this.values[this.locals + this.top++] = var;
	}

	/**
	 * Set register variable.
	 * 
	 * @param index
	 *            index
	 * @param var
	 *            variable
	 */
	public void set(final int index, final Var var) {
		this.values[index] = var;
	}

}