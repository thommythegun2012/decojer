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
		final R s2 = pop(t, true);
		final R s1 = pop(t, true);

		assert R.merge(s1, s2) != null;

		if (s1.getT().isRef()) {
			// (J)CMP EQ / NE
			assert T.VOID == resultT : resultT;

			return;
		}
		if (resultT != T.VOID) {
			// TODO inputs really uninteresting?
			pushConst(resultT != null ? resultT : t);
		}
	}

	private int executeMerge(final Op op, final BB bb) {
		bb.addOp(op);
		this.frame = new Frame(this.cfg.getFrame(op.getPc()));
		int nextPc = op.getPc() + 1;
		switch (op.getOptype()) {
		case ADD: {
			final ADD cop = (ADD) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case ALOAD: {
			final ALOAD cop = (ALOAD) op;
			pop(T.INT, true); // index
			pop(T.REF, true); // array
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

			pop(T.REF, true); // array
			pushConst(T.INT); // length
			break;
		}
		case ASTORE: {
			final ASTORE cop = (ASTORE) op;
			pop(cop.getT(), true); // value
			pop(T.INT, true); // index
			pop(T.REF, true); // array
			break;
		}
		case CAST: {
			final CAST cop = (CAST) op;
			pop(cop.getT(), true);
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
			assert op instanceof FILLARRAY;

			pop(T.REF, true);
			break;
		}
		case GET: {
			final GET cop = (GET) op;
			final F f = cop.getF();
			if (!f.check(AF.STATIC)) {
				pop(f.getT(), true);
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
			final R r = read(cop.getReg(), cop.getT(), true);
			r.inc(cop.getValue());
			break;
		}
		case INSTANCEOF: {
			assert op instanceof INSTANCEOF;

			pop(T.REF, true);
			// operation contains check-type as argument, not important here
			pushConst(T.BOOLEAN);
			break;
		}
		case INVOKE: {
			final INVOKE cop = (INVOKE) op;
			final M m = cop.getM();
			for (int i = m.getParams(); i-- > 0;) {
				pop(m.getParamT(i), true);
			}
			if (!m.check(AF.STATIC)) {
				pop(m.getT(), true);
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
			pop(cop.getT(), true);
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
			final Frame targetFrame = this.cfg.getFrame(cop.getTargetPc());
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
					this.frame = new Frame(this.cfg.getFrame(ret.getPc()));
					if (sub != read(ret.getReg(), T.RET, true).getValue()) {
						// don't assert here, need this get for frames return-address-null update
						LOGGER.warning("Incorrect sub!");
					}
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
				pushConst(T.RET, sub);
				merge(cop.getTargetPc());
			}
			return -1;
		}
		case LOAD: {
			final LOAD cop = (LOAD) op;
			final R r = read(cop.getReg(), cop.getT(), false);
			// no previous for stack
			pushMove(r);
			break;
		}
		case MONITOR: {
			assert op instanceof MONITOR;

			pop(T.REF, true);
			break;
		}
		case MUL: {
			final MUL cop = (MUL) op;
			evalBinaryMath(cop.getT());
			break;
		}
		case NEG: {
			final NEG cop = (NEG) op;
			final R r = pop(cop.getT(), true);
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
				pop(T.INT, true);
			}
			pushConst(this.cfg.getDu().getArrayT(cop.getT(), cop.getDimensions()));
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
			pop(f.getValueT(), true);
			if (!f.check(AF.STATIC)) {
				pop(f.getT(), true);
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
			final R r = read(cop.getReg(), T.RET, true);
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
			final RETURN cop = (RETURN) op;

			// don't need op type here, could check, but why should we...
			final T returnT = cop.getT();

			assert returnT == this.cfg.getMd().getM().getReturnT();

			if (returnT != T.VOID) {
				pop(returnT, true); // just read type reduction
			}
			return -1;
		}
		case SHL: {
			final SHL cop = (SHL) op;
			pop(cop.getShiftT(), true);
			pop(cop.getT(), true);
			pushConst(cop.getT());
			break;
		}
		case SHR: {
			final SHR cop = (SHR) op;
			pop(cop.getShiftT(), true);
			pop(cop.getT(), true);
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
			final R r = pop(cop.getT(), false);

			final R storeR = store(cop.getReg(), r);
			final V debugV = this.cfg.getDebugV(cop.getReg(), nextPc);
			if (debugV != null) {
				storeR.setRealT(debugV.getT());
			}
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

			pop(T.INT, true);
			merge(cop.getDefaultPc());
			for (final int casePc : cop.getCasePcs()) {
				merge(casePc);
			}
			return -1;
		}
		case THROW:
			assert op instanceof THROW;

			// just type reduction
			pop(this.cfg.getDu().getT(Throwable.class), true);
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
		final BB splitBb = newBb(bb.getPc());

		bb.moveIns(splitBb);
		splitBb.setSucc(bb);
		while (bb.getOps() > 0 && bb.getOp(0).getPc() != pc) {
			final Op op = bb.removeOp(0);
			splitBb.addOp(op);
			this.pc2bbs[op.getPc()] = splitBb;
		}
		bb.setPc(pc);
		return bb;
	}

	private void merge(final int pc) {
		final Frame targetFrame = this.cfg.getFrame(pc);
		if (targetFrame == null) {
			// first visit at target frame -> no BB join -> no type merge
			this.cfg.setFrame(pc, new Frame(this.frame));
			return;
		}
		// target frame already visited -> BB join -> type merge
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

	private void mergeExceptions(final Op op) {
		if (this.isIgnoreExceptions || this.cfg.getExcs() == null) {
			return;
		}
		final int pc = op.getPc();
		for (final Exc exc : this.cfg.getExcs()) {
			if (!exc.validIn(pc)) {
				continue;
			}
			this.frame = new Frame(this.cfg.getFrame(pc));

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

		// could have no operations yet (concurrent CFG building)
		Frame frame = this.cfg.getFrame(bb.getPc());
		R replacedR = frame.replaceReg(i, prevR, newR);
		for (int j = 1; replacedR != null && j < bb.getOps(); ++j) {
			frame = this.cfg.getInFrame(bb.getOp(j));
			replacedR = frame.replaceReg(i, replacedR, newR);
		}
		if (replacedR != null) {
			for (final E out : bb.getOuts()) {
				// backward merge in loop could create new merge necessity at start point!
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
			for (final Exc exc : excs) {
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

	private R pop(final T t, final boolean read) {
		final R s = this.frame.pop();
		if (read) {
			if (!s.read(t)) {
				throw new RuntimeException("Incompatible local register type!");
			}
		} else {
			if (!s.isAssignableTo(t)) {
				throw new RuntimeException("Incompatible local register type!");
			}
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

	private R read(final int i, final T t, final boolean read) {
		// start new register and TODO backpropagate alive for existing (read number)
		final R prevR = this.frame.get(i);
		if (prevR == null) {
			throw new RuntimeException("Cannot read register " + i + " (null) as type '" + t + "'!");
		}
		if (read) {
			if (!prevR.read(t)) {
				throw new RuntimeException("Cannot read register " + i + " (" + prevR
						+ ") as type '" + t + "'!");
			}
		} else {
			if (!prevR.isAssignableTo(t)) {
				throw new RuntimeException("Cannot read register " + i + " (" + prevR
						+ ") as type '" + t + "'!");
			}
		}
		if (t == T.RET) {
			// bytecode restriction: internal return address type can only be read once
			this.frame.set(i, null);
			return prevR;
		}
		this.frame.set(i, new R(this.pc, prevR.getT(), prevR.getValue(), Kind.READ, prevR));
		return prevR;
	}

	/**
	 * Exception block changes in BB? -> split necessary!
	 * 
	 * @param bb
	 *            BB
	 * @return original BB or new BB for beginning exception block
	 */
	private BB splitExceptions(final BB bb) {
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
				if (!exc.validIn(bb.getPc())) {
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
		final R prevR = this.frame.get(i);
		final R newR = prevR == null ? new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r)
				: new R(this.pc, r.getT(), r.getValue(), Kind.MOVE, r, prevR);
		this.frame.set(i, newR);
		return newR;
	}

	private void transform() {
		this.cfg.initFrames();

		this.isIgnoreExceptions = this.cfg.getCu().check(DFlag.IGNORE_EXCEPTIONS);
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
				bb = splitExceptions(bb);
				this.pc2bbs[this.pc] = bb;
			}
			final Op op = this.cfg.getOp(this.pc);
			this.pc = executeMerge(op, bb);
			mergeExceptions(op); // execute has influence on this, read type reduce
		}
	}

}