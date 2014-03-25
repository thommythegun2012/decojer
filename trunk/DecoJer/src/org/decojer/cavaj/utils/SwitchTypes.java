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

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.ASTORE;
import org.decojer.cavaj.model.code.ops.CmpType;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.INVOKE;
import org.decojer.cavaj.model.code.ops.JCND;
import org.decojer.cavaj.model.code.ops.LOAD;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.PUSH;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Static functions for decompiling switch types like switch(char|Enum|String).
 *
 * @author André Pankraz
 * @see TrCfg2JavaExpressionStmts
 */
public class SwitchTypes {

	/**
	 * Is used for string-switches. Execute switch case BB to create the case value map: string to
	 * BB.
	 *
	 * @param caseBb
	 *            case BB
	 * @param stringReg
	 *            string register
	 * @param hash
	 *            hash for string
	 * @param defaultCase
	 *            default case
	 * @param string2bb
	 *            case value map: string to BB
	 * @return {@code true} - success
	 */
	@SuppressWarnings("null")
	private static boolean executeBbStringHashCond(final BB caseBb, final int stringReg,
			final int hash, final BB defaultCase, final Map<String, BB> string2bb) {
		final Stack<Object> stack = new Stack<Object>();
		String str = null;
		for (int i = 0; i < caseBb.getOps(); ++i) {
			final Op op = caseBb.getOp(i);
			switch (op.getOptype()) {
			case LOAD:
				stack.push(((LOAD) op).getReg());
				break;
			case PUSH:
				stack.push(((PUSH) op).getValue());
				break;
			case INVOKE:
				final M m = ((INVOKE) op).getM();
				if (!"equals".equals(m.getName())
						|| !"(Ljava/lang/Object;)Z".equals(m.getDescriptor())) {
					return false;
				}
				final Object value = stack.pop();
				if (!(value instanceof String)) {
					return false;
				}
				if (value.hashCode() != hash) {
					return false;
				}
				final Object reg = stack.pop();
				if ((Integer) reg != stringReg) {
					return false;
				}
				stack.push(true);
				str = (String) value;
				break;
			case JCND:
				final Object equalsResult = stack.pop();
				if (!(equalsResult instanceof Boolean)) {
					return false;
				}
				boolean dir = ((Boolean) equalsResult).booleanValue();
				if (((JCND) op).getCmpType() == CmpType.T_EQ) {
					dir = !dir;
				}
				string2bb.put(str, dir ? caseBb.getTrueSucc() : caseBb.getFalseSucc());
				final E out = dir ? caseBb.getFalseOut() : caseBb.getTrueOut();
				if (out.getRelevantEnd() == defaultCase) {
					return true;
				}
				return executeBbStringHashCond(out.getEnd(), stringReg, hash, defaultCase,
						string2bb);
			default:
				return false;
			}
		}
		return false;
	}

