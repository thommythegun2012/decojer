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

import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.types.ArrayT;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.model.types.ParamT.TypeArg;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.WildcardType;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Helper functions for expressions handling.
 * 
 * @author André Pankraz
 */
public final class Expressions {

	private final static Logger LOGGER = Logger.getLogger(Expressions.class.getName());

	private static final String JAVA_LANG = "java.lang";

	private static final String PROP_LITERAL_VALUE = "propLiteralValue";

	/**
	 * Get boolean value from literal.
	 * 
	 * @param expression
	 *            expression
	 * @return {@code null} - no boolean literal, {@code Boolean#TRUE} - true, {@code Boolean#FALSE}
	 *         - true
	 */
	public static Boolean booleanFromLiteral(final Expression expression) {
		if (expression instanceof BooleanLiteral) {
			return ((BooleanLiteral) expression).booleanValue();
		}
		if (!(expression instanceof NumberLiteral)) {
			return null;
		}
		final String token = ((NumberLiteral) expression).getToken();
		if (token.length() != 1) {
			return null;
		}
		final char c = token.charAt(0);
		if ('0' == c) {
			return false;
		}
		if ('1' == c) {
			return true;
		}
		return null;
	}

	/**
	 * Decompile literal.
	 * 
	 * @param t
	 *            literal type
	 * @param value
	 *            literal value
	 * @param td
	 *            type declaration (context)
	 * @return AST Expression
	 */
	public static Expression decompileLiteral(final T t, final Object value, final TD td) {
		final Expression e = decompileLiteral2(t, value, td);
		e.setProperty(PROP_LITERAL_VALUE, value);
		return e;
	}

