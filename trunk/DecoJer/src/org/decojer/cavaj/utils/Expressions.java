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
package org.decojer.cavaj.utils;

import static org.decojer.cavaj.utils.OperatorPrecedence.priority;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

/**
 * Helper functions for expressions handling.
 * 
 * @author André Pankraz
 */
public final class Expressions {

	private static boolean isNot(final Expression expression) {
		if (!(expression instanceof PrefixExpression)) {
			return false;
		}
		return ((PrefixExpression) expression).getOperator() == PrefixExpression.Operator.NOT;
	}

	/**
	 * New infix expression.
	 * 
	 * @param operator
	 *            infix expression operator
	 * @param leftOperand
	 *            left operand expression
	 * @param rightOperand
	 *            right operand expression
	 * @return expression
	 */
	public static Expression newInfixExpression(final InfixExpression.Operator operator,
			final Expression leftOperand, final Expression rightOperand) {
		final InfixExpression infixExpression = leftOperand.getAST().newInfixExpression();
		infixExpression.setOperator(operator);
		final int operatorPriority = priority(infixExpression).getPriority();
		infixExpression.setLeftOperand(wrap(leftOperand, operatorPriority));
		// more operators possible, but PLUS... really necessary here?!
		final boolean assoc = operator == InfixExpression.Operator.PLUS
				|| operator == InfixExpression.Operator.CONDITIONAL_AND
				|| operator == InfixExpression.Operator.CONDITIONAL_OR;
		infixExpression.setRightOperand(wrap(rightOperand, operatorPriority - (assoc ? 0 : 1)));
		return infixExpression;
	}

	/**
	 * New prefix expression.
	 * 
	 * @param operator
	 *            prefix expression operator
	 * @param operand
	 *            operand expression
	 * @return expression
	 */
	public static Expression newPrefixExpression(final PrefixExpression.Operator operator,
			final Expression operand) {
		if (operator == PrefixExpression.Operator.NOT) {
			return not(operand);
		}
		final PrefixExpression prefixExpression = operand.getAST().newPrefixExpression();
		prefixExpression.setOperator(operator);
		prefixExpression.setOperand(wrap(operand, priority(prefixExpression)));
		return prefixExpression;
	}

	/**
	 * Not expression.
	 * 
	 * @param operand
	 *            operand expression
	 * @return !expression
	 */
	public static Expression not(final Expression operand) {
		if (operand instanceof ParenthesizedExpression) {
			return not(((ParenthesizedExpression) operand).getExpression());
		}
		if (isNot(operand)) {
			// !!a => a
			return unwrap(((PrefixExpression) operand).getOperand());
		}
		if (operand instanceof InfixExpression) {
			final InfixExpression infixExpression = (InfixExpression) operand;
			if (infixExpression.getOperator() == InfixExpression.Operator.EQUALS) {
				// operator priority doesn't change here, reuse for all such cases...
				infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
				return infixExpression;
			}
			if (infixExpression.getOperator() == InfixExpression.Operator.GREATER) {
				infixExpression.setOperator(InfixExpression.Operator.LESS_EQUALS);
				return infixExpression;
			}
			if (infixExpression.getOperator() == InfixExpression.Operator.GREATER_EQUALS) {
				infixExpression.setOperator(InfixExpression.Operator.LESS);
				return infixExpression;
			}
			if (infixExpression.getOperator() == InfixExpression.Operator.LESS) {
				infixExpression.setOperator(InfixExpression.Operator.GREATER_EQUALS);
				return infixExpression;
			}
			if (infixExpression.getOperator() == InfixExpression.Operator.LESS_EQUALS) {
				infixExpression.setOperator(InfixExpression.Operator.GREATER);
				return infixExpression;
			}
			if (infixExpression.getOperator() == InfixExpression.Operator.NOT_EQUALS) {
				infixExpression.setOperator(InfixExpression.Operator.EQUALS);
				return infixExpression;
			}

			if (infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_AND
					|| infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
				// operator priorities change, don't reuse
				return newInfixExpression(
						infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_AND ? InfixExpression.Operator.CONDITIONAL_OR
								: InfixExpression.Operator.CONDITIONAL_AND,
						not(infixExpression.getLeftOperand()),
						not(infixExpression.getRightOperand()));
			}
		}
		if (operand instanceof ConditionalExpression) {
			// conditional has very low operator priority (before assignment), reuse possible
			final ConditionalExpression conditionalExpression = (ConditionalExpression) operand;
			final Expression thenExpression = not(conditionalExpression.getThenExpression());
			if (conditionalExpression.getThenExpression() != thenExpression) {
				conditionalExpression.setThenExpression(wrap(thenExpression, Priority.CONDITIONAL));
			}
			final Expression elseExpression = not(conditionalExpression.getElseExpression());
			if (conditionalExpression.getElseExpression() != elseExpression) {
				conditionalExpression.setElseExpression(wrap(elseExpression, Priority.CONDITIONAL));
			}
			return conditionalExpression;
		}
		final PrefixExpression prefixExpression = operand.getAST().newPrefixExpression();
		prefixExpression.setOperator(PrefixExpression.Operator.NOT);
		prefixExpression.setOperand(wrap(operand, Priority.PREFIX_OR_POSTFIX));
		return prefixExpression;
	}

	/**
	 * Unwrap expression. Ensures that there is no parent set.
	 * 
	 * @param expression
	 *            expression
	 * @return unwrapped expression
	 */
	public static Expression unwrap(final Expression expression) {
		Expression rawExpression;
		if (expression instanceof ParenthesizedExpression) {
			rawExpression = ((ParenthesizedExpression) expression).getExpression();
		} else {
			if (expression.getParent() == null) {
				return expression;
			}
			rawExpression = expression;
		}
		return (Expression) ASTNode.copySubtree(expression.getAST(), rawExpression);
	}

	/**
	 * Wrap expression. Ensures that there is no parent set.
	 * 
	 * @param expression
	 *            expression
	 * @return wrapped expression
	 */
	public static Expression wrap(final Expression expression) {
		return wrap(expression, Integer.MAX_VALUE);
	}

	private static Expression wrap(final Expression expression, final int priority) {
		final Expression rawExpression = expression.getParent() == null ? expression
				: (Expression) ASTNode.copySubtree(expression.getAST(), expression);
		if (priority(rawExpression).getPriority() <= priority) {
			return rawExpression;
		}
		final ParenthesizedExpression parenthesizedExpression = expression.getAST()
				.newParenthesizedExpression();
		parenthesizedExpression.setExpression(rawExpression);
		return parenthesizedExpression;
	}

	/**
	 * Wrap expression. Ensures that there is no parent set and adds parantheses if necessary
	 * (compares operator priority).
	 * 
	 * @param expression
	 *            expression
	 * @param priority
	 *            priority
	 * @return expression
	 */
	public static Expression wrap(final Expression expression, final Priority priority) {
		return wrap(expression, priority.getPriority());
	}

	private Expressions() {
		// static helper class
	}

}