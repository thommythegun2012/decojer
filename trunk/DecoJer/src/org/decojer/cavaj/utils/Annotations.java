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

import static org.decojer.cavaj.utils.Expressions.newSimpleName;
import static org.decojer.cavaj.utils.Expressions.newType;
import static org.decojer.cavaj.utils.Expressions.newTypeName;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.types.T;
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
public final class Annotations {

	private final static Logger LOGGER = Logger.getLogger(Annotations.class.getName());

	/**
	 * Decompile Annotation.
	 * 
	 * @param t
	 *            Type Declaration
	 * @param a
	 *            Annotation
	 * @return Annotation AST Node
	 */
	private static Annotation decompileAnnotation(final T t, final A a) {
		final AST ast = t.getCu().getAst();
		final Set<Entry<String, Object>> members = a.getMembers();
		if (!members.isEmpty()) {
			if (members.size() == 1) {
				final Object memberValue = a.getMember("value");
				if (memberValue != null) {
					// a single member name "value=" is optional
					final SingleMemberAnnotation singleMemberAnnotation = ast
							.newSingleMemberAnnotation();
					singleMemberAnnotation.setTypeName(newTypeName(a.getT(), t));
					singleMemberAnnotation
							.setValue(decompileAnnotationDefaultValue(t, memberValue));
					return singleMemberAnnotation;
				}
			}
			final NormalAnnotation normalAnnotation = ast.newNormalAnnotation();
			normalAnnotation.setTypeName(newTypeName(a.getT(), t));
			for (final Entry<String, Object> member : members) {
				final Expression expression = decompileAnnotationDefaultValue(t, member.getValue());
				if (expression != null) {
					final MemberValuePair newMemberValuePair = ast.newMemberValuePair();
					newMemberValuePair.setName(newSimpleName(member.getKey(), ast));
					newMemberValuePair.setValue(expression);
					normalAnnotation.values().add(newMemberValuePair);
				}
			}
			if (normalAnnotation.values().size() > 0) {
				return normalAnnotation;
			}
		}
		final MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
		markerAnnotation.setTypeName(newTypeName(a.getT(), t));
		return markerAnnotation;
	}

	/**
	 * Decompile Annotation Default Value (value or Default Value literal).
	 * 
	 * @param t
	 *            Type Declaration
	 * @param defaultValue
	 *            Default Value
	 * @return Expression AST Node or {@code null}
	 */
	public static Expression decompileAnnotationDefaultValue(final T t, final Object defaultValue) {
		final AST ast = t.getCu().getAst();
		if (defaultValue == null) {
			return null;
		}
		if (defaultValue instanceof A) {
			return decompileAnnotation(t, (A) defaultValue);
		}
		// could be primitive array - use slow reflection
		if (defaultValue.getClass().isArray()) {
			final int size = Array.getLength(defaultValue);
			if (size == 1) {
				// single entry autoboxing
				return decompileAnnotationDefaultValue(t, Array.get(defaultValue, 0));
			}
			final ArrayInitializer arrayInitializer = ast.newArrayInitializer();
			for (int i = 0; i < size; ++i) {
				final Expression expression = decompileAnnotationDefaultValue(t,
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
			typeLiteral.setType(newType((T) defaultValue, t));
			return typeLiteral;
		}
		if (defaultValue instanceof Double) {
			return ast.newNumberLiteral(defaultValue.toString() + 'D');
		}
		if (defaultValue instanceof F) {
			final F f = (F) defaultValue;
			if (!f.isEnum()) {
				LOGGER.warning("Default value field must be enum!");
			}
			return ast.newQualifiedName(newTypeName(f.getT(), t), newSimpleName(f.getName(), ast));
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
	 * @param as
	 *            Annotations
	 * @param annotations
	 *            Annotation AST Nodes
	 * @param contextT
	 *            Type Declaration
	 */
	public static void decompileAnnotations(final A[] as, final List<Annotation> annotations,
			final T contextT) {
		if (as == null) {
			return;
		}
		for (final A a : as) {
			if (isRepeatable(a)) {
				for (final Object aa : (Object[]) a.getValueMember()) {
					annotations.add(decompileAnnotation(contextT, (A) aa));
				}
			} else {
				annotations.add(decompileAnnotation(contextT, a));
			}
		}
	}

	/**
	 * Decompile Annotations.
	 * 
	 * @param t
	 *            Annotated Type
	 * @param annotations
	 *            Annotation AST Nodes
	 * @param contextT
	 *            Type Declaration
	 */
	public static void decompileAnnotations(final T t, final List<Annotation> annotations,
			final T contextT) {
		if (t.isAnnotated()) {
			decompileAnnotations(t.getAs(), annotations, contextT);
		}
	}

	/**
	 * Do Annotations contain the Deprecated Annotation?
	 * 
	 * @param as
	 *            Annotations
	 * @return {@code true} - Annotations contain the Deprecated Annotation
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

	private static boolean isRepeatable(final A a) {
		if (a.getMembers().size() != 1) {
			// annotation declaration can have other members, but they must have defaults...this is
			// implicitely true because our code can only be compiled against such annotations with
			// only one value field (containing child annotations)
			return false;
		}
		final Object valueMember = a.getValueMember();
		if (!(valueMember instanceof Object[])) {
			return false;
		}
		T aT = null;
		for (final Object o : (Object[]) valueMember) {
			if (!(o instanceof A)) {
				return false;
			}
			final T t = ((A) o).getT();
			if (aT == null) {
				aT = t;
				continue;
			}
			if (!aT.equals(t)) {
				return false;
			}
		}
		if (aT == null) {
			return false;
		}
		if (!aT.isDeclaration()) {
			LOGGER.warning("Potential repeatable annotation '" + a
					+ "' with repeated annotation type '" + aT
					+ "' has not the necessary declaration information!");
		}
		final A[] aAs = aT.getAs();
		if (aAs == null) {
			return false;
		}
		for (final A aA : aAs) {
			if (aA.getT().getName().equals("java.lang.annotation.Repeatable")) {
				final Object value = aA.getValueMember();
				if (!(value instanceof T)) {
					return false;
				}
				if (((T) value).equals(a.getT())) {
					return true;
				}
			}
		}
		return false;
	}

	private Annotations() {
		// static helper class
	}

}