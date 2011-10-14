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
package org.decojer.cavaj.util;

import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;

/**
 * Operator precedence helper class. Check: http://www.uni-bonn.de/~manfear/javaoperators.php
 * 
 * @author André Pankraz
 */
public class OperatorPrecedence {

	private final static Logger LOGGER = Logger.getLogger(OperatorPrecedence.class.getName());

	private static final boolean[] IS_LEFT_ASSOC = { true, true, false, true, true, true, true,
			true, true, true, true, true, true, false, false };

	/**
	 * Is priority class left associative?
	 * 
	 * @param priority
	 *            priority class
	 * @return true - is left associative
	 */
	public static boolean isLeftAssoc(final int priority) {
		return IS_LEFT_ASSOC[priority];
	}

	/**
	 * Get priority for expression.
	 * 
	 * @param expression
	 *            expression
	 * @return priority
	 */
	public static int priority(final Expression expression) {
		if (expression instanceof ArrayAccess) {
			return 1;
		}
		if (expression instanceof MethodInvocation) {
			return 1;
		}
		if (expression instanceof FieldAccess) {
			return 1;
		}
		if (expression instanceof PrefixExpression) {
			return 2;
		}
		if (expression instanceof CastExpression) {
			return 2;
		}
		if (expression instanceof ClassInstanceCreation) {
			return 1; // should be 2, but what of "new Bla().doSomething();"
		}
		if (expression instanceof InfixExpression) {
			final InfixExpression.Operator operator = ((InfixExpression) expression).getOperator();
			if (operator == InfixExpression.Operator.TIMES
					|| operator == InfixExpression.Operator.DIVIDE
					|| operator == InfixExpression.Operator.REMAINDER) {
				return 3;
			}
			if (operator == Operator.PLUS || operator == Operator.MINUS) {
				return 4;
			}
			if (operator == InfixExpression.Operator.LEFT_SHIFT
					|| operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED
					|| operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
				return 5;
			}
			if (operator == InfixExpression.Operator.LESS
					|| operator == InfixExpression.Operator.LESS_EQUALS
					|| operator == InfixExpression.Operator.GREATER
					|| operator == InfixExpression.Operator.GREATER_EQUALS) {
				return 6;
			}
			if (operator == InfixExpression.Operator.EQUALS
					|| operator == InfixExpression.Operator.NOT_EQUALS) {
				return 7;
			}
			if (operator == InfixExpression.Operator.AND) {
				return 8;
			}
			if (operator == InfixExpression.Operator.XOR) {
				return 9;
			}
			if (operator == InfixExpression.Operator.OR) {
				return 10;
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
				return 11;
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
				return 12;
			}
			LOGGER.warning("Unknown infix expression operator '" + operator + "'!");
			return 0;
		}
		if (expression instanceof InstanceofExpression) {
			return 6;
		}
		if (expression instanceof Assignment) {
			return 14;
		}
		return 0;
	}

}