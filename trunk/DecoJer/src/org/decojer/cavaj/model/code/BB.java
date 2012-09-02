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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Struct;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

/**
 * Basic block for CFG.
 * 
 * @author André Pankraz
 */
public final class BB {

	@Getter
	private final CFG cfg;

	@Getter
	private final ArrayList<E> ins = new ArrayList<E>(2);

	private final List<Op> ops = new ArrayList<Op>();

	@Getter
	protected final ArrayList<E> outs = new ArrayList<E>(2);

	/**
	 * Must cache and manage first operation PC separately because operations are removed through
	 * Java statement transformations.
	 */
	@Getter
	@Setter
	private int pc;

	@Getter
	@Setter
	private int postorder;

	private final List<Statement> stmts = new ArrayList<Statement>();

	@Getter
	@Setter
	private Struct struct;

	/**
	 * Stack expression number (stack size).
	 */
	@Getter
	private int top;

	private Expression[] vs;

	protected BB(final CFG cfg, final int pc) {
		this.cfg = cfg;
		this.pc = pc;
		this.vs = new Expression[getRegs()];
	}

	/**
	 * Add catch successors.
	 * 
	 * @param handlerTypes
	 *            handler types, null is any
	 * @param handlerSucc
	 *            handler successor
	 */
	public void addCatchSucc(final T[] handlerTypes, final BB handlerSucc) {
		final E out = new E(this, handlerSucc, handlerTypes);
		this.outs.add(out);
		handlerSucc.ins.add(out);
	}

	/**
	 * Add operation.
	 * 
	 * @param op
	 *            operation
	 */
	public void addOp(final Op op) {
		this.ops.add(op);
	}

	/**
	 * Add statement.
	 * 
	 * @param stmt
	 *            statement
	 */
	public void addStmt(final Statement stmt) {
		this.stmts.add(stmt);
	}

	/**
	 * Add switch successors.
	 * 
	 * TODO setSwitchSuccs() better?
	 * 
	 * @param caseKeys
	 *            case keys, null is default
	 * @param caseSucc
	 *            case successor
	 */
	public void addSwitchSucc(final Integer[] caseKeys, final BB caseSucc) {
		final E out = new E(this, caseSucc, caseKeys);
		this.outs.add(out);
		caseSucc.ins.add(out);
	}

	/**
	 * Copy content from BB.
	 * 
	 * @param bb
	 *            BB
	 */
	public void copyContentFrom(final BB bb) {
		this.ops.addAll(bb.ops);
		this.stmts.addAll(bb.stmts);
		if (bb.top > 0) {
			if (getRegs() + this.top + bb.top > this.vs.length) {
				final Expression[] newVs = new Expression[getRegs() + this.top + bb.top];
				System.arraycopy(this.vs, 0, newVs, 0, getRegs() + this.top);
				this.vs = newVs;
			}
			System.arraycopy(bb.vs, bb.getRegs(), this.vs, getRegs() + this.top, bb.top);
			this.top += bb.top;
		}
	}

	/**
	 * Get local expression.
	 * 
	 * @param i
	 *            index
	 * @return expression
	 */
	public Expression get(final int i) {
		return this.vs[i];
	}

	/**
	 * Get catch out edges.
	 * 
	 * @return catch out edges
	 */
	public List<E> getCatchOuts() {
		final ArrayList<E> catchOuts = new ArrayList<E>();
		for (final E out : this.outs) {
			if (out.isCatch()) {
				catchOuts.add(out);
			}
		}
		return catchOuts;
	}

	/**
	 * Get (conditional) false out edge.
	 * 
	 * @return false out edge
	 */
	public E getFalseOut() {
		for (final E out : this.outs) {
			if (Boolean.FALSE == out.getValue()) {
				return out;
			}
		}
		return null;
	}

	/**
	 * Get false (branch) successor (for conditionals only, else exception).
	 * 
	 * @return false (branch) successor
	 */
	public BB getFalseSucc() {
		return getFalseOut().getEnd();
	}

	/**
	 * Get final operation.
	 * 
	 * @return final operation or null
	 */
	public Op getFinalOp() {
		return this.ops.isEmpty() ? null : this.ops.get(this.ops.size() - 1);
	}

