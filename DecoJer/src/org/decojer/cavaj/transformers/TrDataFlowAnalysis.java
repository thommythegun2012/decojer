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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.DU;
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
import org.decojer.cavaj.model.code.ops.TypedOp;
import org.decojer.cavaj.model.code.ops.XOR;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Transformer: Data Flow Analysis and create CFG.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrDataFlowAnalysis {

	/**
	 * Transform CFG.
	 *
	 * @param cfg
	 *            CFG
	 */
	public static void transform(@Nonnull final CFG cfg) {
		new TrDataFlowAnalysis(cfg).transform();
	}

	@Getter(AccessLevel.PROTECTED)
	@Nonnull
	private final CFG cfg;

	/**
	 * Current BB.
	 */
	private BB currentBb;

	/**
	 * Current frame.
	 */
	private Frame currentFrame;

	private final boolean isIgnoreExceptions;

	/**
	 * Remember open PCs.
	 */
	private LinkedList<Integer> openPcs;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2bbs;

	private TrDataFlowAnalysis(@Nonnull final CFG cfg) {
		this.cfg = cfg;
		this.isIgnoreExceptions = getCfg().getCu().check(DFlag.IGNORE_EXCEPTIONS);
	}

	private boolean checkRegisterAccessInSub(final int i, final RET ret) {
		final Frame retFrame = getFrame(ret.getPc());
		assert retFrame != null;
		final R regAtRet = retFrame.load(i);

		final Sub sub = (Sub) retFrame.load(ret.getReg()).getValue();

		final Frame subFrame = getFrame(sub.getPc());
		assert subFrame != null;
		final R regAtSub = subFrame.load(i);

		return regAtRet != regAtSub; // register changed somewhere in sub
	}

	private void evalBinaryMath(final TypedOp op) {
		evalBinaryMath(op, null);
	}

	private void evalBinaryMath(final TypedOp op, final T resultT) {
		// AND, OR, XOR and JCMP can have T.BOOLEAN and T.INT!
		final T t = op.getT();

		final R s2 = popRead(t);
		final R s1 = popRead(t);

		// TODO calc value
		// s1 and s2 can have values, e.g. in a loop with ++ and JCMP
		// TODO merge delete value for loop, com.google.common.base.CharMatcher.setBits

		// reduce to reasonable parameters pairs, e.g. BOOL, {SHORT,BOOL}-Constant -> both BOOL
		final T intersectT = T.intersect(s1.getT(), s2.getT());
		assert intersectT != null : getM() + ": intersect to null not allowed";
		// TODO ref1 == ref2 is allowed with result void (bool math)

		s2.assignTo(intersectT);
		s1.assignTo(intersectT);

		if (resultT != null) {
			if (resultT != T.VOID) {
				pushConst(resultT);
			}
			return;
		}
		// TODO byte/short/char -> int
		if (intersectT.is(T.BOOLEAN, T.INT)) {
			pushBoolmath(intersectT, s1, s2);
		} else {
			pushConst(intersectT);
		}
	}

	@SuppressWarnings("null")
	private int execute() {
		final int currentPc = getCurrentPc();
		final Op op = getOp(currentPc);
		this.currentBb.addOp(op);
		int nextPc = currentPc + 1;
		switch (op.getOptype()) {
		case ADD: {
			evalBinaryMath((ADD) op);
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
			evalBinaryMath((AND) op);
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
			final T intersectT = T.intersect(vR.getT(), aR.getT().getComponentT());
			// FIXME replace vR with super?
			// org.eclipse.jdt.internal.codeassist.InternalExtendedCompletionContext.getVisibleElements()
			// org.eclipse.jdt.internal.core.JavaElement.read(
			// {java.lang.Object,org.eclipse.jdt.core.IJavaElement,org.eclipse.core.runtime.IAdaptable})
			if (intersectT == null || !vR.assignTo(intersectT)) {
				log.warn(getM() + ": Cannot store array value!");
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
			evalBinaryMath((CMP) op, T.INT);
			break;
		}
		case DIV: {
			evalBinaryMath((DIV) op);
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
				if (!s2.isWide()) {
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
				if (!s1.isWide()) {
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
				if (!s1.isWide()) {
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
				if (!s1.isWide()) {
					final R s2 = popSingle();
					final R s3 = pop();
					if (!s3.isWide()) {
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
				if (!s3.isWide()) {
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
				log.warn(getM() + ": Unknown DUP type '" + cop.getKind() + "'!");
			}
			break;
		}
		case FILLARRAY: {
			peek(T.REF);
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
			// read-store necessary, so that we can change the const value,
			// char c = this.c++; is possible, even though char c = this.c + 1 would complain
			final R r = store(cop.getReg(), loadRead(cop.getReg(), cop.getT()));
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
			this.currentBb.setConds(getTargetBb(cop.getTargetPc()), getTargetBb(nextPc));
			// possible: bool != bool
			// see org.eclipse.jdt.core.dom.ASTMatcher.match(BooleanLiteral)
			evalBinaryMath(cop, T.VOID);
			merge(nextPc);
			merge(cop.getTargetPc());
			return -1;
		}
		case JCND: {
			final JCND cop = (JCND) op;
			this.currentBb.setConds(getTargetBb(cop.getTargetPc()), getTargetBb(nextPc));
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
			this.currentBb.setSucc(subBb);
			// use common value (we take Sub) instead of jsr-follow-address because of merge
			final Frame subFrame = getFrame(subPc);
			if (subFrame == null) {
				// never been at this sub -> create new jsr-follow-address (Sub) and merge -> return
				final Sub sub = new Sub(subPc);
				if (!this.currentFrame.pushSub(sub)) {
					return -1;
				}
				this.currentFrame.push(R.createConstR(subPc, this.currentFrame.size(), T.RET, sub));
				merge(subPc);
				return -1;
			}
			// already visited this sub -> restore jsr-follow-address (Sub) and merge -> check RET
			final R subR = subFrame.peekSub(this.currentFrame.getTop(), subPc);
			if (subR == null) {
				assert false : getM() + ": already visited sub with pc '" + subPc
				+ "' but didn't find initial sub register";

			return -1;
			}
			final Sub sub = (Sub) subR.getValue();
			if (!this.currentFrame.pushSub(sub)) {
				return -1;
			}
			this.currentFrame.push(subR);
			merge(subPc);

			// RET already visited? -> link RET BB to JSR follower and merge
			final RET ret = sub.getRet();
			if (ret == null) {
				return -1;
			}
			this.currentFrame = new Frame(getFrame(ret.getPc()));
			if (load(ret.getReg(), T.RET).getValue() != sub) {
				log.warn(getM() + ": Incorrect sub!");
			}
			final BB retBb = getBb(ret.getPc());
			final int jsrFollowPc = jsr.getPc() + 1;
			retBb.setSucc(getTargetBb(jsrFollowPc));
			// modify RET frame for untouched registers in sub
			final Frame jsrFrame = getCfg().getInFrame(jsr);
			for (int i = this.currentFrame.size(); i-- > 0;) {
				if (!checkRegisterAccessInSub(i, ret)) {
					this.currentFrame.store(i, jsrFrame.load(i));
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
			switch (cop.getKind()) {
			case ENTER: {
				// always split, even for trivial / empty synchronize-blocks without
				// rethrow-handlers: else we would be forced to check & remember header nodes and
				// statement number for the later control flow analysis
				this.currentBb.setSucc(getTargetBb(nextPc));
				merge(nextPc);
				return -1; // switch current BB
			}
			case EXIT:
				break;
			default:
				log.warn(getM() + ": Unknown MONITOR type '" + cop.getKind() + "'!");
			}
			break;
		}
		case MUL: {
			evalBinaryMath((MUL) op);
			break;
		}
		case NEG: {
			final NEG cop = (NEG) op;
			popRead(cop.getT());
			// TODO calc value
			pushConst(cop.getT()); // not r.getT()
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
			evalBinaryMath(cop);
			break;
		}
		case POP: {
			final POP cop = (POP) op;
			// no new register or type reduction necessary, simply let it die off
			switch (cop.getKind()) {
			case POP2:
				final R s1 = pop();
				if (s1.isWide()) {
					break;
				}
				// fall through for second pop iff none-wide
			case POP: {
				popSingle();
				break;
			}
			default:
				log.warn(getM() + ": Unknown POP type '" + cop.getKind() + "'!");
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
			evalBinaryMath((REM) op);
			break;
		}
		case RET: {
			final RET ret = (RET) op;
			final R r = loadRead(ret.getReg(), T.RET);
			// bytecode restriction: only called via matching JSR, Sub known as register value
			final Sub sub = (Sub) r.getValue();
			if (!this.currentFrame.popSub(sub)) {
				return -1;
			}
			// remember RET for later JSRs to this Sub
			sub.setRet(ret);

			// link RET BB to all yet known JSR followers and merge, Sub BB incomings are JSRs
			final int subPc = sub.getPc();
			final BB subBb = getBb(subPc);
			for (final E in : subBb.getIns()) {
				// JSR is last operation in previous BB
				final Op jsr = in.getStart().getFinalOp();
				final int jsrFollowPc = jsr.getPc() + 1;
				this.currentBb.setSucc(getTargetBb(jsrFollowPc));
				// modify RET frame for untouched registers in sub
				final Frame jsrFrame = getCfg().getInFrame(jsr);
				for (int i = this.currentFrame.size(); i-- > 0;) {
					if (!checkRegisterAccessInSub(i, ret)) {
						this.currentFrame.store(i, jsrFrame.load(i));
					}
				}
				merge(jsrFollowPc);
			}
			return -1;
		}
		case RETURN: {
			final RETURN cop = (RETURN) op;
			final T returnT = getM().getReturnT();
			assert cop.getT().isAssignableFrom(returnT) : getM() + ": cannot assign '" + returnT
			+ "' to return type '" + cop.getT() + "'";

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

			// use potentially known variable types, e.g. "boolean b = true"
			final V debugV = getCfg().getDebugV(cop.getReg(), nextPc);
			final R r = popRead(debugV != null ? debugV.getT() : cop.getT());
			store(cop.getReg(), r);
			break;
		}
		case SUB: {
			evalBinaryMath((SUB) op);
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
				this.currentBb.addSwitchCase(getTargetBb(casePc2valuesEntry.getKey()),
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
			evalBinaryMath(cop);
			break;
		}
		default:
			throw new DecoJerException("Unknown intermediate vm operation '" + op + "'!");
		}
		if (getBb(nextPc) != null) {
			// already have been here, switch current BB
			this.currentBb.setSucc(getTargetBb(nextPc));
			merge(nextPc);
			return -1;
		}
		merge(nextPc);
		return nextPc;
	}

	private void executeExceptions() {
		if (isNoExceptions()) {
			return;
		}
		final int currentPc = getCurrentPc();
		if (this.currentBb.getOps() == 1) {
			// build sorted map: unique handler pc -> matching handler types
			final TreeMap<Integer, List<T>> handlerPc2type = Maps.newTreeMap();
			for (final Exc exc : getCfg().getExcs()) {
				if (!exc.validIn(currentPc)) {
					continue;
				}
				// it would be nice to prone unreachable outer exception handlers here, but this
				// is not possible because we very often havn't sufficient exception information
				// (super classes etc.)
				// extend sorted map for successors
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
				final T[] ts = types.toArray(new T[types.size()]);
				assert ts != null;
				this.currentBb.addCatchHandler(getTargetBb(handlerPc2typeEntry.getKey()), ts);
			}
		}
		for (final Exc exc : getCfg().getExcs()) {
			if (!exc.validIn(currentPc)) {
				continue;
			}
			final int handlerPc = exc.getHandlerPc();
			final Frame handlerFrame = getFrame(handlerPc);
			R excR;
			if (handlerFrame == null) {
				// null is <any> (means Java finally) -> Throwable
				excR = R.createConstR(handlerPc, getCfg().getRegs(), exc.getT() == null ? getDu()
						.getT(Throwable.class) : exc.getT(), null);
			} else {
				if (handlerFrame.getTop() != 1) {
					log.warn(getM() + ": Handler stack for exception merge not of size 1!");
				}
				excR = handlerFrame.peek(); // reuse exception register
			}
			// use current PC before operation for forwarding failed assignments -> null
			this.currentFrame = new Frame(getFrame(currentPc), excR);
			merge(handlerPc);
		}
	}

	private BB getBb(final int pc) {
		return this.pc2bbs[pc];
	}

	private int getCurrentPc() {
		return this.currentFrame.getPc();
	}

	@Nonnull
	private DU getDu() {
		return getCfg().getDu();
	}

	@Nullable
	private Frame getFrame(final int pc) {
		return getCfg().getFrame(pc);
	}

	@Nonnull
	private M getM() {
		return getCfg().getM();
	}

	private Op getOp(final int pc) {
		return getCfg().getOps()[pc];
	}

	/**
	 * Get target BB for PC. Split or create new if necessary.
	 *
	 * @param pc
	 *            target PC
	 * @return target BB
	 */
	@Nonnull
	private BB getTargetBb(final int pc) {
		final BB bb = getBb(pc); // get BB for target PC
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
			assert op != null;
			newInBb.addOp(op);
			setBb(op.getPc(), newInBb);
		}
		bb.setPc(pc); // necessary because we must preserve outgoing BB
		return bb;
	}

	private boolean isNoExceptions() {
		return this.isIgnoreExceptions || getCfg().getExcs() == null;
	}

	private R load(final int i, final T t) {
		final R r = this.currentFrame.load(i);
		if (!r.assignTo(t)) {
			throw new DecoJerException("Incompatible type for register '" + i
					+ "'! Cannot assign '" + r + "' to '" + t + "'.");
		}
		if (r.getT() == T.RET) {
			// bytecode restriction: internal return address type can only be read once
			this.currentFrame.store(i, null);
		}
		return r;
	}

	private R loadRead(final int i, final T t) {
		final R r = load(i, t);
		markAlive(this.currentBb, i);
		return r;
	}

	@SuppressWarnings("null")
	private void markAlive(final BB bb, final int i) {
		// mark this BB alive for register i;
		// we defer MOVE alive markings, to prevent DUP/POP stuff etc.
		int aliveI = i;
		for (int j = bb.getOps(); j-- > 1;) {
			final int pc = bb.getOp(j).getPc();
			final Frame frame = getFrame(pc);
			if (!frame.markAlive(aliveI)) {
				return; // was already alive, can happen via MOVE-out
			}
			final R r = frame.load(aliveI);
			assert r != null : getM() + ": register is null for pc '" + pc + "' and index '"
					+ aliveI + "' for operation: " + bb.getOp(j);

			if (r.getPc() == pc) {
				// register does change here...
				switch (r.getKind()) {
				case BOOLMATH:
				case CONST:
					// stop backpropagation here
					return;
				case MERGE:
					assert false : getM() + ": MERGE can only be first op in BB";

				// stop backpropagation here
				return;
				case MOVE:
					// register changes here, MOVE from different incoming register in same BB
					aliveI = r.getIn().getI();
					continue;
				}
				// stop backpropagation here
				return;
			}
		}
		final int pc = bb.getPc();
		final Frame frame = getFrame(pc);
		if (!frame.markAlive(aliveI)) {
			return; // was already alive, can happen via MOVE-out
		}

		// now backpropagate to other BBs;
		// MERGE could contain a wrapped CONST or MOVE with same register change PC...CONST
		// wouldn't be a problem, but MOVE could change the back propagation index!
		// so we could fan out into multiple register indices here!
		R[] mergeIns = null;
		final R r = frame.load(aliveI);
		if (r.getPc() == pc) {
			// different kinds possible, e.g. exceptions can split BBs
			switch (r.getKind()) {
			case BOOLMATH:
			case CONST:
				// stop backpropagation here
				return;
			case MERGE:
				// register does change at BB start, we are interested in a merge which could wrap
				// other freshly changed registers; but could also be a CONST from method start or
				// catched exception
				mergeIns = r.getIns();
				assert mergeIns.length > 1 : getM() + ": merge ins must exist";

				break;
			case MOVE:
				// register changes here, MOVE from different incoming register in previous BB
				aliveI = r.getIn().getI();
				break;
			}
		}
		// backpropagate alive to previous BBs
		previousLoop: for (final E in : bb.getIns()) {
			final BB inBb = in.getStart();
			final Op finalOp = inBb.getFinalOp();
			if (finalOp instanceof RET) {
				// jump over subs, where the register is unchanged
				if (!checkRegisterAccessInSub(aliveI, (RET) finalOp)) {
					markAlive(getBb(pc - 1), aliveI);
					continue previousLoop;
				}
			}
			if (mergeIns != null) {
				// if the MERGE wraps a register, then we must stop or fix the alive index
				final int finalOpOutPc = finalOp.getPc() + 1;
				for (final R inR : mergeIns) {
					if (finalOpOutPc == inR.getPc()) {
						switch (inR.getKind()) {
						case BOOLMATH:
						case CONST:
						case MERGE:
							continue previousLoop; // stop backpropagation here
						case MOVE:
							markAlive(inBb, inR.getIn().getI());
							continue previousLoop; // alive index changed and backpropagated
						}
						continue previousLoop; // stop backpropagation here
					}
				}
			}
			markAlive(inBb, aliveI);
		}
	}

	private void merge(final int targetPc) {
		final Frame targetFrame = getFrame(targetPc);
		if (targetFrame == null) {
			// first visit for this target frame -> no BB join -> no type merge
			getCfg().setFrame(targetPc, this.currentFrame);
			return;
		}
		assert targetFrame.getPc() != getCurrentPc() : getM() + ": is called twice";
		// target frame has already been visited before, hence this must be a BB start with multiple
		// predecessors => register merge necessary
		assert targetFrame.size() == this.currentFrame.size() : getM()
				+ ": incompatible frame sizes";

		final BB targetBb = getBb(targetPc);
		assert targetBb != null && targetBb.getPc() == targetPc : getM()
				+ ": target PC is not start of a target BB";

		for (int i = targetFrame.size(); i-- > 0;) {
			mergeReg(targetBb, i, this.currentFrame.load(i));
		}
	}

	private void mergeReg(final BB targetBb, final int i, @Nullable final R newR) {
		final Frame targetFrame = getFrame(targetBb.getPc());
		assert targetFrame != null;
		final R prevR = targetFrame.load(i);
		if (prevR == newR || prevR == null) {
			// previous register is null? => merge to null => nothing to do
			return;
		}
		if (newR == null) {
			// new register is null? => merge to null => replace previous register from here
			replaceRegBbDeep(targetBb, prevR, null);
			return;
		}

		final T intersectT = T.intersect(prevR.getT(), newR.getT());
		if (intersectT == null) {
			// merge type is null? => merge to null => replace previous register from here
			// TODO handle types with unknown super not as null-intersect
			replaceRegBbDeep(targetBb, prevR, null);
			return;
		}
		if (targetFrame.isAlive(i)) {
			// register i for current BB must be alive too for merging
			if (newR.getPc() != targetBb.getPc()) {
				markAlive(this.currentBb, i);
			} else if (newR.getKind() == Kind.MOVE) {
				markAlive(this.currentBb, newR.getIn().getI());
			}
			newR.assignTo(intersectT);
			prevR.assignTo(intersectT);
		}

		if (prevR.getKind() == Kind.MERGE && prevR.getPc() == targetBb.getPc()) {
			// this is a new merge register, add us in
			prevR.addInMerge(intersectT, newR);
			return;
		}
		// else start new merge register
		final R mergeR = R.createMergeR(targetBb.getPc(), i, intersectT, null, prevR, newR);
		replaceRegBbDeep(targetBb, prevR, mergeR);
	}

	@Nonnull
	private BB newBb(final int pc) {
		final BB bb = getCfg().newBb(pc);
		setBb(pc, bb);
		return bb;
	}

	private R peek() {
		return this.currentFrame.peek();
	}

	private R peek(final T t) {
		final R s = this.currentFrame.peek();
		if (!s.assignTo(t)) {
			throw new DecoJerException("Incompatible type for stack register! Cannot assign '" + s
					+ "' to '" + t + "'.");
		}
		return s;
	}

	private R peekSingle() {
		return peekSingle(0);
	}

	private R peekSingle(final int i) {
		final R s = this.currentFrame.peek(i);
		if (s.isWide()) {
			log.warn(getM() + ": Peek '" + i + "' attempts to split long or double on the stack!");
		}
		return s;
	}

	private R pop() {
		return this.currentFrame.pop();
	}

	private R pop(final T t) {
		final R s = this.currentFrame.pop();
		if (!s.assignTo(t)) {
			throw new DecoJerException("Incompatible type for stack register! Cannot assign '" + s
					+ "' to '" + t + "'.");
		}
		return s;
	}

	private R popRead(final T t) {
		markAlive(this.currentBb, this.currentFrame.size() - 1);
		return pop(t);
	}

	private R popSingle() {
		final R s = this.currentFrame.pop();
		if (s.isWide()) {
			log.warn(getM() + ": Pop attempts to split long or double on the stack!");
		}
		return s;
	}

	private R push(final R r) {
		return this.currentFrame
				.push(R.createMoveR(getCurrentPc() + 1, this.currentFrame.size(), r));
	}

	private R pushBoolmath(final T t, final R r1, final R r2) {
		return this.currentFrame.push(R.createBoolmathR(getCurrentPc() + 1,
				this.currentFrame.size(), t, null /* TODO do something? */, r1, r2));
	}

	private R pushConst(final T t) {
		return pushConst(t, null);
	}

	private R pushConst(final T t, final Object value) {
		return this.currentFrame.push(R.createConstR(getCurrentPc() + 1, this.currentFrame.size(),
				t, value));
	}

	private boolean replaceRegBb(final BB bb, final R prevR, @Nullable final R newR) {
		// BB possibly not visited yet => BB input frame known, but no operations exist,
		// but BB input frame cannot be null here
		if (!replaceRegFrame(bb.getPc(), prevR, newR)) {
			return false;
		}
		// replacement propagation to already known BB operations
		for (int j = 1; j < bb.getOps(); ++j) {
			if (!replaceRegFrame(bb.getOp(j).getPc(), prevR, newR)) {
				return false;
			}
		}
		return true;
	}

	private void replaceRegBbDeep(@Nonnull final BB bb, @Nonnull final R prevR,
			@Nullable final R newR) {
		if (!replaceRegBb(bb, prevR, newR)) {
			return;
		}
		// final operation is RET & register untouched in sub => modify to state before sub
		// TODO check JSR and RET, register same before and after??? replace RET
		// TODO not same? overwrite RET
		final Op finalOp = bb.getFinalOp();
		final boolean jumpOverSub = finalOp instanceof RET ? !checkRegisterAccessInSub(
				prevR.getI(), (RET) finalOp) : false;

		// replacement propagation to next BB necessary
		for (final E out : bb.getOuts()) {
			final BB outBb = out.getEnd();
			if (getFrame(outBb.getPc()) == null) {
				assert out.isCatch() : getM()
						+ ": out frames can just be null for splitted catch-handlers that havn't been visited yet: "
						+ out;

				continue;
			}
			// final operation is RET & register untouched in sub => modify to state before sub
			R newOutR;
			if (jumpOverSub) {
				final Frame frame = getFrame(outBb.getPc() - 1);
				assert frame != null;
				newOutR = frame.load(prevR.getI());
			} else {
				newOutR = newR;
			}
			replaceRegBbDeep(outBb, prevR, newOutR);
		}
	}

	private boolean replaceRegFrame(final int pc, final R prevR, @Nullable final R newR) {
		if (prevR == newR) {
			return false;
		}
		// replace potential out registers before the current register, this function goes always
		// one step further
		final R[] outs = prevR.getOuts();
		if (outs != null) {
			for (final R out : outs) {
				if (out.getPc() == pc) {
					// no complete merge handling here...what if we replace only one occurence in
					// multi-merge of same register...handle in BB navigation: replaceBbRegDeep
					out.replaceIn(prevR, newR);
				}
			}
		}
		final int i = prevR.getI();
		final Frame frame = getFrame(pc);
		assert frame != null;
		if (i >= frame.size() || prevR != frame.load(i)) {
			return false;
		}
		frame.store(i, newR);
		return true;
	}

	private BB setBb(final int pc, final BB bb) {
		return this.pc2bbs[pc] = bb;
	}

	/**
	 * Exception block changes in current BB? -> split necessary!
	 *
	 * @param currentPc
	 *            current pc
	 * @return original BB or new BB for beginning exception block
	 */
	private BB splitExceptions(final int currentPc) {
		if (isNoExceptions()) {
			return this.currentBb;
		}
		assert this.currentBb.getPc() == currentPc || this.currentBb.getOps() > 0 : getM()
				+ ": could happen with GOTO-mode: create no entry in BB, currently unused";

		for (final Exc exc : getCfg().getExcs()) {
			if (exc.validIn(currentPc)) {
				if (exc.validIn(this.currentBb.getPc())) {
					// exception is valid - has been valid at BB entry -> OK
					continue;
				}
			} else {
				// exception endPc is eclusive, but often points to final GOTO or RETURN in
				// try-block, this is especially not usefull for returns with values!
				final Op currentOp = getOp(currentPc);
				if (!exc.validIn(this.currentBb.getPc()) || currentPc == exc.getEndPc()
						&& (currentOp instanceof GOTO || currentOp instanceof RETURN)) {
					// exception isn't valid - hasn't bean valid at BB entry -> OK
					continue;
				}
			}
			// at least one exception has changed, newBb() links exceptions
			final BB succBb = newBb(currentPc);
			this.currentBb.setSucc(succBb);
			return succBb;
		}
		return this.currentBb;
	}

	private R store(final int i, final R r) {
		return this.currentFrame.store(i, R.createMoveR(getCurrentPc() + 1, i, r));
	}

	private void transform() {
		final CFG cfg = getCfg();
		this.pc2bbs = new BB[cfg.getOps().length];
		this.openPcs = Lists.newLinkedList();

		// start with PC 0 and new BB
		int currentPc = 0; // better not as global attribute, current context changes sometimes
		this.currentBb = newBb(0); // need pc2bb and openPcs

		cfg.initFrames();
		cfg.setStartBb(this.currentBb);

		while (true) {
			if (currentPc < 0) {
				// next open pc?
				if (this.openPcs.isEmpty()) {
					break;
				}
				currentPc = this.openPcs.removeFirst();
				this.currentBb = getBb(currentPc);
			} else {
				this.currentBb = splitExceptions(currentPc); // exception boundary? split...
				setBb(currentPc, this.currentBb);
			}
			this.currentFrame = new Frame(getFrame(currentPc)); // copy, merge to next PCs
			final int nextPc = execute();
			executeExceptions();
			currentPc = nextPc;
		}
	}

}