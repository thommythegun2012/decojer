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
import lombok.Setter;

import org.decojer.cavaj.model.T;

/**
 * Register.
 * 
 * @author André Pankraz
 */
public final class R {

	/**
	 * Register kind.
	 * 
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * STORE_CONST.
		 */
		CONST,

		/**
		 * Merge ins. Incoming registers.
		 */
		MERGE,

		/**
		 * STORE_MOVE. Source register, maybe previous register.
		 */
		MOVE,

		/**
		 * Previous register.
		 */
		READ

	}

	/**
	 * Merge register types.
	 * 
	 * @param r1
	 *            register 1
	 * @param r2
	 *            register 2
	 * @return merged register type or null
	 */
	public static T merge(final R r1, final R r2) {
		if (r1 == null || r2 == null) {
			return null;
		}
		return T.join(r1.getT(), r2.getT());
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

	@Getter
	private final Kind kind;

	@Getter
	@Setter
	private T realT;

	private T readT;

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
	 * Is this type instance assignable from given type instance?
	 * 
	 * Attention: Does work for primtives implicit conversion (byte 2 short 2 int, char 2 int).
	 * 
	 * @param t
	 *            type
	 * @return true - is assignable
	 */
	public boolean isAssignableTo(final T t) {
		return this.t.read(t) != null;
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
		this.t = T.join(this.t, r.t);
		final R[] newIns = new R[this.ins.length + 1];
		System.arraycopy(this.ins, 0, newIns, 0, this.ins.length);
		newIns[this.ins.length] = r;
		this.ins = newIns;
		if (this.readT != null) {
			r.read(this.readT);
		}
	}

	/**
	 * Read type.
	 * 
	 * @param t
	 *            type
	 * @return true - success
	 */
	public boolean read(final T t) {
		final T reducedT = this.t.read(t);
		if (reducedT == null) {
			if (!this.t.isResolveable()) {
				return true;
			}
			// TODO problem with generic type reduction to classes, invoke interface allowed
			assert false;
		}
		if (this.t != reducedT) {
			// possible primitive multitype reduction
			this.t = reducedT;
			if (this.outs != null) {
				for (final R out : this.outs) {
					out.readForwardPropagate(t);
				}
			}
		}
		switch (this.kind) {
		case MERGE:
			for (final R in : this.ins) {
				// TODO endless loop in.read(t);
				if (in != null) {
					// TODO
				}
			}
			break;
		case MOVE:
		case READ:
			this.ins[0].read(t);
		}

		this.readT = T.union(this.readT, t);
		return true;
	}

	private boolean readForwardPropagate(final T t) {
		final T reducedT = this.t.read(t);
		if (reducedT == null) {
			if (!this.t.isResolveable()) {
				return true;
			}
			assert false;
		}
		if (this.t != reducedT) {
			// possible primitive multitype reduction
			this.t = reducedT;
			if (this.outs != null) {
				for (final R out : this.outs) {
					out.readForwardPropagate(t);
				}
			}
		}
		return true;
	}

	/**
	 * Replace input register.
	 * 
	 * @param prevIn
	 *            previous input register
	 * @param newIn
	 *            new input register
	 */
	public void replaceIn(final R prevIn, final R newIn) {
		assert this.ins != null;

		for (int i = this.ins.length; i-- > 0;) {
			if (this.ins[i] == prevIn) {
				if (newIn != null) {
					this.ins[i] = newIn;
					linkIn(newIn);
					// oldIn dies anyway, no out remove necessary
					return;
				}
				switch (getKind()) {
				case CONST:
				case MOVE:
					if (this.ins.length < 2 || this.ins[1] != newIn) {
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
	}

	@Override
	public String toString() {
		return "R" + this.pc + "_" + this.kind.name().substring(0, 2) + ": " + this.t;
	}

}