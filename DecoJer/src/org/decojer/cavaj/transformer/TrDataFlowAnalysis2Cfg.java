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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
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
import org.decojer.cavaj.model.code.op.ADD;
import org.decojer.cavaj.model.code.op.ALOAD;
import org.decojer.cavaj.model.code.op.AND;
import org.decojer.cavaj.model.code.op.ARRAYLENGTH;
import org.decojer.cavaj.model.code.op.ASTORE;
import org.decojer.cavaj.model.code.op.CAST;
import org.decojer.cavaj.model.code.op.CMP;
import org.decojer.cavaj.model.code.op.DIV;
import org.decojer.cavaj.model.code.op.DUP;
import org.decojer.cavaj.model.code.op.FILLARRAY;
import org.decojer.cavaj.model.code.op.GET;
import org.decojer.cavaj.model.code.op.GOTO;
import org.decojer.cavaj.model.code.op.INC;
import org.decojer.cavaj.model.code.op.INSTANCEOF;
import org.decojer.cavaj.model.code.op.INVOKE;
import org.decojer.cavaj.model.code.op.JCMP;
import org.decojer.cavaj.model.code.op.JCND;
import org.decojer.cavaj.model.code.op.JSR;
import org.decojer.cavaj.model.code.op.LOAD;
import org.decojer.cavaj.model.code.op.MONITOR;
import org.decojer.cavaj.model.code.op.MUL;
import org.decojer.cavaj.model.code.op.NEG;
import org.decojer.cavaj.model.code.op.NEW;
import org.decojer.cavaj.model.code.op.NEWARRAY;
import org.decojer.cavaj.model.code.op.OR;
import org.decojer.cavaj.model.code.op.Op;
import org.decojer.cavaj.model.code.op.POP;
import org.decojer.cavaj.model.code.op.PUSH;
import org.decojer.cavaj.model.code.op.PUT;
import org.decojer.cavaj.model.code.op.REM;
import org.decojer.cavaj.model.code.op.RET;
import org.decojer.cavaj.model.code.op.RETURN;
import org.decojer.cavaj.model.code.op.SHL;
import org.decojer.cavaj.model.code.op.SHR;
import org.decojer.cavaj.model.code.op.STORE;
import org.decojer.cavaj.model.code.op.SUB;
import org.decojer.cavaj.model.code.op.SWAP;
import org.decojer.cavaj.model.code.op.SWITCH;
import org.decojer.cavaj.model.code.op.THROW;
import org.decojer.cavaj.model.code.op.XOR;

/**
 * Transform Data Flow Analysis and building Control Flow Graph.
 * 
 * @author André Pankraz
 */
public final class TrDataFlowAnalysis2Cfg {

	private final static Logger LOGGER = Logger.getLogger(TrDataFlowAnalysis2Cfg.class.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrDataFlowAnalysis2Cfg(cfg).transform();
	}

	private final CFG cfg;

	/**
	 * Current frame.
	 */
	private Frame frame;

	private boolean isIgnoreExceptions;

	/**
	 * Remember open PCs.
	 */
	private final LinkedList<Integer> openPcs = new LinkedList<Integer>();

	/**
	 * Current PC.
	 */
	private int pc;

	/**
	 * Remember BBs for PCs.
	 */
	private BB[] pc2bbs;

	private TrDataFlowAnalysis2Cfg(final CFG cfg) {
		this.cfg = cfg;
	}

	private void evalBinaryMath(final T t) {
		evalBinaryMath(t, null);
	}

	private void evalBinaryMath(final T t, final T resultT) {
		final R s2 = pop(t);
		final R s1 = pop(t);

		assert R.merge(s1, s2) != null;

		if (s1.getT().isReference()) {
			// (J)CMP EQ / NE
			assert T.VOID == resultT : resultT;

			return;
		}
		if (resultT != T.VOID) {
			// TODO inputs really uninteresting
			pushConst(resultT != null ? resultT : t);
		}
	}

