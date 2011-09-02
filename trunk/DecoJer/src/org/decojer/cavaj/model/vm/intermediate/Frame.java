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
package org.decojer.cavaj.model.vm.intermediate;

import java.util.Stack;

/**
 * @author André Pankraz
 */
public class Frame {

	public Stack<Var> stack;

	public Var[] vars;

	/**
	 * Constructor.
	 */
	public Frame() {
		// nothing
	}

	/**
	 * Constructor.
	 * 
	 * @param frame
	 *            copy frame
	 */
	public Frame(final Frame frame) {
		this.stack = (Stack<Var>) frame.stack.clone();
		this.vars = frame.vars.clone();
	}

	/**
	 * Merge this target frame to given frame.
	 * 
	 * @param frame
	 *            frame
	 * @return this target frame changed
	 */
	public boolean merge(final Frame frame) {
		return true;
	}

}