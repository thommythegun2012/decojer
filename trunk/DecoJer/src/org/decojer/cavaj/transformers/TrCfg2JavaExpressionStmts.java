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

import static org.decojer.cavaj.utils.Expressions.getBooleanValue;
import static org.decojer.cavaj.utils.Expressions.getNumberValue;
import static org.decojer.cavaj.utils.Expressions.getOp;
import static org.decojer.cavaj.utils.Expressions.newAssignment;
import static org.decojer.cavaj.utils.Expressions.newInfixExpression;
import static org.decojer.cavaj.utils.Expressions.newLiteral;
import static org.decojer.cavaj.utils.Expressions.newPostfixExpression;
import static org.decojer.cavaj.utils.Expressions.newPrefixExpression;
import static org.decojer.cavaj.utils.Expressions.newSimpleName;
import static org.decojer.cavaj.utils.Expressions.newSingleVariableDeclaration;
import static org.decojer.cavaj.utils.Expressions.newType;
import static org.decojer.cavaj.utils.Expressions.newTypeName;
import static org.decojer.cavaj.utils.Expressions.not;
import static org.decojer.cavaj.utils.Expressions.setOp;
import static org.decojer.cavaj.utils.Expressions.wrap;
import static org.decojer.cavaj.utils.Expressions.wrapAddAll;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.Container;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.ops.CAST;
import org.decojer.cavaj.model.code.ops.CmpType;
import org.decojer.cavaj.model.code.ops.DUP;
import org.decojer.cavaj.model.code.ops.FILLARRAY;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.INC;
import org.decojer.cavaj.model.code.ops.INSTANCEOF;
import org.decojer.cavaj.model.code.ops.INVOKE;
import org.decojer.cavaj.model.code.ops.JCMP;
import org.decojer.cavaj.model.code.ops.JCND;
import org.decojer.cavaj.model.code.ops.LOAD;
import org.decojer.cavaj.model.code.ops.MONITOR;
import org.decojer.cavaj.model.code.ops.NEW;
import org.decojer.cavaj.model.code.ops.NEWARRAY;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.POP;
import org.decojer.cavaj.model.code.ops.PUSH;
import org.decojer.cavaj.model.code.ops.PUT;
import org.decojer.cavaj.model.code.ops.RETURN;
import org.decojer.cavaj.model.code.ops.SHR;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.code.ops.SWITCH;
import org.decojer.cavaj.model.code.ops.THROW;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.model.types.Version;
import org.decojer.cavaj.utils.Expressions;
import org.decojer.cavaj.utils.Priority;
import org.decojer.cavaj.utils.SwitchTypes;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.google.common.collect.Lists;

