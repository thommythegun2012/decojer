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
package org.decojer.cavaj.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.Var;
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

	private final int maxRegs;

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

	private Var[][] varss;

	/**
	 * Constructor.
	 * 
	 * @param md
	 *            method declaration
	 * @param maxRegs
	 *            max register number
	 * @param maxStack
	 *            max stack number
	 */
	public CFG(final MD md, final int maxRegs, final int maxStack) {
		assert md != null;
		assert maxRegs >= 0;
		assert maxStack >= 0;

		this.md = md;
		this.maxRegs = maxRegs;
		this.maxStack = maxStack;
	}

	private int _calculatePostorder(final int postorder, final BB bb, final Set<BB> traversed) {
		// DFS
		traversed.add(bb);
		int _postorder = postorder;
		for (final BB succBb : bb.getSuccBbs()) {
			if (traversed.contains(succBb)) {
				continue;
			}
			_postorder = _calculatePostorder(_postorder, succBb, traversed);
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
	public void addVar(final int reg, final Var var) {
		assert var != null;

		Var[] vars = null;
		if (this.varss == null) {
			this.varss = new Var[reg + 1][];
		} else if (reg >= this.varss.length) {
			final Var[][] newVarss = new Var[reg + 1][];
			System.arraycopy(this.varss, 0, newVarss, 0, this.varss.length);
			this.varss = newVarss;
		} else {
			vars = this.varss[reg];
		}
		if (vars == null) {
			vars = new Var[1];
		} else {
			final Var[] newVars = new Var[vars.length + 1];
			System.arraycopy(vars, 0, newVars, 0, vars.length);
			vars = newVars;
		}
		vars[vars.length - 1] = var;
		this.varss[reg] = vars;
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
				for (final BB p : this.postorderedBbs.get(b).getPredBbs()) {
					if (this.iDoms[p.getPostorder()] == null) {
						continue;
					}
					if (iDomNew == null) {
						iDomNew = p;
						continue;
					}
					iDomNew = intersectIDoms(p, iDomNew);
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
	public Var getFrameVar(final int reg, final int pc) {
		return this.frames[pc].getReg(reg);
	}

	/**
	 * Get immediate dominator (IDom) for basic block.
	 * 
	 * @param basicBlock
	 *            basic block
	 * @return immediate domminator (IDom) for basic block
	 */
	public BB getIDom(final BB basicBlock) {
		return this.iDoms[basicBlock.getPostorder()];
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
	 * Get max register number.
	 * 
	 * @return max register number
	 */
	public int getMaxRegs() {
		return this.maxRegs;
	}

	/**
	 * Get max stack number.
	 * 
	 * @return max stack number
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
	public Var getVar(final int reg, final int pc) {
		if (this.varss == null || reg >= this.varss.length) {
			return null;
		}
		final Var[] vars = this.varss[reg];
		if (vars == null) {
			return null;
		}
		for (int i = vars.length; i-- > 0;) {
			final Var var = vars[i];
			if (var.getStartPc() <= pc && (pc < var.getEndPc() || var.getEndPc() == 0)) {
				return var;
			}
		}
		return null;
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
		return this.error || this.ops == null || this.ops.length == 0;
	}

	/**
	 * New basic block.
	 * 
	 * @param opPc
	 *            operation pc
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

		if (this.varss == null) {
			this.varss = new Var[this.maxRegs][];
		} else if (this.maxRegs < this.varss.length) {
			LOGGER.warning("Max registers less than biggest register with local variable info!");
		} else if (this.maxRegs > this.varss.length) {
			final Var[][] newVarss = new Var[this.maxRegs][];
			System.arraycopy(this.varss, 0, newVarss, 0, this.varss.length);
			this.varss = newVarss;
		}
		if (td.isDalvik()) {
			// Dalvik...function parameters right aligned
			int reg = this.maxRegs;
			for (int i = paramTs.length; i-- > 0;) {
				final T paramT = paramTs[i];
				if (paramT.isWide()) {
					--reg;
				}
				// parameter name was encoded in extra debug info, copy names
				// and parameter types to local vars
				Var[] vars = this.varss[--reg];
				if (vars != null) {
					LOGGER.warning("Found local variable info for method parameter '" + reg + "'!");
				}
				// check
				vars = new Var[1];
				final Var var = new Var(paramT);
				var.setName(m.getParamName(i));
				vars[0] = var;
				this.varss[reg] = vars;
			}
			if (!m.checkAf(AF.STATIC)) {
				Var[] vars = this.varss[--reg];
				if (vars != null) {
					LOGGER.warning("Found local variable info for method parameter 'this'!");
				}
				// check
				vars = new Var[1];
				final Var var = new Var(td.getT());
				var.setName("this");
				vars[0] = var;
				this.varss[reg] = vars;
			}
			return;
		}
		// JVM...function parameters left aligned
		int reg = 0;
		if (!m.checkAf(AF.STATIC)) {
			Var[] vars = this.varss[reg];
			if (vars != null) {
				if (vars.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter 'this'!");
				}
				++reg;
			} else {
				vars = new Var[1];
				final Var var = new Var(td.getT());
				var.setName("this");
				vars[0] = var;
				this.varss[reg++] = vars;
			}
		}
		for (int i = 0; i < paramTs.length; ++i) {
			final T paramT = paramTs[i];
			Var[] vars = this.varss[reg];
			if (vars != null) {
				if (vars.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter '"
							+ reg + "'!");
				}
				m.setParamName(i, vars[0].getName());
				++reg;
			} else {
				vars = new Var[1];
				final Var var = new Var(paramT);
				var.setName(m.getParamName(i));
				vars[0] = var;
				this.varss[reg++] = vars;
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

}