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
package org.decojer.cavaj.readers.asm;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.methods.QualifiedM;
import org.decojer.cavaj.model.types.T;
import org.objectweb.asm.TypePath;

/**
 * ASM Utilities.
 * 
 * @author André Pankraz
 */
@Slf4j
public class ReadUtils {

	/**
	 * Annotate given method with annotation under consideration of the type path.
	 * 
	 * @param m
	 *            method
	 * @param a
	 *            annotation
	 * @param typePath
	 *            type path
	 * @return annotated method
	 */
	public static M annotateM(final M m, final A a, final TypePath typePath) {
		final T annotatedT = annotateT(m.getT(), a, typePath);
		if (m.getT() == annotatedT) {
			return m;
		}
		if (m instanceof QualifiedM) {
			m.setQualifierT(annotatedT);
			return m;
		}
		return new QualifiedM(annotatedT, m);
	}

	private static T annotatePart(final T t, final A a, final TypePath typePath, final int index) {
		if (typePath == null) {
			return DU.getAnnotatedT(t, a);
		}
		final int typePathLength = typePath.getLength();
		if (typePathLength == index) {
			return DU.getAnnotatedT(t, a);
		}
		assert index < typePathLength : "type path exceeded for: " + t;

		// JVMS: If the value of the type_path_kind item is 0, 1, or 2, then the value of the
		// type_argument_index item is 0.
		// If the value of the type_path_kind item is 3, then the value of
		// the type_argument_index item specifies which type argument of a
		// parameterized type is annotated, where 0 indicates the first type argument
		// of a parameterized type.

		final int step = typePath.getStep(index);
		final int arg = typePath.getStepArgument(index);

		// that we are here means, that we have to zoom into the modified type...so we can
		// unwrap the annotation type here
		switch (step) {
		case TypePath.ARRAY_ELEMENT: {
			assert arg == 0 : arg;

			// @C T @A [] @B [] f;
			// @A applies to the array type T[][], @B applies to its component type T[], and
			// @C applies to the final element type T.
			// -> Bytecode: T[][] and @A (NEW / VAR), @B (ARRAY), @C (ARRAY, ARRAY)

			// hence: type path step "ARRAY" can be interpreted as: component type,
			// @A applies to full type: Anno(T[][], @A) -> T @A[][]
			// Anno(Array(Anno(Array(Anno(T, @C), @B)), @A))

			final T componentT = t.getComponentT();
			if (componentT == null) {
				log.warn("Not enough array components in '" + t + "' for type annotation path!");
				break;
			}
			t.setComponentT(annotateT(componentT, a, typePath, index + 1));
			return t;
		}
		case TypePath.INNER_TYPE: {
			assert false : "type path step argument for INNER must be handled in calling function annotate()";
		}
		case TypePath.TYPE_ARGUMENT: {
			final T[] typeArgs = t.getTypeArgs();
			if (typeArgs == null || typeArgs.length <= arg) {
				log.warn("Not enough type arguments in '" + t
						+ "' for type annotation path with argument '" + arg + "'!");
				break;
			}
			final T typeArg = typeArgs[arg];
			typeArgs[arg] = annotateT(typeArg, a, typePath, index + 1);
			break;
		}
		case TypePath.WILDCARD_BOUND: {
			assert arg == 0 : arg;

			final T boundT = t.getBoundT();
			if (boundT == null) {
				log.warn("No wildcard bound in '" + t + "' for type annotation path!");
				break;
			}
			t.setBoundT(annotateT(boundT, a, typePath, index + 1));
			break;
		}
		default:
			log.warn("Unknown step '0x" + Integer.toHexString(step) + "' in '" + t
					+ "' for type annotation path with argument '" + arg + "'!");
		}
		return t;
	}

	/**
	 * Annotate given type with annotation under consideration of the type path.
	 * 
	 * @param t
	 *            type
	 * @param a
	 *            annotation
	 * @param typePath
	 *            type path
	 * @return annotated type
	 */
	public static T annotateT(final T t, final A a, final TypePath typePath) {
		return annotateT(t, a, typePath, 0);
	}

	private static T annotateT(final T t, final A a, final TypePath typePath, final int index) {
		int innerCounter = 0;
		if (typePath != null) {
			final int typePathLength = typePath.getLength();
			for (; index + innerCounter < typePathLength; ++innerCounter) {
				if (typePath.getStep(index + innerCounter) != TypePath.INNER_TYPE) {
					break;
				}
				assert typePath.getStepArgument(index + innerCounter) == 0 : "type path step argument for INNER must be 0";
			}
		}
		final T[] qualifierTs = t.getQualifierTs();
		if (innerCounter >= qualifierTs.length) {
			log.warn("Not enough qualifiers in '" + t + "' for type annotation with path depth '"
					+ innerCounter + "'!");
			return t;
		}
		if (innerCounter == qualifierTs.length - 1) {
			return annotatePart(qualifierTs[innerCounter], a, typePath, index + innerCounter);
		}
		return DU.getQualifiedT(
				annotatePart(qualifierTs[innerCounter], a, typePath, index + innerCounter), t);
	}

	private ReadUtils() {
		// nothing
	}

}