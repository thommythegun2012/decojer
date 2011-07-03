/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.decojer.cavaj.model.TD;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

/**
 * Annotations decompiler.
 * 
 * @author Andre Pankraz
 */
public class AnnotationsDecompiler {

	private final static Logger LOGGER = Logger
			.getLogger(AnnotationsDecompiler.class.getName());

	/**
	 * Decompile annotation.
	 * 
	 * @param td
	 *            type declaration
	 * @param annotation
	 *            annotation
	 * @return annotation node or null
	 */
	@SuppressWarnings("unchecked")
	public static org.eclipse.jdt.core.dom.Annotation decompileAnnotation(
			final TD td, final Annotation annotation) {
		final AST ast = td.getCu().getAst();
		if (annotation == null) {
			return null;
		}
		final Set<String> memberNames = annotation.getMemberNames();
		if (memberNames != null) {
			// a single member name "value=" is optional
			if (memberNames.size() == 1
					&& "value".equals(memberNames.iterator().next())) {
				final Expression expression = decompileAnnotationMemberValue(
						td, annotation.getMemberValue("value"));
				if (expression != null) {
					final SingleMemberAnnotation newSingleMemberAnnotation = ast
							.newSingleMemberAnnotation();
					newSingleMemberAnnotation.setTypeName(ast
							.newName(annotation.getTypeName()));
					newSingleMemberAnnotation.setValue(expression);
					return newSingleMemberAnnotation;
				}
			}
			final NormalAnnotation newNormalAnnotation = ast
					.newNormalAnnotation();
			newNormalAnnotation.setTypeName(ast.newName(annotation
					.getTypeName()));
			for (final String memberName : memberNames) {
				final Expression expression = decompileAnnotationMemberValue(
						td, annotation.getMemberValue(memberName));
				if (expression != null) {
					final MemberValuePair newMemberValuePair = ast
							.newMemberValuePair();
					newMemberValuePair.setName(ast.newSimpleName(memberName));
					newMemberValuePair.setValue(expression);
					newNormalAnnotation.values().add(newMemberValuePair);
				}
			}
			if (newNormalAnnotation.values().size() > 0) {
				return newNormalAnnotation;
			}
		}
		final MarkerAnnotation newMarkerAnnotation = ast.newMarkerAnnotation();
		newMarkerAnnotation.setTypeName(ast.newName(annotation.getTypeName()));
		return newMarkerAnnotation;
	}

