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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.Exc;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.R.Kind;
import org.decojer.cavaj.model.code.Sub;
import org.decojer.cavaj.model.code.ops.ADD;
import org.decojer.cavaj.model.code.ops.ALOAD;
import org.decojer.cavaj.model.code.ops.AND;
import org.decojer.cavaj.model.code.ops.ASTORE;
import org.decojer.cavaj.model.code.ops.CAST;
import org.decojer.cavaj.model.code.ops.CMP;
import org.decojer.cavaj.model.code.ops.DIV;
import org.decojer.cavaj.model.code.ops.DUP;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.INC;
import org.decojer.cavaj.model.code.ops.INVOKE;
import org.decojer.cavaj.model.code.ops.JCMP;
import org.decojer.cavaj.model.code.ops.JCND;
import org.decojer.cavaj.model.code.ops.JSR;
import org.decojer.cavaj.model.code.ops.LOAD;
import org.decojer.cavaj.model.code.ops.MONITOR;
import org.decojer.cavaj.model.code.ops.MUL;
import org.decojer.cavaj.model.code.ops.NEG;
import org.decojer.cavaj.model.code.ops.NEW;
import org.decojer.cavaj.model.code.ops.NEWARRAY;
import org.decojer.cavaj.model.code.ops.OR;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.POP;
import org.decojer.cavaj.model.code.ops.PUSH;
import org.decojer.cavaj.model.code.ops.PUT;
import org.decojer.cavaj.model.code.ops.REM;
import org.decojer.cavaj.model.code.ops.RET;
import org.decojer.cavaj.model.code.ops.RETURN;
import org.decojer.cavaj.model.code.ops.SHL;
import org.decojer.cavaj.model.code.ops.SHR;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.code.ops.SUB;
import org.decojer.cavaj.model.code.ops.SWITCH;
import org.decojer.cavaj.model.code.ops.XOR;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Transformer: Data Flow Analysis and create CFG.
 * 
 * @author André Pankraz
 */
public final class TrDataFlowAnalysis {

	private final static Logger LOGGER = Logger.getLogger(TrDataFlowAnalysis.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrDataFlowAnalysis(cfg).transform();
	}

	private final CFG cfg;

	/**
	 * Current frame.
	 */
	private Frame frame;

	private final boolean isIgnoreExceptions;

	/**
	 * Remember open PCs.
	 */
	private LinkedList<Integer> openPcs;

