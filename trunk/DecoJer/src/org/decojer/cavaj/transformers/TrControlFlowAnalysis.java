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
import static org.decojer.cavaj.utils.Expressions.newSimpleName;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.ops.MONITOR;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Block;
import org.decojer.cavaj.model.code.structs.Catch;
import org.decojer.cavaj.model.code.structs.Cond;
import org.decojer.cavaj.model.code.structs.Loop;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.code.structs.Switch;
import org.decojer.cavaj.model.code.structs.Switch.Kind;
import org.decojer.cavaj.model.code.structs.Sync;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.utils.Expressions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Transformer: Control Flow Analysis.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrControlFlowAnalysis {

	private static boolean containsEnd(final Set<E> outs, final BB end) {
		for (final E out : outs) {
			if (out.getEnd() == end) {
				return true;
			}
		}
		return false;
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
	 * @param exits
	 *            found struct exits, they are potential struct follows
	 */
	private static void findBranch(@Nonnull final Struct struct, @Nonnull final E firstIn,
			@Nonnull final List<BB> members, @Nonnull final Set<E> exits) {
		final Struct parentStruct = struct.getParent();
		// no recursion, can be very deep
		final List<E> checkEs = Lists.newArrayList(firstIn);
		outer: do {
			final E checkE = checkEs.remove(0);
			final BB checkBb = checkE.getEnd();
			if (!members.contains(checkBb)) { // given first member (handler/switch)?
				if (checkBb.isStartBb()) {
					// special case: checkBb is loop head and is CFG-startBb (no additional ins)
					exits.add(checkE);
					continue;
				}
				final List<E> checkBbIns = checkBb.getIns();
				if (checkBbIns.size() > 1) {
					// has checkBb a none-member predecessor? only possible for multiple ins
					for (final E in : checkBbIns) {
						if (in == checkE) {
							continue; // also ignores first incoming edge into branch
						}
						if (in.isBack()) {
							// ignore incoming back edges, sub loop-heads belong to branch
							continue;
						}
						final BB pred = in.getStart();
						if (members.contains(pred)) {
							continue;
						}
						if (!in.isCatch()) {
							// wrapping handlers are always catches...don't add as follow
							exits.add(checkE);
						}
						// this is the usual loop end, till we have encountered all ins to checkBb
						continue outer;
					}
					// all predecessors of checkBb are members
					for (final E in : checkBbIns) {
						exits.remove(in); // maybe we where already here
					}
				}
				if (parentStruct != null) {
					// stop at loop lasts
					if (parentStruct instanceof Loop && ((Loop) parentStruct).hasLast(checkBb)) {
						exits.add(checkE);
						continue outer;
					}
					// can e.g. happen for synchronize follows (no additional ins for follow)
					if (parentStruct.hasFollow(checkBb)) {
						assert checkBb.getIns().size() == 1;
						exits.add(checkE);
						continue outer;
					}
				}
				// checkBb is a member
				members.add(checkBb);
			}
			// deep recursion into out edges of this member,
			// should happen only once, because the usual loop end is the upper continue outer
			for (final E out : checkBb.getOuts()) {
				final BB succ = out.getEnd();
				// enclosed catch-handlers could be part of branch: add here und check ins later
				if (!members.contains(succ)) {
					// follows must be checked again
					checkEs.add(out);
				}
			}
		} while (!checkEs.isEmpty());
	}

	/**
	 * Find all outmost unhandled catch edges for the given BB.
	 *
	 * Nested catches with same heads are possible. The returned list is filtered in a way that the
	 * outmost catches belonging to a single try-catch-finally are given.
	 *
	 * @param bb
	 *            BB
	 * @return all outmost unhandled catch edges for the given BB
	 */
	@Nullable
	private static List<E> findCatchOutmostUnhandled(final BB bb) {
		List<E> unhandledCatches = null;
		E unhandledFinally = null;
		outer: for (final E iCatch : bb.getOuts()) {
			if (!isUnhandledCatch(iCatch)) {
				continue;
			}
			if (iCatch.isFinally()) {
				// check later if unhandled finally is conform to unhandled catches
				if (unhandledFinally == null
						|| unhandledFinally.getEnd().hasSucc(iCatch.getEnd())) {
					unhandledFinally = iCatch;
				}
				continue;
			}
			if (unhandledCatches == null) {
				// nothing further to check, is currently first (for now assumed outmost) edge
				unhandledCatches = Lists.newArrayList(iCatch);
				continue;
			}
			// check if new edge is "more outmost" then previous edges
			final BB iHandler = iCatch.getEnd();
			for (int j = unhandledCatches.size(); j-- > 0;) {
				final E unhandledCatch = unhandledCatches.get(j);
				final BB unhandledHandler = unhandledCatch.getEnd();

				if (unhandledHandler.hasSucc(iHandler)) {
					unhandledCatches.remove(j);
					continue;
				}
				if (iHandler.hasSucc(unhandledHandler)) {
					continue outer;
				}
			}
			unhandledCatches.add(iCatch);
		}
		if (unhandledFinally == null) {
			return unhandledCatches;
		}
		if (unhandledCatches == null) {
			return Lists.newArrayList(unhandledFinally);
		}
		// check if unhandled finally is conform to unhandled catches
		final BB finallyHandler = unhandledFinally.getEnd();
		for (int j = unhandledCatches.size(); j-- > 0;) {
			final E unhandledCatch = unhandledCatches.get(j);
			final BB unhandledHandler = unhandledCatch.getEnd();

			if (!unhandledHandler.hasSucc(finallyHandler)) {
				return Lists.newArrayList(unhandledFinally); // invalid inner catch
			}
			if (finallyHandler.hasSucc(unhandledHandler)) {
				return unhandledCatches; // invalid inner finally
			}
		}
		unhandledCatches.add(unhandledFinally);
		return unhandledCatches;
	}

	/**
	 * Find all unhandled loop back edges for the given BB. The given BB is a loop head if at least
	 * one such back BB exists, which isn't a self-catch handler (valid bytecode).
	 *
	 * Nested post loops with same head are possible and cannot be handled by continue (which goes
	 * in this cases to the last loop node), hence we have to exclude already handled enclosing loop
	 * back BBs.
	 *
	 * @param bb
	 *            BB
	 *
	 * @return all unhandled loop back BBs for the given BB
	 */
	@Nullable
	private static List<E> findLoopUnhandledBacks(final BB bb) {
		// list necessary, multiple backs possible with no related postorder, identify last later
		List<E> backs = null;
		isHandled: for (final E in : bb.getIns()) {
			if (!in.isBack() || in.isCatch() || in.isJsr() || in.isRet()) {
				continue;
			}
			final BB pred = in.getStart();
			for (Struct struct = pred.getStruct(); struct != null
					&& struct.getHead() == bb; struct = struct.getParent()) {
				// iterate through parent structs, loop and catch heads can alternate
				if (struct instanceof Loop
						&& (!((Loop) struct).isPost() || ((Loop) struct).hasLast(pred))) {
					continue isHandled;
				}
			}
			if (backs == null) {
				backs = Lists.newArrayList();
			}
			backs.add(in);
		}
		return backs;
	}

	private static boolean findReverseBranch(@Nonnull final Struct struct, @Nonnull final BB bb,
			@Nonnull final List<BB> members, @Nonnull final Set<BB> traversedBbs) {
		if (members.contains(bb) || struct.hasHead(bb)) {
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

	private static boolean handleSyncFinally(@Nonnull final BB bb) {
		final E catchIn = bb.getCatchIn();
		if (catchIn == null || !catchIn.isFinally()) {
			return false;
		}
		if (bb.getStmts() != 2) {
			// sync still in here, is deleted later
			return false;
		}
		final E throwBb = bb.getSequenceOut();
		if (throwBb == null || throwBb.isBack() || throwBb.getEnd().getStmts() != 1
				|| !(throwBb.getEnd().getStmt(0) instanceof ThrowStatement)) {
			return false;
		}
		bb.remove();
		return true;
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
			if (loopStruct.isPost() && loopStruct.hasLast(bb)) {
				// exit: conditional already used for post loops last condition
				return false;
			}
		}
		return true;
	}

	private static boolean isLoopInCatches(final @Nonnull List<E> backs,
			final @Nonnull List<E> catches) {
		// check if there is a back-start that is not handled -> loop is assumed
		// outer for Java, cannot translate all possible CFG structures here
		for (final E catchE : catches) {
			// iterate through all catch handles (for currently checked head BB), check if loop last
			// is handled too
			final List<E> handlerCatches = catchE.getEnd().getIns();
			back: for (final E back : backs) {
				final BB loopLast = back.getStart();
				for (final E handlerCatchE : handlerCatches) {
					if (loopLast == handlerCatchE.getStart()) {
						continue back;
					}
				}
				return false;
			}
		}
		return true;
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
	 * Is given edge (any edge) an _unhandled_ catch edge?
	 *
	 * @param e
	 *            edge to be checked
	 * @return {@code true} - edge is an unhandled catch edge
	 */
	private static boolean isUnhandledCatch(final E e) {
		if (!e.isCatch()) {
			return false;
		}
		final BB findHandler = e.getEnd();
		final T[] findCatchTypes = (T[]) e.getValue();
		assert findCatchTypes != null;

		// handler already handled in outer struct?
		for (Struct struct = findHandler.getStruct(); struct != null; struct = struct.getParent()) {
			// member-catch-values T[] are usually same for same handlers, see DataFlowAnalysis
			if (struct instanceof Catch
					&& ((Catch) struct).hasHandler(findCatchTypes, findHandler)) {
				return false; // this handler is already handled, skip this catch
			}
		}
		return true;
	}

	private static boolean removeOutsWithEndFromSet(final Set<E> outs, final BB end) {
		boolean removed = false;
		for (final Iterator<E> outIt = outs.iterator(); outIt.hasNext();) {
			final E out = outIt.next();
			if (out.getEnd() == end) {
				outIt.remove();
				removed = true;
			}
		}
		return removed;
	}

	private static boolean rewriteFinally(@Nonnull final Catch catchStruct,
			@Nonnull final Set<E> exits) {
		final BB finallyHandler = catchStruct.getFirstMember(Catch.FINALLY_TS);
		if (finallyHandler == null) {
			return false;
		}
		// JSR-finally: finallyHandler BB has as single statement the temporary throwable
		// declaration, a single JSR-out to the finally-BBs (potentially also additional outer
		// catches) and a RET to a BB with final throw as single statement
		final E finallyJsr = finallyHandler.getJsrOut();
		if (finallyJsr != null) {
			assert finallyHandler.getStmts() == 1;
			assert finallyHandler.getStmt(0) instanceof VariableDeclarationStatement;
			final BB subBb = finallyJsr.getEnd();

			// we will kill all JSRs now (prevent concurrent modification exception)
			final List<E> jsrs = subBb.getIns();
			for (int i = jsrs.size(); i-- > 0;) {
				final E jsr = jsrs.get(i);
				if (jsr == finallyJsr) {
					continue;
				}
				// was not always in exits (nested finally), but remove always as follow candidate:
				final boolean jsrIsExit = exits.remove(jsr);

				final E ret = jsr.getRetOut();
				if (ret == null) {
					// no ret is possible, e.g. while-loop with nested finally (Sub with back edge)
					jsr.remove();
					continue;
				}
				final BB start = jsr.getStart();
				final BB end = ret.getEnd();
				// jsr edge removed automatically be following operations
				if (start.isEmpty()) {
					// start-ins can be exits, but are automatically relocated as end-ins exits
					start.moveIns(end);
				} else {
					final E succ = start.setSucc(end);
					if (jsrIsExit) {
						exits.add(succ);
					}
				}
				ret.remove(); // must remove, end-incomings are not modified by previous operations
			}
			// add finally nodes as members
			final List<BB> handlerMembers = Lists.newArrayList();
			findBranch(catchStruct, finallyJsr, handlerMembers, exits);
			catchStruct.addMembers(finallyHandler.getIns().get(0).getValue(), handlerMembers);

			// remove here, could have been added again by findBranch with back edge
			exits.remove(finallyJsr);

			final E finallyRet = finallyJsr.getRetOut();
			if (finallyRet != null) {
				// no ret is possible, e.g. while-loop with nested finally (Sub with back edge)
				final BB end = finallyRet.getEnd();
				assert end.getIns().size() == 1;
				finallyRet.getStart().joinSuccBb(end); // retOut collapse
			}
			finallyHandler.setPostorder(subBb.getPostorder());
			finallyHandler.joinSuccBb(subBb); // jsrOut collapse
			return true;
		}
		// JDK6 finally: finallyHandler BB has as first statement the temporary throwable
		// declaration, the finally statements and a final throw statement

		// TODO all follows should be same and can be reduced
		for (final E exit : exits) {
			if (exit.getEnd() == finallyHandler) {
				continue;
			}
			// compare handler with follows...strip same
		}
		return false;
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

	int labelCounter = 0;

	private TrControlFlowAnalysis(@Nonnull final CFG cfg) {
		this.cfg = cfg;
	}

	/**
	 * Check for given exit if it's a branching statement, that means explicitely struct leaving
	 * with break/continue etc. (either labeled or none-labeled), or if it's a normal exit to a
	 * potential follow.
	 *
	 * @param struct
	 *            current struct
	 * @param exit
	 *            exit, that means leaving the current struct
	 * @return {@code true} - explicit struct leaving with break/continue etc. is necessary
	 */
	private boolean checkBranching(@Nonnull final Struct struct, @Nonnull final E exit) {
		boolean defaultBreakableConsumed = false;
		final BB bb = exit.getEnd();
		for (Struct followStruct = struct
				.getParent(); followStruct != null; followStruct = followStruct.getParent()) {
			if (!followStruct.hasBreakTarget(bb) && !followStruct.hasContinueTarget(bb)) {
				if (followStruct.isDefaultBreakable()) {
					// none-labeled break/continue not possible anymore
					defaultBreakableConsumed = true;
				}
				continue;
			}
			if (followStruct == struct.getParent()) {
				// check directly enclosing struct for potential fall-through scenarios
				if (followStruct instanceof Loop && ((Loop) followStruct).hasLast(bb)) {
					return false; // direct fall-through to direct enclosing loop-last allowed
				}
				if (!followStruct.isDefaultBreakable()) {
					// fall-through to directly enclosing struct allowed (for cond, not for loop)
					return false;
				}
			}
			// create label (if not already existing or not none-labeled break/continue possible)
			if (followStruct.getLabel() == null
					&& (!followStruct.isDefaultBreakable() || defaultBreakableConsumed)) {
				final String label = followStruct.getDefaultLabelName()
						+ (this.labelCounter++ == 0 ? "" : this.labelCounter);
				followStruct.setLabel(label);
				if (followStruct.hasBreakTarget(bb)) {
					final BreakStatement breakStatement = getAst().newBreakStatement();
					breakStatement.setLabel(newSimpleName(label, getAst()));
					exit.setBranchingStmt(breakStatement);
				} else {
					final ContinueStatement continueStatement = getAst().newContinueStatement();
					continueStatement.setLabel(newSimpleName(label, getAst()));
					exit.setBranchingStmt(continueStatement);
				}
			} else {
				// cannot add breaks/continues to exit.start, e.g. direct IfStmt-breaks/backs happen
				if (followStruct.hasBreakTarget(bb)) {
					exit.setBranchingStmt(getAst().newBreakStatement());
				} else {
					exit.setBranchingStmt(getAst().newContinueStatement());
				}
			}
			return true;
		}
		return false;
	}

	@Nonnull
	private Block createBlockStruct(@Nonnull final Struct enclosedStruct,
			@Nonnull final BB follow) {
		// assume proper follow...go up untill we find some
		Struct childStruct = enclosedStruct;

		for (Struct parent = childStruct.getParent(); parent != null && !parent.hasMember(follow)
				&& !parent.hasFollow(follow)
				&& parent.getFollow() != null; parent = childStruct.getParent()) {
			childStruct = parent;
		}
		if (childStruct.getParent() instanceof Block && childStruct.getParent().hasFollow(follow)) {
			// TODO yikes...checkBranching should kill this one earlier...needs additional infos?
			return null;
		}
		final Block block = new Block(childStruct);

		// fill members: follow-ins up to already existing members or block head
		final List<BB> blockMembers = block.getMembers(null);
		assert blockMembers != null;
		final List<BB> members = Lists.newArrayList(blockMembers);
		final Set<BB> traversedBbs = Sets.newHashSet();
		for (final E in : follow.getIns()) {
			if (in.isBack()) {
				continue;
			}
			findReverseBranch(block, in.getStart(), members, traversedBbs);
		}
		block.addMembers(null, members);

		block.setFollow(follow);

		block.setLabel(
				block.getDefaultLabelName() + (this.labelCounter++ == 0 ? "" : this.labelCounter));
		return block;
	}

	@Nonnull
	private Catch createCatchStruct(@Nonnull final BB head, @Nonnull final List<E> catches) {
		final Catch catchStruct = new Catch(head);

		// exits means: _leaving_ the catch area or handler branches
		final Set<E> exits = Sets.newHashSet();
		// gather potential initial exits (but catches cannot be exits),
		// exits initialized with head-outs, because these are not handled in handler-in loop
		for (final E out : head.getOuts()) {
			if (!out.isCatch() && !catchStruct.hasHead(out.getEnd())) {
				exits.add(out);
			}
		}
		// for each handler:
		// 1) add all ins to members (catch area) and all ins-outs as potential follows
		// 2) add handler branch to members and handler branch follows as potential follows
		for (int i = 0; i < catches.size(); ++i) {
			final E catchE = catches.get(i);
			final BB handler = catchE.getEnd();

			final List<E> handlerIns = handler.getIns();
			for (final E handlerIn : handlerIns) {
				final BB member = handlerIn.getStart();
				if (catchStruct.hasMember(member)) {
					continue; // already in
				}
				removeOutsWithEndFromSet(exits, member); // member cannot be an exit
				if (catches.size() == 1 || !handlerIn.isFinally()) {
					// finally catch also contains other handlers, all added seperately
					catchStruct.addMember(null, member);
				}
				// gather potential new follows (but catches cannot be follows)
				for (final E out : member.getOuts()) {
					if (out.isCatch()) {
						continue;
					}
					if (!catchStruct.hasMember(null, out.getEnd())) {
						exits.add(out);
					}
				}
				if (i != 0 && !handlerIn.isFinally()) {
					// finally-catch also includes exception-handlers (later also use this info?),
					// other exceptions should share the same try-block
					assert 0 == 1 : "Not properly nested catch struct: " + catchStruct;
					log.warn(getM() + ": Not properly nested catch struct: " + catchStruct);
				}
			}
			final List<BB> handlerMembers = Lists.newArrayList(handler);
			findBranch(catchStruct, catchE, handlerMembers, exits);
			catchStruct.addMembers(catchE.getValue(), handlerMembers);
		}
		rewriteFinally(catchStruct, exits);
		final BB firstFollow = findFollow(catchStruct, exits);
		if (firstFollow != null) {
			catchStruct.setFollow(firstFollow);
		}
		return catchStruct;
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
		// 2nd part handles condition with direct back edge in loop last
		final boolean secondIsDirectBack = trueSucc.hasSucc(head);
		final boolean negated = falseSucc.hasSourceBefore(trueSucc) || secondIsDirectBack;
		final E firstOut = negated ? falseOut : trueOut;
		final E secondOut = negated ? trueOut : falseOut;

		final List<BB> firstMembers = Lists.newArrayList();
		final Set<E> firstExits = Sets.newHashSet();
		findBranch(cond, firstOut, firstMembers, firstExits);

		// handle/filter follows:
		// * direct follow parent: 2 alternatives, if/else (prefer) or break
		// * outer follow parent: break
		// * topmost follow is our follow
		// * all other follows -> create artificial break-block

		final BB firstFollow = findFollow(cond, firstExits);

		// no else BBs: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (firstFollow == null || firstFollow == secondOut.getEnd()) {
			// normal in JVM 6 bytecode, ifnot-expressions
			cond.setKind(negated ? Cond.Kind.IFNOT : Cond.Kind.IF);
			// also handles empty if-statements, members are empty in this case, see
			// DecTestIfStmt.emptyIf()
			cond.addMembers(negated, firstMembers);
			cond.setFollow(secondOut.getEnd());
			return cond;
		}

		final List<BB> secondMembers = Lists.newArrayList();
		final Set<E> secondExits = Sets.newHashSet();
		findBranch(cond, secondOut, secondMembers, secondExits);

		final BB secondFollow = findFollow(cond, secondExits);

		// no else BBs: normal if-block without else or
		// if-continues, if-returns, if-throws => no else necessary
		if (secondFollow == null || secondFollow == firstOut.getEnd()) {
			// also often in JVM 6 bytecode, especially in parent structs
			cond.setKind(negated ? Cond.Kind.IF : Cond.Kind.IFNOT);
			cond.addMembers(!negated, secondMembers);
			cond.setFollow(firstOut.getEnd());
			return cond;
		}

		cond.setKind(negated ? Cond.Kind.IFNOT_ELSE : Cond.Kind.IF_ELSE);
		cond.addMembers(negated, firstMembers);
		cond.addMembers(!negated, secondMembers);

		if (firstFollow == null) {
			if (secondFollow != null) {
				cond.setFollow(secondFollow);
			}
		} else if (secondFollow == null || firstFollow == secondFollow) {
			cond.setFollow(firstFollow);
		} else if (firstFollow.hasSourceBefore(secondFollow)) {
			createBlockStruct(cond, secondFollow);
			cond.setFollow(firstFollow);
		} else if (secondFollow.hasSourceBefore(firstFollow)) {
			createBlockStruct(cond, firstFollow);
			cond.setFollow(secondFollow);
		}
		return cond;
	}

	@Nonnull
	private Loop createLoopStruct(@Nonnull final BB head, @Nonnull final List<E> backs) {
		final Loop loop = new Loop(head);

		final Set<BB> traversedBbs = Sets.newHashSet();
		final List<BB> members = Lists.newArrayList();
		BB last = null;
		for (final E back : backs) {
			assert back != null;
			final BB backBb = back.getStart();
			final boolean found = findReverseBranch(loop, backBb, members, traversedBbs);
			if (!found) {
				assert false : "cannot have a loop back BB that is not in reverse branch";

				log.warn(getM() + ": cannot have a loop back BB that is not in reverse branch");
			}
			if (last == null || last.hasSourceBefore(backBb)) {
				last = backBb;
			}
		}
		loop.addMembers(null, members);
		if (last != null) {
			loop.setLast(last);
		} else {
			assert false;
		}
		// we check if this could be a pre-loop:
		Loop.Kind headKind = null;
		E headExit = null;
		if (head.getStmts() == 1 && head.isCond()) {
			final E falseOut = head.getFalseOut();
			assert falseOut != null;
			final E trueOut = head.getTrueOut();
			assert trueOut != null;
			if (loop.hasMember(trueOut.getEnd()) && !loop.hasMember(falseOut.getEnd())) {
				// JVM 6: true is member, opPc of pre head > next member,
				// leading goto
				headKind = Loop.Kind.WHILE;
				headExit = falseOut;
			} else if (loop.hasMember(falseOut.getEnd()) && !loop.hasMember(trueOut.getEnd())) {
				// JVM 5: false is member, opPc of pre head < next member,
				// trailing goto (negated, check class javascript.Decompiler)
				headKind = Loop.Kind.WHILENOT;
				headExit = trueOut;
			}
			// no proper pre-loop head!
		}
		// we check if this could be a post-loop:
		Loop.Kind lastKind = null;
		E lastExit = null;
		if (last != null && last.isCond()) {
			// beware: simple back loops with multiple statements possible
			final E falseOut = last.getFalseOut();
			assert falseOut != null;
			final E trueOut = last.getTrueOut();
			assert trueOut != null;
			if (loop.hasHead(trueOut.getEnd())) {
				lastKind = Loop.Kind.DO_WHILE;
				lastExit = falseOut;
			} else if (loop.hasHead(falseOut.getEnd())) {
				lastKind = Loop.Kind.DO_WHILENOT;
				lastExit = trueOut;
			}
			// no proper post-loop last!
		}
		// now we check with some heuristics if pre-loop or post-loop is the prefered loop, this is
		// not always exact science...
		if (headKind != null && lastKind == null) {
			loop.setKind(headKind);
			assert headExit != null;
			loop.setFollow(headExit.getEnd());
			return loop;
		}
		if (headKind == null && lastKind != null) {
			loop.setKind(lastKind);
			assert lastExit != null;
			loop.setFollow(lastExit.getEnd());
			return loop;
		}
		// we have to compare the tails for head and last
		if (headKind != null && lastKind != null) {
			assert headExit != null && lastExit != null;
			final List<BB> headMembers = Lists.newArrayList();
			final Set<E> headExits = Sets.newHashSet();
			findBranch(loop, headExit, headMembers, headExits);
			if (containsEnd(headExits, lastExit.getEnd())) {
				loop.setKind(lastKind);
				loop.setFollow(lastExit.getEnd());
				return loop;
			}
			final List<BB> lastMembers = Lists.newArrayList();
			final Set<E> lastExits = Sets.newHashSet();
			findBranch(loop, lastExit, lastMembers, lastExits);
			if (containsEnd(lastExits, headExit.getEnd())) {
				loop.setKind(headKind);
				loop.setFollow(headExit.getEnd());
				return loop;
			}
			// we can decide like we want: we prefer pre-loops
			loop.setKind(headKind);
			loop.setFollow(headExit.getEnd());
			return loop;
		}
		loop.setKind(Loop.Kind.ENDLESS);
		return loop;
	}

	@Nonnull
	private Switch createSwitchStruct(@Nonnull final BB head) {
		final Switch switchStruct = new Switch(head);

		// for case reordering we need this as changeable lists
		final List<E> caseOuts = head.getOuts();
		final int size = caseOuts.size();

		final Set<E> exits = Sets.newHashSet();

		int hack = 10;
		cases: for (int i = 0; i < size; ++i) {
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
						if (--hack <= 0) {
							assert false : "Switch endless loop?";
							log.warn(getM() + ": Switch endless loop?");
							break cases;
						}
						head.moveOut(i--, caseOuts.size() - 1);
						continue cases;
					}
				}
				if (prevCaseIndex < i - 1) {
					head.moveOut(i, prevCaseIndex + 1);
				}
				if (caseOut.isSwitchDefault() && exits.size() == 1 && containsEnd(exits, caseBb)) {
					// TODO when we are the final fall-through without other follows...
					switchStruct.setKind(Kind.NO_DEFAULT);
					switchStruct.setFollow(caseBb);
					continue cases;
				}
				isFallThrough = true;
			}
			final List<BB> members = Lists.newArrayList();
			if (isFallThrough) { // TODO better check follows.contains(caseBb))?
				removeOutsWithEndFromSet(exits, caseBb);
				members.add(caseBb);
			}
			findBranch(switchStruct, caseOut, members, exits);
			switchStruct.addMembers(caseOut.getValue(), members);
		}
		if (switchStruct.getFollow() != null) {
			return switchStruct;
		}
		switchStruct.setKind(Switch.Kind.WITH_DEFAULT);
		// highest follow is switch struct follow
		BB switchFollow = null;
		for (final E exit : exits) {
			if (switchFollow == null || exit.getEnd().hasSourceBefore(switchFollow)) {
				switchFollow = exit.getEnd();
			}
		}
		if (switchFollow != null) {
			switchStruct.setFollow(switchFollow);
		}
		return switchStruct;
	}

	@Nonnull
	private Sync createSyncStruct(@Nonnull final BB head, @Nonnull final R syncR) {
		final Sync sync = new Sync(head);
		// BBs are always split behind MONITOR_ENTER! hence: struct starts at sequence out
		final E sequenceOut = head.getSequenceOut();
		if (sequenceOut == null || sequenceOut.isBack()) {
			assert false;
			return sync;
		}
		final List<BB> checkBbs = Lists.newArrayList(sequenceOut.getEnd());
		BB syncFollow = null;
		do {
			final BB checkBb = checkBbs.remove(0);
			// BBs are always split behind MONITOR_EXIT! hence: check last statement for exit
			final Statement stmt = checkBb.getFinalStmt();
			checkSyncEnd: if (stmt instanceof SynchronizedStatement) {
				final Op monitorOp = getOp(stmt);
				if (!(monitorOp instanceof MONITOR)) {
					assert false;
					break checkSyncEnd;
				}
				if (((MONITOR) monitorOp).getKind() != MONITOR.Kind.EXIT) {
					break checkSyncEnd;
				}
				if (syncR != getCfg().getInFrame(monitorOp).peek().toOriginal()) {
					break checkSyncEnd;
				}
				// gotcha! kill all exits, they are encoded in struct now

				if (handleSyncFinally(checkBb)) {
					// typical default finally-exit catch-handler is removed completely:
					// checKBb == finally can happen, if there is no explicit sync-exit, e.g.
					// because of embedded throw, see add checkBb below
					continue;
				}
				checkBb.removeFinalStmt();

				sync.addMember(null, checkBb);
				// find relevant follow and kill potential finally-exit catch-handler
				final List<E> outs = checkBb.getOuts(); // list modified current iteration
				for (int i = outs.size(); i-- > 0;) {
					final E syncOut = outs.get(i);
					if (syncOut.isBack()) {
						continue;
					}
					final BB succ = syncOut.getEnd();
					if (handleSyncFinally(succ)) {
						// typical default finally-exit catch-handler is removed completely
						checkBbs.remove(succ);
						continue;
					}
					if (!syncOut.isSequence()) {
						continue;
					}
					if (syncFollow == null || syncOut.getEnd().hasSourceBefore(syncFollow)) {
						// TODO check outer follow or reduce like cond
						syncFollow = syncOut.getEnd();
						continue;
					}
				}
				continue; // next checkBb
			}
			sync.addMember(null, checkBb);
			// deep recursion into out edges of this member
			for (final E out : checkBb.getOuts()) {
				if (out.isBack()) {
					continue;
				}
				// catches must also be added, e.g. if there is no explicit sync-exit
				final BB succ = out.getEnd();
				if (succ != syncFollow && !sync.hasMember(succ) && !checkBbs.contains(succ)) {
					checkBbs.add(succ);
				}
			}
		} while (!checkBbs.isEmpty());
		if (syncFollow != null) {
			sync.setFollow(syncFollow);
		}
		return sync;
	}

	@Nullable
	private BB findFollow(@Nonnull final Struct struct, @Nonnull final Set<E> exits) {
		BB follow = null;
		for (final E exit : exits) {
			assert exit != null;
			if (checkBranching(struct, exit)) {
				continue;
			}
			final BB findFollow = exit.getEnd();
			assert!struct.hasMember(findFollow);
			if (follow == null) {
				follow = findFollow;
				continue;
			}
			if (follow != findFollow && findFollow.hasSourceBefore(follow)) {
				createBlockStruct(struct, follow);
				follow = findFollow;
			}
		}
		return follow;
	}

	@Nonnull
	private AST getAst() {
		return getCfg().getCu().getAst();
	}

	private M getM() {
		return getCfg().getM();
	}

	/**
	 * Is sync head?
	 *
	 * Works even for trivial / empty sync sections, because BBs are always split behind
	 * MONITOR_ENTER (see {@link TrDataFlowAnalysis}).
	 *
	 * We shouldn't find any MONITOR_EXIT here because they are consumed by createSyncStruct.
	 *
	 * @param bb
	 *            BB
	 *
	 * @return {@code true} - is sync head
	 */
	private R isSyncHead(final BB bb) {
		final Statement statement = bb.getFinalStmt();
		// BBs are always split behind MONITOR_ENTER/EXIT! (see {@link TrDataFlowAnalysis})
		if (!(statement instanceof SynchronizedStatement)) {
			return null;
		}
		final Op monitorOp = Expressions.getOp(statement);
		if (!(monitorOp instanceof MONITOR)) {
			return null;
		}
		switch (((MONITOR) monitorOp).getKind()) {
		case ENTER:
			return getCfg().getInFrame(monitorOp).peek().toOriginal();
		case EXIT:
			log.warn(getM() + ": Unexpected synchronized exit in: " + bb);
			assert bb.getCatchIn() != null;
			bb.removeFinalStmt();
			break;
		default:
			log.warn(getM() + ": Unknown MONITOR type '" + ((MONITOR) monitorOp).getKind() + "'!");
			assert false;
		}
		return null;
	}

	private void transform() {
		final List<BB> bbs = getCfg().getPostorderedBbs();
		// for all nodes in _reverse_ postorder: find outer structs first
		nextBb: for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			if (bb == null) {
				// could have been removed by synchronized-check
				continue;
			}
			bb.sortOuts();

			// check for catch & loop structs first: catch and endless / post loop structs have
			// additional sub struct heads;
			// including nested catches & also post loops that cannot be mitigated by continue
			while (true) {
				final List<E> catches = findCatchOutmostUnhandled(bb);
				final List<E> backs = findLoopUnhandledBacks(bb);

				if (catches != null && (backs == null || isLoopInCatches(backs, catches))) {
					createCatchStruct(bb, catches);
					continue; // additional sub struct heads possible for catch
				}
				if (backs == null) {
					break; // exit: isn't catch or loop struct, try other structs
				}
				if (createLoopStruct(bb, backs).isPre()) {
					// exit: no additional struct head possible here
					continue nextBb;
				}
				// additional sub struct heads possible for endless / post loops
			}
			final R syncR = isSyncHead(bb); // also warn about unexpected exits
			if (syncR != null) {
				createSyncStruct(bb, syncR); // also consume/remove exits
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