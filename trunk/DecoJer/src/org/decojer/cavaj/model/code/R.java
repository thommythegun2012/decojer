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
	public static R createBoolmathR(final int pc, final int i, final T t, final Object value,
			final R r1, final R r2) {
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
	public static R createConstR(final int pc, final int i, final T t, final Object value) {
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
	public static R createMergeR(final int pc, final int i, final T t, final Object value,
			final R r1, final R r2) {
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

	private R[] ins;

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
	private T lowerT;

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
	private T upperT;

	/**
	 * Register value, for constants as far as we can derive them easily.
	 */
	@Setter
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
	private R(final int pc, final int i, final T t, final Object value, final Kind kind,
			final R... ins) {
		this.pc = pc;
		this.i = i;
		this.lowerT = t;
		this.value = value;
		this.kind = kind;
		if (ins != null) {
			this.ins = ins;
			for (final R in : ins) {
				in.addOut(this);
			}
		}
	}

	public void addInMerge(final T t, final R r) {
		assert getKind() == R.Kind.MERGE;

		setLowerT(t);
		final R[] newIns = new R[this.ins.length + 1];
		System.arraycopy(this.ins, 0, newIns, 0, this.ins.length);
		newIns[this.ins.length] = r;
		this.ins = newIns;
	}

	private void addOut(final R r) {
		if (this.outs == null) {
			this.outs = new R[] { r };
		} else {
			final R[] newOuts = new R[this.outs.length + 1];
			System.arraycopy(this.outs, 0, newOuts, 0, this.outs.length);
			newOuts[this.outs.length] = r;
			this.outs = newOuts;
		}
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
			if (this.lowerT.is(T.BOOLEAN) || t.is(T.BOOLEAN)) {
				// TODO HACK: check net.miginfocom.layout.Grid$Cell.access$476 in
				// miglayout-3.6-swing.jar
				// not a valid Java code but valid JVM code
				return true;
			}
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
		for (final R in : this.ins) {
			in.assignTo(reducedT);
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
		assert getKind() == Kind.MOVE && this.ins.length == 1;

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
	 * @see CFG#initFrames()
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
		assert this.ins != null;

		for (int i = this.ins.length; i-- > 0;) {
			if (this.ins[i] != prevIn) {
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
			this.ins[i] = newIn;
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
		assert !isMethodParam();

		this.lowerT = t;
		if (this.outs != null) {
			for (final R r : getOuts()) {
				r.setLowerT(t);
			}
		}

		if (getKind() == Kind.MERGE) {
			// TODO isAlive, out-check not sufficient because outs could also be down the road with
			// a read at start
			return;
		}
		if (this.ins != null) {
			for (final R r : getIns()) {
				r.setLowerT(t);
			}
		}
	}

	@Override
	public String toString() {
		return "R" + getI() + "." + getSimpleName();
	}

}