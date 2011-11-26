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
package org.decojer.cavaj.model;

import java.util.ArrayList;
import java.util.List;

import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.struct.Struct;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

/**
 * Basic Block.
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

	protected final List<BB> predBbs = new ArrayList<BB>(0);

	private final List<Statement> stmts = new ArrayList<Statement>();

	private Struct struct;

	protected final List<BB> succBbs = new ArrayList<BB>(0);

	protected final List<Object> succValues = new ArrayList<Object>(0);

	private int top; // stack top

	private Expression[] vs;

	protected BB(final CFG cfg, final int opPc) {
		this.cfg = cfg;
		this.opPc = opPc;
		this.vs = new Expression[cfg.getMaxLocals()];
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
	 * @param statement
	 *            statement
	 */
	public void addStmt(final Statement statement) {
		this.stmts.add(statement);
	}

	/**
	 * Add successor basic block with an edge value. The successors are ordered after the basic
	 * block order index for order indexes greater than this.
	 * 
	 * @param succBb
	 *            successor basic block
	 * @param succValue
	 *            edge value
	 */
	public void addSucc(final BB succBb, final Object succValue) {
		assert succBb != null;

		final int thisOrder = getOpPc();
		final int succOrder = succBb.getOpPc();

		// sort forward edges
		for (int i = 0; i < this.succBbs.size(); ++i) {
			final BB bb = this.succBbs.get(i);
			final int order = bb.getOpPc();
			if (succOrder < order || thisOrder > order) {
				// insert here and return
				this.succBbs.add(i, succBb);
				this.succValues.add(i, succValue);
				succBb.predBbs.add(this);
				return;
			}
		}
		// append and return
		this.succBbs.add(succBb);
		this.succValues.add(succValue);
		succBb.predBbs.add(this);
	}

	/**
	 * Copy content from basic block.
	 * 
	 * @param bb
	 *            basic block
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
	 * Get control flow graph.
	 * 
	 * @return control flow graph
	 */
	public CFG getCfg() {
		return this.cfg;
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
	 * Get poerstorder.
	 * 
	 * @return postorder
	 */
	public int getPostorder() {
		return this.postorder;
	}

	/**
	 * Get predecessor basic blocks.
	 * 
	 * @return predecessor basic blocks
	 */
	public List<BB> getPredBbs() {
		return this.predBbs;
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
	 *            index
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
	 * Get succcessor basic blocks.
	 * 
	 * @return succcessor basic blocks
	 */
	public List<BB> getSuccBbs() {
		return this.succBbs;
	}

	/**
	 * Get false successor (for conditional BBs only).
	 * 
	 * @return false successor, not null (assert)
	 */
	public BB getSuccFalse() {
		final int index = this.succValues.indexOf(Boolean.FALSE);
		assert index != -1;

		return this.succBbs.get(index);
	}

	/**
	 * Get true successor (for conditional BBs only).
	 * 
	 * @return true successor, not null (assert)
	 */
	public BB getSuccTrue() {
		final int index = this.succValues.indexOf(Boolean.TRUE);
		assert index != -1;

		return this.succBbs.get(index);
	}

	/**
	 * Get successor values.
	 * 
	 * @return successor values
	 */
	public List<Object> getSuccValues() {
		return this.succValues;
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
	 * Move predecessors to another basic block.
	 * 
	 * @param targetBB
	 *            basic block
	 */
	public void movePredBbs(final BB targetBB) {
		targetBB.predBbs.addAll(this.predBbs);
		for (final BB predBB : this.predBbs) {
			final int index = predBB.succBbs.indexOf(this);
			predBB.succBbs.set(index, targetBB);
		}
		this.predBbs.clear();
	}

	/**
	 * Move successors to another basic block.
	 * 
	 * @param targetBB
	 *            basic block
	 */
	public void moveSuccBbs(final BB targetBB) {
		targetBB.succBbs.addAll(this.succBbs);
		targetBB.succValues.addAll(this.succValues);
		for (final BB succBB : this.succBbs) {
			final int index = succBB.predBbs.indexOf(this);
			succBB.predBbs.set(index, targetBB);
		}
		this.succBbs.clear();
		this.succValues.clear();
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
	 * Remove basic block from control flow graph.
	 */
	public void remove() {
		for (final BB succBB : this.succBbs) {
			succBB.predBbs.remove(this);
		}
		for (final BB predBB : this.predBbs) {
			final int index = predBB.succBbs.indexOf(this);
			predBB.succBbs.remove(index);
			predBB.succValues.remove(index);
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
	 * Set value for given successor basic block.
	 * 
	 * @param succBB
	 *            successor basic block
	 * @param value
	 *            successor value
	 */
	public void setSuccValue(final BB succBB, final Object value) {
		final int index = this.succBbs.indexOf(succBB);
		this.succValues.set(index, value);
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
		if (this.succBbs.size() > 1) {
			sb.append("\nSucc: ");
			for (final BB bb : this.succBbs) {
				sb.append(bb.getPostorder()).append(' ');
			}
		}
		return sb.toString();
	}

}