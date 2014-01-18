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
		for (int i = 0; i < typePath.getLength(); ++i) {
			final boolean isLast = i == typePath.getLength() - 1;
			// JVMS: If the value of the type_path_kind item is 0, 1, or 2, then the value of the
			// type_argument_index item is 0.
			// If the value of the type_path_kind item is 3, then the value of
			// the type_argument_index item specifies which type argument of a
			// parameterized type is annotated, where 0 indicates the first type argument
			// of a parameterized type.
			final int arg = typePath.getStepArgument(i);
			final int step = typePath.getStep(i);
			switch (step) {
			case TypePath.ARRAY_ELEMENT: {
				assert arg == 0;

				if (currentT.isAnnotation()) {
					currentT = ((AnnotT) currentT).getRawT();
				}
				final T componentT = currentT.getComponentT();
				if (!isLast) {
					currentT = componentT;
					continue;
				}
				currentT.setComponentT(DU.getAnnotT(componentT, a));
				break;
			}
			case TypePath.INNER_TYPE: {
				assert arg == 0;

				LOGGER.warning("TODO Annotate Inner type.");
				break;
			}
			case TypePath.TYPE_ARGUMENT: {
				if (currentT.isAnnotation()) {
					currentT = ((AnnotT) currentT).getRawT();
				}
				final T[] typeArgs = currentT.getTypeArgs();
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

				if (currentT.isAnnotation()) {
					currentT = ((AnnotT) currentT).getRawT();
				}
				final T bound = currentT.getBoundT();
				assert bound != null;

				if (!isLast) {
					currentT = bound;
					continue;
				}
				currentT.setBoundT(DU.getAnnotT(bound, a));
				break;
			}
			default:
				LOGGER.warning("Unknown type path step: 0x" + Integer.toHexString(step) + " : "
						+ typePath.getStepArgument(i));
			}
		}
		return t;
	}

	private Utils() {
		// nothing
	}

}