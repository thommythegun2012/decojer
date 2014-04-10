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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.MONITOR;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Cond;
import org.decojer.cavaj.model.code.structs.Loop;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.code.structs.Switch;
import org.decojer.cavaj.model.code.structs.Switch.Kind;
import org.decojer.cavaj.model.code.structs.Sync;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.utils.Expressions;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Transformer: Control Flow Analysis.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrControlFlowAnalysis {

	/**
	 * Is unhandled loop head?
	 *
	 * @param bb
	 *            BB
	 *
	 * @return {@code true} - is unhandled loop head
	 */
	private static List<BB> getLoopBackBbs(final BB bb) {
		// at least one incoming edge must be a back edge (self loop possible), in Java only
		// possible for loop heads (exclude JVM self catches)
		// nested post/endless loops with same head are possible and cannot be handled by continue!
		List<BB> backBbs = null;
		for (final E in : bb.getIns()) {
			if (!in.isBack() || in.isCatch()) {
				continue;
			}
			final Struct struct = in.getStart().getStruct();
			if (!(struct instanceof Loop) || ((Loop) struct).isPre() || !((Loop) struct).isLast(bb)) {
				if (backBbs == null) {
					backBbs = Lists.newArrayList();
				}
				backBbs.add(in.getStart());
			}
		}
		return backBbs;
	}

	/**
	 * Is switch head?
	 *
	 * @param bb
	 *            BB
	 *
	 * @return {@code true} - is switch head
	 */
	private static boolean isSwitchHead(final BB bb) {
		// don't use successor number as indicator, switch with 2 successors
		// (JVM 6: 1 case and default) possible, not optimized
		return bb.getFinalStmt() instanceof SwitchStatement;
	}

	/**
	 * Is sync head?
	 *
	 * Works even for trivial / empty sync sections, because BBs are always split behind
	 * MONITOR_ENTER (see Data Flow Analysis).
	 *
	 * @param bb
	 *            BB
	 *
	 * @return {@code true} - is sync head
	 */
	private static boolean isSyncHead(final BB bb) {
		final Statement statement = bb.getFinalStmt();
		if (!(statement instanceof SynchronizedStatement)) {
			return false;
		}
		final Op op = Expressions.getOp(statement);
		if (!(op instanceof MONITOR)) {
			return false;
		}
		return ((MONITOR) op).getKind() == MONITOR.Kind.ENTER;
	}

	/**
	 * Transform CFG.
	 *
	 * @param cfg
	 *            CFG
	 */
	public static void transform(@Nonnull final CFG cfg) {
		new TrControlFlowAnalysis(cfg).transform();
	}

	@Getter(AccessLevel.PROTECTED)
	@Nonnull
	private final CFG cfg;

	private TrControlFlowAnalysis(@Nonnull final CFG cfg) {
		this.cfg = cfg;
	}

	private Cond createCondStruct(@Nonnull final BB head) {
		final Cond cond = new Cond(head);

		final BB falseSucc = head.getFalseSucc();
		final BB trueSucc = head.getTrueSucc();
		assert falseSucc != null && trueSucc != null;

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

		// 2nd condition part is trick against things like iteration in loop last:
		final boolean negated = falseSucc.isBefore(trueSucc) || trueSucc.isBefore(head);
		final BB firstSucc = negated ? falseSucc : trueSucc;
		final BB secondSucc = negated ? trueSucc : falseSucc;
		final Boolean firstValue = negated;
		final Boolean secondValue = !negated;

		final List<BB> firstMembers = Lists.newArrayList();
		final Set<BB> firstFollows = Sets.newHashSet();
		findBranch(cond, firstSucc, firstMembers, firstFollows);

		// no else BBs: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (firstFollows.isEmpty() || firstFollows.contains(secondSucc)) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setKind(negated ? Cond.Kind.IFNOT : Cond.Kind.IF);
			cond.addMembers(firstValue, firstMembers);
			cond.setFollow(secondSucc);
			return cond;
		}

		final List<BB> secondMembers = Lists.newArrayList();
		final Set<BB> secondFollows = Sets.newHashSet();
		findBranch(cond, secondSucc, secondMembers, secondFollows);

		// no else BBs: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (secondFollows.isEmpty() || secondFollows.contains(firstSucc)) {
			// also often in JDK 6 bytecode, especially in parent structs
			cond.setKind(negated ? Cond.Kind.IF : Cond.Kind.IFNOT);
			cond.addMembers(secondValue, secondMembers);
			cond.setFollow(firstSucc);
			return cond;
		}

		// end nodes are follows or breaks - no continues, returns, throws

		// JDK 6: highest follow is potential branch follow
		BB firstFollow = null;
		for (final BB follow : firstFollows) {
			if (follow.isBefore(firstFollow)) {
				firstFollow = follow;
			}
		}
		BB secondFollow = null;
		for (final BB follow : secondFollows) {
			if (follow.isBefore(secondFollow)) {
				secondFollow = follow;
			}
		}

		// follow exists?
		if (firstFollow == secondFollow) {
			// normal stuff
			cond.setKind(negated ? Cond.Kind.IFNOT_ELSE : Cond.Kind.IF_ELSE);
			cond.addMembers(firstValue, firstMembers);
			cond.addMembers(secondValue, secondMembers);
			cond.setFollow(firstFollow);
			return cond;
		}
		// only if unrelated conditional tails???
		log.warn(getMd() + ": Unknown struct, no common follow for:\n" + cond);
		cond.setKind(Cond.Kind.IF);
		return cond;
	}

	private Loop createLoopStruct(@Nonnull final BB head, @Nonnull final List<BB> backBbs) {
		final Loop loop = new Loop(head);

		final Set<BB> traversedBbs = Sets.<BB> newHashSet();
		final List<BB> members = loop.getMembers(null);
		BB last = null;
		for (final BB backBb : backBbs) {
			assert backBb != null;
			final boolean found = findReverseBranch(loop, backBb, members, traversedBbs);
			assert found : "cannot have a loop back BB that is not in reverse branch";
			if (last == null || last.isBefore(backBb)) {
				last = backBb;
			}
		}
		loop.setLast(last);

		// we check if this could be a pre-loop:
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
			// no proper pre-loop head!
		}
		// we check if this could be a post-loop:
		Loop.Kind lastKind = null;
		BB lastFollow = null;
		if (last != null && last.isCond()) {
			// don't exclude last.isLoopHead(), simple back loops with multiple statements possible
			final BB falseSucc = last.getFalseSucc();
			final BB trueSucc = last.getTrueSucc();
			if (loop.isHead(trueSucc)) {
				lastKind = Loop.Kind.DO_WHILE;
				lastFollow = falseSucc;
			} else if (loop.isHead(falseSucc)) {
				lastKind = Loop.Kind.DO_WHILENOT;
				lastFollow = trueSucc;
			}
			// no proper post-loop last!
		}
		// now we check with some heuristics if pre-loop or post-loop is the prefered loop, this is
		// not always exact science...
		if (headKind != null && lastKind == null) {
			loop.setKind(headKind);
			loop.setFollow(headFollow);
			return loop;
		}
		if (headKind == null && lastKind != null) {
			loop.setKind(lastKind);
			loop.setFollow(lastFollow);
			return loop;
		}
		// we have to compare the tails for head and last
		if (headKind != null && lastKind != null) {
			assert headFollow != null && lastFollow != null;
			final List<BB> headMembers = Lists.newArrayList();
			final Set<BB> headFollows = Sets.newHashSet();
			findBranch(loop, headFollow, headMembers, headFollows);
			if (headFollows.contains(lastFollow)) {
				loop.setKind(lastKind);
				loop.setFollow(lastFollow);
				return loop;
			}
			final List<BB> lastMembers = Lists.newArrayList();
			final Set<BB> lastFollows = Sets.newHashSet();
			findBranch(loop, lastFollow, lastMembers, lastFollows);
			if (lastFollows.contains(headFollow)) {
				loop.setKind(headKind);
				loop.setFollow(headFollow);
				return loop;
			}
			// we can decide like we want: we prefer pre-loops
			loop.setKind(headKind);
			loop.setFollow(headFollow);
			return loop;
		}
		loop.setKind(Loop.Kind.ENDLESS);
		return loop;
	}

	private Switch createSwitchStruct(@Nonnull final BB head) {
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
					final BB inBb = in.getStart();
					for (int j = i; j-- > 0;) {
						if (!switchStruct.isMember(caseOuts.get(j).getValue(), inBb)) {
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
			assert members != null;
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
		// highest follow is switch struct follow
		BB switchFollow = null;
		for (final BB follow : follows) {
			if (follow.isBefore(switchFollow)) {
				switchFollow = follow;
			}
		}
		switchStruct.setFollow(switchFollow);
		return switchStruct;
	}

	private void createSyncStruct(@Nonnull final BB head) {
		// Works even for trivial / empty sync sections, because BBs are always split behind
		// MONITOR_ENTER (see Data Flow Analysis).
		final Sync sync = new Sync(head);
		// monitor enter-exit ranges are always paired, it's not necessary to track the register
		class SyncE {

			final E e;

			final int level; // monitor level, top is 1, nested is 2, 3...

			public SyncE(final E e, final int level) {
				this.e = e;
				this.level = level;
			}

		}
		final LinkedList<SyncE> es = Lists.newLinkedList();
		es.push(new SyncE(head.getSequenceOut(), 1));
		BB syncFollow = null;
		bbs: while (!es.isEmpty()) {
			final SyncE syncE = es.poll();
			final BB bb = syncE.e.getEnd();
			int level = syncE.level;
			for (int i = 0; i < bb.getStmts(); ++i) {
				final Statement stmt = bb.getStmt(i);
				if (!(stmt instanceof SynchronizedStatement)) {
					continue;
				}
				final Op monitorOp = getOp(stmt);
				if (!(monitorOp instanceof MONITOR)) {
					continue;
				}
				switch (((MONITOR) monitorOp).getKind()) {
				case ENTER:
					++level;
					break;
				case EXIT:
					if (--level > 0) {
						break;
					}
					// highest follow is sync struct follow
					if (bb.isBefore(syncFollow) && !bb.isCatchHandler()) {
						syncFollow = bb;
					}
					continue bbs;
				default:
					log.warn(getMd() + ": Unknown MONITOR type for operation '" + monitorOp + "'!");
				}
			}
			sync.addMember(null, bb);
			for (final E out : bb.getOuts()) {
				if (out.isBack()) {
					continue;
				}
				es.add(new SyncE(out, level));
			}
		}
		sync.setFollow(syncFollow);
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
	 * @param followBbs
	 *            potential struct follows (no continues or breaks)
	 */
	private void findBranch(@Nonnull final Struct struct, @Nonnull final BB bb,
			@Nonnull final List<BB> members, @Nonnull final Set<BB> followBbs) {
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
					if (followBbs.contains(bb)) {
						return;
					}
					if (bb.isCatchHandler()) {
						// if all incoming catches are inside branch then the above pred-member
						// check was allready sucessful, but we will never be a follow
						return;
					}
					// multiple follows during iteration possible, reduce after #findBranch() to
					// single top follow
					followBbs.add(bb);
					return;
				}
				followBbs.remove(bb); // all pred are now members, maybe we where already here
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
				findBranch(struct, succ, members, followBbs);
			}
		}
	}

	private boolean findReverseBranch(@Nonnull final Struct struct, @Nonnull final BB bb,
			@Nonnull final List<BB> members, @Nonnull final Set<BB> traversedBbs) {
		if (members.contains(bb) || struct.isHead(bb)) {
			return true;
		}
		if (traversedBbs.contains(bb)) {
			return false;
		}
		traversedBbs.add(bb);
		boolean isMember = false;
		for (final E in : bb.getIns()) {
			if (in.isBack()) {
				continue; // ignore incoming back edges, sub loop-heads belong to branch
			}
			final BB pred = in.getStart();
			// we could check if postorder smaller than loop head, but invalid in-jumps are an
			// exception anyway
			if (findReverseBranch(struct, pred, members, traversedBbs)) {
				isMember = true;
			}
		}
		if (isMember) {
			members.add(bb);
			return true;
		}
		return false;
	}

	private M getMd() {
		return getCfg().getM();
	}

	private void transform() {
		final List<BB> bbs = getCfg().getPostorderedBbs();
		// for all nodes in _reverse_ postorder: find outer structs first
		nextBb: for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			bb.sortOuts();

			// check loop first, could be a post / endless loop with additional sub struct heads;
			// including also nested loops, that cannot be mitigated by continue
			final List<BB> loopBackBbs = getLoopBackBbs(bb);
			if (loopBackBbs != null) {
				do {
					final Loop loop = createLoopStruct(bb, loopBackBbs);
					if (loop.isPre()) {
						// exit: no additional struct head possible here
						// TODO but we know that "loopHeadIns - last" are continues? mark?
						continue nextBb;
					}
					// continue to this loop head not possible, nested loop check...
					loopBackBbs.remove(loop.getLast());
				} while (!loopBackBbs.isEmpty());
				// fall through: additional sub struct heads possible for post / endless
			}
			if (isSyncHead(bb)) {
				createSyncStruct(bb);
				continue;
			}
			if (isSwitchHead(bb)) {
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
		}
	}
}