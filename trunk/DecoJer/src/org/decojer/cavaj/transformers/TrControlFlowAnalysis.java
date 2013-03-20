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

import static org.decojer.cavaj.utils.Expressions.getOp;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Cond;
import org.decojer.cavaj.model.code.structs.Loop;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.code.structs.Switch;
import org.decojer.cavaj.model.code.structs.Switch.Kind;
import org.decojer.cavaj.model.code.structs.Sync;

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
		// JDK 1 && 2 create wrong line numbers for final return (see DecTestIfStmt)

		// second part is trick against things like iteration in loop last:
		final boolean negated = falseSucc.isBefore(trueSucc) || trueSucc.isBefore(head);
		final BB firstSucc = negated ? falseSucc : trueSucc;
		final BB secondSucc = negated ? trueSucc : falseSucc;
		final Boolean firstValue = negated;
		final Boolean secondValue = !negated;

		final Set<BB> firstFollows = Sets.newHashSet();
		final List<BB> firstMembers = Lists.newArrayList();
		findBranch(cond, firstSucc, firstMembers, firstFollows);

		// no else basic blocks: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (firstFollows.isEmpty() || firstFollows.contains(secondSucc)) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setKind(negated ? Cond.Kind.IFNOT : Cond.Kind.IF);
			cond.setFollow(secondSucc);
			cond.addMembers(firstValue, firstMembers);
			return cond;
		}

		final Set<BB> secondFollows = Sets.newHashSet();
		final List<BB> secondMembers = Lists.newArrayList();
		findBranch(cond, secondSucc, secondMembers, secondFollows);

		// no else basic blocks: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (secondFollows.isEmpty() || secondFollows.contains(firstSucc)) {
			// also often in JDK 6 bytecode, especially in parent structs
			cond.setKind(negated ? Cond.Kind.IF : Cond.Kind.IFNOT);
			cond.setFollow(firstSucc);
			cond.addMembers(secondValue, secondMembers);
			return cond;
		}

		// end nodes are follows or breaks - no continues, returns, throws

		// JDK 6: end node with smallest order could be the follow
		BB firstEndNode = null;
		for (final BB endNode : firstFollows) {
			if (firstEndNode == null || endNode.isBefore(firstEndNode)) {
				firstEndNode = endNode;
			}
		}
		BB secondEndNode = null;
		for (final BB endNode : secondFollows) {
			if (secondEndNode == null || endNode.isBefore(secondEndNode)) {
				secondEndNode = endNode;
			}
		}

		// follow exists?
		if (firstEndNode == secondEndNode) {
			// normal stuff
			cond.setFollow(firstEndNode);
			cond.setKind(negated ? Cond.Kind.IFNOT_ELSE : Cond.Kind.IF_ELSE);
			cond.addMembers(firstValue, firstMembers);
			cond.addMembers(secondValue, secondMembers);
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
		assert tail != null; // FIXME can currently happen if endless-return-try-catch

		Loop.Kind headKind = null;
		BB headFollow = null;

		if (head.getStmts() == 1 && head.isCond()) {
			final BB falseSucc = head.getFalseSucc();
			final BB trueSucc = head.getTrueSucc();
			if (loop.isMember(trueSucc) && !loop.isMember(falseSucc)) {
				// JDK 6: true is member, opPc of pre head > next member,
				// leading goto
				headKind = Loop.Kind.WHILE;
				headFollow = falseSucc;
			} else if (loop.isMember(falseSucc) && !loop.isMember(trueSucc)) {
				// JDK 5: false is member, opPc of pre head < next member,
				// trailing goto (negated, check class javascript.Decompiler)
				headKind = Loop.Kind.WHILENOT;
				headFollow = trueSucc;
			}
			// no proper pre head!
		}

		Loop.Kind tailKind = null;
		BB tailFollow = null;

		if (tail.isCond() && !tail.isLoopHead() && !head.isLoopLast()) {
			final BB falseSucc = tail.getFalseSucc();
			final BB trueSucc = tail.getTrueSucc();
			if (loop.isHead(trueSucc)) {
				tailKind = Loop.Kind.DO_WHILE;
				tailFollow = falseSucc;
			} else if (loop.isHead(falseSucc)) {
				tailKind = Loop.Kind.DO_WHILENOT;
				tailFollow = trueSucc;
			}
		}

		if (headKind != null && tailKind == null) {
			loop.setKind(headKind);
			loop.setFollow(headFollow);
			return loop;
		}
		if (headKind == null && tailKind != null) {
			loop.setKind(tailKind);
			loop.setFollow(tailFollow);
			return loop;
		}
		if (headKind != null && tailKind != null) {
			final List<BB> headMembers = Lists.newArrayList();
			final Set<BB> headFollows = Sets.newHashSet();
			findBranch(loop, headFollow, headMembers, headFollows);
			if (headFollows.contains(tailFollow)) {
				loop.setKind(tailKind);
				loop.setFollow(tailFollow);
				return loop;
			}
			final List<BB> tailMembers = Lists.newArrayList();
			final Set<BB> tailFollows = Sets.newHashSet();
			findBranch(loop, tailFollow, tailMembers, tailFollows);
			if (tailFollows.contains(headFollow)) {
				loop.setKind(headKind);
				loop.setFollow(headFollow);
				return loop;
			}
		}
		loop.setKind(Loop.Kind.ENDLESS);
		return loop;
	}

	private Switch createSwitchStruct(final BB head) {
		final Switch switchStruct = new Switch(head);

		// for case reordering we need this as changeable lists
		final List<E> caseOuts = head.getOuts();
		final int size = caseOuts.size();

		final Set<BB> follows = Sets.newHashSet();

		int hack = 10;
		cases: for (int i = 0; i < size && hack > 0; ++i) {
			final E caseOut = caseOuts.get(i);
			if (!caseOut.isSwitchCase()) {
				continue;
			}
			final BB caseBb = caseOut.getEnd();
			final List<E> ins = caseBb.getIns();

			boolean isFallThrough = false;
			if (ins.size() > 1) {
				// are we a follow?
				// - in this case _all_ incomings must come from _one_ previous case
				// - then we have to...
				// 1) insert current edge after previous case edge
				// 2) remove this edge as potential switch follow
				// 3) add current case BB explicitely as member
				int prevCaseIndex = -1;
				for (final E in : ins) {
					if (in == caseOut) {
						continue;
					}
					if (in.isBack()) {
						continue;
					}
					// 1) is direct break or continue
					// 2) is fall through in previous follow
					// 3) is fall through in later follow
					// 4) is incoming goto if nothing works anymore...
					final BB prevBb = in.getStart();
					for (int j = i; j-- > 0;) {
						if (!switchStruct.isMember(caseOuts.get(j).getValue(), prevBb)) {
							continue;
						}
						if (prevCaseIndex == j) {
							continue;
						}
						if (prevCaseIndex == -1) {
							prevCaseIndex = j;
							continue;
						}
						// multiple previous cases => we cannot be a fall-through and are a
						// real follow ... should just happen for _direct_ breaks or _empty_ default
						if (caseOut.isSwitchDefault()) {
							// TODO hmmm...could be a default with direct continue etc. -> no follow
							// if other follows...
							switchStruct.setKind(Kind.NO_DEFAULT);
							switchStruct.setFollow(caseBb);
						}
						continue cases;
					}
					if (prevCaseIndex == -1) {
						if (caseOut.isSwitchDefault()) {
							switchStruct.setKind(Kind.NO_DEFAULT);
							switchStruct.setFollow(caseBb);
							continue cases;
						}
						// we cannot be a fall-through yet...may be later
						caseOuts.remove(i--);
						--hack;
						caseOuts.add(caseOut); // as last for now
						continue cases;
					}
				}
				if (prevCaseIndex < i - 1) {
					caseOuts.remove(i);
					caseOuts.add(prevCaseIndex + 1, caseOut);
				}
				if (caseOut.isSwitchDefault() && follows.size() == 1 && follows.contains(caseBb)) {
					// TODO when we are the final fall-through without other follows...
					switchStruct.setKind(Kind.NO_DEFAULT);
					switchStruct.setFollow(caseBb);
					continue cases;
				}
				isFallThrough = true;
			}

			final List<BB> members = switchStruct.getMembers(caseOut.getValue());
			if (isFallThrough) {
				follows.remove(caseBb);
				members.add(caseBb);
			}
			findBranch(switchStruct, caseBb, members, follows);
		}
		if (switchStruct.getFollow() != null) {
			return switchStruct;
		}
		switchStruct.setKind(Switch.Kind.WITH_DEFAULT);
		// TODO end node with smallest order could be the follow
		BB switchFollow = null;
		for (final BB follow : follows) {
			if (switchFollow == null || follow.isBefore(switchFollow)) {
				switchFollow = follow;
			}
		}
		switchStruct.setFollow(switchFollow);
		return switchStruct;
	}

	private void createSyncStruct(final BB head) {
		final Sync sync = new Sync(head);

		// extract sync mutex
		final Op op = getOp(head.getFinalStmt());
		// this.cfg.getInFrame(op);
		// really? enter and exit have to be the same! how to check? combinations:
		// dup enter exit / dup store enter | store load enter -> load exit

		// exit must backtrack state to register-read for synchronized-enter
	}

	/**
	 * Add successors until unknown predecessors are encountered.
	 * 
	 * Check incoming edges first, because the start BB could be a direct continue or break!
	 * 
	 * Switch-case fall-throughs start with case BB as member.
	 * 
	 * @param struct
	 *            struct
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
			// TODO this is not sufficient...cond-follow with self-loop will be recognized as
			// follow, see commons-io:FileUtils#decode()
			if (bb.getIns().size() > 1) {
				// is there a none-member pred?
				for (final E in : bb.getIns()) {
					if (in.isBack()) {
						continue; // ignore incoming back edges, sub loop-heads belong to branch
					}
					final BB pred = in.getStart();
					if (members.contains(pred)) {
						continue;
					}
					if (pred == struct.getHead()) {
						if (members.isEmpty()) {
							continue;
						}
					} else if (!pred.hasPred(struct.getHead())) {
						return;
					}
					if (follows.contains(bb)) {
						return;
					}
					if (bb.isCatchHandler()) {
						// if all incoming catches are inside branch then the above pred-member
						// check was allready sucessful, but we will never be a follow
						return;
					}
					// multiple follows during iteration possible, reduce after #findBranch() to
					// single top follow
					follows.add(bb);
					return;
				}
				follows.remove(bb); // all pred are now members, maybe we where already here
			}
			members.add(bb);
		}
		for (final E out : bb.getOuts()) {
			if (out.isBack()) {
				continue;
			}
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
			if (out.isCatch()) {
				// exclude for now...if handler encloses whole inner loop sequence, the loop last
				// could be overwritten with final ret-back in handler
				continue;
			}
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
				loop.addMember(null, bb);
			}
			return true;
		}
		if (backEdge) {
			// find tail (no loopSucc!), pred member with biggest opPc
			// TODO or biggest line number or further structure analyzis?
			// TODO e.g. tail with >2 succ not possible, see warning
			if (loop.getLast() == null || loop.getLast().isBefore(bb)) {
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
			bb.sortOuts();

			// check loop first, could be a post / endless loop with additional sub struct heads
			if (bb.isLoopHead()) {
				if (createLoopStruct(bb).isPre()) {
					// exit: no additional struct head possible here
					continue;
				}
				// fall through: additional sub struct heads possible
			}
			if (bb.isSwitchHead()) {
				createSwitchStruct(bb);
				continue;
			}
			if (bb.isCond()) {
				if (bb.getStruct() instanceof Loop) {
					final Loop loopStruct = (Loop) bb.getStruct();
					if (loopStruct.isPost() && loopStruct.isLast(bb)) {
						// exit: conditional already used for post loops last condition
						continue;
					}
				}
				createCondStruct(bb);
				continue;
			}
			if (bb.isSyncHead()) {
				createSyncStruct(bb);
				continue;
			}
		}
	}

}