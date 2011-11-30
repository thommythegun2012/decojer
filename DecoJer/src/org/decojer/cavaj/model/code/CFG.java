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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.op.Op;
import org.eclipse.jdt.core.dom.Block;

/**
 * Control Flow Graph.
 * 
 * @author André Pankraz
 */
public class CFG {

	private final static Logger LOGGER = Logger.getLogger(CFG.class.getName());

	private Block block;

	private Exc[] excs;

	/**
	 * Array with Immediate Dominators, index is postorder.
	 */
	private BB[] iDoms;

	private final int maxLocals;

	private final int maxStack;

	private final MD md;

	private Frame[] frames;

	private boolean error;

	private Op[] ops;

	/**
	 * Array with postordered basic blocks.
	 */
	private List<BB> postorderedBbs;

	private BB startBb;

	private V[][] vss;

	/**
	 * Constructor.
	 * 
	 * @param md
	 *            method declaration
	 * @param maxLocals
	 *            max locals
	 * @param maxStack
	 *            max stack size
	 */
	public CFG(final MD md, final int maxLocals, final int maxStack) {
		assert md != null;
		assert maxLocals >= 0;
		assert maxStack >= 0;

		this.md = md;
		this.maxLocals = maxLocals;
		this.maxStack = maxStack;
	}

