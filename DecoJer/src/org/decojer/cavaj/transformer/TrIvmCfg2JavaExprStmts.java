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

import static org.decojer.cavaj.tool.Expressions.newInfixExpression;
import static org.decojer.cavaj.tool.Expressions.newPrefixExpression;
import static org.decojer.cavaj.tool.Expressions.wrap;
import static org.decojer.cavaj.tool.OperatorPrecedence.priority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.DataType;
import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.ALOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.AND;
import org.decojer.cavaj.model.vm.intermediate.operations.ARRAYLENGTH;
import org.decojer.cavaj.model.vm.intermediate.operations.ASTORE;
import org.decojer.cavaj.model.vm.intermediate.operations.CHECKCAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
import org.decojer.cavaj.model.vm.intermediate.operations.CONVERT;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.DUP;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.INC;
import org.decojer.cavaj.model.vm.intermediate.operations.INSTANCEOF;
import org.decojer.cavaj.model.vm.intermediate.operations.INVOKE;
import org.decojer.cavaj.model.vm.intermediate.operations.JCMP;
import org.decojer.cavaj.model.vm.intermediate.operations.JCND;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.MONITOR;
import org.decojer.cavaj.model.vm.intermediate.operations.MUL;
import org.decojer.cavaj.model.vm.intermediate.operations.NEG;
import org.decojer.cavaj.model.vm.intermediate.operations.NEW;
import org.decojer.cavaj.model.vm.intermediate.operations.NEWARRAY;
import org.decojer.cavaj.model.vm.intermediate.operations.OR;
import org.decojer.cavaj.model.vm.intermediate.operations.POP;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.PUT;
import org.decojer.cavaj.model.vm.intermediate.operations.REM;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;
import org.decojer.cavaj.tool.Types;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;

/**
 * Transform CFG IVM to HLL Expression Statements.
 * 
 * @author André Pankraz
 */
public class TrIvmCfg2JavaExprStmts {

	private final static Logger LOGGER = Logger
			.getLogger(TrIvmCfg2JavaExprStmts.class.getName());

	public static void transform(final CFG cfg) {
		new TrIvmCfg2JavaExprStmts(cfg).transform();
		cfg.calculatePostorder();
	}

	public static void transform(final TD td) {
		// no parallelism! 2 shared instance variables: code and nextPc
		final List<BD> bds = td.getBds();
		for (int i = 0; i < bds.size(); ++i) {
			final BD bd = bds.get(i);
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			if (cfg == null) {
				continue;
			}
			transform(cfg);
		}
	}

	private final CFG cfg;

	private TrIvmCfg2JavaExprStmts(final CFG cfg) {
		this.cfg = cfg;
	}