	private static Expression decompileLiteral2(final T t, final Object value, final TD td) {
		final AST ast = td.getCu().getAst();
		if (t.isRef() /* incl. T.AREF */) {
			if (value == null) {
				return ast.newNullLiteral();
			}
			if (value instanceof T && t.isAssignableFrom(Class.class)) {
				final TypeLiteral typeLiteral = ast.newTypeLiteral();
				typeLiteral.setType(decompileType((T) value, td));
				return typeLiteral;
			}
			if (value instanceof String && t.isAssignableFrom(String.class)) {
				final StringLiteral stringLiteral = ast.newStringLiteral();
				stringLiteral.setLiteralValue((String) value);
				return stringLiteral;
			}
			LOGGER.warning("Unknown reference type '" + t + "'!");
			return ast.newNullLiteral();
		}
		// TODO really prefer INT in undecideable multi-case?
		if (t.is(T.INT)) {
			if (value instanceof Number) {
				final int i = ((Number) value).intValue();
				if (i == Integer.MAX_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Integer"),
							ast.newSimpleName("MAX_VALUE"));
				}
				if (i == Integer.MIN_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Integer"),
							ast.newSimpleName("MIN_VALUE"));
				}
				return ast.newNumberLiteral(Integer.toString(i));
			}
			if (value == null) {
				return ast.newNumberLiteral(Integer.toString(0));
			}
			LOGGER.warning("Integer type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString());
		}
		if (t.is(T.SHORT)) {
			if (value instanceof Number) {
				final short s = ((Number) value).shortValue();
				if (s == Short.MAX_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Short"),
							ast.newSimpleName("MAX_VALUE"));
				}
				if (s == Short.MIN_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Short"),
							ast.newSimpleName("MIN_VALUE"));
				}
				return ast.newNumberLiteral(Short.toString(s));
			}
			if (value == null) {
				return ast.newNumberLiteral(Short.toString((short) 0));
			}
			LOGGER.warning("Short type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString());
		}
		if (t.is(T.BYTE)) {
			if (value instanceof Number) {
				final byte b = ((Number) value).byteValue();
				if (b == Byte.MAX_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Byte"),
							ast.newSimpleName("MAX_VALUE"));
				}
				if (b == Byte.MIN_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Byte"),
							ast.newSimpleName("MIN_VALUE"));
				}
				return ast.newNumberLiteral(Byte.toString(b));
			}
			if (value == null) {
				return ast.newNumberLiteral(Byte.toString((byte) 0));
			}
			LOGGER.warning("Byte type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString());
		}
		if (t.is(T.BOOLEAN)) {
			if (value instanceof Boolean) {
				return ast.newBooleanLiteral(((Boolean) value).booleanValue());
			}
			if (value instanceof Number) {
				return ast.newBooleanLiteral(((Number) value).intValue() != 0);
			}
			if (value == null) {
				return ast.newBooleanLiteral(false);
			}
			LOGGER.warning("Boolean type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newBooleanLiteral(value instanceof String ? Boolean.valueOf((String) value)
					: true /* value is not null here */);
		}
		if (t.is(T.CHAR)) {
			if (value instanceof Character || value instanceof Number || value instanceof String
					&& ((String) value).length() == 1) {
				final char c = value instanceof Character ? (Character) value
						: value instanceof Number ? (char) ((Number) value).intValue()
								: ((String) value).charAt(0);
				switch (c) {
				case Character.MAX_VALUE:
					return ast.newQualifiedName(ast.newSimpleName("Character"),
							ast.newSimpleName("MAX_VALUE"));
				case Character.MIN_VALUE:
					return ast.newQualifiedName(ast.newSimpleName("Character"),
							ast.newSimpleName("MIN_VALUE"));
				case Character.MAX_HIGH_SURROGATE:
					if (td.getVersion() >= 49) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MAX_HIGH_SURROGATE"));
					}
					break;
				case Character.MAX_LOW_SURROGATE:
					if (td.getVersion() >= 49) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MAX_LOW_SURROGATE"));
					}
					break;
				case Character.MIN_HIGH_SURROGATE:
					if (td.getVersion() >= 49) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MIN_HIGH_SURROGATE"));
					}
					break;
				case Character.MIN_LOW_SURROGATE:
					if (td.getVersion() >= 49) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MIN_LOW_SURROGATE"));
					}
					break;
				}
				final CharacterLiteral characterLiteral = ast.newCharacterLiteral();
				characterLiteral.setCharValue(c);
				return characterLiteral;
			}
			if (value == null) {
				final CharacterLiteral characterLiteral = ast.newCharacterLiteral();
				characterLiteral.setCharValue((char) 0);
				return characterLiteral;
			}
			LOGGER.warning("Character type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			// char is per default 'X'
			return ast.newCharacterLiteral();
		}
		if (t.is(T.FLOAT)) {
			if (value instanceof Float || value instanceof Integer) {
				final float f = value instanceof Float ? (Float) value : Float
						.intBitsToFloat((Integer) value);
				if (Float.isNaN(f)) {
					return ast.newQualifiedName(ast.newSimpleName("Float"),
							ast.newSimpleName("NaN"));
				}
				if (f == Float.POSITIVE_INFINITY) {
					return ast.newQualifiedName(ast.newSimpleName("Float"),
							ast.newSimpleName("POSITIVE_INFINITY"));
				}
				if (f == Float.NEGATIVE_INFINITY) {
					return ast.newQualifiedName(ast.newSimpleName("Float"),
							ast.newSimpleName("NEGATIVE_INFINITY"));
				}
				if (f == Float.MAX_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Float"),
							ast.newSimpleName("MAX_VALUE"));
				}
				if (f == Float.MIN_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Float"),
							ast.newSimpleName("MIN_VALUE"));
				}
				if (f == Float.MIN_NORMAL) {
					if (td.getVersion() >= 50) {
						return ast.newQualifiedName(ast.newSimpleName("Float"),
								ast.newSimpleName("MIN_NORMAL"));
					}
				}
				return ast.newNumberLiteral(Float.toString(f) + 'F');
			}
			if (value == null) {
				return ast.newNumberLiteral(Float.toString(0F) + 'F');
			}
			LOGGER.warning("Float type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString() + 'F');
		}
		if (t.is(T.LONG)) {
			if (value instanceof Long) {
				final long l = (Long) value;
				if (l == Long.MAX_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Long"),
							ast.newSimpleName("MAX_VALUE"));
				}
				if (l == Long.MIN_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Long"),
							ast.newSimpleName("MIN_VALUE"));
				}
				return ast.newNumberLiteral(Long.toString(l) + 'L');
			}
			if (value == null) {
				return ast.newNumberLiteral(Long.toString(0L) + 'L');
			}
			LOGGER.warning("Long type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString() + 'L');
		}
		if (t.is(T.DOUBLE)) {
			if (value instanceof Double || value instanceof Long) {
				final double d = value instanceof Double ? (Double) value : Double
						.longBitsToDouble((Long) value);
				if (Double.isNaN(d)) {
					return ast.newQualifiedName(ast.newSimpleName("Double"),
							ast.newSimpleName("NaN"));
				}
				if (d == Double.POSITIVE_INFINITY) {
					return ast.newQualifiedName(ast.newSimpleName("Double"),
							ast.newSimpleName("POSITIVE_INFINITY"));
				}
				if (d == Double.NEGATIVE_INFINITY) {
					return ast.newQualifiedName(ast.newSimpleName("Double"),
							ast.newSimpleName("NEGATIVE_INFINITY"));
				}
				if (d == Double.MAX_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Double"),
							ast.newSimpleName("MAX_VALUE"));
				}
				if (d == Double.MIN_VALUE) {
					return ast.newQualifiedName(ast.newSimpleName("Double"),
							ast.newSimpleName("MIN_VALUE"));
				}
				if (d == Double.MIN_NORMAL) {
					if (td.getVersion() >= 50) {
						return ast.newQualifiedName(ast.newSimpleName("Double"),
								ast.newSimpleName("MIN_NORMAL"));
					}
				}
				return ast.newNumberLiteral(Double.toString(d) + 'D');
			}
			if (value == null) {
				return ast.newNumberLiteral(Double.toString(0D) + 'D');
			}
			LOGGER.warning("Double type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString() + 'D');
		}
		LOGGER.warning("Unknown data type '" + t + "'!");
		final StringLiteral stringLiteral = ast.newStringLiteral();
		if (value != null) {
			stringLiteral.setLiteralValue(value.toString());
		}
		return stringLiteral;
	}

	/**
	 * Decompile type name.
	 * 
	 * @param t
	 *            type
	 * @param td
	 *            type declaration (context)
	 * @return AST type name
	 */
	public static Name decompileName(final T t, final TD td) {
		final AST ast = td.getCu().getAst();
		final String packageName = t.getPackageName();
		// check if not in same package...
		if (!Objects.equal(td.getPackageName(), t.getPackageName())) {
			// check if at least Java default package...
			if (JAVA_LANG.equals(packageName)) {
				// ignore Java default package
				return ast.newName(t.getPName());
			}
			// ...full name necessary
			return ast.newName(t.getName());
		}
		// ...is in same package, check if not enclosed...
		if (t.getEnclosingT() == null) {
			return ast.newName(t.getPName());
		}
		// ...is enclosed

		// convert inner classes separator '$' into '.',
		// cannot use string replace because '$' is also a regular Java type name!
		// find common name dominator and stop there, for relative inner names
		final String name = td.getName();
		final List<String> names = Lists.newArrayList(t.getSimpleIdentifier());
		T enclosingT = t.getEnclosingT();
		while (enclosingT != null && !name.startsWith(enclosingT.getName())) {
			names.add(enclosingT.getSimpleIdentifier());
			enclosingT = enclosingT.getEnclosingT();
		}
		return ast.newName(names.toArray(new String[names.size()]));
	}

	/**
	 * Decompile type.
	 * 
	 * @param t
	 *            type
	 * @param td
	 *            type declaration (context)
	 * @return AST Type
	 */
	public static Type decompileType(final T t, final TD td) {
		final AST ast = td.getCu().getAst();
		if (t instanceof ArrayT) {
			return ast.newArrayType(decompileType(t.getComponentT(), td));
		}
		if (t instanceof ParamT) {
			final ParameterizedType parameterizedType = ast.newParameterizedType(decompileType(
					((ParamT) t).getGenericT(), td));
			for (final TypeArg typeArg : ((ParamT) t).getTypeArgs()) {
				switch (typeArg.getKind()) {
				case UNBOUND: {
					parameterizedType.typeArguments().add(ast.newWildcardType());
					break;
				}
				case SUBCLASS_OF: {
					final WildcardType wildcardType = ast.newWildcardType();
					// default...newWildcardType.setUpperBound(true);
					wildcardType.setBound(decompileType(typeArg.getT(), td));
					parameterizedType.typeArguments().add(wildcardType);
					break;
				}
				case SUPER_OF: {
					final WildcardType wildcardType = ast.newWildcardType();
					wildcardType.setUpperBound(false);
					wildcardType.setBound(decompileType(typeArg.getT(), td));
					parameterizedType.typeArguments().add(wildcardType);
					break;
				}
				default: {
					parameterizedType.typeArguments().add(decompileType(typeArg.getT(), td));
				}
				}
			}
			return parameterizedType;
		}
		if (t.isMulti()) {
			LOGGER.warning("Convert type for multi-type '" + t + "'!");
		}
		if (t.is(T.INT)) {
			return ast.newPrimitiveType(PrimitiveType.INT);
		}
		if (t.is(T.SHORT)) {
			return ast.newPrimitiveType(PrimitiveType.SHORT);
		}
		if (t.is(T.BYTE)) {
			return ast.newPrimitiveType(PrimitiveType.BYTE);
		}
		if (t.is(T.CHAR)) {
			return ast.newPrimitiveType(PrimitiveType.CHAR);
		}
		if (t.is(T.BOOLEAN)) {
			return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		}
		if (t.is(T.FLOAT)) {
			return ast.newPrimitiveType(PrimitiveType.FLOAT);
		}
		if (t.is(T.LONG)) {
			return ast.newPrimitiveType(PrimitiveType.LONG);
		}
		if (t.is(T.DOUBLE)) {
			return ast.newPrimitiveType(PrimitiveType.DOUBLE);
		}
		if (t.is(T.VOID)) {
			return ast.newPrimitiveType(PrimitiveType.VOID);
		}
		if (t instanceof ClassT) {
			final T enclosingT = ((ClassT) t).getEnclosingT();
			if (enclosingT != null) {
				// could be ParamT etc., not decompileable with Name as target
				final Type qualifier = decompileType(enclosingT, td);
				return ast.newQualifiedType(qualifier, ast.newSimpleName(t.getSimpleIdentifier()));
			}
		}
		return ast.newSimpleType(decompileName(t, td));
	}

	/**
	 * Get original integer value for literal expression.
	 * 
	 * Sometimes we must backtranslate literal constants like Byte.MAX_VALUE.
	 * 
	 * Potential problem: ASTNode#copySubtree() will forget additional properties, but for the
	 * backtranslate use case this isn't really expected. Fall back is cast to NumberLiteral and
	 * Integer parsing.
	 * 
	 * @param e
	 *            literal expression
	 * @return integer for literal
	 */
	public static int getIntValue(final Expression e) {
		final Object value = e.getProperty(PROP_LITERAL_VALUE);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		assert false; // shouldn't really happen

		return Integer.parseInt(((NumberLiteral) e).getToken());
	}

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
		final int operatorPriority = Priority.priority(infixExpression).getPriority();
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
		prefixExpression.setOperand(wrap(operand, Priority.priority(prefixExpression)));
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
	 * Unwrap expression, means remove parathesizes.
	 * 
	 * We don't remove parents here, copy lazy at wrap time again.
	 * 
	 * @param expression
	 *            expression
	 * @return unwrapped expression
	 */
	public static Expression unwrap(final Expression expression) {
		Expression e = expression;
		while (e instanceof ParenthesizedExpression) {
			e = ((ParenthesizedExpression) e).getExpression();
		}
		return e;
	}

	/**
	 * Wrap expression. Ensures that there is no parent set.
	 * 
	 * @param expression
	 *            expression
	 * @return wrapped expression
	 */
	public static Expression wrap(final Expression expression) {
		return expression.getParent() == null ? expression : (Expression) ASTNode.copySubtree(
				expression.getAST(), expression);
	}

	private static Expression wrap(final Expression expression, final int priority) {
		final Expression e = wrap(expression);
		if (Priority.priority(e).getPriority() <= priority) {
			return e;
		}
		final ParenthesizedExpression parenthesizedExpression = expression.getAST()
				.newParenthesizedExpression();
		parenthesizedExpression.setExpression(e);
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