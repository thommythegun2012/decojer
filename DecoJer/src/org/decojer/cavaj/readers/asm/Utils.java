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
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.types.AnnotT;
import org.decojer.cavaj.model.types.ArrayT;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.model.types.ParamT.TypeArg;
import org.objectweb.asm.TypePath;

/**
 * Utilities.
 * 
 * @author André Pankraz
 */
public class Utils {

	private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());

	public static AnnotT annotate(final T t, final A a) {
		if (!(t instanceof AnnotT)) {
			return new AnnotT(t, new A[] { a });
		}
		// don't change annotation array (changes name), recreate type
		final A[] oldAs = ((AnnotT) t).getAs();
		final A[] as = new A[oldAs.length + 1];
		System.arraycopy(oldAs, 0, as, 0, oldAs.length);
		as[oldAs.length] = a;
		return new AnnotT(((AnnotT) t).getRawT(), as);
	}

	public static T annotate(final T t, final A a, final TypePath typePath) {
		if (typePath == null) {
			return annotate(t, a);
		}
		T currentT = t;
		for (int i = 0; i < typePath.getLength(); ++i) {
			final boolean isLast = i == typePath.getLength() - 1;
			final int arg = typePath.getStepArgument(i);
			final int step = typePath.getStep(i);
			switch (step) {
			case TypePath.ARRAY_ELEMENT: {
				if (currentT instanceof AnnotT) {
					currentT = ((AnnotT) currentT).getRawT();
				}
				LOGGER.warning("TODO Annotate Array Type.");
				((ArrayT) currentT).getComponentT();
				break;
			}
			case TypePath.INNER_TYPE: {
				LOGGER.warning("TODO Annotate Inner type.");
				break;
			}
			case TypePath.TYPE_ARGUMENT: {
				if (currentT instanceof AnnotT) {
					currentT = ((AnnotT) currentT).getRawT();
				}
				final TypeArg[] typeArgs = ((ParamT) currentT).getTypeArgs();
				final TypeArg typeArg = typeArgs[arg];
				if (!isLast) {
					// TODO wrong, typeArg itself would be new context! must derive T? => ArgT!
					currentT = typeArg.getT();
					continue;
				}
				// TODO wrong, have to annotate typeArgs itself here, not the bound!
				// typeArgs[arg] = new TypeArg(annotate(typeArg.getT(), a), typeArg.getKind());
				break;
			}
			case TypePath.WILDCARD_BOUND: {
				if (currentT instanceof AnnotT) {
					currentT = ((AnnotT) currentT).getRawT();
				}
				final TypeArg[] typeArgs = ((ParamT) currentT).getTypeArgs();
				final TypeArg typeArg = typeArgs[arg];
				if (!isLast) {
					currentT = typeArg.getT();
					continue;
				}
				typeArgs[arg] = new TypeArg(annotate(typeArg.getT(), a), typeArg.getKind());
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