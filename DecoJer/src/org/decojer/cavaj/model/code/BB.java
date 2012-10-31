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
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Struct;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

import com.google.common.collect.Lists;

/**
 * Basic block for CFG.
 * 
 * @author André Pankraz
 */
public final class BB {

	@Getter
	private final CFG cfg;

	@Getter
	private final List<E> ins = Lists.newArrayListWithCapacity(1);

	private final List<Op> ops = Lists.newArrayList();

	/**
	 * Cond / loop-back / switch case and catch outs, pc-ordered after initial read.
	 * 
	 * Pc-ordering at initial read automatically through read-order and some sorts at branches.
	 */
	@Getter
	private final List<E> outs = Lists.newArrayListWithCapacity(1);

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

	private final List<Statement> stmts = new ArrayList<Statement>(4);

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
	 * Add handler.
	 * 
	 * @param handler
	 *            handler BB
	 * @param catchTs
	 *            catch types
	 * @return out edge
	 */
	public E addCatchHandler(final BB handler, final T[] catchTs) {
		return addSucc(handler, catchTs);
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
	 * Add successor.
	 * 
	 * @param succ
	 *            successor BB
	 * @param value
	 *            value
	 * @return out edge
	 */
	private final E addSucc(final BB succ, final Object value) {
		final E e = new E(this, succ, value);
		this.outs.add(e);
		succ.ins.add(e);
		return e;
	}

	/**
	 * Add switch case.
	 * 
	 * @param caseBb
	 *            case BB
	 * @param values
	 *            Integer values
	 * @return out edge
	 */
	public E addSwitchCase(final BB caseBb, final Integer[] values) {
		return addSucc(caseBb, values);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof BB)) {
			return false;
		}
		return this.pc == ((BB) obj).pc;
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
	 * Get unique (sequence) out edge.
	 * 
	 * @return out edge
	 */
	public E getOut() {
		for (final E out : this.outs) {
			if (out.isSequence()) {
				return out;
			}
		}
		return null;
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
	 * Get relevant incoming edge.
	 * 
	 * @return relevant incoming edge
	 * 
	 * @see BB#isRelevant()
	 */
	public E getRelevantIn() {
		if (this.ins.size() != 1) {
			return null;
		}
		return this.ins.get(0).relevantIn();
	}

	/**
	 * Get relevant outgoing edge.
	 * 
	 * @return relevant outgoing edge
	 * 
	 * @see BB#isRelevant()
	 */
	public E getRelevantOut() {
		final E out = getOut(); // without catches etc.
		if (out == null) {
			return null;
		}
		return out.relevantOut();
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

	@Override
	public int hashCode() {
		return this.pc;
	}

	/**
	 * Has this BB given BB as predecessor? This excludes same BB!
	 * 
	 * @param bb
	 *            BB
	 * @return {@code true} - given BB is predecessor of this BB
	 */
	public boolean hasPred(final BB bb) {
		if (this.postorder >= bb.postorder) {
			return false;
		}
		for (final E in : this.ins) {
			if (in.isBack()) {
				continue;
			}
			if (in.hasPred(bb)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Has this BB given BB as successor? This excludes same BB!
	 * 
	 * @param bb
	 *            BB
	 * @return {@code true} - given BB is successor of this BB
	 */
	public boolean hasSucc(final BB bb) {
		return !hasPred(bb);
	}

	/**
	 * Is this BB before given BB?
	 * 
	 * @param bb
	 *            given BB
	 * @return {@code true} - this BB is before given BB
	 */
	public boolean isBefore(final BB bb) {
		if (getLine() < bb.getLine()) {
			return true;
		}
		if (getLine() == bb.getLine() && getPc() < bb.getPc()) {
			return true;
		}
		return false;
	}

	/**
	 * Is BB a catch handler?
	 * 
	 * @return {@code true} - BB is a catch handler
	 */
	public boolean isCatchHandler() {
		for (final E in : this.ins) {
			if (in.isCatch()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is BB a catch try?
	 * 
	 * @return {@code true} - BB is a catch try
	 */
	public boolean isCatchTry() {
		for (final E out : this.outs) {
			if (out.isCatch()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is conditional or pre loop head?
	 * 
	 * @return {@code true} - is conditional or pre loop head
	 */
	public boolean isCondOrPreLoopHead() {
		return this.stmts.isEmpty() ? false
				: this.stmts.get(this.stmts.size() - 1) instanceof IfStatement;
	}

	/**
	 * Is line information available?
	 * 
	 * @return {@code true} - line information is available
	 */
	public boolean isLineInfo() {
		return getLine() >= 0;
	}

	public boolean isLoopHead() {
		// at least one incoming edge must be a back edge (self loop possible), in Java only
		// possible for loop heads
		for (final E in : this.ins) {
			if (in.isBack()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is BB relevant?
	 * 
	 * Multiple incomings, none-empty BBs which are not single GOTO operations (after CFG building)
	 * are relevant.
	 * 
	 * We could exclude this BBs in CFG building, but may be they are an interesting info for
	 * decompiling structures.
	 * 
	 * @return {@code true} - BB is empty
	 */
	public boolean isRelevant() {
		return this.ins.size() > 1 || !this.stmts.isEmpty() || this.top > 0 || !this.ops.isEmpty()
				&& !(this.ops.get(0) instanceof GOTO);
	}

	/**
	 * Is BB a sequence?
	 * 
	 * @return {@code true} - BB is a sequence
	 */
	public boolean isSequence() {
		for (final E out : this.outs) {
			if (out.isSequence()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is stack empty?
	 * 
	 * @return {@code true} - stack is empty
	 */
	public boolean isStackEmpty() {
		return this.top <= 0;
	}

	/**
	 * Has BB necessary stack size for first operation?
	 * 
	 * @return {@code true} - BB has necessary stack size for first operation
	 */
	public boolean isStackUnderflow() {
		final Op op = getOp(0);
		return op.getInStackSize() - this.cfg.getInFrame(op).wideStacks(op.getInStackSize()) > getTop();
	}

	/**
	 * Is start BB?
	 * 
	 * @return {@code true} - is start BB
	 */
	public boolean isStartBb() {
		return this.cfg.getStartBb() == this;
	}

	/**
	 * Is switch head?
	 * 
	 * @return {@code true} - is switch head
	 */
	public boolean isSwitchHead() {
		// don't use successor number as indicator, switch with 2 successors
		// (JVM 6: 1 case and default) possible, not optimized
		return this.stmts.isEmpty() ? false
				: this.stmts.get(this.stmts.size() - 1) instanceof SwitchStatement;
	}

	/**
	 * Copy content from BB.
	 * 
	 * @param bb
	 *            BB
	 */
	public void joinPredBb(final BB bb) {
		this.ops.addAll(0, bb.ops);
		this.stmts.addAll(0, bb.stmts);
		if (bb.top > 0) {
			if (getRegs() + bb.top + this.top > this.vs.length) {
				final Expression[] newVs = new Expression[getRegs() + bb.top + this.top];
				System.arraycopy(this.vs, 0, newVs, 0, getRegs());
				System.arraycopy(this.vs, getRegs(), newVs, getRegs() + bb.top, this.top);
				this.vs = newVs;
			} else {
				// shift right
				System.arraycopy(this.vs, getRegs(), this.vs, getRegs() + bb.top, this.top);
			}
			System.arraycopy(bb.vs, getRegs(), this.vs, getRegs(), bb.top);
			this.top += bb.top;
		}
		bb.moveIns(this);
		bb.remove();
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
		this.cfg.getPostorderedBbs().set(this.postorder, null);
		for (int i = this.ins.size(); i-- > 0;) {
			this.ins.get(i).remove();
		}
		for (int i = this.outs.size(); i-- > 0;) {
			this.outs.get(i).remove();
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
	 * Add conditionals.
	 * 
	 * @param trueBb
	 *            true BB
	 * @param falseBb
	 *            false BB
	 */
	public void setConds(final BB trueBb, final BB falseBb) {
		// preserve pc-order as edge-order
		if (falseBb.getPc() < trueBb.getPc()) {
			// usual case, if not a direct branching
			addSucc(falseBb, Boolean.FALSE);
			addSucc(trueBb, Boolean.TRUE);
			return;
		}
		addSucc(trueBb, Boolean.TRUE);
		addSucc(falseBb, Boolean.FALSE);
	}

	/**
	 * Set successor.
	 * 
	 * @param succ
	 *            successor
	 * @return out edge
	 */
	public final E setSucc(final BB succ) {
		return addSucc(succ, null);
	}

	/**
	 * Sort outs.
	 */
	public void sortOuts() {
		Collections.sort(this.outs, E.LINE_COMPARATOR);
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