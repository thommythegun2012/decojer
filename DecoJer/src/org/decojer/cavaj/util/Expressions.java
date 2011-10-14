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

import static org.decojer.cavaj.util.OperatorPrecedence.priority;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

/**
 * Helper functions for expressions handling.
 * 
 * @author André Pankraz
 */
public class Expressions {

	/**
	 * New infix expression. Attention: First comes right expression, good for stack simulation.
	 * 
	 * @param operator
	 *            infix expression operator
	 * @param rightOperand
	 *            right operand expression
	 * @param leftOperand
	 *            left operand expression
	 * @return expression
	 */
	public static Expression newInfixExpression(final InfixExpression.Operator operator,
			final Expression rightOperand, final Expression leftOperand) {
		final InfixExpression infixExpression = leftOperand.getAST().newInfixExpression();
		infixExpression.setOperator(operator);
		final int operatorPriority = priority(infixExpression);
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
			// !!a => a
			if (operand instanceof PrefixExpression) {
				final PrefixExpression prefixExpression = (PrefixExpression) operand;
				if (prefixExpression.getOperator() == PrefixExpression.Operator.NOT) {
					return (Expression) ASTNode.copySubtree(operand.getAST(),
							prefixExpression.getOperand());
				}
			}
			if (operand instanceof InfixExpression) {
				final InfixExpression infixExpression = (InfixExpression) operand;
				if (infixExpression.getOperator() == InfixExpression.Operator.EQUALS) {
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
			}
		}
		final PrefixExpression prefixExpression = operand.getAST().newPrefixExpression();
		prefixExpression.setOperator(operator);
		prefixExpression.setOperand(wrap(operand, priority(prefixExpression)));
		return prefixExpression;
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
	public static Expression wrap(final Expression expression, final int priority) {
		final Expression expressionP = expression.getParent() == null ? expression
				: (Expression) ASTNode.copySubtree(expression.getAST(), expression);
		if (priority(expression) <= priority) {
			return expressionP;
		}
		final ParenthesizedExpression parenthesizedExpression = expression.getAST()
				.newParenthesizedExpression();
		parenthesizedExpression.setExpression(expressionP);
		return parenthesizedExpression;
	}

}