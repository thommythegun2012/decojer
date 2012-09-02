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
package org.decojer.cavaj.model.code.structs;

import org.decojer.cavaj.model.code.BB;

/**
 * Loop struct.
 * 
 * @author André Pankraz
 */
public class Loop extends Struct {

	public static final int ENDLESS = 1;

	public static final int WHILE = 2;

	public static final int WHILENOT = 3;

	public static final int DO_WHILE = 4;

	public static final int DO_WHILENOT = 5;

	public static final String[] TYPE_NAME = { "<UNKNOWN>", "ENDLESS", "WHILE", "WHILENOT",
			"DO_WHILE", "DO_WHILENOT" };

	private BB last;

	private int type;

	/**
	 * Constructor.
	 * 
	 * @param bb
	 *            loop head BB
	 */
	public Loop(final BB bb) {
		super(bb);
	}

	/**
	 * Get last BB.
	 * 
	 * @return last BB
	 */
	public BB getLast() {
		return this.last;
	}

	/**
	 * Get loop type.
	 * 
	 * @return loop type (endless, do-while etc.)
	 */
	public int getType() {
		return this.type;
	}

	@Override
	public boolean isBranching(final BB bb) {
		if (isContinueTarget(bb)) {
			return true;
		}
		return super.isBranching(bb);
	}

	/**
	 * Is BB target for continue?
	 * 
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is target for continue
	 */
	public boolean isContinueTarget(final BB bb) {
		switch (this.type) {
		case DO_WHILE:
		case DO_WHILENOT:
		case ENDLESS: // must reduce empty BBs to direct edges before!
			return isHead(bb);
		case WHILE:
		case WHILENOT:
			return isLast(bb);
		}
		return false;
	}

	/**
	 * Is loop endless?
	 * 
	 * @return {@code true} - loop is endless
	 */
	public boolean isEndless() {
		return this.type == ENDLESS;
	}

	/**
	 * Is BB last?
	 * 
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is last
	 */
	public boolean isLast(final BB bb) {
		return getLast() == bb;
	}

	@Override
	public boolean isMember(final BB bb) {
		// last BB for loops is separately stored but counts as normal member
		return isLast(bb) || super.isMember(bb);
	}

	/**
	 * Is loop post?
	 * 
	 * @return {@code true} - loop is post
	 */
	public boolean isPost() {
		return this.type == DO_WHILE || this.type == DO_WHILENOT;
	}

	/**
	 * Is loop pre?
	 * 
	 * @return {@code true} - loop is pre
	 */
	public boolean isPre() {
		return this.type == WHILE || this.type == WHILENOT;
	}

	/**
	 * Set last BB.
	 * 
	 * @param bb
	 *            last BB
	 */
	public void setLast(final BB bb) {
		// cannot add as member, tail could be equal to head!
		this.last = bb;
		bb.setStruct(this);
	}

	/**
	 * Set loop type.
	 * 
	 * @param type
	 *            loop type (endless, do-while etc.)
	 */
	public void setType(final int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\nLast: BB " + getLast().getPostorder());
		sb.append("\nType: " + TYPE_NAME[getType()]);
		return sb.toString();
	}

}