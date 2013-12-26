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

import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;

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

	@Getter(value = AccessLevel.PRIVATE)
	private final CFG cfg;

	/**
	 * Current BB.
	 */
	private BB currentBb;

	/**
	 * Current frame.
	 */
	private Frame currentFrame;

	/**
	 * Current PC.
	 */
	private int currentPc;

	/**
	 * Current operation.
	 */
	private Op currentOp;

	private final boolean isIgnoreExceptions;

	/**
	 * Remember open PCs.
	 */
	private LinkedList<Integer> openPcs;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2bbs;

	private TrDataFlowAnalysis(final CFG cfg) {
		this.cfg = cfg;
		this.isIgnoreExceptions = getCfg().getCu().check(DFlag.IGNORE_EXCEPTIONS);
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
		final T joinT = T.join(s1.getT(), s2.getT());
		assert joinT != null; // TODO ref1 == ref2 is allowed with result void (bool math)

		s2.assignTo(joinT);
		s1.assignTo(joinT);

		if (resultT != null) {
			if (resultT != T.VOID) {
				pushConst(resultT);
			}
			return;
		}
		// TODO byte/short/char -> int
		if (joinT.is(T.BOOLEAN, T.INT)) {
			pushBoolmath(joinT, s1, s2);
		} else {
			pushConst(joinT);
		}
	}

	private int execute() {
		final Op op = this.currentOp;
		int nextPc = this.currentPc + 1;
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
				this.currentFrame.push(new R(subPc, this.currentFrame.size(), T.RET, sub,
						Kind.CONST));
				merge(subPc);
				return -1;
			}
			// already visited this sub -> restore jsr-follow-address (Sub) and merge -> check RET
			final R subR = subFrame.peekSub(this.currentFrame.getTop(), subPc);
			if (subR == null) {
				return -1;
			}
			final Sub sub = (Sub) subR.getValue();
			if (!this.currentFrame.pushSub(sub)) {
				return -1;
			}
			this.currentFrame.push(subR);
			merge(subPc);
			// RET already visited -> link RET BB to JSR follower and merge
			final RET ret = sub.getRet();
			if (ret == null) {
				return -1;
			}
			this.currentFrame = new Frame(getFrame(ret.getPc()));
			if (loadRead(ret.getReg(), T.RET).getValue() != sub) {
				log("Incorrect sub!");
			}
			final BB retBb = getBb(ret.getPc());
			final int jsrFollowPc = jsr.getPc() + 1;
			retBb.setSucc(getTargetBb(jsrFollowPc));
			// modify RET frame for untouched registers in sub
			final Frame jsrFrame = getCfg().getInFrame(jsr);
			for (int i = this.currentFrame.size(); i-- > 0;) {
				if (jumpOverSub(ret, i)) {
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
			merge(nextPc);
			if (cop.getKind() == MONITOR.Kind.ENTER) {
				// always split, even for trivial / empty synchronize-blocks without
				// rethrow-handlers: else we would be forced to check & remember header nodes and
				// statement number for the later control flow analysis
				this.currentBb.setSucc(getTargetBb(nextPc));
				return -1;
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
			final NEWARRAY cop = (NEWARRAY) this.currentOp;
			for (int i = cop.getDimensions(); i-- > 0;) {
				popRead(T.INT);
			}
			pushConst(cop.getT());
			break;
		}
		case OR: {
			final OR cop = (OR) this.currentOp;
			evalBinaryMath(cop);
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
					if (jumpOverSub(ret, i)) {
						this.currentFrame.store(i, jsrFrame.load(i));
					}
				}
				merge(jsrFollowPc);
			}
			return -1;
		}
		case RETURN: {
			final RETURN cop = (RETURN) op;
			final T returnT = getCfg().getMd().getReturnT();
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
			log("Operation '" + op + "' not handled!");
		}
		if (getBb(nextPc) != null) {
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
		if (this.currentBb.getOps() == 1) {
			// build sorted map: unique handler pc -> matching handler types
			final TreeMap<Integer, List<T>> handlerPc2type = Maps.newTreeMap();
			for (final Exc exc : getCfg().getExcs()) {
				if (!exc.validIn(this.currentPc)) {
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
				this.currentBb.addCatchHandler(getTargetBb(handlerPc2typeEntry.getKey()),
						handlerPc2typeEntry.getValue().toArray(new T[types.size()]));
			}
		}
		for (final Exc exc : getCfg().getExcs()) {
			if (!exc.validIn(this.currentPc)) {
				continue;
			}
			final int handlerPc = exc.getHandlerPc();
			final Frame handlerFrame = getFrame(handlerPc);
			R excR;
			if (handlerFrame == null) {
				// null is <any> (means Java finally) -> Throwable
				excR = new R(handlerPc, getCfg().getRegs(), exc.getT() == null ? getDu().getT(
						Throwable.class) : exc.getT(), Kind.CONST);
			} else {
				if (handlerFrame.getTop() != 1) {
					log("Handler stack for exception merge not of size 1!");
				}
				excR = handlerFrame.peek(); // reuse exception register
			}
			// use current PC before operation for forwarding failed assignments -> null
			this.currentFrame = new Frame(getFrame(this.currentPc), excR);
			merge(handlerPc);
		}
	}

	private BB getBb(final int pc) {
		return this.pc2bbs[pc];
	}

	private DU getDu() {
		return getCfg().getDu();
	}

	private Frame getFrame(final int pc) {
		return getCfg().getFrame(pc);
	}

	/**
	 * Get target BB for PC. Split or create new if necessary.
	 * 
	 * @param pc
	 *            target PC
	 * @return target BB
	 */
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
			newInBb.addOp(op);
			setBb(op.getPc(), newInBb);
		}
		bb.setPc(pc); // necessary because we must preserve outgoing BB
		return bb;
	}

	private boolean isNoExceptions() {
		return this.isIgnoreExceptions || getCfg().getExcs() == null;
	}

	private boolean jumpOverSub(final RET ret, final int i) {
		final Frame retFrame = getFrame(ret.getPc());
		final R retR = retFrame.load(ret.getReg());
		final Sub sub = (Sub) retR.getValue();
		final Frame subFrame = getFrame(sub.getPc());

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
		final R r = this.currentFrame.load(i);
		if (!r.assignTo(t)) {
			throw new RuntimeException("Incompatible type for register '" + i
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

	private void log(final String message) {
		LOGGER.warning(getCfg().getMd() + ": " + message);
	}

	private void markAlive(final BB bb, final int i) {
		// mark this BB alive for register i;
		// we defer MOVE alive markings, to prevent DUP/POP stuff etc.
		int aliveI = i;
		for (int j = bb.getOps(); j-- > 1;) {
			final Op op = bb.getOp(j);
			final int pc = op.getPc();
			final Frame frame = getCfg().getFrame(pc);
			if (!frame.markAlive(aliveI)) {
				return;
			}
			final R r = frame.load(aliveI);
			if (r.getPc() == pc) {
				// register does change here...
				if (r.getKind() == Kind.MOVE) {
					// register changes here, MOVE from different incoming register in same BB
					aliveI = r.getIn().getI();
					continue;
				}
				assert r.getKind() != Kind.MERGE; // can only be first op in BB
				// stop backpropagation here
				return;
			}
		}
		final int pc = bb.getPc();
		final Frame frame = getCfg().getFrame(pc);
		if (!frame.markAlive(aliveI)) {
			return;
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
			case MERGE:
				// register does change at BB start, we are interested in a merge which could wrap
				// other freshly changed registers; but could also be a CONST from method start or
				// catched exception
				mergeIns = r.getIns();
				assert mergeIns.length > 1;

				break;
			case MOVE:
				// register changes here, MOVE from different incoming register in different BB
				aliveI = r.getIn().getI();
				break;
			default:
				// stop backpropagation here
				return;
			}
		}
		// backpropagate alive to previous BBs
		marked: for (final E in : bb.getIns()) {
			final BB predBb = in.getStart();
			// TODO conditionally jump over JSR-RET!
			if (mergeIns != null) {
				final int opPc = predBb.getFinalOp().getPc() + 1;
				for (final R inR : mergeIns) {
					if (inR.getPc() == opPc) {
						if (inR.getKind() == Kind.MOVE) {
							markAlive(predBb, inR.getIn().getI());
						}
						continue marked;
					}
				}
			}
			markAlive(predBb, aliveI);
		}
	}

	private void merge(final int targetPc) {
		final Frame targetFrame = getFrame(targetPc);
		if (targetFrame == null) {
			// first visit for this target frame -> no BB join -> no type merge
			getCfg().setFrame(targetPc, this.currentFrame);
			return;
		}
		// target frame has already been visited before, hence this must be a BB start with multiple
		// predecessors => register merge necessary
		assert targetFrame.size() == this.currentFrame.size();

		final BB targetBb = getBb(targetPc);
		assert targetBb != null;

		for (int i = targetFrame.size(); i-- > 0;) {
			mergeReg(targetBb, i, this.currentFrame.load(i));
		}
	}

	private void mergeReg(final BB targetBb, final int i, @Nullable final R newR) {
		final Frame targetFrame = getFrame(targetBb.getPc());
		final R prevR = targetFrame.load(i);
		if (prevR == newR || prevR == null) {
			// previous register is null? merge to null => nothing to do
			return;
		}
		if (newR == null) {
			// new register is null? merge to null => replace previous register from here
			assert !targetFrame.isAlive(i);

			replaceBbRegDeep(targetBb, prevR, null);
			return;
		}
		if (prevR.getKind() == Kind.MERGE && prevR.getPc() == targetBb.getPc()) {
			final T t = T.join(prevR.getT(), newR.getT());
			if (t == null) {
				// merge type is null? merge to null => replace previous register from here
				assert !targetFrame.isAlive(i);

				replaceBbRegDeep(targetBb, prevR, null);
				return;
			}
			if (targetFrame.isAlive(i)) {
				// register i for current BB must be alive too for merging
				if (newR.getPc() != targetBb.getPc()) {
					markAlive(targetBb, i);
				} else if (newR.getKind() == Kind.MOVE) {
					markAlive(targetBb, newR.getIn().getI());
				}
				newR.assignTo(t);
				prevR.assignTo(t);
			}
			prevR.addInMerge(t, newR);
			return;
		}
		final T t = T.join(prevR.getT(), newR.getT());
		if (t == null) {
			// merge type is null? merge to null => replace previous register from here
			assert !targetFrame.isAlive(i);

			// FIXME dangerous if unknown super types...defer this op, remember merge
			// register with 2 inputs and try join only on read/re-store
			replaceBbRegDeep(targetBb, prevR, null);
			return;
		}
		if (targetFrame.isAlive(i)) {
			// register i for current BB must be alive too for merging
			if (newR.getPc() != targetBb.getPc()) {
				markAlive(targetBb, i);
			} else if (newR.getKind() == Kind.MOVE) {
				markAlive(targetBb, newR.getIn().getI());
			}
			newR.assignTo(t);
			prevR.assignTo(t);
		}
		// start new merge register
		final R mergeR = new R(targetBb.getPc(), i, t, Kind.MERGE, prevR, newR);
		final List<BB> endBbs = replaceBbRegDeep(targetBb, prevR, mergeR);
		if (endBbs != null) {
			for (final BB endBb : endBbs) {
				mergeReg(endBb, prevR.getI(), mergeR);
			}
		}
	}

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
			throw new RuntimeException("Incompatible type for stack register! Cannot assign '" + s
					+ "' to '" + t + "'.");
		}
		return s;
	}

	private R peekSingle() {
		return peekSingle(0);
	}

	private R peekSingle(final int i) {
		final R s = this.currentFrame.peek(i);
		if (s.getT().isWide()) {
			log("Peek '" + i + "' attempts to split long or double on the stack!");
		}
		return s;
	}

	private R pop() {
		return this.currentFrame.pop();
	}

	private R pop(final T t) {
		final R s = this.currentFrame.pop();
		if (!s.assignTo(t)) {
			throw new RuntimeException("Incompatible type for stack register! Cannot assign '" + s
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
		if (s.getT().isWide()) {
			log("Pop attempts to split long or double on the stack!");
		}
		return s;
	}

	private R push(final R r) {
		return this.currentFrame.push(new R(this.currentPc + 1, this.currentFrame.size(), r.getT(),
				r.getValue(), Kind.MOVE, r));
	}

	private R pushBoolmath(final T t, final R r1, final R r2) {
		return this.currentFrame.push(new R(this.currentPc + 1, this.currentFrame.size(), t,
				null /* TODO do something? */, Kind.BOOLMATH, r1, r2));
	}

	private R pushConst(final T t) {
		return pushConst(t, null);
	}

	private R pushConst(final T t, final Object value) {
		return this.currentFrame.push(new R(this.currentPc + 1, this.currentFrame.size(), t, value,
				Kind.CONST));
	}

	private List<BB> replaceBbRegDeep(final BB bb, final R prevR, @Nullable final R newR) {
		assert newR == null || prevR.getI() == newR.getI() && newR.getKind() == Kind.MERGE : newR;

		if (!replaceBbRegSingle(bb, prevR, newR)) {
			return null;
		}
		// navigate to next BBs...replace branch as far as possible before creating/extending merges
		final BB currentBb = bb;
		final List<BB> endBbs = Lists.newArrayList();
		for (int i = endBbs.size(); i-- > 0;) {

			// final operation is RET & register untouched in sub => modify to state before sub
			// TODO check JSR and RET, register same before and after??? replace RET
			// TODO not same? overwrite RET
			final boolean jumpOverSub;
			final Op finalOp = currentBb.getFinalOp();
			jumpOverSub = finalOp instanceof RET ? jumpOverSub((RET) finalOp, prevR.getI()) : false;

			// replacement propagation to next BB necessary
			for (final E out : currentBb.getOuts()) {
				final BB outBb = out.getEnd();
				if (getFrame(outBb.getPc()) == null) {
					// possible for freshly splitted catch-handlers that havn't been visited yet
					assert out.isCatch() : out;

					continue;
				}
				// final operation is RET & register untouched in sub => modify to state before sub
				replaceBbRegDeep(outBb, prevR,
						jumpOverSub ? getFrame(outBb.getPc() - 1).load(prevR.getI()) : newR);
			}
		}
		return endBbs;
	}

	private boolean replaceBbRegSingle(final BB bb, final R prevR, @Nullable final R newR) {
		// BB possibly not visited yet => BB input frame known, but no operations exist,
		// but BB input frame cannot be null here
		if (!replaceFrameReg(bb.getPc(), prevR, newR)) {
			return false;
		}
		// replacement propagation to already known BB operations
		for (int j = 1; j < bb.getOps(); ++j) {
			if (!replaceFrameReg(bb.getOp(j).getPc(), prevR, newR)) {
				return false;
			}
		}
		return true;
	}

	private boolean replaceFrameReg(final int pc, final R prevR, @Nullable final R newR) {
		// replace potential out registers before the current register, this function goes always
		// one step further
		for (final R out : prevR.getOuts()) {
			if (out.getPc() == pc) {
				// no complete merge handling here...what if we replace only one occurence in
				// multi-merge of same register...handle in BB navigation: replaceBbRegDeep
				out.replaceIn(prevR, newR);
			}
		}
		final int i = prevR.getI();
		final Frame frame = getFrame(pc);
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
	 * @return original BB or new BB for beginning exception block
	 */
	private BB splitExceptions() {
		if (isNoExceptions()) {
			return this.currentBb;
		}
		// could happen with GOTO-mode: create no entry in BB, currently unused
		assert this.currentBb.getPc() == this.currentPc || this.currentBb.getOps() > 0;

		for (final Exc exc : getCfg().getExcs()) {
			if (exc.validIn(this.currentPc)) {
				if (exc.validIn(this.currentBb.getPc())) {
					// exception is valid - has been valid at BB entry -> OK
					continue;
				}
			} else {
				// exception endPc is eclusive, but often points to final GOTO or RETURN in
				// try-block, this is especially not usefull for returns with values!
				if (!exc.validIn(this.currentBb.getPc()) || this.currentPc == exc.getEndPc()
						&& (this.currentOp instanceof GOTO || this.currentOp instanceof RETURN)) {
					// exception isn't valid - hasn't bean valid at BB entry -> OK
					continue;
				}
			}
			// at least one exception has changed, newBb() links exceptions
			final BB succBb = newBb(this.currentPc);
			this.currentBb.setSucc(succBb);
			return succBb;
		}
		return this.currentBb;
	}

	private R store(final int i, final R r) {
		return this.currentFrame.store(i, new R(this.currentPc + 1, i, r.getT(), r.getValue(),
				Kind.MOVE, r));
	}

	private void transform() {
		final Op[] ops = getCfg().getOps();
		this.pc2bbs = new BB[ops.length];
		this.openPcs = Lists.newLinkedList();

		// start with PC 0 and new BB
		this.currentPc = 0;
		this.currentBb = newBb(0); // need pc2bb and openPcs

		getCfg().initFrames();
		getCfg().setStartBb(this.currentBb);

		while (true) {
			if (this.currentPc < 0) {
				// next open pc?
				if (this.openPcs.isEmpty()) {
					break;
				}
				this.currentPc = this.openPcs.removeFirst();
				this.currentOp = ops[this.currentPc];
				this.currentBb = getBb(this.currentPc);
			} else {
				this.currentOp = ops[this.currentPc];
				this.currentBb = splitExceptions(); // exception boundary? split...
				setBb(this.currentPc, this.currentBb);
			}
			this.currentBb.addOp(this.currentOp);
			this.currentFrame = new Frame(getFrame(this.currentPc)); // copy, merge to next PCs
			final int nextPc = execute();
			executeExceptions();
			this.currentPc = nextPc;
		}
	}

}