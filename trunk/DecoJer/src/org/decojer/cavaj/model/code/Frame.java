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

	private final Var[] regs;

	private Var[] stack;

	private int stackTop;

	/**
	 * Constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.regs = frame.regs.clone();
		this.stack = new Var[frame.stackTop];
		System.arraycopy(frame.stack, 0, this.stack, 0, frame.stackTop);
		this.stackTop = frame.stackTop;
	}

	/**
	 * Constructor.
	 * 
	 * @param regsSize
	 *            registers size
	 */
	public Frame(final int regsSize) {
		this.regs = new Var[regsSize];
		this.stack = new Var[0];
	}

	/**
	 * Get register variable.
	 * 
	 * @param index
	 *            index
	 * @return variable
	 */
	public Var getReg(final int index) {
		return this.regs[index];
	}

	/**
	 * Get registers size.
	 * 
	 * @return registers size
	 */
	public int getRegsSize() {
		return this.regs.length;
	}

	/**
	 * Get stack variable.
	 * 
	 * @param index
	 *            index
	 * @return variable
	 */
	public Var getStack(final int index) {
		return this.stack[index];
	}

	/**
	 * Get stack size.
	 * 
	 * @return stack size
	 */
	public int getStackTop() {
		return this.stackTop;
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
		for (int reg = this.regs.length; reg-- > 0;) {
			final Var var = calculatedFrame.regs[reg];
			if (var == null) {
				continue;
			}
			if (this.regs[reg] == null) {
				this.regs[reg] = var;
				changed = true;
				continue;
			}
			changed |= this.regs[reg].merge(var.getT());
		}
		for (int index = this.stackTop; index-- > 0;) {
			final Var targetVar = this.stack[index];
			final Var calculatedVar = calculatedFrame.stack[index];
			if (targetVar == calculatedVar) {
				continue;
			}

			// take new calculated var, override propagation
			this.stack[index] = calculatedVar;
			// TODO replace targetVar.getStartPc() - stack and requeue
			changed |= calculatedVar.merge(targetVar.getT());
		}
		return changed;
	}

	/**
	 * Peek variable from stack.
	 * 
	 * @return variable
	 */
	public Var peek() {
		if (this.stackTop < 1) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.stack[this.stackTop - 1];
	}

	/**
	 * Pop variable from stack.
	 * 
	 * @return variable
	 */
	public Var pop() {
		if (this.stackTop < 1) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.stack[--this.stackTop];
	}

	/**
	 * Push variable to stack.
	 * 
	 * @param var
	 *            variable
	 */
	public void push(final Var var) {
		if (this.stackTop >= this.stack.length) {
			final Var[] newStack = new Var[this.stackTop + 1];
			System.arraycopy(this.stack, 0, newStack, 0, this.stackTop);
			this.stack = newStack;
		}
		this.stack[this.stackTop++] = var;
	}

	/**
	 * Set register variable.
	 * 
	 * @param index
	 *            index
	 * @param var
	 *            variable
	 */
	public void setReg(final int index, final Var var) {
		this.regs[index] = var;
	}

}