/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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
import java.util.Map;
import java.util.Set;

import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.eclipse.jdt.core.dom.Block;

/**
 * Control Flow Graph.
 * 
 * @author Andr� Pankraz
 */
public class CFG {

	private Block block;

	/**
	 * Array with Immediate Dominators, index is postorder.
	 */
	private BB[] iDoms;

	private final int maxStack;

	private final MD md;

	private Operation[] operations;

	/**
	 * Array with postordered basic blocks.
	 */
	private List<BB> postorderedBbs;

	private final int maxRegs;

	private BB startBb;

	/**
	 * Constructor. Init start basic block, may change through splitting.
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

		this.md = md;
		this.maxRegs = maxRegs;
		this.maxStack = maxStack;
		this.startBb = newBb(0);
	}

	private int _calculatePostorder(final int postorder, final BB bb,
			final Set<BB> traversed) {
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
	 * Get Eclipse block.
	 * 
	 * @return Eclipse block
	 */
	public Block getBlock() {
		return this.block;
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
	public Operation[] getOperations() {
		return this.operations;
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
	 * Get start basic block. Should be the final block after all
	 * transformations.
	 * 
	 * @return start basic block
	 */
	public BB getStartBb() {
		return this.startBb;
	}

	/**
	 * Get target basic block. Split if necessary, but keep outgoing part same
	 * (for later adding of outging edges).
	 * 
	 * @param pc
	 *            target pc
	 * @param pcBbs
	 *            map: pc -> basic block
	 * @return target basic block
	 */
	public BB getTargetBb(final int pc, final Map<Integer, BB> pcBbs) {
		// operation with pc must be in this basic block
		final BB targetBb = pcBbs.get(pc);
		// no basic block found for pc yet
		if (targetBb == null) {
			return null;
		}

		final List<Operation> operations = targetBb.getOperations();

		int i = 0;
		// find operation index in basic block with given pc
		while (i < operations.size()) {
			if (operations.get(i).getOpPc() == pc) {
				break;
			}
			++i;
		}
		// first operation in basic block has target pc, return basic block,
		// no split necessary
		if (i == 0) {
			return targetBb;
		}
		// split basic block, new incoming block, adapt basic block pcs
		final BB splitSourceBb = newBb(targetBb.getOpPc());
		targetBb.setOpPc(pc);
		if (this.startBb == targetBb) {
			this.startBb = splitSourceBb;
		}
		// first preserve predecessors...
		targetBb.movePredBbs(splitSourceBb);
		// ...then add connection
		splitSourceBb.addSucc(targetBb, null);

		// move operations, change pc map
		while (i-- > 0) {
			final Operation vmOperation = operations.remove(0);
			splitSourceBb.addOperation(vmOperation);
			pcBbs.put(vmOperation.getOpPc(), splitSourceBb);
		}
		return targetBb;
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
	 * Set block.
	 * 
	 * @param block
	 *            block
	 */
	public void setBlock(final Block block) {
		this.block = block;
	}

	/**
	 * Set operations.
	 * 
	 * @param operations
	 *            operations
	 */
	public void setOperations(final Operation[] operations) {
		this.operations = operations;
	}

}