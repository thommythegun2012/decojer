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

import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

/**
 * Helper functions for types.
 * 
 * @author André Pankraz
 */
public class Types {

	private final static Logger LOGGER = Logger.getLogger(Types.class.getName());

	/**
	 * Convert literal.
	 * 
	 * @param t
	 *            literal type
	 * @param value
	 *            literal value
	 * @param td
	 *            type declaration context
	 * @param ast
	 *            abstract syntax tree
	 * @return Eclipse literal expression
	 */
	public static Expression convertLiteral(final T t, final Object value, final TD td,
			final AST ast) {
		if (t instanceof T.TT) {
			LOGGER.warning("Multi-type '" + t + "'!");
			final T[] ts = ((T.TT) t).getTs();
			return convertLiteral(ts[0], value, td, ast);
		}
		if (t.isReference() /* incl. T.AREF */) {
			if (value == null) {
				return ast.newNullLiteral();
			}
			if (t.getName().equals(Class.class.getName())) {
				final TypeLiteral typeLiteral = ast.newTypeLiteral();
				typeLiteral.setType(convertType((T) value, td, ast));
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
		if (t == T.BOOLEAN) {
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
					: value != null);
		}
		if (t == T.BYTE) {
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
		if (t == T.CHAR) {
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
		if (t == T.DOUBLE) {
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
		if (t == T.FLOAT) {
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
		if (t == T.INT) {
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
		if (t == T.LONG) {
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
		if (t == T.SHORT) {
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
		LOGGER.warning("Unknown data type '" + t + "'!");
		final StringLiteral stringLiteral = ast.newStringLiteral();
		stringLiteral.setLiteralValue(value.toString());
		return stringLiteral;
	}

	/**
	 * Convert type.
	 * 
	 * @param t
	 *            type
	 * @param td
	 *            type declaration context
	 * @param ast
	 *            abstract syntax tree
	 * @return Eclipse type
	 */
	public static Type convertType(final T t, final TD td, final AST ast) {
		Type type;
		final T baseT = t.getBaseT();
		if (baseT == T.VOID) {
			type = ast.newPrimitiveType(PrimitiveType.VOID);
		} else if (baseT == T.BYTE) {
			type = ast.newPrimitiveType(PrimitiveType.BYTE);
		} else if (baseT == T.CHAR) {
			type = ast.newPrimitiveType(PrimitiveType.CHAR);
		} else if (baseT == T.DOUBLE) {
			type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
		} else if (baseT == T.FLOAT) {
			type = ast.newPrimitiveType(PrimitiveType.FLOAT);
		} else if (baseT == T.INT) {
			type = ast.newPrimitiveType(PrimitiveType.INT);
		} else if (baseT == T.LONG) {
			type = ast.newPrimitiveType(PrimitiveType.LONG);
		} else if (baseT == T.SHORT) {
			type = ast.newPrimitiveType(PrimitiveType.SHORT);
		} else if (baseT == T.BOOLEAN) {
			type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		} else {
			type = ast.newSimpleType(td.newTypeName(baseT.getName()));
		}
		if (t.isArray()) {
			for (int i = t.getDim(); i-- > 0;) {
				type = ast.newArrayType(type);
			}
		}
		return type;
	}

}