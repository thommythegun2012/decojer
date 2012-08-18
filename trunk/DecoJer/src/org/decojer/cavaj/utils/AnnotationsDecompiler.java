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

import java.lang.reflect.Array;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

/**
 * Annotations Decompiler.
 * 
 * @author André Pankraz
 */
public final class AnnotationsDecompiler {

	private final static Logger LOGGER = Logger.getLogger(AnnotationsDecompiler.class.getName());

	/**
	 * Decompile Annotation.
	 * 
	 * @param td
	 *            Type Declaration
	 * @param a
	 *            Annotation
	 * @return Annotation AST Node or {@code null}
	 */
	private static Annotation decompileAnnotation(final TD td, final A a) {
		final AST ast = td.getCu().getAst();
		if (a == null) {
			return null;
		}
		final Set<String> memberNames = a.getMemberNames();
		if (memberNames != null) {
			// a single member name "value=" is optional
			if (memberNames.size() == 1 && "value".equals(memberNames.iterator().next())) {
				final Expression expression = decompileAnnotationDefaultValue(td,
						a.getMemberValue("value"));
				if (expression != null) {
					final SingleMemberAnnotation singleMemberAnnotation = ast
							.newSingleMemberAnnotation();
					singleMemberAnnotation.setTypeName(td.newTypeName(a.getT()));
					singleMemberAnnotation.setValue(expression);
					return singleMemberAnnotation;
				}
			}
			final NormalAnnotation normalAnnotation = ast.newNormalAnnotation();
			normalAnnotation.setTypeName(td.newTypeName(a.getT()));
			for (final String memberName : memberNames) {
				final Expression expression = decompileAnnotationDefaultValue(td,
						a.getMemberValue(memberName));
				if (expression != null) {
					final MemberValuePair newMemberValuePair = ast.newMemberValuePair();
					newMemberValuePair.setName(ast.newSimpleName(memberName));
					newMemberValuePair.setValue(expression);
					normalAnnotation.values().add(newMemberValuePair);
				}
			}
			if (normalAnnotation.values().size() > 0) {
				return normalAnnotation;
			}
		}
		final MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
		markerAnnotation.setTypeName(td.newTypeName(a.getT()));
		return markerAnnotation;
	}

	/**
	 * Decompile Annotation Default Value (value or Default Value literal).
	 * 
	 * @param td
	 *            Type Declaration
	 * @param defaultValue
	 *            Default Value
	 * @return Expression AST Node or {@code null}
	 */
	public static Expression decompileAnnotationDefaultValue(final TD td, final Object defaultValue) {
		final AST ast = td.getCu().getAst();
		if (defaultValue == null) {
			return null;
		}
		if (defaultValue instanceof A) {
			return decompileAnnotation(td, (A) defaultValue);
		}
		// could be primitive array - use slow reflection
		if (defaultValue.getClass().isArray()) {
			final int size = Array.getLength(defaultValue);
			if (size == 1) {
				// single entry autoboxing
				return decompileAnnotationDefaultValue(td, Array.get(defaultValue, 0));
			}
			final ArrayInitializer arrayInitializer = ast.newArrayInitializer();
			for (int i = 0; i < size; ++i) {
				final Expression expression = decompileAnnotationDefaultValue(td,
						Array.get(defaultValue, i));
				if (expression != null) {
					arrayInitializer.expressions().add(expression);
				}
			}
			return arrayInitializer;
		}
		if (defaultValue instanceof Boolean) {
			return ast.newBooleanLiteral((Boolean) defaultValue);
		}
		if (defaultValue instanceof Byte) {
			return ast.newNumberLiteral(defaultValue.toString());
		}
		if (defaultValue instanceof Character) {
			final CharacterLiteral characterLiteral = ast.newCharacterLiteral();
			characterLiteral.setCharValue((Character) defaultValue);
			return characterLiteral;
		}
		if (defaultValue instanceof T) {
			final TypeLiteral typeLiteral = ast.newTypeLiteral();
			typeLiteral.setType(Types.convertType((T) defaultValue, td));
			return typeLiteral;
		}
		if (defaultValue instanceof Double) {
			return ast.newNumberLiteral(defaultValue.toString() + 'D');
		}
		if (defaultValue instanceof F) {
			final F f = (F) defaultValue;
			if (!f.check(AF.ENUM)) {
				LOGGER.warning("Default value field must be enum!");
			}
			return ast.newQualifiedName(td.newTypeName(f.getT()), ast.newSimpleName(f.getName()));
		}
		if (defaultValue instanceof Float) {
			return ast.newNumberLiteral(defaultValue.toString() + 'F');
		}
		if (defaultValue instanceof Integer) {
			return ast.newNumberLiteral(defaultValue.toString());
		}
		if (defaultValue instanceof Long) {
			return ast.newNumberLiteral(defaultValue.toString() + 'L');
		}
		if (defaultValue instanceof Short) {
			return ast.newNumberLiteral(defaultValue.toString());
		}
		if (defaultValue instanceof String) {
			final StringLiteral stringLiteral = ast.newStringLiteral();
			stringLiteral.setLiteralValue((String) defaultValue);
			return stringLiteral;
		}
		LOGGER.warning("Unknown member value type '" + defaultValue.getClass().getName() + "'!");
		final StringLiteral stringLiteral = ast.newStringLiteral();
		stringLiteral.setLiteralValue(defaultValue.toString());
		return stringLiteral;
	}

	/**
	 * Decompile Annotations.
	 * 
	 * @param td
	 *            Type Declaration
	 * @param modifiers
	 *            Annotation AST Nodes
	 * @param as
	 *            Annotations
	 */
	public static void decompileAnnotations(final TD td, final List<Annotation> modifiers,
			final A[] as) {
		if (as == null) {
			return;
		}
		for (final A a : as) {
			final Annotation decompileAnnotation = decompileAnnotation(td, a);
			if (decompileAnnotation != null) {
				modifiers.add(decompileAnnotation);
			}
		}
	}

	/**
	 * Do Annotations contain the Deprecated Annotation?
	 * 
	 * @param as
	 *            Annotations
	 * @return true - Annotations contain the Deprecated Annotation
	 */
	public static boolean isDeprecatedAnnotation(final A[] as) {
		if (as != null) {
			for (final A a : as) {
				if (a.getT().is(Deprecated.class)) {
					return true;
				}
			}
		}
		return false;
	}

	private AnnotationsDecompiler() {
		// static helper class
	}

}