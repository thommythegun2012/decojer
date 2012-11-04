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

import static org.decojer.cavaj.utils.Expressions.booleanFromLiteral;
import static org.decojer.cavaj.utils.Expressions.newInfixExpression;
import static org.decojer.cavaj.utils.Expressions.newPrefixExpression;
import static org.decojer.cavaj.utils.Expressions.not;
import static org.decojer.cavaj.utils.Expressions.wrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
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
import org.decojer.cavaj.model.code.ops.NEW;
import org.decojer.cavaj.model.code.ops.NEWARRAY;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.POP;
import org.decojer.cavaj.model.code.ops.PUSH;
import org.decojer.cavaj.model.code.ops.PUT;
import org.decojer.cavaj.model.code.ops.RETURN;
import org.decojer.cavaj.model.code.ops.SHR;
import org.decojer.cavaj.model.code.ops.STORE;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.OperatorPrecedence;
import org.decojer.cavaj.utils.Priority;
import org.decojer.cavaj.utils.Types;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
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
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
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

	private static Expression newInfixExpressionPop(final Operator operator, final BB bb) {
		final Expression rightExpression = bb.pop();
		return newInfixExpression(operator, bb.pop(), rightExpression);
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
		boolean fieldInit = true; // small hack for now...later because of conditionals?
		while (bb.getOps() > 0) {
			while (bb.isStackUnderflow()) {
				// try to pull previous single sequence nodes including stack
				if (bb.getIns().isEmpty()) {
					return true; // nothing to do...deleted node
				}
				final E in = bb.getRelevantIn();
				if (in == null) {
					return false; // multiple or conditional incomings -> real fail
				}
				bb.joinPredBb(in.getStart());
			}
			final Op op = bb.removeOp(0);
			Statement statement = null;
			switch (op.getOptype()) {
			case ADD: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.PLUS, bb));
				break;
			}
			case ALOAD: {
				final ArrayAccess arrayAccess = getAst().newArrayAccess();
				arrayAccess.setIndex(wrap(bb.pop()));
				arrayAccess.setArray(wrap(bb.pop(), Priority.ARRAY_INDEX));
				bb.push(arrayAccess);
				break;
			}
			case AND: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.AND, bb));
				break;
			}
			case ARRAYLENGTH: {
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
						// not all indexes may be set, null/0/false in JVM 7 are not set, fill
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
				if (!bb.isStackEmpty() && bb.peek() == rightExpression) {
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
				// pseudo expression for following JCND, not really the correct
				// answer for -1, 0, 1
				bb.push(newInfixExpressionPop(InfixExpression.Operator.LESS_EQUALS, bb));
				break;
			}
			case DIV: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.DIVIDE, bb));
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
					if (f.check(AF.SYNTHETIC)
							&& (f.getName().startsWith("class$") || f.getName()
									.startsWith("array$"))) {
						if (rewriteCachedClassLiteral(bb)) {
							break;
						}
						LOGGER.warning("Couldn't rewrite cached class literal '" + f + "'!");
					}
					bb.push(getAst().newQualifiedName(this.cfg.getTd().newTypeName(f.getT()),
							getAst().newSimpleName(f.getName())));
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

				if (!bb.isStackEmpty()) {
					// TODO could be inline at begin!
					if (value == 1 || value == -1) {
						final PrefixExpression prefixExpression = getAst().newPrefixExpression();
						prefixExpression
								.setOperator(value == 1 ? PrefixExpression.Operator.INCREMENT
										: PrefixExpression.Operator.DECREMENT);
						final String name = getVarName(cop.getReg(), cop.getPc());
						prefixExpression.setOperand(getAst().newSimpleName(name));
						statement = getAst().newExpressionStatement(prefixExpression);
					} else {
						final Assignment assignment = getAst().newAssignment();
						assignment.setOperator(value >= 0 ? Assignment.Operator.PLUS_ASSIGN
								: Assignment.Operator.MINUS_ASSIGN);
						assignment.setRightHandSide(Types.convertLiteral(cop.getT(),
								value >= 0 ? value : -value, this.cfg.getTd()));
						statement = getAst().newExpressionStatement(assignment);
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

				final Expression methodExpression;
				if (cop.isDirect()) {
					final Expression expression = bb.pop();
					if (m.isConstructor()) {
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
							fieldInit = true;
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
						LOGGER.warning("Constructor expects expression class 'ThisExpression' or 'ClassInstanceCreation' but is '"
								+ expression.getClass() + "' with value: " + expression);
						break;
					}
					if (expression instanceof ThisExpression) {
						final SuperMethodInvocation superMethodInvocation = getAst()
								.newSuperMethodInvocation();
						superMethodInvocation.setName(getAst().newSimpleName(m.getName()));
						superMethodInvocation.arguments().addAll(arguments);
						methodExpression = superMethodInvocation;
					} else {
						// could be private method call in same object, nothing special in syntax
						final MethodInvocation methodInvocation = getAst().newMethodInvocation();
						methodInvocation.setExpression(wrap(expression, Priority.METHOD_CALL));
						methodInvocation.setName(getAst().newSimpleName(m.getName()));
						methodInvocation.arguments().addAll(arguments);
						methodExpression = methodInvocation;
					}
				} else if (m.check(AF.STATIC)) {
					final MethodInvocation methodInvocation = getAst().newMethodInvocation();
					methodInvocation.setExpression(this.cfg.getTd().newTypeName(m.getT()));
					methodInvocation.setName(getAst().newSimpleName(m.getName()));
					methodInvocation.arguments().addAll(arguments);
					methodExpression = methodInvocation;
				} else {
					if ("toString".equals(m.getName())
							&& (m.getT().is(StringBuilder.class) || m.getT().is(StringBuffer.class))) {
						if (rewriteStringAppend(bb)) {
							break;
						}
					}
					final MethodInvocation methodInvocation = getAst().newMethodInvocation();
					final Expression expression = bb.pop();
					if (!(expression instanceof ThisExpression)) {
						methodInvocation.setExpression(wrap(expression, Priority.METHOD_CALL));
					}
					methodInvocation.setName(getAst().newSimpleName(m.getName()));
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
				((IfStatement) statement).setExpression(newInfixExpressionPop(operator, bb));
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
						expression = not(expression);
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
				// TODO
				break;
			}
			case LOAD: {
				final LOAD cop = (LOAD) op;
				/*
				 * final V v = this.cfg.getFrameVar(cop.getReg(), cop.getPc()); if (v == null ||
				 * v.getName() == null) { // temporary local final Expression expression =
				 * bb.get(cop.getReg()); if (expression != null) { bb.push(bb.get(cop.getReg()));
				 * break; } }
				 */

				// must not access method parameters for fieldInits...
				fieldInit &= cop.getReg() == 0 && this.cfg.getMd().isConstructor()
						|| !this.cfg.getInFrame(cop).get(cop.getReg()).isMethodParam();

				final String name = getVarName(cop.getReg(), cop.getPc());
				if ("this".equals(name)) {
					bb.push(getAst().newThisExpression());
				} else {
					bb.push(getAst().newSimpleName(name));
				}
				break;
			}
			case MONITOR: {
				bb.pop();
				break;
			}
			case MUL: {
				bb.push(newInfixExpressionPop(InfixExpression.Operator.TIMES, bb));
				break;
			}
			case NEG: {
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
				bb.push(newInfixExpressionPop(InfixExpression.Operator.OR, bb));
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
				if (fieldInit && this.cfg.getMd().getTd().getT() == f.getT()
						&& rewriteFieldInit(bb, f, rightExpression)) {
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
				if (!bb.isStackEmpty() && bb.peek() == rightExpression) {
					bb.pop();
					bb.push(assignment);
				} else if (!bb.isStackEmpty()
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
				bb.push(newInfixExpressionPop(InfixExpression.Operator.REMAINDER, bb));
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
				bb.push(newInfixExpressionPop(InfixExpression.Operator.LEFT_SHIFT, bb));
				break;
			}
			case SHR: {
				final SHR cop = (SHR) op;
				bb.push(newInfixExpressionPop(
						cop.isUnsigned() ? InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED
								: InfixExpression.Operator.RIGHT_SHIFT_SIGNED, bb));
				break;
			}
			case STORE: {
				final STORE cop = (STORE) op;

				final Expression rightExpression = bb.pop();

				// inline assignment, DUP -> STORE
				final boolean isInlineAssignment = !bb.isStackEmpty()
						&& bb.peek() == rightExpression;
				final V v = this.cfg.getFrameVar(cop.getReg(), cop.getPc() + 1);

				if (v == null /* tmp hack */|| v.getName() == null) {
					// temporary local
					// bb.set(cop.getReg(), rightExpression);
					// break;
					// TODO else not really necessary later if this is sure
				} else {
					// TODO if () int i = 0 else int i = 1 ???
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
				bb.push(newInfixExpressionPop(InfixExpression.Operator.MINUS, bb));
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
				final SwitchStatement switchStatement = getAst().newSwitchStatement();
				switchStatement.setExpression(wrap(bb.pop()));
				statement = switchStatement;
				break;
			}
			case THROW: {
				final Expression exceptionExpression = bb.pop();
				if (rewriteAssertStatement(exceptionExpression, bb)) {
					break;
				}
				final ThrowStatement throwStatement = getAst().newThrowStatement();
				throwStatement.setExpression(wrap(exceptionExpression));
				statement = throwStatement;
				break;
			}
			case XOR: {
				final Expression rightExpression = bb.pop();
				// "a ^ -1" => "~a"
				if (rightExpression instanceof NumberLiteral
						&& ((NumberLiteral) rightExpression).getToken().equals("-1")) {
					bb.push(newPrefixExpression(PrefixExpression.Operator.COMPLEMENT, bb.pop()));
				} else {
					bb.push(newInfixExpression(InfixExpression.Operator.XOR, bb.pop(),
							rightExpression));
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

	private boolean rewriteAssertStatement(final Expression exceptionExpression, final BB bb) {
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
		if (!start.isCondOrPreLoopHead()) {
			return false;
		}
		final IfStatement ifStatement = (IfStatement) start.getFinalStmt();
		Expression expression = ifStatement.getExpression();
		Expression condExpression;
		if (expression instanceof InfixExpression) {
			final InfixExpression infixExpression = (InfixExpression) expression;
			if (infixExpression.getOperator() != InfixExpression.Operator.CONDITIONAL_OR) {
				return false;
			}
			expression = infixExpression.getLeftOperand();
			condExpression = infixExpression.getRightOperand();
		} else {
			condExpression = null;
		}
		start.removeFinalStmt();
		final AssertStatement assertStatement = getAst().newAssertStatement();
		assertStatement.setExpression(condExpression == null ? getAst().newBooleanLiteral(false)
				: wrap(condExpression));
		if (messageExpression != null) {
			assertStatement.setMessage(wrap(messageExpression));
		}
		start.addStmt(assertStatement);
		start.getTrueSucc().joinPredBb(start);
		return true;
	}

	private boolean rewriteBooleanCompound(final BB bb) {
		for (final E in : bb.getIns()) {
			// all following patterns have the following base form:
			// "A(final IfStmt) -> C(unique IfStmt) -> bb"
			final E c_bb = in.relevantIn();
			if (c_bb == null || !c_bb.isCond()) {
				continue;
			}
			final BB c = c_bb.getStart();
			if (c.getStmts() != 1 || !c.isCondOrPreLoopHead() || !c.isStackEmpty()) {
				continue;
			}
			final E a_c = c.getRelevantIn();
			if (a_c == null || !a_c.isCond()) {
				continue;
			}
			final BB a = a_c.getStart();
			if (!a.isCondOrPreLoopHead()) {
				continue;
			}
			// now we have the potential compound head, go down again and identify patterns
			final E a_x = a_c.isCondTrue() ? a.getFalseOut() : a.getTrueOut();
			final BB x = a_x.getRelevantEnd();
			final E c_bb2 = c_bb.isCondTrue() ? c.getFalseOut() : c.getTrueOut();
			final BB bb2 = c_bb2.getRelevantEnd();

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
				final Expression leftExpression = ifStatement.getExpression();
				final Expression rightExpression = ((IfStatement) c.removeStmt(0)).getExpression();
				if (bb == x && c_bb.isCondTrue() || bb2 == x && c_bb2.isCondTrue() /* ?t */) {
					if (a_x.isCondTrue() /* tt */) {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_OR, leftExpression,
								rightExpression));
					} else {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_OR, not(leftExpression),
								rightExpression));
					}
				} else {
					if (a_x.isCondTrue() /* tf */) {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_AND, not(leftExpression),
								rightExpression));
					} else {
						ifStatement.setExpression(newInfixExpression(
								InfixExpression.Operator.CONDITIONAL_AND, leftExpression,
								rightExpression));
					}
				}
				c.joinPredBb(a);
				return true;
			}
			if (x.getStmts() != 1 || !x.isCondOrPreLoopHead() || !x.isStackEmpty()) {
				continue;
			}
			// check cross...
			E x_bb = x.getTrueOut();
			if (x_bb.getRelevantEnd() == bb) {
				if (x.getFalseOut().getRelevantEnd() != bb2) {
					continue;
				}
			} else {
				if (x_bb.getRelevantEnd() != bb2) {
					continue;
				}
				x_bb = x.getFalseOut();
				if (x_bb.getRelevantEnd() != bb) {
					continue;
				}
			}

			// This is a conditional compound (since JDK 4 with C/x is cond), example is A ? C : x:
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
			// This should be the unique structure that leads to none-flat CFGs for forward-edges.

			// rewrite AST
			final IfStatement ifStatement = (IfStatement) a.getFinalStmt();

			Expression expression = ifStatement.getExpression();
			Expression thenExpression;
			Expression elseExpression;
			if (c.isBefore(x)) {
				if (a_c.isCondFalse()) {
					expression = not(expression);
				}
				thenExpression = ((IfStatement) c.removeStmt(0)).getExpression();
				elseExpression = ((IfStatement) x.removeStmt(0)).getExpression();
				if (c_bb.isCondTrue() ^ x_bb.isCondTrue()) {
					// cross is true/false mix, for join we must inverse the none-join node x
					elseExpression = not(elseExpression);
				}
			} else { /* x is before c */
				if (a_x.isCondFalse()) {
					expression = not(expression);
				}
				thenExpression = ((IfStatement) x.removeStmt(0)).getExpression();
				elseExpression = ((IfStatement) c.removeStmt(0)).getExpression();
				if (c_bb.isCondTrue() ^ x_bb.isCondTrue()) {
					// cross is true/false mix, for join we must inverse the none-join node x
					thenExpression = not(thenExpression);
				}
			}
			final ConditionalExpression conditionalExpression = getAst().newConditionalExpression();
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
	 * Class Literal Caching (no direct Constant Pool Class Literals before JDK 5).
	 * 
	 * @param bb
	 *            BB
	 * @return {@code true} - rewritten
	 */
	private boolean rewriteCachedClassLiteral(final BB bb) {
		// field-get for synthetic field which name starts with "class$" or "array$"
		// (is class$<[0-9]+> for Eclipse or class$<classname> for JDK)

		// I admit this function looks very ugly...more general pattern matching would be nice!
		try {
			if (bb.getOps() == 1) {
				// try JDK style:
				// GET class$java$lang$String JCND_NE
				// (PUSH "typeLiteral" INVOKE Class.forName DUP PUT class$java$lang$String GOTO #)
				// GET class$java$lang$String #
				if (!(bb.getOp(0) instanceof JCND)) {
					return false;
				}
				// JDK 1 & 2 is EQ, 3 & 4 is NE, >=5 has direct Class Literals
				final E initCacheOut = ((JCND) bb.getOp(0)).getCmpType() == CmpType.T_EQ ? bb
						.getTrueOut() : bb.getFalseOut();

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
					// JDK 3 & 4
					return false;
				}

				final BB getBb = initCacheOut.isCondTrue() ? bb.getFalseOut().getEnd() : bb
						.getTrueOut().getEnd();
				if (getBb.getOps() != 1 && getBb.getOps() != 2) {
					return false;
				}
				if (!(getBb.getOp(0) instanceof GET)) {
					return false;
				}
				if (pushBb.getOps() == 2 && !(getBb.getOp(1) instanceof GOTO)) {
					// JDK 1 & 2
					return false;
				}
				final BB followBb = getBb.getOut().getEnd();
				// can just happen for JDK<5: replace . -> /
				final String classInfo = ((String) ((PUSH) pushBb.getOp(0)).getValue()).replace(
						'.', '/');
				followBb.push(Types.convertLiteral(this.cfg.getDu().getT(Class.class), this.cfg
						.getDu().getT(classInfo), this.cfg.getTd()));
				bb.removeOp(0);
				followBb.joinPredBb(bb);
				return true;
			}
			// try Eclipse style, ignore Exception-handling:
			// (see Eclipse 1.2 JDT: org.eclipse.jdt.core.JDTCompilerAdapter.execute())
			//
			// GET class$0 DUP JCND_NE
			// (_POP_ PUSH "typeLiteral" INVOKE Class.forName DUP PUT class$0 GOTO #)
			// #
			if (bb.getOps() != 2) {
				return false;
			}
			if (!(bb.getOp(0) instanceof DUP)) {
				return false;
			}
			if (!(bb.getOp(1) instanceof JCND)) {
				return false;
			}
			final BB popBb = bb.getFalseOut().getEnd();
			if (popBb.getOps() != 1) {
				return false;
			}
			if (!(popBb.getOp(0) instanceof POP)) {
				return false;
			}
			final BB pushBb = popBb.getOut().getEnd();
			if (pushBb.getOps() != 2) {
				return false;
			}
			if (!(pushBb.getOp(0) instanceof PUSH)) {
				return false;
			}
			if (!(pushBb.getOp(1) instanceof INVOKE)) {
				return false;
			}
			final BB dupBb = pushBb.getOut().getEnd();
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
			final BB followBb = dupBb.getOut().getEnd();
			if (followBb != bb.getTrueOut().getEnd()) {
				return false;
			}
			// can just happen for JDK<5: replace . -> /
			final String classInfo = ((String) ((PUSH) pushBb.getOp(0)).getValue()).replace('.',
					'/');
			followBb.push(Types.convertLiteral(this.cfg.getDu().getT(Class.class), this.cfg.getDu()
					.getT(classInfo), this.cfg.getTd()));
			bb.removeOp(0);
			bb.removeOp(0);
			followBb.joinPredBb(bb);
			return true;
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Rewrite to class-literal didn't work!", e);
			return false;
		}
	}

	private boolean rewriteConditionalCompoundValue(final BB bb) {
		if (bb.getIns().size() < 2) {
			// this has 3 preds: a == null ? 0 : a.length() == 0 ? 0 : 1
			// even more preds possible with boolean conditionals
			return false;
		}
		for (final E in : bb.getIns()) {
			final E c_bb = in.relevantIn();
			if (c_bb == null) {
				continue;
			}
			final BB c = c_bb.getStart();
			if (c.getIns().size() != 1 || c.getStmts() > 0 || c.getTop() != 1) {
				continue;
			}
			final E a_c = c.getRelevantIn();
			final BB a = a_c.getStart();
			if (!a.isCondOrPreLoopHead()) {
				continue;
			}
			// now we have the potential compound head, go down again and identify pattern
			final E a_x = a_c.isCondTrue() ? a.getFalseOut() : a.getTrueOut();
			final BB x = a_x.getRelevantEnd();
			if (x.getIns().size() != 1 || x.getStmts() > 0 || x.getTop() != 1) {
				continue;
			}
			if (bb != x.getRelevantOut().getEnd()) {
				continue;
			}
			// This is a conditional compound value, example is A ? C : x:
			//
			// ...|...
			// ...A...
			// .t/.\f.
			// .C...x.
			// ..\./..
			// ...B...
			// ...|...

			Expression thenExpression = c.peek();
			Expression elseExpression = x.peek();
			Expression expression = ((IfStatement) a.removeFinalStmt()).getExpression();
			final Boolean thenBooleanConst = booleanFromLiteral(thenExpression);
			final Boolean elseBooleanConst = booleanFromLiteral(elseExpression);

			rewrite: if (thenBooleanConst != null && elseBooleanConst != null) {
				// expression: A ? true : false => A,
				// accept if one is BooleanLiteral - merging didn't work ;)
				if (a_c.isCondTrue() ^ thenBooleanConst) {
					expression = not(expression);
				}
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
					if (this.cfg.getTd().getVersion() >= 49) {
						LOGGER.warning("Unexpected class literal code with class$() in >= JVM 5 code!");
					}
					try {
						final String classInfo = ((StringLiteral) methodInvocation.arguments().get(
								0)).getLiteralValue();
						expression = Types.convertLiteral(this.cfg.getDu().getT(Class.class),
								this.cfg.getDu().getT(classInfo), this.cfg.getTd());
						break rewrite;
					} catch (final Exception e) {
						// rewrite to class literal didn't work
					}
				}
				// expressions: expression ? trueExpression : falseExpression
				final ConditionalExpression conditionalExpression = getAst()
						.newConditionalExpression();
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
			if (bb.getIns().size() > 2) {
				a.push(expression);
				a.setSucc(bb);
			} else {
				bb.joinPredBb(a);
				// push new conditional expression, here only "a ? true : false" as "a"
				bb.push(expression);
			}
			c.remove();
			x.remove();
			return true;
		}
		return false;
	}

	private boolean rewriteFieldInit(final BB bb, final F f, final Expression rightExpression) {
		// set local field, could be initializer
		if (f.check(AF.STATIC)) {
			if (!this.cfg.getMd().isInitializer()) {
				return false;
			}
		} else {
			if (!this.cfg.getMd().isConstructor()) {
				return false;
			}
			if (!(bb.peek() instanceof ThisExpression)) {
				return false;
			}
			// multiple constructors with different signatures possible, all of them
			// contain the same field initializer code after super() - simply overwrite
		}
		if (this.cfg.getStartBb() != bb || bb.getStmts() > 1) {
			return false;
		}
		if (bb.getStmts() == 1 && !(bb.getStmt(0) instanceof SuperConstructorInvocation)) {
			// initial super(<arguments>) is allowed
			return false;
		}
		// TODO this checks are not enough, we must assure that we don't use method
		// arguments here!!!
		if (((ClassT) f.getT()).check(AF.ENUM) && !this.cfg.getCu().check(DFlag.IGNORE_ENUM)) {
			if (f.check(AF.ENUM)) {
				// assignment to enum constant declaration
				if (!(rightExpression instanceof ClassInstanceCreation)) {
					LOGGER.warning("Assignment to enum field '" + f
							+ "' is no class instance creation!");
					return false;
				}
				final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) rightExpression;
				// first two arguments must be String (== field name) and int (ordinal)
				final List<Expression> arguments = classInstanceCreation.arguments();
				if (arguments.size() < 2) {
					LOGGER.warning("Class instance creation for enum field '" + f
							+ "' has less than 2 arguments!");
					return false;
				}
				if (!(arguments.get(0) instanceof StringLiteral)) {
					LOGGER.warning("Class instance creation for enum field '" + f
							+ "' must contain string literal as first parameter!");
					return false;
				}
				final String literalValue = ((StringLiteral) arguments.get(0)).getLiteralValue();
				if (!literalValue.equals(f.getName())) {
					LOGGER.warning("Class instance creation for enum field '"
							+ f
							+ "' must contain string literal equal to field name as first parameter!");
					return false;
				}
				if (!(arguments.get(1) instanceof NumberLiteral)) {
					LOGGER.warning("Class instance creation for enum field '" + f
							+ "' must contain number literal as first parameter!");
					return false;
				}
				final FD fd = this.cfg.getTd().getT().getF(f.getName(), f.getValueT()).getFd();
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
					enumConstantDeclaration.setAnonymousClassDeclaration(anonymousClassDeclaration);
					// normally contains one constructor, that calls a synthetic super
					// constructor with the enum class as additional last parameter,
					// this may contain field initializers, that we must keep,
					// so we can only remove the constructor in final merge (because
					// anonymous inner classes cannot hava visible Java constructor)
				}
				return true;
			}
			if ("$VALUES".equals(f.getName()) || "ENUM$VALUES".equals(f.getName())) {
				return true; // ignore such assignments completely
			}
		}
		if (f.check(AF.SYNTHETIC)) {
			if (this.cfg.getCu().check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
				return false; // not as field initializer
			}
			return true; // ignore such assignments completely
		}
		final FD fd = this.cfg.getTd().getT().getF(f.getName(), f.getValueT()).getFd();
		if (fd == null || !(fd.getFieldDeclaration() instanceof FieldDeclaration)) {
			return false;
		}
		try {
			((VariableDeclarationFragment) ((FieldDeclaration) fd.getFieldDeclaration())
					.fragments().get(0)).setInitializer(wrap(rightExpression, Priority.ASSIGNMENT));
			// TODO move anonymous TD to FD as child!!! important for ClassEditor
			// select, if fixed change ClassEditor#findDeclarationForJavaElement too
			if (!f.check(AF.STATIC)) {
				bb.pop();
			}
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Reewrite to field-initializer didn't work!", e);
			return false;
		}
		return true;
	}

	private boolean rewriteHandler(final BB bb) {
		if (!bb.isCatchHandler()) {
			return false;
		}
		// first operations are usually STRORE or POP (if exception not needed)
		final Op firstOp = bb.getOps() == 0 ? null : bb.getOp(0);
		String name = null;
		if (firstOp instanceof STORE) {
			bb.removeOp(0);
			final STORE cop = (STORE) firstOp;
			name = getVarName(cop.getReg(), cop.getPc() + 1);
		} else if (firstOp instanceof POP) {
			bb.removeOp(0);
			name = "e"; // TODO hmmm...free variable name needed...
		} else {
			LOGGER.warning("First operation in handler '" + firstOp + "' isn't STORE or POP: " + bb);
			name = "e"; // TODO hmmm...free variable name needed...
			bb.push(getAst().newSimpleName(name));
		}
		final T[] handlerTypes = (T[]) bb.getIns().get(0).getValue();
		final boolean isFinally = 1 == handlerTypes.length && null == handlerTypes[0];

		final TryStatement tryStatement = getAst().newTryStatement();
		if (!isFinally) {
			final CatchClause catchClause = getAst().newCatchClause();
			final SingleVariableDeclaration singleVariableDeclaration = getAst()
					.newSingleVariableDeclaration();
			singleVariableDeclaration.setName(getAst().newSimpleName(name));
			if (handlerTypes.length == 1) {
				singleVariableDeclaration.setType(Types.convertType(handlerTypes[0],
						this.cfg.getTd()));
			} else {
				// Multi-Catch
				final UnionType unionType = getAst().newUnionType();
				for (final T t : handlerTypes) {
					unionType.types().add(Types.convertType(t, this.cfg.getTd()));
				}
				singleVariableDeclaration.setType(unionType);
			}
			catchClause.setException(singleVariableDeclaration);
			tryStatement.catchClauses().add(catchClause);
		}
		bb.addStmt(tryStatement);
		return true;
	}

	private boolean rewritePushJcnd(final BB bb) {
		// in boolean compounds in JDK 1 & 2: PUSH true/false -> JCND
		if (bb.getOps() == 0 || !bb.isStackEmpty()) {
			return false;
		}
		final Op op = bb.getOp(0);
		if (!(op instanceof JCND)) {
			return false;
		}
		final CmpType cmpType = ((JCND) op).getCmpType();

		for (final E in : bb.getIns()) {
			final E c_bb = in.relevantIn();
			if (c_bb == null || !c_bb.isSequence()) {
				continue;
			}
			final BB a = c_bb.getStart();
			if (a.isStackEmpty()) {
				continue;
			}
			Boolean booleanConst = booleanFromLiteral(a.peek());
			if (booleanConst == null) {
				continue;
			}
			switch (cmpType) {
			case T_EQ:
				// "== 0" means "is false"
				booleanConst = !booleanConst;
				break;
			case T_NE:
				// "!= 0" means "is true"
				break;
			default:
				LOGGER.warning("Unknown cmp type '" + cmpType + "'!");
			}
			a.pop();
			a.setSucc(booleanConst ? bb.getTrueSucc() : bb.getFalseSucc());
			in.remove();
			return true;
		}
		return false;
	}

	private boolean rewriteStringAppend(final BB bb) {
		// method-invoke for StringBuffer.toString() or StringBuilder.toString()

		// jdk1.1.6:
		// new StringBuffer(String.valueOf(super.toString())).append(" TEST").toString()
		// jdk1.3:
		// new StringBuffer().append(super.toString()).append(" TEST").toString();
		// jdk1.5.0:
		// new StringBuilder().append(super.toString()).append(" TEST").toString()
		// Eclipse (constructor argument fail?):
		// new StringBuilder(String.valueOf(super.toString())).append(" TEST").toString()

		// ..."" at the beginning or end are handled very differently...
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
				// parentheses necessary for arithmetic add after string: "s" + (l1 + l2)
				// TODO System.out.println(((double) i + d) + s); ...must handle this here too for
				// JDK>=5 ...allready encountered append(String) here? must iterate from start?
				// where is the info in AST?
				if (OperatorPrecedence.priority(appendArgumentExpression) == Priority.ADD_SUB) {
					appendArgumentExpression = wrap(appendArgumentExpression, Priority.MULT_DIV);
				}
				if (stringExpression == null) {
					stringExpression = appendArgumentExpression;
				} else {
					stringExpression = newInfixExpression(InfixExpression.Operator.PLUS,
							appendArgumentExpression, stringExpression);
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
				// OK, no parentheses necessary for arithmetic add before string: l1 + l2 + "s"
				stringExpression = newInfixExpression(InfixExpression.Operator.PLUS,
						appendArgumentExpression, stringExpression != null ? stringExpression
								: getAst().newStringLiteral());
			}
			bb.pop();
			bb.push(stringExpression);
			return true;
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Rewrite to string-add didn't work!", e);
			return false;
		}
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
			final boolean handler = rewriteHandler(bb);
			if (!handler) {
				// TODO rewritePushJcnd() as last...or we should check if all incomings are bool
				// constants, not like "c ? true : a"
				while (rewriteBooleanCompound(bb) || rewriteConditionalCompoundValue(bb)
						|| rewritePushJcnd(bb)) {
					// merge superior BBs, multiple iterations possible, e.g.:
					// a == null ? 0 : a.length() == 0 ? 0 : 1
				}
			}
			// previous expressions merged into bb, now rewrite:
			if (!convertToHLLIntermediate(bb)) {
				// should never happen in forward mode
				// TODO can currently happen with exceptions, RETURN x is not in catch!
				LOGGER.warning("Stack underflow in '" + this.cfg + "':\n" + bb);
			}
		}
	}

}