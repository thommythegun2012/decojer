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

import java.util.Iterator;
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
		final Cond condStruct = new Cond(head);

		final List<E> outs = head.getOuts();
		final int size = outs.size();

		E trueOut = null;
		E falseOut = null;
		for (int i = 0; i < size; ++i) {
			final E condOut = outs.get(i);
			if (!condOut.isSwitchCase()) {
				continue;
			}
			if (((Boolean) condOut.getValue()).booleanValue()) {
				assert trueOut == null;

				trueOut = condOut;
			} else {
				assert falseOut == null;

				falseOut = condOut;
			}
		}
		if (trueOut == null || falseOut == null) {
			log("Not a valid cond struct: " + head);
			return condStruct;
		}

		final Set<BB> follows = Sets.newHashSet();

		for (int i = 0; i < size; ++i) {
			final E condOut = outs.get(i);
			if (!condOut.isCond()) {
				continue;
			}
			final BB condBb = condOut.getEnd();

			final List<BB> members = condStruct.getMembers(condOut.getValue());
			findBranch(condStruct, condBb, members, follows);

			if (members.isEmpty()) {
				if (follows.isEmpty()) {
					// direct continue or break
					continue;
				}
				if (follows.contains(condBb)) {

				}
			}
			if (follows.isEmpty()) {
				// continue or break with statements
				// condBb.setF
			}
		}

		return condStruct;
	}

	private Cond createCondStructOld(final BB head) {
		final Cond cond = new Cond(head);

		final BB falseSucc = head.getFalseSucc();
		final BB trueSucc = head.getTrueSucc();

		// if-statement compilation hasn't changed with JDK versions (unlike boolean expressions)
		// means: negated if-expression, false successor contains if-body (PC & line before true),
		// special unnegated cases (with JDK 5 compiler?):
		// direct continue back-edges and break forward-edges (only possible with true)

		// we have to check all possibilities anyway...but prefer normal variants
		/*
		 * // check direct continue-back-edges first, no members if (trueSucc.getPostorder() >=
		 * head.getPostorder()) { // normal JDK continue-back-edge, only since JDK 5
		 * (target-indepandant) compiler? cond.setType(Cond.IF); cond.setFollow(falseSucc); return
		 * cond; } if (falseSucc.getPostorder() >= head.getPostorder()) { // no JDK, not really
		 * possible with normal JCND / JCMP conditionals
		 * log("Unexpected unnegated direct continue-back-edge."); cond.setType(Cond.IFNOT);
		 * cond.setFollow(falseSucc); return cond; }
		 */
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
		if (firstFollows.isEmpty()) {
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

		if (head.getStmts() == 1 && head.isCondOrPreLoopHead()) {
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

		if (tail.isCondOrPreLoopHead()) {
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

		final List<E> outs = head.getOuts();
		final int size = outs.size();

		int cases = 0;
		for (int i = 0; i < size; ++i) {
			if (outs.get(i).isSwitchCase()) {
				++cases;
			}
		}

		final Set<BB> follows = Sets.newHashSet();

		outer: for (int i = 0; i < size; ++i) {
			final E caseOut = outs.get(i);
			if (!caseOut.isSwitchCase()) {
				continue;
			}
			final BB caseBb = caseOut.getEnd();

			if (cases == 1 && (follows.isEmpty() || follows.contains(caseBb))) {
				switchStruct.setType(Switch.SWITCH);
				switchStruct.setFollow(caseBb);
				return switchStruct;
			}

			final List<BB> members = switchStruct.getMembers(caseOut.getValue());
			findBranch(switchStruct, caseBb, members, follows);

			if (members.isEmpty()) {
				if (follows.isEmpty()) {
					// continue or break
					continue;
				}
				if (follows.contains(caseBb)) {
					// first case can only have head as predecessor (no fall-through), elsewise try
					// case reordering; fall-through follow-cases can have multiple predecessors
					if (i == size - 1) { // endless-loop prevention
						log("Fall-through case '" + caseOut + "' is empty: '" + head);
					} else {
						log("Fall-through case '" + caseOut + "' must be reordered: '" + head);
						outs.remove(i--);
						outs.add(caseOut); // as last for now
					}
					continue;
				}
				log("Special case '" + caseOut + "' must be handled: '" + head);
				continue;
			}
			--cases;
			if (follows.isEmpty()) {
				continue;
			}
			// a follow has 2 incomings, one is head?
			// => is switch-follow or fall-through must follow!
			for (final Iterator<BB> it = follows.iterator(); it.hasNext();) {
				final BB follow = it.next();
				final List<E> followIns = follow.getIns();
				if (followIns.size() >= 2) {
					for (final E followIn : followIns) {
						if (followIn == caseOut || followIn.getStart() != head) {
							continue;
						}
						// switch-follow or fall-through found
						if (followIn.isSwitchDefault()) {
							// TODO not sufficient, count if a follow occurs multiple times!
							continue;
						}
						final List<BB> followCaseMembers = switchStruct.getMembers(followIn
								.getValue());
						if (!followCaseMembers.isEmpty()) {
							log("Fall-through case '" + caseOut
									+ "' cannot be target of multiple fall-throughs: '" + head);
						}

						followCaseMembers.add(follow);
						it.remove(); // remove as struct follow

						// move in to i + 1
						outs.remove(followIn);
						outs.add(i + 1, followIn);
						continue outer;
					}
				}
			}

		}
		switchStruct.setType(Switch.SWITCH_DEFAULT);
		// TODO end node with smallest order could be the follow
		BB switchFollow = null;
		for (final BB follow : follows) {
			if (switchFollow == null || switchFollow.getOrder() > follow.getOrder()) {
				switchFollow = follow;
			}
		}
		switchStruct.setFollow(switchFollow);
		return switchStruct;
	}

	/**
	 * Add successors until unknown predecessors are encountered.
	 * 
	 * Check incoming edges first, because the start BB could be a direct continue or break!
	 * 
	 * Switch-case fall-throughs start with case BB as member.
	 * 
	 * @param struct
	 *            enclosing struct
	 * @param bb
	 *            current BB
	 * @param members
	 *            found struct members
	 * @param follows
	 *            potential struct follows (no continues or breaks)
	 */
	private void findBranch(final Struct struct, final BB bb, final List<BB> members,
			final Set<BB> follows) {
		if (!members.contains(bb)) { // necessary check because of switch-case fall-throughs
			if (bb.getIns().size() > 1) {
				// is there a none-member prev?
				for (final E in : bb.getIns()) {
					if (in.isBack()) {
						continue; // ignore incoming back edges, sub loop-heads belong to branch
					}
					final BB prev = in.getStart();
					if (members.contains(prev)) {
						continue;
					}

					if (struct.getParent() == null || struct.getParent().isMember(prev)) {
						if (struct.getParent() instanceof Switch && struct.getParent().isFollow(bb)) {
							// prefer direct breaks for switches
							return;
						}
						follows.add(bb); // TODO only 1 possible?
						return;
					}
					// is break or continue, artificial block might be necessary!
					log("Java has single-entry structs, not allowed - GOTO necessary");
				}
				follows.remove(bb); // all prev are now members, maybe we where already here
			}
			members.add(bb);
		}
		for (final E out : bb.getOuts()) {
			final BB succ = out.getEnd();
			if (!members.contains(succ)) {
				// DFS
				findBranch(struct, succ, members, follows);
			}
		}
	}

	private boolean findLoop(final Loop loop, final BB bb, final Set<BB> traversed) {
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

	private void log(final String message) {
		LOGGER.warning(this.cfg.getMd() + ": " + message);
	}

	private void transform() {
		final List<BB> bbs = this.cfg.getPostorderedBbs();
		// for all nodes in _reverse_ postorder: find outer structs first
		for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			if (this.cfg.isLineInfo()) {
				bb.sortOuts();
			}
			// check loop first, could be a post / endless loop with additional sub struct heads
			if (bb.isLoopHead()) {
				if (createLoopStruct(bb).isPre()) {
					// no additional struct head possible here, fail fast
					continue;
				}
			}
			if (bb.isSwitchHead()) {
				createSwitchStruct(bb);
				continue;
			}
			if (bb.isCondOrPreLoopHead()) {
				if (bb.getStruct() instanceof Loop) {
					final Loop loopStruct = (Loop) bb.getStruct();
					if (loopStruct.isPost() && loopStruct.isLast(bb)) {
						continue;
					}
				}
				createCondStructOld(bb);
				continue;
			}
		}
	}

}