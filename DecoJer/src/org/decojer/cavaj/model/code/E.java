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
package org.decojer.cavaj.model.code;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.T;

import com.google.common.base.Objects;

/**
 * Edge for CFG.
 *
 * @author André Pankraz
 */
public final class E {

	public static final Comparator<E> LINE_COMPARATOR = new Comparator<E>() {

		@Override
		public int compare(final E e1, final E e2) {
			// don't change order for out lines that are before the in line
			final int startLine = e1.getStart().getLine();
			final int endLine1 = e1.getEnd().getLine();
			final int endLine2 = e2.getEnd().getLine();
			if (startLine > endLine1 || startLine > endLine2) {
				return 0;
			}
			return compare(e1.getEnd().getLine(), e2.getEnd().getLine());
		}

		// since JDK 7...GAE not
		private int compare(final int x, final int y) {
			return x < y ? -1 : x == y ? 0 : 1;
		}

	};

	@Nullable
	private BB end;

	@Nullable
	private BB start;

	@Getter
	@Setter
	@Nullable
	private Object value;

	/**
	 * Constructor.
	 *
	 * Init as empty edge in "removed state" and update start/end via BB-add/remove-In/out.
	 *
	 * @param value
	 *            value
	 */
	public E(@Nullable final Object value) {
		this.value = value;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof E)) {
			return false;
		}
		final E e = (E) obj;
		return Objects.equal(this.start, e.start) && Objects.equal(this.end, e.end);
	}

	@Nonnull
	public BB getEnd() {
		final BB end = this.end;
		assert end != null && !end.isRemoved();
		return end;
	}

	/**
	 * Get relevant end BB.
	 *
	 * @return relevant end BB
	 *
	 * @see E#getRelevantOut()
	 */
	@Nonnull
	public BB getRelevantEnd() {
		return getRelevantOut().getEnd();
	}

	/**
	 * Get relevant incoming edge, maybe {@code this}.
	 *
	 * @return relevant incoming edge, maybe {@code this}
	 *
	 * @see BB#isRelevant()
	 */
	@Nonnull
	public E getRelevantIn() {
		final BB start = getStart();
		if (start.isRelevant()) {
			return this;
		}
		final E relevantIn = start.getRelevantIn();
		return relevantIn == null ? this : relevantIn;
	}

	/**
	 * Get relevant outgoing edge, maybe {@code this}.
	 *
	 * @return relevant outgoing edge, maybe {@code this}
	 *
	 * @see BB#isRelevant()
	 */
	@Nonnull
	public E getRelevantOut() {
		final BB end = getEnd();
		if (end.isRelevant()) {
			return this;
		}
		final E relevantOut = end.getRelevantOut();
		return relevantOut == null ? this : relevantOut;
	}

	/**
	 * Get relevant start BB.
	 *
	 * @return relevant start BB
	 *
	 * @see E#getRelevantIn()
	 */
	@Nonnull
	public BB getRelevantStart() {
		return getRelevantIn().getStart();
	}

	@Nonnull
	public BB getStart() {
		final BB start = this.start;
		assert start != null && !start.isRemoved();
		return start;
	}

	@Nonnull
	public String getValueString() {
		final BB start = this.start;
		final List<E> outs = start == null || start.isRemoved() ? null : start.getOuts();
		final String prefix = outs != null && outs.size() > 1 ? outs.indexOf(this) + " " : "";
		if (this.value == null) {
			return prefix + "";
		}
		if (this.value instanceof Object[]) {
			return prefix + Arrays.toString((Object[]) this.value);
		}
		return prefix + "(" + this.value + ")";
	}

	@Override
	public int hashCode() {
		return getStart().hashCode() * 13 + getEnd().hashCode();
	}

	/**
	 * Has this edge given BB as predecessor?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - given BB is predecessor of this edge
	 */
	public boolean hasPred(@Nonnull final BB bb) {
		return getStart().isPred(bb);
	}

	/**
	 * Is back edge?
	 *
	 * @return {@code true} - is back edge
	 */
	public boolean isBack() {
		// equal: check self back edge too, ignore catch handler self loops
		return getStart().getPostorder() <= getEnd().getPostorder();
	}

	/**
	 * Is catch?
	 *
	 * @return {@code true} - is catch
	 */
	public boolean isCatch() {
		return this.value instanceof T[];
	}

	/**
	 * Is conditional?
	 *
	 * @return {@code true} - is conditional
	 */
	public boolean isCond() {
		return this.value instanceof Boolean;
	}

	/**
	 * Is conditional false?
	 *
	 * @return {@code true} - is conditional false
	 */
	public boolean isCondFalse() {
		return this.value == Boolean.FALSE;
	}

	/**
	 * Is conditional true?
	 *
	 * @return {@code true} - is conditional true
	 */
	public boolean isCondTrue() {
		return this.value == Boolean.TRUE;
	}

	/**
	 * Is JSR edge?
	 *
	 * @return {@code true} - is JSR edge
	 */
	public boolean isJsr() {
		final Object value = getValue();
		if (!(value instanceof Sub)) {
			return false;
		}
		final Sub sub = (Sub) value;
		return sub.getPc() == getEnd().getPc();
	}

	/**
	 * Is RET edge?
	 *
	 * @return {@code true} - is RET edge
	 */
	public boolean isRet() {
		final Object value = getValue();
		if (!(value instanceof Sub)) {
			return false;
		}
		final Sub sub = (Sub) value;
		return sub.getPc() != getEnd().getPc();
	}

	/**
	 * Is sequence?
	 *
	 * @return {@code true} - is sequence
	 */
	public boolean isSequence() {
		return this.value == null;
	}

	/**
	 * Is switch case?
	 *
	 * @return {@code true} - is switch case
	 */
	public boolean isSwitchCase() {
		return this.value instanceof Object[] && !isCatch();
	}

	/**
	 * Is switch default in cases?
	 *
	 * @return {@code true} - switch default is in cases
	 */
	public boolean isSwitchDefault() {
		if (!isSwitchCase()) {
			return false;
		}
		final Object[] caseValues = (Object[]) this.value;
		assert caseValues != null;
		for (int i = caseValues.length; i-- > 0;) {
			if (caseValues[i] == null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove edge from CFG.
	 */
	public void remove() {
		getEnd().removeIn(this);
		getStart().removeOut(this);
	}

	protected void setEnd(final BB end) {
		assert end == null || this.start != null && !end.isRemoved();
		this.end = end;
	}

	protected void setStart(final BB start) {
		assert start == null ? this.end == null : !start.isRemoved();
		this.start = start;
	}

	@Override
	public String toString() {
		final String valueString = getValueString();
		return (this.start == null ? "null" : getStart().getPc()) + " -> "
		+ (this.start == null ? "null" : getEnd().getPc())
		+ (valueString.isEmpty() ? "" : " : " + getValueString());
	}

}