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

import static org.decojer.cavaj.utils.Expressions.newInfixExpression;
import static org.decojer.cavaj.utils.Expressions.newPrefixExpression;
import static org.decojer.cavaj.utils.Expressions.wrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.ops.ADD;
import org.decojer.cavaj.model.code.ops.ALOAD;
import org.decojer.cavaj.model.code.ops.AND;
import org.decojer.cavaj.model.code.ops.ARRAYLENGTH;
import org.decojer.cavaj.model.code.ops.ASTORE;
import org.decojer.cavaj.model.code.ops.CAST;
import org.decojer.cavaj.model.code.ops.CMP;
import org.decojer.cavaj.model.code.ops.DIV;
import org.decojer.cavaj.model.code.ops.DUP;
import org.decojer.cavaj.model.code.ops.FILLARRAY;
import org.decojer.cavaj.model.code.ops.GET;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.INC;
import org.decojer.cavaj.model.code.ops.INSTANCEOF;
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
import org.decojer.cavaj.model.code.ops.RETURN;
import org.decojer.cavaj.model.code.ops.SHL;
import org.decojer.cavaj.model.code.ops.SHR;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.code.ops.SUB;
import org.decojer.cavaj.model.code.ops.SWAP;
import org.decojer.cavaj.model.code.ops.SWITCH;
import org.decojer.cavaj.model.code.ops.THROW;
import org.decojer.cavaj.model.code.ops.XOR;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.Priority;
import org.decojer.cavaj.utils.Types;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Transformer: CFG to Java Expression Statements ASTs.
 * 
 * @author André Pankraz
 */
public final class TrCfg2JavaExpressionStmts {

	private final static Logger LOGGER = Logger
			.getLogger(TrCfg2JavaExpressionStmts.class.getName());