	private int execute(final Op op, final BB bb) {
		this.frame = new Frame(getFrame(op.getPc()));
		int nextPc = op.getPc() + 1;
		switch (op.getOptype()) {
		case ADD: {
			final ADD cop = (ADD) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case ALOAD: {
			final ALOAD cop = (ALOAD) op;
			pop(T.INT); // index
			pop(T.AREF); // array
			pushConst(cop.getT()); // value
			break;
		}
		case AND: {
			final AND cop = (AND) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case ARRAYLENGTH: {
			assert op instanceof ARRAYLENGTH;

			pop(T.AREF); // array
			pushConst(T.INT); // length
			break;
		}
		case ASTORE: {
			final ASTORE cop = (ASTORE) op;
			pop(cop.getT()); // value
			pop(T.INT); // index
			pop(T.AREF); // array
			break;
		}
		case CAST: {
			final CAST cop = (CAST) op;
			pop(cop.getT());
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
			evalBinaryMath(cop.getT());
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
				if (!s2.isWide()) {
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
				if (!s1.isWide()) {
					final R s2 = this.frame.peekSingle(2);
					pushMove(s2);
					pushMove(s1);
					break;
				}
				pushMove(s1);
				break;
			}
			case DUP2_X1: {
				final R s1 = this.frame.pop();
				if (!s1.isWide()) {
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
				if (!s1.isWide()) {
					final R s2 = this.frame.popSingle();
					final R s3 = this.frame.pop();
					if (!s3.isWide()) {
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
				if (!s3.isWide()) {
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
			assert op instanceof FILLARRAY;

			pop(T.AREF);
			break;
		}
		case GET: {
			final GET cop = (GET) op;
			final F f = cop.getF();
			if (!f.check(AF.STATIC)) {
				pop(f.getT());
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
			final R r = get(cop.getReg(), cop.getT());
			setConst(
					cop.getReg(),
					r.getT(),
					r.getValue() instanceof Number ? ((Number) r.getValue()).intValue()
							+ cop.getValue() : null);
			break;
		}
		case INSTANCEOF: {
			assert op instanceof INSTANCEOF;

			pop(T.AREF);
			// operation contains check-type as argument, not important here
			pushConst(T.BOOLEAN);
			break;
		}
		case INVOKE: {
			final INVOKE cop = (INVOKE) op;
			final M m = cop.getM();
			for (int i = m.getParams(); i-- > 0;) {
				pop(m.getParamT(i));
			}
			if (!m.check(AF.STATIC)) {
				pop(m.getT());
			}
			if (m.getReturnT() != T.VOID) {
				pushConst(m.getReturnT());
			}
			break;
		}
		case JCMP: {
			final JCMP cop = (JCMP) op;
			bb.setCondSuccs(getTargetBb(nextPc), getTargetBb(cop.getTargetPc()));
			evalBinaryMath(cop.getT(), T.VOID);
			merge(nextPc);
			merge(cop.getTargetPc());
			return -1;
		}
		case JCND: {
			final JCND cop = (JCND) op;
			bb.setCondSuccs(getTargetBb(nextPc), getTargetBb(cop.getTargetPc()));
			pop(cop.getT());
			merge(nextPc);
			merge(cop.getTargetPc());
			return -1;
		}
		case JSR: {
			final JSR cop = (JSR) op;
			// Spec, JSR/RET is stack-like:
			// http://docs.oracle.com/javase/7/specs/jvms/JVMS-JavaSE7.pdf
			bb.setSucc(getTargetBb(cop.getTargetPc()));
			// use common value (like Sub) instead of jsr-follow-address because of merge
			final Frame targetFrame = getFrame(cop.getTargetPc());
			jsr: if (targetFrame != null) {
				// JSR already visited, reuse Sub
				if (this.frame.getTop() + 1 != targetFrame.getTop()) {
					LOGGER.warning("Wrong JSR Sub merge! Subroutine stack size different.");
					break jsr;
				}
				final R subR = targetFrame.peek();
				// now check if RET in Sub already visited
				if (!(subR.getValue() instanceof Sub)) {
					LOGGER.warning("Wrong JSR Sub merge! Subroutine stack has wrong peek.");
					break jsr;
				}
				final Sub sub = (Sub) subR.getValue();
				if (sub.getPc() != cop.getTargetPc()) {
					LOGGER.warning("Wrong JSR Sub merge! Subroutine stack has wrong peek.");
					break jsr;
				}
				if (!this.frame.pushSub(sub)) {
					LOGGER.warning("Recursive call to jsr entry!");
					break jsr;
				}
				this.frame.push(subR);
				merge(cop.getTargetPc());

				final RET ret = sub.getRet();
				if (ret != null) {
					// RET already visited, link RET BB to JSR follower and merge
					this.frame = new Frame(getFrame(ret.getPc()));
					// bytecode restriction: register can only be consumed once
					setNull(ret.getReg());
					final BB retBb = this.pc2bbs[ret.getPc()];
					final int returnPc = cop.getPc() + 1;
					retBb.setSucc(getTargetBb(returnPc));
					merge(returnPc);
				}
			} else {
				final Sub sub = new Sub(cop.getTargetPc());
				if (!this.frame.pushSub(sub)) {
					LOGGER.warning("Recursive call to jsr entry!");
					break jsr;
				}
				pushConst(T.RETURN_ADDRESS, sub);
				merge(cop.getTargetPc());
			}
			return -1;
		}
		case LOAD: {
			final LOAD cop = (LOAD) op;
			final R r = get(cop.getReg(), cop.getT());
			// no previous for stack
			pushMove(r);
			break;
		}
		case MONITOR: {
			assert op instanceof MONITOR;

			pop(T.AREF);
			break;
		}
		case MUL: {
			final MUL cop = (MUL) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case NEG: {
			final NEG cop = (NEG) op;
			final R r = pop(cop.getT());
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
				pop(T.INT);
			}
			pushConst(this.cfg.getMd().getM().getT().getDu()
					.getArrayT(cop.getT(), cop.getDimensions()));
			break;
		}
		case OR: {
			final OR cop = (OR) op;
			evalBinaryMath(cop.getT());
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
				if (!s1.isWide()) {
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
			T t = cop.getT();
			// hack for now...doesn't win coolness price...
			if (t == T.AINT || t == T.DINT) {
				final int value = (Integer) cop.getValue();
				if (value < Short.MIN_VALUE || Short.MAX_VALUE < value) {
					// no short
					t = T.merge(t, T.multi(T.INT, T.BYTE, T.CHAR, T.BOOLEAN, T.FLOAT));
				}
				if (value < Character.MIN_VALUE || Character.MAX_VALUE < value) {
					// no char
					t = T.merge(t, T.multi(T.INT, T.SHORT, T.BYTE, T.BOOLEAN, T.FLOAT));
				}
				if (value < Byte.MIN_VALUE || Byte.MAX_VALUE < value) {
					// no byte
					t = T.merge(t, T.multi(T.INT, T.SHORT, T.CHAR, T.BOOLEAN, T.FLOAT));
				}
				if (value < 0 || 1 < value) {
					// no bool
					t = T.merge(t, T.multi(T.INT, T.SHORT, T.BYTE, T.CHAR, T.FLOAT));
				}
			}
			// no previous for stack
			pushConst(t, cop.getValue());
			break;
		}
		case PUT: {
			final PUT cop = (PUT) op;
			final F f = cop.getF();
			pop(f.getValueT());
			if (!f.check(AF.STATIC)) {
				pop(f.getT());
			}
			break;
		}
		case REM: {
			final REM cop = (REM) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case RET: {
			final RET cop = (RET) op;
			final R r = get(cop.getReg(), T.RETURN_ADDRESS);
			// bytecode restriction: register can only be consumed once
			setNull(cop.getReg());
			// bytecode restriction: only called via matching JSR, Sub known as register value
			final Sub sub = (Sub) r.getValue();
			if (!this.frame.popSub(sub)) {
				LOGGER.warning("Illegal return from subroutine! Not in subroutine stack: " + sub);
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
				final int returnPc = jsr.getPc() + 1;
				bb.setSucc(getTargetBb(returnPc));
				merge(returnPc);
			}
			return -1;
		}
		case RETURN: {
			assert op instanceof RETURN;

			// don't need op type here, could check, but why should we...
			final T returnT = this.cfg.getMd().getM().getReturnT();
			if (returnT != T.VOID) {
				// just type reduction
				pop(returnT);
			}
			return -1;
		}
		case SHL: {
			final SHL cop = (SHL) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case SHR: {
			final SHR cop = (SHR) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case STORE: {
			final STORE cop = (STORE) op;
			// The astore instruction is used with an objectref of type returnAddress when
			// implementing the finally clauses of the Java programming language (see Section
			// 7.13, "Compiling finally"). The aload instruction cannot be used to load a value
			// of type returnAddress from a local variable onto the operand stack. This
			// asymmetry with the astore instruction is intentional.
			final R r = pop(cop.getT());

			// TODO hack, check store type in debug variable
			final V debugV = this.cfg.getDebugV(cop.getReg(), nextPc);
			if (debugV != null) {
				r.mergeTo(debugV.getT());
			}

			setMove(cop.getReg(), r);
			break;
		}
		case SUB: {
			final SUB cop = (SUB) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case SWAP: {
			assert op instanceof SWAP;

			final R s1 = this.frame.pop();
			final R s2 = this.frame.pop();
			pushMove(s1);
			pushMove(s2);
			break;
		}
		case SWITCH: {
			final SWITCH cop = (SWITCH) op;

			// build sorted map: unique case pc -> matching case keys
			final TreeMap<Integer, List<Integer>> casePc2keys = new TreeMap<Integer, List<Integer>>();
			List<Integer> keys;
			final int[] caseKeys = cop.getCaseKeys();
			final int[] casePcs = cop.getCasePcs();
			for (int i = 0; i < caseKeys.length; ++i) {
				final int casePc = casePcs[i];
				keys = casePc2keys.get(casePc);
				if (keys == null) {
					keys = new ArrayList<Integer>();
					casePc2keys.put(casePc, keys);
				}
				keys.add(caseKeys[i]);
			}
			// add default branch, can overlay with other cases, even JDK 6 doesn't optimize
			final int defaultPc = cop.getDefaultPc();
			keys = casePc2keys.get(defaultPc);
			if (keys == null) {
				keys = new ArrayList<Integer>();
				casePc2keys.put(defaultPc, keys);
			}
			keys.add(null);

			// now add successors
			for (final Map.Entry<Integer, List<Integer>> casePc2keysEntry : casePc2keys.entrySet()) {
				keys = casePc2keysEntry.getValue();
				bb.addSwitchSucc(keys.toArray(new Integer[keys.size()]),
						getTargetBb(casePc2keysEntry.getKey()));
			}

			pop(T.INT);
			merge(cop.getDefaultPc());
			for (final int casePc : cop.getCasePcs()) {
				merge(casePc);
			}
			return -1;
		}
		case THROW:
			assert op instanceof THROW;

			// just type reduction
			pop(this.cfg.getMd().getM().getT().getDu().getT(Throwable.class));
			return -1;
		case XOR: {
			final XOR cop = (XOR) op;
			evalBinaryMath(cop.getT());
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

	private R get(final int i, final T t) {
		final R r = this.frame.get(i);
		if (!r.read(t)) {
			throw new RuntimeException("Incompatible local register type!");
		}
		return r;
	}

	private Frame getFrame(final int pc) {
		return this.cfg.getFrame(pc);
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
		final BB split = newBb(bb.getPc());

		bb.moveIns(split);
		split.setSucc(bb);
		while (bb.getOps() > 0 && bb.getOp(0).getPc() != pc) {
			final Op op = bb.removeOp(0);
			split.addOp(op);
			this.pc2bbs[op.getPc()] = split;
		}
		bb.setPc(pc);
		return bb;
	}

	private void merge(final int pc) {
		final Frame targetFrame = getFrame(pc);
		if (targetFrame == null) {
			// visit new frame, no merge
			this.cfg.setFrame(pc, new Frame(this.frame));
			return;
		}
		// frame already visited, real merge necessary
		for (int i = targetFrame.size(); i-- > 0;) {
			final R prevR = targetFrame.get(i);
			final R newR = this.frame.get(i);
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
				mergeReplaceReg(this.pc2bbs[pc], i, prevR, null);
				continue;
			}
			final T t = R.merge(prevR, newR);
			if (t == null) {
				// merge type is null? merge to null => replace previous register from here
				mergeReplaceReg(this.pc2bbs[pc], i, prevR, null);
				continue;
			}
			// only here can we create or enhance a merge registers
			if (prevR.getKind() == Kind.MERGE && prevR.getPc() == pc) {
				// merge register already starts here, add new register
				prevR.merge(newR);
				continue;
			}
			// start new merge register
			mergeReplaceReg(this.pc2bbs[pc], i, prevR, new R(pc, t, Kind.MERGE, prevR, newR));
		}
	}

	private void mergeExc(final int pc) {
		final Exc[] excs = this.cfg.getExcs();
		if (excs == null) {
			return;
		}
		for (final Exc exc : excs) {
			if (exc.validIn(pc)) {
				this.frame = new Frame(getFrame(pc));
				this.frame.clear();

				final Frame handlerFrame = getFrame(exc.getHandlerPc());
				if (handlerFrame == null) {
					// TODO handling for mergeExc with single stack exception value suboptimal
					merge(exc.getHandlerPc());
					this.frame = getFrame(exc.getHandlerPc());
					// null is <any> (means Java finally) -> Throwable
					pushConst(exc.getT() == null ? this.cfg.getMd().getM().getT().getDu()
							.getT(Throwable.class) : exc.getT());
				} else {
					if (handlerFrame.getTop() != 1) {
						LOGGER.warning("Handler stack for exception merge not of size 1!");
					}
					this.frame.push(handlerFrame.peek()); // reuse exception register
					merge(exc.getHandlerPc());
				}
			}
		}
	}

	private void mergeReplaceReg(final BB bb, final int i, final R prevR, final R newR) {
		assert prevR != null;

		// could have no operations yet (concurrent CFG building)
		Frame frame = getFrame(bb.getPc());
		R replacedR = frame.replaceReg(i, prevR, newR);
		for (int j = 1; replacedR != null && j < bb.getOps(); ++j) {
			frame = this.cfg.getInFrame(bb.getOp(j));
			replacedR = frame.replaceReg(i, replacedR, newR);
		}
		if (replacedR != null) {
			for (final E out : bb.getOuts()) {
				mergeReplaceReg(out.getEnd(), i, replacedR, newR);
			}
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
			for (final Exc exc : this.cfg.getExcs()) {
				if (!exc.validIn(pc)) {
					continue;
				}
				final int handlerPc = exc.getHandlerPc();
				List<T> types = handlerPc2type.get(handlerPc);
				if (types == null) {
					types = new ArrayList<T>();
					handlerPc2type.put(handlerPc, types);
				}
				types.add(exc.getT());
			}
			// now add successors
			for (final Map.Entry<Integer, List<T>> handlerPc2typeEntry : handlerPc2type.entrySet()) {
				final List<T> types = handlerPc2typeEntry.getValue();
				bb.addCatchSucc(types.toArray(new T[types.size()]),
						getTargetBb(handlerPc2typeEntry.getKey()));
			}
		}
		return bb;
	}

	private R pop(final T t) {
		final R s = this.frame.pop();
		if (!s.read(t)) {
			throw new RuntimeException("Incompatible local register type!");
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

	private R setConst(final int i, final T t, final Object value) {
		final R prevR = this.frame.get(i);
		final R newR = prevR == null ? new R(this.pc, t, value, Kind.CONST) : new R(this.pc, t,
				value, Kind.CONST, prevR);
		this.frame.set(i, newR);
		return newR;
	}

	private R setMove(final int i, final R r) {
		final R prevR = this.frame.get(i);
		final R newR = prevR == null ? new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r)
				: new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r, prevR);
		this.frame.set(i, newR);
		return newR;
	}

	private void setNull(final int i) {
		this.frame.set(i, null);
	}

	private void transform() {
		this.cfg.initFrames();

		this.isIgnoreExceptions = this.cfg.getMd().getTd().getCu().check(DFlag.IGNORE_EXCEPTIONS);
		this.pc2bbs = new BB[this.cfg.getOps()];

		// start with PC 0 and new BB
		this.pc = 0;
		BB bb = newBb(0);
		this.cfg.setStartBb(bb);

		while (true) {
			if (this.pc < 0) {
				// next open pc?
				if (this.openPcs.isEmpty()) {
					break;
				}
				this.pc = this.openPcs.removeFirst();
				bb = this.pc2bbs[this.pc];
			} else {
				this.pc2bbs[this.pc] = bb;
				if (!this.isIgnoreExceptions && bb.getOps() > 0 && this.cfg.getExcs() != null) {
					// exception block changes in none-empty BB? -> split necessary!
					for (final Exc exc : this.cfg.getExcs()) {
						if (exc.validIn(this.pc)) {
							if (exc.validIn(bb.getPc())) {
								// exception is valid - has been valid at BB entry -> OK
								continue;
							}
						} else {
							if (!exc.validIn(bb.getPc())) {
								// exception isn't valid - hasn't bean valid at BB entry -> OK
								continue;
							}
						}
						// at least one exception has changed, newBb() links exceptions
						final BB succ = newBb(this.pc);
						bb.setSucc(succ);
						bb = succ;
						break;
					}
				}
			}
			if (!this.isIgnoreExceptions) {
				mergeExc(this.pc);
			}
			final Op op = this.cfg.getOp(this.pc);
			bb.addOp(op);
			this.pc = execute(op, bb);
		}
		this.cfg.calculatePostorder();
	}

}