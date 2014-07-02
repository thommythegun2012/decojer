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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.code.BB;

/**
 * Loop struct.
 *
 * @author André Pankraz
 */
public class Loop extends Struct {

	/**
	 * Loop kind.
	 *
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * Endless loop.
		 */
		ENDLESS,
		/**
		 * While loop.
		 */
		WHILE,
		/**
		 * Whilenot loop.
		 */
		WHILENOT,
		/**
		 * Do-while loop.
		 */
		DO_WHILE,
		/**
		 * Do-whilenot loop.
		 */
		DO_WHILENOT

	}

	@Getter
	private BB last;

	@Getter
	@Setter
	private Kind kind;

	/**
	 * Constructor.
	 *
	 * @param bb
	 *            loop head BB
	 */
	public Loop(@Nonnull final BB bb) {
		super(bb);
	}

	@Override
	public boolean hasMember(@Nullable final BB bb) {
		// last BB for loops is separately stored but counts as normal member
		return isLast(bb) || super.hasMember(bb);
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
	public boolean isContinueTarget(@Nullable final BB bb) {
		switch (getKind()) {
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
		return getKind() == Kind.ENDLESS;
	}

	/**
	 * Is BB last?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB is last
	 */
	public boolean isLast(@Nullable final BB bb) {
		return getLast() == bb;
	}

	/**
	 * Is loop post?
	 *
	 * @return {@code true} - loop is post
	 */
	public boolean isPost() {
		return getKind() == Kind.DO_WHILE || getKind() == Kind.DO_WHILENOT;
	}

	/**
	 * Is loop pre?
	 *
	 * @return {@code true} - loop is pre
	 */
	public boolean isPre() {
		return getKind() == Kind.WHILE || getKind() == Kind.WHILENOT;
	}

	/**
	 * Set last BB.
	 *
	 * @param bb
	 *            last BB
	 */
	public void setLast(@Nonnull final BB bb) {
		// cannot add as member, tail could be equal to head!
		this.last = bb;
		bb.setStruct(this);
	}

	@Override
	public String toStringSpecial(final String prefix) {
		final StringBuilder sb = new StringBuilder();
		sb.append(prefix).append("Last: BB " + (getLast() == null ? "???" : getLast().getPc()));
		sb.append('\n').append(prefix).append("Kind: ").append(getKind());
		return sb.toString();
	}

}