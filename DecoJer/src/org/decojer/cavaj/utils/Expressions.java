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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.model.types.Version;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.WildcardType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Helper functions for expressions handling.
 *
 * @author André Pankraz
 */
@Slf4j
public final class Expressions {

	/**
	 * Get boolean value from literal.
	 *
	 * @param literal
	 *            literal expression
	 * @return {@code null} - no boolean literal, {@code Boolean#TRUE} - true, {@code Boolean#FALSE}
	 *         - true
	 */
	@Nullable
	public static Boolean getBooleanValue(@Nonnull final Expression literal) {
		// don't add Number and NumberLiteral here or we run into problems for (test ? 4 : 0) etc.,
		// improve data flow analysis instead
		final Object value = getValue(literal);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		if (literal instanceof BooleanLiteral) {
			return ((BooleanLiteral) literal).booleanValue();
		}
		return null;
	}

	/**
	 * Get original number value for literal expression.
	 *
	 * Sometimes we must backtranslate literal constants like Byte.MAX_VALUE.
	 *
	 * Potential problem: ASTNode#copySubtree() will forget additional properties, but for the
	 * backtranslate use case this isn't really expected. Fall back is cast to NumberLiteral and
	 * Integer parsing.
	 *
	 * @param literal
	 *            literal expression
	 * @return integer for literal
	 */
	@Nullable
	public static Number getNumberValue(@Nonnull final Expression literal) {
		final Object value = getValue(literal);
		if (value instanceof Number) {
			return (Number) value;
		}
		if (literal instanceof NumberLiteral) {
			return Integer.getInteger(((NumberLiteral) literal).getToken());
		}
		return null;
	}

	/**
	 * Get originating operation.
	 *
	 * @param node
	 *            AST node
	 * @return originating operation
	 */
	@Nullable
	public static Op getOp(@Nullable final ASTNode node) {
		return node == null ? null : (Op) node.getProperty(PROP_OP);
	}

	/**
	 * Get originating literal value.
	 *
	 * Cannot replace this through getInFrame(getOp()) because PUSH etc. just change unknown out
	 * frame.
	 *
	 * @param literal
	 *            AST literal expression
	 * @return originating literal value
	 */
	@Nullable
	public static Object getValue(@Nonnull final Expression literal) {
		return literal.getProperty(PROP_VALUE);
	}

	/**
	 * Check if given expression is a StatementExpression, that can be wrapped into a single
	 * ExpressionStatement (see Java Language Specification, chapter 14, Blocks and Statements).
	 *
	 * @param e
	 *            expression
	 * @return {@code true} - is a statement expression
	 */
	public static boolean isStatementExpression(@Nullable final Expression e) {
		if (e instanceof Assignment) {
			return true;
		}
		if (e instanceof PrefixExpression) {
			final PrefixExpression pe = (PrefixExpression) e;
			return pe.getOperator() == PrefixExpression.Operator.INCREMENT
					|| pe.getOperator() == PrefixExpression.Operator.DECREMENT;
		}
		if (e instanceof PostfixExpression) {
			final PostfixExpression pe = (PostfixExpression) e;
			return pe.getOperator() == PostfixExpression.Operator.INCREMENT
					|| pe.getOperator() == PostfixExpression.Operator.DECREMENT;
		}
		if (e instanceof MethodInvocation) {
			return true;
		}
		if (e instanceof ClassInstanceCreation) {
			return true;
		}
		return false;
	}

