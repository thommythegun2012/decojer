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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.transformers.TrCalculatePostorder;
import org.decojer.cavaj.transformers.TrControlFlowAnalysis;
import org.decojer.cavaj.transformers.TrControlFlowStmts;
import org.decojer.cavaj.transformers.TrDalvikRemoveTempRegs;
import org.decojer.cavaj.transformers.TrDataFlowAnalysis;
import org.decojer.cavaj.transformers.TrExpressions;
import org.eclipse.jdt.core.dom.Block;

/**
 * Control flow graph.
 *
 * @author André Pankraz
 */
@Slf4j
public final class CFG {

	/**
	 * AST method block.
	 */
	@Getter
	@Setter
	private Block block;

	/**
	 * From JVM Spec, Exceptions: At run time, when an exception is thrown, the Java Virtual Machine
	 * searches the exception handlers of the current method in the order that they appear in the
	 * corresponding exception handler table in the class file, starting from the beginning of that
	 * table. Note that the Java Virtual Machine does not enforce nesting of or any ordering of the
	 * exception table entries of a method.
	 */
	@Getter
	@Setter
	private Exc[] excs;

	private Frame[] frames;

	/**
	 * Array with immediate dominators, index is postorder.
	 */
	private BB[] iDoms;

	/**
	 * Max stack size.
	 */
	@Getter
	private final int maxStack;

	@Getter
	@Nonnull
	private final M m;

	/**
	 * Operations for PCs.
	 */
	@Getter
	private final Op[] ops;

	/**
	 * BBs for PCs.
	 */
	@Getter
	private BB[] bbs;

	/**
	 * Array with postordered BBs.
	 */
	@Getter
	@Setter
	private List<BB> postorderedBbs;

	/**
	 * Register count (max locals).
	 */
	@Getter
	private final int regs;

	private V[][] vss;

	/**
	 * Constructor.
	 *
	 * @param m
	 *            method
	 * @param regs
	 *            register count (max locals)
	 * @param maxStack
	 *            max stack size
	 * @param ops
	 *            operations
	 */
	public CFG(@Nonnull final M m, final int regs, final int maxStack, final Op[] ops) {
		assert regs >= 0 : regs;
		assert maxStack >= 0 : maxStack;

		this.m = m;
		m.setCfg(this);
		this.regs = regs;
		this.maxStack = maxStack;
		this.ops = ops;
	}

	/**
	 * Add local variable.
	 *
	 * Only basic checks, compare later with method parameters.
	 *
	 * @param reg
	 *            register
	 * @param var
	 *            local variable
	 */
	public void addVar(final int reg, final V var) {
		assert var != null;

		V[] vars = null;
		if (this.vss == null) {
			this.vss = new V[reg + 1][];
		} else if (reg >= this.vss.length) {
			final V[][] newVarss = new V[reg + 1][];
			System.arraycopy(this.vss, 0, newVarss, 0, this.vss.length);
			this.vss = newVarss;
		} else {
			vars = this.vss[reg];
		}
		if (vars == null) {
			vars = new V[1];
		} else {
			final V[] newVars = new V[vars.length + 1];
			System.arraycopy(vars, 0, newVars, 0, vars.length);
			vars = newVars;
		}
		vars[vars.length - 1] = var;
		this.vss[reg] = vars;
	}

	/**
	 * Calculate iDoms.
	 */
	public void calculateIDoms() {
		int b = this.postorderedBbs.size();
		this.iDoms = new BB[b--];
		this.iDoms[b] = this.postorderedBbs.get(b);
		boolean changed = true;
		while (changed) {
			changed = false;
			// start with rootNode, means this.postorderBBNodes.length - 1
			for (; b-- > 0;) {
				BB iDomNew = null;
				for (final E in : this.postorderedBbs.get(b).getIns()) {
					final BB pred = in.getStart();
					if (this.iDoms[pred.getPostorder()] == null) {
						continue;
					}
					if (iDomNew == null) {
						iDomNew = pred;
						continue;
					}
					iDomNew = intersectIDoms(pred, iDomNew);
				}
				if (this.iDoms[b] == iDomNew) {
					continue;
				}
				this.iDoms[b] = iDomNew;
				changed = true;
			}
		}
	}

	/**
	 * Clear CFG.
	 */
	public void clear() {
		this.bbs = null;
		this.block = null;
		this.frames = null;
		this.iDoms = null;
		this.postorderedBbs = null;
	}

	/**
	 * Decompile CFG.
	 */
	public void decompile() {
		decompile(10);
	}

	/**
	 * Decompile CFG.
	 *
	 * @param stage
	 *            stage
	 */
	public void decompile(final int stage) {
		assert stage >= 0;

		TrDataFlowAnalysis.transform(this);
		TrCalculatePostorder.transform(this);
		if (stage > 0) {
			TrDalvikRemoveTempRegs.transform(this);

			TrExpressions.transform(this);
			TrCalculatePostorder.transform(this);
		}
		if (stage > 1) {
			TrControlFlowAnalysis.transform(this);
		}
		if (stage > 2) {
			TrControlFlowStmts.transform(this);
		}
	}

