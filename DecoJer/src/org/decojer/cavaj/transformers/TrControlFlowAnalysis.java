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
package org.decojer.cavaj.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.structs.Cond;
import org.decojer.cavaj.model.code.structs.Loop;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.code.structs.Switch;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Transformer: Control Flow Analysis.
 * 
 * @author André Pankraz
 */
public final class TrControlFlowAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrControlFlowAnalysis.class.getName());

	/**
	 * Add successors till unknown predecessor node. Check incoming edges before if this is a struct
	 * member, switch-fall-throughs and other branches have different preconditions for first node!
	 * 
	 * @param struct
	 *            enclosing struct
	 * @param bb
	 *            current BB
	 * @param members
	 *            found struct members
	 * @param follows
	 *            potential struct follows (no continues or outer breaks)
	 */
	private static void findBranch(final Struct struct, final BB bb, final List<BB> members,
			final Set<BB> follows) {
		members.add(bb);
		for (final E out : bb.getOuts()) {
			if (out.isBack()) {
				continue; // "continue" is no follow or member
			}
			final BB succ = out.getEnd();
			if (members.contains(succ)) {
				continue;
			}
			if (succ.getIns().size() != 1) {
				for (final E in : succ.getIns()) {
					final BB prev = in.getStart();
					if (members.contains(prev)) {
						// includes prev == bb,
						// cannot check isHead(), "if" without "else" would include follow node
						continue;
					}

					// TODO outer breaks (loop always?) and block splits?! they are no follows
					if (succ.getStruct() != struct.getParent()
							&& !struct.getParent().isBreakTarget(succ)) {
						// leaving parent struct...
						return;
					}

					follows.add(succ);
					return; // stop DFS
				}
				follows.remove(succ); // often does nothing
			}
			// DFS
			findBranch(struct, succ, members, follows);
		}
	}

	private static boolean findLoop(final Loop loop, final BB bb, final Set<BB> traversed) {
		// DFS
		traversed.add(bb);
		boolean loopSucc = false;
		boolean backEdge = false;
		for (final E out : bb.getOuts()) {
			if (out.isBack()) {
				// back edge (continue, tail, inner loop, outer label-continue)
				if (out.getEnd() == loop.getHead()) {
					backEdge = true;
				}
				// don't track this edge any further
				continue;
			}
			final BB succ = out.getEnd();
			if (loop.isMember(succ)) {
				loopSucc = true;
				continue;
			}
			if (!traversed.contains(succ)) {
				// DFS
				if (findLoop(loop, succ, traversed)) {
					loopSucc = true;
				}
			}
		}
		if (loopSucc) {
			// back edge too? => must be a continue => normal member
			if (loop.getHead() != bb) {
				loop.addMember(bb);
			}
			return true;
		}
		if (backEdge) {
			// find tail (no loopSucc!), pred member with biggest opPc
			// TODO or biggest line number or further structure analyzis?
			// TODO e.g. tail with >2 succ not possible, see warning
			if (loop.getLast() == null || loop.getLast().getOrder() < bb.getOrder()) {
				loop.setLast(bb);
			}
			return true;
		}
		return false;
	}

	private static boolean isCondHead(final BB bb) {
		if (!bb.isFinalStmtCond()) {
			return false;
		}
		// check if conditional is already used for an enclosing loop struct
		final Struct struct = bb.getStruct();
		if (!(struct instanceof Loop)) {
			return true;
		}
		// check if BB is target for continue is not sufficient: no endless here
		final Loop loop = (Loop) struct;
		if (loop.isPost()) {
			return !loop.isLast(bb);
		}
		if (loop.isPre()) {
			return !loop.isHead(bb); // false can never happen here, fail fast in transform()
		}
		return true;
	}

	private static boolean isLoopHead(final BB bb) {
		// at least one incoming edge must be a back edge (self loop possible), in Java only
		// possible for loop heads
		for (final E in : bb.getIns()) {
			if (in.isBack()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSwitchHead(final BB bb) {
		// don't use successor number as indicator, normal switch with 2 successors
		// (JVM 6: 1 case and default) possible
		return bb.isFinalStmtSwitch();
	}

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrControlFlowAnalysis(cfg).transform();
	}

	private final CFG cfg;

	private TrControlFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private Cond createCondStruct(final BB head) {
		final Cond cond = new Cond(head);

		final BB falseSucc = head.getFalseSucc();
		final BB trueSucc = head.getTrueSucc();

		// if-statement compilation hasn't changed with JDK versions (unlike boolean expressions)
		// means: negated if-expression, false successor contains if-body (PC & line before true),
		// special unnegated cases (with JDK 5 compiler?):
		// direct continue back-edges and break forward-edges (only possible with true)

		// we have to check all possibilities anyway...but prefer normal variants

		// check direct continue-back-edges first, no members
		if (trueSucc.getPostorder() >= head.getPostorder()) {
			// normal JDK continue-back-edge, only since JDK 5 (target-indepandant) compiler?
			cond.setType(Cond.IF);
			cond.setFollow(falseSucc);
			return cond;
		}
		if (falseSucc.getPostorder() >= head.getPostorder()) {
			// no JDK, not really possible with normal JCND / JCMP conditionals
			log("Unexpected unnegated direct continue-back-edge.");
			cond.setType(Cond.IFNOT);
			cond.setFollow(falseSucc);
			return cond;
		}

		final boolean negated = falseSucc.getOrder() < trueSucc.getOrder();
		final BB firstSucc = negated ? falseSucc : trueSucc;
		final BB secondSucc = negated ? trueSucc : falseSucc;
		final Boolean firstValue = negated;
		final Boolean secondValue = !negated;

		final Set<BB> firstFollows = Sets.newHashSet();
		if (firstSucc.getIns().size() == 1) {
			findBranch(cond, firstSucc, cond.getMembers(firstValue), firstFollows);
		}

		// no else basic blocks
		if (firstFollows.contains(secondSucc)) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			cond.setFollow(secondSucc);
			return cond;
		}

		// TODO only a trick for now, not ready!!!
		// e.g. if-continues, if-returns, if-throws => no else necessary
		if (firstFollows.size() == 0) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			cond.setFollow(secondSucc);
			return cond;
		}

		final Set<BB> secondFollows = Sets.newHashSet();
		if (secondSucc.getIns().size() == 1) {
			findBranch(cond, secondSucc, cond.getMembers(secondValue), secondFollows);
		}

		if (secondFollows.contains(firstSucc)) {
			// also often in JDK 6 bytecode, especially in parent structs
			cond.setType(negated ? Cond.IF : Cond.IFNOT);
			cond.setFollow(firstSucc);
			return cond;
		}

		// end nodes are follows or breaks, no continues, returns, throws

		// JDK 6: end node with smallest order could be the follow
		BB firstEndNode = null;
		for (final BB endNode : firstFollows) {
			if (firstEndNode == null || firstEndNode.getOrder() > endNode.getOrder()) {
				firstEndNode = endNode;
			}
		}
		BB secondEndNode = null;
		for (final BB endNode : secondFollows) {
			if (secondEndNode == null || secondEndNode.getOrder() > endNode.getOrder()) {
				secondEndNode = endNode;
			}
		}

		// follow exists?
		if (firstEndNode == secondEndNode) {
			// normal stuff
			cond.setFollow(firstEndNode);
			cond.setType(negated ? Cond.IFNOT_ELSE : Cond.IF_ELSE);
			return cond;
		}
		// only if unrelated conditional tails???
		log("Unknown struct, no common follow for:\n" + cond);
		return cond;
	}

	private Loop createLoopStruct(final BB head) {
		final Loop loop = new Loop(head);

		findLoop(loop, head, Sets.<BB> newHashSet());

		final BB tail = loop.getLast();
		assert tail != null;

		int headType = 0;
		BB headFollow = null;

		// WHILE && FOR => only 1 head statement because of iteration back edge,
		// FOR has trailing ExpressionStatements in the loop end node

		// TODO while (<inlineAssignment> > 0) - must inline agressively here

		if (head.getStmts() == 1 && head.isFinalStmtCond()) {
			final BB falseSucc = head.getFalseSucc();
			final BB trueSucc = head.getTrueSucc();
			if (loop.isMember(trueSucc) && !loop.isMember(falseSucc)) {
				// JDK 6: true is member, opPc of pre head > next member,
				// leading goto
				headType = Loop.WHILE;
				headFollow = falseSucc;
			} else if (loop.isMember(falseSucc) && !loop.isMember(trueSucc)) {
				// JDK 5: false is member, opPc of pre head < next member,
				// trailing goto (negated, check class javascript.Decompiler)
				headType = Loop.WHILENOT;
				headFollow = trueSucc;
			}
			// no proper pre head!
		}

		int tailType = 0;
		BB tailFollow = null;

		if (tail.isFinalStmtCond()) {
			final BB falseSucc = tail.getFalseSucc();
			final BB trueSucc = tail.getTrueSucc();
			if (loop.isHead(trueSucc)) {
				tailType = Loop.DO_WHILE;
				tailFollow = falseSucc;
			} else if (loop.isHead(falseSucc)) {
				tailType = Loop.DO_WHILENOT;
				tailFollow = trueSucc;
			}
		}

		if (headType > 0 && tailType == 0) {
			loop.setType(headType);
			loop.setFollow(headFollow);
			return loop;
		}
		if (headType == 0 && tailType > 0) {
			loop.setType(tailType);
			loop.setFollow(tailFollow);
			return loop;
		}
		if (headType > 0 && tailType > 0) {
			final List<BB> headMembers = Lists.newArrayList();
			final Set<BB> headFollows = Sets.newHashSet();
			findBranch(loop, headFollow, headMembers, headFollows);
			if (headFollows.contains(tailFollow)) {
				loop.setType(tailType);
				loop.setFollow(tailFollow);
				return loop;
			}
			final List<BB> tailMembers = Lists.newArrayList();
			final Set<BB> tailFollows = Sets.newHashSet();
			findBranch(loop, tailFollow, tailMembers, tailFollows);
			if (tailFollows.contains(headFollow)) {
				loop.setType(headType);
				loop.setFollow(headFollow);
				return loop;
			}
		}
		loop.setType(Loop.ENDLESS);
		return loop;
	}

	private Switch createSwitchStruct(final BB head) {
		final Switch switchStruct = new Switch(head);

		// cases with branches and values, in normal mode in correct order
		final List<E> outs = head.getSwitchOuts();

		int defaultIndex = -1;
		// first case can only have head as predecessor, else try case
		// reordering; fall-through follow-cases can have multiple predecessors
		int firstIndex = -1;
		// short check and find first node with 1 predecessor und default case
		final int size = outs.size();
		for (int i = 0; i < size; ++i) {
			final BB succ = outs.get(i).getEnd();
			if (succ.getIns().size() == 1 && firstIndex == -1) {
				firstIndex = i;
			}
			// assert preds.contains(head);

			if (Arrays.asList((Integer[]) outs.get(i).getValue()).contains(null)) {
				assert defaultIndex == -1 : "Double Default Case!";

				defaultIndex = i;
			}
		}
		if (defaultIndex == -1) {
			log("Switch with head '" + head + "' has no default branch!");
			return switchStruct;
		}
		if (firstIndex == -1) {
			log("Switch with head '" + head
					+ "' has no case branch with 1 predecessor, necessary for first case!");
			return switchStruct;
		} else if (firstIndex != 0) {
			log("Switch with head '" + head
					+ "' has no first case branch with 1 predecessor, reordering!");
			final E removedOut = outs.remove(firstIndex);
			outs.add(0, removedOut);
		}

		// TODO currently only quick and dirty checks
		int type = 0;
		if (defaultIndex != size - 1) {
			// not last case branch?
			type = Switch.SWITCH_DEFAULT;
		}
		final BB defaultBb = outs.get(defaultIndex).getEnd();
		if (defaultBb.getIns().size() == 1) {
			// no fall-through follow case and no switch follow
			type = Switch.SWITCH_DEFAULT;
		}

		final Set<BB> endNodes = Sets.newHashSet();
		for (int i = 0; i < size; ++i) {
			if (i == size - 1) {
				if (type == 0) {
					type = Switch.SWITCH;
				}
				if (type == Switch.SWITCH) {
					break;
				}
			}
			final BB succ = outs.get(i).getEnd();

			if (succ.getIns().size() > 1) {
				// fall-through follow case?
				if (!endNodes.remove(succ)) {
					log("TODO Case reordering necessary? No proper follow case!");
				}
			}

			findBranch(switchStruct, succ, switchStruct.getMembers(outs.get(i).getValue()),
					endNodes);
			if (endNodes.contains(defaultBb) && i < size - 2) {
				type = Switch.SWITCH;
			}
		}
		switchStruct.setType(type);
		if (type == Switch.SWITCH) {
			switchStruct.setFollow(defaultBb);
		} else {
			// TODO end node with smallest order could be the follow
			BB switchEndNode = null;
			for (final BB endNode : endNodes) {
				if (switchEndNode == null || switchEndNode.getOrder() > endNode.getOrder()) {
					switchEndNode = endNode;
				}
			}
			switchStruct.setFollow(switchEndNode);
		}
		return switchStruct;
	}

	private void log(final String message) {
		LOGGER.warning(this.cfg.getMd() + ": " + message);
	}

	private void transform() {
		final List<BB> bbs = this.cfg.getPostorderedBbs();
		// for all nodes in _reverse_ postorder: find outer structs first
		for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			// if (isCatchHead(bb)) {
			// is also a loop header? => we have to check if for endless / post we enclose the
			// tail too
			// }

			// check loop first, could be a post / endless loop with additional sub struct heads
			if (isLoopHead(bb)) {
				if (createLoopStruct(bb).isPre()) {
					// no additional struct head possible here, fail fast
					continue;
				}
			}
			if (isSwitchHead(bb)) {
				createSwitchStruct(bb);
				continue;
			}
			if (isCondHead(bb)) {
				createCondStruct(bb);
				continue;
			}
		}
	}

}