/**
 * Transformer: CFG to Java Expression Statements ASTs.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrCfg2JavaExpressionStmts {

	private static boolean isLambdaBootstrapMethod(@Nullable final M m) {
		if (m == null) {
			return false;
		}
		final T ownerT = m.getT();
		if (ownerT == null) {
			return false;
		}
		if (!ownerT.getName().equals("java.lang.invoke.LambdaMetafactory")) {
			return false;
		}
		if (!m.getReturnT().getName().equals("java.lang.invoke.CallSite")) {
			return false;
		}
		final T[] paramTs = m.getParamTs();
		if (paramTs.length != 6) {
			return false;
		}
		if (!paramTs[0].getName().equals("java.lang.invoke.MethodHandles$Lookup")) {
			return false;
		}
		if (!paramTs[1].getName().equals("java.lang.String")) {
			return false;
		}
		if (!paramTs[2].getName().equals("java.lang.invoke.MethodType")) {
			return false;
		}
		if (!paramTs[3].getName().equals("java.lang.invoke.MethodType")) {
			return false;
		}
		if (!paramTs[4].getName().equals("java.lang.invoke.MethodHandle")) {
			return false;
		}
		if (!paramTs[5].getName().equals("java.lang.invoke.MethodType")) {
			return false;
		}
		return true;
	}

	@Nonnull
	private static Expression newInfixExpressionPop(@Nullable final Operator operator,
			@Nonnull final BB bb, @Nonnull final Op op) {
		final Expression rightOperand = bb.pop(); // keep order
		return newInfixExpression(operator, bb.pop(), rightOperand, op);
	}

	private static boolean pullStackValue(@Nonnull final BB bb) {
		final E pred2bb = bb.getSequenceIn();
		if (pred2bb == null || pred2bb.isBack()) {
			return false;
		}
		final BB pred = pred2bb.getStart();
		if (pred.getTop() == 0 && !pullStackValue(pred)) {
			// recursion should create stack value
			return false;
		}
		final Expression pop = pred.pop();
		for (final E out : pred.getOuts()) {
			if (out.isCatch()) {
				continue; // stack cleared
			}
			final BB end = out.getEnd();
			final List<E> ins = end.getIns();
			// single incoming edge? no merge necessary
			if (ins.size() == 1) {
				end.pushFirst(pop);
				continue;
			}
			// add artificial entry BB with value
			final BB newBb = bb.getCfg().newBb(end.getPc());
			newBb.setPostorder(end.getPostorder());
			newBb.push(pop);
			pred.addSucc(newBb, out.getValue());
			newBb.setSucc(end);
		}
		return true;
	}

	/**
	 * Rewrite char-switches.
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - success
	 */
	private static boolean rewriteSwitchChar(@Nonnull final BB bb) {
		for (final E out : bb.getOuts()) {
			if (!out.isSwitchCase()) {
				continue;
			}
			final Object[] caseValues = (Object[]) out.getValue();
			assert caseValues != null;
			for (int i = caseValues.length; i-- > 0;) {
				final Object caseValue = caseValues[i];
				if (!(caseValue instanceof Integer)) {
					assert caseValue == null; // default
					continue;
				}
				caseValues[i] = Character.valueOf((char) ((Integer) caseValue).intValue());
			}
		}
		return true;
	}

	/**
	 * Transform CFG.
	 *
	 * @param cfg
	 *            CFG
	 */
	public static void transform(@Nonnull final CFG cfg) {
		new TrCfg2JavaExpressionStmts(cfg).transform();
	}

	@Getter(AccessLevel.PROTECTED)
	@Nonnull
	private final CFG cfg;

	/**
	 * Flag indicates whether rewrite field init (for PUT operation) is possible, that means that no
	 * previous invalid operations happened (like LOAD normal method argument).
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private boolean fieldInit;

	private TrCfg2JavaExpressionStmts(@Nonnull final CFG cfg) {
		this.cfg = cfg;
	}

	private boolean convertToHLLIntermediate(@Nonnull final BB bb) {
		while (bb.getOps() > 0) {
			while (isStackUnderflow(bb)) {
				// we must try to pull stack values down
				while (rewriteConditionalValue(bb)) {
					// nested possible
				} // ...now pull this value
				final Op op = bb.getOp(0);
				if (bb.getIns().size() > 1) {
					// sometimes conditional values are not a possible pattern, because the
					// conditional sides contains statements or multiple ins, special handling for
					// JCND and RETURN possible:

					// TODO scalalib Symbol/updateCache: also found with STORE, this here can be
					// more general! build extra nodes or pushup STORE op?
					// TODO also found invoke(switch(pushes)) in
					// scala.xml.parsing.MarkupParsewrCommons, need temporary vars here
					if (op instanceof JCND) {
						if (rewriteConditionalConstants(bb)) {
							// special handling for JCND, multiple predecessors possible and can be
							// directly routed through this BB
							return true; // deleted myself
						}
					} else if (op instanceof RETURN) {
						if (rewriteConditionalReturn(bb)) {
							// special handling for RETURN, multiple predecessors possible and can
							// directly return value - should be Scala only code
							if (!getCfg().getT().isScala()) {
								log.warn(getM()
										+ ": Rewriting of conditional returns should only happen for Scala based classes!");
							}
							return true; // deleted myself
						}
					}
					return false;
				}
				if (op instanceof STORE) {
					// special trick for store <return_address> till finally is established
					if (peekT(op) == T.RET) {
						bb.removeOp(0);
						if (bb.getOps() != 0) {
							continue;
						}
						// i'm empty now...can delete myself (move ins to succ)
						final E out = bb.getSequenceOut();
						assert out != null;
						bb.moveIns(out.getEnd());
						return true;
					}
				}
				final E pred2bb = bb.getRelevantIn();
				if (pred2bb == null) {
					return false; // multiple or conditional incomings -> real fail
				}
				final BB pred = pred2bb.getStart();
				final E pred2bbBack = pred.getRelevantOut();
				if (pred2bbBack != null && bb == pred2bbBack.getEnd()) {
					bb.joinPredBb(pred);
					continue;
				}
				// this is a last resort (helps e.g. for synthetic enum-switch-map-initializer),
				// sometimes gives wrong results for conditionals with merging stack values:
				// (e.g. PUSH 0, LOAD x, JCND; true -> POP, PUSH 1 -> lastBb; false -> lastBb)
				if (!pullStackValue(bb)) {
					return false;
				}
			}
			final Op op = bb.removeOp(0);
			if (op == null) {
				assert false;
				return false;
			}
			Statement statement = null;
			switch (op.getOptype()) {
			case ADD: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.PLUS, bb, op));
				break;
			}
			case ALOAD: {
				final ArrayAccess arrayAccess = setOp(getAst().newArrayAccess(), op);
				arrayAccess.setIndex(wrap(bb.pop()));
				arrayAccess.setArray(wrap(bb.pop(), Priority.ARRAY_INDEX));
				bb.push(arrayAccess);
				break;
			}
			case AND: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.AND, bb, op));
				break;
			}
			case ARRAYLENGTH: {
				final Expression expression = bb.pop();
				if (expression instanceof Name) {
					// annotationsVisible.length
					bb.push(setOp(
							getAst().newQualifiedName((Name) wrap(expression),
									getAst().newSimpleName("length")), op));
					break;
				}
				// FieldAccess or MethodInvocation:
				// this.code.length, getInterfaces().length
				final FieldAccess fieldAccess = setOp(getAst().newFieldAccess(), op);
				fieldAccess.setExpression(wrap(expression, Priority.MEMBER_ACCESS));
				fieldAccess.setName(getAst().newSimpleName("length"));
				bb.push(fieldAccess);
				break;
			}
			case ASTORE: {
				final Expression rightOperand = bb.pop();
				final Expression indexExpression = bb.pop();
				final Expression arrayRefExpression = bb.pop();
				if (arrayRefExpression instanceof ArrayCreation) {
					final ArrayCreation arrayCreation = (ArrayCreation) arrayRefExpression;
					ArrayInitializer arrayInitializer = arrayCreation.getInitializer();
					if (arrayInitializer == null) {
						arrayInitializer = setOp(getAst().newArrayInitializer(), op);
						arrayCreation.setInitializer(arrayInitializer);
						// TODO for higher performance and for full array creation removement we
						// could defer the 0-fill and rewrite to the final A/STORE phase
						final Object sizeExpression = arrayCreation.dimensions().get(0);
						final Number size = sizeExpression instanceof Expression ? getNumberValue((Expression) sizeExpression)
								: null;
						assert size != null;
						final T t = peekT(op);
						assert t != null;
						// not all indexes may be set, null/0/false in JVM 7 are not set, fill
						for (int i = size.intValue(); i-- > 0;) {
							arrayInitializer.expressions().add(newLiteral(t, null, getM(), op));
						}
						arrayCreation.dimensions().clear();
					}
					final Number numberValue = getNumberValue(indexExpression);
					assert numberValue != null;
					arrayInitializer.expressions().set(numberValue.intValue(), wrap(rightOperand));
					break;
				}
				final ArrayAccess arrayAccess = setOp(getAst().newArrayAccess(), op);
				arrayAccess.setArray(wrap(arrayRefExpression, Priority.ARRAY_INDEX));
				arrayAccess.setIndex(wrap(indexExpression));
				final Assignment assignment = newAssignment(Assignment.Operator.ASSIGN,
						arrayAccess, rightOperand, op);
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				// inline assignment, DUP(_X1) -> PUT
				if (!bb.isStackEmpty() && bb.peek() == rightOperand) {
					bb.pop();
					bb.push(assignment);
					break;
				}
				statement = getAst().newExpressionStatement(assignment);
				break;
			}
			case CAST: {
				final CAST cop = (CAST) op;
				final CastExpression castExpression = setOp(getAst().newCastExpression(), op);
				castExpression.setType(newType(cop.getToT(), getM()));
				castExpression.setExpression(wrap(bb.pop(), Priority.TYPE_CAST));
				bb.push(castExpression);
				break;
			}
			case CMP: {
				// pseudo expression for following JCND, not really the correct
				// answer for -1, 0, 1
				bb.push(newInfixExpressionPop(InfixExpression.Operator.LESS_EQUALS, bb, op));
				break;
			}
			case DIV: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.DIVIDE, bb, op));
				break;
			}
			case DUP: {
				final DUP cop = (DUP) op;
				switch (cop.getKind()) {
				// for all following variants: don't change op without copying!
				case DUP:
					bb.push(bb.peek());
					break;
				case DUP_X1: {
					final Expression e1 = bb.pop();
					final Expression e2 = bb.pop();
					bb.push(e1);
					bb.push(e2);
					bb.push(e1);
					break;
				}
				case DUP_X2: {
					final Expression e1 = bb.pop();
					final Expression e2 = bb.pop();
					if (!isWide(e2)) {
						final Expression e3 = bb.pop();
						bb.push(e1);
						bb.push(e3);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					bb.push(e1);
					bb.push(e2);
					bb.push(e1);
					break;
				}
				case DUP2: {
					final Expression e1 = bb.peek();
					if (!isWide(e1)) {
						final Expression e2 = bb.peek(1);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					bb.push(e1);
					break;
				}
				case DUP2_X1: {
					final Expression e1 = bb.pop();
					if (!isWide(e1)) {
						final Expression e2 = bb.pop();
						final Expression e3 = bb.pop();
						bb.push(e2);
						bb.push(e1);
						bb.push(e3);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					final Expression e3 = bb.pop();
					bb.push(e1);
					bb.push(e3);
					bb.push(e1);
					break;
				}
				case DUP2_X2: {
					final Expression e1 = bb.pop();
					if (!isWide(e1)) {
						final Expression e2 = bb.pop();
						final Expression e3 = bb.pop();
						if (!isWide(e3)) {
							final Expression e4 = bb.pop();
							bb.push(e2);
							bb.push(e1);
							bb.push(e4);
							bb.push(e3);
							bb.push(e2);
							bb.push(e1);
							break;
						}
						bb.push(e2);
						bb.push(e1);
						bb.push(e3);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					final Expression e3 = bb.pop();
					if (!isWide(e3)) {
						final Expression e4 = bb.pop();
						bb.push(e1);
						bb.push(e4);
						bb.push(e3);
						bb.push(e1);
						break;
					}
					bb.push(e1);
					bb.push(e3);
					bb.push(e1);
					break;
				}
				default:
					log.warn(getM() + ": Unknown DUP type '" + cop.getKind() + "'!");
				}
				break;
			}
			case FILLARRAY: {
				final FILLARRAY cop = (FILLARRAY) op;
				final T t = peekT(op);
				final T componentT = t.getComponentT();
				assert componentT != null;

				Expression expression = bb.pop();
				if (!(expression instanceof ArrayCreation)) {
					// LOAD 0, NEWARRAY, STORE 0, LOAD 0, FILLARRAY <end>
					// TODO Dalvik: if STORE x / LOAD x are compressed then we shouldn't need this:
					expression = setOp(getAst().newArrayCreation(), op);
					((ArrayCreation) expression).setType((ArrayType) newType(t, getM()));
				}
				final ArrayInitializer arrayInitializer = setOp(getAst().newArrayInitializer(), op);
				final Object[] values = cop.getValues();
				assert values != null;
				for (final Object value : values) {
					arrayInitializer.expressions().add(newLiteral(componentT, value, getM(), op));
				}
				((ArrayCreation) expression).setInitializer(arrayInitializer);

				bb.push(expression);
				break;
			}
			case GET: {
				final GET cop = (GET) op;
				if (rewriteCachedClassLiteral(bb, cop)) {
					break;
				}
				final F f = cop.getF();
				if (f.isStatic()) {
					// Eclipse AST expects a Name for f.getT(), not a Type:
					// is OK - f.getT() cannot be generic
					bb.push(setOp(
							getAst().newQualifiedName(newTypeName(f.getT(), getM()),
									newSimpleName(f.getName(), getAst())), op));
					break;
				}
				final FieldAccess fieldAccess = setOp(getAst().newFieldAccess(), op);
				fieldAccess.setExpression(wrap(bb.pop(), Priority.MEMBER_ACCESS));
				fieldAccess.setName(newSimpleName(f.getName(), getAst()));
				bb.push(fieldAccess);
				break;
			}
			case GOTO: {
				// not really necessary, but important for
				// 1) correct opPc blocks
				// 2) line numbers

				// TODO put line number anywhere?
				// remember as pseudo statement? but problem with boolean ops
				break;
			}
			case INC: {
				final INC cop = (INC) op;
				final int value = cop.getValue();
				if (value == 1 || value == -1) {
					// ++ / --
					if (rewriteInlinePrefixIncDec(bb, cop) || rewriteInlinePostfixIncDec(bb, cop)) {
						break;
					}
					if (bb.isStackEmpty()) {
						statement = getAst().newExpressionStatement(
								newPrefixExpression(
										cop.getValue() == 1 ? PrefixExpression.Operator.INCREMENT
												: PrefixExpression.Operator.DECREMENT,
												getVarExpression(cop.getReg(), cop.getPc(), op), op));
						break;
					}
					log.warn(getM() + ": Inline ++/--!");
					break;
				}
				if (rewriteInlineRegAssignment(bb, cop)) {
					break;
				}
				if (bb.isStackEmpty()) {
					statement = getAst().newExpressionStatement(
							newAssignment(
									value >= 0 ? Assignment.Operator.PLUS_ASSIGN
											: Assignment.Operator.MINUS_ASSIGN,
											getVarExpression(cop.getReg(), cop.getPc(), op),
											newLiteral(cop.getT(), value >= 0 ? value : -value, getCfg()
													.getM(), op), op));
					break;
				}
				log.warn(getM() + ": Inline INC with value '" + value + "'!");
				break;
			}
			case INSTANCEOF: {
				final INSTANCEOF cop = (INSTANCEOF) op;
				final InstanceofExpression instanceofExpression = setOp(getAst()
						.newInstanceofExpression(), op);
				instanceofExpression.setLeftOperand(wrap(bb.pop(), Priority.INSTANCEOF));
				instanceofExpression.setRightOperand(newType(cop.getT(), getM()));
				bb.push(instanceofExpression);
				break;
			}
			case INVOKE: {
				final INVOKE cop = (INVOKE) op;
				final M m = cop.getM();

				// read method invokation arguments, handle varargs
				final List<Expression> arguments = Lists.newArrayList();
				final int params = m.getParamTs().length;
				if (params > 0) {
					if (m.isVarargs()) {
						final Expression array = bb.pop();
						if (array instanceof ArrayCreation) {
							final ArrayInitializer initializer = ((ArrayCreation) array)
									.getInitializer();
							if (initializer != null) {
								for (final Expression e : (List<Expression>) initializer
										.expressions()) {
									// wrap() in addAll(), DUP could happen with receiver & argument
									arguments.add(e);
								}
								Collections.reverse(arguments);
							}
						} else {
							// can happen if forwarded as variable
							arguments.add(array);
						}
					} else {
						arguments.add(bb.pop());
					}
					// now add remaining parameters that cannot be varargs
					for (int i = params - 1; i-- > 0;) {
						arguments.add(bb.pop());
					}
					Collections.reverse(arguments);
				}

				final Expression methodExpression;
				if (cop.isDirect()) {
					final Expression expression = bb.pop();
					if (m.isConstructor()) {
						methodExpression = null;
						final T ownerT = m.getT();
						if (expression instanceof ThisExpression) {
							enumConstructor: if (ownerT != null && ownerT.is(Enum.class)
									&& !getCfg().getCu().check(DFlag.IGNORE_ENUM)) {
								if (arguments.size() < 2) {
									log.warn(getM() + ": Super constructor invocation '" + m
											+ "' for enum has less than 2 arguments!");
									break enumConstructor;
								}
								if (!m.getParamTs()[0].is(String.class)) {
									log.warn(getM()
											+ ": Super constructor invocation '"
											+ m
											+ "' for enum must contain string literal as first parameter!");
									break enumConstructor;
								}
								if (m.getParamTs()[1] != T.INT) {
									log.warn(getM()
											+ ": Super constructor invocation '"
											+ m
											+ "' for enum must contain number literal as first parameter!");
									break enumConstructor;
								}
								arguments.remove(0);
								arguments.remove(0);
							}
						if (ownerT != null && ownerT.is(getCfg().getT())) {
							final ConstructorInvocation constructorInvocation = getAst()
									.newConstructorInvocation();
							wrapAddAll(constructorInvocation.arguments(), arguments);
							statement = constructorInvocation;
							break;
						}
						if (arguments.size() == 0) {
							// implicit super callout, more checks possible but not necessary
							break;
						}
						final SuperConstructorInvocation superConstructorInvocation = getAst()
								.newSuperConstructorInvocation();
						wrapAddAll(superConstructorInvocation.arguments(), arguments);
						statement = superConstructorInvocation;
						break;
						}
						if (expression instanceof ClassInstanceCreation) {
							if (ownerT != null && ownerT.isInner()
									&& !getCfg().getCu().check(DFlag.IGNORE_CONSTRUCTOR_THIS)) {
								// inner class constructor invocation has synthetic this reference
								// as first argument: remove
								if (arguments.size() == 0) {
									log.warn(getM()
											+ ": Inner class constructor invocation has no synthetic this reference as first argument! No arguments given.");
								} else if (!(arguments.get(0) instanceof ThisExpression)) {
									log.warn(getM()
											+ ": Inner class constructor invocation has no synthetic this reference as first argument! Wrong first argument: "
											+ arguments.get(0));
								} else {
									arguments.remove(0);
								}
							}
							wrapAddAll(((ClassInstanceCreation) expression).arguments(), arguments);
							// normally there was a DUP in advance, don't use:
							// basicBlock.pushExpression(classInstanceCreation);
							break;
						}
						log.warn(getM()
								+ ": Constructor expects expression class 'ThisExpression' or 'ClassInstanceCreation' but is '"
								+ expression.getClass() + "' with value: " + expression);
						break;
					}
					if (expression instanceof ThisExpression) {
						final SuperMethodInvocation superMethodInvocation = getAst()
								.newSuperMethodInvocation();
						superMethodInvocation.setName(newSimpleName(m.getName(), getAst()));
						wrapAddAll(superMethodInvocation.arguments(), arguments);
						methodExpression = superMethodInvocation;
					} else {
						// could be private method call in same object, nothing special in syntax
						final MethodInvocation methodInvocation = getAst().newMethodInvocation();
						methodInvocation.setExpression(wrap(expression, Priority.METHOD_CALL));
						methodInvocation.setName(newSimpleName(m.getName(), getAst()));
						wrapAddAll(methodInvocation.arguments(), arguments);
						methodExpression = methodInvocation;
					}
				} else if (m.isDynamic()) {
					final Object[] bsArgs = cop.getBsArgs();
					if (isLambdaBootstrapMethod(cop.getBsM()) && bsArgs != null
							&& bsArgs.length > 1 && bsArgs[1] instanceof M) {
						final M dynamicM = (M) bsArgs[1];
						if (dynamicM.isSynthetic()) {
							// is lambda
							final LambdaExpression lambdaExpression = getAst()
									.newLambdaExpression();
							// init lambda parameters
							final T[] paramTs = dynamicM.getParamTs();
							final A[][] paramAss = dynamicM.getParamAss();
							// first m.paramTs.length parameters are for outer capture inits
							for (int i = m.getParamTs().length; i < paramTs.length; ++i) {
								lambdaExpression.parameters().add(
										newSingleVariableDeclaration(dynamicM, paramTs, paramAss,
												i, this.cfg.getT()));
							}
							// init lambda body
							final CFG lambdaCfg = dynamicM.getCfg();
							assert lambdaCfg != null;
							if (lambdaCfg.getBlock() == null) {
								// if synthetics are not decompiled...
								// lambda methods are synthetic: init block, could later add more
								// checks and alternatives here if obfuscators play with these flags
								lambdaCfg.setBlock((Block) lambdaExpression.getBody());
								lambdaCfg.decompile();
							} else if (lambdaCfg.getBlock().getParent() instanceof MethodDeclaration) {
								// if synthetics are decompiled...but not for re-decompilation:
								// don't show this recognized (normally synthetic) method
								// declaration
								lambdaCfg.getBlock().delete(); // delete from parent
								dynamicM.setAstNode(null);
								// is our new lambda body
								lambdaExpression.setBody(lambdaCfg.getBlock());
							}
							methodExpression = lambdaExpression;
						} else {
							// is a method reference, 4 different variants possible
							if (dynamicM.isConstructor()) {
								final CreationReference methodReference = getAst()
										.newCreationReference();
								final T ownerT = dynamicM.getT();
								assert ownerT != null;
								methodReference.setType(newType(ownerT, getM()));
								methodExpression = methodReference;
							} else if (dynamicM.isStatic()) {
								final TypeMethodReference methodReference = getAst()
										.newTypeMethodReference();
								final T ownerT = dynamicM.getT();
								assert ownerT != null;
								methodReference.setType(newType(ownerT, getM()));
								methodReference
								.setName(newSimpleName(dynamicM.getName(), getAst()));
								methodExpression = methodReference;
							} else {
								assert arguments.size() == 1 : "expression method reference doesn't have 1 argument";

								final ExpressionMethodReference methodReference = getAst()
										.newExpressionMethodReference();
								methodReference.setExpression(arguments.get(0));
								methodReference
								.setName(newSimpleName(dynamicM.getName(), getAst()));
								methodExpression = methodReference;
							}
							// TODO is in bytecode via lambda, we could let it be or recognize this
							// pattern and shorten down to getAst().newSuperMethodReference();
						}
					} else {
						final MethodInvocation methodInvocation = getAst().newMethodInvocation();
						methodInvocation.setName(newSimpleName(m.getName(), getAst()));
						wrapAddAll(methodInvocation.arguments(), arguments);
						methodExpression = methodInvocation;
					}
				} else if (m.isStatic()) {
					final MethodInvocation methodInvocation = getAst().newMethodInvocation();
					methodInvocation.setExpression(newTypeName(m.getT(), getM()));
					methodInvocation.setName(newSimpleName(m.getName(), getAst()));
					wrapAddAll(methodInvocation.arguments(), arguments);
					methodExpression = methodInvocation;
				} else {
					if (rewriteStringAppend(bb, cop)) {
						break;
					}
					final MethodInvocation methodInvocation = getAst().newMethodInvocation();
					final Expression expression = bb.pop();
					// TODO need this for switch(this.ordinal) rewrites, delete later?
					// if (!(expression instanceof ThisExpression)) {
					methodInvocation.setExpression(wrap(expression, Priority.METHOD_CALL));
					// }
					methodInvocation.setName(newSimpleName(m.getName(), getAst()));
					wrapAddAll(methodInvocation.arguments(), arguments);
					methodExpression = methodInvocation;
				}
				setOp(methodExpression, op);
				final T returnT = m.getReturnT();
				if (returnT == T.VOID) {
					statement = setOp(getAst().newExpressionStatement(methodExpression), op);
					break;
				}
				assert methodExpression != null;
				bb.push(methodExpression);
				break;
			}
			case JCMP: {
				final JCMP cop = (JCMP) op;
				// invert all operators and switch out edge predicates
				final InfixExpression.Operator operator;
				switch (cop.getCmpType()) {
				case T_EQ:
					operator = InfixExpression.Operator.EQUALS;
					break;
				case T_GE:
					operator = InfixExpression.Operator.GREATER_EQUALS;
					break;
				case T_GT:
					operator = InfixExpression.Operator.GREATER;
					break;
				case T_LE:
					operator = InfixExpression.Operator.LESS_EQUALS;
					break;
				case T_LT:
					operator = InfixExpression.Operator.LESS;
					break;
				case T_NE:
					operator = InfixExpression.Operator.NOT_EQUALS;
					break;
				default:
					log.warn(getM() + ": Unknown cmp type '" + cop.getCmpType() + "'!");
					operator = null;
				}
				statement = getAst().newIfStatement();
				((IfStatement) statement).setExpression(newInfixExpressionPop(operator, bb, op));
				break;
			}
			case JCND: {
				final JCND cop = (JCND) op;
				Expression expression = bb.pop();
				// check preceding CMP
				if (expression instanceof InfixExpression
						&& ((InfixExpression) expression).getOperator() == InfixExpression.Operator.LESS_EQUALS) {
					// preceding compare expression (CMP result: -1 / 0 / 1)
					final InfixExpression.Operator operator;
					switch (cop.getCmpType()) {
					case T_EQ:
						operator = InfixExpression.Operator.EQUALS;
						break;
					case T_GE:
						operator = InfixExpression.Operator.GREATER_EQUALS;
						break;
					case T_GT:
						operator = InfixExpression.Operator.GREATER;
						break;
					case T_LE:
						operator = InfixExpression.Operator.LESS_EQUALS;
						break;
					case T_LT:
						operator = InfixExpression.Operator.LESS;
						break;
					case T_NE:
						operator = InfixExpression.Operator.NOT_EQUALS;
						break;
					default:
						log.warn(getM() + ": Unknown cmp type '" + cop.getCmpType() + "'!");
						operator = null;
					}
					((InfixExpression) expression).setOperator(operator);
				} else if (peekT(op).isRef()) {
					final InfixExpression.Operator operator;
					switch (cop.getCmpType()) {
					case T_EQ:
						operator = InfixExpression.Operator.EQUALS;
						break;
					case T_NE:
						operator = InfixExpression.Operator.NOT_EQUALS;
						break;
					default:
						log.warn(getM() + ": Unknown cmp type '" + cop.getCmpType()
								+ "' for null-expression!");
						operator = null;
					}
					expression = newInfixExpression(operator, expression,
							newLiteral(T.REF, null, getM(), op), op);
				} else if (peekT(op).is(T.BOOLEAN)) {
					// "!a" or "a == 0"?
					switch (cop.getCmpType()) {
					case T_EQ:
						// "== 0" means "is false"
						expression = not(expression);
						break;
					case T_NE:
						// "!= 0" means "is true"
						break;
					default:
						log.warn(getM() + ": Unknown cmp type '" + cop.getCmpType()
								+ "' for boolean expression '" + expression + "'!");
					}
				} else {
					final InfixExpression.Operator operator;
					switch (cop.getCmpType()) {
					case T_EQ:
						operator = InfixExpression.Operator.EQUALS;
						break;
					case T_GE:
						operator = InfixExpression.Operator.GREATER_EQUALS;
						break;
					case T_GT:
						operator = InfixExpression.Operator.GREATER;
						break;
					case T_LE:
						operator = InfixExpression.Operator.LESS_EQUALS;
						break;
					case T_LT:
						operator = InfixExpression.Operator.LESS;
						break;
					case T_NE:
						operator = InfixExpression.Operator.NOT_EQUALS;
						break;
					default:
						log.warn(getM() + ": Unknown cmp type '" + cop.getCmpType()
								+ "' for 0-expression!");
						operator = null;
					}
					expression = newInfixExpression(operator, expression,
							newLiteral(T.INT, 0, getM(), op), op);
				}
				statement = getAst().newIfStatement();
				((IfStatement) statement).setExpression(wrap(expression));
				break;
			}
			case JSR: {
				// <finally> till JVM 6 (50), we don't really push something at the sub BB, we
				// rewrite this or catch the stack underflow for the STORE
				break;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;
				/*
				 * final V v = getCfg().getFrameVar(cop.getReg(), cop.getPc()); if (v == null ||
				 * v.getName() == null) { // temporary local final Expression expression =
				 * bb.get(cop.getReg()); if (expression != null) { bb.push(bb.get(cop.getReg()));
				 * break; } }
				 */

				// must not access method parameters for fieldInits...
				fieldInitCheck: if (isFieldInit()) {
					if (!getM().isConstructor()) {
						setFieldInit(false);
						break fieldInitCheck;
					}
					final R r = getCfg().getInFrame(op).load(cop.getReg());
					if (r == null) {
						break fieldInitCheck;
					}
					if (!r.isMethodParam()) {
						setFieldInit(false);
						break fieldInitCheck;
					}
					if (cop.getReg() == 0) {
						break fieldInitCheck; // this
					}
					// only synthetic parameters are allowed
					final T ownerT = getM().getT();
					if (ownerT != null && ownerT.isInner()
							&& !getCfg().getCu().check(DFlag.IGNORE_CONSTRUCTOR_THIS)) {
						if (cop.getReg() == 1 && r.getT().is(getM().getParamTs()[0])) {
							break fieldInitCheck;
						}
					}
					setFieldInit(false);
				}
				bb.push(getVarExpression(cop.getReg(), cop.getPc(), op));
				break;
			}
			case MONITOR: {
				final MONITOR cop = (MONITOR) op;

				switch (cop.getKind()) {
				case ENTER: {
					final SynchronizedStatement synchronizedStatement = setOp(getAst()
							.newSynchronizedStatement(), op);
					synchronizedStatement.setExpression(wrap(bb.pop()));
					statement = synchronizedStatement;
					break;
				}
				case EXIT: {
					// for now: same as ENTER, blocks don't work here,
					// use getOp() to distinguish in control flow analysis
					final SynchronizedStatement synchronizedStatement = setOp(getAst()
							.newSynchronizedStatement(), op);
					synchronizedStatement.setExpression(wrap(bb.pop()));
					statement = synchronizedStatement;
					break;
				}
				default:
					log.warn(getM() + ": Unknown monitor kind '" + cop.getKind() + "'!");
				}
				break;
			}
			case MUL: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.TIMES, bb, op));
				break;
			}
			case NEG: {
				bb.push(newPrefixExpression(PrefixExpression.Operator.MINUS, bb.pop(), op));
				break;
			}
			case NEW: {
				final NEW cop = (NEW) op;

				final ClassInstanceCreation classInstanceCreation = setOp(getAst()
						.newClassInstanceCreation(), op);

				final String thisName = getCfg().getT().getName();
				final T newT = cop.getT();
				final String newName = newT.getName();
				// check for valid inner anonymous
				anonymous: if (newT.validateQualifierName(thisName)) {
					try {
						// anonymous classes typically use a number postfix, try it
						Integer.parseInt(newName.substring(thisName.length() + 1));

						if (newT.isDeclaration()) {
							// anonymous inner can only have a single interface
							// (with generic super "Object") or a super class
							final T[] interfaceTs = newT.getInterfaceTs();
							switch (interfaceTs.length) {
							case 0: {
								final T superT = newT.getSuperT();
								assert superT != null;
								classInstanceCreation.setType(newType(superT, getM()));
								break;
							}
							case 1: {
								final T interfaceT = interfaceTs[0];
								assert interfaceT != null;
								classInstanceCreation.setType(newType(interfaceT, getM()));
								break;
							}
							default:
								break anonymous;
							}
							final AnonymousClassDeclaration anonymousClassDeclaration = setOp(
									getAst().newAnonymousClassDeclaration(), op);
							newT.setAstNode(anonymousClassDeclaration);
							classInstanceCreation
							.setAnonymousClassDeclaration(anonymousClassDeclaration);
							bb.push(classInstanceCreation);
							break;
						}
					} catch (final NumberFormatException e) {
						// no int
					}
				}
				classInstanceCreation.setType(newType(newT, getM()));
				bb.push(classInstanceCreation);
				break;
			}
			case NEWARRAY: {
				final NEWARRAY cop = (NEWARRAY) op;
				final ArrayCreation arrayCreation = setOp(getAst().newArrayCreation(), op);
				arrayCreation.setType((ArrayType) newType(cop.getT(), getM()));
				for (int i = cop.getDimensions(); i-- > 0;) {
					arrayCreation.dimensions().add(wrap(bb.pop()));
				}
				bb.push(arrayCreation);
				break;
			}
			case OR: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.OR, bb, op));
				break;
			}
			case POP: {
				final POP cop = (POP) op;
				switch (cop.getKind()) {
				case POP2: {
					final Expression e = bb.pop();
					if (Expressions.isStatementExpression(e)) {
						statement = getAst().newExpressionStatement(wrap(e));
					} else if (e instanceof SimpleName) {
						// exception or simple literal
						break;
					} else if (isBoolean(e) && !(e instanceof BooleanLiteral)) {
						if (getCfg().getT().isAtLeast(Version.JVM_4)) {
							log.warn(getM() + ": Boolean expression POP in '" + cop
									+ "' for >= JVM 5 code! Rewriting to empty if.");
						}
						final IfStatement ifStatement = getAst().newIfStatement();
						ifStatement.setExpression(wrap(e));
						ifStatement.setThenStatement(getAst().newEmptyStatement());
						statement = ifStatement;
					}
					if (isWide(e)) {
						break;
					}
					// fall through for second pop iff none-wide
				}
				case POP: {
					final Expression e = bb.pop();
					if (Expressions.isStatementExpression(e)) {
						statement = getAst().newExpressionStatement(wrap(e));
					} else if (e instanceof SimpleName) {
						// exception or simple literal
						break;
					} else if (isBoolean(e) && !(e instanceof BooleanLiteral)) {
						if (getCfg().getT().isAtLeast(Version.JVM_4)) {
							log.warn(getM() + ": Boolean expression POP in '" + cop
									+ "' for >= JVM 5 code! Rewriting to empty if.");
						}
						final IfStatement ifStatement = getAst().newIfStatement();
						ifStatement.setExpression(wrap(e));
						ifStatement.setThenStatement(getAst().newEmptyStatement());
						statement = ifStatement;
					}
					break;
				}
				default:
					log.warn(getM() + ": Unknown POP type '" + cop.getKind() + "'!");
				}
				break;
			}
			case PUSH: {
				final PUSH cop = (PUSH) op;
				final T t = getCfg().getOutFrame(op).peek().getT();
				assert t != null;
				bb.push(newLiteral(t, cop.getValue(), getCfg().getM(), op));
				break;
			}
			case PUT: {
				final PUT cop = (PUT) op;
				final F f = cop.getF();
				final Expression rightOperand = bb.pop();
				assert rightOperand != null;
				if (rewriteFieldInit(bb, f, rightOperand)) {
					// was a constructor or initializer field init, done
					break;
				}
				Expression leftOperand;
				if (f.isStatic()) {
					leftOperand = getAst().newQualifiedName(newTypeName(f.getT(), getM()),
							newSimpleName(f.getName(), getAst()));
				} else {
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(bb.pop(), Priority.MEMBER_ACCESS));
					fieldAccess.setName(newSimpleName(f.getName(), getAst()));
					leftOperand = fieldAccess;
				}
				assert leftOperand != null;
				final Assignment assignment = newAssignment(Assignment.Operator.ASSIGN,
						leftOperand, rightOperand, op);
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				// inline assignment, DUP(_X1) -> PUT
				if (!bb.isStackEmpty() && bb.peek() == rightOperand) {
					bb.pop();
					bb.push(assignment);
					break;
				}
				if (!bb.isStackEmpty()
						&& rightOperand instanceof InfixExpression
						&& (((InfixExpression) rightOperand).getOperator() == InfixExpression.Operator.PLUS || ((InfixExpression) rightOperand)
						.getOperator() == InfixExpression.Operator.MINUS)) {
					// if i'm an peek-1 or peek+1 expression, than we can post-inc/dec
					// TODO more checks!
					bb.push(newPostfixExpression(
							((InfixExpression) rightOperand).getOperator() == InfixExpression.Operator.PLUS ? PostfixExpression.Operator.INCREMENT
									: PostfixExpression.Operator.DECREMENT, bb.pop(), op));
					break;
				}
				statement = getAst().newExpressionStatement(assignment);
				break;
			}
			case REM: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.REMAINDER, bb, op));
				break;
			}
			case RET: {
				// TODO
				break;
			}
			case RETURN: {
				final RETURN cop = (RETURN) op;
				final ReturnStatement returnStatement = getAst().newReturnStatement();
				if (cop.getT() != T.VOID) {
					returnStatement.setExpression(wrap(bb.pop()));
				}
				statement = returnStatement;
				break;
			}
			case SHL: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.LEFT_SHIFT, bb, op));
				break;
			}
			case SHR: {
				final SHR cop = (SHR) op;
				bb.push(newInfixExpressionPop(
						cop.isUnsigned() ? InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED
								: InfixExpression.Operator.RIGHT_SHIFT_SIGNED, bb, op));
				break;
			}
			case STORE: {
				final STORE cop = (STORE) op;

				final Expression rightOperand = bb.pop();
				final V v = getCfg().getFrameVar(cop.getReg(), cop.getPc() + 1);
				// check for wrapped temporary exception name
				if (rightOperand instanceof SimpleName && getOp(rightOperand) == null /* is exception */) {
					if (v != null) {
						((SimpleName) rightOperand).setIdentifier(v.getName());
					}
					break;
				}
				// inline assignment, DUP -> STORE
				final boolean isInlineAssignment = !bb.isStackEmpty() && bb.peek() == rightOperand;
				if (v == null /* tmp hack */|| v.getName() == null) {
					// temporary local
					// bb.set(cop.getReg(), rightOperand);
					// break;
					// TODO else not really necessary later if this is sure
				} else {
					// TODO if () int i = 0 else int i = 1 ???
					if (!isInlineAssignment && v.getPcs()[0] /* TODO */== cop.getPc() + 1) {
						final VariableDeclarationFragment variableDeclarationFragment = getAst()
								.newVariableDeclarationFragment();
						variableDeclarationFragment.setName(newSimpleName(v.getName(), getAst()));
						variableDeclarationFragment.setInitializer(wrap(rightOperand,
								Priority.ASSIGNMENT));
						final VariableDeclarationStatement variableDeclarationStatement = getAst()
								.newVariableDeclarationStatement(variableDeclarationFragment);
						variableDeclarationStatement.setType(newType(v.getT(), getM()));
						statement = variableDeclarationStatement;
						break;
					}
				}
				final Assignment assignment = newAssignment(Assignment.Operator.ASSIGN,
						getVarExpression(cop.getReg(), cop.getPc() + 1, op), rightOperand, op);
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				// inline assignment, DUP -> STORE
				if (isInlineAssignment) {
					bb.pop();
					bb.push(assignment);
					break;
				}
				statement = getAst().newExpressionStatement(assignment);
				break;
			}
			case SUB: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.MINUS, bb, op));
				break;
			}
			case SWAP: {
				final Expression e1 = bb.pop();
				final Expression e2 = bb.pop();
				bb.push(e1);
				bb.push(e2);
				break;
			}
			case SWITCH: {
				final SWITCH cop = (SWITCH) op;

				final Expression switchExpression = bb.pop();
				assert switchExpression != null;
				if (rewriteSwitchEnum(bb, cop, switchExpression)) {
					break;
				}
				if (rewriteSwitchString(bb, cop, switchExpression)) {
					break;
				}
				final T t = peekT(op);
				if (t == T.CHAR) {
					rewriteSwitchChar(bb);
				}
				final SwitchStatement switchStatement = getAst().newSwitchStatement();
				switchStatement.setExpression(wrap(switchExpression));
				statement = switchStatement;
				break;
			}
			case THROW: {
				final THROW cop = (THROW) op;
				final Expression exceptionExpression = bb.pop();
				assert exceptionExpression != null;
				if (rewriteAssertStatement(bb, cop, exceptionExpression)) {
					break;
				}
				final ThrowStatement throwStatement = getAst().newThrowStatement();
				throwStatement.setExpression(wrap(exceptionExpression));
				statement = throwStatement;
				break;
			}
			case XOR: {
				final Expression rightOperand = bb.pop();
				// "a ^ -1" => "~a"
				if (rightOperand instanceof NumberLiteral
						&& ((NumberLiteral) rightOperand).getToken().equals("-1")) {
					bb.push(newPrefixExpression(PrefixExpression.Operator.COMPLEMENT, bb.pop(), op));
					break;
				}
				// "a ^ true" => "!a" (found in JVM 1.2 boolean expressions)
				if (rightOperand instanceof BooleanLiteral
						&& getBooleanValue(rightOperand) == Boolean.TRUE) {
					bb.push(newPrefixExpression(PrefixExpression.Operator.NOT, bb.pop(), op));
					break;
				}
				bb.push(newInfixExpression(InfixExpression.Operator.XOR, bb.pop(), rightOperand, op));
				break;
			}
			default:
				throw new DecoJerException("Unknown intermediate vm operation '" + op + "'!");
			}
			if (statement != null) {
				bb.addStmt(setOp(statement, op));
			}
		}
		return true;
	}

	private AST getAst() {
		return getCfg().getCu().getAst();
	}

	@Nonnull
	private M getM() {
		return getCfg().getM();
	}

	@Nullable
	private R getR(@Nonnull final Expression e) {
		final Op op = getOp(e);
		if (op == null) {
			assert 0 == 1 : "expression operation '" + e + "' is unknown";
			return null;
		}
		final R r = getCfg().getOutFrame(op).peek();
		if (r == null) {
			assert 0 == 1 : "expression register '" + e + "' is unknown";
			return null;
		}
		return r;
	}

	@Nonnull
	private Expression getVarExpression(final int reg, final int pc, @Nonnull final Op op) {
		final String name = getVarName(reg, pc);
		if ("this".equals(name)) {
			return setOp(getAst().newThisExpression(), op);
		}
		return setOp(newSimpleName(name, getAst()), op);
	}

	private String getVarName(final int reg, final int pc) {
		final V v = getCfg().getFrameVar(reg, pc);
		final String name = v == null ? null : v.getName();
		if (name == null) {
			if (reg == 0 && !getM().isStatic()) {
				final Frame frame = getCfg().getFrame(pc);
				if (frame != null) {
					final R r = frame.load(reg);
					if (r != null && r.isMethodParam()) {
						// TODO later we move this to a real variable analysis
						return "this";
					}
				}
			}
			return "r" + reg;
		}
		return name;
	}

	private boolean isBoolean(@Nonnull final Expression e) {
		final R r = getR(e);
		if (r == null) {
			return false;
		}
		return r.getT().is(T.BOOLEAN);
	}

	/**
	 * Has BB necessary stack size for first operation?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - BB has necessary stack size for first operation
	 */
	private boolean isStackUnderflow(@Nonnull final BB bb) {
		final Op op = bb.getOp(0);
		final Frame inFrame = getCfg().getInFrame(op);
		if (inFrame == null) {
			assert false;
			return true;
		}
		return op.getInStackSize(inFrame) > bb.getTop();
	}

	private boolean isWide(@Nonnull final Expression e) {
		final R r = getR(e);
		if (r == null) {
			return false;
		}
		return r.isWide();
	}

	private T peekT(@Nonnull final Op op) {
		return getCfg().getInFrame(op).peek().getT();
	}

	private boolean rewriteAssertStatement(@Nonnull final BB bb, @Nonnull final THROW op,
			@Nonnull final Expression exceptionExpression) {
		if (getCfg().getCu().check(DFlag.IGNORE_ASSERT)) {
			return false;
		}
		// if (!DecTestAsserts.$assertionsDisabled && (l1 > 0L ? l1 >= l2 : l1 > l2))
		// throw new AssertionError("complex expression " + l1 - l2);
		if (!(exceptionExpression instanceof ClassInstanceCreation)) {
			return false;
		}
		final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) exceptionExpression;
		final Type type = classInstanceCreation.getType();
		if (!(type instanceof SimpleType)) {
			return false;
		}
		final Name name = ((SimpleType) type).getName();
		if (!(name instanceof SimpleName)) {
			return false;
		}
		if (!"AssertionError".equals(((SimpleName) name).getIdentifier())) {
			return false;
		}
		final Expression messageExpression;
		final List<Expression> arguments = classInstanceCreation.arguments();
		if (arguments.isEmpty()) {
			messageExpression = null;
		} else {
			if (arguments.size() > 1) {
				return false;
			}
			messageExpression = arguments.get(0);
		}
		final E in = bb.getRelevantIn();
		if (in == null) {
			return false;
		}
		final BB start = in.getStart();
		if (!start.isCond()) {
			return false;
		}
		final Statement ifStatement = start.getFinalStmt();
		assert ifStatement instanceof IfStatement; // start.isCond() is true
		Expression expression = ((IfStatement) ifStatement).getExpression();
		if (expression instanceof InfixExpression) {
			final InfixExpression infixExpression = (InfixExpression) expression;
			if (infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
				final Expression leftOperand = infixExpression.getLeftOperand();
				if (leftOperand instanceof QualifiedName
						&& leftOperand.toString().endsWith(".$assertionsDisabled")) {
					expression = infixExpression.getRightOperand();
				}
			}
		} else if (expression instanceof QualifiedName
				&& ((QualifiedName) expression).toString().endsWith(".$assertionsDisabled")) {
			expression = getAst().newBooleanLiteral(false);
		}
		assert expression != null;
		final E trueOut = start.getTrueOut();
		assert trueOut != null; // start.isCond() is true
		final BB trueSucc = trueOut.getEnd();
		final E falseOut = start.getFalseOut();
		assert falseOut != null; // start.isCond() is true
		final BB falseSucc = falseOut.getEnd();

		final boolean negated;
		final E assertOut;
		final BB okSucc;

		if (bb == falseSucc) {
			negated = false;
			assertOut = falseOut;
			okSucc = trueSucc;
		} else {
			negated = true;
			assertOut = trueOut;
			okSucc = falseSucc;
		}

		start.removeFinalStmt();
		final AssertStatement assertStatement = setOp(getAst().newAssertStatement(), op);
		assertStatement.setExpression(wrap(negated ? not(expression) : expression));
		if (messageExpression != null) {
			assertStatement.setMessage(wrap(messageExpression));
		}
		start.addStmt(assertStatement);

		if (okSucc.getIns().size() == 1) {
			okSucc.joinPredBb(start);
		} else {
			// false succ has additional incoming: don't join!
			// remove throw-BB and convert conditional out edge to sequence edge
			assertOut.remove();
			start.setSucc(okSucc); // okOut automatically removed here
		}
		return true;
	}

	private boolean rewriteBooleanCompound(@Nonnull final BB bb) {
		for (final E in : bb.getIns()) {
			// all following patterns have the following base form:
			// "A(final IfStmt) -> C(unique IfStmt) -> bb"
			final E c_bb = in.getRelevantIn();
			if (!c_bb.isCond()) {
				continue;
			}
			final BB c = c_bb.getStart();
			if (c.getStmts() != 1 || !c.isCond() || !c.isStackEmpty() || c.isPred(bb)) {
				continue;
			}
			final E a_c = c.getRelevantIn();
			if (a_c == null || !a_c.isCond()) {
				continue;
			}
			final BB a = a_c.getStart();
			if (!a.isCond() || a.isPred(c)) {
				continue;
			}
			// now we have the potential compound head, go down again and identify patterns
			final E c_bb2 = c_bb.isCondTrue() ? c.getFalseOut() : c.getTrueOut();
			if (c_bb2 == null) {
				assert false;
				continue;
			}
			final BB bb2 = c_bb2.getRelevantEnd();
			// condition wrong for org.objectweb.asm.ALLPerfTest.main(): last 2 conditions in loop
			// a=87 -> c=87 -> bb=92 and bb2=129 as loop head is combining end node
			// if (c.hasPred(bb2)) { continue; }
			final E a_x = a_c.isCondTrue() ? a.getFalseOut() : a.getTrueOut();
			if (a_x == null) {
				assert false;
				continue;
			}
			final BB x = a_x.getRelevantEnd();

			if (bb == x || bb2 == x) {
				// Match pattern from both sides (b2 is right here) because of multiple compounds!
				//
				// This is a short circuit compound, example is A || C:
				//
				// ...|.....
				// ...A.....
				// .t/.\f...
				// .|..C....
				// .|t/.\f../
				// .|/...\./
				// .B.....x. (bb and bb2 or vice versa! further incomings possible)
				// .|.....|.
				//
				// 4 combinations are possible for A -> B and C -> B:
				// - tt is || (see above)
				// - ft is ^||
				// - tf is ^&&
				// - ff is &&

				// rewrite AST
				final IfStatement ifStatement = (IfStatement) a.getFinalStmt();
				assert ifStatement != null;
				final Expression ifExpression = ifStatement.getExpression();
				assert ifExpression != null;
				final IfStatement rightIfStatement = (IfStatement) c.removeStmt(0);
				assert rightIfStatement != null;
				final Expression rightIfExpression = rightIfStatement.getExpression();
				assert rightIfExpression != null;
				final Op op = getOp(rightIfStatement);
				assert op != null;

				if (bb == x && c_bb.isCondTrue() || bb2 == x && c_bb2.isCondTrue() /* ?t */) {
					if (a_x.isCondTrue() /* tt */) {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_OR, ifExpression,
								rightIfExpression, op));
					} else {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_OR, not(ifExpression),
								rightIfExpression, op));
					}
				} else {
					if (a_x.isCondTrue() /* tf */) {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_AND, not(ifExpression),
								rightIfExpression, op));
					} else {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_AND, ifExpression,
								rightIfExpression, op));
					}
				}
				c.joinPredBb(a);
				return true;
			}
			if (x.getStmts() != 1 || !x.isCond() || !x.isStackEmpty() || x.isPred(bb)
					|| x.isPred(bb2)) {
				continue;
			}
			// check cross...
			E x_bb = x.getTrueOut();
			assert x_bb != null;
			if (x_bb.getRelevantEnd() == bb) {
				final E x_bb2 = x.getFalseOut();
				assert x_bb2 != null;
				if (x_bb2.getRelevantEnd() != bb2) {
					continue;
				}
			} else {
				if (x_bb.getRelevantEnd() != bb2) {
					continue;
				}
				x_bb = x.getFalseOut();
				assert x_bb != null;
				if (x_bb.getRelevantEnd() != bb) {
					continue;
				}
			}

			// This is a conditional compound (since JVM 4 with C/x is cond), example is A ? C : x:
			//
			// ...|...
			// ...A...
			// .t/.\f.
			// .C...x.
			// .|\./|.
			// t|.x.|f/
			// .|/.\|/
			// .B...b. (bb and bb2 or vice versa! further incomings possible)
			// .|...|.
			//
			// Reduction of none-flat CFG for forward-edges into flat CFG.
			// (There are other none-flat CFGs, e.g. String-Switch with hash collision.)

			// rewrite AST
			final IfStatement ifStatement = (IfStatement) a.getFinalStmt();
			assert ifStatement != null;

			Expression expression = ifStatement.getExpression();
			assert expression != null;
			Expression thenExpression;
			Expression elseExpression;

			final Statement cStmt = c.removeStmt(0);
			assert cStmt instanceof IfStatement;
			final Statement xStmt = x.removeStmt(0);
			assert xStmt instanceof IfStatement;

			if (c.isBefore(x)) {
				if (a_c.isCondFalse()) {
					expression = not(expression);
				}
				thenExpression = ((IfStatement) cStmt).getExpression();
				elseExpression = ((IfStatement) xStmt).getExpression();
				if (c_bb.isCondTrue() ^ x_bb.isCondTrue()) {
					// cross is true/false mix, for join we must inverse the none-join node x
					assert elseExpression != null;
					elseExpression = not(elseExpression);
				}
			} else { /* x is before c */
				if (a_x.isCondFalse()) {
					expression = not(expression);
				}
				thenExpression = ((IfStatement) xStmt).getExpression();
				elseExpression = ((IfStatement) cStmt).getExpression();
				if (c_bb.isCondTrue() ^ x_bb.isCondTrue()) {
					// cross is true/false mix, for join we must inverse the none-join node x
					assert thenExpression != null;
					thenExpression = not(thenExpression);
				}
			}
			assert elseExpression != null;
			assert thenExpression != null;
			// not from "expression", we need the full resulting expression type
			final ConditionalExpression conditionalExpression = setOp(getAst()
					.newConditionalExpression(), getOp(thenExpression));
			conditionalExpression.setExpression(wrap(expression, Priority.CONDITIONAL));
			conditionalExpression.setThenExpression(wrap(thenExpression, Priority.CONDITIONAL));
			conditionalExpression.setElseExpression(wrap(elseExpression, Priority.CONDITIONAL));
			ifStatement.setExpression(conditionalExpression);
			x.remove();
			c.joinPredBb(a);
			return true;
		}
		return false;
	}

	/**
	 * Class Literal Caching (no direct Constant Pool Class Literals before JVM 5).
	 *
	 * @param bb
	 *            BB
	 * @param op
	 *            GET operation
	 * @return {@code true} - rewritten
	 */
	private boolean rewriteCachedClassLiteral(@Nonnull final BB bb, @Nonnull final GET op) {
		// field-get for synthetic field which name starts with "class$" or "array$"
		// (is class$<[0-9]+> for Eclipse or class$<classname> for JDK)
		final F f = op.getF();
		if (!f.isStatic() || !f.isSynthetic()) {
			return false;
		}
		if (!f.getName().startsWith("class$") && !f.getName().startsWith("array$")) {
			return false;
		}
		if (getCfg().getT().isAtLeast(Version.JVM_5)) {
			log.warn(getM() + ": Class literal caching isn't necessary anymore in JVM 5!");
		}
		// now this really should be a cached class literal, giving warns in other cases are OK
		try {
			if (bb.getOps() == 1) {
				// JDK-Bytecode mode
				if (rewriteCachedClassLiteralJdk(bb, op)) {
					return true;
				}
			} else if (bb.getOps() == 2) {
				// Eclipse-Bytecode mode
				if (rewriteCachedClassLiteralEclipse(bb, op)) {
					return true;
				}
			}
			log.warn(getM() + ": Couldn't rewrite cached class literal '" + f + "'!");
			return false;
		} catch (final Exception e) {
			log.warn(getM() + ": Couldn't rewrite cached class literal '" + f + "'!", e);
			return false;
		}
	}

	private boolean rewriteCachedClassLiteralEclipse(@Nonnull final BB bb, @Nonnull final GET op) {
		// we are not very flexible here...the patterns are very special, but I don't know if more
		// general pattern matching is even possible, kind of none-decidable?

		// GET class$0 DUP JCND_NE
		// (_POP_ PUSH "typeLiteral" INVOKE Class.forName DUP PUT class$0 GOTO #)
		// #

		// ignore Exception-handling,
		// (see Eclipse 1.2 JDT: org.eclipse.jdt.core.JDTCompilerAdapter.execute())
		if (!(bb.getOp(0) instanceof DUP)) {
			return false;
		}
		if (!(bb.getOp(1) instanceof JCND)) {
			return false;
		}
		final E bbFalseOut = bb.getFalseOut();
		if (bbFalseOut == null) {
			assert false;
			return false;
		}
		final BB popBb = bbFalseOut.getEnd();
		if (popBb.getOps() != 1) {
			return false;
		}
		if (!(popBb.getOp(0) instanceof POP)) {
			return false;
		}
		final E popBbSequenceOut = popBb.getSequenceOut();
		if (popBbSequenceOut == null) {
			return false;
		}
		final BB pushBb = popBbSequenceOut.getEnd();
		if (pushBb.getOps() != 2) {
			return false;
		}
		if (!(pushBb.getOp(0) instanceof PUSH)) {
			return false;
		}
		if (!(pushBb.getOp(1) instanceof INVOKE)) {
			return false;
		}
		final E pushBbSequenceOut = pushBb.getSequenceOut();
		if (pushBbSequenceOut == null) {
			return false;
		}
		final BB dupBb = pushBbSequenceOut.getEnd();
		if (dupBb.getOps() != 3) {
			return false;
		}
		if (!(dupBb.getOp(0) instanceof DUP)) {
			return false;
		}
		if (!(dupBb.getOp(1) instanceof PUT)) {
			return false;
		}
		if (!(dupBb.getOp(2) instanceof GOTO)) {
			return false;
		}
		final E dupBbSequenceOut = dupBb.getSequenceOut();
		if (dupBbSequenceOut == null) {
			return false;
		}
		final BB followBb = dupBbSequenceOut.getEnd();
		final E bbTrueOut = bb.getTrueOut();
		if (bbTrueOut == null) {
			assert false;
			return false;
		}
		if (followBb != bbTrueOut.getEnd()) {
			return false;
		}
		// can just happen before JVM 5: replace . -> /
		final Object value = ((PUSH) pushBb.getOp(0)).getValue();
		if (!(value instanceof String)) {
			return false;
		}
		final String classInfo = ((String) value).replace('.', '/');
		assert classInfo != null;
		followBb.push(newLiteral(getCfg().getDu().getT(Class.class),
				getCfg().getDu().getT(classInfo), getM(), op));
		bb.removeOp(0);
		bb.removeOp(0);
		followBb.joinPredBb(bb);
		return true;
	}

	private boolean rewriteCachedClassLiteralJdk(@Nonnull final BB bb, @Nonnull final GET op) {
		// we are not very flexible here...the patterns are very special, but I don't know if more
		// general pattern matching is even possible, kind of none-decidable?

		// GET class$java$lang$String JCND_NE
		// (PUSH "typeLiteral" INVOKE Class.forName DUP PUT class$java$lang$String GOTO #)
		// GET class$java$lang$String #
		if (!(bb.getOp(0) instanceof JCND)) {
			return false;
		}
		// JVM 1 & 2 is EQ, JVM 3 & 4 is NE, since JVM 5 direct class literals
		final E initCacheOut = ((JCND) bb.getOp(0)).getCmpType() == CmpType.T_EQ ? bb.getTrueOut()
				: bb.getFalseOut();
		if (initCacheOut == null) {
			assert false;
			return false;
		}
		final BB pushBb = initCacheOut.getEnd();
		if (pushBb.getOps() != 4 && pushBb.getOps() != 5) {
			return false;
		}
		if (!(pushBb.getOp(0) instanceof PUSH)) {
			return false;
		}
		if (!(pushBb.getOp(1) instanceof INVOKE)) {
			return false;
		}
		if (!(pushBb.getOp(2) instanceof DUP)) {
			return false;
		}
		if (!(pushBb.getOp(3) instanceof PUT)) {
			return false;
		}
		if (pushBb.getOps() == 5 && !(pushBb.getOp(4) instanceof GOTO)) {
			// JVM 3 & 4
			return false;
		}

		final BB getBb = initCacheOut.isCondTrue() ? bb.getFalseSucc() : bb.getTrueSucc();
		assert getBb != null;
		if (getBb.getOps() != 1 && getBb.getOps() != 2) {
			return false;
		}
		if (!(getBb.getOp(0) instanceof GET)) {
			return false;
		}
		if (pushBb.getOps() == 2 && !(getBb.getOp(1) instanceof GOTO)) {
			// JVM 1 & 2
			return false;
		}
		final E getBbSequenceOut = getBb.getSequenceOut();
		if (getBbSequenceOut == null) {
			return false;
		}
		final BB followBb = getBbSequenceOut.getEnd();
		// can just happen before JVM 5: replace . -> /
		final Object value = ((PUSH) pushBb.getOp(0)).getValue();
		if (!(value instanceof String)) {
			return false;
		}
		final String classInfo = ((String) value).replace('.', '/');
		assert classInfo != null;
		followBb.push(newLiteral(getCfg().getDu().getT(Class.class),
				getCfg().getDu().getT(classInfo), getM(), op));
		bb.removeOp(0);
		followBb.joinPredBb(bb);
		return true;
	}

	private boolean rewriteConditionalConstants(@Nonnull final BB bb) {
		// in boolean compounds in JDK 1 & 2: PUSH true/false -> JCND,
		// also found in Scala
		assert bb.getOps() == 1 : bb.getOps();
		assert bb.isStackEmpty() : bb.getTop();

		final JCND opJcnd = (JCND) bb.getOp(0);
		final CmpType cmpType = opJcnd.getCmpType();
		final List<E> ins = bb.getIns();
		assert ins.size() > 1;

		for (int i = ins.size(); i-- > 0;) {
			final E in = ins.get(i).getRelevantIn();
			if (!in.isSequence()) {
				continue;
			}
			final BB pred = in.getStart();
			if (pred.isStackEmpty()) {
				continue;
			}
			Expression expression = pred.pop();
			Boolean booleanConst = getBooleanValue(expression);
			if (booleanConst != null) {
				switch (cmpType) {
				case T_EQ:
					// "== 0" means "is false"
					booleanConst = !booleanConst;
					break;
				case T_NE:
					// "!= 0" means "is true"
					break;
				default:
					log.warn(getM() + ": Unknown cmp type '" + cmpType + "'!");
				}
				final BB constantSucc = booleanConst ? bb.getTrueSucc() : bb.getFalseSucc();
				assert constantSucc != null;
				if (pred.getStmts() == 0 && pred.isStackEmpty()) {
					pred.moveIns(constantSucc);
				} else {
					pred.setSucc(constantSucc);
				}
				continue;
			}
			switch (cmpType) {
			case T_EQ:
				// "== 0" means "is false"
				expression = not(expression);
				break;
			case T_NE:
				// "!= 0" means "is true"
				break;
			default:
				log.warn(getM() + ": Unknown cmp type '" + cmpType + "' for boolean expression '"
						+ expression + "'!");
			}
			final IfStatement ifStatement = setOp(getAst().newIfStatement(), opJcnd);
			ifStatement.setExpression(wrap(expression));
			pred.addStmt(ifStatement);
			final BB trueSucc = bb.getTrueSucc();
			assert trueSucc != null;
			final BB falseSucc = bb.getFalseSucc();
			assert falseSucc != null;
			pred.setConds(trueSucc, falseSucc);
		}
		return ins.isEmpty();
	}

	private boolean rewriteConditionalReturn(@Nonnull final BB bb) {
		// return conditional value with additional statements in conditions,
		// rewriteConditionalValue() cannot work here, seen in Scala
		// TODO BB is NEW/NEW -> [INVOKE <init>(Lscala/Either;)V, RETURN]
		assert bb.getOps() == 1 : bb.getOps();
		assert bb.isStackEmpty() : bb.getTop();

		final RETURN opReturn = (RETURN) bb.getOp(0);
		final List<E> ins = bb.getIns();
		assert ins.size() > 1;

		for (int i = ins.size(); i-- > 0;) {
			final E in = ins.get(i).getRelevantIn();
			if (!in.isSequence()) {
				continue;
			}
			final BB pred = in.getStart();
			if (pred.isStackEmpty()) {
				continue;
			}
			final ReturnStatement cReturnStatement = setOp(getAst().newReturnStatement(), opReturn);
			cReturnStatement.setExpression(wrap(pred.pop()));
			pred.addStmt(cReturnStatement);
			in.remove();
		}
		return ins.isEmpty();
	}

	private boolean rewriteConditionalValue(@Nonnull final BB bb) {
		// Is this a conditional compound value, example is A ? C : x:
		//
		// ...|...
		// ...A...
		// .t/.\f.
		// .C...x.
		// ..\./..
		// ...B...
		// ...|...
		final List<E> ins = bb.getIns();
		if (ins.size() < 2) {
			// this has 3 preds: a == null ? 0 : a.length() == 0 ? 0 : 1
			// even more preds possible with boolean conditionals
			return false;
		}
		for (final E in : ins) {
			final E c_bb = in.getRelevantIn();
			final BB c = c_bb.getStart();
			// in Scala exists a more complex ternary variant with sub statements
			if (c.getTop() != 1 || c.getStmts() != 0 || c.getIns().size() != 1) {
				continue;
			}
			final E a_c = c.getRelevantIn();
			if (a_c == null) {
				assert false;
				continue;
			}
			final BB a = a_c.getStart();
			if (!a.isCond()) {
				continue;
			}
			// now we have the potential compound head, go down again and identify pattern
			final E a_x = a_c.isCondTrue() ? a.getFalseOut() : a.getTrueOut();
			if (a_x == null) {
				assert false;
				continue;
			}
			final BB x = a_x.getRelevantEnd();
			if (x.getTop() != 1 || x.getStmts() != 0 || x.getIns().size() != 1) {
				continue;
			}
			final E x_bb = x.getRelevantOut();
			if (x_bb == null) {
				assert false;
				continue;
			}
			if (x_bb.getEnd() != bb) {
				continue;
			}
			// is a conditional compound value
			Expression thenExpression = c.peek();
			Expression elseExpression = x.peek();
			final Statement aStmt = a.removeFinalStmt();
			if (!(aStmt instanceof IfStatement)) {
				assert false;
				continue;
			}
			Expression expression = ((IfStatement) aStmt).getExpression();
			if (expression == null) {
				assert false;
				continue;
			}
			final Boolean thenBooleanConst = getBooleanValue(thenExpression);
			final Boolean elseBooleanConst = getBooleanValue(elseExpression);
			rewrite: if (thenBooleanConst != null && elseBooleanConst != null) {
				// expression: A ? true : false => A,
				// accept if one is BooleanLiteral - merging didn't work ;)
				if (a_c.isCondTrue() ^ thenBooleanConst) {
					expression = not(expression);
				}
				// use one of the constants, so that we get the correct out type
				setOp(expression, getOp(thenExpression));
			} else {
				classLiteral: if (expression instanceof InfixExpression) {
					// Class-literals unknown in pre JVM 1.5 bytecode
					// (only primitive wrappers have constants like
					// getstatic java.lang.Void.TYPE : java.lang.Class)
					// ...construct Class-literals with synthetic local method:
					// static synthetic java.lang.Class class$(java.lang.String x0);
					// ...and cache this Class-literals in synthetic local fields:
					// static synthetic java.lang.Class class$java$lang$String;
					// static synthetic java.lang.Class array$$I;
					// resulting conditional code:
					// DecTestFields.array$$I != null ? DecTestFields.array$$I :
					// (DecTestFields.array$$I = DecTestFields.class$("[[I"))
					// ...convert too: int[][].class
					final InfixExpression equalsExpression = (InfixExpression) expression;
					if (!(equalsExpression.getRightOperand() instanceof NullLiteral)) {
						break classLiteral;
					}
					final Assignment assignment;
					if (equalsExpression.getOperator() == InfixExpression.Operator.EQUALS) {
						// JVM < 1.3
						if (!(thenExpression instanceof Assignment)) {
							break classLiteral;
						}
						assignment = (Assignment) thenExpression;
					} else if (equalsExpression.getOperator() == InfixExpression.Operator.NOT_EQUALS) {
						// JVM >= 1.3
						if (!(elseExpression instanceof Assignment)) {
							break classLiteral;
						}
						assignment = (Assignment) elseExpression;
					} else {
						break classLiteral;
					}
					if (!(assignment.getRightHandSide() instanceof MethodInvocation)) {
						break classLiteral;
					}
					final MethodInvocation methodInvocation = (MethodInvocation) assignment
							.getRightHandSide();
					if (!"class$".equals(methodInvocation.getName().getIdentifier())) {
						break classLiteral;
					}
					if (methodInvocation.arguments().size() != 1) {
						break classLiteral;
					}
					if (getCfg().getT().isAtLeast(Version.JVM_5)) {
						log.warn(getM()
								+ ": Unexpected class literal code with class$() in >= JVM 5 code!");
					}
					try {
						final String classInfo = ((StringLiteral) methodInvocation.arguments().get(
								0)).getLiteralValue();
						assert classInfo != null;
						expression = newLiteral(getCfg().getDu().getT(Class.class), getCfg()
								.getDu().getT(classInfo), getM(), getOp(equalsExpression));
						break rewrite;
					} catch (final Exception e) {
						// rewrite to class literal didn't work
					}
				}
				// use one of the constants, so that we get the correct out type
			final ConditionalExpression conditionalExpression = setOp(getAst()
					.newConditionalExpression(), getOp(thenExpression));
			if (!c.isBefore(x)) {
				final Expression swapExpression = thenExpression;
				thenExpression = elseExpression;
				elseExpression = swapExpression;
				if (a_c.isCondTrue()) {
					expression = not(expression);
				}
			} else if (a_c.isCondFalse()) {
				expression = not(expression);
			}
			conditionalExpression.setExpression(wrap(expression, Priority.CONDITIONAL));
			conditionalExpression.setThenExpression(wrap(thenExpression, Priority.CONDITIONAL));
			conditionalExpression.setElseExpression(wrap(elseExpression, Priority.CONDITIONAL));
			expression = conditionalExpression;
			}
			a.push(expression);
			a.setSucc(bb);
			c.remove();
			x.remove();
			return true;
		}
		return false;
	}

	/**
	 * Rewrite PUT-access to a class (static) or instance field.
	 *
	 * @param bb
	 *            current BB
	 * @param f
	 *            lass (static) or instance field
	 * @param rightOperand
	 *            right operand expression
	 * @return {@code true} - success
	 */
	private boolean rewriteFieldInit(@Nonnull final BB bb, @Nonnull final F f,
			@Nonnull final Expression rightOperand) {
		if (!isFieldInit()) {
			return false;
		}
		if (!f.isDeclaration()) {
			return false;
		}
		final T ownerT = getM().getT();
		if (ownerT == null) {
			return false;
		}
		if (!ownerT.is(f.getT())) {
			return false;
		}
		// set local field, could be initializer
		if (f.isStatic()) {
			if (!getM().isInitializer()) {
				return false;
			}
			if (getCfg().getStartBb() != bb || bb.getStmts() > 0) {
				return false;
			}
			if (f.getAstNode() == null) {
				// synthetic has been recognized before in TrJvmStruct2JavaAst, ignore assignment
				return true;
			}
			if (f.isEnum() && f.getT().isEnum() && !getCfg().getCu().check(DFlag.IGNORE_ENUM)) {
				// assignment to enum constant declaration
				if (!(rightOperand instanceof ClassInstanceCreation)) {
					log.warn(getM() + ": Enum field initialization for '" + f
							+ "' has no class instance creation as operand!");
					return false;
				}
				final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) rightOperand;
				// first two arguments must be String (== field name) and int (ordinal)
				final List<Expression> arguments = classInstanceCreation.arguments();
				if (arguments.size() < 2) {
					log.warn(getM() + ": Enum field initialization for '" + f
							+ "' has less than 2 arguments!");
					return false;
				}
				if (!(arguments.get(0) instanceof StringLiteral)) {
					log.warn(getM() + ": Enum field initialization for '" + f
							+ "' has no string literal as first parameter!");
					return false;
				}
				final String literalValue = ((StringLiteral) arguments.get(0)).getLiteralValue();
				if (!literalValue.equals(f.getName())) {
					log.warn(getM() + ": Enum field initialization for '" + f
							+ "' has no string literal equal to field name as first parameter!");
					return false;
				}
				if (!(arguments.get(1) instanceof NumberLiteral)) {
					log.warn(getM() + ": Enum field initialization for '" + f
							+ "' has no number literal as first parameter!");
					return false;
				}
				final Object astNode = f.getAstNode();
				if (!(astNode instanceof EnumConstantDeclaration)) {
					log.warn(getM() + ": Enum field initialization for '" + f
							+ "' has no EnumConstantDeclaration as AST node!");
					return false;
				}
				final EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) astNode;
				for (int i = arguments.size(); i-- > 2;) {
					final Expression e = arguments.get(i);
					e.delete();
					enumConstantDeclaration.arguments().add(0, e);
				}
				final AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation
						.getAnonymousClassDeclaration();
				if (anonymousClassDeclaration != null) {
					final Element declarationForNode = f.getT().getCu()
							.getDeclarationForNode(anonymousClassDeclaration);
					if (declarationForNode == null) {
						log.warn(getM() + ": Enum field initialization for '" + f
								+ "' with anonymous declaration has no declaration node!");
					} else {
						anonymousClassDeclaration.delete();
						enumConstantDeclaration
								.setAnonymousClassDeclaration(anonymousClassDeclaration);
						// normally contains one constructor, that calls a synthetic super
						// constructor with the enum class as additional last parameter,
						// this may contain field initializers, that we must keep,
						// so we can only remove the constructor in final merge (because
						// anonymous inner classes cannot have visible Java constructor)
						declarationForNode.setDeclarationOwner(f);
					}
				}
				return true;
			}
		} else {
			if (!getM().isConstructor()) {
				return false;
			}
			if (!(bb.peek() instanceof ThisExpression)) {
				return false;
			}
			if (getCfg().getStartBb() != bb || bb.getStmts() > 1 || bb.getStmts() == 1
					&& !(bb.getStmt(0) instanceof SuperConstructorInvocation)) {
				// initial super(<arguments>) is allowed for constructors
				return false;
			}
			if (f.getAstNode() == null) {
				// synthetic has been recognized before in TrJvmStruct2JavaAst, ignore assignment
				bb.pop();
				return true;
			}
			// multiple constructors with different signatures possible, all of them
			// contain the same field initializer code after super() - simply overwrite
		}
		final Object astNode = f.getAstNode();
		if (!(astNode instanceof FieldDeclaration)) {
			log.warn(getM() + ": Field initialization for '" + f
					+ "' has no FieldDeclaration as AST node!");
			return false;
		}
		((VariableDeclarationFragment) ((FieldDeclaration) astNode).fragments().get(0))
		.setInitializer(wrap(rightOperand, Priority.ASSIGNMENT));
		// TODO move anonymous TD to FD as child!!! important for ClassEditor
		// select, if fixed change ClassEditor#findDeclarationForJavaElement too
		if (!f.isStatic()) {
			bb.pop();
		}
		return true;
	}

	private boolean rewriteHandler(@Nonnull final BB bb) {
		if (!bb.isCatchHandler() || bb.getStmts() > 0 || bb.getTop() > 0) {
			// check stmts and top: some rewrites could combine join nodes and this would again be a
			// catch handler
			return false;
		}
		final SimpleName name = newSimpleName("e", getAst());
		bb.push(name);

		final T[] handlerTypes = (T[]) bb.getIns().get(0).getValue();
		assert handlerTypes != null && handlerTypes.length > 0;
		final T handlerType = handlerTypes[0];

		final TryStatement tryStatement = getAst().newTryStatement();
		if (handlerType != null) { // not "finally"
			final CatchClause catchClause = getAst().newCatchClause();
			final SingleVariableDeclaration singleVariableDeclaration = getAst()
					.newSingleVariableDeclaration();
			singleVariableDeclaration.setName(name);
			if (handlerTypes.length == 1) {
				singleVariableDeclaration.setType(newType(handlerType, getM()));
			} else {
				// Multi-Catch
				final UnionType unionType = getAst().newUnionType();
				for (final T t : handlerTypes) {
					assert t != null;
					unionType.types().add(newType(t, getM()));
				}
				singleVariableDeclaration.setType(unionType);
			}
			catchClause.setException(singleVariableDeclaration);
			tryStatement.catchClauses().add(catchClause);
		}
		bb.addStmt(tryStatement);
		return true;
	}

	private boolean rewriteInlinePostfixIncDec(@Nonnull final BB bb, @Nonnull final INC op) {
		if (bb.isStackEmpty()) {
			return false;
		}
		final Expression e = bb.peek();
		if (!(e instanceof SimpleName)) {
			return false;
		}
		final String name = getVarName(op.getReg(), op.getPc());
		if (!((SimpleName) e).getIdentifier().equals(name)) {
			return false;
		}
		bb.pop();
		bb.push(newPostfixExpression(op.getValue() == 1 ? PostfixExpression.Operator.INCREMENT
				: PostfixExpression.Operator.DECREMENT, newSimpleName(name, getAst()), op));
		return true;
	}

	private boolean rewriteInlinePrefixIncDec(@Nonnull final BB bb, @Nonnull final INC op) {
		if (bb.getOps() == 0) {
			return false;
		}
		final Op nextOp = bb.getOp(0);
		if (!(nextOp instanceof LOAD)) {
			return false;
		}
		if (((LOAD) nextOp).getReg() != op.getReg()) {
			return false;
		}
		bb.removeOp(0);
		bb.push(newPrefixExpression(op.getValue() == 1 ? PrefixExpression.Operator.INCREMENT
				: PrefixExpression.Operator.DECREMENT,
				getVarExpression(op.getReg(), op.getPc(), op), op));
		return true;
	}

	private boolean rewriteInlineRegAssignment(@Nonnull final BB bb, @Nonnull final INC op) {
		if (bb.getOps() == 0) {
			return false;
		}
		final Op nextOp = bb.getOp(0);
		if (!(nextOp instanceof LOAD)) {
			return false;
		}
		if (((LOAD) nextOp).getReg() != op.getReg()) {
			return false;
		}
		final int value = op.getValue();
		final Assignment assignment = newAssignment(value >= 0 ? Assignment.Operator.PLUS_ASSIGN
				: Assignment.Operator.MINUS_ASSIGN, getVarExpression(op.getReg(), op.getPc(), op),
				newLiteral(op.getT(), value >= 0 ? value : -value, getM(), op), op);
		bb.removeOp(0);
		bb.push(assignment);
		return true;
	}

	private boolean rewriteStringAppend(@Nonnull final BB bb, @Nonnull final INVOKE op) {
		// method-invoke for StringBuffer.toString() or StringBuilder.toString()

		// JVM 1.1.6:
		// new StringBuffer(String.valueOf(super.toString())).append(" TEST").toString()
		// JVM 1.3:
		// new StringBuffer().append(super.toString()).append(" TEST").toString();
		// JVM 1.5.0:
		// new StringBuilder().append(super.toString()).append(" TEST").toString()
		// Eclipse (constructor argument fail?):
		// new StringBuilder(String.valueOf(super.toString())).append(" TEST").toString()

		// ..."" at the beginning or end are handled very differently...

		final M m = op.getM();
		final T ownerT = m.getT();
		if (!"toString".equals(m.getName()) || ownerT == null || !ownerT.is(StringBuilder.class)
				&& !ownerT.is(StringBuffer.class)) {
			return false;
		}
		try {
			Expression stringExpression = null;
			Expression appendExpression = bb.peek();
			while (appendExpression instanceof MethodInvocation) {
				// no append at all could happen with in Eclipse: i + ""
				final MethodInvocation methodInvocation = (MethodInvocation) appendExpression;
				if (!"append".equals(methodInvocation.getName().getIdentifier())
						|| methodInvocation.arguments().size() != 1) {
					return false;
				}
				Expression appendArgumentExpression = (Expression) methodInvocation.arguments()
						.get(0);
				if (appendArgumentExpression == null) {
					assert false;
					return false;
				}
				// parentheses necessary for arithmetic add after string: "s" + (l1 + l2)
				// TODO System.out.println(((double) i + d) + s); ...must handle this here too for
				// JVM>=5 ...allready encountered append(String) here? must iterate from start?
				// where is the info in AST?
				if (Priority.priority(appendArgumentExpression) == Priority.ADD_SUB) {
					appendArgumentExpression = wrap(appendArgumentExpression, Priority.MULT_DIV_MOD);
				}
				if (stringExpression == null) {
					stringExpression = appendArgumentExpression;
				} else {
					stringExpression = newInfixExpression(InfixExpression.Operator.PLUS,
							appendArgumentExpression, stringExpression, op);
				}
				appendExpression = methodInvocation.getExpression();
			}
			if (!(appendExpression instanceof ClassInstanceCreation)) {
				return false;
			}
			final ClassInstanceCreation builder = (ClassInstanceCreation) appendExpression;
			// TODO "" + i isn't handled correctly!!! i+ "" too? cannot ignore this - conversion to
			// String necessary after all arithmetic ops
			if (!builder.arguments().isEmpty()) {
				if (builder.arguments().size() > 1) {
					return false;
				}
				Expression appendArgumentExpression = (Expression) builder.arguments().get(0);
				if (appendArgumentExpression instanceof MethodInvocation) {
					final MethodInvocation methodInvocation = (MethodInvocation) appendArgumentExpression;
					if (!"valueOf".equals(methodInvocation.getName().getIdentifier())
							|| methodInvocation.arguments().size() != 1) {
						return false;
					}
					final Expression methodExpression = methodInvocation.getExpression();
					if (!(methodExpression instanceof SimpleName)
							|| !"String".equals(((SimpleName) methodExpression).getIdentifier())) {
						return false;
					}
					appendArgumentExpression = (Expression) methodInvocation.arguments().get(0);
				}
				if (appendArgumentExpression == null) {
					assert false;
					return false;
				}
				// OK, no parentheses necessary for arithmetic add before string: l1 + l2 + "s"
				if (stringExpression != null) {
					stringExpression = newInfixExpression(InfixExpression.Operator.PLUS,
							appendArgumentExpression, stringExpression, op);
				} else {
					final StringLiteral stringLiteral = getAst().newStringLiteral();
					assert stringLiteral != null;
					stringExpression = newInfixExpression(InfixExpression.Operator.PLUS,
							appendArgumentExpression, stringLiteral, op);
				}
			}
			bb.pop();
			assert stringExpression != null;
			bb.push(stringExpression);
			return true;
		} catch (final Exception e) {
			log.warn(getM() + ": Rewrite to string-add didn't work!", e);
			return false;
		}
	}

	private boolean rewriteSwitchEnum(@Nonnull final BB bb, @Nonnull final SWITCH op,
			@Nonnull final Expression switchExpression) {
		if (getCfg().getCu().check(DFlag.IGNORE_ENUM)) {
			return false;
		}
		// we shouldn't do this earlier at the operations level because the switchExpression could
		// be very complex
		if (!(switchExpression instanceof ArrayAccess)) {
			return false;
		}
		final ArrayAccess arrayAccess = (ArrayAccess) switchExpression;
		// check index expression: ordinal() : int
		final Expression indexExpression = arrayAccess.getIndex();
		if (!(indexExpression instanceof MethodInvocation)) {
			return false;
		}
		final MethodInvocation ordinalMethodInvocation = (MethodInvocation) indexExpression;
		final Op ordinalMOp = getOp(ordinalMethodInvocation);
		if (!(ordinalMOp instanceof INVOKE)) {
			assert false;
			return false;
		}
		final M ordinalM = ((INVOKE) ordinalMOp).getM();
		final T ordinalOwnerT = ordinalM.getT();
		if (!"ordinal".equals(ordinalM.getName()) || !"()I".equals(ordinalM.getDescriptor())
				|| ordinalOwnerT == null) {
			return false;
		}
		final Expression enumSwitchExpression = ordinalMethodInvocation.getExpression();
		if (enumSwitchExpression == null) {
			assert false;
			return false;
		}
		// extract index 2 enumeration map
		final Map<Integer, F> index2enum;

		// check array expression: GET (JDK) or INVOKE (Eclipse)
		final Expression arrayExpression = arrayAccess.getArray();
		final Op arrayOp = getOp(arrayExpression);
		if (arrayExpression instanceof QualifiedName && arrayOp instanceof GET) {
			// JDK-Bytecode mode: map in different class file - or general in a field with
			// static initializer
			final F arrayF = ((GET) arrayOp).getF();
			final Container declarationOwner = arrayF.getDeclarationOwner();
			if (declarationOwner == null) {
				return false;
			}
			M initializer = null;
			for (final Element declaration : declarationOwner.getDeclarations()) {
				if (!(declaration instanceof M)) {
					continue;
				}
				final M m = (M) declaration;
				if (m.isInitializer()) {
					initializer = m;
					break;
				}
			}
			if (initializer == null) {
				return false;
			}
			index2enum = SwitchTypes.extractIndex2enum(initializer, ordinalOwnerT);
		} else if (arrayExpression instanceof MethodInvocation && arrayOp instanceof INVOKE) {
			// Eclipse-Bytecode mode: map in same class file - or general in a function
			final M arrayM = ((INVOKE) arrayOp).getM();
			index2enum = SwitchTypes.extractIndex2enum(arrayM, ordinalOwnerT);
		} else {
			return false;
		}
		if (!SwitchTypes.rewriteCaseValues(bb, index2enum)) {
			return false;
		}
		if (getCfg().getT().isBelow(Version.JVM_5)) {
			log.warn(getM()
					+ ": Enumerations switches are not known before JVM 5! Rewriting anyway, check this.");
		}
		final SwitchStatement switchStatement = setOp(getAst().newSwitchStatement(), op);
		switchStatement.setExpression(wrap(enumSwitchExpression));
		bb.addStmt(switchStatement);
		return true;
	}

	private boolean rewriteSwitchString(@Nonnull final BB bb, @Nonnull final SWITCH op,
			@Nonnull final Expression switchExpression) {
		// we shouldn't do this earlier at the operations level because the switchExpression could
		// be very complex (may be possible here because of single stack entry before patterns)
		if (!(switchExpression instanceof MethodInvocation)) {
			return false;
		}
		final MethodInvocation hashMethodInvocation = (MethodInvocation) switchExpression;
		final Op invokeOp = getOp(hashMethodInvocation);
		if (!(invokeOp instanceof INVOKE)) {
			assert false;
			return false;
		}
		final M hashM = ((INVOKE) invokeOp).getM();
		if (!"hashCode".equals(hashM.getName()) || !"()I".equals(hashM.getDescriptor())) {
			return false;
		}
		Expression stringSwitchExpression = hashMethodInvocation.getExpression();
		if (stringSwitchExpression == null) {
			assert false;
			return false;
		}
		final E switchDefaultOut = bb.getSwitchDefaultOut();
		if (switchDefaultOut == null) {
			assert false;
			return false;
		}
		final BB defaultCase = switchDefaultOut.getRelevantEnd();
		// we are not very flexible here...the patterns are very special, but I don't know if more
		// general pattern matching is even possible, kind of none-decidable?
		// obfuscators or different compilers could currently easily sabotage this method...
		if (stringSwitchExpression instanceof SimpleName) {
			// switch (r<x>)
			// JDK-Bytecode mode: combination of 2 switches, first switch assigns to index
			final Op loadOp = getOp(stringSwitchExpression);
			if (!(loadOp instanceof LOAD)) {
				assert false;
				return false;
			}
			final int tmpReg = ((LOAD) loadOp).getReg();
			if (bb.getStmts() < 2) {
				assert false;
				return false;
			}
			// extract second statement: r<y> = -1
			final Expression indexAssignment = bb.getExpression(bb.getStmts() - 1);
			if (!(indexAssignment instanceof Assignment)) {
				assert false;
				return false;
			}
			final Expression indexRightHandSide = ((Assignment) indexAssignment).getRightHandSide();
			if (!(indexRightHandSide instanceof NumberLiteral)) {
				assert false;
				return false;
			}
			if (!"-1".equals(((NumberLiteral) indexRightHandSide).getToken())) {
				assert false;
				return false;
			}
			final Expression indexLeftHandSide = ((Assignment) indexAssignment).getLeftHandSide();
			if (indexLeftHandSide == null) {
				assert false;
				return false;
			}
			final Op indexStoreOp = getOp(indexLeftHandSide);
			if (!(indexStoreOp instanceof STORE)) {
				assert false;
				return false;
			}
			final int indexReg = ((STORE) indexStoreOp).getReg();
			// extract first statement: r<x> = stringSwitchExpression
			final Expression assignment = bb.getExpression(bb.getStmts() - 2);
			if (!(assignment instanceof Assignment)) {
				assert false;
				return false;
			}
			final Expression leftHandSide = ((Assignment) assignment).getLeftHandSide();
			if (leftHandSide == null) {
				assert false;
				return false;
			}
			final Op storeOp = getOp(leftHandSide);
			if (!(storeOp instanceof STORE)) {
				assert false;
				return false;
			}
			if (((STORE) storeOp).getReg() != tmpReg) {
				return false;
			}
			stringSwitchExpression = ((Assignment) assignment).getRightHandSide();
			if (stringSwitchExpression == null) {
				assert false;
				return false;
			}
			// extract string case values
			final Map<String, BB> string2bb = SwitchTypes.extractString2bb(bb, tmpReg, defaultCase);
			if (string2bb == null) {
				return false;
			}
			// rewrite switch
			final Map<Integer, String> index2string = SwitchTypes.extractIndex2string(string2bb,
					indexReg, defaultCase);
			if (index2string == null) {
				SwitchTypes.rewriteCaseStrings(bb, string2bb, defaultCase);
				final SwitchStatement switchStatement = setOp(getAst().newSwitchStatement(), op);
				switchStatement.setExpression(wrap(stringSwitchExpression));
				bb.removeFinalStmt(); // r<y> = -1
				bb.removeFinalStmt(); // r<x> = stringSwitchExpression
				bb.addStmt(switchStatement);
			} else {
				if (!SwitchTypes.rewriteCaseValues(defaultCase, index2string)) {
					return false;
				}
				final SwitchStatement switchStatement = setOp(getAst().newSwitchStatement(), op);
				switchStatement.setExpression(wrap(stringSwitchExpression));
				bb.removeFinalStmt(); // r<y> = -1
				bb.removeFinalStmt(); // r<x> = stringSwitchExpression
				defaultCase.removeOp(0); // LOAD y
				defaultCase.removeOp(0); // second SWITCH
				defaultCase.joinPredBb(bb);
				defaultCase.addStmt(switchStatement);
			}
			if (getCfg().getT().isBelow(Version.JVM_7)) {
				log.warn(getM()
						+ ": String switches are not known before JVM 7! Rewriting anyway, check this.");
			}
			return true;
		}
		if (stringSwitchExpression instanceof ParenthesizedExpression) {
			// switch((r<x>=stringSwitchExpression).hashCode())
			// more compact Eclipse-Bytecode mode: one switch
			final Expression assignment = ((ParenthesizedExpression) stringSwitchExpression)
					.getExpression();
			if (!(assignment instanceof Assignment)) {
				assert false;
				return false;
			}
			// extract left assignment: r<x>, tmpReg: x
			final Expression leftHandSide = ((Assignment) assignment).getLeftHandSide();
			if (leftHandSide == null) {
				assert false;
				return false;
			}
			final Op storeOp = getOp(leftHandSide);
			if (!(storeOp instanceof STORE)) {
				assert false;
				return false;
			}
			final int tmpReg = ((STORE) storeOp).getReg();
			// extract right assignment: stringSwitchExpression
			stringSwitchExpression = ((Assignment) assignment).getRightHandSide();
			if (stringSwitchExpression == null) {
				assert false;
				return false;
			}
			// extract string case values
			final Map<String, BB> string2bb = SwitchTypes.extractString2bb(bb, tmpReg, defaultCase);
			if (string2bb == null) {
				return false;
			}
			// rewrite switch
			SwitchTypes.rewriteCaseStrings(bb, string2bb, defaultCase);
			final SwitchStatement switchStatement = setOp(getAst().newSwitchStatement(), op);
			switchStatement.setExpression(wrap(stringSwitchExpression));
			bb.addStmt(switchStatement);

			if (getCfg().getT().isBelow(Version.JVM_7)) {
				log.warn(getM()
						+ ": String switches are not known before JVM 7! Rewriting anyway, check this.");
			}
			return true;
		}
		return false;
	}

	private void transform() {
		setFieldInit(getM().isConstructor() || getM().isInitializer());
		final List<BB> bbs = getCfg().getPostorderedBbs();
		// for all nodes in _reverse_ postorder: is also backward possible with nice optimizations,
		// but this way easier handling of dalvik temporary registers
		for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			if (bb == null) {
				// can happen if BB deleted through rewrite
				continue;
			}
			if (!rewriteHandler(bb)) {
				// handler BB cannot match following patterns
				while (rewriteBooleanCompound(bb)) {
					// nested possible
				}
			}
			// previous expressions merged into bb, now rewrite
			if (!convertToHLLIntermediate(bb)) {
				// in Java should never happen in forward mode, but in Scala exists a more complex
				// conditional value ternary variant with sub statements
				log.warn(getM() + ": Stack underflow in '" + getCfg() + "':\n" + bb);
				assert false : getM() + ": Stack underflow in '" + getCfg() + "':\n" + bb;
			}
			// remove empty nodes
			if (bb.getStmts() == 0 && bb.getTop() == 0) {
				final E sequenceOut = bb.getSequenceOut();
				if (sequenceOut != null) {
					bb.moveIns(sequenceOut.getEnd());
				}
			}
		}
	}

}