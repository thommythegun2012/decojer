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
import java.util.Stack;

import org.decojer.cavaj.model.struct.Struct;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Basic Block.
 * 
 * @author André Pankraz
 */
public class BB {

	private final CFG cfg;

	private final Stack<Expression> expressions = new Stack<Expression>();

	private final List<Operation> operations = new ArrayList<Operation>();

	private int opLine;

	private int opPc;

	private int postorder;

	protected final List<BB> predBbs = new ArrayList<BB>(0);

	private final List<Statement> statements = new ArrayList<Statement>();

	private Struct struct;

	protected final List<BB> succBbs = new ArrayList<BB>(0);

	protected final List<Object> succValues = new ArrayList<Object>(0);

	protected BB(final CFG cfg, final int opPc) {
		this.cfg = cfg;
		this.opPc = opPc;
	}

	public void addOperation(final Operation operation) {
		if (this.operations.size() == 0) {
			this.opLine = operation.getLineNumber();
		}
		this.operations.add(operation);
	}

	public void addStatement(final Statement statement) {
		this.statements.add(statement);
	}

	/**
	 * Add a basic block as successor with an edge value. This function is for
	 * the initial creation of the control flow graph. The successors are
	 * ordered after the basic block order index for order indexes greater than
	 * this.
	 * 
	 * @param succBb
	 *            successor basic block
	 * @param succValue
	 *            edge value
	 */
	public void addSucc(final BB succBb, final Object succValue) {
		assert succBb != null;

		final int thisOrder = getOrder();
		final int succOrder = succBb.getOrder();

		// sort forward edges
		for (int i = 0; i < this.succBbs.size(); ++i) {
			final BB bb = this.succBbs.get(i);
			final int order = bb.getOrder();
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

	public boolean containsExpression(final Expression expression) {
		for (final Expression e : this.expressions) {
			if (expression == e) {
				return true;
			}
		}
		return false;
	}

	public void copyContent(final BB bb) {
		this.operations.addAll(bb.operations);
		this.statements.addAll(bb.statements);
		this.expressions.addAll(bb.expressions);
	}

	public CFG getCfg() {
		return this.cfg;
	}

	public int getExpressionsSize() {
		return this.expressions.size();
	}

	/**
	 * Get final statement, often used.
	 * 
	 * @return last statement or null
	 */
	public Statement getFinalStatement() {
		final int size = this.statements.size();
		return size == 0 ? null : this.statements.get(size - 1);
	}

	/**
	 * Get immediate dominator (IDom).
	 * 
	 * @return immediate domminator (IDom)
	 */
	public BB getIDom() {
		return getCfg().getIDom(this);
	}

	public Operation getOperation(final int i) {
		return this.operations.get(i);
	}

	public int getOperationsSize() {
		return this.operations.size();
	}

	public int getOpLine() {
		return this.opLine;
	}

	public int getOpPc() {
		return this.opPc;
	}

	/**
	 * Get order index, may be line number if given or operation program
	 * counter.
	 * 
	 * @return order index, may be line number if given or operation program
	 *         counter
	 */
	public int getOrder() {
		final int opLine = getOpLine();
		return opLine == -1 ? getOpPc() : opLine;
	}

	public int getPostorder() {
		return this.postorder;
	}

	public List<BB> getPredBbs() {
		return this.predBbs;
	}

	public List<Statement> getStatements() {
		return this.statements;
	}

	public Struct getStruct() {
		return this.struct;
	}

	public BB getSuccBb(final Object value) {
		final int index = this.succValues.indexOf(value);
		return index == -1 ? null : this.succBbs.get(index);
	}

	public List<BB> getSuccBbs() {
		return this.succBbs;
	}

	public List<Object> getSuccValues() {
		return this.succValues;
	}

	public boolean isExpression() {
		return this.operations.size() == 0 && this.statements.size() == 0
				&& this.expressions.size() == 1;
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

	public Expression peekExpression() {
		return this.expressions.peek();
	}

	public Expression popExpression() {
		return this.expressions.pop();
	}

	public Expression pushExpression(final Expression expression) {
		return this.expressions.push(expression);
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

	public Operation removeOperation(final int i) {
		return this.operations.remove(i);
	}

	public void setOpPc(final int opPc) {
		this.opPc = opPc;
	}

	public void setPostorder(final int postorder) {
		this.postorder = postorder;
	}

	public void setStruct(final Struct struct) {
		this.struct = struct;
	}

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
		if (this.operations.size() > 0) {
			sb.append("\nOps: ").append(this.operations);
		}
		if (this.expressions.size() > 0) {
			sb.append("\nExprs: ").append(this.expressions);
		}
		if (this.statements.size() > 0) {
			sb.append("\nStmts: ").append(this.statements);
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