	private static boolean rewriteShortCircuitCompound(final BB bb) {
		if (bb.getIns().size() != 1) {
			return false;
		}
		// must be single if statement for short-circuit compound boolean expression structure
		if (bb.getStmts() != 1 || !bb.isFinalStmtCond()) {
			return false;
		}
		final BB trueSucc = bb.getTrueSucc();
		final BB falseSucc = bb.getFalseSucc();

		final BB pred = bb.getIns().get(0).getStart();
		if (!pred.isFinalStmtCond()) {
			return false;
		}
		final BB predTrueSucc = pred.getTrueSucc();
		final BB predFalseSucc = pred.getFalseSucc();

		if (predTrueSucc == bb) {
			if (predFalseSucc == trueSucc) {
				// !A || B

				// ....|..
				// ....A..
				// ..t/.\f
				// ...B..|
				// .f/.\t|
				// ./...\|
				// F.....T

				// rewrite AST
				final Expression leftExpression = ((IfStatement) pred.getFinalStmt())
						.getExpression();
				final Expression rightExpression = ((IfStatement) bb.getStmt(0)).getExpression();
				((IfStatement) pred.getFinalStmt()).setExpression(newInfixExpression(
						InfixExpression.Operator.CONDITIONAL_OR, rightExpression,
						newPrefixExpression(PrefixExpression.Operator.NOT, leftExpression)));
				// rewrite CFG
				bb.removeStmt(0);
				bb.copyContentFrom(pred);
				pred.moveIns(bb);
				pred.remove();
				return true;
			}
			if (predFalseSucc == falseSucc) {
				// A && B

				// ..|....
				// ..A....
				// f/.\t..
				// |..B...
				// |f/.\t.
				// |/...\.
				// F.....T

				// rewrite AST
				final Expression leftExpression = ((IfStatement) pred.getFinalStmt())
						.getExpression();
				final Expression rightExpression = ((IfStatement) bb.getStmt(0)).getExpression();
				((IfStatement) pred.getFinalStmt()).setExpression(newInfixExpression(
						InfixExpression.Operator.CONDITIONAL_AND, rightExpression, leftExpression));
				// rewrite CFG
				bb.removeStmt(0);
				bb.copyContentFrom(pred);
				pred.moveIns(bb);
				pred.remove();
				return true;
			}
		} else if (predFalseSucc == bb) {
			if (predTrueSucc == trueSucc) {
				// A || B

				// ....|..
				// ....A..
				// ..f/.\t
				// ...B..|
				// .f/.\t|
				// ./...\|
				// F.....T

				// rewrite AST
				final Expression leftExpression = ((IfStatement) pred.getFinalStmt())
						.getExpression();
				final Expression rightExpression = ((IfStatement) bb.getStmt(0)).getExpression();
				((IfStatement) pred.getFinalStmt()).setExpression(newInfixExpression(
						InfixExpression.Operator.CONDITIONAL_OR, rightExpression, leftExpression));
				// rewrite CFG
				bb.removeStmt(0);
				bb.copyContentFrom(pred);
				pred.moveIns(bb);
				pred.remove();
				return true;
			}
			if (predTrueSucc == falseSucc) {
				// !A && B

				// ..|....
				// ..A....
				// t/.\f..
				// |..B...
				// |f/.\t.
				// |/...\.
				// F.....T

				// rewrite AST
				final Expression leftExpression = ((IfStatement) pred.getFinalStmt())
						.getExpression();
				final Expression rightExpression = ((IfStatement) bb.getStmt(0)).getExpression();
				((IfStatement) pred.getFinalStmt()).setExpression(newInfixExpression(
						InfixExpression.Operator.CONDITIONAL_AND, rightExpression,
						newPrefixExpression(PrefixExpression.Operator.NOT, leftExpression)));
				// rewrite CFG
				bb.removeStmt(0);
				bb.copyContentFrom(pred);
				pred.moveIns(bb);
				pred.remove();
				return true;
			}
		}
		return false;
	}

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrCfg2JavaExpressionStmts(cfg).transform();
	}

	private final CFG cfg;

	private TrCfg2JavaExpressionStmts(final CFG cfg) {
		this.cfg = cfg;
	}

	private boolean convertToHLLIntermediate(final BB bb) {
		while (bb.getOps() > 0) {
			final Op op = bb.getOp(0);
			if (!bb.hasStackSizeFor(op)) {
				return false;
			}
			bb.removeOp(0);
			Statement statement = null;
			switch (op.getOptype()) {
			case ADD: {
				assert op instanceof ADD;

				bb.push(newInfixExpression(InfixExpression.Operator.PLUS, bb.pop(), bb.pop()));
				break;
			}
			case ALOAD: {
				assert op instanceof ALOAD;

				final ArrayAccess arrayAccess = getAst().newArrayAccess();
				arrayAccess.setIndex(wrap(bb.pop()));
				arrayAccess.setArray(wrap(bb.pop(), Priority.ARRAY_INDEX));
				bb.push(arrayAccess);
				break;
			}
			case AND: {
				assert op instanceof AND;

				bb.push(newInfixExpression(InfixExpression.Operator.AND, bb.pop(), bb.pop()));
				break;
			}
			case ARRAYLENGTH: {
				assert op instanceof ARRAYLENGTH;

				final Expression expression = bb.pop();
				if (expression instanceof Name) {
					// annotationsVisible.length
					bb.push(getAst().newQualifiedName((Name) wrap(expression),
							getAst().newSimpleName("length")));
				} else {
					// FieldAccess or MethodInvocation:
					// this.code.length, getInterfaces().length
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(expression, Priority.MEMBER_ACCESS));
					fieldAccess.setName(getAst().newSimpleName("length"));
					bb.push(fieldAccess);
				}
				break;
			}
			case ASTORE: {
				assert op instanceof ASTORE;

				final Expression rightExpression = bb.pop();
				final Expression indexExpression = bb.pop();
				final Expression arrayRefExpression = bb.pop();
				if (arrayRefExpression instanceof ArrayCreation) {
					final ArrayCreation arrayCreation = (ArrayCreation) arrayRefExpression;
					ArrayInitializer arrayInitializer = arrayCreation.getInitializer();
					if (arrayInitializer == null) {
						arrayInitializer = getAst().newArrayInitializer();
						arrayCreation.setInitializer(arrayInitializer);
						// TODO for higher performance and for full array creation removement we
						// could defer the 0-fill and rewrite to the final A/STORE phase
						final int size = Integer.parseInt(((NumberLiteral) arrayCreation
								.dimensions().get(0)).getToken());
						// not all indexes may be set, null/0/false in JDK 7 are not set, fill
						for (int i = size; i-- > 0;) {
							arrayInitializer.expressions().add(
									Types.convertLiteral(bb.getCfg().getInFrame(op).peek().getT(),
											null, this.cfg.getTd()));
						}
						arrayCreation.dimensions().clear();
					}
					final int index = Integer
							.parseInt(((NumberLiteral) indexExpression).getToken());
					arrayInitializer.expressions().set(index, wrap(rightExpression));
					break;
				}
				final ArrayAccess arrayAccess = getAst().newArrayAccess();
				arrayAccess.setArray(wrap(arrayRefExpression, Priority.ARRAY_INDEX));
				arrayAccess.setIndex(wrap(indexExpression));
				final Assignment assignment = getAst().newAssignment();
				assignment.setLeftHandSide(arrayAccess);
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				assignment.setRightHandSide(wrap(rightExpression, Priority.ASSIGNMENT));
				// inline assignment, DUP(_X1) -> PUT
				if (bb.getTop() > 0 && bb.peek() == rightExpression) {
					bb.pop();
					bb.push(assignment);
				} else {
					statement = getAst().newExpressionStatement(assignment);
				}
				break;
			}
			case CAST: {
				final CAST cop = (CAST) op;
				final CastExpression castExpression = getAst().newCastExpression();
				castExpression.setType(Types.convertType(cop.getToT(), this.cfg.getTd()));
				castExpression.setExpression(wrap(bb.pop(), Priority.TYPE_CAST));
				bb.push(castExpression);
				break;
			}
			case CMP: {
				assert op instanceof CMP;

				// pseudo expression for following JCND, not really the correct
				// answer for -1, 0, 1
				bb.push(newInfixExpression(InfixExpression.Operator.LESS_EQUALS, bb.pop(), bb.pop()));
				break;
			}
			case DIV: {
				assert op instanceof DIV;

				bb.push(newInfixExpression(InfixExpression.Operator.DIVIDE, bb.pop(), bb.pop()));
				break;
			}
			case DUP: {
				final DUP cop = (DUP) op;
				switch (cop.getKind()) {
				case DUP2:
					if (!isWide(cop)) {
						final Expression e1 = bb.pop();
						final Expression e2 = bb.pop();
						bb.push(e2);
						bb.push(e1);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					// fall through for wide
				case DUP:
					bb.push(bb.peek());
					break;
				case DUP2_X1:
					if (!isWide(cop)) {
						final Expression e1 = bb.pop();
						final Expression e2 = bb.pop();
						final Expression e3 = bb.pop();
						bb.push(e2);
						bb.push(e1);
						bb.push(e3);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					// fall through for wide
				case DUP_X1: {
					final Expression e1 = bb.pop();
					final Expression e2 = bb.pop();
					bb.push(e1);
					bb.push(e2);
					bb.push(e1);
					break;
				}
				case DUP2_X2:
					if (!isWide(cop)) {
						final Expression e1 = bb.pop();
						final Expression e2 = bb.pop();
						final Expression e3 = bb.pop();
						final Expression e4 = bb.pop();
						bb.push(e2);
						bb.push(e1);
						bb.push(e4);
						bb.push(e3);
						bb.push(e2);
						bb.push(e1);
						break;
					}
					// fall through for wide
				case DUP_X2: {
					final Expression e1 = bb.pop();
					final Expression e2 = bb.pop();
					final Expression e3 = bb.pop();
					bb.push(e1);
					bb.push(e3);
					bb.push(e2);
					bb.push(e1);
					break;
				}
				default:
					LOGGER.warning("Unknown DUP type '" + cop.getKind() + "'!");
				}
				break;
			}
			case FILLARRAY: {
				final FILLARRAY cop = (FILLARRAY) op;

				final T t = this.cfg.getInFrame(op).peek().getT();
				final T componentT = t.getComponentT();

				Expression expression = bb.pop();
				if (!(expression instanceof ArrayCreation)) {
					// TODO Dalvik...assignment happened already...temporary register
					expression = getAst().newArrayCreation();
					((ArrayCreation) expression).setType((ArrayType) Types.convertType(t,
							this.cfg.getTd()));
				}

				final ArrayInitializer arrayInitializer = getAst().newArrayInitializer();
				for (final Object value : cop.getValues()) {
					arrayInitializer.expressions().add(
							Types.convertLiteral(componentT, value, this.cfg.getTd()));
				}
				((ArrayCreation) expression).setInitializer(arrayInitializer);

				bb.push(expression);
				break;
			}
			case GET: {
				final GET cop = (GET) op;
				final F f = cop.getF();
				if (f.check(AF.STATIC)) {
					final Name name = getAst().newQualifiedName(
							this.cfg.getTd().newTypeName(f.getT()),
							getAst().newSimpleName(f.getName()));
					bb.push(name);
				} else {
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(bb.pop(), Priority.MEMBER_ACCESS));
					fieldAccess.setName(getAst().newSimpleName(f.getName()));
					bb.push(fieldAccess);
				}
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

				if (bb.getTop() == 0) {
					if (value == 1 || value == -1) {
						final PrefixExpression prefixExpression = getAst().newPrefixExpression();
						prefixExpression
								.setOperator(value == 1 ? PrefixExpression.Operator.INCREMENT
										: PrefixExpression.Operator.DECREMENT);
						final String name = getVarName(cop.getReg(), cop.getPc());
						prefixExpression.setOperand(getAst().newSimpleName(name));
						statement = getAst().newExpressionStatement(prefixExpression);
					} else {
						LOGGER.warning("INC with value '" + value + "'!");
						// TODO
					}
				} else {
					LOGGER.warning("Inline INC with value '" + value + "'!");
					// TODO ... may be inline
				}

				break;
			}
			case INSTANCEOF: {
				final INSTANCEOF cop = (INSTANCEOF) op;
				final InstanceofExpression instanceofExpression = getAst()
						.newInstanceofExpression();
				instanceofExpression.setLeftOperand(wrap(bb.pop(), Priority.INSTANCEOF));
				instanceofExpression
						.setRightOperand(Types.convertType(cop.getT(), this.cfg.getTd()));
				bb.push(instanceofExpression);
				break;
			}
			case INVOKE: {
				final INVOKE cop = (INVOKE) op;
				final M m = cop.getM();

				// read method invokation arguments
				final List<Expression> arguments = new ArrayList<Expression>();
				for (int i = m.getParamTs().length; i-- > 0;) {
					arguments.add(wrap(bb.pop()));
				}
				Collections.reverse(arguments);

				final String mName = m.getName();

				final Expression methodExpression;
				if (cop.isDirect()) {
					final Expression expression = bb.pop();
					if ("<init>".equals(mName)) {
						methodExpression = null;
						if (expression instanceof ThisExpression) {
							enumConstructor: if (m.getT().is(Enum.class)
									&& !this.cfg.getCu().check(DFlag.IGNORE_ENUM)) {
								if (arguments.size() < 2) {
									LOGGER.warning("Super constructor invocation '" + m
											+ "' for enum has less than 2 arguments!");
									break enumConstructor;
								}
								if (!m.getParamTs()[0].is(String.class)) {
									LOGGER.warning("Super constructor invocation '"
											+ m
											+ "' for enum must contain string literal as first parameter!");
									break enumConstructor;
								}
								if (m.getParamTs()[1] != T.INT) {
									LOGGER.warning("Super constructor invocation '"
											+ m
											+ "' for enum must contain number literal as first parameter!");
									break enumConstructor;
								}
								arguments.remove(0);
								arguments.remove(0);
							}
							if (arguments.size() == 0) {
								// implicit super callout, more checks possible but not necessary
								break;
							}
							final SuperConstructorInvocation superConstructorInvocation = getAst()
									.newSuperConstructorInvocation();
							superConstructorInvocation.arguments().addAll(arguments);
							bb.addStmt(superConstructorInvocation);
							break;
						}
						if (expression instanceof ClassInstanceCreation) {
							final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) expression;
							if (classInstanceCreation.getAnonymousClassDeclaration() != null) {
								// if none-static context remove initial this parameter or check
								// this$0 in inner?
							}
							// ignore synthetic constructor parameter for inner classes:
							// none-static inner classes get extra constructor argument,
							// anonymous inner classes are static if context is static
							// (see SignatureDecompiler.decompileMethodTypes)
							// TODO
							classInstanceCreation.arguments().addAll(arguments);
							// normally there was a DUP in advance, don't use:
							// basicBlock.pushExpression(classInstanceCreation);
							break;
						}
						LOGGER.warning("Constructor method '<init> expects expression class 'ThisExpression' or 'ClassInstanceCreation' but is '"
								+ expression.getClass() + "' with value: " + expression);
						break;
					}
					if (expression instanceof ThisExpression) {
						final SuperMethodInvocation superMethodInvocation = getAst()
								.newSuperMethodInvocation();
						superMethodInvocation.setName(getAst().newSimpleName(mName));
						superMethodInvocation.arguments().addAll(arguments);
						methodExpression = superMethodInvocation;
					} else {
						// could simply be private method call in same object, nothing special in
						// syntax
						final MethodInvocation methodInvocation = getAst().newMethodInvocation();
						methodInvocation.setExpression(wrap(expression, Priority.METHOD_CALL));
						methodInvocation.setName(getAst().newSimpleName(mName));
						methodInvocation.arguments().addAll(arguments);
						methodExpression = methodInvocation;
					}
				} else if (m.check(AF.STATIC)) {
					final MethodInvocation methodInvocation = getAst().newMethodInvocation();
					methodInvocation.setExpression(this.cfg.getTd().newTypeName(m.getT()));
					methodInvocation.setName(getAst().newSimpleName(mName));
					methodInvocation.arguments().addAll(arguments);
					methodExpression = methodInvocation;
				} else {
					stringAdd: if ("toString".equals(mName)
							&& (m.getT().is(StringBuilder.class) || m.getT().is(StringBuffer.class))) {
						// jdk1.1.6:
						// new
						// StringBuffer(String.valueOf(super.toString())).append(" TEST").toString()
						// jdk1.3:
						// new StringBuffer().append(super.toString()).append(" TEST").toString();
						// jdk1.5.0:
						// new StringBuilder().append(super.toString()).append(" TEST").toString()
						// Eclipse (constructor argument fail?):
						// new
						// StringBuilder(String.valueOf(super.toString())).append(" TEST").toString()
						try {
							Expression stringExpression = null;
							Expression appendExpression = bb.peek();
							do {
								final MethodInvocation methodInvocation = (MethodInvocation) appendExpression;
								if (!"append".equals(methodInvocation.getName().getIdentifier())
										|| methodInvocation.arguments().size() != 1) {
									break stringAdd;
								}
								appendExpression = methodInvocation.getExpression();
								if (stringExpression == null) {
									stringExpression = (Expression) methodInvocation.arguments()
											.get(0);
									continue;
								}
								stringExpression = newInfixExpression(
										InfixExpression.Operator.PLUS, stringExpression,
										(Expression) methodInvocation.arguments().get(0));
							} while (appendExpression instanceof MethodInvocation);
							final ClassInstanceCreation builder = (ClassInstanceCreation) appendExpression;
							// additional type check for pure append-chain not necessary
							if (builder.arguments().size() > 1) {
								break stringAdd;
							}
							if (builder.arguments().size() == 1) {
								stringExpression = newInfixExpression(
										InfixExpression.Operator.PLUS, stringExpression,
										(Expression) builder.arguments().get(0));
							}
							bb.pop();
							bb.push(stringExpression);
							break;
						} catch (final Exception e) {
							// rewrite to string-add didn't work
						}
					}
					final MethodInvocation methodInvocation = getAst().newMethodInvocation();
					methodInvocation.setExpression(wrap(bb.pop(), Priority.METHOD_CALL));
					methodInvocation.setName(getAst().newSimpleName(mName));
					methodInvocation.arguments().addAll(arguments);
					methodExpression = methodInvocation;
				}
				final T returnT = m.getReturnT();
				if (returnT.is(void.class)) {
					statement = getAst().newExpressionStatement(methodExpression);
				} else {
					bb.push(methodExpression);
				}
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
					LOGGER.warning("Unknown cmp type '" + cop.getCmpType() + "'!");
					operator = null;
				}
				statement = getAst().newIfStatement();
				((IfStatement) statement).setExpression(newInfixExpression(operator, bb.pop(),
						bb.pop()));
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
						LOGGER.warning("Unknown cmp type '" + cop.getCmpType() + "'!");
						operator = null;
					}
					((InfixExpression) expression).setOperator(operator);
				} else if (this.cfg.getInFrame(op).peek().getT().isRef()) {
					final InfixExpression.Operator operator;
					switch (cop.getCmpType()) {
					case T_EQ:
						operator = InfixExpression.Operator.EQUALS;
						break;
					case T_NE:
						operator = InfixExpression.Operator.NOT_EQUALS;
						break;
					default:
						LOGGER.warning("Unknown cmp type '" + cop.getCmpType()
								+ "' for null-expression!");
						operator = null;
					}
					final InfixExpression infixExpression = getAst().newInfixExpression();
					infixExpression.setLeftOperand(expression);
					infixExpression.setRightOperand(getAst().newNullLiteral());
					infixExpression.setOperator(operator);
					expression = infixExpression;
				} else if (this.cfg.getInFrame(op).peek().getT() == T.BOOLEAN) {
					// "!a" or "a == 0"?
					switch (cop.getCmpType()) {
					case T_EQ:
						// "== 0" means "is false"
						expression = newPrefixExpression(PrefixExpression.Operator.NOT, expression);
						break;
					case T_NE:
						// "!= 0" means "is true"
						break;
					default:
						LOGGER.warning("Unknown cmp type '" + cop.getCmpType()
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
						LOGGER.warning("Unknown cmp type '" + cop.getCmpType()
								+ "' for 0-expression!");
						operator = null;
					}
					final InfixExpression infixExpression = getAst().newInfixExpression();
					infixExpression.setLeftOperand(expression);
					infixExpression.setRightOperand(getAst().newNumberLiteral("0"));
					infixExpression.setOperator(operator);
					expression = infixExpression;
				}
				statement = getAst().newIfStatement();
				((IfStatement) statement).setExpression(expression);
				break;
			}
			case JSR: {
				assert op instanceof JSR;
				// TODO
				break;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;

				final V v = this.cfg.getFrameVar(cop.getReg(), cop.getPc());
				if (v == null || v.getName() == null) {
					// temporary local
					final Expression expression = bb.get(cop.getReg());
					if (expression != null) {
						bb.push(bb.get(cop.getReg()));
						break;
					}
				}

				final String name = getVarName(cop.getReg(), cop.getPc());
				if ("this".equals(name)) {
					bb.push(getAst().newThisExpression());
				} else {
					bb.push(getAst().newSimpleName(name));
				}
				break;
			}
			case MONITOR: {
				assert op instanceof MONITOR;

				bb.pop();
				break;
			}
			case MUL: {
				assert op instanceof MUL;

				bb.push(newInfixExpression(InfixExpression.Operator.TIMES, bb.pop(), bb.pop()));
				break;
			}
			case NEG: {
				assert op instanceof NEG;

				bb.push(newPrefixExpression(PrefixExpression.Operator.MINUS, bb.pop()));
				break;
			}
			case NEW: {
				final NEW cop = (NEW) op;

				final ClassInstanceCreation classInstanceCreation = getAst()
						.newClassInstanceCreation();

				final String thisName = this.cfg.getTd().getName();
				final T newT = cop.getT();
				final String newName = newT.getName();
				if (newName.startsWith(thisName) && newName.length() >= thisName.length() + 2
						&& newName.charAt(thisName.length()) == '$') {
					inner: try {
						Integer.parseInt(newName.substring(thisName.length() + 1));

						final TD newTd = ((ClassT) newT).getTd();
						if (newTd != null) {
							// anonymous inner can only have a single interface
							// (with generic super "Object") or a super class
							final T[] interfaceTs = newT.getInterfaceTs();
							switch (interfaceTs.length) {
							case 0:
								classInstanceCreation.setType(Types.convertType(newT.getSuperT(),
										this.cfg.getTd()));
								break;
							case 1:
								classInstanceCreation.setType(Types.convertType(interfaceTs[0],
										this.cfg.getTd()));
								break;
							default:
								break inner;
							}
							if (newTd.getParent() == null) {
								// TODO this.cfg.getCu().addTd(newTd);
							}
							// TODO newTd.setPd(this.cfg.getMd());

							final AnonymousClassDeclaration anonymousClassDeclaration = getAst()
									.newAnonymousClassDeclaration();
							newTd.setTypeDeclaration(anonymousClassDeclaration);

							classInstanceCreation
									.setAnonymousClassDeclaration(anonymousClassDeclaration);

							bb.push(classInstanceCreation);
							break;
						}
					} catch (final NumberFormatException e) {
						// no int
					}
				}
				classInstanceCreation.setType(Types.convertType(newT, this.cfg.getTd()));
				bb.push(classInstanceCreation);
				break;
			}
			case NEWARRAY: {
				final NEWARRAY cop = (NEWARRAY) op;
				final ArrayCreation arrayCreation = getAst().newArrayCreation();
				arrayCreation.setType(getAst().newArrayType(
						Types.convertType(cop.getT(), this.cfg.getTd())));
				for (int i = cop.getDimensions(); i-- > 0;) {
					arrayCreation.dimensions().add(bb.pop());
				}
				bb.push(arrayCreation);
				break;
			}
			case OR: {
				assert op instanceof OR;

				bb.push(newInfixExpression(InfixExpression.Operator.OR, bb.pop(), bb.pop()));
				break;
			}
			case POP: {
				final POP cop = (POP) op;
				switch (cop.getKind()) {
				case POP2:
					if (!isWide(cop)) {
						statement = getAst().newExpressionStatement(bb.pop());

						LOGGER.warning("TODO: POP2 for not wide in '" + this.cfg
								+ "'! Statement output?");
						bb.pop();
						break;
					}
					// fall through for wide
				case POP:
					statement = getAst().newExpressionStatement(bb.pop());
					break;
				default:
					LOGGER.warning("Unknown POP type '" + cop.getKind() + "'!");
				}
				break;
			}
			case PUSH: {
				final PUSH cop = (PUSH) op;
				final Expression expr = Types.convertLiteral(
						this.cfg.getOutFrame(op).peek().getT(), cop.getValue(), this.cfg.getTd());
				if (expr != null) {
					bb.push(expr);
				}
				break;
			}
			case PUT: {
				final PUT cop = (PUT) op;
				final Expression rightExpression = bb.pop();
				final F f = cop.getF();
				fieldInit: if (this.cfg.getMd().getTd().getT() == f.getT()) {
					// set local field, could be initializer
					if (f.check(AF.STATIC)) {
						if (!"<clinit>".equals(this.cfg.getMd().getName())) {
							break fieldInit;
						}
					} else {
						if (!"<init>".equals(this.cfg.getMd().getName())) {
							break fieldInit;
						}
						if (!(bb.peek() instanceof ThisExpression)) {
							break fieldInit;
						}
						// multiple constructors with different signatures possible, all of them
						// contain the same field initializer code after super() - simply overwrite
					}
					if (this.cfg.getStartBb() != bb || bb.getStmts() > 1) {
						break fieldInit;
					}
					if (bb.getStmts() == 1
							&& !(bb.getStmt(0) instanceof SuperConstructorInvocation)) {
						// initial super(<arguments>) is allowed
						break fieldInit;
					}
					// TODO this checks are not enough, we must assure that we don't use method
					// arguments here!!!
					if (((ClassT) f.getT()).check(AF.ENUM)
							&& !this.cfg.getCu().check(DFlag.IGNORE_ENUM)) {
						if (f.check(AF.ENUM)) {
							// assignment to enum constant declaration
							if (!(rightExpression instanceof ClassInstanceCreation)) {
								LOGGER.warning("Assignment to enum field '" + f
										+ "' is no class instance creation!");
								break fieldInit;
							}
							final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) rightExpression;
							// first two arguments must be String (== field name) and int (ordinal)
							final List<Expression> arguments = classInstanceCreation.arguments();
							if (arguments.size() < 2) {
								LOGGER.warning("Class instance creation for enum field '" + f
										+ "' has less than 2 arguments!");
								break fieldInit;
							}
							if (!(arguments.get(0) instanceof StringLiteral)) {
								LOGGER.warning("Class instance creation for enum field '" + f
										+ "' must contain string literal as first parameter!");
								break fieldInit;
							}
							final String literalValue = ((StringLiteral) arguments.get(0))
									.getLiteralValue();
							if (!literalValue.equals(f.getName())) {
								LOGGER.warning("Class instance creation for enum field '"
										+ f
										+ "' must contain string literal equal to field name as first parameter!");
								break fieldInit;
							}
							if (!(arguments.get(1) instanceof NumberLiteral)) {
								LOGGER.warning("Class instance creation for enum field '" + f
										+ "' must contain number literal as first parameter!");
								break fieldInit;
							}
							final FD fd = this.cfg.getTd().getT().getF(f.getName(), f.getValueT())
									.getFd();
							final BodyDeclaration fieldDeclaration = fd.getFieldDeclaration();
							assert fieldDeclaration instanceof EnumConstantDeclaration : fieldDeclaration;
							final EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) fieldDeclaration;

							for (int i = arguments.size(); i-- > 2;) {
								final Expression e = arguments.get(i);
								e.delete();
								enumConstantDeclaration.arguments().add(0, e);
							}

							final AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation
									.getAnonymousClassDeclaration();
							if (anonymousClassDeclaration != null) {
								anonymousClassDeclaration.delete();
								enumConstantDeclaration
										.setAnonymousClassDeclaration(anonymousClassDeclaration);
								// normally contains one constructor, that calls a synthetic super
								// constructor with the enum class as additional last parameter,
								// this may contain field initializers, that we must keep,
								// so we can only remove the constructor in final merge (because
								// anonymous inner classes cannot hava visible Java constructor)
							}
							break;
						}
						if ("$VALUES".equals(f.getName()) || "ENUM$VALUES".equals(f.getName())) {
							break; // ignore such assignments completely
						}
					}
					if (f.check(AF.SYNTHETIC)) {
						if (this.cfg.getCu().check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
							break fieldInit; // not as field initializer
						}
						break; // ignore such assignments completely
					}
					final FD fd = this.cfg.getTd().getT().getF(f.getName(), f.getValueT()).getFd();
					if (fd == null || !(fd.getFieldDeclaration() instanceof FieldDeclaration)) {
						break fieldInit;
					}
					try {
						((VariableDeclarationFragment) ((FieldDeclaration) fd.getFieldDeclaration())
								.fragments().get(0)).setInitializer(wrap(rightExpression,
								Priority.ASSIGNMENT));
						if (!f.check(AF.STATIC)) {
							bb.pop();
						}
					} catch (final Exception e) {
						// rewrite to field-initializer didn't work
					}
					break;
				}
				final Assignment assignment = getAst().newAssignment();
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				assignment.setRightHandSide(wrap(rightExpression, Priority.ASSIGNMENT));

				if (f.check(AF.STATIC)) {
					final Name name = getAst().newQualifiedName(
							this.cfg.getTd().newTypeName(f.getT()),
							getAst().newSimpleName(f.getName()));
					assignment.setLeftHandSide(name);
				} else {
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(bb.pop(), Priority.MEMBER_ACCESS));
					fieldAccess.setName(getAst().newSimpleName(f.getName()));
					assignment.setLeftHandSide(fieldAccess);
				}
				// inline assignment, DUP(_X1) -> PUT
				if (bb.getTop() > 0 && bb.peek() == rightExpression) {
					bb.pop();
					bb.push(assignment);
				} else if (bb.getTop() > 0
						&& rightExpression instanceof InfixExpression
						&& (((InfixExpression) rightExpression).getOperator() == InfixExpression.Operator.PLUS || ((InfixExpression) rightExpression)
								.getOperator() == InfixExpression.Operator.MINUS)) {
					// if i'm an peek-1 or peek+1 expression, than we can post-inc/dec
					// TODO more checks!
					final PostfixExpression postfixExpression = getAst().newPostfixExpression();
					postfixExpression
							.setOperator(((InfixExpression) rightExpression).getOperator() == InfixExpression.Operator.PLUS ? PostfixExpression.Operator.INCREMENT
									: PostfixExpression.Operator.DECREMENT);
					postfixExpression.setOperand(wrap(bb.pop(), Priority.PREFIX_OR_POSTFIX));
					bb.push(postfixExpression);
				} else {
					statement = getAst().newExpressionStatement(assignment);
				}
				break;
			}
			case REM: {
				assert op instanceof REM;

				bb.push(newInfixExpression(InfixExpression.Operator.REMAINDER, bb.pop(), bb.pop()));
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
				assert op instanceof SHL;

				bb.push(newInfixExpression(InfixExpression.Operator.LEFT_SHIFT, bb.pop(), bb.pop()));
				break;
			}
			case SHR: {
				final SHR cop = (SHR) op;
				bb.push(newInfixExpression(
						cop.isUnsigned() ? InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED
								: InfixExpression.Operator.RIGHT_SHIFT_SIGNED, bb.pop(), bb.pop()));
				break;
			}
			case STORE: {
				final STORE cop = (STORE) op;

				final Expression rightExpression = bb.pop();

				// inline assignment, DUP -> STORE
				final boolean isInlineAssignment = bb.getTop() > 0 && bb.peek() == rightExpression;
				final V v = this.cfg.getFrameVar(cop.getReg(), cop.getPc() + 1);

				if (v == null /* tmp hack */|| v.getName() == null) {
					// temporary local
					// bb.set(cop.getReg(), rightExpression);
					// break;
					// TODO else not really necessary later if this is sure
				} else {
					if (!isInlineAssignment && v.getPcs()[0] /* TODO */== cop.getPc() + 1) {
						final VariableDeclarationFragment variableDeclarationFragment = getAst()
								.newVariableDeclarationFragment();
						variableDeclarationFragment.setName(getAst().newSimpleName(v.getName()));
						variableDeclarationFragment.setInitializer(wrap(rightExpression,
								Priority.ASSIGNMENT));
						final VariableDeclarationStatement variableDeclarationStatement = getAst()
								.newVariableDeclarationStatement(variableDeclarationFragment);
						variableDeclarationStatement.setType(Types.convertType(v.getT(),
								this.cfg.getTd()));
						statement = variableDeclarationStatement;
						break;
					}
				}

				final Assignment assignment = getAst().newAssignment();
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				assignment.setRightHandSide(wrap(rightExpression, Priority.ASSIGNMENT));

				final String name = getVarName(cop.getReg(), cop.getPc() + 1);
				assignment.setLeftHandSide(getAst().newSimpleName(name));
				// inline assignment, DUP -> STORE
				if (isInlineAssignment) {
					bb.pop();
					bb.push(assignment);
				} else {
					statement = getAst().newExpressionStatement(assignment);
				}
				break;
			}
			case SUB: {
				assert op instanceof SUB;

				bb.push(newInfixExpression(InfixExpression.Operator.MINUS, bb.pop(), bb.pop()));
				break;
			}
			case SWAP: {
				assert op instanceof SWAP;

				final Expression e1 = bb.pop();
				final Expression e2 = bb.pop();
				bb.push(e1);
				bb.push(e2);
				break;
			}
			case SWITCH: {
				assert op instanceof SWITCH;

				final SwitchStatement switchStatement = getAst().newSwitchStatement();
				switchStatement.setExpression(wrap(bb.pop()));
				statement = switchStatement;
				break;
			}
			case THROW: {
				assert op instanceof THROW;

				final ThrowStatement throwStatement = getAst().newThrowStatement();
				throwStatement.setExpression(wrap(bb.pop()));
				statement = throwStatement;
				break;
			}
			case XOR: {
				assert op instanceof XOR;

				final Expression expression = bb.pop();
				// "a ^ -1" => "~a"
				if (expression instanceof NumberLiteral
						&& ((NumberLiteral) expression).getToken().equals("-1")) {
					bb.push(newPrefixExpression(PrefixExpression.Operator.COMPLEMENT, bb.pop()));
				} else {
					bb.push(newInfixExpression(InfixExpression.Operator.XOR, expression, bb.pop()));
				}
				break;
			}
			default:
				throw new RuntimeException("Unknown intermediate vm operation '" + op + "'!");
			}
			if (statement != null) {
				bb.addStmt(statement);
			}
		}
		return true;
	}

	private AST getAst() {
		return this.cfg.getCu().getAst();
	}

	private String getVarName(final int reg, final int pc) {
		final V v = this.cfg.getFrameVar(reg, pc);
		final String name = v == null ? null : v.getName();
		return name == null ? "r" + reg : name;
	}

	private boolean isWide(final Op op) {
		final R r = this.cfg.getInFrame(op).peek();
		if (r == null) {
			return false;
		}
		return r.getT().isWide();
	}

	private boolean rewriteClassForNameCachedLiteral(final BB bb) {
		// seen in JDK 1.2 Eclipse Core:
		// DUP-POP conditional variant: GET class$0 DUP JCND_NE
		// (_POP_ PUSH "typeLiteral" INVOKE Class.forName DUP PUT class$0 GOTO)
		if (bb.getOps() != 6 || !(bb.getOp(0) instanceof POP) || !(bb.getOp(1) instanceof PUSH)
				|| !(bb.getOp(2) instanceof INVOKE) || !(bb.getOp(3) instanceof DUP)
				|| !(bb.getOp(4) instanceof PUT) || !(bb.getOp(5) instanceof GOTO)) {
			return false;
		}
		final BB followBb = bb.getSucc();
		if (followBb == null || bb.getIns().size() != 1) {
			return false;
		}
		final BB condHead = bb.getIns().get(0).getStart();
		if (!condHead.isFinalStmtCond()) {
			return false;
		}
		final BB trueSucc = condHead.getTrueSucc();
		final BB falseSucc = condHead.getFalseSucc();
		if (falseSucc == bb && trueSucc != followBb || trueSucc == bb && falseSucc != followBb) {
			return false;
		}

		// TODO check, but is very rare:
		// Expression QualifiedName: JDTCompilerAdapter.class$0 (or Simple?)
		// IFStatement

		Expression expression = condHead.peek();
		try {
			final String classInfo = (String) ((PUSH) bb.getOp(1)).getValue();
			expression = Types.convertLiteral(this.cfg.getDu().getT(Class.class), this.cfg.getDu()
					.getT(classInfo), this.cfg.getTd());
		} catch (final Exception e) {
			// rewrite to class literal didn't work
			return false;
		}

		condHead.pop();
		condHead.removeFinalStmt();

		followBb.copyContentFrom(condHead);
		condHead.moveIns(followBb);
		condHead.remove();
		falseSucc.remove();
		followBb.push(expression);
		return true;
	}

	private boolean rewriteConditional(final BB bb) {
		// IF ? T : F

		// ....|..
		// ....I..
		// ..t/.\f
		// ..T...F
		// ...\./.
		// ....B..

		// this has 3 preds: a == null ? 0 : a.length() == 0 ? 0 : 1
		// even more preds possible with boolean conditionals
		if (bb.getIns().size() < 2) {
			return false;
		}
		BB condHead = null;
		for (final E in : bb.getIns()) {
			final BB pred = in.getStart();
			// should be impossible?!
			// if (pred.getSucc() == null) {
			// return false;
			// }
			if (pred.getIns().size() != 1) {
				return false;
			}
			if (pred.getTop() != 1) {
				return false;
			}
			if (pred.getStmts() > 0) {
				return false;
			}
			final BB predPred = pred.getIns().get(0).getStart();
			if (condHead == null || predPred.getPostorder() < condHead.getPostorder()) {
				condHead = predPred;
			}
		}
		if (condHead == null || !condHead.isFinalStmtCond()) {
			return false;
		}

		final BB trueSucc = condHead.getTrueSucc();
		final BB falseSucc = condHead.getFalseSucc();

		final Expression trueExpression = trueSucc.peek();
		final Expression falseExpression = falseSucc.peek();

		Expression expression = ((IfStatement) condHead.getFinalStmt()).getExpression();
		rewrite: if ((trueExpression instanceof BooleanLiteral || trueExpression instanceof NumberLiteral)
				&& (falseExpression instanceof BooleanLiteral || falseExpression instanceof NumberLiteral)) {
			// expressions: expression ? true : false => a
			// TODO NumberLiteral necessary until Data Flow Analysis works
			if (trueExpression instanceof BooleanLiteral
					&& !((BooleanLiteral) trueExpression).booleanValue()
					|| trueExpression instanceof NumberLiteral
					&& ((NumberLiteral) trueExpression).getToken().equals("0")) {
				expression = newPrefixExpression(PrefixExpression.Operator.NOT, expression);
			}
		} else {
			classLiteral: if (expression instanceof InfixExpression) {
				// Class-literals unknown in pre JDK 1.5 bytecode
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
					// JDK < 1.3
					if (!(trueExpression instanceof Assignment)) {
						break classLiteral;
					}
					assignment = (Assignment) trueExpression;
				} else if (equalsExpression.getOperator() == InfixExpression.Operator.NOT_EQUALS) {
					// JDK >= 1.3
					if (!(falseExpression instanceof Assignment)) {
						break classLiteral;
					}
					assignment = (Assignment) falseExpression;
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
				if (this.cfg.getTd().getVersion() >= 49) {
					LOGGER.warning("Unexpected class literal code with class$() in >= JDK 5 code!");
				}
				try {
					final String classInfo = ((StringLiteral) methodInvocation.arguments().get(0))
							.getLiteralValue();
					expression = Types.convertLiteral(this.cfg.getDu().getT(Class.class), this.cfg
							.getDu().getT(classInfo), this.cfg.getTd());
					break rewrite;
				} catch (final Exception e) {
					// rewrite to class literal didn't work
				}
			}
			// expressions: expression ? trueExpression : falseExpression
			final ConditionalExpression conditionalExpression = getAst().newConditionalExpression();
			conditionalExpression.setExpression(wrap(expression, Priority.CONDITIONAL));
			conditionalExpression.setThenExpression(wrap(trueExpression, Priority.CONDITIONAL));
			conditionalExpression.setElseExpression(wrap(falseExpression, Priority.CONDITIONAL));
			expression = conditionalExpression;
		}

		// is conditional expression, modify graph
		// remove IfStatement
		condHead.removeFinalStmt();

		trueSucc.remove();
		falseSucc.remove();

		if (bb.getIns().size() != 0) {
			condHead.push(expression);
			condHead.setSucc(bb);
		} else {
			// pull
			bb.copyContentFrom(condHead);
			condHead.moveIns(bb);
			condHead.remove();

			// push new conditional expression, here only "a ? true : false" as "a"
			bb.push(expression);
		}

		return true;
	}

	private boolean rewriteHandler(final BB bb) {
		if (bb.getOps() == 0 || !(bb.getOp(0) instanceof STORE)) {
			LOGGER.warning("First operation in handler isn't STORE: " + bb);
			return false;
		}
		final T[] handlerTypes = (T[]) bb.getIns().get(0).getValue();
		final boolean isFinally = 1 == handlerTypes.length && null == handlerTypes[0];

		final STORE cop = (STORE) bb.removeOp(0);
		final String name = getVarName(cop.getReg(), cop.getPc() + 1);

		final TryStatement tryStatement = getAst().newTryStatement();
		if (!isFinally) {
			final CatchClause catchClause = getAst().newCatchClause();
			final SingleVariableDeclaration singleVariableDeclaration = getAst()
					.newSingleVariableDeclaration();
			singleVariableDeclaration.setName(getAst().newSimpleName(name));
			if (handlerTypes.length == 1) {
				singleVariableDeclaration.setType(getAst().newSimpleType(
						this.cfg.getTd().newTypeName(handlerTypes[0])));
			} else {
				// Multi-Catch
				final UnionType unionType = getAst().newUnionType();
				for (final T t : handlerTypes) {
					unionType.types().add(getAst().newSimpleType(this.cfg.getTd().newTypeName(t)));
				}
				singleVariableDeclaration.setType(unionType);
			}
			catchClause.setException(singleVariableDeclaration);
			tryStatement.catchClauses().add(catchClause);
		}
		bb.addStmt(tryStatement);
		return true;
	}

	private void transform() {
		final List<BB> bbs = this.cfg.getPostorderedBbs();
		// for all nodes in _reverse_ postorder: is also backward possible with nice optimizations,
		// but this way easier handling of dalvik temporary registers
		for (int i = bbs.size(); i-- > 0;) {
			final BB bb = bbs.get(i);
			if (bb == null) {
				// can happen if BB deleted through rewrite
				continue;
			}
			final boolean handler = bb.isHandler();
			if (handler) {
				rewriteHandler(bb);
			} else {
				while (rewriteConditional(bb)) {
					// delete superior BBs, multiple iterations possible:
					// a == null ? 0 : a.length() == 0 ? 0 : 1
				}
			}
			// previous expressions merged into bb, now rewrite:
			if (!convertToHLLIntermediate(bb)) {
				// DUP-POP conditional variant for pre JDK 5 cached class literals
				if (rewriteClassForNameCachedLiteral(bb)) {
					// delete myself and superior nodes
					continue;
				}
				// should never happen in forward mode
				LOGGER.warning("Stack underflow in '" + this.cfg + "':\n" + bb);
			}
			if (!handler) {
				// single IfStatement created? then check:
				while (rewriteShortCircuitCompound(bb)) {
					// delete superior BBs, multiple iterations possible
				}
			}
		}
	}

}