	/**
	 * Current PC.
	 */
	private int pc;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2bbs;

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
		this.isIgnoreExceptions = this.cfg.getCu().check(DFlag.IGNORE_EXCEPTIONS);
	}

	private void evalBinaryIntBoolMath(final T t) {
		evalBinaryMath(t == T.INT ? T.AINT : t, null);
	}

	private void evalBinaryIntBoolMath(final T t, final T resultT) {
		evalBinaryMath(t == T.INT ? T.AINT : t, resultT);
	}

	private void evalBinaryIntMath(final T t) {
		evalBinaryMath(t, null);
	}

	private void evalBinaryMath(final T t, final T resultT) {
		final R s2 = pop(t);
		final R s1 = pop(t);

		// reduce to reasonable parameters pairs, e.g. BOOL, {SHORT,BOOL}-Constant -> both BOOL
		// hence: T.INT not sufficient for int/boolean operators like OR
		final T m = T.join(s1.getT(), s2.getT());
		assert m != null; // TODO ref1 == ref2 is allowed with result void (bool math)

		s2.assignTo(m);
		s1.assignTo(m);

		if (resultT == T.VOID) {
			return;
		}
		pushConst(resultT != null ? resultT : m);
	}

	private int execute(final BB bb, final Op op) {
		bb.addOp(op);
		this.frame = new Frame(this.cfg.getInFrame(op));
		int nextPc = op.getPc() + 1;
		switch (op.getOptype()) {
		case ADD: {
			final ADD cop = (ADD) op;
			evalBinaryIntMath(cop.getT());
			break;
		}
		case ALOAD: {
			final ALOAD cop = (ALOAD) op;
			popRead(T.INT); // index
			final R aR = popRead(getDu().getArrayT(cop.getT())); // array
			pushConst(aR.getT() == T.REF ? T.REF : aR.getT().getComponentT()); // value
			break;
		}
		case AND: {
			final AND cop = (AND) op;
			evalBinaryIntBoolMath(cop.getT());
			break;
		}
		case ARRAYLENGTH: {
			popRead(getDu().getArrayT(T.ANY));

			pushConst(T.INT); // length
			break;
		}
		case ASTORE: {
			final ASTORE cop = (ASTORE) op;
			final R vR = popRead(cop.getT()); // value: no read here, more specific possible, see
												// below
			popRead(T.INT); // index
			// don't use getArrayT(vR.getT()), wrong assignment direction for supertype,
			// e.g. java.lang.Object[] <- java.io.PrintWriter[] or int[] <- {byte,char}[]
			final R aR = popRead(getDu().getArrayT(cop.getT())); // array

			// now a more specific value read is possible...

			// aR could be more specific than vR (e.g. interface[] i = new instance[]), we have to
			// store-join types

			// class AnnotationBinding implements IAnnotationBinding {
			// public IMemberValuePairBinding[] getDeclaredMemberValuePairs()
			// (pairs is derived more specific org.eclipse.jdt.core.dom.MemberValuePairBinding[]
			// not
			// org.eclipse.jdt.core.dom.IMemberValuePairBinding)
			// pairs[counter++] = this.bindingResolver.getMemberValuePairBinding(valuePair);

			// more specific read possible here
			final T joinT = T.join(vR.getT(), aR.getT().getComponentT());
			// FIXME replace vR with super?
			// org.eclipse.jdt.internal.codeassist.InternalExtendedCompletionContext.getVisibleElements()
			// org.eclipse.jdt.internal.core.JavaElement.read(
			// {java.lang.Object,org.eclipse.jdt.core.IJavaElement,org.eclipse.core.runtime.IAdaptable})
			if (joinT == null || !vR.assignTo(joinT)) {
				log("Cannot store array value!");
			}
			break;
		}
		case CAST: {
			final CAST cop = (CAST) op;
			popRead(cop.getT());
			pushConst(cop.getToT());
			break;
		}
		case CMP: {
			final CMP cop = (CMP) op;
			evalBinaryMath(cop.getT(), T.INT);
			break;
		}
		case DIV: {
			final DIV cop = (DIV) op;
			evalBinaryIntMath(cop.getT());
			break;
		}
		case DUP: {
			final DUP cop = (DUP) op;
			switch (cop.getKind()) {
			case DUP: {
				final R s = peekSingle();
				push(s);
				break;
			}
			case DUP_X1: {
				final R s1 = popSingle();
				final R s2 = popSingle();
				push(s1);
				push(s2);
				push(s1);
				break;
			}
			case DUP_X2: {
				final R s1 = popSingle();
				final R s2 = pop();
				if (!s2.getT().isWide()) {
					final R s3 = popSingle();
					push(s1);
					push(s3);
					push(s2);
					push(s1);
					break;
				}
				push(s1);
				push(s2);
				push(s1);
				break;
			}
			case DUP2: {
				final R s1 = peek();
				if (!s1.getT().isWide()) {
					final R s2 = peekSingle(1);
					push(s2);
					push(s1);
					break;
				}
				push(s1);
				break;
			}
			case DUP2_X1: {
				final R s1 = pop();
				if (!s1.getT().isWide()) {
					final R s2 = popSingle();
					final R s3 = popSingle();
					push(s2);
					push(s1);
					push(s3);
					push(s2);
					push(s1);
					break;
				}
				final R s3 = pop();
				push(s1);
				push(s3);
				push(s1);
				break;
			}
			case DUP2_X2: {
				final R s1 = pop();
				if (!s1.getT().isWide()) {
					final R s2 = popSingle();
					final R s3 = pop();
					if (!s3.getT().isWide()) {
						final R s4 = popSingle();
						push(s2);
						push(s1);
						push(s4);
						push(s3);
						push(s2);
						push(s1);
						break;
					}
					push(s2);
					push(s1);
					push(s3);
					push(s2);
					push(s1);
					break;
				}
				final R s3 = pop();
				if (!s3.getT().isWide()) {
					final R s4 = popSingle();
					push(s1);
					push(s4);
					push(s3);
					push(s1);
					break;
				}
				push(s1);
				push(s3);
				push(s1);
				break;
			}
			default:
				log("Unknown DUP type '" + cop.getKind() + "'!");
			}
			break;
		}
		case FILLARRAY: {
			peek(T.AREF);
			break;
		}
		case GET: {
			final GET cop = (GET) op;
			final F f = cop.getF();
			if (!f.isStatic()) {
				popRead(f.getT());
			}
			pushConst(f.getValueT());
			break;
		}
		case GOTO: {
			final GOTO cop = (GOTO) op;
			// follow without new BB, lazy splitting, at target PC other catches possible!
			nextPc = cop.getTargetPc();
			break;
		}
		case INC: {
			final INC cop = (INC) op;
			final R r = loadRead(cop.getReg(), cop.getT());
			if (r.getValue() != null) {
				r.setValue(((Number) r.getValue()).intValue() + cop.getValue());
			}
			break;
		}
		case INSTANCEOF: {
			popRead(T.REF);
			// operation contains check-type as argument, not important here
			pushConst(T.BOOLEAN);
			break;
		}
		case INVOKE: {
			final INVOKE cop = (INVOKE) op;
			final M m = cop.getM();
			final T[] paramTs = m.getParamTs();
			for (int i = m.getParamTs().length; i-- > 0;) {
				popRead(paramTs[i]);
			}
			if (!m.isStatic()) {
				popRead(m.getT());
			}
			if (m.getReturnT() != T.VOID) {
				pushConst(m.getReturnT());
			}
			break;
		}
		case JCMP: {
			final JCMP cop = (JCMP) op;
			bb.setConds(getTargetBb(cop.getTargetPc()), getTargetBb(nextPc));
			// possible: bool != bool
			// see org.eclipse.jdt.core.dom.ASTMatcher.match(BooleanLiteral)
			evalBinaryIntBoolMath(cop.getT(), T.VOID);
			merge(nextPc);
			merge(cop.getTargetPc());
			return -1;
		}
		case JCND: {
			final JCND cop = (JCND) op;
			bb.setConds(getTargetBb(cop.getTargetPc()), getTargetBb(nextPc));
			popRead(cop.getT());
			merge(nextPc);
			merge(cop.getTargetPc());
			return -1;
		}
		case JSR: {
			final JSR jsr = (JSR) op;

			final int subPc = jsr.getTargetPc();
			// Spec, JSR/RET is stack-like:
			// http://docs.oracle.com/javase/specs/jvms/se7/jvms7.pdf
			final BB subBb = getTargetBb(subPc);
			bb.setSucc(subBb);
			// use common value (we take Sub) instead of jsr-follow-address because of merge
			final Frame subFrame = this.cfg.getFrame(subPc);
			if (subFrame == null) {
				// never bean as this sub -> create new jsr-follow-address (Sub) and merge -> return
				final Sub sub = new Sub(subPc);
				if (!this.frame.pushSub(sub)) {
					return -1;
				}
				pushConst(T.RET, sub);
				merge(subPc);
				return -1;
			}
			// already visited this sub -> restore jsr-follow-address (Sub) and merge -> check RET
			final R subR = subFrame.peekSub(this.frame.getTop(), subPc);
			if (subR == null) {
				return -1;
			}
			final Sub sub = (Sub) subR.getValue();
			if (!this.frame.pushSub(sub)) {
				return -1;
			}
			this.frame.push(subR);
			merge(subPc);
			// RET already visited -> link RET BB to JSR follower and merge
			final RET ret = sub.getRet();
			if (ret == null) {
				return -1;
			}
			this.frame = new Frame(this.cfg.getFrame(ret.getPc()));
			if (loadRead(ret.getReg(), T.RET).getValue() != sub) {
				log("Incorrect sub!");
			}
			final BB retBb = this.pc2bbs[ret.getPc()];
			final int jsrFollowPc = jsr.getPc() + 1;
			retBb.setSucc(getTargetBb(jsrFollowPc));
			// modify RET frame for untouched registers in sub
			final Frame jsrFrame = this.cfg.getInFrame(jsr);
			for (int i = this.frame.size(); i-- > 0;) {
				if (jumpOverSub(ret, i)) {
					this.frame.store(i, jsrFrame.load(i));
				}
			}
			merge(jsrFollowPc);
			return -1;
		}
		case LOAD: {
			final LOAD cop = (LOAD) op;
			final R r = load(cop.getReg(), cop.getT());
			// no previous for stack
			push(r);
			break;
		}
		case MONITOR: {
			final MONITOR cop = (MONITOR) op;
			popRead(T.REF);
			merge(nextPc);
			if (cop.getKind() == MONITOR.Kind.ENTER) {
				// always split, even for trivial / empty synchronize-blocks without
				// rethrow-handlers: else we would be forced to check & remember header nodes and
				// statement number for the later control flow analysis
				bb.setSucc(getTargetBb(nextPc));
				return -1;
			}
			break;
		}
		case MUL: {
			final MUL cop = (MUL) op;
			evalBinaryIntMath(cop.getT());
			break;
		}
		case NEG: {
			final NEG cop = (NEG) op;
			final R r = popRead(cop.getT());
			pushConst(r.getT());
			break;
		}
		case NEW: {
			final NEW cop = (NEW) op;
			pushConst(cop.getT());
			break;
		}
		case NEWARRAY: {
			final NEWARRAY cop = (NEWARRAY) op;
			for (int i = cop.getDimensions(); i-- > 0;) {
				popRead(T.INT);
			}
			pushConst(cop.getT());
			break;
		}
		case OR: {
			final OR cop = (OR) op;
			evalBinaryIntBoolMath(cop.getT());
			break;
		}
		case POP: {
			final POP cop = (POP) op;
			// no new register or type reduction necessary, simply let it die off
			switch (cop.getKind()) {
			case POP: {
				popSingle();
				break;
			}
			case POP2:
				final R s1 = pop();
				if (!s1.getT().isWide()) {
					popSingle();
					break;
				}
				break;
			default:
				log("Unknown POP type '" + cop.getKind() + "'!");
			}
			break;
		}
		case PUSH: {
			final PUSH cop = (PUSH) op;
			// no previous for stack
			pushConst(cop.getT(), cop.getValue());
			break;
		}
		case PUT: {
			final PUT cop = (PUT) op;
			final F f = cop.getF();
			popRead(f.getValueT());
			if (!f.isStatic()) {
				popRead(f.getT());
			}
			break;
		}
		case REM: {
			final REM cop = (REM) op;
			evalBinaryIntMath(cop.getT());
			break;
		}
		case RET: {
			final RET ret = (RET) op;
			final R r = loadRead(ret.getReg(), T.RET);
			// bytecode restriction: only called via matching JSR, Sub known as register value
			final Sub sub = (Sub) r.getValue();
			if (!this.frame.popSub(sub)) {
				return -1;
			}
			// remember RET for later JSRs to this Sub
			sub.setRet(ret);

			// link RET BB to all yet known JSR followers and merge, Sub BB incomings are JSRs
			final int subPc = sub.getPc();
			final BB subBb = this.pc2bbs[subPc];
			for (final E in : subBb.getIns()) {
				// JSR is last operation in previous BB
				final Op jsr = in.getStart().getFinalOp();
				final int jsrFollowPc = jsr.getPc() + 1;
				bb.setSucc(getTargetBb(jsrFollowPc));
				// modify RET frame for untouched registers in sub
				final Frame jsrFrame = this.cfg.getInFrame(jsr);
				for (int i = this.frame.size(); i-- > 0;) {
					if (jumpOverSub(ret, i)) {
						this.frame.store(i, jsrFrame.load(i));
					}
				}
				merge(jsrFollowPc);
			}
			return -1;
		}
		case RETURN: {
			final RETURN cop = (RETURN) op;
			final T returnT = this.cfg.getMd().getReturnT();
			assert cop.getT().isAssignableFrom(returnT);

			if (returnT != T.VOID) {
				popRead(returnT); // just read type reduction
			}
			return -1;
		}
		case SHL: {
			final SHL cop = (SHL) op;
			popRead(cop.getShiftT());
			popRead(cop.getT());
			pushConst(cop.getT());
			break;
		}
		case SHR: {
			final SHR cop = (SHR) op;
			popRead(cop.getShiftT());
			popRead(cop.getT());
			pushConst(cop.getT());
			break;
		}
		case STORE: {
			final STORE cop = (STORE) op;
			// The astore instruction is used with an objectref of type returnAddress when
			// implementing the finally clauses of the Java programming language (see Section
			// 7.13, "Compiling finally"). The aload instruction cannot be used to load a value
			// of type returnAddress from a local variable onto the operand stack. This
			// asymmetry with the astore instruction is intentional.
			final R r = popRead(cop.getT());
			store(cop.getReg(), r);
			// final R storeR = store(cop.getReg(), r);
			// final V debugV = this.cfg.getDebugV(cop.getReg(), nextPc);
			// if (debugV != null) {
			// storeR.setRealT(debugV.getT());
			// }
			break;
		}
		case SUB: {
			final SUB cop = (SUB) op;
			evalBinaryIntMath(cop.getT());
			break;
		}
		case SWAP: {
			final R s1 = pop();
			final R s2 = pop();
			push(s1);
			push(s2);
			break;
		}
		case SWITCH: {
			final SWITCH cop = (SWITCH) op;

			// build sorted map: unique case pc -> matching case keys
			final TreeMap<Integer, List<Integer>> casePc2values = Maps.newTreeMap();

			// add case branches
			final int[] caseKeys = cop.getCaseKeys();
			final int[] casePcs = cop.getCasePcs();
			for (int i = 0; i < caseKeys.length; ++i) {
				final int casePc = casePcs[i];
				List<Integer> keys = casePc2values.get(casePc);
				if (keys == null) {
					keys = Lists.newArrayList();
					casePc2values.put(casePc, keys); // pc-sorted
				}
				keys.add(caseKeys[i]);
			}
			// add default branch
			final int defaultPc = cop.getDefaultPc();
			List<Integer> caseValues = casePc2values.get(defaultPc);
			if (caseValues == null) {
				caseValues = Lists.newArrayList();
				casePc2values.put(defaultPc, caseValues);
			}
			caseValues.add(null);

			// now add successors, preserve pc-order as edge-order
			for (final Map.Entry<Integer, List<Integer>> casePc2valuesEntry : casePc2values
					.entrySet()) {
				caseValues = casePc2valuesEntry.getValue();
				bb.addSwitchCase(getTargetBb(casePc2valuesEntry.getKey()),
						caseValues.toArray(new Object[caseValues.size()]));
			}

			popRead(T.INT);
			merge(cop.getDefaultPc());
			for (final int casePc : cop.getCasePcs()) {
				merge(casePc);
			}
			return -1;
		}
		case THROW:
			// just type reduction
			popRead(getDu().getT(Throwable.class));
			return -1;
		case XOR: {
			final XOR cop = (XOR) op;
			evalBinaryIntBoolMath(cop.getT());
			break;
		}
		default:
			log("Operation '" + op + "' not handled!");
		}
		if (this.pc2bbs[nextPc] != null) {
			bb.setSucc(getTargetBb(nextPc));
			merge(nextPc);
			return -1;
		}
		merge(nextPc);
		return nextPc;
	}

	private DU getDu() {
		return this.cfg.getDu();
	}

	/**
	 * Get target BB for PC. Split or create new if necessary.
	 * 
	 * @param pc
	 *            target PC
	 * @return target BB
	 */
	private BB getTargetBb(final int pc) {
		final BB bb = this.pc2bbs[pc]; // get BB for target PC
		if (bb == null) {
			// PC not processed yet
			this.openPcs.add(pc);
			return newBb(pc);
		}
		// found BB has target PC as first PC => return BB, no split necessary
		if (bb.getPc() == pc) {
			return bb;
		}
		// split BB, new incoming block, adapt BB pcs,
		// it's necessary to preserve the outgoing block for back edges to same BB!!!
		final BB newInBb = newBb(bb.getPc());
		bb.moveIns(newInBb);
		newInBb.setSucc(bb);
		while (bb.getOps() > 0 && bb.getOp(0).getPc() != pc) {
			final Op op = bb.removeOp(0);
			newInBb.addOp(op);
			this.pc2bbs[op.getPc()] = newInBb;
		}
		bb.setPc(pc); // necessary because we must preserve outgoing BB
		return bb;
	}

	private boolean jumpOverSub(final RET ret, final int i) {
		final Frame retFrame = this.cfg.getFrame(ret.getPc());
		final R retR = retFrame.load(ret.getReg());
		final Sub sub = (Sub) retR.getValue();
		final Frame subFrame = this.cfg.getFrame(sub.getPc());

		final R regAtSub = subFrame.load(i);
		final R regAtRet = retFrame.load(i);

		if (regAtSub != regAtRet) {
			return false;
		}
		// are equal...null or merge at sub pc?
		if (regAtRet == null) {
			return true;
		}
		// no initial merge? simply route through, even if not changed in sub
		if (regAtRet.getKind() != R.Kind.MERGE) {
			return false;
		}
		if (regAtRet.getPc() != sub.getPc()) {
			return false;
		}
		// TODO check merge-ins? check null-merges in sub?
		return true;
	}

	private R load(final int i, final T t) {
		// start new register and TODO backpropagate alive for existing (read number)
		final R r = this.frame.load(i);
		if (!r.assignTo(t)) {
			throw new RuntimeException("Incompatible type for register '" + i
					+ "'! Cannot assign '" + r + "' to '" + t + "'.");
		}
		if (r.getT() == T.RET) {
			// bytecode restriction: internal return address type can only be read once
			this.frame.store(i, null);
		} else {
			this.frame.store(i, new R(this.pc, r.getT(), Kind.READ, r));
		}
		return r;
	}

	private R loadRead(final int i, final T t) {
		final R r = load(i, t);
		markAlive(this.pc2bbs[this.pc], i);
		return r;
	}

	private void log(final String message) {
		LOGGER.warning(this.cfg.getMd() + ": " + message);
	}

	private void markAlive(final BB bb, final int i) {
		for (int j = bb.getOps(); j-- > 0;) {
			final Op op = bb.getOp(j);
			final Frame frame = this.cfg.getInFrame(op);
			if (frame.isAlive(i)) {
				return;
			}
			frame.markAlive(i);
			if (true) {
				return;
			}
			final R r = frame.load(i);
			if (r.getPc() == op.getPc()) {
				// register created through current operation
				switch (r.getKind()) {
				case CONST:
					break;
				case MERGE:
					// TODO ??? delete merge? may be overlaying in frame
				case MOVE:
					// TODO
				}
				return;
			}
		}
		// TODO propagate to bb.ins
	}

	private void merge(final int pc) {
		final Frame targetFrame = this.cfg.getFrame(pc);
		if (targetFrame == null) {
			// first visit for this target frame -> no BB join -> no type merge
			this.cfg.setFrame(pc, new Frame(this.frame));
			return;
		}
		assert targetFrame.size() == this.frame.size();

		// TODO merge Sub

		// target frame has already been visited -> BB join -> type merge
		final BB targetBb = this.pc2bbs[pc];
		for (int i = targetFrame.size(); i-- > 0;) {
			final R prevR = targetFrame.load(i);
			final R newR = this.frame.load(i);
			if (prevR == newR) {
				continue;
			}
			// register merge necessary, all following conditions can only happen at BB join point,
			// that means a new BB start!
			if (prevR == null) {
				// previous register is null? merge to null => nothing to do
				continue;
			}
			if (newR == null) {
				// new register is null? merge to null => replace previous register from here
				replaceBbReg(targetBb, i, prevR, null);
				continue;
			}
			final T t = T.join(prevR.getT(), newR.getT());
			if (t == null) {
				// merge type is null? merge to null => replace previous register from here

				// FIXME dangerous if unknown super types...defer this op, remember merge register
				// with 2 inputs and try join only on read/re-store
				replaceBbReg(targetBb, i, prevR, null);
				continue;
			}
			// only here can we create or enhance a merge registers
			if (prevR.getKind() == Kind.MERGE && prevR.getPc() == pc) {
				// merge register already starts here, add new register
				prevR.addInMerge(t, newR);
				continue;
			}
			// start new merge register
			replaceBbReg(targetBb, i, prevR, new R(pc, t, Kind.MERGE, prevR, newR));
		}
	}

	private void mergeExceptions(final Op op) {
		if (this.isIgnoreExceptions || this.cfg.getExcs() == null) {
			return;
		}
		for (final Exc exc : this.cfg.getExcs()) {
			if (!exc.validIn(op.getPc())) {
				continue;
			}
			this.frame = new Frame(this.cfg.getInFrame(op));

			// in handler start frame the stack just consists of exception type
			this.frame.clear();
			final Frame handlerFrame = this.cfg.getFrame(exc.getHandlerPc());
			R excR;
			if (handlerFrame == null) {
				// null is <any> (means Java finally) -> Throwable
				excR = new R(exc.getHandlerPc(), exc.getT() == null ? getDu().getT(Throwable.class)
						: exc.getT(), Kind.CONST);
			} else {
				if (handlerFrame.getTop() != 1) {
					log("Handler stack for exception merge not of size 1!");
				}
				excR = handlerFrame.peek(); // reuse exception register
			}
			this.frame.push(excR);

			merge(exc.getHandlerPc());
		}
	}

	private BB newBb(final int pc) {
		final BB bb = this.cfg.newBb(pc);
		this.pc2bbs[pc] = bb;
		if (!this.isIgnoreExceptions) {
			final Exc[] excs = this.cfg.getExcs();
			if (excs == null) {
				return bb;
			}
			// build sorted map: unique handler pc -> matching handler types
			final TreeMap<Integer, List<T>> handlerPc2type = Maps.newTreeMap();
			// remember handled exception types...see below
			final List<T> handledTypes = Lists.newLinkedList();
			excs: for (final Exc exc : excs) {
				if (!exc.validIn(pc)) {
					continue;
				}
				final T t = exc.getT();
				// exception type has already been handled? exception handlers are ordered!
				for (final T handledType : handledTypes) {
					// cannot always work with unsufficient type information
					if (handledType == null || handledType.isAssignableFrom(t)) {
						continue excs;
					}
				}
				handledTypes.add(t);
				// extend sorted map for successors
				final int handlerPc = exc.getHandlerPc();
				List<T> types = handlerPc2type.get(handlerPc);
				if (types == null) {
					types = Lists.newArrayList();
					handlerPc2type.put(handlerPc, types);
				}
				types.add(t);
			}
			// now add successors
			for (final Map.Entry<Integer, List<T>> handlerPc2typeEntry : handlerPc2type.entrySet()) {
				final List<T> types = handlerPc2typeEntry.getValue();
				bb.addCatchHandler(getTargetBb(handlerPc2typeEntry.getKey()), handlerPc2typeEntry
						.getValue().toArray(new T[types.size()]));
			}
		}
		return bb;
	}

	private R peek() {
		return this.frame.peek();
	}

	private R peek(final T t) {
		final R s = this.frame.peek();
		if (!s.assignTo(t)) {
			throw new RuntimeException("Incompatible type for stack register! Cannot assign '" + s
					+ "' to '" + t + "'.");
		}
		return s;
	}

	private R peekSingle() {
		return peekSingle(0);
	}

	private R peekSingle(final int i) {
		final R s = this.frame.peek(i);
		if (s.getT().isWide()) {
			log("Peek '" + i + "' attempts to split long or double on the stack!");
		}
		return s;
	}

	private R pop() {
		return this.frame.pop();
	}

	private R pop(final T t) {
		final R s = this.frame.pop();
		if (!s.assignTo(t)) {
			throw new RuntimeException("Incompatible type for stack register! Cannot assign '" + s
					+ "' to '" + t + "'.");
		}
		return s;
	}

	private R popRead(final T t) {
		final R s = pop(t);
		// 2 tricks: current operation is last in current BB - operation propagation not necessary,
		// pop first and than the frame size is the register index for the highest stack register
		markAlive(this.pc2bbs[this.pc], this.frame.size());
		return s;
	}

	private R popSingle() {
		final R s = this.frame.pop();
		if (s.getT().isWide()) {
			log("Pop attempts to split long or double on the stack!");
		}
		return s;
	}

	private R push(final R r) {
		return this.frame.push(new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r));
	}

	private R pushConst(final T t) {
		return this.frame.push(new R(this.pc, t, Kind.CONST));
	}

	private R pushConst(final T t, final Object value) {
		return this.frame.push(new R(this.pc, t, value, Kind.CONST));
	}

	private void replaceBbReg(final BB bb, final int i, final R prevR, final R newR) {
		assert prevR != null;

		// BB possibly not visited yet => than: BB input frame known, but no operations exist,
		// but BB input frame cannot be null here
		R replacedR = replaceFrameReg(bb.getPc(), i, prevR, newR);
		if (replacedR == null) {
			return;
		}
		// replacement propagation to already known BB operations
		for (int j = 1; j < bb.getOps(); ++j) {
			replacedR = replaceFrameReg(bb.getOp(j).getPc(), i, replacedR, newR);
			if (replacedR == null) {
				return;
			}
		}
		// final operation is RET -> modify newR for untouched registers in sub
		final boolean jumpOverSub;
		final Op finalOp = bb.getFinalOp();
		if (finalOp instanceof RET) {
			jumpOverSub = jumpOverSub((RET) finalOp, i);
		} else {
			jumpOverSub = false;
		}
		// replacement propagation to next BB necessary
		for (final E out : bb.getOuts()) {
			final BB outBb = out.getEnd();
			if (this.cfg.getInFrame(outBb) == null) {
				// possible for freshly splitted catch-handlers that havn't been visited yet
				assert out.isCatch() : out;

				continue;
			}
			// final operation is RET -> modify newR for untouched registers in sub
			if (jumpOverSub) {
				final Frame jsrFrame = this.cfg.getFrame(outBb.getPc() - 1);
				replaceBbReg(outBb, i, replacedR, jsrFrame.load(i));
				continue;
			}
			replaceBbReg(outBb, i, replacedR, newR);
		}
	}

	private R replaceFrameReg(final int pc, final int i, final R prevR, final R newR) {
		assert prevR != null;

		final Frame frame = this.cfg.getFrame(pc);
		if (i < frame.size()) {
			final R frameR = frame.load(i);
			if (prevR == frameR) {
				frame.store(i, newR);
				return prevR;
			}
		}
		// stack value already used or new register from here on -> stop replace,
		// but here we could have MOVEs or OPs into other registers
		for (int j = frame.size(); j-- > 0;) {
			final R frameR = frame.load(j);
			if (frameR != null) {
				// TODO not sufficient...setT() must forward the types
				frameR.replaceIn(prevR, newR);
			}
		}
		return null;
	}

	/**
	 * Exception block changes in BB? -> split necessary!
	 * 
	 * @param bb
	 *            BB
	 * @param op
	 *            operation
	 * @return original BB or new BB for beginning exception block
	 */
	private BB splitExceptions(final BB bb, final Op op) {
		if (this.isIgnoreExceptions || this.cfg.getExcs() == null) {
			return bb;
		}
		// could happen with GOTO-mode: create no entry in BB, currently unused
		assert bb.getPc() == this.pc || bb.getOps() > 0;

		for (final Exc exc : this.cfg.getExcs()) {
			if (exc.validIn(this.pc)) {
				if (exc.validIn(bb.getPc())) {
					// exception is valid - has been valid at BB entry -> OK
					continue;
				}
			} else {
				// exception endPc is eclusive, but often points to final GOTO or RETURN in
				// try-block, this is especially not usefull for returns with values!
				if (!exc.validIn(bb.getPc()) || op.getPc() == exc.getEndPc()
						&& (op instanceof GOTO || op instanceof RETURN)) {
					// exception isn't valid - hasn't bean valid at BB entry -> OK
					continue;
				}
			}
			// at least one exception has changed, newBb() links exceptions
			final BB succBb = newBb(this.pc);
			bb.setSucc(succBb);
			return succBb;
		}
		return bb;
	}

	private R store(final int i, final R r) {
		return this.frame.store(i, new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r));
	}

	private void transform() {
		this.cfg.initFrames();

		// start with PC 0 and new BB
		this.pc = 0;
		final Op[] ops = this.cfg.getOps();
		this.pc2bbs = new BB[ops.length];
		this.openPcs = Lists.newLinkedList();
		BB bb = newBb(0); // need pc2bb and openPcs
		this.cfg.setStartBb(bb);

		while (true) {
			final Op op;
			if (this.pc < 0) {
				// next open pc?
				if (this.openPcs.isEmpty()) {
					break;
				}
				this.pc = this.openPcs.removeFirst();
				op = ops[this.pc];
				bb = this.pc2bbs[this.pc];
			} else {
				op = ops[this.pc];
				bb = splitExceptions(bb, op); // may change with exception boundary
				this.pc2bbs[this.pc] = bb;
			}
			this.pc = execute(bb, op);
			mergeExceptions(op); // execute has influence on this, read type reduce
		}
	}

}