	/**
	 * Get final statement.
	 * 
	 * @return final statement or null
	 */
	public Statement getFinalStmt() {
		return this.stmts.isEmpty() ? null : this.stmts.get(this.stmts.size() - 1);
	}

	/**
	 * Get immediate dominator (IDom).
	 * 
	 * @return immediate domminator (IDom)
	 */
	public BB getIDom() {
		return getCfg().getIDom(this);
	}

	/**
	 * Get first operation line.
	 * 
	 * @return first operation line
	 */
	public int getLine() {
		return this.cfg.getOps()[this.pc].getLine();
	}

	/**
	 * Get operation at index.
	 * 
	 * @param i
	 *            operation index
	 * @return operation
	 */
	public Op getOp(final int i) {
		return this.ops.get(i);
	}

	/**
	 * Get operations number.
	 * 
	 * @return operations number
	 */
	public int getOps() {
		return this.ops.size();
	}

	/**
	 * Get order index, may be line number if given or operation program counter.
	 * 
	 * @return order index, may be line number if given or operation program counter
	 */
	public int getOrder() {
		final int opLine = getLine();
		return opLine == -1 ? getPc() : opLine;
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
	 * Get statement at index.
	 * 
	 * @param i
	 *            statement index
	 * @return statement or null
	 */
	public Statement getStmt(final int i) {
		final int size = this.stmts.size();
		return size <= i ? null : this.stmts.get(i);
	}

	/**
	 * Get statements numer.
	 * 
	 * @return statements number
	 */
	public int getStmts() {
		return this.stmts.size();
	}

	/**
	 * Get switch out edges.
	 * 
	 * @return switch out edges
	 */
	public List<E> getSwitchOuts() {
		final ArrayList<E> switchOuts = new ArrayList<E>();
		for (final E out : this.outs) {
			if (out.isSwitch()) {
				switchOuts.add(out);
			}
		}
		return switchOuts;
	}

	/**
	 * Get (conditional) true out edge.
	 * 
	 * @return true out edge
	 */
	public E getTrueOut() {
		for (final E out : this.outs) {
			if (Boolean.TRUE == out.getValue()) {
				return out;
			}
		}
		return null;
	}

	/**
	 * Get true (branch) successor (for conditionals only, else exception).
	 * 
	 * @return true (branch) successor
	 */
	public BB getTrueSucc() {
		return getTrueOut().getEnd();
	}

	/**
	 * Has BB necessary stack size for given operation?
	 * 
	 * @param op
	 *            operation
	 * @return {@code true} - BB has necessary stack size
	 */
	public boolean hasStackSizeFor(final Op op) {
		return op.getInStackSize() - this.cfg.getInFrame(op).wideStacks(op.getInStackSize()) <= getTop();
	}

	/**
	 * Is final statement conditional (IfStatement)?
	 * 
	 * @return {@code true} - final statement is conditional
	 */
	public boolean isFinalStmtCond() {
		return this.stmts.isEmpty() ? false
				: this.stmts.get(this.stmts.size() - 1) instanceof IfStatement;
	}

	/**
	 * Is final statement switch (SwitchStatement)?
	 * 
	 * @return {@code true} - final statement is switch
	 */
	public boolean isFinalStmtSwitch() {
		return this.stmts.isEmpty() ? false
				: this.stmts.get(this.stmts.size() - 1) instanceof SwitchStatement;
	}

	/**
	 * Is BB an exception handler?
	 * 
	 * @return {@code true} - BB is an exception handler
	 */
	public boolean isHandler() {
		for (final E in : this.ins) {
			if (in.isCatch()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Move in edges to target BB. Adjust CFG start BB.
	 * 
	 * @param target
	 *            target BB
	 */
	public void moveIns(final BB target) {
		if (this == this.cfg.getStartBb()) {
			this.cfg.setStartBb(target);
		}
		for (final E in : this.ins) {
			in.setEnd(target);
			target.ins.add(in);
		}
		this.ins.clear();
	}

	/**
	 * Move out edges (no catches) to target BB.
	 * 
	 * @param target
	 *            target BB
	 */
	public void moveOuts(final BB target) {
		for (final E out : this.outs) {
			if (out.isCatch()) {
				continue;
			}
			out.setStart(target);
			target.outs.add(out);
		}
		this.outs.clear();
	}

	/**
	 * Peek stack expression.
	 * 
	 * @return expression
	 */
	public Expression peek() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.vs[getRegs() + this.top - 1];
	}

	/**
	 * Pop stack expression.
	 * 
	 * @return expression
	 */
	public Expression pop() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		return this.vs[getRegs() + --this.top];
	}

	/**
	 * Push stack expression.
	 * 
	 * @param v
	 *            expression
	 */
	public void push(final Expression v) {
		if (getRegs() + this.top >= this.vs.length) {
			final Expression[] newVs = new Expression[getRegs() + this.top + 1];
			System.arraycopy(this.vs, 0, newVs, 0, getRegs() + this.top);
			this.vs = newVs;
		}
		this.vs[getRegs() + this.top++] = v;
	}

	/**
	 * Remove BB from CFG.
	 */
	public void remove() {
		for (final E in : this.ins) {
			in.getStart().outs.remove(in);
		}
		for (final E out : this.outs) {
			out.getEnd().ins.remove(out);
		}
	}

	/**
	 * Remove final statement.
	 * 
	 * @return statement or null
	 */
	public Statement removeFinalStmt() {
		return this.stmts.isEmpty() ? null : this.stmts.remove(this.stmts.size() - 1);
	}

	/**
	 * Remove operation at index.
	 * 
	 * @param i
	 *            operation index
	 * @return operation or null
	 */
	public Op removeOp(final int i) {
		final int size = this.ops.size();
		return size <= i ? null : this.ops.remove(i);
	}

	/**
	 * Remove statement at index.
	 * 
	 * @param i
	 *            statement index
	 * @return statement or null
	 */
	public Statement removeStmt(final int i) {
		final int size = this.stmts.size();
		return size <= i ? null : this.stmts.remove(i);
	}

	/**
	 * Set local expression.
	 * 
	 * @param i
	 *            index
	 * @param v
	 *            expression
	 */
	public void set(final int i, final Expression v) {
		this.vs[i] = v;
	}

	/**
	 * Set conditional successors.
	 * 
	 * @param falseSucc
	 *            false (branch) successor
	 * @param trueSucc
	 *            true (branch) successor
	 */
	public void setCondSuccs(final BB falseSucc, final BB trueSucc) {
		assert getFalseOut() == null : getFalseOut();
		assert getTrueOut() == null : getTrueOut();

		final E falseOut = new E(this, falseSucc, Boolean.FALSE);
		this.outs.add(falseOut);
		falseSucc.ins.add(falseOut);

		final E trueOut = new E(this, trueSucc, Boolean.TRUE);
		this.outs.add(trueOut);
		trueSucc.ins.add(trueOut);
	}

	/**
	 * Set successor.
	 * 
	 * @param succ
	 *            successor
	 */
	public void setSucc(final BB succ) {
		// TODO RET assert getOut() == null : getOut();

		final E e = new E(this, succ, null);
		this.outs.add(e);
		succ.ins.add(e);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("BB ");
		sb.append(getPostorder()).append(" (");
		if (getLine() >= 0) {
			sb.append("l ").append(getLine()).append(", ");
		}
		sb.append("pc ").append(getPc()).append(")");
		if (this.ops.size() > 0) {
			sb.append("\nOps: ").append(this.ops);
		}
		if (this.stmts.size() > 0) {
			sb.append("\nStmts: ").append(this.stmts);
		}
		if (this.top > 0) {
			sb.append("\nStack: ").append(
					Arrays.toString(Arrays.copyOfRange(this.vs, getRegs(), getRegs() + this.top)));
		}
		if (this.outs.size() > 1) {
			sb.append("\nSucc: ");
			for (final E out : this.outs) {
				sb.append(out.getEnd().getPostorder()).append(' ');
			}
		}
		return sb.toString();
	}

}