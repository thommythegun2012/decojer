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

import java.util.List;
import java.util.Set;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Transformer: Calculate postorder for basic blocks of CFG.
 * 
 * @author André Pankraz
 */
public final class TrCalculatePostorder {

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrCalculatePostorder(cfg).transform();
	}

	private final CFG cfg;

	private List<BB> postorderedBbs;

	private Set<BB> traversed;

	/**
	 * Constructor.
	 * 
	 * @param cfg
	 *            CFG
	 */
	private TrCalculatePostorder(final CFG cfg) {
		this.cfg = cfg;
	}

	private int calculatePostorder(final int postorder, final BB bb) {
		// DFS
		this.traversed.add(bb);
		int _postorder = postorder;
		for (final E out : bb.getOuts()) {
			final BB succ = out.getEnd();
			if (this.traversed.contains(succ)) {
				continue;
			}
			_postorder = calculatePostorder(_postorder, succ);
		}
		bb.setPostorder(_postorder);
		this.postorderedBbs.add(bb);
		return _postorder + 1;
	}

	public void transform() {
		this.postorderedBbs = Lists.newArrayList();
		this.traversed = Sets.newHashSet();
		calculatePostorder(0, this.cfg.getStartBb());
		this.cfg.setPostorderedBbs(this.postorderedBbs);
	}

}