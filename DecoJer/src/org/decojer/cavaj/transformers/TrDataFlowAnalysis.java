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
import org.decojer.cavaj.model.code.V;
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
		final T m = R.merge(s1, s2);
		assert m != null;

		s2.read(m, false);
		s1.read(m, false);

		if (resultT == T.VOID) {
			return;
		}
		pushConst(resultT != null ? resultT : m);
	}

	private int executeMerge(final BB bb, final Op op) {
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
			final R aR = popRead(this.cfg.getDu().getArrayT(cop.getT())); // array
			pushConst(aR.getT().getComponentT()); // value
			break;
		}
		case AND: {
			final AND cop = (AND) op;
			evalBinaryIntBoolMath(cop.getT());
			break;
		}
		case ARRAYLENGTH: {
			popRead(T.REF); // array
			pushConst(T.INT); // length
			break;
		}
		case ASTORE: {
			final ASTORE cop = (ASTORE) op;
			final R vR = pop(cop.getT()); // value: no read here, more specific possible, see below
			popRead(T.INT); // index
			// don't use getArrayT(vR.getT()), wrong assignment direction for supertype,
			// e.g. java.lang.Object[] <- java.io.PrintWriter[] or int[] <- {byte,char}[]
			final R aR = popRead(this.cfg.getDu().getArrayT(cop.getT())); // array

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
			if (joinT == null || !vR.read(joinT, true)) {
				LOGGER.warning("Cannot store array value!");
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
				final R s = this.frame.peekSingle();
				pushMove(s);
				break;
			}
			case DUP_X1: {
				final R s1 = this.frame.popSingle();
				final R s2 = this.frame.popSingle();
				pushMove(s1);
				pushMove(s2);
				pushMove(s1);
				break;
			}
			case DUP_X2: {
				final R s1 = this.frame.popSingle();
				final R s2 = this.frame.pop();
				if (!s2.getT().isWide()) {
					final R s3 = this.frame.popSingle();
					pushMove(s1);
					pushMove(s3);
					pushMove(s2);
					pushMove(s1);
					break;
				}
				pushMove(s1);
				pushMove(s2);
				pushMove(s1);
				break;
			}
			case DUP2: {
				final R s1 = this.frame.peek();
				if (!s1.getT().isWide()) {
					final R s2 = this.frame.peekSingle(1);
					pushMove(s2);
					pushMove(s1);
					break;
				}
				pushMove(s1);
				break;
			}
			case DUP2_X1: {
				final R s1 = this.frame.pop();
				if (!s1.getT().isWide()) {
					final R s2 = this.frame.popSingle();
					final R s3 = this.frame.popSingle();
					pushMove(s2);
					pushMove(s1);
					pushMove(s3);
					pushMove(s2);
					pushMove(s1);
					break;
				}
				final R s3 = this.frame.pop();
				pushMove(s1);
				pushMove(s3);
				pushMove(s1);
				break;
			}
			case DUP2_X2: {
				final R s1 = this.frame.pop();
				if (!s1.getT().isWide()) {
					final R s2 = this.frame.popSingle();
					final R s3 = this.frame.pop();
					if (!s3.getT().isWide()) {
						final R s4 = this.frame.popSingle();
						pushMove(s2);
						pushMove(s1);
						pushMove(s4);
						pushMove(s3);
						pushMove(s2);
						pushMove(s1);
						break;
					}
					pushMove(s2);
					pushMove(s1);
					pushMove(s3);
					pushMove(s2);
					pushMove(s1);
					break;
				}
				final R s3 = this.frame.pop();
				if (!s3.getT().isWide()) {
					final R s4 = this.frame.popSingle();
					pushMove(s1);
					pushMove(s4);
					pushMove(s3);
					pushMove(s1);
					break;
				}
				pushMove(s1);
				pushMove(s3);
				pushMove(s1);
				break;
			}
			default:
				LOGGER.warning("Unknown DUP type '" + cop.getKind() + "'!");
			}
			break;
		}
		case FILLARRAY: {
			popRead(T.REF);
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
			r.inc(cop.getValue());
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
			final JSR cop = (JSR) op;

			final int subPc = cop.getTargetPc();
			// Spec, JSR/RET is stack-like:
			// http://docs.oracle.com/javase/specs/jvms/se7/jvms7.pdf
			final BB subBb = getTargetBb(subPc);
			bb.setSucc(subBb);
			// use common value (like Sub) instead of jsr-follow-address because of merge
			final Frame subFrame = this.cfg.getFrame(subPc);
			if (subFrame == null) {
				final Sub sub = new Sub(subPc);
				if (!this.frame.pushSub(sub)) {
					return -1;
				}
				pushConst(T.RET, sub);
				merge(subPc);
				return -1;
			}
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

			final RET ret = sub.getRet();
			if (ret != null) {
				// RET already visited, link RET BB to JSR follower and merge
				this.frame = new Frame(this.cfg.getFrame(ret.getPc()));
				if (loadRead(ret.getReg(), T.RET).getValue() != sub) {
					// don't assert here, need this get for frames return-address-null update
					LOGGER.warning("Incorrect sub!");
				}
				final BB retBb = this.pc2bbs[ret.getPc()];
				final int retPc = cop.getPc() + 1;
				retBb.setSucc(getTargetBb(retPc));
				// TODO rebuild frame for merge ret
				merge(retPc);
			}
			return -1;
		}
		case LOAD: {
			final LOAD cop = (LOAD) op;
			final R r = load(cop.getReg(), cop.getT());
			// no previous for stack
			pushMove(r);
			break;
		}
		case MONITOR: {
			popRead(T.REF);
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
			T t = cop.getT();
			for (int i = cop.getDimensions(); i-- > 0;) {
				popRead(T.INT);
				t = this.cfg.getDu().getArrayT(t);
			}
			pushConst(t);
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
				this.frame.popSingle();
				break;
			}
			case POP2:
				final R s1 = this.frame.pop();
				if (!s1.getT().isWide()) {
					this.frame.popSingle();
					break;
				}
				break;
			default:
				LOGGER.warning("Unknown POP type '" + cop.getKind() + "'!");
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
			final RET cop = (RET) op;
			final R r = loadRead(cop.getReg(), T.RET);
			// bytecode restriction: only called via matching JSR, Sub known as register value
			final Sub sub = (Sub) r.getValue();
			if (!this.frame.popSub(sub)) {
				return -1;
			}
			// remember RET for later JSRs to this Sub
			sub.setRet(cop);

			// link RET BB to all yet known JSR followers and merge, Sub BB incomings are JSRs
			final int subPc = sub.getPc();
			final BB subBb = this.pc2bbs[subPc];
			for (final E in : subBb.getIns()) {
				// JSR is last operation in previous BB
				final Op jsr = in.getStart().getFinalOp();
				final int retPc = jsr.getPc() + 1;
				bb.setSucc(getTargetBb(retPc));
				// TODO rebuild frame for merge ret
				merge(retPc);
			}
			return -1;
		}
		case RETURN: {
			final RETURN cop = (RETURN) op;
			final T returnT = cop.getT();
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

			final R storeR = store(cop.getReg(), r);
			final V debugV = this.cfg.getDebugV(cop.getReg(), nextPc);
			if (debugV != null) {
				storeR.setRealT(debugV.getT());
			}
			break;
		}
		case SUB: {
			final SUB cop = (SUB) op;
			evalBinaryIntMath(cop.getT());
			break;
		}
		case SWAP: {
			final R s1 = this.frame.pop();
			final R s2 = this.frame.pop();
			pushMove(s1);
			pushMove(s2);
			break;
		}
		case SWITCH: {
			final SWITCH cop = (SWITCH) op;

			// build sorted map: unique case pc -> matching case keys
			final TreeMap<Integer, List<Integer>> casePc2keys = Maps.newTreeMap();

			// add case branches
			final int[] caseKeys = cop.getCaseKeys();
			final int[] casePcs = cop.getCasePcs();
			for (int i = 0; i < caseKeys.length; ++i) {
				final int casePc = casePcs[i];
				List<Integer> keys = casePc2keys.get(casePc);
				if (keys == null) {
					keys = Lists.newArrayList();
					casePc2keys.put(casePc, keys); // pc-sorted
				}
				keys.add(caseKeys[i]);
			}
			// add default branch
			final int defaultPc = cop.getDefaultPc();
			List<Integer> keys = casePc2keys.get(defaultPc);
			if (keys == null) {
				keys = Lists.newArrayList();
				casePc2keys.put(defaultPc, keys);
			}
			keys.add(null);

			// now add successors, preserve pc-order as edge-order
			for (final Map.Entry<Integer, List<Integer>> casePc2keysEntry : casePc2keys.entrySet()) {
				keys = casePc2keysEntry.getValue();
				bb.addSwitchCase(getTargetBb(casePc2keysEntry.getKey()),
						keys.toArray(new Integer[keys.size()]));
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
			popRead(this.cfg.getDu().getT(Throwable.class));
			return -1;
		case XOR: {
			final XOR cop = (XOR) op;
			evalBinaryIntBoolMath(cop.getT());
			break;
		}
		default:
			LOGGER.warning("Operation '" + op + "' not handled!");
		}
		if (this.pc2bbs[nextPc] != null) {
			bb.setSucc(getTargetBb(nextPc));
			merge(nextPc);
			return -1;
		}
		merge(nextPc);
		return nextPc;
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

		// split basic block, new incoming block, adapt basic block pcs,
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

	private R load(final int i, final T t) {
		// start new register and TODO backpropagate alive for existing (read number)
		final R r = this.frame.load(i);
		if (!r.read(t, false)) {
			throw new RuntimeException("Incompatible type for register '" + i
					+ "'! Cannot assign '" + r + "' to '" + t + "'.");
		}
		if (r.getT() == T.RET) {
			// bytecode restriction: internal return address type can only be read once
			this.frame.store(i, null);
			return r;
		}
		this.frame.store(i, new R(this.pc, r.getT(), r.getValue(), Kind.LOAD, r));
		return r;
	}

	private R loadRead(final int i, final T t) {
		final R r = load(i, t);
		if (!r.read(t, true)) {
			throw new RuntimeException("Incompatible type for register '" + i + "'! Cannot read '"
					+ r + "' as '" + t + "'.");
		}
		return r;
	}

	private void merge(final int pc) {
		final Frame targetFrame = this.cfg.getFrame(pc);
		if (targetFrame == null) {
			// first visit for this target frame -> no BB join -> no type merge
			this.cfg.setFrame(pc, new Frame(this.frame));
			return;
		}
		assert targetFrame.size() == this.frame.size();

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
				mergeReplaceReg(targetBb, i, prevR, null);
				continue;
			}
			final T t = R.merge(prevR, newR);
			if (t == null) {
				// merge type is null? merge to null => replace previous register from here

				// FIXME dangerous if unknown super types...defer this op, remember merge register
				// with 2 inputs and try join only on read/re-store
				mergeReplaceReg(targetBb, i, prevR, null);
				continue;
			}
			// only here can we create or enhance a merge registers
			if (prevR.getKind() == Kind.MERGE && prevR.getPc() == pc) {
				// merge register already starts here, add new register
				prevR.merge(newR);
				continue;
			}
			// start new merge register
			mergeReplaceReg(targetBb, i, prevR, new R(pc, t, Kind.MERGE, prevR, newR));
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
				excR = new R(exc.getHandlerPc(), exc.getT() == null ? this.cfg.getDu().getT(
						Throwable.class) : exc.getT(), Kind.CONST);
			} else {
				if (handlerFrame.getTop() != 1) {
					LOGGER.warning("Handler stack for exception merge not of size 1!");
				}
				excR = handlerFrame.peek(); // reuse exception register
			}
			this.frame.push(excR);

			merge(exc.getHandlerPc());
		}
	}

	private void mergeReplaceReg(final BB bb, final int i, final R prevR, final R newR) {
		assert prevR != null;

		// BB possibly not visited yet => than: BB input frame known, but no operations exist
		Frame frame = this.cfg.getInFrame(bb); // but BB input frame cannot be null here
		R replacedR = frame.replaceReg(i, prevR, newR);
		if (replacedR == null) {
			return;
		}
		// replacement propagation to already known BB operations
		for (int j = 1; j < bb.getOps(); ++j) {
			frame = this.cfg.getInFrame(bb.getOp(j));
			replacedR = frame.replaceReg(i, replacedR, newR);
			if (replacedR == null) {
				return;
			}
		}
		// replacement propagation to next BB necessary
		for (final E out : bb.getOuts()) {
			final BB outBb = out.getEnd();
			if (this.cfg.getInFrame(outBb) == null) {
				// TODO currently only possible for exceptions, link later when really visited?!
				assert out.isCatch();

				continue;
			}
			// TODO BB has final RET? break forward replacement if jsrMerge and newR not in sub,
			// -> check newR order > subBb order...not possible?!!!!
			mergeReplaceReg(outBb, i, replacedR, newR);
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
			final TreeMap<Integer, List<T>> handlerPc2type = new TreeMap<Integer, List<T>>();
			for (final Exc exc : excs) {
				if (!exc.validIn(pc)) {
					continue;
				}
				final int handlerPc = exc.getHandlerPc();
				List<T> types = handlerPc2type.get(handlerPc);
				if (types == null) {
					types = Lists.newArrayList();
					handlerPc2type.put(handlerPc, types);
				}
				types.add(exc.getT());
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

	private R pop(final T t) {
		final R s = this.frame.pop();
		if (!s.read(t, false)) {
			// TODO bad infinispan.CacheImpl:...Incompatible local register type! Cannot assign
			// 'R25_MO: javax.transaction.SystemException' to 'java.lang.Throwable'.
			throw new RuntimeException("Incompatible type for stack register! Cannot assign '" + s
					+ "' to '" + t + "'.");
		}
		return s;
	}

	private R popRead(final T t) {
		final R s = pop(t);
		if (!s.read(t, true)) {
			throw new RuntimeException("Incompatible type for stack register! Cannot read '" + s
					+ "' as '" + t + "'.");
		}
		return s;
	}

	private R pushConst(final T t) {
		final R s = new R(this.pc, t, Kind.CONST);
		this.frame.push(s);
		return s;
	}

	private R pushConst(final T t, final Object value) {
		final R s = new R(this.pc, t, value, Kind.CONST);
		this.frame.push(s);
		return s;
	}

	private R pushMove(final R r) {
		final R s = new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r);
		this.frame.push(s);
		return s;
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
		final R prevR = this.frame.load(i);
		final R newR = prevR == null ? new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r)
				: new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r, prevR);
		this.frame.store(i, newR);
		return newR;
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
			bb.addOp(op);
			this.pc = executeMerge(bb, op);
			mergeExceptions(op); // execute has influence on this, read type reduce
		}
	}

}