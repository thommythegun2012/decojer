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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.T;

/**
 * Register.
 *
 * @author André Pankraz
 */
@Getter
public final class R {

	/**
	 * Register kind.
	 *
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * Bool math ins, late decide boolean or int params (AND, OR, XOR). Incoming 2 registers.
		 */
		BOOLMATH,

		/**
		 * Push const.
		 */
		CONST,

		/**
		 * Merge ins. Incoming registers.
		 */
		MERGE,

		/**
		 * Store move. Incoming register.
		 */
		MOVE

	}

	/**
	 * Create register of type BOOLMATH.
	 *
	 * @param pc
	 *            register start pc (BOOLMATH operation pc)
	 * @param i
	 *            register index
	 * @param t
	 *            value type
	 * @param value
	 *            value
	 * @param r1
	 *            operation register 1
	 * @param r2
	 *            operation register 2
	 * @return register of type BOOLMATH
	 */
	public static R createBoolmathR(final int pc, final int i, @Nonnull final T t,
			@Nullable final Object value, @Nonnull final R r1, @Nonnull final R r2) {
		return new R(pc, i, t, value, Kind.BOOLMATH, r1, r2);
	}

	/**
	 * Create register of type CONST.
	 *
	 * @param pc
	 *            register start pc (CONST operation pc)
	 * @param i
	 *            register index
	 * @param t
	 *            value type
	 * @param value
	 *            value
	 * @return register of type CONST
	 */
	public static R createConstR(final int pc, final int i, @Nonnull final T t,
			@Nullable final Object value) {
		// value == null possible for method parameters, GETs, exception handlers etc.
		return new R(pc, i, t, value, Kind.CONST);
	}

	/**
	 * Create register of type MERGE.
	 *
	 * @param pc
	 *            register start pc (MERGE pc)
	 * @param i
	 *            register index
	 * @param t
	 *            value type
	 * @param value
	 *            value
	 * @param r1
	 *            input register 1
	 * @param r2
	 *            input register 2
	 * @return register of type MERGE
	 */
	public static R createMergeR(final int pc, final int i, @Nonnull final T t,
			@Nullable final Object value, @Nonnull final R r1, @Nonnull final R r2) {
		return new R(pc, i, t, value, Kind.MERGE, r1, r2);
	}

	/**
	 * Create register of type MOVE.
	 *
	 * @param pc
	 *            register start pc (MOVE operation pc)
	 * @param i
	 *            register index
	 * @param r
	 *            moved register
	 * @return register of type MOVE
	 */
	public static R createMoveR(final int pc, final int i, final R r) {
		return new R(pc, i, r.getLowerT(), r.getValue(), Kind.MOVE, r);
	}

	private final int i;

	@Nullable
	private R[] ins;

	@Nonnull
	private final Kind kind;

	/**
	 * Lower bound of register type, stores (and merges) rise the type bound through type joins.
	 *
	 * Lowest bound is artificial union type "Bottom". All reads must be assignable from this type.
	 * The derived Java variable type must be somewhere between upperT and lowerT. We prefer the
	 * most exact type near lowerT.
	 *
	 * Primitive types behave a bit different for stores: The JVM doesn't really differentiate
	 * between int, bool etc. but Java does. In the JVM you can assign int to bool, but not in Java.
	 * E.g. Dalvik often uses 0-constants that are assigned to int and bool variables for variable
	 * initialization. Hence we sometimes extend the lower type through unions into multi-types for
	 * primitives.
	 */
	@Nonnull
	private T lowerT;

	@Nullable
	private R[] outs;

	/**
	 * Register start PC, this is generally the previous changing operation.
	 *
	 * Method parameter registers have -1 and merge registers have BB start as PC.
	 */
	private final int pc;

	/**
	 * Upper bound of register type, reads lower the type bound through type unions.
	 *
	 * Highest bound for reference types is Object. All stores must be assignable to this type. The
	 * derived Java variable type must be somewhere between upperT and lowerT. We prefer the most
	 * exact type near lowerT.
	 */
	@Nullable
	private T upperT;

	/**
	 * Register value, for constants as far as we can derive them easily.
	 */
	@Setter
	@Nullable
	private Object value;

	/**
	 * Constructor.
	 *
	 * @param pc
	 *            register start pc
	 * @param i
	 *            register index
	 * @param t
	 *            register type
	 * @param value
	 *            register value
	 * @param kind
	 *            register kind
	 * @param ins
	 *            input registers
	 */
	private R(final int pc, final int i, @Nonnull final T t, @Nullable final Object value,
			@Nonnull final Kind kind, @Nullable final R... ins) {
		this.pc = pc;
		this.i = i;
		assert t != null;
		this.lowerT = t;
		this.value = value;
		this.kind = kind;
		if (ins != null && ins.length > 0) {
			this.ins = ins;
			for (final R in : ins) {
				in.addOut(this);
			}
		}
	}

	public void addInMerge(@Nonnull final T t, @Nonnull final R r) {
		assert getKind() == R.Kind.MERGE;
		final R[] prevIns = getIns();
		assert prevIns != null;

		setLowerT(t);
		final R[] newIns = new R[prevIns.length + 1];
		System.arraycopy(prevIns, 0, newIns, 0, prevIns.length);
		newIns[prevIns.length] = r;
		this.ins = newIns;
	}

	private void addOut(@Nonnull final R r) {
		final R[] prevOuts = getOuts();
		if (prevOuts == null) {
			this.outs = new R[] { r };
			return;
		}
		final R[] newOuts = new R[prevOuts.length + 1];
		System.arraycopy(prevOuts, 0, newOuts, 0, prevOuts.length);
		newOuts[prevOuts.length] = r;
		this.outs = newOuts;
	}

	/**
	 * Read type.
	 *
	 * @param t
	 *            type
	 * @return {@code true} - success
	 */
	public boolean assignTo(@Nonnull final T t) {
		final T reducedT = this.lowerT.assignTo(t); // primitive reduction for this.t possible
		if (reducedT == null) {
			if (this.lowerT.isUnresolvable() && this.lowerT != T.REF) {
				return true;
			}
			// TODO could have multiple reasons, one of them is central null-constant for
			// incompatibly typed reads? see T.assignTo(T) for T.REF
			assert false : "cannot assign '" + this + "' to '" + t + "'";
			return false;
		}
		if (this.lowerT.equals(reducedT)) {
			return true;
		}
		// possible primitive multitype reduction
		setLowerT(reducedT);
		if (this.outs != null) {
			for (final R out : this.outs) {
				out.assignTo(t);
			}
		}
		if (this.ins != null) {
			for (final R in : this.ins) {
				in.assignTo(reducedT);
			}
		}
		this.upperT = T.union(this.upperT, t);
		return true;
	}

	/**
	 * Get incoming register.
	 *
	 * Should be a MOVE.
	 *
	 * @return incoming register
	 */
	public R getIn() {
		assert getKind() == Kind.MOVE && this.ins != null && this.ins.length == 1;

		return this.ins[0];
	}

	/**
	 * Get simple name, e.g.: "MO123: int[]"
	 *
	 * @return simple name
	 */
	public String getSimpleName() {
		return getKind().name().substring(0, 2) + getPc() + ": " + this.lowerT.getSimpleName();
	}

	public T getT() {
		// TODO for now we return lower T, especially if value is set...later we must find the
		// proper type between upper and lower
		return getLowerT();
	}

	/**
	 * Is register a method parameter?
	 *
	 * @return {@code true} - is method parameter
	 */
	public boolean isMethodParam() {
		return this.pc == 0;
	}

	/**
	 * Is wide type?
	 *
	 * @return {@code true} - is wide type
	 */
	public boolean isWide() {
		return this.lowerT.isWide();
	}

	/**
	 * Replace input register.
	 *
	 * @param prevIn
	 *            previous input register
	 * @param newIn
	 *            new input register
	 * @return {@code true} - forward replace merge register to null
	 */
	public boolean replaceIn(final R prevIn, final R newIn) {
		final R[] ins = this.ins;
		assert ins != null;

		for (int i = ins.length; i-- > 0;) {
			if (ins[i] != prevIn) {
				continue;
			}
			if (newIn == null) {
				// newIn == null!
				if (getKind() == Kind.MERGE) {
					return true; // forward replace to null
				}
				assert false; // not possible to read, move etc. null

				return false;
			}
			ins[i] = newIn;
			newIn.addOut(this);
			// oldIn dies anyway, no out remove necessary
			setLowerT(newIn.lowerT);
		}
		return false;
	}

	private void setLowerT(final T t) {
		if (this.lowerT == t) {
			return;
		}
		assert !isMethodParam() : "Cannot change register type '" + getLowerT() + "' to '" + t
		+ "!"; // TODO, change VarT bound?
		assert t != null;
		this.lowerT = t;
		if (this.outs != null) {
			for (final R r : this.outs) {
				r.setLowerT(t);
			}
		}

		if (getKind() == Kind.MERGE) {
			// TODO isAlive, out-check not sufficient because outs could also be down the road with
			// a read at start
			return;
		}
		if (this.ins != null) {
			for (final R r : this.ins) {
				r.setLowerT(t);
			}
		}
	}

	/**
	 * Get original R (mostly constant).
	 *
	 * @return original R
	 */
	public R toOriginal() {
		R original = this;
		while (true) {
			final R ins[] = original.ins;
			if (ins == null || ins.length == 0) {
				return original;
			}
			original = ins[0];
		}
	}

	@Override
	public String toString() {
		return "R" + getI() + "." + getSimpleName();
	}

}