	/**
	 * New assignment expression.
	 *
	 * @param operator
	 *            assignment expression operator
	 * @param leftOperand
	 *            left operand expression
	 * @param rightOperand
	 *            right operand expression
	 * @param op
	 *            originating operation
	 * @return expression
	 */
	@Nonnull
	public static Assignment newAssignment(@Nullable final Assignment.Operator operator,
			@Nonnull final Expression leftOperand, @Nonnull final Expression rightOperand,
			@Nonnull final Op op) {
		final Assignment assignment = setOp(leftOperand.getAST().newAssignment(), op);
		assignment.setOperator(operator);
		final int operatorPriority = Priority.priority(assignment).getPriority();
		assignment.setLeftHandSide(wrap(leftOperand, operatorPriority));
		assignment.setRightHandSide(wrap(rightOperand, operatorPriority));
		return assignment;
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
	 * @param op
	 *            originating operation
	 * @return expression
	 */
	@Nonnull
	public static InfixExpression newInfixExpression(
			@Nullable final InfixExpression.Operator operator,
			@Nonnull final Expression leftOperand, @Nonnull final Expression rightOperand,
			@Nonnull final Op op) {
		final InfixExpression infixExpression = setOp(leftOperand.getAST().newInfixExpression(), op);
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
	 * New literal.
	 *
	 * @param t
	 *            literal type
	 * @param value
	 *            literal value
	 * @param context
	 *            context
	 * @param op
	 *            originating operation, can be {@code null} for field constants
	 * @return AST literal expression
	 */
	@Nonnull
	public static Expression newLiteral(@Nonnull final T t, @Nullable final Object value,
			@Nonnull final Element context, @Nullable final Op op) {
		return setOp(setValue(newLiteral2(t, value, context), value), op);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Expression newLiteral2(@Nonnull final T t, @Nullable final Object value,
			@Nonnull final Element context) {
		final AST ast = context.getCu().getAst();
		if (t.isRef() /* incl. T.AREF */) {
			if (value == null) {
				return ast.newNullLiteral();
			}
			if (value instanceof T && t.isAssignableFrom(Class.class)) {
				final TypeLiteral typeLiteral = ast.newTypeLiteral();
				typeLiteral.setType(newType((T) value, context));
				return typeLiteral;
			}
			if (value instanceof String && t.isAssignableFrom(String.class)) {
				final StringLiteral stringLiteral = ast.newStringLiteral();
				try {
					stringLiteral.setLiteralValue((String) value);
				} catch (final IllegalArgumentException e) {
					// TODO hmm, escaping doesn't always work?
					stringLiteral.setLiteralValue("<Invalid string literal>");
				}
				return stringLiteral;
			}
			log.warn(context + ": Unknown reference type '" + t + "'!");
			return ast.newNullLiteral();
		}
		if (t.is(T.BOOLEAN)) {
			// we prefer boolean, even if this is a multi-type
			if (value instanceof Boolean) {
				return ast.newBooleanLiteral(((Boolean) value).booleanValue());
			}
			if (value instanceof Number) {
				return ast.newBooleanLiteral(((Number) value).intValue() != 0);
			}
			if (value == null) {
				return ast.newBooleanLiteral(false);
			}
			log.warn("Boolean type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newBooleanLiteral(value instanceof String ? Boolean.valueOf((String) value)
					: true /* value is not null here */);
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
			log.warn("Byte type with value '" + value + "' has type '" + value.getClass().getName()
					+ "'!");
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
			log.warn("Short type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString());
		}
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
			log.warn("Integer type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString());
		}
		if (t.is(T.CHAR)) {
			// if this is a multi-type, we only use char if this is not already consumed;
			// we don't want to output strange characters if we are not very shure about this
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
					if (context.getT().isAtLeast(Version.JVM_5)) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MAX_HIGH_SURROGATE"));
					}
					break;
				case Character.MAX_LOW_SURROGATE:
					if (context.getT().isAtLeast(Version.JVM_5)) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MAX_LOW_SURROGATE"));
					}
					break;
				case Character.MIN_HIGH_SURROGATE:
					if (context.getT().isAtLeast(Version.JVM_5)) {
						return ast.newQualifiedName(ast.newSimpleName("Character"),
								ast.newSimpleName("MIN_HIGH_SURROGATE"));
					}
					break;
				case Character.MIN_LOW_SURROGATE:
					if (context.getT().isAtLeast(Version.JVM_5)) {
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
			log.warn("Character type with value '" + value + "' has type '"
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
					if (context.getT().isAtLeast(Version.JVM_6)) {
						return ast.newQualifiedName(ast.newSimpleName("Float"),
								ast.newSimpleName("MIN_NORMAL"));
					}
				}
				return ast.newNumberLiteral(Float.toString(f) + 'F');
			}
			if (value == null) {
				return ast.newNumberLiteral(Float.toString(0F) + 'F');
			}
			log.warn("Float type with value '" + value + "' has type '"
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
			log.warn("Long type with value '" + value + "' has type '" + value.getClass().getName()
					+ "'!");
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
					if (context.getT().isAtLeast(Version.JVM_6)) {
						return ast.newQualifiedName(ast.newSimpleName("Double"),
								ast.newSimpleName("MIN_NORMAL"));
					}
				}
				return ast.newNumberLiteral(Double.toString(d) + 'D');
			}
			if (value == null) {
				return ast.newNumberLiteral(Double.toString(0D) + 'D');
			}
			log.warn("Double type with value '" + value + "' has type '"
					+ value.getClass().getName() + "'!");
			return ast.newNumberLiteral(value.toString() + 'D');
		}
		log.warn("Unknown data type '" + t + "'!");
		final StringLiteral stringLiteral = ast.newStringLiteral();
		if (value != null) {
			stringLiteral.setLiteralValue(value.toString());
		}
		return stringLiteral;
	}

	/**
	 * New name. Handles illegal Java names.
	 *
	 * @param identifiers
	 *            identifiers
	 * @param ast
	 *            AST
	 * @return AST name
	 */
	@Nonnull
	public static Name newName(@Nonnull final String[] identifiers, @Nonnull final AST ast) {
		// update internalSetName(String[] if changed
		final int count = identifiers.length;
		if (count == 0) {
			throw new IllegalArgumentException();
		}
		Name result = newSimpleName(identifiers[0], ast);
		for (int i = 1; i < count; i++) {
			final SimpleName name = newSimpleName(identifiers[i], ast);
			result = ast.newQualifiedName(result, name);
			assert result != null;
		}
		return result;
	}

	/**
	 * New postfix expression.
	 *
	 * @param operator
	 *            postfix expression operator
	 * @param operand
	 *            operand expression
	 * @param op
	 *            originating operation
	 * @return expression
	 */
	@Nonnull
	public static PostfixExpression newPostfixExpression(
			@Nullable final PostfixExpression.Operator operator, @Nonnull final Expression operand,
			@Nonnull final Op op) {
		final PostfixExpression postfixExpression = setOp(operand.getAST().newPostfixExpression(),
				op);
		postfixExpression.setOperator(operator);
		postfixExpression.setOperand(wrap(operand, Priority.priority(postfixExpression)));
		return postfixExpression;
	}

	/**
	 * New prefix expression.
	 *
	 * @param operator
	 *            prefix expression operator
	 * @param operand
	 *            operand expression
	 * @param op
	 *            originating operation
	 * @return expression
	 */
	@Nonnull
	public static Expression newPrefixExpression(
			@Nullable final PrefixExpression.Operator operator, @Nonnull final Expression operand,
			@Nonnull final Op op) {
		if (operator == PrefixExpression.Operator.NOT) {
			return not(operand);
		}
		final PrefixExpression prefixExpression = setOp(operand.getAST().newPrefixExpression(), op);
		prefixExpression.setOperator(operator);
		prefixExpression.setOperand(wrap(operand, Priority.priority(prefixExpression)));
		return prefixExpression;
	}

	/**
	 * New simple name. Handles illegal Java names.
	 *
	 * @param identifier
	 *            identifier
	 * @param ast
	 *            AST
	 * @return AST simple name
	 */
	@Nonnull
	public static SimpleName newSimpleName(final String identifier, final AST ast) {
		SimpleName simpleName;
		try {
			simpleName = ast.newSimpleName(identifier);
			// setIdentifier() uses fixed scanner.sourceLevel = ClassFileConstants.JDK1_3,
			// don't know why, but this doesn't recognize JDK4 assert and JDK5 enum here
			// TODO create Eclipse Bug report for this, expensive double check,
			// seen in AbstractExecutorService.assert (1.4 code)
			if ("assert".equals(identifier) || "enum".equals(identifier)) {
				throw new IllegalArgumentException();
			}
		} catch (final IllegalArgumentException e) {
			String name;
			if (JAVA_KEYWORDS.contains(identifier)) {
				// e.g. scala uses "default" as valid identifier
				name = "_" + identifier;
			} else {
				// obfuscated code might run into this...e.g. "a.123"
				final StringBuilder sb = new StringBuilder(identifier.length());
				for (int i = 0; i < identifier.length(); ++i) {
					final char c = identifier.charAt(i);
					if (!Character.isJavaIdentifierPart(c)) {
						sb.append('_');
						continue;
					}
					if (i == 0 && !Character.isJavaIdentifierStart(c)) {
						sb.append('_');
					}
					sb.append(c);
				}
				name = sb.toString();
			}
			try {
				simpleName = ast.newSimpleName(name);
				log.info("Couldn't create simple name with identifier '" + identifier
						+ "'! Rewritten to '" + name + "'.");
			} catch (final IllegalArgumentException e1) {
				log.warn("Couldn't create simple name with identifier '" + identifier
						+ "' or rewritten '" + name + "'!", e);
				simpleName = ast.newSimpleName("_invalid_identifier_");
			}
		}
		assert simpleName != null;
		return simpleName;
	}

	/**
	 * New single variable declaration.
	 *
	 * @param m
	 *            method declaration
	 * @param paramTs
	 *            parameter types
	 * @param paramAss
	 *            parameter annotations
	 * @param i
	 *            index
	 * @param contextT
	 *            type declaration (context)
	 * @return single variable declaration
	 */
	@SuppressWarnings("deprecation")
	public static SingleVariableDeclaration newSingleVariableDeclaration(final M m,
			final T[] paramTs, final A[][] paramAss, final int i, final T contextT) {
		final AST ast = contextT.getCu().getAst();
		final SingleVariableDeclaration singleVariableDeclaration = ast
				.newSingleVariableDeclaration();
		if (paramAss != null && i < paramAss.length) {
			final List<IExtendedModifier> modifiers = singleVariableDeclaration.modifiers();
			assert modifiers != null;
			Annotations.decompileAnnotations(paramAss[i], modifiers, contextT);
		}
		final T paramT = paramTs[i];
		assert paramT != null;
		final Type methodParameterType = newType(paramT, contextT);
		// decompile varargs (flag set, ArrayType and last method param)
		if (i == paramTs.length - 1 && m.isVarargs()) {
			if (methodParameterType instanceof ArrayType) {
				singleVariableDeclaration.setVarargs(true);
				// must copy because we cannot delete mandatory ArrayType.componentType
				if (ast.apiLevel() <= AST.JLS4) {
					singleVariableDeclaration.setType((Type) ASTNode.copySubtree(ast,
							((ArrayType) methodParameterType).getComponentType()));
				} else {
					singleVariableDeclaration.setType((Type) ASTNode.copySubtree(ast,
							((ArrayType) methodParameterType).getElementType()));
				}
			} else {
				log.warn("Last method parameter is no ArrayType, but method '" + m.getName()
						+ "' has vararg attribute!");
				// try handling as normal type
				singleVariableDeclaration.setType(methodParameterType);
			}
		} else {
			singleVariableDeclaration.setType(methodParameterType);
		}
		singleVariableDeclaration.setName(newSimpleName(m.getParamName(i), ast));
		return singleVariableDeclaration;
	}

	/**
	 * New type.
	 *
	 * @param t
	 *            type
	 * @param context
	 *            context
	 * @return AST type
	 */
	@SuppressWarnings("deprecation")
	public static Type newType(@Nonnull final T t, @Nonnull final Element context) {
		final AST ast = context.getCu().getAst();
		// handle array first because annot(array()) is special
		if (t.isArray()) {
			if (ast.apiLevel() <= AST.JLS4) {
				final T componentT = t.getComponentT();
				assert componentT != null;
				return ast.newArrayType(newType(componentT, context));
			}
			final T elementT = t.getElementT();
			assert elementT != null;
			for (T checkT = t; checkT != null && checkT.isArray(); checkT = checkT.getComponentT()) {
				if (checkT.isAnnotated()) {
					final ArrayType arrayType = ast.newArrayType(newType(elementT, context));
					final List<Dimension> dimensions = arrayType.dimensions();
					dimensions.clear();
					for (T componentT = t; componentT != null && componentT.isArray(); componentT = componentT
							.getComponentT()) {
						final Dimension dimension = ast.newDimension();
						final List<IExtendedModifier> annotations = dimension.annotations();
						assert annotations != null;
						if (componentT.isAnnotated()) {
							Annotations.decompileAnnotations(componentT, annotations, context);
						}
						dimensions.add(dimension);
					}
					return arrayType;
				}
			}
			return ast.newArrayType(newType(elementT, context), t.getDimensions());
		}
		if (t.isAnnotated()) {
			Type type = newType(t.getRawT(), context);
			if (ast.apiLevel() <= AST.JLS4) {
				log.warn("Cannot decompile type annotations for type '" + t
						+ "' in Eclipse AST JLS4!");
				return type;
			}
			// parameterized type is not directly annotateable in Eclipse; but DecoJer thinks the
			// whole type is meant, not just the generic type, hence translate here
			AnnotatableType annotatableType = (AnnotatableType) (type instanceof ParameterizedType ? ((ParameterizedType) type)
					.getType() : type);
			qualified: if (annotatableType instanceof SimpleType) {
				// direct annotation of qualified types adds the annotations as first element,
				// strange Java spec doesn't allow "@A package.Name" but wants "package.@A Name"
				final Name typeName = ((SimpleType) annotatableType).getName();
				if (!(typeName instanceof QualifiedName)) {
					break qualified;
				}
				final Name qualifier = ((QualifiedName) typeName).getQualifier();
				final SimpleName name = ((QualifiedName) typeName).getName();
				// cannot delete mandory childs, copy them
				annotatableType = ast.newNameQualifiedType(
						(Name) ASTNode.copySubtree(ast, qualifier),
						(SimpleName) ASTNode.copySubtree(ast, name));
				if (type instanceof ParameterizedType) {
					((ParameterizedType) type).setType(annotatableType);
				} else {
					type = annotatableType;
				}
			}
			final List<IExtendedModifier> annotations = annotatableType.annotations();
			assert annotations != null;
			Annotations.decompileAnnotations(t, annotations, context);
			return type;
		}
		// doesn't work, now with Dimension (see above): if (t.isArray()) { return
		// ast.newArrayType(newType(t.getComponentT(), contextT)); }
		if (t.isParameterized()) {
			final T genericT = t.getGenericT();
			assert genericT != null;
			final ParameterizedType parameterizedType = ast.newParameterizedType(newType(genericT,
					context));
			for (final T typeArg : t.getTypeArgs()) {
				assert typeArg != null;
				parameterizedType.typeArguments().add(newType(typeArg, context));
			}
			return parameterizedType;
		}
		if (t.isWildcard()) {
			final T boundT = t.getBoundT();
			if (boundT == null) {
				return ast.newWildcardType();
			}
			if (t.isSubclassOf()) {
				final WildcardType wildcardType = ast.newWildcardType();
				// default...newWildcardType.setUpperBound(true);
				wildcardType.setBound(newType(boundT, context));
				return wildcardType;
			}
			final WildcardType wildcardType = ast.newWildcardType();
			wildcardType.setUpperBound(false);
			wildcardType.setBound(newType(boundT, context));
			return wildcardType;
		}
		if (t.isMulti()) {
			log.warn("Convert type for multi-type '" + t + "'!");
			// prefer boolean for multi-type with 0 or 1, synchronous to newLiteral()!
			// prefer byte before char if no explicit char type given
		}
		if (t.is(T.BOOLEAN)) {
			return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		}
		if (t.is(T.BYTE)) {
			return ast.newPrimitiveType(PrimitiveType.BYTE);
		}
		if (t.is(T.CHAR)) {
			return ast.newPrimitiveType(PrimitiveType.CHAR);
		}
		if (t.is(T.SHORT)) {
			return ast.newPrimitiveType(PrimitiveType.SHORT);
		}
		if (t.is(T.INT)) {
			return ast.newPrimitiveType(PrimitiveType.INT);
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
		if (t.isQualified()) {
			final T qualifierT = t.getQualifierT();
			assert qualifierT != null : "cannot be null for qualified";
			// could be ParamT etc., not decompileable with name as target;
			// restrict qualifications to really necessary enclosings:
			// t = Outer.Inner.InnerInner, t = Outer.Inner ==> Inner
			final T contextT = context.getT();
			assert contextT != null;
			if (contextT.getFullName().startsWith(qualifierT.getFullName())) {
				// TODO full name has too much info yet (like annotations)
				return ast.newSimpleType(newSimpleName(t.getSimpleIdentifier(), ast));
			}
			return ast.newQualifiedType(newType(qualifierT, context),
					newSimpleName(t.getSimpleIdentifier(), ast));
		}
		// else fall through...
		return ast.newSimpleType(newTypeName(t, context));
	}

	/**
	 * New type name.
	 *
	 * @param t
	 *            type
	 * @param context
	 *            context
	 * @return AST type name
	 */
	public static Name newTypeName(final T t, @Nonnull final Element context) {
		final AST ast = context.getCu().getAst();
		final String contextName = context.getName();
		final T contextT = context.getT();
		assert contextT != null;
		final String contextPackageName = contextT.getPackageName();

		// convert inner classes separator '$' into '.',
		// cannot use string replace because '$' is also a regular Java type name!
		// must use enclosing info

		T currentT = t;
		final List<String> names = Lists.newArrayList();

		while (true) {
			final T enclosingT = currentT.getEnclosingT();
			if (enclosingT == null) {
				names.add(0, currentT.getPName());
				final String packageName = currentT.getPackageName();
				if (packageName == null || packageName.equals(contextPackageName)
						|| packageName.equals(JAVA_LANG)) {
					// ignore package iff same like context or like Java default package
					break;
				}
				// TODO here we could seperate packages for outest type annotations
				int end = packageName.length();
				while (true) {
					final int pos = packageName.lastIndexOf('.', end - 1);
					if (pos == -1) {
						names.add(0, packageName.substring(0, end));
						break;
					}
					names.add(0, packageName.substring(pos + 1, end));
					end = pos;
				}
				break;
			}
			names.add(0, currentT.getSimpleIdentifier());
			if (contextName.startsWith(enclosingT.getName())) {
				// find common name dominator and stop there, for relative inner names
				break;
			}
			currentT = enclosingT;
		}
		final String[] namesArray = names.toArray(new String[names.size()]);
		assert namesArray != null;
		return newName(namesArray, ast);
	}

	/**
	 * Not expression.
	 *
	 * @param operand
	 *            operand expression
	 * @return !expression
	 */
	@Nonnull
	@SuppressWarnings("null")
	public static Expression not(@Nonnull final Expression operand) {
		if (operand instanceof ParenthesizedExpression) {
			return not(((ParenthesizedExpression) operand).getExpression());
		}
		if (operand instanceof PrefixExpression) {
			if (((PrefixExpression) operand).getOperator() == PrefixExpression.Operator.NOT) {
				// !!a => a
				return unwrap(((PrefixExpression) operand).getOperand());
			}
		} else if (operand instanceof InfixExpression) {
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
								not(infixExpression.getRightOperand()), getOp(infixExpression));
			}
		} else if (operand instanceof ConditionalExpression) {
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
		final PrefixExpression prefixExpression = setOp(operand.getAST().newPrefixExpression(),
				getOp(operand));
		prefixExpression.setOperator(PrefixExpression.Operator.NOT);
		prefixExpression.setOperand(wrap(operand, Priority.priority(prefixExpression)));
		return prefixExpression;
	}

	/**
	 * Set originating operation.
	 *
	 * @param node
	 *            AST node
	 * @param op
	 *            originating operation
	 * @return expression
	 */
	@Nonnull
	public static <E extends ASTNode> E setOp(final E node, @Nullable final Op op) {
		assert node != null; // don't mark as @Nonnull in params, many warnings
		if (op != null) {
			node.setProperty(PROP_OP, op);
		}
		return node;
	}

	/**
	 * Set originating literal value.
	 *
	 * Cannot replace this through setOp() -> getInFrame(getOp()) because PUSH etc. just change
	 * unknown out frame.
	 *
	 * @param expression
	 *            AST expression
	 * @param value
	 *            originating literal value
	 * @return expression
	 */
	@Nonnull
	public static <E extends Expression> E setValue(final E expression, @Nullable final Object value) {
		assert expression != null; // don't mark as @Nonnull in params, many warnings
		if (value != null) {
			expression.setProperty(PROP_VALUE, value);
		}
		return expression;
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
	@Nonnull
	public static Expression unwrap(@Nonnull final Expression expression) {
		Expression e = expression;
		while (e instanceof ParenthesizedExpression) {
			e = ((ParenthesizedExpression) e).getExpression();
		}
		assert e != null;
		return e;
	}

	/**
	 * Wrap expression. Ensures that there is no parent set.
	 *
	 * @param expression
	 *            expression
	 * @return wrapped expression
	 */
	@Nonnull
	public static Expression wrap(@Nonnull final Expression expression) {
		if (expression.getParent() == null) {
			return expression;
		}
		return setOp(
				setValue((Expression) ASTNode.copySubtree(expression.getAST(), expression),
						getValue(expression)), getOp(expression));
	}

	@Nonnull
	private static Expression wrap(@Nonnull final Expression expression, final int priority) {
		final Expression e = wrap(expression);
		if (Priority.priority(e).getPriority() <= priority) {
			return e;
		}
		final ParenthesizedExpression parenthesizedExpression = setOp(expression.getAST()
				.newParenthesizedExpression(), getOp(expression));
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
	@Nonnull
	public static Expression wrap(@Nonnull final Expression expression,
			@Nonnull final Priority priority) {
		return wrap(expression, priority.getPriority());
	}

	/**
	 * Wrap and add expressions. Ensures that there is no parent set.
	 *
	 * We use this instead of returning a wrapped list because of potential DUP operations.
	 *
	 * @param expressions
	 *            expressions
	 * @param addExpressions
	 *            expressions that are wrapped and added
	 */
	public static void wrapAddAll(final List<Expression> expressions,
			final List<Expression> addExpressions) {
		for (final Expression expression : addExpressions) {
			assert expression != null;
			expressions.add(wrap(expression));
		}
	}

	private static final String JAVA_LANG = "java.lang";

	public static final String PROP_OP = "propOp";

	private static final String PROP_VALUE = "propValue";

	private static final Set<String> JAVA_KEYWORDS;

	static {
		JAVA_KEYWORDS = Sets.newHashSet();
		JAVA_KEYWORDS.add("abstract");
		JAVA_KEYWORDS.add("assert"); // added in JVM 4
		JAVA_KEYWORDS.add("boolean");
		JAVA_KEYWORDS.add("break");
		JAVA_KEYWORDS.add("byte");
		JAVA_KEYWORDS.add("case");
		JAVA_KEYWORDS.add("catch");
		JAVA_KEYWORDS.add("char");
		JAVA_KEYWORDS.add("class");
		JAVA_KEYWORDS.add("const"); // not used
		JAVA_KEYWORDS.add("continue");
		JAVA_KEYWORDS.add("default");
		JAVA_KEYWORDS.add("do");
		JAVA_KEYWORDS.add("double");
		JAVA_KEYWORDS.add("else");
		JAVA_KEYWORDS.add("enum"); // added in JVM 5
		JAVA_KEYWORDS.add("extends");
		JAVA_KEYWORDS.add("false"); // boolean literal
		JAVA_KEYWORDS.add("final");
		JAVA_KEYWORDS.add("finally");
		JAVA_KEYWORDS.add("float");
		JAVA_KEYWORDS.add("for");
		JAVA_KEYWORDS.add("goto"); // not used
		JAVA_KEYWORDS.add("if");
		JAVA_KEYWORDS.add("implements");
		JAVA_KEYWORDS.add("import");
		JAVA_KEYWORDS.add("instanceof");
		JAVA_KEYWORDS.add("int");
		JAVA_KEYWORDS.add("interface");
		JAVA_KEYWORDS.add("long");
		JAVA_KEYWORDS.add("native");
		JAVA_KEYWORDS.add("new");
		JAVA_KEYWORDS.add("null"); // null literal
		JAVA_KEYWORDS.add("package");
		JAVA_KEYWORDS.add("private");
		JAVA_KEYWORDS.add("protected");
		JAVA_KEYWORDS.add("public");
		JAVA_KEYWORDS.add("return");
		JAVA_KEYWORDS.add("short");
		JAVA_KEYWORDS.add("static");
		JAVA_KEYWORDS.add("strictfp"); // added in JVM 4
		JAVA_KEYWORDS.add("super");
		JAVA_KEYWORDS.add("switch");
		JAVA_KEYWORDS.add("synchronized");
		JAVA_KEYWORDS.add("this");
		JAVA_KEYWORDS.add("throw");
		JAVA_KEYWORDS.add("throws");
		JAVA_KEYWORDS.add("transient");
		JAVA_KEYWORDS.add("true"); // boolean literal
		JAVA_KEYWORDS.add("try");
		JAVA_KEYWORDS.add("void");
		JAVA_KEYWORDS.add("volatile");
		JAVA_KEYWORDS.add("while");
	}

	private Expressions() {
		// static helper class
	}

}