	/**
	 * Decompile annotation member value (value or default value literal).
	 * 
	 * @param td
	 *            type declaration
	 * @param memberValue
	 *            member value
	 * @return expression node or null
	 */
	@SuppressWarnings("unchecked")
	public static Expression decompileAnnotationMemberValue(final TD td,
			final MemberValue memberValue) {
		final AST ast = td.getCu().getAst();
		if (memberValue == null) {
			return null;
		}
		if (memberValue instanceof AnnotationMemberValue) {
			return decompileAnnotation(td,
					((AnnotationMemberValue) memberValue).getValue());
		}
		if (memberValue instanceof ArrayMemberValue) {
			final ArrayInitializer arrayInitializer = ast.newArrayInitializer();
			for (final MemberValue arrayMemberValue : ((ArrayMemberValue) memberValue)
					.getValue()) {
				final Expression expression = decompileAnnotationMemberValue(
						td, arrayMemberValue);
				if (expression != null) {
					arrayInitializer.expressions().add(expression);
				}
			}
			return arrayInitializer;
		}
		if (memberValue instanceof BooleanMemberValue) {
			return ast.newBooleanLiteral(((BooleanMemberValue) memberValue)
					.getValue());
		}
		if (memberValue instanceof ByteMemberValue) {
			return ast.newNumberLiteral(Byte
					.toString(((ByteMemberValue) memberValue).getValue()));
		}
		if (memberValue instanceof CharMemberValue) {
			final CharacterLiteral characterLiteral = ast.newCharacterLiteral();
			characterLiteral.setCharValue(((CharMemberValue) memberValue)
					.getValue());
			return characterLiteral;
		}
		if (memberValue instanceof ClassMemberValue) {
			final TypeLiteral typeLiteral = ast.newTypeLiteral();
			final String value = ((ClassMemberValue) memberValue).getValue();
			// value: byte, java.util.List, java.lang.Byte[][][], void
			// no type arguments possible
			// => PrimitiveType, SimpleType, ArrayType
			final int pos = value.indexOf('[');
			final String name = pos == -1 ? value : value.substring(0, pos);
			Type type;
			// similar to switch in SignatureDecompiler
			if ("void".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.VOID);
			} else if ("byte".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.BYTE);
			} else if ("char".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.CHAR);
			} else if ("double".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
			} else if ("float".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.FLOAT);
			} else if ("int".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.INT);
			} else if ("long".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.LONG);
			} else if ("short".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.SHORT);
			} else if ("boolean".equals(name)) {
				type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
			} else {
				type = ast.newSimpleType(td.newTypeName(name));
			}
			if (pos > 0) {
				// array number is "length" - "pos of first [" / 2
				for (int i = (value.length() - pos) / 2; i-- > 0;) {
					type = ast.newArrayType(type);
				}
			}
			typeLiteral.setType(type);
			return typeLiteral;
		}
		if (memberValue instanceof DoubleMemberValue) {
			return ast.newNumberLiteral(Double
					.toString(((DoubleMemberValue) memberValue).getValue())
					+ "D");
		}
		if (memberValue instanceof EnumMemberValue) {
			return ast.newName(((EnumMemberValue) memberValue).getType() + "."
					+ ((EnumMemberValue) memberValue).getValue());
			// TODO default java.lang.Thread$State.BLOCKED
		}
		if (memberValue instanceof FloatMemberValue) {
			return ast.newNumberLiteral(Double
					.toString(((FloatMemberValue) memberValue).getValue())
					+ "F");
		}
		if (memberValue instanceof IntegerMemberValue) {
			return ast.newNumberLiteral(Integer
					.toString(((IntegerMemberValue) memberValue).getValue()));
		}
		if (memberValue instanceof LongMemberValue) {
			return ast
					.newNumberLiteral(Long
							.toString(((LongMemberValue) memberValue)
									.getValue())
							+ "L");
		}
		if (memberValue instanceof ShortMemberValue) {
			return ast.newNumberLiteral(Integer
					.toString(((ShortMemberValue) memberValue).getValue()));
		}
		if (memberValue instanceof StringMemberValue) {
			final StringLiteral stringLiteral = ast.newStringLiteral();
			stringLiteral.setLiteralValue(((StringMemberValue) memberValue)
					.getValue());
			return stringLiteral;
		}
		LOGGER.log(Level.WARNING, "Unknown member value type '"
				+ memberValue.getClass().getName() + "'!");
		final StringLiteral stringLiteral = ast.newStringLiteral();
		stringLiteral.setLiteralValue(memberValue.toString());
		return stringLiteral;
	}

	/**
	 * Decompile annotations.
	 * 
	 * @param td
	 *            type declaration
	 * @param modifiers
	 *            modifier nodes
	 * @param annotations
	 *            annotations
	 */
	public static void decompileAnnotations(final TD td,
			final List<org.eclipse.jdt.core.dom.Annotation> modifiers,
			final Annotation[] annotations) {
		if (annotations == null) {
			return;
		}
		for (final Annotation annotation : annotations) {
			final org.eclipse.jdt.core.dom.Annotation decompileAnnotation = decompileAnnotation(
					td, annotation);
			if (decompileAnnotation != null) {
				modifiers.add(decompileAnnotation);
			}
		}
	}

	/**
	 * Gets statement if annotations contain the deprecated annotation.
	 * 
	 * @param annotationsAttribute
	 *            annotations attribute
	 * @return true - annotations contain deprecated annotation.
	 */
	public static boolean isDeprecatedAnnotation(
			final AnnotationsAttribute annotationsAttribute) {
		if (annotationsAttribute != null) {
			for (final Annotation annotation : annotationsAttribute
					.getAnnotations()) {
				if ("java.lang.Deprecated".equals(annotation.getTypeName())) {
					return true;
				}
			}
		}
		return false;
	}

}