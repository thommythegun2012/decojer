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

import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;
import org.objectweb.asm.TypePath;

/**
 * ASM Utilities.
 * 
 * @author André Pankraz
 */
public class ReadUtils {

	private final static Logger LOGGER = Logger.getLogger(ReadUtils.class.getName());

	private static T annotate(final T t, final A a) {
		// flip inner type annotations, look for outmost enclosing
		T findT = t;
		for (T enclosingT = findT.getEnclosingT(); enclosingT != null; enclosingT = findT
				.getEnclosingT()) {
			findT = enclosingT;
		}
		return DU.getAnnotatedT(findT, a);
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
	public static T annotate(final T t, final A a, final TypePath typePath) {
		return annotate(t, a, typePath, 0);
	}

	private static T annotate(final T t, final A a, final TypePath typePath, final int index) {
		if (typePath == null) {
			return annotate(t, a);
		}
		final int typePathLength = typePath.getLength();
		if (typePathLength == index) {
			return annotate(t, a);
		}
		if (typePathLength < index) {
			LOGGER.warning("Type path exceeded for '" + t + "'!");
			return t;
		}

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
				LOGGER.warning("Not enough array components in '" + t
						+ "' for type annotation path!");
				break;
			}
			t.setComponentT(annotate(componentT, a, typePath, index + 1));
			return t;
		}
		case TypePath.INNER_TYPE: {
			assert arg == 0 : arg;

			// @A T0.@B T1.@C T2;
			// -> Bytecode: T0.T1.T2 and @A (NEW / VAR), @B (INNER), @C (INNER, INNER)

			// hence: type path step "INNER" can be interpreted from front to end,
			// @C applies to full type: Anno(T0.T1.T2, @A) -> package.@A T0.T1.T2
			// we have to flip this strange behaviour into:
			// Anno(Qual(Anno(Qual(Anno(T0, @A), T1), @B), T2), @C)

			int innerCounter = 1;
			for (; index + innerCounter < typePathLength; ++innerCounter) {
				if (typePath.getStep(index + innerCounter) != TypePath.INNER_TYPE) {
					break;
				}
			}
			final T[] qualifierTs = t.getQualifierTs();
			if (innerCounter >= qualifierTs.length) {
				LOGGER.warning("Not enough qualifiers in '" + t
						+ "' for type annotation with path depth '" + innerCounter + "'!");
				break;
			}
			if (index + innerCounter == typePathLength) {
				// flip inner type annotations: don't annotate outmost enclosing here
				if (innerCounter == qualifierTs.length - 1) {
					return DU.getAnnotatedT(qualifierTs[innerCounter], a);
				}
				return DU.getQualifiedT(DU.getAnnotatedT(qualifierTs[innerCounter], a), t);
			}
			if (innerCounter == qualifierTs.length - 1) {
				return annotate(qualifierTs[innerCounter], a, typePath, index + innerCounter);
			}
			return DU.getQualifiedT(
					annotate(qualifierTs[innerCounter], a, typePath, index + innerCounter), t);
		}
		case TypePath.TYPE_ARGUMENT: {
			final T[] typeArgs = t.getTypeArgs();
			if (typeArgs == null || typeArgs.length <= arg) {
				LOGGER.warning("Not enough type arguments in '" + t
						+ "' for type annotation path with argument '" + arg + "'!");
				break;
			}
			final T typeArg = typeArgs[arg];
			typeArgs[arg] = annotate(typeArg, a, typePath, index + 1);
			break;
		}
		case TypePath.WILDCARD_BOUND: {
			assert arg == 0 : arg;

			final T boundT = t.getBoundT();
			if (boundT == null) {
				LOGGER.warning("No wildcard bound in '" + t + "' for type annotation path!");
				break;
			}
			t.setBoundT(annotate(boundT, a, typePath, index + 1));
			break;
		}
		default:
			LOGGER.warning("Unknown step '0x" + Integer.toHexString(step) + "' in '" + t
					+ "' for type annotation path with argument '" + arg + "'!");
		}
		return t;
	}

	private ReadUtils() {
		// nothing
	}

}