	/**
	 * Is used for JDK-Bytecode mode string-switches. Execute switch case BB to create the case
	 * value map: index to string.
	 *
	 * @param caseBb
	 *            case BB
	 * @param indexReg
	 *            index register
	 * @param str
	 *            string
	 * @param defaultCase
	 *            default case
	 * @param index2string
	 *            case value map: index to string
	 * @return {@code true} - success
	 */
	private static boolean executeBbStringIndex(@Nonnull final BB caseBb, final int indexReg,
			@Nonnull final String str, @Nonnull final BB defaultCase,
			@Nonnull final Map<Integer, String> index2string) {
		assert defaultCase != null; // prevent warning for now, later check more

		final Stack<Object> stack = new Stack<Object>();
		for (int i = 0; i < caseBb.getOps(); ++i) {
			final Op op = caseBb.getOp(i);
			switch (op.getOptype()) {
			case PUSH:
				stack.push(((PUSH) op).getValue());
				break;
			case STORE:
				if (((STORE) op).getReg() != indexReg) {
					return false;
				}
				final Object index = stack.pop();
				if (!(index instanceof Integer)) {
					return false;
				}
				index2string.put((Integer) index, str);
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	/**
	 * Extract from bytecode for enumeration-switches the case value map: index to enum field.
	 *
	 * @param m
	 *            method containing the map.
	 * @param enumT
	 *            enum type for filtering
	 * @return case value map: index to enum field
	 */
	@Nonnull
	public static Map<Integer, F> extractIndex2enum(@Nonnull final M m, @Nonnull final T enumT) {
		// very simplistic matcher and may have false positives with obfuscated / strange bytecode,
		// works for JDK / Eclipse
		final Map<Integer, F> index2enums = Maps.newHashMap();
		final Op[] ops = m.getCfg().getOps();
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
			final M refM = ((INVOKE) op).getM();
			final T ownerT = refM.getT();
			if (ownerT == null || !ownerT.equals(enumT)) {
				continue;
			}
			if (!"ordinal".equals(refM.getName()) || !"()I".equals(refM.getDescriptor())) {
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
	 * Extract from bytecode for string-switches the case value map: index to string.
	 *
	 * @param string2bb
	 *            previously extracted map string to BB
	 * @param indexReg
	 *            index register
	 * @param defaultCase
	 *            default case
	 * @return case value map: index to string
	 */
	@Nullable
	public static Map<Integer, String> extractIndex2string(
			@Nonnull final Map<String, BB> string2bb, final int indexReg,
			@Nonnull final BB defaultCase) {
		final Map<Integer, String> index2string = Maps.newHashMap();
		for (final Map.Entry<String, BB> string2bbEntry : string2bb.entrySet()) {
			if (!executeBbStringIndex(string2bbEntry.getValue(), indexReg, string2bbEntry.getKey(),
					defaultCase, index2string)) {
				return null;
			}
		}
		return index2string;
	}

	/**
	 * Extract from bytecode for string-switches the case value map: hash to BB.
	 *
	 * @param switchHead
	 *            switch head
	 * @param stringReg
	 *            string register
	 * @param defaultCase
	 *            default case
	 * @return case value map: hash to BB
	 */
	@Nullable
	public static Map<String, BB> extractString2bb(@Nonnull final BB switchHead,
			final int stringReg, @Nonnull final BB defaultCase) {
		// remember string order: linked!
		final Map<String, BB> string2bb = Maps.newLinkedHashMap();
		for (final E out : switchHead.getOuts()) {
			if (!out.isSwitchCase()) {
				assert out.isCatch() : out; // just catch possible

				continue;
			}
			final Object[] values = (Object[]) out.getValue();
			assert values != null;
			assert values.length == 1 : values.length; // just one hash possible

			final Object value = values[0];
			if (value == null) {
				continue; // ignore default
			}
			if (!executeBbStringHashCond(out.getEnd(), stringReg, (Integer) value, defaultCase,
					string2bb)) {
				return null;
			}
		}
		return string2bb;
	}

	/**
	 * Rewrite string-switches from hash to value: Apply previously extracted case value maps to
	 * bytecode case edges.
	 *
	 * This works differently to {@link #rewriteCaseValues(BB, Map)} because strings could yield to
	 * same hashes and cases have to be restructered. It is more reasonable to add new case edges
	 * and delete all previous case edges.
	 *
	 * @param switchHead
	 *            switch head
	 * @param string2bb
	 *            case value map: string to BB
	 * @param defaultCase
	 *            default case
	 */
	public static void rewriteCaseStrings(@Nonnull final BB switchHead,
			@Nonnull final Map<String, BB> string2bb, @Nonnull final BB defaultCase) {
		// remember old switch case edges, delete later
		final List<E> outs = switchHead.getOuts();
		int i = outs.size();

		// build sorted map: unique case pc -> matching case keys
		final TreeMap<Integer, List<String>> casePc2values = Maps.newTreeMap();

		// add case branches
		for (final Map.Entry<String, BB> string2bbEntry : string2bb.entrySet()) {
			final int casePc = string2bbEntry.getValue().getPc();
			List<String> caseValues = casePc2values.get(casePc);
			if (caseValues == null) {
				caseValues = Lists.newArrayList();
				casePc2values.put(casePc, caseValues); // pc-sorted
			}
			caseValues.add(string2bbEntry.getKey());
		}
		// add default branch
		final int defaultPc = defaultCase.getPc();
		List<String> caseValues = casePc2values.get(defaultPc);
		if (caseValues == null) {
			caseValues = Lists.newArrayList();
			casePc2values.put(defaultPc, caseValues);
		}
		caseValues.add(null);

		// now add successors, preserve pc-order as edge-order
		for (final Map.Entry<Integer, List<String>> casePc2valuesEntry : casePc2values.entrySet()) {
			caseValues = casePc2valuesEntry.getValue();
			final String caseValue = caseValues.get(0);
			switchHead.addSwitchCase(caseValue == null ? defaultCase : string2bb.get(caseValue),
					caseValues.toArray(new Object[caseValues.size()]));
		}

		// delete all previous outgoing switch cases
		for (; i-- > 0;) {
			final E out = outs.get(i);
			if (out.isSwitchCase()) {
				out.remove();
			}
		}
	}

	/**
	 * Rewrite enumeration- or string-switches from index to value: Apply previously extracted case
	 * value maps to bytecode case edges.
	 *
	 * @param switchHead
	 *            switch head
	 * @param caseIndex2value
	 *            case value map: index to value (enum field or string)
	 * @return {@code true} - success
	 */
	public static boolean rewriteCaseValues(@Nonnull final BB switchHead,
			@Nonnull final Map<Integer, ?> caseIndex2value) {
		for (final E out : switchHead.getOuts()) {
			if (!out.isSwitchCase()) {
				continue;
			}
			final Object[] caseValues = (Object[]) out.getValue();
			assert caseValues != null;
			// check for all or nothing...
			for (final Object caseValue : caseValues) {
				if (!(caseValue instanceof Integer)) {
					assert caseValue == null; // default

					continue;
				}
				if (!caseIndex2value.containsKey(caseValue)) {
					// OK for defaults, e.g. JDK-Bytecode string-switches create none-reachable keys
					if (!out.isSwitchDefault()) {
						return false;
					}
				}
			}
		}
		for (final E out : switchHead.getOuts()) {
			if (!out.isSwitchCase()) {
				continue;
			}
			final Object[] caseValues = (Object[]) out.getValue();
			assert caseValues != null;
			for (int i = caseValues.length; i-- > 0;) {
				final Integer caseIndex = (Integer) caseValues[i];
				if (caseIndex == null) {
					continue;
				}
				final Object caseValue = caseIndex2value.get(caseIndex);
				// really handle this following? potential double defaults are also handled later
				if (caseValue == null) {
					if (caseValues.length == 1) {
						out.remove();
					}
					out.setValue(ArrayUtils.remove(caseValues, i));
					continue;
				}
				caseValues[i] = caseValue;
			}
		}
		return true;
	}
}