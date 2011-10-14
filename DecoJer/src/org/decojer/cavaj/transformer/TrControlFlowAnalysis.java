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
package org.decojer.cavaj.transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.struct.Cond;
import org.decojer.cavaj.model.code.struct.Loop;
import org.decojer.cavaj.model.code.struct.Struct;
import org.decojer.cavaj.model.code.struct.Switch;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

/**
 * Transform Control Flow Analysis.
 * 
 * @author André Pankraz
 */
public class TrControlFlowAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrControlFlowAnalysis.class.getName());

	private static boolean dfsFindInnerLoopMembers(final Loop loop, final BB bb,
			final Set<BB> traversed) {
		// DFS
		traversed.add(bb);
		boolean loopSucc = false;
		boolean backEdge = false;
		for (final BB succBb : bb.getSuccBbs()) {
			// equal: check self back edge too
			if (bb.getPostorder() <= succBb.getPostorder()) {
				// back edge (continue, tail, inner loop, outer label-continue)
				if (succBb != loop.getHead()) {
					// unimportant back edge (inner loop, outer label-continue)
					continue;
				}
				backEdge = true;
				// don't track this edge any further
				continue;
			}
			if (loop.isMember(succBb)) {
				loopSucc = true;
				continue;
			}
			if (traversed.contains(succBb)) {
				continue;
			}
			// DFS
			if (dfsFindInnerLoopMembers(loop, succBb, traversed)) {
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
			if (loop.getTail() == null || loop.getTail().getOrder() < bb.getOrder()) {
				loop.setTail(bb);
			}
			return true;
		}
		return false;
	}

	// add successors till unknown predecessor node
	private static void dfsFindTail(final List<BB> members, final Set<BB> endNodes, final BB bb,
			final Struct struct) {
		// check important for switch fall-throughs with early member addition
		if (!members.contains(bb)) {
			if (bb.getPredBbs().size() <= 1) {
				// predecessor check unnecessary, but is also necessary as
				// startup for branch member search
				members.add(bb);
			} else {
				for (final BB predBb : bb.getPredBbs()) {
					if (predBb.getPostorder() <= bb.getPostorder()) {
						// ignore back edges
						continue;
					}
					if (!members.contains(predBb)) {
						endNodes.add(bb);
						return;
					}
				}
				endNodes.remove(bb);
				members.add(bb);
			}
		}
		for (final BB succBb : bb.getSuccBbs()) {
			if (members.contains(succBb)) {
				continue;
			}
			if (bb.getPostorder() <= succBb.getPostorder()) {
				// back edge to no-member successor, outer struct allready known
				// TODO handle pre loop continue
				continue;
			}
			// not from succBb, follow doesn't know
			if (struct != null) {
				if (struct instanceof Loop && ((Loop) struct).isTail(succBb)) {
					// TODO handle post loop continue
					continue;
				}
				if (struct.isFollow(succBb)) {
					// TODO handle break
					continue;
				}
			}
			// DFS
			dfsFindTail(members, endNodes, succBb, struct);
		}
	}

	private static boolean isCondHead(final BB bb) {
		if (!(bb.getFinalStatement() instanceof IfStatement)) {
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
			if (loop.isTail(bb) && loop.isPost()) {
				// no additional cond head possible
				return false;
			}
		}
		return true;
	}

	private static boolean isLoopHead(final BB bb) {
		for (final BB predBb : bb.getPredBbs()) {
			if (predBb.getPostorder() <= bb.getPostorder()) {
				// must be a back edge (eg. self loop), in Java only possible
				// for loop heads
				return true;
			}
		}
		return false;
	}

	private static boolean isSwitchHead(final BB bb) {
		// don't use successor number as indicator, normal switch with 2
		// successors (JDK 6: 1 case and default) possible
		return bb.getFinalStatement() instanceof SwitchStatement;
	}

	public static void transform(final CFG cfg) {
		new TrControlFlowAnalysis(cfg).transform();
	}

	public static void transform(final TD td) {
		final List<BD> bds = td.getBds();
		for (int i = 0; i < bds.size(); ++i) {
			final BD bd = bds.get(i);
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			if (cfg == null || cfg.isIgnore()) {
				continue;
			}
			try {
				transform(cfg);
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Cannot transform '" + cfg.getMd() + "'!", e);
				cfg.setError(true);
			}
		}
	}

	private final CFG cfg;

	private TrControlFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
	}

	private void findCondMembers(final Cond cond) {
		final BB headBb = cond.getHead();

		// cases with branches and values, in normal mode in correct order
		final List<BB> succBbs = headBb.getSuccBbs();
		final List<Object> succValues = headBb.getSuccValues();

		assert succBbs.size() == 2;

		final BB firstBb = succBbs.get(0);
		final BB secondBb = succBbs.get(1);

		final Object firstValue = succValues.get(0);
		final Object secondValue = succValues.get(1);

		assert firstValue == Boolean.TRUE && secondValue == Boolean.FALSE
				|| firstValue == Boolean.FALSE && secondValue == Boolean.TRUE;

		// TODO check problem: direct true back-edge in JDK 6 possible, order
		// smaller then
		final boolean negated = firstValue == Boolean.FALSE;

		if (!negated && this.cfg.getMd().getTd().getVersion() >= 50) {
			log("Uncommon usage of unnegated conditional in JDK 6:\n" + cond);
		}

		final List<BB> firstMembers = new ArrayList<BB>();
		final Set<BB> firstEndNodes = new HashSet<BB>();
		// don't follow back edges
		if (headBb.getPostorder() >= firstBb.getPostorder()) {
			dfsFindTail(firstMembers, firstEndNodes, firstBb, cond.getParent());
		}
		// possible conditional follow is back edge
		if (headBb.getPostorder() < secondBb.getPostorder()) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			for (final BB bb : firstMembers) {
				cond.addMember(firstValue, bb);
			}
			return;
		}
		// no else basic blocks
		if (firstEndNodes.contains(secondBb)) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			for (final BB bb : firstMembers) {
				cond.addMember(firstValue, bb);
			}
			cond.setFollow(secondBb);
			return;
		}

		// TODO only a trick for now, not ready!!!
		// e.g. if-continues, if-returns, if-throws => no else necessary
		if (firstEndNodes.size() == 0) {
			// normal in JDK 6 bytecode, ifnot-expressions
			cond.setType(negated ? Cond.IFNOT : Cond.IF);
			for (final BB bb : firstMembers) {
				cond.addMember(firstValue, bb);
			}
			cond.setFollow(secondBb);
			return;
		}

		final List<BB> secondMembers = new ArrayList<BB>();
		final Set<BB> secondEndNodes = new HashSet<BB>();
		// don't follow back edges
		if (headBb.getPostorder() >= secondBb.getPostorder()) {
			dfsFindTail(secondMembers, secondEndNodes, secondBb, cond.getParent());
		}
		if (secondEndNodes.contains(firstBb)) {
			// really bad, order is wrong!
			log("Order preservation not possible for cond:\n?" + cond);

			cond.setType(negated ? Cond.IF : Cond.IFNOT);
			for (final BB bb : secondMembers) {
				cond.addMember(secondValue, bb);
			}
			cond.setFollow(firstBb);
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
		for (final BB endNode : firstEndNodes) {
			if (firstEndNode == null || firstEndNode.getOrder() > endNode.getOrder()) {
				firstEndNode = endNode;
			}
		}
		BB secondEndNode = null;
		for (final BB endNode : secondEndNodes) {
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
		final BB headBb = loop.getHead();

		dfsFindInnerLoopMembers(loop, headBb, new HashSet<BB>());

		final BB tailBb = loop.getTail();
		assert tailBb != null;

		int headType = 0;
		BB headFollow = null;

		// WHILE && FOR => only 1 head statement because of iteration back edge,
		// FOR has trailing ExpressionStatements in the loop end node
		final List<Statement> headStatements = headBb.getStatements();
		if (headStatements.size() == 1 && headStatements.get(0) instanceof IfStatement) {
			final BB trueSuccBb = headBb.getSuccBb(Boolean.TRUE);
			final BB falseSuccBb = headBb.getSuccBb(Boolean.FALSE);
			if (loop.isMember(trueSuccBb) && !loop.isMember(falseSuccBb)) {
				// JDK 6: true is member, opPc of pre head > next member,
				// leading goto
				headType = Loop.WHILE;
				headFollow = falseSuccBb;
			} else if (loop.isMember(falseSuccBb) && !loop.isMember(trueSuccBb)) {
				// JDK 5: false is member, opPc of pre head < next member,
				// trailing goto (negated, check class javascript.Decompiler)
				headType = Loop.WHILENOT;
				headFollow = trueSuccBb;
			}
			// no proper pre head!
		}

		int tailType = 0;
		BB tailFollow = null;

		if (tailBb.getFinalStatement() instanceof IfStatement) {
			final BB trueSuccBb = tailBb.getSuccBb(Boolean.TRUE);
			final BB falseSuccBb = tailBb.getSuccBb(Boolean.FALSE);
			if (loop.isHead(trueSuccBb)) {
				tailType = Loop.DO_WHILE;
				tailFollow = falseSuccBb;
			} else if (loop.isHead(falseSuccBb)) {
				tailType = Loop.DO_WHILENOT;
				tailFollow = trueSuccBb;
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
			dfsFindTail(headMembers, headEndNodes, headFollow, loop.getParent());
			if (headEndNodes.contains(tailFollow)) {
				loop.setType(tailType);
				loop.setFollow(tailFollow);
				return;
			}
			final List<BB> tailMembers = new ArrayList<BB>();
			final Set<BB> tailEndNodes = new HashSet<BB>();
			dfsFindTail(tailMembers, tailEndNodes, tailFollow, loop.getParent());
			if (tailEndNodes.contains(headFollow)) {
				loop.setType(headType);
				loop.setFollow(headFollow);
				return;
			}
		}
		loop.setType(Loop.ENDLESS);
	}

	@SuppressWarnings("rawtypes")
	private void findSwitchMembers(final Switch switchStruct) {
		final BB headBb = switchStruct.getHead();

		// cases with branches and values, in normal mode in correct order
		final List<BB> succBbs = headBb.getSuccBbs();
		final int succNumber = succBbs.size();
		final List<Object> succValues = headBb.getSuccValues();

		int defaultIndex = -1;
		// first case can only have head as predecessor, else try case
		// reordering; fall-through follow-cases can have multiple predecessors
		int firstIndex = -1;
		// short check and find first node with 1 predecessor und default case
		for (int i = 0; i < succNumber; ++i) {
			final BB succBb = succBbs.get(i);
			assert succBb != null;

			final List<BB> predBbs = succBb.getPredBbs();
			assert predBbs != null;

			if (predBbs.size() == 1 && firstIndex == -1) {
				firstIndex = i;
			}
			assert predBbs.size() >= 1;
			assert predBbs.contains(headBb);

			final Object succValue = succValues.get(i);
			assert succValue instanceof List;

			if (((List) succValue).contains(null)) {
				assert defaultIndex == -1 : "Double Default Case!";

				defaultIndex = i;
			}
		}
		if (defaultIndex == -1) {
			log("Switch with head '" + headBb + "' has no default branch!");
			return;
		}
		if (firstIndex == -1) {
			log("Switch with head '" + headBb
					+ "' has no case branch with 1 predecessor, necessary for first case!");
			return;
		} else if (firstIndex != 0) {
			log("Switch with head '" + headBb
					+ "' has no first case branch with 1 predecessor, reordering!");
			final BB removedBb = succBbs.remove(firstIndex);
			final Object removedValue = succValues.remove(firstIndex);
			succBbs.add(0, removedBb);
			succValues.add(0, removedValue);
		}

		// TODO currently only quick and dirty checks
		int type = 0;
		if (defaultIndex != succNumber - 1) {
			// not last case branch?
			type = Switch.SWITCH_DEFAULT;
		}
		final BB defaultBb = succBbs.get(defaultIndex);
		if (defaultBb.getPredBbs().size() == 1) {
			// no fall-through follow case and no switch follow
			type = Switch.SWITCH_DEFAULT;
		}

		final Set<BB> endNodes = new HashSet<BB>();
		for (int i = 0; i < succNumber; ++i) {
			if (i == succNumber - 1) {
				if (type == 0) {
					type = Switch.SWITCH;
				}
				if (type == Switch.SWITCH) {
					break;
				}
			}
			final BB succBb = succBbs.get(i);

			final List<BB> members = new ArrayList<BB>();

			final List<BB> predBbs = succBb.getPredBbs();
			if (predBbs.size() > 1) {
				// fall-through follow case?
				if (!endNodes.remove(succBb)) {
					log("TODO Case reordering necessary? No proper follow case!");
				}
				members.add(succBb);
			}

			dfsFindTail(members, endNodes, succBb, switchStruct.getParent());
			for (final BB bb : members) {
				switchStruct.addMember(succValues.get(i), bb);
			}
			if (endNodes.contains(defaultBb) && i < succNumber - 2) {
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
		LOGGER.warning(this.cfg.getMd().toString() + ": " + message);
	}

	private void log(final String message, final Throwable e) {
		LOGGER.log(Level.SEVERE, this.cfg.getMd().toString() + ": " + message, e);
	}

	private void transform() {
		final List<BB> bbs = this.cfg.getPostorderedBbs();
		// top down struct, find outer first
		for (int postorder = bbs.size(); postorder-- > 0;) {
			final BB bb = bbs.get(postorder);
			// check loop first, could be a post / endless loop with
			// additional
			// sub struct heads
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