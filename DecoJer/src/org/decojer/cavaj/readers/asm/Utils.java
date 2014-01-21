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
import org.decojer.cavaj.model.types.AnnotT;
import org.objectweb.asm.TypePath;

/**
 * Utilities.
 * 
 * @author André Pankraz
 */
public class Utils {

	private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());

	public static T annotate(final T t, final A a, final TypePath typePath) {
		if (typePath == null) {
			return DU.getAnnotT(t, a);
		}
		T currentT = t;
		int innerCounter = 0;
		for (int i = 0; i < typePath.getLength(); ++i) {
			final boolean isLast = i == typePath.getLength() - 1;
			// JVMS: If the value of the type_path_kind item is 0, 1, or 2, then the value of the
			// type_argument_index item is 0.
			// If the value of the type_path_kind item is 3, then the value of
			// the type_argument_index item specifies which type argument of a
			// parameterized type is annotated, where 0 indicates the first type argument
			// of a parameterized type.
			final int step = typePath.getStep(i);
			final int arg = typePath.getStepArgument(i);
			// inner path is from front to end, but enclosings are from end to front (enclosings),
			// ParamT can be enclosings with own type annotation sub pathes
			if (innerCounter > 0 && step != TypePath.INNER_TYPE) {
				final T[] enclosingTs = t.getEnclosingTs();
				if (innerCounter >= enclosingTs.length) {
					LOGGER.warning("Not enough enclosings in '" + t + "' for '" + currentT
							+ "' for type annotation with path depth '" + innerCounter + "'!");
					break;
				}
				currentT = enclosingTs[innerCounter];
				innerCounter = 0;
			}
			// that we are here means, that we have to zoom into the modified type...so we can
			// unwrap the annotation type here
			if (currentT.isAnnotation()) {
				currentT = ((AnnotT) currentT).getRawT();
			}
			switch (step) {
			case TypePath.ARRAY_ELEMENT: {
				assert arg == 0;

				// TODO need a counter here too because the spec fails totally here in the ordering
				final T componentT = currentT.getComponentT();
				if (componentT == null) {
					LOGGER.warning("Not enough array components in '" + t + "' for '" + currentT
							+ "'  for type annotation path!");
					break;
				}
				if (!isLast) {
					currentT = componentT;
					continue;
				}
				currentT.setComponentT(DU.getAnnotT(componentT, a));
				break;
			}
			case TypePath.INNER_TYPE: {
				assert arg == 0;

				++innerCounter;
				if (!isLast) {
					continue;
				}
				final T[] enclosingTs = t.getEnclosingTs();
				if (innerCounter >= enclosingTs.length) {
					LOGGER.warning("Not enough enclosings in '" + t + "' for '" + currentT
							+ "' for type annotation with path depth '" + innerCounter + "'!");
					break;
				}
				// TODO hmmm, how do we change our parent type? recursive mode?
				DU.getAnnotT(enclosingTs[innerCounter], a);
				innerCounter = 0;
				break;
			}
			case TypePath.TYPE_ARGUMENT: {
				final T[] typeArgs = currentT.getTypeArgs();
				if (typeArgs == null || typeArgs.length <= arg) {
					LOGGER.warning("Not enough type arguments in '" + t + "' for '" + currentT
							+ "' for type annotation path with argument '" + arg + "'!");
					break;
				}
				final T typeArg = typeArgs[arg];
				if (!isLast) {
					currentT = typeArg;
					continue;
				}
				typeArgs[arg] = DU.getAnnotT(typeArg, a);
				break;
			}
			case TypePath.WILDCARD_BOUND: {
				assert arg == 0;

				final T bound = currentT.getBoundT();
				if (bound == null) {
					LOGGER.warning("No wildcard bound in '" + t + "' for '" + currentT
							+ "'  for type annotation path!");
					break;
				}
				if (!isLast) {
					currentT = bound;
					continue;
				}
				currentT.setBoundT(DU.getAnnotT(bound, a));
				break;
			}
			default:
				LOGGER.warning("Unknown step '0x" + Integer.toHexString(step) + "' in '" + t
						+ "' for '" + currentT + "' for type annotation path with argument '" + arg
						+ "'!");
			}
		}
		return t;
	}

	private Utils() {
		// nothing
	}

}