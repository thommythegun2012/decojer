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
import java.util.Deque;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.utils.ELineComperator;
import org.eclipse.jdt.core.dom.Statement;

import com.google.common.base.Objects;
import com.google.common.collect.Queues;

import lombok.Getter;
import lombok.Setter;

/**
 * Edge for CFG.
 *
 * @author André Pankraz
 */
public final class E {

	public static final ELineComperator LINE_COMPARATOR = new ELineComperator();

	@Getter
	@Setter
	@Nullable
	private Statement branchingStmt;

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

	@Nullable
	public E getRetOut() {
		final Object value = getValue();
		if (!(value instanceof Call)) {
			return null;
		}
		final Call call = (Call) value;

		final Deque<BB> checkBbs = Queues.newArrayDeque();
		checkBbs.push(getEnd());
		while (!checkBbs.isEmpty()) {
			final BB checkBb = checkBbs.removeFirst();
			for (final E out : checkBb.getOuts()) {
				if (call == out.getValue()) {
					// different call-pairs for same sub necessary here
					return out;
				}
				if (!out.isBack()) {
					checkBbs.push(out.getEnd());
				}
			}
		}
		return null;
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
		final StringBuilder sb = new StringBuilder();
		// find edge index of this out in start BB
		final List<E> outs = start == null || start.isRemoved() ? null : start.getOuts();
		if (outs != null && outs.size() > 1) {
			sb.append(outs.indexOf(this));
		}
		if (this.value != null) {
			if (sb.length() != 0) {
				sb.append(' ');
			}
			if (this.value instanceof Object[]) {
				sb.append(Arrays.toString((Object[]) this.value));
			} else {
				sb.append('(').append(this.value).append(')');
			}
		}
		if (this.branchingStmt != null) {
			if (sb.length() != 0) {
				sb.append(' ');
			}
			sb.append(this.branchingStmt);
		}
		final String ret = sb.toString();
		assert ret != null;
		return ret;
	}

	@Override
	public int hashCode() {
		return getStart().hashCode() * 13 + getEnd().hashCode();
	}

	/**
	 * Is back edge? This includes self-loops (same node).
	 *
	 * @return {@code true} - is back edge
	 */
	public boolean isBack() {
		// don't use same postorder as check, could happen through artificial helper nodes like
		// created by TrCfg2JavaExpressionStmts#pullStackValue()
		return getStart().getPostorder() < getEnd().getPostorder() || getStart() == getEnd();
	}

	/**
	 * Is catch (including finally)?
	 *
	 * @return {@code true} - is catch (including finally)
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
	 * Is finally?
	 *
	 * @return {@code true} - is catch
	 */
	public boolean isFinally() {
		final Object value = getValue();
		if (!(value instanceof T[])) {
			return false; // !isCatch()
		}
		return ((T[]) value).length == 0;
	}

	/**
	 * Is JSR edge?
	 *
	 * @return {@code true} - is JSR edge
	 */
	public boolean isJsr() {
		final Object value = getValue();
		if (!(value instanceof Call)) {
			return false;
		}
		final Sub sub = ((Call) value).getSub();
		return sub.getPc() == getEnd().getPc();
	}

	/**
	 * Is RET edge?
	 *
	 * @return {@code true} - is RET edge
	 */
	public boolean isRet() {
		final Object value = getValue();
		if (!(value instanceof Call)) {
			return false;
		}
		final Sub sub = ((Call) value).getSub();
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

	@SuppressWarnings("null")
	protected void setEnd(final BB end) {
		assert this.end != end;
		assert end == null || this.start != null && !end.isRemoved();
		// fix JSR/RET ins with Sub value, see also BB.setPc(int)
		if (this.value instanceof Call && this.end != null && end != null
				&& ((Call) this.value).getSub().getPc() == this.end.getPc()) {
			((Call) this.value).getSub().setPc(end.getPc());
		}
		this.end = end;
	}

	protected void setStart(final BB start) {
		assert this.start != start;
		assert start == null ? this.end == null : !start.isRemoved();
		this.start = start;
	}

	@Override
	public String toString() {
		final String valueString = getValueString();
		return (this.start == null ? "null" : getStart().getPc()) + " -> "
				+ (this.end == null ? "null" : getEnd().getPc())
				+ (valueString.isEmpty() ? "" : " : " + getValueString());
	}

}