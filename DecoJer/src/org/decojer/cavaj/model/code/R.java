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

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Register.
 * 
 * @author André Pankraz
 */
public class R {

	/**
	 * Register kind.
	 * 
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * STORE_CONST. Maybe previous r.
		 */
		CONST,

		/**
		 * Previous r.
		 */
		LOAD,

		/**
		 * Merge ins.
		 */
		MERGE,

		/**
		 * STORE_MOVE. New r, maybe previous r.
		 */
		MOVE

	}

	@Getter
	private final Kind kind;

	/**
	 * Merge register types.
	 * 
	 * @param r1
	 *            register 1
	 * @param r2
	 *            register 2
	 * @return merged register type
	 */
	public static T merge(final R r1, final R r2) {
		if (r1 == null || r2 == null) {
			return null;
		}
		return T.merge(r1.getT(), r2.getT());
	}

	@Getter
	private R[] outs;

	@Getter
	private T t;

	@Getter
	private Object value;

	@Getter
	private R[] ins;

	/**
	 * Register start pc. Method parameters (0) and merge event pcs can overlap with real operation.
	 */
	@Getter
	private final int pc;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            register start pc
	 * @param t
	 *            register type
	 * @param kind
	 *            register kind
	 * @param ins
	 *            input registers
	 */
	public R(final int pc, final T t, final Kind kind, final R... ins) {
		this(pc, t, null, kind, ins);
	}

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            register start pc
	 * @param t
	 *            register type
	 * @param value
	 *            register value
	 * @param kind
	 *            register kind
	 * @param ins
	 *            input registers
	 */
	public R(final int pc, final T t, final Object value, final Kind kind, final R... ins) {
		this.pc = pc;
		this.t = t;
		this.value = value;
		this.kind = kind;
		if (ins != null) {
			this.ins = ins;
			for (final R in : ins) {
				linkIn(in);
			}
		}
	}

	/**
	 * Increment value.
	 * 
	 * @param inc
	 *            increment
	 */
	public void inc(final int inc) {
		if (getValue() == null) {
			return;
		}
		this.value = ((Number) getValue()).intValue() + inc;
	}

	/**
	 * Is wide type?
	 * 
	 * @return true - is wide type
	 */
	public boolean isWide() {
		return this.t.isWide();
	}

	private void linkIn(final R in) {
		final R[] inOuts = in.outs;
		if (in.outs == null) {
			in.outs = new R[] { this };
		} else {
			in.outs = new R[inOuts.length + 1];
			System.arraycopy(inOuts, 0, in.outs, 0, inOuts.length);
			in.outs[inOuts.length] = this;
		}
	}

	public void merge(final R r) {
		mergeTo(r.getT());
		final R[] newIns = new R[this.ins.length + 1];
		System.arraycopy(this.ins, 0, newIns, 0, this.ins.length);
		newIns[this.ins.length] = r;
		this.ins = newIns;
	}

	/**
	 * Merge to type.
	 * 
	 * @param t
	 *            type
	 * @return true - success
	 */
	public boolean mergeTo(final T t) {
		final T mergeTo = this.t.mergeTo(t);
		if (this.t == mergeTo) {
			return true;
		} else if (null == mergeTo) {
			return false;
		}
		this.t = mergeTo;
		if (this.outs != null) {
			for (final R out : this.outs) {
				out.mergeTo(mergeTo);
			}
		}
		if (this.ins == null || this.ins.length == 0) {
			return true;
		}
		switch (getKind()) {
		case CONST:
		case MOVE:
			this.ins[0].mergeTo(mergeTo);
			return true;
		case MERGE:
			for (final R in : this.ins) {
				in.mergeTo(mergeTo);
			}
		}
		return true;
	}

	/**
	 * Read type.
	 * 
	 * @param t
	 *            type
	 * @return true - success
	 */
	public boolean read(final T t) {
		return mergeTo(t);
	}

	/**
	 * Replace input register.
	 * 
	 * @param oldIn
	 *            old input register
	 * @param in
	 *            new input register
	 */
	public void replaceIn(final R oldIn, final R in) {
		assert this.ins != null;

		for (int i = this.ins.length; i-- > 0;) {
			if (this.ins[i] == oldIn) {
				if (in != null) {
					this.ins[i] = in;
					linkIn(in);
					// oldIn dies anyway, no out remove necessary
					return;
				}
				switch (getKind()) {
				case CONST:
				case MOVE:
					if (this.ins.length < 2 || this.ins[1] != in) {
						System.out.println("Register replace to null has wrong previous!");
					}
					this.ins = new R[] { this.ins[0] };
					return;
				case MERGE:
					System.out.println("Register replace to null for merge not possible!");
				}
				return;
			}
		}
		assert false;
	}

	@Override
	public String toString() {
		return "R_" + this.pc + ": " + this.t;
	}

}