	private int _calculatePostorder(final int postorder, final BB bb, final Set<BB> traversed) {
		// DFS
		traversed.add(bb);
		int _postorder = postorder;
		for (final E out : bb.getOuts()) {
			final BB succ = out.getEnd();
			if (traversed.contains(succ)) {
				continue;
			}
			_postorder = _calculatePostorder(_postorder, succ, traversed);
		}
		bb.setPostorder(_postorder);
		this.postorderedBbs.add(bb);
		return _postorder + 1;
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
	 * Calculate postorder.
	 */
	public void calculatePostorder() {
		this.postorderedBbs = new ArrayList<BB>();
		_calculatePostorder(0, this.startBb, new HashSet<BB>());
	}

	/**
	 * Clear CFG.
	 */
	public void clear() {
		this.block = null;
		this.frames = null;
		this.error = false;
		this.iDoms = null;
		this.postorderedBbs = null;
		this.startBb = null;
	}

	/**
	 * Get Eclipse block.
	 * 
	 * @return Eclipse block
	 */
	public Block getBlock() {
		return this.block;
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
			if (v.validIn(pc)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * Get exception handlers.
	 * 
	 * @return exception handlers
	 */
	public Exc[] getExcs() {
		return this.excs;
	}

	/**
	 * Get frames.
	 * 
	 * @return frames
	 */
	public Object getFrames() {
		return this.frames;
	}

	/**
	 * Get local variable (from frame).
	 * 
	 * @param reg
	 *            register
	 * @param pc
	 *            pc
	 * @return local variable (from frame)
	 */
	public V getFrameVar(final int reg, final int pc) {
		return this.frames[pc].get(reg);
	}

	/**
	 * Get immediate dominator (IDom) for basic block.
	 * 
	 * @param bb
	 *            basic block
	 * @return immediate domminator (IDom) for basic block
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
	 * Get max locals.
	 * 
	 * @return max locals
	 */
	public int getMaxLocals() {
		return this.maxLocals;
	}

	/**
	 * Get max stack size.
	 * 
	 * @return max stack size
	 */
	public int getMaxStack() {
		return this.maxStack;
	}

	/**
	 * Get method declaration.
	 * 
	 * @return method declaration
	 */
	public MD getMd() {
		return this.md;
	}

	/**
	 * Get operations.
	 * 
	 * @return operations
	 */
	public Op[] getOps() {
		return this.ops;
	}

	/**
	 * Get output frame for operation. Doesn't (and must not) work for control flow statements.
	 * 
	 * @param op
	 *            operation
	 * @return output frame
	 */
	public Frame getOutFrame(final Op op) {
		return this.frames[op.getPc() + 1];
	}

	/**
	 * Get postordered basic blocks.
	 * 
	 * @return postordered basic blocks
	 */
	public List<BB> getPostorderedBbs() {
		return this.postorderedBbs;
	}

	/**
	 * Get start basic block. Should be the final block after all transformations.
	 * 
	 * @return start basic block
	 */
	public BB getStartBb() {
		return this.startBb;
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
	 * Should transformer ignore this?
	 * 
	 * @return true - ignore this
	 */
	public boolean isIgnore() {
		return this.error || this.ops == null || this.ops.length == 0 || this.block == null;
	}

	/**
	 * New basic block.
	 * 
	 * @param opPc
	 *            first operation PC
	 * 
	 * @return basic block
	 */
	public BB newBb(final int opPc) {
		return new BB(this, opPc);
	}

	/**
	 * Post process local variables, e.g. extract method parameter. Better in read step than in
	 * transformator for generic base model. Dalvik has parameter names separately encoded.
	 */
	public void postProcessVars() {
		final M m = this.md.getM();
		final TD td = this.md.getTd();
		final T[] paramTs = m.getParamTs();

		if (this.vss == null) {
			this.vss = new V[this.maxLocals][];
		} else if (this.maxLocals < this.vss.length) {
			LOGGER.warning("Max registers less than biggest register with local variable info!");
		} else if (this.maxLocals > this.vss.length) {
			final V[][] newVarss = new V[this.maxLocals][];
			System.arraycopy(this.vss, 0, newVarss, 0, this.vss.length);
			this.vss = newVarss;
		}
		if (td.isDalvik()) {
			// Dalvik...function parameters right aligned
			int reg = this.maxLocals;
			for (int i = paramTs.length; i-- > 0;) {
				final T paramT = paramTs[i];
				if (paramT.isWide()) {
					--reg;
				}
				// parameter name was encoded in extra debug info, copy names
				// and parameter types to local vars
				final V[] vs = this.vss[--reg];
				if (vs != null) {
					LOGGER.warning("Found local variable info for method parameter '" + reg + "'!");
				}
				this.vss[reg] = new V[] { new V(paramT, m.getParamName(i), 0, this.ops.length) };
			}
			if (!m.checkAf(AF.STATIC)) {
				final V[] vs = this.vss[--reg];
				if (vs != null) {
					LOGGER.warning("Found local variable info for method parameter '" + reg
							+ "' (this)!");
				}
				this.vss[reg] = new V[] { new V(td.getT(), "this", 0, this.ops.length) };
			}
			return;
		}
		// JVM...function parameters left aligned
		int reg = 0;
		if (!m.checkAf(AF.STATIC)) {
			final V[] vs = this.vss[reg];
			if (vs != null) {
				if (vs.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter '"
							+ reg + "' (this)!");
				}
				++reg;
			} else {
				this.vss[reg++] = new V[] { new V(td.getT(), "this", 0, this.ops.length) };
			}
		}
		for (int i = 0; i < paramTs.length; ++i) {
			final T paramT = paramTs[i];
			final V[] vs = this.vss[reg];
			if (vs != null) {
				if (vs.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter '"
							+ reg + "'!");
				}
				m.setParamName(i, vs[0].getName());
				++reg;
			} else {
				this.vss[reg++] = new V[] { new V(paramT, m.getParamName(i), 0, this.ops.length) };
			}
			if (paramT.isWide()) {
				++reg;
			}
		}
	}

	/**
	 * Set block.
	 * 
	 * @param block
	 *            block
	 */
	public void setBlock(final Block block) {
		this.block = block;
	}

	/**
	 * Set flag for error occured.
	 * 
	 * @param error
	 *            error occured
	 */
	public void setError(final boolean error) {
		this.error = error;
	}

	/**
	 * Set exception handlers.
	 * 
	 * @param excs
	 *            exception handlers
	 */
	public void setExcs(final Exc[] excs) {
		this.excs = excs;
	}

	/**
	 * Set frames.
	 * 
	 * @param frames
	 *            frames
	 */
	public void setFrames(final Frame[] frames) {
		this.frames = frames;
	}

	/**
	 * Set operations.
	 * 
	 * @param ops
	 *            operations
	 */
	public void setOps(final Op[] ops) {
		this.ops = ops;
	}

	/**
	 * Set start basic block.
	 * 
	 * @param startBb
	 *            start basic block
	 */
	public void setStartBb(final BB startBb) {
		this.startBb = startBb;
	}

	@Override
	public String toString() {
		return getMd().toString() + " (ops: " + this.ops.length + ", regs: " + this.maxLocals + ")";
	}

}