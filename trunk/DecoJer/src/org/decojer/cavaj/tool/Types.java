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
package org.decojer.cavaj.tool;

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

	private final static Logger LOGGER = Logger
			.getLogger(Types.class.getName());

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
	public static Expression convertLiteral(final T t, final Object value,
			final TD td, final AST ast) {
		if (t instanceof T.TT) {
			LOGGER.warning("Multi-type '" + t + "'!");
			final T[] ts = ((T.TT) t).getTs();
			return convertLiteral(ts[ts.length - 1], value, td, ast);
		}
		if (t == T.AREF) {
			if (value == null) {
				return ast.newNullLiteral();
			}
			LOGGER.warning("No not null aref constants possible!");
			return null;
		}
		if (t == T.BOOLEAN) {
			if (value instanceof Boolean) {
				return ast.newBooleanLiteral(((Boolean) value).booleanValue());
			} else {
				return ast.newBooleanLiteral(((Integer) value).intValue() != 0);
			}
		}
		if (t == T.BYTE) {
			return ast.newNumberLiteral(value.toString());
		}
		if (t == T.CHAR) {
			final CharacterLiteral characterLiteral = ast.newCharacterLiteral();
			characterLiteral.setCharValue((char) ((Integer) value).intValue());
			return characterLiteral;
		}
		if (t == T.DOUBLE) {
			return ast.newNumberLiteral(value.toString() + 'D');
		}
		if (t == T.FLOAT) {
			return ast.newNumberLiteral(value.toString() + 'F');
		}
		if (t == T.INT) {
			return ast.newNumberLiteral(value.toString());
		}
		if (t == T.LONG) {
			return ast.newNumberLiteral(value.toString() + 'L');
		}
		if (t == T.SHORT) {
			return ast.newNumberLiteral(value.toString());
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