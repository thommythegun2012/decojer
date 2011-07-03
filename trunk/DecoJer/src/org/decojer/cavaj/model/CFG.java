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
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.bytecode.AttributeInfo;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.StackMap;
import javassist.bytecode.StackMapTable;

import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.eclipse.jdt.core.dom.Block;

/**
 * Control Flow Graph.
 * 
 * @author André Pankraz
 */
public class CFG {

	private final static Logger LOGGER = Logger.getLogger(CFG.class.getName());

	private final Block block;

	private final CodeAttribute codeAttribute;

	/**
	 * Array with Immediate Dominators, index is postorder.
	 */
	private BB[] iDoms;

	public LineNumberAttribute lineNumberAttribute;

	public LocalVariableAttribute localVariableAttribute;

	private LocalVariableAttribute localVariableTypeAttribute;

	private final MD md;

	private StackMap stackMap;

	private StackMapTable stackMapTable;

	/**
	 * Array with postordered basic blocks.
	 */
	private List<BB> postorderedBbs;

	private BB startBb;

	/**
	 * Constructor. Init start basic block, may change through splitting.
	 * 
	 * @param md
	 *            method declaration
	 * @param block
	 *            Eclipse block
	 * @param codeAttribute
	 *            code attribute
	 */
	@SuppressWarnings("unchecked")
	public CFG(final MD md, final Block block, final CodeAttribute codeAttribute) {
		assert md != null;

		this.md = md;
		this.block = block;
		this.codeAttribute = codeAttribute;
		if (codeAttribute != null) {
			for (final AttributeInfo attributeInfo : (List<AttributeInfo>) codeAttribute
					.getAttributes()) {
				final String attributeTag = attributeInfo.getName();
				if (LineNumberAttribute.tag.equals(attributeTag)) {
					this.lineNumberAttribute = (LineNumberAttribute) attributeInfo;
				} else if (LocalVariableAttribute.tag.equals(attributeTag)) {
					this.localVariableAttribute = (LocalVariableAttribute) attributeInfo;
				} else if (LocalVariableAttribute.typeTag.equals(attributeTag)) {
					this.localVariableTypeAttribute = (LocalVariableAttribute) attributeInfo;
				} else if (StackMap.tag.equals(attributeTag)) {
					this.stackMap = (StackMap) attributeInfo;
				} else if (StackMapTable.tag.equals(attributeTag)) {
					this.stackMapTable = (StackMapTable) attributeInfo;
				} else {
					// TODO parent
					LOGGER.log(Level.WARNING, "Unknown code attribute tag '"
							+ attributeTag + "' in '" + getMd().getSignature()
							+ "'!");
				}
			}
		}
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
	 * Get code.
	 * 
	 * @return code
	 */
	public byte[] getCode() {
		return this.codeAttribute.getCode();
	}

	/**
	 * Get const pool.
	 * 
	 * @return const pool
	 */
	public ConstPool getConstPool() {
		return this.codeAttribute.getConstPool();
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
	 * Get method declaration.
	 * 
	 * @return method declaration
	 */
	public MD getMd() {
		return this.md;
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
	 * @param pcBBs
	 *            map: pc -> basic block
	 * @return target basic block
	 */
	public BB getTargetBb(final int pc, final Map<Integer, BB> pcBBs) {
		// operation with pc must be in this basic block
		final BB targetBB = pcBBs.get(pc);
		// no basic block found for pc yet
		if (targetBB == null) {
			return null;
		}
		int i = 0;
		// find operation index in basic block with given pc
		while (i < targetBB.getOperationsSize()) {
			if (targetBB.getOperation(i).getOpPc() == pc) {
				break;
			}
			++i;
		}
		// first operation in basic block has target pc, return basic block,
		// no split necessary
		if (i == 0) {
			return targetBB;
		}
		// split basic block, new incoming block, adapt basic block pcs
		final BB splitSourceBB = newBb(targetBB.getOpPc());
		targetBB.setOpPc(pc);
		if (this.startBb == targetBB) {
			this.startBb = splitSourceBB;
		}
		// first preserve predecessors...
		targetBB.movePredBbs(splitSourceBB);
		// ...then add connection
		splitSourceBB.addSucc(targetBB, null);

		// move operations, change pc map
		while (i-- > 0) {
			final Operation vmOperation = targetBB.removeOperation(0);
			splitSourceBB.addOperation(vmOperation);
			pcBBs.put(vmOperation.getOpPc(), splitSourceBB);
		}
		return targetBB;
	}

	/**
	 * Get variable name for index. If no local variable name info is found,
	 * return "arg[index]".
	 * 
	 * @param i
	 *            index
	 * @return variable name
	 */
	public String getVariableName(final int i) {
		if (this.localVariableAttribute != null
				&& this.localVariableAttribute.tableLength() > i) {
			try {
				return this.localVariableAttribute.variableName(i);
			} catch (final Exception e) {
				// TODO parent
				LOGGER.log(Level.WARNING,
						"Couldn't read variable name for index '" + i
								+ "' in '" + getMd().getDescriptor() + "'!", e);
			}
		}
		// TODO
		return "arg" + i;
	}

	/**
	 * Initialize.
	 */
	public void init() {
		this.startBb = newBb(0);
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

}