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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.MONITOR;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Catch;
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

	@Nullable
	private static Catch createCatchStruct(final E e) {
		assert e != null;
		return null;
	}

	@Nonnull
	private static Loop createLoopStruct(@Nonnull final BB head, @Nonnull final List<BB> backBbs) {
		final Loop loop = new Loop(head);

		final Set<BB> traversedBbs = Sets.<BB> newHashSet();
		final List<BB> members = Lists.newArrayList();
		BB last = null;
		for (final BB backBb : backBbs) {
			assert backBb != null;
			final boolean found = findReverseBranch(loop, backBb, members, traversedBbs);
			assert found : "cannot have a loop back BB that is not in reverse branch";
			if (last == null || last.isBefore(backBb)) {
				last = backBb;
			}
		}
		loop.addMembers(null, members);
		loop.setLast(last);

		// we check if this could be a pre-loop:
		Loop.Kind headKind = null;
		E headFollowOut = null;
		if (head.getStmts() == 1 && head.isCond()) {
			final E falseOut = head.getFalseOut();
			assert falseOut != null;
			final E trueOut = head.getTrueOut();
			assert trueOut != null;
			if (loop.hasMember(trueOut.getEnd()) && !loop.hasMember(falseOut.getEnd())) {
				// JDK 6: true is member, opPc of pre head > next member,
				// leading goto
				headKind = Loop.Kind.WHILE;
				headFollowOut = falseOut;
			} else if (loop.hasMember(falseOut.getEnd()) && !loop.hasMember(trueOut.getEnd())) {
				// JDK 5: false is member, opPc of pre head < next member,
				// trailing goto (negated, check class javascript.Decompiler)
				headKind = Loop.Kind.WHILENOT;
				headFollowOut = trueOut;
			}
			// no proper pre-loop head!
		}
		// we check if this could be a post-loop:
		Loop.Kind lastKind = null;
		E lastFollowOut = null;
		if (last != null && last.isCond()) {
			// don't exclude last.isLoopHead(), simple back loops with multiple statements possible
			final E falseOut = last.getFalseOut();
			assert falseOut != null;
			final E trueOut = last.getTrueOut();
			assert trueOut != null;
			if (loop.isHead(trueOut.getEnd())) {
				lastKind = Loop.Kind.DO_WHILE;
				lastFollowOut = falseOut;
			} else if (loop.isHead(falseOut.getEnd())) {
				lastKind = Loop.Kind.DO_WHILENOT;
				lastFollowOut = trueOut;
			}
			// no proper post-loop last!
		}
		// now we check with some heuristics if pre-loop or post-loop is the prefered loop, this is
		// not always exact science...
		if (headKind != null && lastKind == null) {
			loop.setKind(headKind);
			assert headFollowOut != null;
			loop.setFollow(headFollowOut.getEnd());
			return loop;
		}
		if (headKind == null && lastKind != null) {
			loop.setKind(lastKind);
			assert lastFollowOut != null;
			loop.setFollow(lastFollowOut.getEnd());
			return loop;
		}
		// we have to compare the tails for head and last
		if (headKind != null && lastKind != null) {
			assert headFollowOut != null && lastFollowOut != null;
			final List<BB> headMembers = Lists.newArrayList();
			final Set<BB> headFollows = Sets.newHashSet();
			findBranch(loop, headFollowOut, headMembers, headFollows);
			if (headFollows.contains(lastFollowOut.getEnd())) {
				loop.setKind(lastKind);
				loop.setFollow(lastFollowOut.getEnd());
				return loop;
			}
			final List<BB> lastMembers = Lists.newArrayList();
			final Set<BB> lastFollows = Sets.newHashSet();
			findBranch(loop, lastFollowOut, lastMembers, lastFollows);
			if (lastFollows.contains(headFollowOut.getEnd())) {
				loop.setKind(headKind);
				loop.setFollow(headFollowOut.getEnd());
				return loop;
			}
			// we can decide like we want: we prefer pre-loops
			loop.setKind(headKind);
			loop.setFollow(headFollowOut.getEnd());
			return loop;
		}
		loop.setKind(Loop.Kind.ENDLESS);
		return loop;
	}

	@Nonnull
	private static Switch createSwitchStruct(@Nonnull final BB head) {
		final Switch switchStruct = new Switch(head);

		// for case reordering we need this as changeable lists
		final List<E> caseOuts = head.getOuts();
		final int size = caseOuts.size();

		final Set<BB> follows = Sets.newHashSet();

		int hack = 10;
		cases: for (int i = 0; i < size && hack > 0; ++i) {
			final E caseOut = caseOuts.get(i);
			if (caseOut.isBack()) {
				continue;
			}
			if (!caseOut.isSwitchCase()) {
				assert caseOut.isCatch();
				continue;
			}
			final BB caseBb = caseOut.getEnd();
			boolean isFallThrough = false;
			final List<E> ins = caseBb.getIns();
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
						if (!switchStruct.hasMember(caseOuts.get(j).getValue(), inBb)) {
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
						// we cannot be a fall-through yet...may be later, add as last for now
						--hack;
						head.moveOut(i--, caseOuts.size() - 1);
						continue cases;
					}
				}
				if (prevCaseIndex < i - 1) {
					head.moveOut(i, prevCaseIndex + 1);
				}
				if (caseOut.isSwitchDefault() && follows.size() == 1 && follows.contains(caseBb)) {
					// TODO when we are the final fall-through without other follows...
					switchStruct.setKind(Kind.NO_DEFAULT);
					switchStruct.setFollow(caseBb);
					continue cases;
				}
				isFallThrough = true;
			}
			final List<BB> members = Lists.newArrayList();
			if (isFallThrough) { // TODO better check follows.contains(caseBb))?
				follows.remove(caseBb);
				members.add(caseBb);
			}
			findBranch(switchStruct, caseOut, members, follows);
			switchStruct.addMembers(caseOut.getValue(), members);
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
		if (switchFollow != null) {
			switchStruct.setFollow(switchFollow);
		}
		return switchStruct;
	}

	@Nonnull
	private static Sync createSyncStruct(@Nonnull final BB head) {
		final Sync sync = new Sync(head);
		final E sequenceOut = head.getSequenceOut();
		assert sequenceOut != null;

		final List<BB> checkBbs = Lists.newArrayList(sequenceOut.getEnd());
		BB syncFollow = null;
		outer: while (!checkBbs.isEmpty()) {
			final BB checkBb = checkBbs.remove(0);

			for (int i = 0; i < checkBb.getStmts(); ++i) {
				final Statement stmt = checkBb.getStmt(i);
				if (!(stmt instanceof SynchronizedStatement)) {
					continue;
				}
				final Op monitorOp = getOp(stmt);
				if (!(monitorOp instanceof MONITOR)) {
					assert false;
					continue;
				}
				if (((MONITOR) monitorOp).getKind() != MONITOR.Kind.EXIT) {
					continue;
				}
				// kill exits, encoded in struct now
				checkBb.removeStmt(i);
				// highest follow is sync struct follow
				// TODO add checkBb always as member, check if sequenceOut -> this must be follow or
				// break
				if (checkBb.isCatchHandler()) {
					sync.addMember(null, checkBb);
				} else if (syncFollow == null) {
					syncFollow = checkBb;
				} else if (syncFollow.isBefore(checkBb)) {
					sync.addMember(null, syncFollow);
					syncFollow = checkBb;
				}
				continue outer;
			}
			if (!sync.addMember(null, checkBb)) {
				continue;
			}
			// deep recursion into out edges of this member
			for (final E out : checkBb.getOuts()) {
				if (out.isBack()) {
					continue;
				}
				final BB succ = out.getEnd();
				if (succ != syncFollow && !sync.hasMember(succ) && !checkBbs.contains(succ)) {
					checkBbs.add(succ);
				}
			}
		}
		if (syncFollow != null) {
			sync.setFollow(syncFollow);
		}
		return sync;
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
	 * @param firstIn
	 *            first incoming edge into branch
	 * @param members
	 *            found struct members
	 * @param followBbs
	 *            potential struct follows (no continues or breaks)
	 */
	private static void findBranch(@Nonnull final Struct struct, @Nonnull final E firstIn,
			@Nonnull final List<BB> members, @Nonnull final Set<BB> followBbs) {
		// no recursion, can be very deep
		final List<BB> checkBbs = Lists.newArrayList(firstIn.getEnd());
		outer: do {
			final BB checkBb = checkBbs.remove(0);
			if (!members.contains(checkBb)) { // check necessary for switch case fall throughs
				if (checkBb.isStartBb()) {
					// special case: checkBb is loop head and is CFG-startBb (no additional ins)
					followBbs.add(checkBb);
					continue;
				}
				if (checkBb.getIns().size() > 1) {
					// has checkBb a none-member predecessor? only possible for multiple ins
					for (final E in : checkBb.getIns()) {
						if (in == firstIn) {
							continue; // ignore first incoming edge into branch
						}
						if (in.isBack()) {
							continue; // ignore incoming back edges, sub loop-heads belong to branch
						}
						final BB pred = in.getStart();
						if (members.contains(pred)) {
							continue;
						}
						followBbs.add(checkBb);
						continue outer;
					}
					// all predecessors of checkBb are members
					followBbs.remove(checkBb); // maybe we where already here
				}
				// checkBb is a member
				members.add(checkBb);
			}
			// deep recursion into out edges of this member
			// TODO jump over finally here? handle before and remove?
			for (final E out : checkBb.getOuts()) {
				final BB succ = out.getEnd();
				if (succ.isCatchHandler()) {
					// don't follow catches or we get handler BBs from enclosing catches as follows
					// TODO shouldn't happen later, handle catches and remove edges before
					continue;
				}
				if (!members.contains(succ) && !checkBbs.contains(succ)) {
					// follows must be checked again
					checkBbs.add(succ);
				}
			}
		} while (!checkBbs.isEmpty());
	}

	private static boolean findReverseBranch(@Nonnull final Struct struct, @Nonnull final BB bb,
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
			// we could check if postorder smaller than loop head, but invalid struct multi-entries
			// are a rare exception anyway
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

	private static E findUnhandledCatch(final BB bb) {
		for (final E out : bb.getOuts()) {
			if (!out.isCatch()) {
				continue;
			}
			if (isHandlerHandled(out.getEnd())) {
				continue;
			}
			// TODO find outmost or return list
			return out;
		}
		return null;
	}

	/**
	 * Find all unhandled loop back BBs for the given BB. The given BB is a loop head if at least
	 * one such back BB exists, which isn't a self-catch handler (valid bytecode).
	 *
	 * Nested post loops with same head are possible and cannot be handled by continue (which goes
	 * in this cases to the last loop node), hence we have to exclude already handled enclosing loop
	 * back BBs.
	 *
	 * @param bb
	 *            BB
	 *
	 * @return {@code true} - is unhandled loop head
	 */
	@Nullable
	private static List<BB> findUnhandledLoopBackBbs(final BB bb) {
		List<BB> backBbs = null;
		for (final E in : bb.getIns()) {
			if (!in.isBack() || in.isCatch()) {
				continue;
			}
			final BB pred = in.getStart();
			final Struct struct = pred.getStruct();
			if (struct instanceof Loop && struct.getHead() == bb
					&& (!((Loop) struct).isPost() || ((Loop) struct).isLast(pred))) {
				continue;
			}
			if (backBbs == null) {
				backBbs = Lists.newArrayList();
			}
			backBbs.add(pred);
		}
		return backBbs;
	}

	/**
	 * Is cond head?
	 *
	 * @param bb
	 *            BB
	 *
	 * @return {@code true} - is cond head
	 */
	private static boolean isCondHead(final BB bb) {
		if (!bb.isCond()) {
			return false;
		}
		for (Struct struct = bb.getStruct(); struct != null; struct = struct.getParent()) {
			if (!(struct instanceof Loop)) {
				continue;
			}
			final Loop loopStruct = (Loop) struct;
			if (loopStruct.isPost() && loopStruct.isLast(bb)) {
				// exit: conditional already used for post loops last condition
				return false;
			}
		}
		return true;
	}

	/**
	 * Is given BB a handler and has already been assigned to a cond struct?
	 *
	 * Handler could already be part of enclosing struct like e.g. enclosing try, so getting only
	 * the outmost struct doesn't work.
	 *
	 * @param bb
	 *            potential handler
	 * @return {@code true} - is handler with assigned struct
	 */
	private static boolean isHandlerHandled(final BB bb) {
		for (Struct struct = bb.getStruct(); struct != null; struct = struct.getParent()) {
			if (struct instanceof Catch && ((Catch) struct).getHandler() == bb) {
				return true;
			}
		}
		return false;
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
	 * MONITOR_ENTER (see {@link TrDataFlowAnalysis}).
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

	@Nonnull
	private Cond createCondStruct(@Nonnull final BB head) {
		final Cond cond = new Cond(head);

		final E falseOut = head.getFalseOut();
		assert falseOut != null;
		final E trueOut = head.getTrueOut();
		assert trueOut != null;

		final BB falseSucc = falseOut.getEnd();
		final BB trueSucc = trueOut.getEnd();

		// if-statement compilation hasn't changed with JDK versions (unlike boolean expressions)
		// means: negated if-expression, false successor contains if-body (PC & line before true),
		// special unnegated cases (with JDK 5 compiler?):
		// direct continue back-edges and break forward-edges (only possible with true)

		// we have to check all possibilities anyway...but prefer normal variants
		// JDK 1 && 2 create wrong line numbers for final return (see DecTestIfStmt)

		// negated also handles empty if-statements as negated by default,
		// 2nd condition part is trick against things like iteration in loop last,
		final boolean negated = falseSucc.isBefore(trueSucc) || trueSucc.isBefore(head);
		final E firstOut = negated ? falseOut : trueOut;
		final E secondOut = negated ? trueOut : falseOut;
		final Boolean firstValue = negated;
		final Boolean secondValue = !negated;

		final List<BB> firstMembers = Lists.newArrayList();
		final Set<BB> firstFollows = Sets.newHashSet();
		findBranch(cond, firstOut, firstMembers, firstFollows);

		// no else BBs: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (firstFollows.isEmpty() || firstFollows.contains(secondOut.getEnd())) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setKind(negated ? Cond.Kind.IFNOT : Cond.Kind.IF);
			// also handles empty if-statements, members are empty in this case, see
			// DecTestIfStmt.emptyIf()
			cond.addMembers(firstValue, firstMembers);
			cond.setFollow(secondOut.getEnd());
			return cond;
		}

		final List<BB> secondMembers = Lists.newArrayList();
		final Set<BB> secondFollows = Sets.newHashSet();
		findBranch(cond, secondOut, secondMembers, secondFollows);

		// no else BBs: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (secondFollows.isEmpty() || secondFollows.contains(firstOut.getEnd())) {
			// also often in JDK 6 bytecode, especially in parent structs
			cond.setKind(negated ? Cond.Kind.IF : Cond.Kind.IFNOT);
			cond.addMembers(secondValue, secondMembers);
			cond.setFollow(firstOut.getEnd());
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
		assert firstFollow != null;
		BB secondFollow = null;
		for (final BB follow : secondFollows) {
			if (follow.isBefore(secondFollow)) {
				secondFollow = follow;
			}
		}
		assert secondFollow != null;

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

	private M getMd() {
		return getCfg().getM();
	}

	private void transform() {
		final List<BB> bbs = getCfg().getPostorderedBbs();
		// for all nodes in _reverse_ postorder: find outer structs first
		nextBb: for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			bb.sortOuts();

			while (true) {
				final E e = findUnhandledCatch(bb);
				if (e == null) {
					break;
				}
				// TODO change to nonnull? just for testing now
				if (createCatchStruct(e) == null) {
					break;
				}
			}
			// check loop first, could be endless / post loop with additional sub struct heads;
			// including nested post loops that cannot be mitigated by continue
			while (true) {
				final List<BB> loopBackBbs = findUnhandledLoopBackBbs(bb);
				if (loopBackBbs == null) {
					break;
				}
				final Loop loop = createLoopStruct(bb, loopBackBbs);
				if (loop.isPre()) {
					// exit: no additional struct head possible here
					continue nextBb;
				}
				// fall through: additional sub struct heads possible for post / endless, including
				// nested post loops that cannot be handled by continue
			}
			if (isSyncHead(bb)) {
				createSyncStruct(bb);
				continue;
			}
			if (isSwitchHead(bb)) {
				createSwitchStruct(bb);
				continue;
			}
			if (isCondHead(bb)) {
				createCondStruct(bb);
			}
		}
	}

}