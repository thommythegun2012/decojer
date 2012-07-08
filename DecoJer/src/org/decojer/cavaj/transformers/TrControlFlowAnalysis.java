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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

/**
 * Transformer "Control Flow Analysis".
 * 
 * @author André Pankraz
 */
public final class TrControlFlowAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrControlFlowAnalysis.class.getName());

	// add successors till unknown predecessor node
	private static void dfsFindFollows(final List<BB> members, final Set<BB> follows, final BB bb,
			final Struct struct) {
		// check important for switch fall-throughs with early member addition
		if (!members.contains(bb)) {
			if (bb.getIns().size() <= 1) {
				// predecessor check unnecessary, but is also necessary as
				// startup for branch member search
				members.add(bb);
			} else {
				for (final E in : bb.getIns()) {
					if (in.isBack()) {
						// ignore back edges
						continue;
					}
					if (!members.contains(in.getStart())) {
						follows.add(bb);
						return;
					}
				}
				follows.remove(bb);
				members.add(bb);
			}
		}
		for (final E out : bb.getOuts()) {
			final BB succ = out.getEnd();
			if (members.contains(succ)) {
				continue;
			}
			if (out.isBack()) {
				// back edge to no-member successor, outer struct allready known
				// TODO handle pre loop continue
				continue;
			}
			// not from succ, follow doesn't know
			if (struct != null) {
				if (struct instanceof Loop && ((Loop) struct).isLast(succ)) {
					// TODO handle post loop continue
					continue;
				}
				if (struct.isFollow(succ)) {
					// TODO handle break
					continue;
				}
			}
			// DFS
			dfsFindFollows(members, follows, succ, struct);
		}
	}

	private static boolean dfsFindInnerLoopMembers(final Loop loop, final BB bb,
			final Set<BB> traversed) {
		// DFS
		traversed.add(bb);
		boolean loopSucc = false;
		boolean backEdge = false;
		for (final E out : bb.getOuts()) {
			if (out.isBack()) {
				// back edge (continue, tail, inner loop, outer label-continue)
				if (out.getEnd() != loop.getHead()) {
					// unimportant back edge (inner loop, outer label-continue)
					continue;
				}
				backEdge = true;
				// don't track this edge any further
				continue;
			}
			final BB succ = out.getEnd();
			if (loop.isMember(succ)) {
				loopSucc = true;
				continue;
			}
			if (traversed.contains(succ)) {
				continue;
			}
			// DFS
			if (dfsFindInnerLoopMembers(loop, succ, traversed)) {
				loopSucc = true;
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
		// check if condition is used for loop struct...
		final Struct struct = bb.getStruct();
		if (struct instanceof Loop) {
			final Loop loop = (Loop) struct;

			if (loop.isHead(bb) && loop.isPre()) {
				// no additional cond head possible
				return false;
			}
			if (loop.isLast(bb) && loop.isPost()) {
				// no additional cond head possible
				return false;
			}
		}
		return true;
	}

	private static boolean isLoopHead(final BB bb) {
		for (final E in : bb.getIns()) {
			if (in.isBack()) {
				// must be a back edge (eg. self loop), in Java only possible for loop heads
				return true;
			}
		}
		return false;
	}

	private static boolean isSwitchHead(final BB bb) {
		// don't use successor number as indicator, normal switch with 2 successors
		// (JDK 6: 1 case and default) possible
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

	private void findCondMembers(final Cond cond) {
		final BB head = cond.getHead();

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
			return;
		}
		if (falseSucc.getPostorder() >= head.getPostorder()) {
			// no JDK, not really possible with normal JCND / JCMP conditionals
			log("Unexpected unnegated direct continue-back-edge.");
			cond.setType(Cond.IFNOT);
			cond.setFollow(falseSucc);
			return;
		}

		final boolean negated = falseSucc.getOrder() < trueSucc.getOrder();
		final BB firstSucc = negated ? falseSucc : trueSucc;
		final BB secondSucc = negated ? trueSucc : falseSucc;
		final Boolean firstValue = negated;
		final Boolean secondValue = !negated;

		final List<BB> firstMembers = new ArrayList<BB>();
		final Set<BB> firstFollows = new HashSet<BB>();
		dfsFindFollows(firstMembers, firstFollows, firstSucc, cond.getParent());

		// no else basic blocks
		if (firstFollows.contains(secondSucc)) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			for (final BB bb : firstMembers) {
				cond.addMember(firstValue, bb);
			}
			cond.setFollow(secondSucc);
			return;
		}

		// TODO only a trick for now, not ready!!!
		// e.g. if-continues, if-returns, if-throws => no else necessary
		if (firstFollows.size() == 0) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			for (final BB bb : firstMembers) {
				cond.addMember(firstValue, bb);
			}
			cond.setFollow(secondSucc);
			return;
		}

		final List<BB> secondMembers = new ArrayList<BB>();
		final Set<BB> secondFollows = new HashSet<BB>();
		dfsFindFollows(secondMembers, secondFollows, secondSucc, cond.getParent());

		if (secondFollows.contains(firstSucc)) {
			// really bad, order is wrong!
			log("Order preservation not possible for cond:\n?" + cond);
			cond.setType(negated ? Cond.IF : Cond.IFNOT);
			for (final BB bb : secondMembers) {
				cond.addMember(secondValue, bb);
			}
			cond.setFollow(firstSucc);
			return;
		}

		// end nodes are follows or breaks, no continues, returns, throws
		for (final BB bb : firstMembers) {
			cond.addMember(firstValue, bb);
		}
		for (final BB bb : secondMembers) {
			cond.addMember(secondValue, bb);
		}

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
			return;
		}
		// only if unrelated conditional tails???
		log("Unknown struct, no common follow for:\n" + cond);
	}

	private void findLoopMembers(final Loop loop) {
		final BB head = loop.getHead();

		dfsFindInnerLoopMembers(loop, head, new HashSet<BB>());

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
			return;
		}
		if (headType == 0 && tailType > 0) {
			loop.setType(tailType);
			loop.setFollow(tailFollow);
			return;
		}
		if (headType > 0 && tailType > 0) {
			final List<BB> headMembers = new ArrayList<BB>();
			final Set<BB> headEndNodes = new HashSet<BB>();
			dfsFindFollows(headMembers, headEndNodes, headFollow, loop.getParent());
			if (headEndNodes.contains(tailFollow)) {
				loop.setType(tailType);
				loop.setFollow(tailFollow);
				return;
			}
			final List<BB> tailMembers = new ArrayList<BB>();
			final Set<BB> tailEndNodes = new HashSet<BB>();
			dfsFindFollows(tailMembers, tailEndNodes, tailFollow, loop.getParent());
			if (tailEndNodes.contains(headFollow)) {
				loop.setType(headType);
				loop.setFollow(headFollow);
				return;
			}
		}
		loop.setType(Loop.ENDLESS);
	}

	private void findSwitchMembers(final Switch switchStruct) {
		final BB head = switchStruct.getHead();

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
			return;
		}
		if (firstIndex == -1) {
			log("Switch with head '" + head
					+ "' has no case branch with 1 predecessor, necessary for first case!");
			return;
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

		final Set<BB> endNodes = new HashSet<BB>();
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

			final List<BB> members = new ArrayList<BB>();

			if (succ.getIns().size() > 1) {
				// fall-through follow case?
				if (!endNodes.remove(succ)) {
					log("TODO Case reordering necessary? No proper follow case!");
				}
				members.add(succ);
			}

			dfsFindFollows(members, endNodes, succ, switchStruct.getParent());
			for (final BB bb : members) {
				switchStruct.addMember(outs.get(i).getValue(), bb);
			}
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
				final Loop loop = new Loop(bb);
				findLoopMembers(loop);
				if (loop.getType() == Loop.WHILE || loop.getType() == Loop.WHILENOT) {
					// no additional struct head possible
					continue;
				}
			}
			if (isSwitchHead(bb)) {
				final Switch switchStruct = new Switch(bb);
				findSwitchMembers(switchStruct);
				continue;
			}
			if (isCondHead(bb)) {
				final Cond cond = new Cond(bb);
				findCondMembers(cond);
				continue;
			}
		}
	}

}