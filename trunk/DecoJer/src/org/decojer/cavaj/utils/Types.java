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

import java.util.ArrayList;
import java.util.logging.Logger;

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.types.ArrayT;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.model.types.ParamT.TypeArg;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.WildcardType;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Helper functions for Types.
 * 
 * @author André Pankraz
 */
public final class Types {

	private final static Logger LOGGER = Logger.getLogger(Types.class.getName());

	private static final String JAVA_LANG = "java.lang";

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
		final AST ast = td.getCu().getAst();
		if (t.isRef() /* incl. T.AREF */) {
			if (value == null) {
				return ast.newNullLiteral();
			}
			if (t.getName().equals(Class.class.getName())) {
				final TypeLiteral typeLiteral = ast.newTypeLiteral();
				typeLiteral.setType(decompileType((T) value, td));
				return typeLiteral;
			}
			if (t.getName().equals(String.class.getName())) {
				final StringLiteral stringLiteral = ast.newStringLiteral();
				stringLiteral.setLiteralValue((String) value);
				return stringLiteral;
			}
			LOGGER.warning("Unknown reference type '" + t + "'!");
			return ast.newNullLiteral();
		}
		// if (t.isMulti()) {
		// often not decideable, e.g. if byte or char
		// LOGGER.warning("Convert literal for multi-type '" + t + "'!");
		// }
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
		final ArrayList<String> names = Lists.newArrayList(t.getSimpleIdentifier());
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
					wildcardType.setBound(Types.decompileType(typeArg.getT(), td));
					parameterizedType.typeArguments().add(wildcardType);
					break;
				}
				case SUPER_OF: {
					final WildcardType wildcardType = ast.newWildcardType();
					wildcardType.setUpperBound(false);
					wildcardType.setBound(Types.decompileType(typeArg.getT(), td));
					parameterizedType.typeArguments().add(wildcardType);
					break;
				}
				default: {
					parameterizedType.typeArguments().add(Types.decompileType(typeArg.getT(), td));
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
		return ast.newSimpleType(Types.decompileName(t, td));
	}

	private Types() {
		// static helper class
	}

}