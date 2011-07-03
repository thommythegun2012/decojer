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
package org.decojer.cavaj.model.ast;

import org.decojer.cavaj.model.CU;
import org.eclipse.jdt.core.dom.Expression;

public class Ast {

	private final CU cu;

	public Ast(final CU cu) {
		this.cu = cu;
	}

	public CondStatement newCondStatement(final Expression expression) {
		return new CondStatement(this, expression);
	}

	public ExpressionStatement newExpressionStatement(
			final Expression expression) {
		return new ExpressionStatement(this, expression);
	}

	public GotoStatement newGotoStatement() {
		return new GotoStatement(this);
	}

	public ReturnStatement newReturnStatement(final Expression expression) {
		return new ReturnStatement(this, expression);
	}

	public SuperConstructorInvocationStatement newSuperConstructorInvocationStatement(
			final Expression expression) {
		return new SuperConstructorInvocationStatement(this, expression);
	}

	public SwitchStatement newSwitchStatement(final Expression expression) {
		return new SwitchStatement(this, expression);
	}

	public ThrowStatement newThrowStatement(final Expression expression) {
		return new ThrowStatement(this, expression);
	}

}