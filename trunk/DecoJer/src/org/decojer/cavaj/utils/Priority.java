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

import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

/**
 * Expression priority.
 * 
 * @author André Pankraz
 */
public enum Priority {

	LITERAL(0, true),

	ARRAY_INDEX(1, true),

	METHOD_CALL(1, true),

	MEMBER_ACCESS(1, true),

	PREFIX_OR_POSTFIX(2, false),

	TYPE_CAST(2, true),

	MULT_DIV_MOD(3, true),

	ADD_SUB(4, true),

	SHIFT(5, true),

	LESS_GREATER(6, true),

	INSTANCEOF(6, true),

	EQUALS_NOT(7, true),

	AND(8, true),

	XOR(9, true),

	OR(10, true),

	CONDITIONAL_AND(11, true),

	CONDITIONAL_OR(12, true),

	CONDITIONAL(13, false),

	ASSIGNMENT(14, false); // TODO aassignment + op == 15?
							// http://openbook.galileodesign.de/javainsel8/javainsel_02_005.htm#mjb7472917f33c38fb79c0baa79ca8a846

	private final static Logger LOGGER = Logger.getLogger(Priority.class.getName());

	/**
	 * Get priority for expression. From http://bmanolov.free.fr/javaoperators.php
	 * 
	 * @param expression
	 *            expression
	 * @return priority
	 */
	public static Priority priority(final Expression expression) {
		if (expression instanceof ArrayAccess) {
			return ARRAY_INDEX;
		}
		if (expression instanceof MethodInvocation) {
			return METHOD_CALL;
		}
		if (expression instanceof FieldAccess) {
			return MEMBER_ACCESS;
		}
		if (expression instanceof PrefixExpression || expression instanceof PostfixExpression) {
			return PREFIX_OR_POSTFIX;
		}
		if (expression instanceof CastExpression) {
			return TYPE_CAST;
		}
		if (expression instanceof ClassInstanceCreation) {
			return METHOD_CALL; // should be 2, but what of "new Bla().doSomething();"
		}
		if (expression instanceof InfixExpression) {
			final InfixExpression.Operator operator = ((InfixExpression) expression).getOperator();
			if (operator == InfixExpression.Operator.TIMES
					|| operator == InfixExpression.Operator.DIVIDE
					|| operator == InfixExpression.Operator.REMAINDER) {
				return MULT_DIV_MOD;
			}
			if (operator == Operator.PLUS || operator == Operator.MINUS) {
				return ADD_SUB;
			}
			if (operator == InfixExpression.Operator.LEFT_SHIFT
					|| operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED
					|| operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
				return SHIFT;
			}
			if (operator == InfixExpression.Operator.LESS
					|| operator == InfixExpression.Operator.LESS_EQUALS
					|| operator == InfixExpression.Operator.GREATER
					|| operator == InfixExpression.Operator.GREATER_EQUALS) {
				return LESS_GREATER;
			}
			if (operator == InfixExpression.Operator.EQUALS
					|| operator == InfixExpression.Operator.NOT_EQUALS) {
				return EQUALS_NOT;
			}
			if (operator == InfixExpression.Operator.AND) {
				return AND;
			}
			if (operator == InfixExpression.Operator.XOR) {
				return XOR;
			}
			if (operator == InfixExpression.Operator.OR) {
				return OR;
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
				return CONDITIONAL_AND;
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
				return CONDITIONAL_OR;
			}
			LOGGER.warning("Unknown infix expression operator '" + operator + "'!");
			return LITERAL;
		}
		if (expression instanceof InstanceofExpression) {
			return INSTANCEOF;
		}
		if (expression instanceof ConditionalExpression) {
			return CONDITIONAL;
		}
		if (expression instanceof Assignment) {
			return ASSIGNMENT;
		}
		return LITERAL;
	}

	private final boolean isLeftAssoc;

	private final int priority;

	/**
	 * Constructor.
	 * 
	 * @param priority
	 *            priority
	 * @param isLeftAssoc
	 *            {@true} - is left associative
	 */
	private Priority(final int priority, final boolean isLeftAssoc) {
		this.priority = priority;
		this.isLeftAssoc = isLeftAssoc;
	}

	/**
	 * Get priority.
	 * 
	 * @return priority
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * Is priority class left associative?
	 * 
	 * @return {@code true} - is left associative
	 */
	public boolean isLeftAssoc() {
		return this.isLeftAssoc;
	}

}