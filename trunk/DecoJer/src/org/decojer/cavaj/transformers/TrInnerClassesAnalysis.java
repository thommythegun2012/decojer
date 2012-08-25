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
package org.decojer.cavaj.transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.D;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.ops.NEW;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.types.ClassT;

/**
 * Transformer: Analyze inner classes and create compilation units.
 * 
 * @author André Pankraz
 */
public class TrInnerClassesAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrInnerClassesAnalysis.class.getName());

	private static void checkBinaryCompatibilityNamingRules(final Collection<T> ts) {
		for (final T t : ts) {
			final TD td = t.getTd();
			if (td == null) {
				continue;
			}
			// Inner name is not necessary anymore since JVM 5, see T#getInnerName(), but we
			// validate the new "Binary Compatibility" rules here.
			if (td.getVersion() < 48 || t.getEnclosingT() == null) {
				continue;
			}
			final String innerName = t.getInnerName();
			final String simpleName = getSimpleClassName(t);
			if (innerName == null && !simpleName.isEmpty() || innerName != null
					&& !innerName.equals(simpleName)) {
				// TODO check oracle.net.aso.m in obfuscated
				// .m2\repository\com\oracle\ojdbc6\11.2.0.1.0\ojdbc6-11.2.0.1.0.jar
				// should be a local class with name "m" in constructor?
				LOGGER.warning("Inner name '" + innerName + "' for type '" + t
						+ "' is different from enclosing info '" + simpleName + "'!");
			}
		}
	}

	/**
	 * All JVMs < 5 have no enclosing method attribute and wrong (JVM 1) or missing (JVM 2...4)
	 * informations. We are looking for explicit new-ops, this must be the parent method.
	 * 
	 * @param ts
	 *            all types
	 */
	private static void findEnclosingMethods(final Collection<T> ts) {
		for (final T t : ts) {
			final TD td = t.getTd();
			if (td == null) {
				continue;
			}
			for (final BD bd : td.getBds()) {
				if (!(bd instanceof MD)) {
					continue;
				}
				final MD enclosingMd = (MD) bd;
				final CFG cfg = enclosingMd.getCfg();
				if (cfg == null) {
					continue;
				}
				final Op[] ops = cfg.getOps();
				if (ops == null) {
					continue;
				}
				for (final Op op : ops) {
					if (!(op instanceof NEW)) {
						continue;
					}
					final T newT = ((NEW) op).getT();
					if (!newT.isAnonymous()) {
						continue;
					}
					final TD newTd = newT.getTd();
					if (newTd == null) {
						continue;
					}
					final M enclosingM = newT.getEnclosingM();
					if (enclosingM != null) {
						// TODO check if equal
					}
					final T enclosingT = newT.getEnclosingT();
					if (enclosingT != null) {
						// TODO check if equal
					}
					final D newParent = newTd.getParent();
					if (newParent != null) {
						// TODO can happen for each constructor if this is a field value!!!
						if (newParent instanceof MD && ((MD) newParent).isConstructor()) {
							// TODO should link to MDs parent, but might not be linked yet???
							// parallel findTopTds necessary?
							continue;
						}
						LOGGER.warning("New ananymous type declaration '" + newTd
								+ "' already has parent '" + newParent + "'!");
						continue;
					}
					enclosingMd.addTd(newTd);
				}
			}
		}
	}

	private static List<TD> findTopTds(final Collection<T> ts) {
		final List<TD> tds = new ArrayList<TD>();
		// separate all read tds, not just selected tds
		for (final T t : ts) {
			final TD td = t.getTd();
			if (td == null) {
				continue;
			}
			if (td.isAnonymous()) {
				if (td.getParent() != null) {
					if (!(td.getParent() instanceof MD)) {
						LOGGER.warning("Parent of inner local/anonymous type '" + t
								+ "' is no method but '" + td.getParent() + "'!");
					}
					continue;
				}
				if (isEnumSwitchMap(td)) {
					// use enclosingT info, should exist
					final ClassT enclosingT = t.getEnclosingT();
					if (enclosingT != null) {
						final TD enclosingTd = enclosingT.getTd();
						if (enclosingTd != null) {
							enclosingTd.addTd(td);
							continue;
						}
					}
					LOGGER.warning("No enclosing type info for inner class with Enum Switch Map '"
							+ t + "'!");
				}
				// use existing enclosing info
			}
			// first check enclosing method, potentially deeper nested than in type
			final M enclosingM = t.getEnclosingM();
			if (enclosingM != null) {
				final MD enclosingMd = enclosingM.getMd();
				if (enclosingMd != null) {
					enclosingMd.addTd(td);
					continue;
				}
			}
			final ClassT enclosingT = t.getEnclosingT();
			if (enclosingT != null) {
				final TD enclosingTd = enclosingT.getTd();
				if (enclosingTd != null) {
					enclosingTd.addTd(td);
					continue;
				}
			}
			tds.add(td);
		}
		return tds;
	}

	/**
	 * Returns the "simple binary name" of the underlying class, i.e., the binary name without the
	 * leading enclosing class name. Returns {@code null} if the underlying class is a top level
	 * class.
	 * 
	 * Works just for JVM >= 5.
	 * 
	 * @param t
	 *            type
	 * @return simple binary name
	 * @since 1.5
	 * @see Class#getSimpleName()
	 */
	private static String getSimpleBinaryName(final T t) {
		final T enclosingT = t.getEnclosingT();
		if (enclosingT == null) {
			return null;
		}
		if (!t.getName().startsWith(enclosingT.getName())) {
			return null;
		}
		return t.getName().substring(enclosingT.getName().length());
	}

	/**
	 * Get simple name, like appearing in Java source code.
	 * 
	 * Works just for JVM >= 5.
	 * 
	 * @param t
	 *            type
	 * @return simple name
	 * @since 1.5
	 * @see Class#getSimpleName()
	 */
	private static String getSimpleClassName(final T t) {
		final String simpleName = getSimpleBinaryName(t);
		if (simpleName == null) { // is top level class
			return t.getPName();
		}
		// According to JLS3 "Binary Compatibility" (13.1) the binary
		// name of non-package classes (not top level) is the binary
		// name of the immediately enclosing class followed by a '$' followed by:
		// (for nested and inner classes): the simple name.
		// (for local classes): 1 or more digits followed by the simple name.
		// (for anonymous classes): 1 or more digits.

		// Since getSimpleBinaryName() will strip the binary name of
		// the immediatly enclosing class, we are now looking at a
		// string that matches the regular expression "\$[0-9]*"
		// followed by a simple name (considering the simple of an
		// anonymous class to be the empty string).

		// Remove leading "\$[0-9]*" from the name
		final int length = simpleName.length();
		if (length < 1 || simpleName.charAt(0) != '$') {
			throw new InternalError("Malformed class name");
		}
		int index = 1;
		while (index < length && isAsciiDigit(simpleName.charAt(index))) {
			index++;
		}
		// Eventually, this is the empty string iff this is an anonymous class
		return simpleName.substring(index);
	}

	private static String getSourceId(final TD mainTd) {
		final String sourceFileName = mainTd.getSourceFileName();
		if (sourceFileName == null) {
			return null;
		}
		final String packageName = mainTd.getPackageName();
		return packageName == null ? sourceFileName : packageName + "." + sourceFileName;
	}

	/**
	 * Character.isDigit answers {@code true} to some non-ascii digits. This one does not.
	 * 
	 * @param c
	 *            character
	 * @return {@code true} - is ascii digit
	 */
	private static boolean isAsciiDigit(final char c) {
		return '0' <= c && c <= '9';
	}

	/**
	 * Enum switches use static inner with static cached map, use enclosingT info.
	 * 
	 * @param td
	 *            type declaration
	 * @return {@code true} - is enum switch mal inner
	 */
	private static boolean isEnumSwitchMap(final TD td) {
		for (final BD bd : td.getBds()) {
			if (!(bd instanceof FD)) {
				continue;
			}
			final FD fd = (FD) bd;
			if (!fd.check(AF.STATIC)) {
				continue;
			}
			if (!fd.getName().startsWith("$SwitchMap$")) {
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * Transform decompilation unit.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public static void transform(final DU du) {
		final Collection<T> ts = du.getTs();

		checkBinaryCompatibilityNamingRules(ts);
		findEnclosingMethods(ts);
		final List<TD> topTds = findTopTds(ts);

		final List<CU> cus = new ArrayList<CU>();
		final Map<String, CU> sourceId2cu = new HashMap<String, CU>();
		for (final TD topTd : topTds) {
			final String sourceId = getSourceId(topTd);
			if (sourceId != null) {
				final CU cu = sourceId2cu.get(sourceId);
				if (cu != null) {
					cu.addTd(topTd);
					continue;
				}
			}
			final String sourceFileName = sourceId != null ? topTd.getSourceFileName() : topTd
					.getPName() + ".java";
			final CU cu = new CU(topTd, sourceFileName);
			if (sourceId != null) {
				sourceId2cu.put(sourceId, cu);
			}
			cus.add(cu);
		}
		// not very optimized...but it works for now...
		final List<CU> selectedCus = new ArrayList<CU>();
		for (final CU cu : cus) {
			for (final TD td : cu.getAllTds()) {
				if (du.getSelectedTds().contains(td)) {
					selectedCus.add(cu);
					break;
				}
			}
		}
		Collections.sort(selectedCus, new Comparator<CU>() {

			@Override
			public int compare(final CU cu1, final CU cu2) {
				return cu1.getName().compareTo(cu2.getName());
			}

		});
		du.setCus(selectedCus);
	}

}