	@SuppressWarnings("unchecked")
	private boolean convertToHLLIntermediate(final BB bb) {
		while (bb.getOperationsSize() != 0) {
			final Operation operation = bb.getOperation(0);
			if (operation.getInStackSize() > bb.getExpressionsSize()) {
				return false;
			}
			bb.removeOperation(0);
			Statement statement = null;
			switch (operation.getOpcode()) {
			case Opcode.ADD: {
				final ADD op = (ADD) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.PLUS, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.ALOAD: {
				final ALOAD op = (ALOAD) operation;
				final ArrayAccess arrayAccess = getAst().newArrayAccess();
				arrayAccess.setIndex(wrap(bb.popExpression()));
				arrayAccess.setArray(wrap(bb.popExpression(),
						priority(arrayAccess)));
				bb.pushExpression(arrayAccess);
			}
				break;
			case Opcode.AND: {
				final AND op = (AND) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.AND, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.ARRAYLENGTH: {
				final ARRAYLENGTH op = (ARRAYLENGTH) operation;
				final Expression expression = bb.popExpression();
				if (expression instanceof Name) {
					// annotationsVisible.length
					bb.pushExpression(getAst().newQualifiedName(
							(Name) wrap(expression),
							getAst().newSimpleName("length")));
				} else {
					// FieldAccess or MethodInvocation:
					// this.code.length, getInterfaces().length
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(expression,
							priority(fieldAccess)));
					fieldAccess.setName(getAst().newSimpleName("length"));
					bb.pushExpression(fieldAccess);
				}
			}
				break;
			case Opcode.ASTORE: {
				final ASTORE op = (ASTORE) operation;
				final Expression rightExpression = bb.popExpression();
				final Expression indexExpression = bb.popExpression();
				final Expression arrayRefExpression = bb.popExpression();
				if (arrayRefExpression instanceof ArrayCreation) {
					final ArrayCreation arrayCreation = (ArrayCreation) arrayRefExpression;
					if (arrayCreation.getInitializer() == null) {
						final ArrayInitializer arrayInitializer = getAst()
								.newArrayInitializer();
						arrayCreation.setInitializer(arrayInitializer);
						// TODO remove dimension? multi?
					}
					arrayCreation.getInitializer().expressions()
							.add(wrap(rightExpression));
				} else {
					final Assignment assignment = getAst().newAssignment();
					// TODO a = a +/- 1 => a++ / a--
					// TODO a = a <op> expr => a <op>= expr
					assignment.setRightHandSide(wrap(rightExpression,
							priority(assignment)));
					final ArrayAccess arrayAccess = getAst().newArrayAccess();
					arrayAccess.setArray(wrap(arrayRefExpression,
							priority(arrayAccess)));
					arrayAccess.setIndex(wrap(indexExpression));
					assignment.setLeftHandSide(arrayAccess);
					// inline assignment, DUP(_X1) -> PUT
					if (bb.getExpressionsSize() > 0
							&& bb.peekExpression() == rightExpression) {
						bb.popExpression();
						bb.pushExpression(assignment);
					} else {
						statement = getAst().newExpressionStatement(assignment);
					}
				}
			}
				break;
			case Opcode.CHECKCAST: {
				final CHECKCAST op = (CHECKCAST) operation;
				final CastExpression castExpression = getAst()
						.newCastExpression();
				castExpression.setType(Types.convertType(op.getT(), getTd()));
				castExpression.setExpression(wrap(bb.popExpression(),
						priority(castExpression)));
				bb.pushExpression(castExpression);
			}
				break;
			case Opcode.CMP: {
				final CMP op = (CMP) operation;
				// pseudo expression for following JCND, not really the correct
				// answer for -1, 0, 1
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.LESS_EQUALS,
						bb.popExpression(), bb.popExpression()));
			}
				break;
			case Opcode.CONVERT: {
				final CONVERT op = (CONVERT) operation;
				final CastExpression castExpression = getAst()
						.newCastExpression();
				// TODO same code like in NEWARRAY creation
				final PrimitiveType.Code typeCode = new PrimitiveType.Code[] {
						null, null, null, null, PrimitiveType.BOOLEAN,
						PrimitiveType.CHAR, PrimitiveType.FLOAT,
						PrimitiveType.DOUBLE, PrimitiveType.BYTE,
						PrimitiveType.SHORT, PrimitiveType.INT,
						PrimitiveType.LONG }[op.getToType()];
				castExpression.setType(getAst().newPrimitiveType(typeCode));
				castExpression.setExpression(wrap(bb.popExpression(),
						priority(castExpression)));
				bb.pushExpression(castExpression);
			}
				break;
			case Opcode.DIV: {
				final DIV op = (DIV) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.DIVIDE, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.DUP: {
				final DUP op = (DUP) operation;
				switch (op.getDupType()) {
				case DUP.T_DUP:
					bb.pushExpression(bb.peekExpression());
					break;
				case DUP.T_DUP_X1: {
					final Expression e1 = bb.popExpression();
					final Expression e2 = bb.popExpression();
					bb.pushExpression(e1);
					bb.pushExpression(e2);
					bb.pushExpression(e1);
				}
					break;
				case DUP.T_DUP_X2: {
					final Expression e1 = bb.popExpression();
					final Expression e2 = bb.popExpression();
					final Expression e3 = bb.popExpression();
					bb.pushExpression(e1);
					bb.pushExpression(e3);
					bb.pushExpression(e2);
					bb.pushExpression(e1);
				}
					break;
				case DUP.T_DUP2: {
					final Expression e1 = bb.popExpression();
					final Expression e2 = bb.popExpression();
					bb.pushExpression(e2);
					bb.pushExpression(e1);
					bb.pushExpression(e2);
					bb.pushExpression(e1);
				}
					break;
				case DUP.T_DUP2_X1: {
					final Expression e1 = bb.popExpression();
					final Expression e2 = bb.popExpression();
					final Expression e3 = bb.popExpression();
					bb.pushExpression(e2);
					bb.pushExpression(e1);
					bb.pushExpression(e3);
					bb.pushExpression(e2);
					bb.pushExpression(e1);
				}
					break;
				case DUP.T_DUP2_X2: {
					final Expression e1 = bb.popExpression();
					final Expression e2 = bb.popExpression();
					final Expression e3 = bb.popExpression();
					final Expression e4 = bb.popExpression();
					bb.pushExpression(e2);
					bb.pushExpression(e1);
					bb.pushExpression(e4);
					bb.pushExpression(e3);
					bb.pushExpression(e2);
					bb.pushExpression(e1);
				}
					break;
				default:
					LOGGER.warning("Unknown dup type '" + op.getDupType()
							+ "'!");
				}
			}
				break;
			case Opcode.GET: {
				final GET op = (GET) operation;
				final F f = op.getF();
				if (f.checkAf(AF.STATIC)) {
					final Name name = getAst().newQualifiedName(
							getTd().newTypeName(f.getT().getName()),
							getAst().newSimpleName(f.getName()));
					bb.pushExpression(name);
				} else {
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(bb.popExpression(),
							priority(fieldAccess)));
					fieldAccess.setName(getAst().newSimpleName(f.getName()));
					bb.pushExpression(fieldAccess);
				}
				break;
			}
			case Opcode.GOTO: {
				// not really necessary, but important for
				// 1) correct opPc blocks
				// 2) line numbers

				// TODO put line number anywhere?
				// remember as pseudo statement? but problem with boolean ops
			}
				break;
			case Opcode.INC: {
				final INC op = (INC) operation;
				op.getConstValue();
			}
				break;
			case Opcode.INSTANCEOF: {
				final INSTANCEOF op = (INSTANCEOF) operation;
				final InstanceofExpression instanceofExpression = getAst()
						.newInstanceofExpression();
				instanceofExpression.setLeftOperand(wrap(bb.popExpression(),
						priority(instanceofExpression)));
				instanceofExpression.setRightOperand(Types.convertType(
						op.getT(), getTd()));
				bb.pushExpression(instanceofExpression);
			}
				break;
			case Opcode.INVOKE: {
				final INVOKE op = (INVOKE) operation;
				final M m = op.getM();

				// read method invokation arguments
				final List<Expression> arguments = new ArrayList<Expression>();
				for (int i = 0; i < m.getParamTs().length; ++i) {
					arguments.add(wrap(bb.popExpression()));
				}
				Collections.reverse(arguments);

				final Expression methodExpression;
				switch (op.getFunctionType()) {
				case INVOKE.T_SPECIAL: {
					if ("<init>".equals(m.getName())) {
						methodExpression = null;
						final Expression expression = bb.popExpression();
						if (expression instanceof ThisExpression) {
							final SuperConstructorInvocation superConstructorInvocation = getAst()
									.newSuperConstructorInvocation();
							superConstructorInvocation.arguments().addAll(
									arguments);
							bb.addStatement(superConstructorInvocation);
							break;
						}
						if (expression instanceof ClassInstanceCreation) {
							final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) expression;
							classInstanceCreation.arguments().addAll(arguments);
							// normally there was a DUP in advance, don't use:
							// basicBlock.pushExpression(classInstanceCreation);
							break;
						}
						LOGGER.warning("Constructor method '<init> expects expression class 'ThisExpression' or 'ClassInstanceCreation' but is '"
								+ expression.getClass()
								+ "' with value: "
								+ expression);
						break;
					}
					final Expression expression = bb.popExpression();

					if (expression instanceof ThisExpression) {
						final SuperMethodInvocation superMethodInvocation = getAst()
								.newSuperMethodInvocation();
						superMethodInvocation.setName(getAst().newSimpleName(
								m.getName()));
						superMethodInvocation.arguments().addAll(arguments);

						methodExpression = superMethodInvocation;
						break;
					}

					methodExpression = null;
				}
					break;
				case INVOKE.T_INTERFACE:
				case INVOKE.T_VIRTUAL: {
					final MethodInvocation methodInvocation = getAst()
							.newMethodInvocation();
					methodInvocation.setName(getAst()
							.newSimpleName(m.getName()));
					methodInvocation.arguments().addAll(arguments);
					methodInvocation.setExpression(wrap(bb.popExpression(),
							priority(methodInvocation)));
					methodExpression = methodInvocation;
				}
					break;
				case INVOKE.T_STATIC: {
					final MethodInvocation methodInvocation = getAst()
							.newMethodInvocation();
					methodInvocation.setName(getAst()
							.newSimpleName(m.getName()));
					methodInvocation.arguments().addAll(arguments);
					methodInvocation.setExpression(getTd().newTypeName(
							m.getT().getName()));
					methodExpression = methodInvocation;
				}
					break;
				default:
					methodExpression = null;
				}
				if (methodExpression != null) {
					final T returnType = m.getReturnT();
					if (void.class.getName().equals(returnType.getName())) {
						statement = getAst().newExpressionStatement(
								methodExpression);
					} else {
						bb.pushExpression(methodExpression);
					}
				}
			}
				break;
			case Opcode.JCMP: {
				final JCMP op = (JCMP) operation;
				// invert all operators and switch out edge predicates
				final InfixExpression.Operator operator;
				switch (op.getCmpType()) {
				case CompareType.T_EQ:
					operator = InfixExpression.Operator.EQUALS;
					break;
				case CompareType.T_GE:
					operator = InfixExpression.Operator.GREATER_EQUALS;
					break;
				case CompareType.T_GT:
					operator = InfixExpression.Operator.GREATER;
					break;
				case CompareType.T_LE:
					operator = InfixExpression.Operator.LESS_EQUALS;
					break;
				case CompareType.T_LT:
					operator = InfixExpression.Operator.LESS;
					break;
				case CompareType.T_NE:
					operator = InfixExpression.Operator.NOT_EQUALS;
					break;
				default:
					LOGGER.warning("Unknown cmp type '" + op.getCmpType()
							+ "'!");
					operator = null;
				}
				statement = getAst().newIfStatement();
				((IfStatement) statement).setExpression(newInfixExpression(
						operator, bb.popExpression(), bb.popExpression()));
			}
				break;
			case Opcode.JCND: {
				final JCND op = (JCND) operation;
				Expression expression = bb.popExpression();
				// check preceding CMP
				if (expression instanceof InfixExpression
						&& ((InfixExpression) expression).getOperator() == InfixExpression.Operator.LESS_EQUALS) {
					expression = wrap(expression);
					// preceding compare expression (CMP result: -1 / 0 / 1)
					final InfixExpression.Operator operator;
					switch (op.getCmpType()) {
					case CompareType.T_EQ:
						operator = InfixExpression.Operator.EQUALS;
						break;
					case CompareType.T_GE:
						operator = InfixExpression.Operator.GREATER_EQUALS;
						break;
					case CompareType.T_GT:
						operator = InfixExpression.Operator.GREATER;
						break;
					case CompareType.T_LE:
						operator = InfixExpression.Operator.LESS_EQUALS;
						break;
					case CompareType.T_LT:
						operator = InfixExpression.Operator.LESS;
						break;
					case CompareType.T_NE:
						operator = InfixExpression.Operator.NOT_EQUALS;
						break;
					default:
						LOGGER.warning("Unknown cmp type '" + op.getCmpType()
								+ "'!");
						operator = null;
					}
					((InfixExpression) expression).setOperator(operator);
				} else {
					// TODO check if boolean type or number type!
					// "!a" or "a == 0"?
					// boolean:
					switch (op.getCmpType()) {
					case CompareType.T_EQ:
						// "== 0" means "is false"
						expression = newPrefixExpression(
								PrefixExpression.Operator.NOT, expression);
						break;
					case CompareType.T_NE:
						// "!= 0" means "is true"
						break;
					default:
						// TODO check more strange cases?
						LOGGER.warning("TODO expression may simply not be a boolean! - Unknown cmp type '"
								+ op.getCmpType() + "' for boolean expression!");
					}
				}
				statement = getAst().newIfStatement();
				((IfStatement) statement).setExpression(expression);
			}
				break;
			case Opcode.LOAD: {
				final LOAD op = (LOAD) operation;

				if ("this".equals(op.getVarName())) {
					bb.pushExpression(getAst().newThisExpression());
				} else {
					bb.pushExpression(getAst().newSimpleName(op.getVarName()));
				}
			}
				break;
			case Opcode.MONITOR: {
				final MONITOR op = (MONITOR) operation;
				bb.popExpression();
			}
				break;
			case Opcode.MUL: {
				final MUL op = (MUL) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.TIMES, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.NEG: {
				final NEG op = (NEG) operation;
				bb.pushExpression(newPrefixExpression(
						PrefixExpression.Operator.MINUS, bb.popExpression()));
			}
				break;
			case Opcode.NEW: {
				final NEW op = (NEW) operation;
				final ClassInstanceCreation classInstanceCreation = getAst()
						.newClassInstanceCreation();
				classInstanceCreation.setType(Types.convertType(op.getT(),
						getTd()));
				// classInstanceCreation.setAnonymousClassDeclaration(decl);
				bb.pushExpression(classInstanceCreation);
			}
				break;
			case Opcode.NEWARRAY: {
				final NEWARRAY op = (NEWARRAY) operation;
				final ArrayCreation arrayCreation = getAst().newArrayCreation();
				arrayCreation.setType(getAst().newArrayType(
						Types.convertType(op.getT(), getTd())));
				arrayCreation.dimensions().add(bb.popExpression());
				bb.pushExpression(arrayCreation);
			}
				break;
			case Opcode.OR: {
				final OR op = (OR) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.OR, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.POP: {
				final POP op = (POP) operation;
				switch (op.getPopType()) {
				case POP.T_POP: {
					final Expression expression = bb.popExpression();
					statement = getAst().newExpressionStatement(expression);
					break;
				}
				case POP.T_POP2: {
					final Expression expression = bb.popExpression();
					statement = getAst().newExpressionStatement(expression);
					// CHECK pop another??? happens when? another statement?
					bb.popExpression();
					break;
				}
				default:
					LOGGER.warning("Unknown pop type '" + op.getPopType()
							+ "'!");
				}
			}
				break;
			case Opcode.PUSH: {
				final PUSH op = (PUSH) operation;
				final Expression expr;
				switch (op.getType()) {
				case DataType.T_AREF:
					if (op.getValue() == null) {
						expr = getAst().newNullLiteral();
					} else {
						LOGGER.warning("No not null aref constants possible!");
						expr = null;
					}
					break;
				case DataType.T_DOUBLE:
					expr = getAst().newNumberLiteral(
							Double.toString((Double) op.getValue()));
					break;
				case DataType.T_INT:
					expr = getAst().newNumberLiteral(
							Integer.toString((Integer) op.getValue()));
					break;
				case DataType.T_LONG:
					expr = getAst().newNumberLiteral(
							Long.toString((Long) op.getValue()));
					break;
				case DataType.T_STRING:
					expr = getAst().newStringLiteral();
					((StringLiteral) expr).setLiteralValue((String) op
							.getValue());
					break;
				case DataType.T_CLASS:
					expr = getAst().newTypeLiteral();
					((TypeLiteral) expr).setType(Types.convertType(
							(T) op.getValue(), getTd()));
					break;
				default:
					LOGGER.warning("Unknown data type '" + op.getType() + "'!");
					expr = null;
				}
				if (expr != null) {
					bb.pushExpression(expr);
				}
			}
				break;
			case Opcode.PUT: {
				final PUT op = (PUT) operation;
				final Expression rightExpression = bb.popExpression();
				final Assignment assignment = getAst().newAssignment();
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				assignment.setRightHandSide(wrap(rightExpression,
						priority(assignment)));
				switch (op.getFunctionType()) {
				case PUT.T_DYNAMIC:
					final FieldAccess fieldAccess = getAst().newFieldAccess();
					fieldAccess.setExpression(wrap(bb.popExpression(),
							priority(fieldAccess)));
					fieldAccess.setName(getAst().newSimpleName(
							op.getFieldrefName()));
					assignment.setLeftHandSide(fieldAccess);
					break;
				case PUT.T_STATIC:
					final Name name = getAst().newQualifiedName(
							getTd().newTypeName(op.getFieldrefClassName()),
							getAst().newSimpleName(op.getFieldrefName()));
					assignment.setLeftHandSide(name);
					break;
				default:
					LOGGER.warning("Unknown function type '"
							+ op.getFunctionType() + "'!");
				}
				// inline assignment, DUP(_X1) -> PUT
				if (bb.getExpressionsSize() > 0
						&& bb.peekExpression() == rightExpression) {
					bb.popExpression();
					bb.pushExpression(assignment);
				} else {
					statement = getAst().newExpressionStatement(assignment);
				}
			}
				break;
			case Opcode.REM: {
				final REM op = (REM) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.REMAINDER, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.RETURN: {
				final RETURN op = (RETURN) operation;
				final ReturnStatement returnStatement = getAst()
						.newReturnStatement();
				if (op.getType() != DataType.T_VOID) {
					returnStatement.setExpression(wrap(bb.popExpression()));
				}
				statement = returnStatement;
			}
				break;
			case Opcode.STORE: {
				final STORE op = (STORE) operation;

				final Expression rightExpression = bb.popExpression();
				final Assignment assignment = getAst().newAssignment();
				// TODO a = a +/- 1 => a++ / a--
				// TODO a = a <op> expr => a <op>= expr
				assignment.setRightHandSide(wrap(rightExpression,
						priority(assignment)));

				assignment.setLeftHandSide(getAst().newSimpleName(
						op.getVarName()));
				// inline assignment, DUP -> STORE
				if (bb.getExpressionsSize() > 0
						&& bb.peekExpression() == rightExpression) {
					bb.popExpression();
					bb.pushExpression(assignment);
				} else {
					statement = getAst().newExpressionStatement(assignment);
				}
			}
				break;
			case Opcode.SUB: {
				final SUB op = (SUB) operation;
				bb.pushExpression(newInfixExpression(
						InfixExpression.Operator.MINUS, bb.popExpression(),
						bb.popExpression()));
			}
				break;
			case Opcode.SWAP: {
				final Expression e1 = bb.popExpression();
				final Expression e2 = bb.popExpression();
				bb.pushExpression(e1);
				bb.pushExpression(e2);
			}
				break;
			case Opcode.SWITCH: {
				final SWITCH op = (SWITCH) operation;
				final SwitchStatement switchStatement = getAst()
						.newSwitchStatement();
				switchStatement.setExpression(wrap(bb.popExpression()));
				statement = switchStatement;
			}
				break;
			case Opcode.THROW: {
				final THROW op = (THROW) operation;
				final ThrowStatement throwStatement = getAst()
						.newThrowStatement();
				throwStatement.setExpression(wrap(bb.popExpression()));
				statement = throwStatement;
			}
				break;
			case Opcode.XOR: {
				final XOR op = (XOR) operation;
				final Expression expression = bb.popExpression();
				// "a ^ -1" => "~a"
				if (expression instanceof NumberLiteral
						&& ((NumberLiteral) expression).getToken().equals("-1")) {
					bb.pushExpression(newPrefixExpression(
							PrefixExpression.Operator.COMPLEMENT,
							bb.popExpression()));
				} else {
					bb.pushExpression(newInfixExpression(
							InfixExpression.Operator.XOR, expression,
							bb.popExpression()));
				}
			}
				break;
			default:
				throw new RuntimeException(
						"Unknown intermediate vm operation '" + operation
								+ "'!");
			}
			if (statement != null) {
				bb.addStatement(statement);
			}
		}
		return true;
	}

	private AST getAst() {
		return getCu().getAst();
	}

	private CFG getCfg() {
		return this.cfg;
	}

	private CU getCu() {
		return getTd().getCu();
	}

	private MD getMd() {
		return getCfg().getMd();
	}

	private TD getTd() {
		return getMd().getTd();
	}

	private void transform() {
		final List<BB> postorderedBbs = this.cfg.getPostorderedBbs();
		boolean changed = false;
		// for all nodes in postorder
		for (int i = 0; i < postorderedBbs.size(); ++i) {
			final BB bb = postorderedBbs.get(i);
			if (bb == null) {
				continue;
			}
			// initially convert all operations to statements and
			// expressions till possible stack underflow
			convertToHLLIntermediate(bb);
			if (bb.getSuccBbs().size() != 2) {
				// must be 2-way
				continue;
			}
			final List<Statement> stmts = bb.getStatements();
			if (stmts.size() == 0) {
				// no statements converted yet, try later
				continue;
			}
			final Object statement = stmts.get(stmts.size() - 1);
			if (!(statement instanceof IfStatement)) {
				continue;
			}
			// this can only be the last statement -> all operations converted
			BB trueBb = bb.getSuccBb(Boolean.TRUE);
			BB falseBb = bb.getSuccBb(Boolean.FALSE);
			// check for short-circuit compound boolean expression structure
			if (trueBb.getSuccBbs().size() == 2
					&& trueBb.getStatements().size() == 1
					&& trueBb.getPredBbs().size() == 1) {
				// another IfStatement in true direction
				final Object statement2 = trueBb.getStatements().get(0);
				if (!(statement2 instanceof IfStatement)) {
					continue;
				}
				final BB trueBb2 = trueBb.getSuccBb(Boolean.TRUE);
				final BB falseBb2 = trueBb.getSuccBb(Boolean.FALSE);
				if (falseBb == trueBb2) {
					// !A || B

					// ....|..
					// ....A..
					// ..t/.\f
					// ...B..|
					// .f/.\t|
					// ./...\|
					// F.....T

					// rewrite AST
					final Expression leftExpression = ((IfStatement) statement)
							.getExpression();
					((IfStatement) statement).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					final Expression rightExpression = ((IfStatement) statement2)
							.getExpression();
					((IfStatement) statement2).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					((IfStatement) statement).setExpression(newInfixExpression(
							InfixExpression.Operator.CONDITIONAL_OR,
							rightExpression,
							newPrefixExpression(PrefixExpression.Operator.NOT,
									leftExpression)));
					// rewrite CFG
					trueBb.remove();
					bb.setSuccValue(trueBb2, Boolean.TRUE);
					bb.addSucc(falseBb2, Boolean.FALSE);
					// for conditional expression stage
					trueBb = trueBb2;
					falseBb = falseBb2;
					changed = true;
				} else if (falseBb == falseBb2) {
					// A && B

					// ..|....
					// ..A....
					// f/.\t..
					// |..B...
					// |f/.\t.
					// |/...\.
					// F.....T

					// rewrite AST
					final Expression leftExpression = ((IfStatement) statement)
							.getExpression();
					((IfStatement) statement).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					final Expression rightExpression = ((IfStatement) statement2)
							.getExpression();
					((IfStatement) statement2).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					((IfStatement) statement).setExpression(newInfixExpression(
							InfixExpression.Operator.CONDITIONAL_AND,
							rightExpression, leftExpression));
					// rewrite CFG
					trueBb.remove();
					bb.addSucc(trueBb2, Boolean.TRUE);
					// for conditional expression stage
					trueBb = trueBb2;
					changed = true;
				}
			} else if (falseBb.getSuccBbs().size() == 2
					&& falseBb.getStatements().size() == 1
					&& falseBb.getPredBbs().size() == 1) {
				// another IfStatement in false direction
				final Object statement2 = falseBb.getStatements().get(0);
				if (!(statement2 instanceof IfStatement)) {
					continue;
				}
				final BB trueBb2 = falseBb.getSuccBb(Boolean.TRUE);
				final BB falseBb2 = falseBb.getSuccBb(Boolean.FALSE);
				if (trueBb == trueBb2) {
					// A || B

					// ....|..
					// ....A..
					// ..f/.\t
					// ...B..|
					// .f/.\t|
					// ./...\|
					// F.....T

					// rewrite AST
					final Expression leftExpression = ((IfStatement) statement)
							.getExpression();
					((IfStatement) statement).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					final Expression rightExpression = ((IfStatement) statement2)
							.getExpression();
					((IfStatement) statement2).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					((IfStatement) statement).setExpression(newInfixExpression(
							InfixExpression.Operator.CONDITIONAL_OR,
							rightExpression, leftExpression));
					// rewrite CFG
					falseBb.remove();
					bb.addSucc(falseBb2, Boolean.FALSE);
					// for conditional expression stage
					falseBb = falseBb2;
					changed = true;
				} else if (trueBb == falseBb2) {
					// !A && B

					// ..|....
					// ..A....
					// t/.\f..
					// |..B...
					// |f/.\t.
					// |/...\.
					// F.....T

					// rewrite AST
					final Expression leftExpression = ((IfStatement) statement)
							.getExpression();
					((IfStatement) statement).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					final Expression rightExpression = ((IfStatement) statement2)
							.getExpression();
					((IfStatement) statement2).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					((IfStatement) statement).setExpression(newInfixExpression(
							InfixExpression.Operator.CONDITIONAL_AND,
							rightExpression,
							newPrefixExpression(PrefixExpression.Operator.NOT,
									leftExpression)));
					// rewrite CFG
					falseBb.remove();
					bb.setSuccValue(falseBb2, Boolean.FALSE);
					bb.addSucc(trueBb2, Boolean.TRUE);
					// for conditional expression stage
					trueBb = trueBb2;
					falseBb = falseBb2;
					changed = true;
				}
			}
			// check for conditional expression structure
			if (trueBb.getPredBbs().size() == 1
					&& falseBb.getPredBbs().size() == 1
					&& trueBb.isExpression() && falseBb.isExpression()) {
				final Collection<BB> trueSuccessors = trueBb.getSuccBbs();
				if (trueSuccessors.size() != 1) {
					continue;
				}
				final Collection<BB> falseSuccessors = falseBb.getSuccBbs();
				if (falseSuccessors.size() != 1) {
					continue;
				}
				final BB trueBb2 = trueSuccessors.iterator().next();
				final BB falseBb2 = falseSuccessors.iterator().next();
				if (trueBb2 != falseBb2) {
					continue;
				}
				if (trueBb2.getPredBbs().size() != 2) {
					continue;
				}
				final Expression trueExpression = trueBb.peekExpression();
				final Expression falseExpression = falseBb.peekExpression();
				if (trueExpression instanceof NumberLiteral
						&& falseExpression instanceof NumberLiteral) {
					Expression expression = ((IfStatement) statement)
							.getExpression();
					((IfStatement) statement).setExpression(getAst()
							.newBooleanLiteral(false)); // delete parent
					if (((NumberLiteral) trueExpression).getToken().equals("0")) {
						expression = newPrefixExpression(
								PrefixExpression.Operator.NOT, expression);
					}
					// is conditional expression, modify graph
					// remove IfStatement
					stmts.remove(stmts.size() - 1);
					// push new conditional expression
					// TODO here only "a ? true : false" as "a"
					bb.pushExpression(expression);
					// copy successor values to bbNode
					bb.copyContent(trueBb2);
					// again convert operations, stack underflow might be solved
					changed = true;
					// first preserve successors...
					trueBb2.moveSuccBbs(bb);
					// then remove basic blocks
					trueBb.remove();
					falseBb.remove();
					trueBb2.remove();
				}
			}
			if (changed) {
				changed = false;
				--i;
			}
		}
	}

}