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
package org.decojer.cavaj.model.struct;

import org.decojer.cavaj.model.BB;

public class Loop extends Struct {

	public static final int ENDLESS = 1;

	public static final int WHILE = 2;

	public static final int WHILENOT = 3;

	public static final int DO_WHILE = 4;

	public static final int DO_WHILENOT = 5;

	public static final String[] TYPE_NAME = { "<UNKNOWN>", "ENDLESS", "WHILE",
			"WHILENOT", "DO_WHILE", "DO_WHILENOT" };

	private BB tail;

	private int type;

	public Loop(final BB bb) {
		super(bb);
	}

	public BB getTail() {
		return this.tail;
	}

	public int getType() {
		return this.type;
	}

	public boolean isEndless() {
		return this.type == ENDLESS;
	}

	@Override
	public boolean isMember(final BB bb) {
		return isTail(bb) || super.isMember(bb);
	}

	public boolean isPost() {
		return this.type == DO_WHILE || this.type == DO_WHILENOT;
	}

	public boolean isPre() {
		return this.type == WHILE || this.type == WHILENOT;
	}

	public boolean isTail(final BB bb) {
		return getTail() == bb;
	}

	public void setTail(final BB bb) {
		// cannot add as member, tail could be equal to head!
		this.tail = bb;
		bb.setStruct(this);
	}

	public void setType(final int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\nTail: BB " + getTail().getPostorder());
		sb.append("\nType: " + TYPE_NAME[getType()]);
		return sb.toString();
	}

}