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
public final class R {

	/**
	 * Register kind.
	 * 
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * Push const.
		 */
		CONST,

		/**
		 * Merge ins. Incoming registers.
		 */
		MERGE,

		/**
		 * Store move. Source register, maybe previous register.
		 */
		MOVE,
		/**
		 * READ.
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
				in.addOut(this);
			}
		}
	}

	public void addInMerge(final T t, final R r) {
		assert getKind() == R.Kind.MERGE;

		setT(t);
		final R[] newIns = new R[this.ins.length + 1];
		System.arraycopy(this.ins, 0, newIns, 0, this.ins.length);
		newIns[this.ins.length] = r;
		this.ins = newIns;
		if (this.readT != null) {
			r.assignTo(this.readT);
		}
	}

	private void addOut(final R outR) {
		if (this.outs == null) {
			this.outs = new R[] { outR };
		} else {
			final R[] newOuts = new R[this.outs.length + 1];
			System.arraycopy(this.outs, 0, newOuts, 0, this.outs.length);
			newOuts[this.outs.length] = outR;
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
	public boolean assignTo(final T t) {
		final T reducedT = this.t.assignTo(t); // primitive reduction for this.t possible
		if (reducedT == null) {
			if (this.t.isUnresolvable() && this.t != T.REF) {
				return true;
			}
			// TODO problem with generic type reduction to classes, invoke interface allowed

			// TODO check org.decojer.cavaj.test.jdk6.DecTestMethodTypeParams:
			// RETURN Long as TypeParam U - resolve to TypeArg <U extends Long>
			assert false;
		}
		if (!this.t.equals(reducedT)) {
			// possible primitive multitype reduction
			setT(reducedT);
			if (this.outs != null) {
				for (final R out : this.outs) {
					out.readForwardPropagate(t);
				}
			}
		}
		switch (this.kind) {
		case CONST:
			break;
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
			this.ins[0].assignTo(t);
		}
		this.readT = T.union(this.readT, t);
		return true;
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
	 * Is register a method parameter?
	 * 
	 * @return {@code true} - is method parameter
	 * @see CFG#initFrames()
	 */
	public boolean isMethodParam() {
		return this.pc < 0;
	}

	private boolean readForwardPropagate(final T t) {
		final T reducedT = this.t.assignTo(t);
		if (reducedT == null) {
			if (this.t.isUnresolvable()) {
				return true;
			}
			assert false;
		}
		if (!this.t.equals(reducedT)) {
			// possible primitive multitype reduction
			setT(reducedT);
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
			setT(newIn.getT());
		}
		return false;
	}

	private void setT(final T t) {
		if (this.t == t) {
			return;
		}
		assert !isMethodParam();

		this.t = t;
		if (this.outs != null) {
			for (final R r : getOuts()) {
				r.setT(t);
			}
		}

		if (getKind() == Kind.MERGE) {
			// TODO check alive
			return;
		}
		if (this.ins != null) {
			for (final R r : getIns()) {
				r.setT(t);
			}
		}
	}

	@Override
	public String toString() {
		return "R" + this.pc + "_" + this.kind.name().substring(0, 2) + ": " + this.t;
	}

}