	/**
	 * Get BB for PC.
	 *
	 * @param pc
	 *            PC
	 * @return BB
	 */
	public BB getBb(final int pc) {
		return this.bbs[pc];
	}

	/**
	 * Get compilation unit.
	 *
	 * @return compilation unit
	 */
	public CU getCu() {
		return getT().getCu();
	}

	/**
	 * Get local variable (from debug info).
	 *
	 * @param reg
	 *            register
	 * @param pc
	 *            pc
	 *
	 * @return local variable (from debug info)
	 */
	@Nullable
	public V getDebugV(final int reg, final int pc) {
		if (this.vss == null || reg >= this.vss.length) {
			return null;
		}
		final V[] vs = this.vss[reg];
		if (vs == null) {
			return null;
		}
		for (int i = vs.length; i-- > 0;) {
			final V v = vs[i];
			if (v.validFor(pc)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * Get decompilation unit.
	 *
	 * @return decompilation unit
	 */
	@Nonnull
	public DU getDu() {
		final DU du = getT().getDu();
		assert du != null;
		return du;
	}

	/**
	 * Get frame for PC.
	 *
	 * @param pc
	 *            PC
	 * @return frame
	 */
	@Nullable
	public Frame getFrame(final int pc) {
		return this.frames[pc];
	}

	/**
	 * Get local variable (from frame).
	 *
	 * @param reg
	 *            register
	 * @param pc
	 *            PC
	 * @return local variable (from frame)
	 */
	public V getFrameVar(final int reg, final int pc) {
		return getDebugV(reg, pc); // hack TODO this.frames[pc].get(reg);
	}

	/**
	 * Get immediate dominator (IDom) for BB.
	 *
	 * @param bb
	 *            BB
	 * @return immediate domminator (IDom) for BB
	 */
	public BB getIDom(final BB bb) {
		return this.iDoms[bb.getPostorder()];
	}

	/**
	 * Get input frame for operation.
	 *
	 * @param op
	 *            operation
	 * @return input frame
	 */
	public Frame getInFrame(final Op op) {
		return this.frames[op.getPc()];
	}

	/**
	 * Get output frame for operation.
	 *
	 * Doesn't (and isn't required to) work for control flow statements.
	 *
	 * @param op
	 *            operation
	 * @return output frame
	 */
	public Frame getOutFrame(final Op op) {
		return this.frames[op.getPc() + 1];
	}

	/**
	 * Get start BB.
	 * 
	 * @return start BB
	 */
	public BB getStartBb() {
		return getBb(0);
	}

	/**
	 * Get type.
	 *
	 * @return type
	 */
	public T getT() {
		return getM().getT();
	}

	/**
	 * Initialize frames. Create first frame from method parameters.
	 *
	 * @return start BB
	 * @see R#isMethodParam()
	 */
	@Nonnull
	public BB init() {
		this.bbs = new BB[this.ops.length];
		this.frames = new Frame[this.ops.length];
		final Frame frame = new Frame(this);

		int reg = 0;
		if (!getM().isStatic()) {
			final T ownerT = getM().getT();
			assert ownerT != null;
			frame.store(0, R.createConstR(0, 0, ownerT, null));
			++reg;
		}
		final T[] paramTs = getM().getParamTs();
		for (final T paramT : paramTs) {
			assert paramT != null;
			frame.store(reg, R.createConstR(0, reg, paramT, null));
			++reg;
			if (paramT.isWide()) {
				++reg;
			}
		}
		this.frames[0] = frame;
		return newBb(0);
	}

	private BB intersectIDoms(final BB b1, final BB b2) {
		BB finger1 = b1;
		BB finger2 = b2;
		while (finger1 != finger2) {
			while (finger1.getPostorder() < finger2.getPostorder()) {
				finger1 = this.iDoms[finger1.getPostorder()];
			}
			while (finger2.getPostorder() < finger1.getPostorder()) {
				finger2 = this.iDoms[finger2.getPostorder()];
			}
		}
		return finger1;
	}

	/**
	 * Are frames initialized?
	 *
	 * @return {@code true} - frames are initialized
	 */
	public boolean isFrames() {
		return this.frames != null;
	}

	/**
	 * Should transformer ignore this?
	 *
	 * @return {@code true} - ignore this
	 */
	public boolean isIgnore() {
		return this.ops == null || this.ops.length == 0 || this.block == null;
	}

	/**
	 * Is line information available?
	 *
	 * @return {@code true} - line information is available
	 */
	public boolean isLineInfo() {
		return getStartBb().isLineInfo();
	}

	private void log(final String message) {
		log.warn(getM() + ": " + message);
	}

	/**
	 * New BB.
	 *
	 * @param opPc
	 *            first operation PC
	 *
	 * @return BB
	 */
	@Nonnull
	public BB newBb(final int opPc) {
		return new BB(this, opPc);
	}

	/**
	 * Post process local variables, e.g. extract method parameter. Better in read step than in
	 * transformator for generic base model. Dalvik has parameter names separately encoded.
	 */
	public void postProcessVars() {
		if (this.vss == null) {
			this.vss = new V[this.regs][];
		} else if (this.regs < this.vss.length) {
			log("Register count less than biggest register with local variable info!");
		} else if (this.regs > this.vss.length) {
			final V[][] newVarss = new V[this.regs][];
			System.arraycopy(this.vss, 0, newVarss, 0, this.vss.length);
			this.vss = newVarss;
		}
		final T[] paramTs = this.m.getParamTs();
		final T ownerT = this.m.getT();
		assert ownerT != null;
		if (ownerT.isDalvik()) {
			// Dalvik...function parameters right aligned
			int reg = this.regs;
			for (int i = paramTs.length; i-- > 0;) {
				final T paramT = paramTs[i];
				if (paramT.isWide()) {
					--reg;
				}
				// parameter name is encoded in extra debug info, copy names and parameter types to
				// local vars - sometimes jar2dex forgets to eliminate these infos, e.g.
				// for synthetic this in <init> or for arguments with generic type like U, T
				final V[] vs = this.vss[--reg];
				if (vs != null) {
					// this can happen, we can find local variable info for generic type parameters
					// in Dalvik because they are not properly eliminated from JVM Bytecode, e.g. in
					// DecTestMethodTypeParams.parameterizedStaticClassMethod:
					// [pc: 0, pc: 15] local: a index: 0 type: T

					// log("Found local variable info for method parameter '" + reg + "'!");

					// this info is usually more detailed, can have multiple valid ranges and start
					// at higher PCs, e.g. for inner

					// nevertheless we simply overwrite it for now...
				}
				this.vss[reg] = new V[] { new V(paramT, this.m.getParamName(i), 0, this.ops.length) };
			}
			if (!this.m.isStatic()) {
				final V[] vs = this.vss[--reg];
				if (vs != null) {
					// this can happen, we can find local variable info for synthetic "this" in JVM
					// ByteCode too, e.g. in DecTestMethodTypeParams.<init>:
					// [pc: 0, pc: 5] local: this index: 0 type:
					// org.decojer.cavaj.test.jdk5.DecTestMethodTypeParams<T>
					// but also happens for none-generic, see e.g. DecTestField.<init>)

					// log("Found local variable info for method parameter '" + reg + "' (this)!");

					// this info is usually more detailed, can have multiple valid ranges and start
					// at higher PCs, e.g. for inner

					// nevertheless we simply overwrite it for now...
				}
				this.vss[reg] = new V[] { new V(ownerT, "this", 0, this.ops.length) };
			}
			return;
		}
		// JVM...function parameters left aligned
		int reg = 0;
		if (!this.m.isStatic()) {
			final V[] vs = this.vss[reg];
			if (vs != null) {
				// fix missing / wrong stuff in CFG#initFrames()
				if (vs.length > 1) {
					log("Found local variable info for method parameter '" + reg
							+ "' (this) with multiple ranges!");
				}
				final int[] pcs = vs[0].getPcs();
				if (pcs[0] != 0) {
					log("Found local variable info for method parameter '" + reg
							+ "' (this) with non-zero start pc '" + pcs[0] + "'!");
				}
				++reg;
			} else {
				this.vss[reg++] = new V[] { new V(ownerT, "this", 0, this.ops.length) };
			}
		}
		for (int i = 0; i < paramTs.length; ++i) {
			final T paramT = paramTs[i];
			assert paramT != null;
			final V[] vs = this.vss[reg];
			if (vs != null) {
				if (vs.length > 1) {
					log("Found multiple local variable info for method parameter '" + reg + "'!");
				}
				this.m.setParamName(i, vs[0].getName());
				++reg;
			} else {
				this.vss[reg++] = new V[] { new V(paramT, this.m.getParamName(i), 0,
						this.ops.length) };
			}
			if (paramT.isWide()) {
				++reg;
			}
		}
	}

	protected BB setBb(final int pc, final BB bb) {
		return this.bbs[pc] = bb;
	}

	/**
	 * Set frame for PC.
	 *
	 * Copy frame to prevent lots of possible effects with multi-merges like in RET and
	 * automatically set frame PC.
	 *
	 * @param pc
	 *            PC
	 * @param frame
	 *            frame
	 */
	public void setFrame(final int pc, final Frame frame) {
		this.frames[pc] = new Frame(frame, pc);
	}

	@Override
	public String toString() {
		return this.m.toString() + " (ops: " + this.ops.length + ", regs: " + this.regs + ")";
	}

}