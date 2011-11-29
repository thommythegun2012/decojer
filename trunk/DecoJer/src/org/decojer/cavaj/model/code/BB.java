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
import java.util.List;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.struct.Struct;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

/**
 * CFG Basic Block (BB).
 * 
 * @author André Pankraz
 */
public final class BB {

	private final CFG cfg;

	private final List<Op> ops = new ArrayList<Op>();

	/**
	 * Must cache first PC separately because operations are removed through transformations.
	 */
	private final int opPc;

	private int postorder;

	protected final ArrayList<E> ins = new ArrayList<E>(2);

	private final List<Statement> stmts = new ArrayList<Statement>();

	private Struct struct;

	protected final ArrayList<E> outs = new ArrayList<E>(2);

	private int top; // stack top

	private Expression[] vs;

	protected BB(final CFG cfg, final int opPc) {
		this.cfg = cfg;
		this.opPc = opPc;
		this.vs = new Expression[cfg.getMaxLocals()];
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
			if (getLocals() + this.top + bb.top > this.vs.length) {
				final Expression[] newVs = new Expression[getLocals() + this.top + bb.top];
				System.arraycopy(this.vs, 0, newVs, 0, getLocals() + this.top);
				this.vs = newVs;
			}
			System.arraycopy(bb.vs, bb.getLocals(), this.vs, getLocals() + this.top, bb.top);
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
	 * Get CFG.
	 * 
	 * @return CFG
	 */
	public CFG getCfg() {
		return this.cfg;
	}

	/**
	 * Get false out edge.
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
	 * Get false successor (for conditionals only, else exception).
	 * 
	 * @return false successor
	 */
	public BB getFalseSucc() {
		return getFalseOut().getEnd();
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
	 * Get in edges.
	 * 
	 * @return in edges
	 */
	public List<E> getIns() {
		return this.ins;
	}

	private int getLocals() {
		return this.cfg.getMaxLocals();
	}

	/**
	 * Get first operation line.
	 * 
	 * @return first operation line
	 */
	public int getOpLine() {
		return this.cfg.getOps()[this.opPc].getLine();
	}

	/**
	 * Get first operation pc.
	 * 
	 * @return first operation pc
	 */
	public int getOpPc() {
		return this.opPc;
	}

	/**
	 * Get operations.
	 * 
	 * @return operations
	 */
	public List<Op> getOps() {
		return this.ops;
	}

	/**
	 * Get order index, may be line number if given or operation program counter.
	 * 
	 * @return order index, may be line number if given or operation program counter
	 */
	public int getOrder() {
		final int opLine = getOpLine();
		return opLine == -1 ? getOpPc() : opLine;
	}

	/**
	 * Get sequence out edge. Returns null if last statement is a control flow statement.
	 * 
	 * @return out edge (or null)
	 */
	public E getOut() {
		for (final E succ : this.outs) {
			if (null == succ.getValue()) {
				return succ;
			}
		}
		return null;
	}

	/**
	 * Get out edges.
	 * 
	 * @return out edges
	 */
	public List<E> getOuts() {
		return this.outs;
	}

	/**
	 * Get postorder.
	 * 
	 * @return postorder
	 */
	public int getPostorder() {
		return this.postorder;
	}

	/**
	 * Get expression stack size.
	 * 
	 * @return expression stack size
	 */
	public int getStackSize() {
		return this.top;
	}

	/**
	 * Get statement.
	 * 
	 * @param index
	 *            statement index
	 * @return statement or null
	 */
	public Statement getStmt(final int index) {
		final int size = this.stmts.size();
		return size <= index ? null : this.stmts.get(index);
	}

	/**
	 * Get number of statements.
	 * 
	 * @return number of statements
	 */
	public int getStmts() {
		return this.stmts.size();
	}

	/**
	 * Get control flow structure.
	 * 
	 * @return control flow structure
	 */
	public Struct getStruct() {
		return this.struct;
	}

	/**
	 * Get sequence successor.
	 * 
	 * @return sequence successor or null
	 */
	public BB getSucc() {
		return getOut() == null ? null : getOut().getEnd();
	}

	/**
	 * Get true out edge.
	 * 
	 * @return true out edge
	 */
	public E getTrueOut() {
		for (final E succ : this.outs) {
			if (Boolean.TRUE == succ.getValue()) {
				return succ;
			}
		}
		return null;
	}

	/**
	 * Get true successor (for conditionals only, else exception).
	 * 
	 * @return true successor
	 */
	public BB getTrueSucc() {
		return getTrueOut().getEnd();
	}

	/**
	 * Is final statement conditional (IfStatement)?
	 * 
	 * @return true - final statement is conditional
	 */
	public boolean isFinalStmtCond() {
		return this.stmts.isEmpty() ? false
				: this.stmts.get(this.stmts.size() - 1) instanceof IfStatement;
	}

	/**
	 * Is final statement switch (SwitchStatement)?
	 * 
	 * @return true - final statement is switch
	 */
	public boolean isFinalStmtSwitch() {
		return this.stmts.isEmpty() ? false
				: this.stmts.get(this.stmts.size() - 1) instanceof SwitchStatement;
	}

	/**
	 * Move in edges to target BB.
	 * 
	 * @param target
	 *            target BB
	 */
	public void moveIns(final BB target) {
		for (final E in : this.ins) {
			in.setEnd(target);
			target.ins.add(in);
		}
		this.ins.clear();
	}

	/**
	 * Move out edges to target BB.
	 * 
	 * @param target
	 *            target BB
	 */
	public void moveOuts(final BB target) {
		for (final E out : this.outs) {
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
		return this.vs[getLocals() + this.top - 1];
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
		return this.vs[getLocals() + --this.top];
	}

	/**
	 * Push stack expression.
	 * 
	 * @param v
	 *            expression
	 */
	public void push(final Expression v) {
		if (getLocals() + this.top >= this.vs.length) {
			final Expression[] newVs = new Expression[getLocals() + this.top + 1];
			System.arraycopy(this.vs, 0, newVs, 0, getLocals() + this.top);
			this.vs = newVs;
		}
		this.vs[getLocals() + this.top++] = v;
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
	 * @param index
	 *            index
	 * @return statement or null
	 */
	public Statement removeFinalStmt() {
		return this.stmts.isEmpty() ? null : this.stmts.remove(this.stmts.size() - 1);
	}

	/**
	 * Remove statement.
	 * 
	 * @param index
	 *            index
	 * @return statement or null
	 */
	public Statement removeStmt(final int index) {
		final int size = this.stmts.size();
		return size <= index ? null : this.stmts.remove(index);
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
	 *            false successor
	 * @param trueSucc
	 *            true successor
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
	 * Set postorder.
	 * 
	 * @param postorder
	 *            postorder
	 */
	public void setPostorder(final int postorder) {
		this.postorder = postorder;
	}

	/**
	 * Set control flow structure.
	 * 
	 * @param struct
	 *            control flow structure
	 */
	public void setStruct(final Struct struct) {
		this.struct = struct;
	}

	/**
	 * Set successor.
	 * 
	 * @param succ
	 *            successor
	 */
	public void setSucc(final BB succ) {
		assert getOut() == null : getOut();

		final E e = new E(this, succ, null);
		this.outs.add(e);
		succ.ins.add(e);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("BB ");
		sb.append(getPostorder()).append(" (");
		if (getOpLine() >= 0) {
			sb.append("l ").append(getOpLine()).append(", ");
		}
		sb.append("pc ").append(getOpPc()).append(")");
		if (this.ops.size() > 0) {
			sb.append("\nOps: ").append(this.ops);
		}
		if (this.top > 0) {
			sb.append("\nExprs: ").append(this.top);
		}
		if (this.stmts.size() > 0) {
			sb.append("\nStmts: ").append(this.stmts);
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