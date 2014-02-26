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

import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.Getter;

import org.decojer.cavaj.model.code.CFG;

/**
 * Transformer: Dalvik Remove Temporary Registers.
 * 
 * @author André Pankraz
 */
public final class TrDalvikRemoveTempRegs {

	private final static Logger LOGGER = Logger.getLogger(TrDalvikRemoveTempRegs.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		if (!cfg.getT().isDalvik()) {
			return;
		}
		new TrDalvikRemoveTempRegs(cfg).transform();
	}

	@Getter(value = AccessLevel.PRIVATE)
	private final CFG cfg;

	private TrDalvikRemoveTempRegs(final CFG cfg) {
		this.cfg = cfg;
	}

	private void transform() {
		LOGGER.info("transform: " + getCfg());
		/*
		 * TODO ZIP ZAP: PUSH v1 STORE r1 ... PUSH vn STORE rn, LOAD v1 ... LOAD vn, INVOKE => kill
		 * STORES and LOADS PUSH 0 STORE r0
		 */
		/*
		 * TODO PUSH 1 STORE r1 (final never rewritten) => direct constant replacement
		 */
	}

}