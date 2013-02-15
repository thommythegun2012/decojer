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

import java.util.Map;

import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.ASTORE;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.INVOKE;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.PUSH;
import org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts;

import com.google.common.collect.Maps;

/**
 * Static functions for decompiling switch types like switch(char|Enum|String).
 * 
 * @author André Pankraz
 * @see TrCfg2JavaExpressionStmts
 */
public class SwitchTypes {

	/**
	 * Extract from bytecode for enumeration-switches the case value map: index to enum field.
	 * 
	 * @param md
	 *            method containing the map.
	 * @param enumT
	 *            enum type for filtering
	 * @return case value map: index to enum field
	 */
	public static Map<Integer, F> extractIndex2Enum(final MD md, final T enumT) {
		// very simplistic matcher and may have false positives with obfuscated / strange bytecode,
		// works for JDK / Eclipse
		final Map<Integer, F> index2enums = Maps.newHashMap();
		final Op[] ops = md.getCfg().getOps();
		final int length = ops.length - 3;
		for (int i = 0; i < length; ++i) {
			Op op = ops[i];
			if (!(op instanceof GET)) {
				continue;
			}
			final F f = ((GET) op).getF();
			if (!f.getT().equals(enumT)) {
				continue;
			}
			op = ops[i + 1];
			if (!(op instanceof INVOKE)) {
				continue;
			}
			final M m = ((INVOKE) op).getM();
			if (!m.getT().equals(enumT)) {
				continue;
			}
			if (!"ordinal".equals(m.getName()) || !"()I".equals(m.getDescriptor())) {
				continue;
			}
			op = ops[i + 2];
			if (!(op instanceof PUSH)) {
				continue;
			}
			final Object value = ((PUSH) op).getValue();
			if (!(value instanceof Integer)) {
				continue;
			}
			op = ops[i + 3];
			if (!(op instanceof ASTORE)) {
				continue;
			}
			index2enums.put((Integer) value, f);
			i += 3;
		}
		return index2enums;
	}

	/**
	 * Rewrite enumeration- or string-switches: Apply previously extracted case value maps to
	 * bytecode case edges.
	 * 
	 * @param bb
	 *            BB
	 * @param index2enum
	 *            case value map: index to value (enum field or string)
	 * @return {@code true} - success
	 */
	public static boolean rewriteCaseValues(final BB bb, final Map<Integer, ?> index2enum) {
		for (final E out : bb.getOuts()) {
			if (!out.isSwitchCase()) {
				continue;
			}
			// check for all or nothing...
			for (final Object caseValue : (Object[]) out.getValue()) {
				if (!(caseValue instanceof Integer)) {
					assert caseValue == null; // default

					continue;
				}
				if (!index2enum.containsKey(caseValue)) {
					return false;
				}
			}
		}
		for (final E out : bb.getOuts()) {
			if (!out.isSwitchCase()) {
				continue;
			}
			final Object[] caseValues = (Object[]) out.getValue();
			for (int i = caseValues.length; i-- > 0;) {
				final Integer caseValue = (Integer) caseValues[i];
				if (caseValue != null) {
					caseValues[i] = index2enum.get(caseValue);
				}
			}
		}
		return true;
	}

}