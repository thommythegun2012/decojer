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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.types.AnnotT;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.model.types.ParamT.TypeArg;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.WildcardType;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Helper functions for expressions handling.
 * 
 * @author André Pankraz
 */
public final class Expressions {

	private final static Logger LOGGER = Logger.getLogger(Expressions.class.getName());

	private static final String JAVA_LANG = "java.lang";

	public static final String PROP_OP = "propOp";

	private static final String PROP_VALUE = "propValue";

	private static final Set<String> JAVA_KEYWORDS;

	static {
		JAVA_KEYWORDS = Sets.newHashSet();
		JAVA_KEYWORDS.add("abstract");
		JAVA_KEYWORDS.add("assert"); // added in JDK 5
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
		JAVA_KEYWORDS.add("enum"); // added in JDK 5
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
		JAVA_KEYWORDS.add("strictfp"); // added JDK 4
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

	/**
	 * Get boolean value from literal.
	 * 
	 * @param literal
	 *            literal expression
	 * @return {@code null} - no boolean literal, {@code Boolean#TRUE} - true, {@code Boolean#FALSE}
	 *         - true
	 */
	public static Boolean getBooleanValue(final Expression literal) {
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
	 * @return integer for literal or {@code null}
	 */
	public static Number getNumberValue(final Expression literal) {
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
	public static Op getOp(final ASTNode node) {
		return (Op) node.getProperty(PROP_OP);
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
	public static Object getValue(final Expression literal) {
		return literal.getProperty(PROP_VALUE);
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
	public static Assignment newAssignment(final Assignment.Operator operator,
			final Expression leftOperand, final Expression rightOperand, final Op op) {
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
	public static InfixExpression newInfixExpression(final InfixExpression.Operator operator,
			final Expression leftOperand, final Expression rightOperand, final Op op) {
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
	 * @param td
	 *            type declaration (context)
	 * @param op
	 *            originating operation
	 * @return AST literal expression
	 */
	public static Expression newLiteral(final T t, final Object value, final TD td, final Op op) {
		return setOp(setValue(newLiteral2(t, value, td), value), op);
	}

	private static Expression newLiteral2(final T t, final Object value, final TD td) {
		final AST ast = td.getCu().getAst();
		if (t.isRef() /* incl. T.AREF */) {
			if (value == null) {
				return ast.newNullLiteral();
			}
			if (value instanceof T && t.isAssignableFrom(Class.class)) {
				final TypeLiteral typeLiteral = ast.newTypeLiteral();
				typeLiteral.setType(newType((T) value, td));
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
		if (t.isMulti() && t.is(T.BOOLEAN)) {
			LOGGER.warning("Convert literal '" + value + "' for multi-type '" + t + "'!");
			// prefer boolean for multi-type with 0 or 1, synchronous to newType()!
			// prefer byte before char if no explicit char type given
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
	public static PostfixExpression newPostfixExpression(final PostfixExpression.Operator operator,
			final Expression operand, final Op op) {
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
	public static Expression newPrefixExpression(final PrefixExpression.Operator operator,
			final Expression operand, final Op op) {
		if (operator == PrefixExpression.Operator.NOT) {
			return not(operand);
		}
		final PrefixExpression prefixExpression = setOp(operand.getAST().newPrefixExpression(), op);
		prefixExpression.setOperator(operator);
		prefixExpression.setOperand(wrap(operand, Priority.priority(prefixExpression)));
		return prefixExpression;
	}

	/**
	 * New simple name.
	 * 
	 * @param identifier
	 *            identifier
	 * @param ast
	 *            AST
	 * @return AST simple name
	 */
	public static SimpleName newSimpleName(final String identifier, final AST ast) {
		try {
			return ast.newSimpleName(identifier);
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
				final SimpleName simpleName = ast.newSimpleName(name);
				LOGGER.info("Couldn't create simple name with identifier '" + identifier
						+ "'! Rewritten to '" + name + "'.");
				return simpleName;
			} catch (final IllegalArgumentException e1) {
				LOGGER.log(Level.WARNING, "Couldn't create simple name with identifier '"
						+ identifier + "' or rewritten '" + name + "'!", e);
				return ast.newSimpleName("_invalid_identifier_");
			}
		}
	}

	/**
	 * New type.
	 * 
	 * @param t
	 *            type
	 * @param td
	 *            type declaration (context)
	 * @return AST type
	 */
	public static Type newType(final T t, final TD td) {
		final AST ast = td.getCu().getAst();
		if (t instanceof AnnotT) {
			final Type type = newType(t.getRawT(), td);
			Type annotatableType = type;
			if (annotatableType instanceof ParameterizedType) {
				annotatableType = ((ParameterizedType) annotatableType).getType();
			}
			Annotations.decompileAnnotations(td, ((AnnotatableType) annotatableType).annotations(),
					t);
			return type;
		}
		if (t.isArray()) {
			if (ast.apiLevel() >= AST.JLS8) {
				return ast.newArrayType(newType(t.getElementT(), td), t.getDimensions());
			}
			return ast.newArrayType(newType(t.getComponentT(), td));
		}
		if (t instanceof ParamT) {
			final ParameterizedType parameterizedType = ast.newParameterizedType(newType(
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
					wildcardType.setBound(newType(typeArg.getT(), td));
					parameterizedType.typeArguments().add(wildcardType);
					break;
				}
				case SUPER_OF: {
					final WildcardType wildcardType = ast.newWildcardType();
					wildcardType.setUpperBound(false);
					wildcardType.setBound(newType(typeArg.getT(), td));
					parameterizedType.typeArguments().add(wildcardType);
					break;
				}
				default: {
					parameterizedType.typeArguments().add(newType(typeArg.getT(), td));
				}
				}
			}
			return parameterizedType;
		}
		if (t.isMulti()) {
			LOGGER.warning("Convert type for multi-type '" + t + "'!");
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
		if (t instanceof ClassT) {
			final T enclosingT = ((ClassT) t).getEnclosingT();
			if (enclosingT != null) {
				// could be ParamT etc., not decompileable with Name as target
				// TODO restrict qualifications to really necessary enclosings...
				// td = Outer.Inner.InnerInner, t = Outer.Inner -> Inner
				final Type qualifier = newType(enclosingT, td);
				return ast.newQualifiedType(qualifier, ast.newSimpleName(t.getSimpleIdentifier()));
			}
		}
		return ast.newSimpleType(newTypeName(t, td));
	}

	/**
	 * New type name.
	 * 
	 * @param t
	 *            type
	 * @param td
	 *            type declaration (context)
	 * @return AST type name
	 */
	public static Name newTypeName(final T t, final TD td) {
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
	public static <E extends ASTNode> E setOp(final E node, final Op op) {
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
	public static <E extends Expression> E setValue(final E expression, final Object value) {
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
		if (expression.getParent() == null) {
			return expression;
		}
		return setOp(
				setValue((Expression) ASTNode.copySubtree(expression.getAST(), expression),
						getValue(expression)), getOp(expression));
	}

	private static Expression wrap(final Expression expression, final int priority) {
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
	public static Expression wrap(final Expression expression, final Priority priority) {
		return wrap(expression, priority.getPriority());
	}

	private Expressions() {
		// static helper class
	}

}