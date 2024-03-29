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

import static org.decojer.cavaj.utils.Expressions.getOp;
import static org.decojer.cavaj.utils.Expressions.newLiteral;
import static org.decojer.cavaj.utils.Expressions.newSimpleName;
import static org.decojer.cavaj.utils.Expressions.newType;
import static org.decojer.cavaj.utils.Expressions.not;
import static org.decojer.cavaj.utils.Expressions.setOp;
import static org.decojer.cavaj.utils.Expressions.wrap;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.structs.Block;
import org.decojer.cavaj.model.code.structs.Catch;
import org.decojer.cavaj.model.code.structs.Cond;
import org.decojer.cavaj.model.code.structs.Loop;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.code.structs.Switch;
import org.decojer.cavaj.model.code.structs.Sync;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Transformer: Structured CFG to Java Control Flow Statements ASTs.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrControlFlowStmts {

	/**
	 * Transform CFG.
	 *
	 * @param cfg
	 *            CFG
	 */
	public static void transform(@Nonnull final CFG cfg) {
		new TrControlFlowStmts(cfg).transform();
	}

	@Nullable
	private static BB transformBranching(@Nonnull final E e,
			@Nonnull final List<Statement> statements) {
		final Statement branchingStmt = e.getBranchingStmt();
		if (branchingStmt == null) {
			return e.isBack() ? null : e.getEnd();
		}
		statements.add(branchingStmt);
		return null;
	}

	@Getter(AccessLevel.PROTECTED)
	@Nonnull
	private final CFG cfg;

	private final Set<Struct> traversedStructs = Sets.newHashSet();

	private TrControlFlowStmts(@Nonnull final CFG cfg) {
		this.cfg = cfg;
	}

	@Nonnull
	private AST getAst() {
		return getCfg().getCu().getAst();
	}

	@Nonnull
	private M getM() {
		return getCfg().getM();
	}

	public void transform() {
		if (getCfg().getBlock() == null) {
			// can happen, e.g. if synthethic
			return;
		}
		this.traversedStructs.clear(); // possible in debug mode

		final List<Statement> statements = getCfg().getBlock().statements();
		statements.clear(); // possible in debug mode

		final BB startBb = getCfg().getStartBb();
		assert startBb != null;
		transformSequence(null, startBb, statements);

		// remove final empty return statement from method
		if (!statements.isEmpty()) {
			final Object object = statements.get(statements.size() - 1);
			if (object instanceof ReturnStatement
					&& ((ReturnStatement) object).getExpression() == null) {
				((ReturnStatement) object).delete();
			}
		}
	}

	private AssertStatement transformAssert(@Nonnull final E out,
			@Nonnull final Expression expression) {
		// has to be done after Control Flow Analysis! Could be a manually thrown AssertionError in
		// combination with other structs (like directly behind pre-loop)
		if (getCfg().getCu().check(DFlag.IGNORE_ASSERT)) {
			return null;
		}
		// if (!DecTestAsserts.$assertionsDisabled && (l1 > 0L ? l1 >= l2 : l1 > l2))
		// throw new AssertionError("complex expression " + l1 - l2);
		final BB bb = out.getEnd();
		if (bb.getStmts() != 1) {
			return null;
		}
		final Statement throwStmt = bb.getStmt(0);
		if (!(throwStmt instanceof ThrowStatement)) {
			return null;
		}
		final Expression exceptionExpression = ((ThrowStatement) throwStmt).getExpression();
		if (!(exceptionExpression instanceof ClassInstanceCreation)) {
			return null;
		}
		final ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) exceptionExpression;
		final Type type = classInstanceCreation.getType();
		if (!(type instanceof SimpleType)) {
			return null;
		}
		final Name name = ((SimpleType) type).getName();
		if (!(name instanceof SimpleName)) {
			return null;
		}
		if (!"AssertionError".equals(((SimpleName) name).getIdentifier())) {
			return null;
		}
		final Expression messageExpression;
		final List<Expression> arguments = classInstanceCreation.arguments();
		if (arguments.isEmpty()) {
			messageExpression = null;
		} else {
			if (arguments.size() > 1) {
				return null;
			}
			messageExpression = arguments.get(0);
		}
		Expression assertExpression = expression;
		if (expression instanceof InfixExpression) {
			final InfixExpression infixExpression = (InfixExpression) expression;
			if (infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
				final Expression leftOperand = infixExpression.getLeftOperand();
				if (leftOperand instanceof QualifiedName
						&& leftOperand.toString().endsWith(".$assertionsDisabled")) {
					assertExpression = infixExpression.getRightOperand();
					assert assertExpression != null;
				}
			}
		} else if (expression instanceof QualifiedName
				&& ((QualifiedName) expression).toString().endsWith(".$assertionsDisabled")) {
			assertExpression = getAst().newBooleanLiteral(false);
			assert assertExpression != null;
		}
		final AssertStatement assertStatement = setOp(getAst().newAssertStatement(),
				getOp(throwStmt));
		assertStatement.setExpression(wrap(not(assertExpression)));
		if (messageExpression != null) {
			assertStatement.setMessage(wrap(messageExpression));
		}
		return assertStatement;
	}

	@Nullable
	private Statement transformBlock(@Nonnull final Block block) {
		// always create block, is important because it's either labeled or a declaration boundary
		final org.eclipse.jdt.core.dom.Block blockStatement = getAst().newBlock();
		final List<Statement> blockStatements = blockStatement.statements();
		assert blockStatements != null;
		transformSequence(block, block.getHead(), blockStatements);
		return blockStatement;
	}

	@Nullable
	private TryStatement transformCatch(@Nonnull final Catch catchStruct) {
		final BB head = catchStruct.getHead();
		final TryStatement tryStatement = getAst().newTryStatement();
		final List<Statement> statements = tryStatement.getBody().statements();
		assert statements != null;
		transformSequence(catchStruct, head, statements);
		// now add handlers
		for (final E catchE : head.getOuts()) {
			if (!catchE.isCatch()) {
				continue;
			}
			final BB handler = catchE.getEnd();
			final T[] catchTypes = (T[]) catchE.getValue();
			assert catchTypes != null;
			if (!catchStruct.hasHandler(catchTypes, handler)) {
				continue;
			}
			// extract exception name from temporary throwable declaration, don't remove in CFG
			final Statement throwableDeclaration = handler.getStmt(0);
			assert throwableDeclaration instanceof VariableDeclarationStatement;
			final List<VariableDeclarationFragment> throwableDeclarationFragments = ((VariableDeclarationStatement) throwableDeclaration)
					.fragments();
			assert throwableDeclarationFragments.size() == 1;
			final VariableDeclarationFragment throwableDeclarationFragment = throwableDeclarationFragments
					.get(0);
			final SimpleName exceptionName = throwableDeclarationFragment.getName();
			assert exceptionName != null;

			if (catchE.isFinally()) {
				// finally handler
				tryStatement.setFinally(getAst().newBlock());
				final List<Statement> finallyStatements = tryStatement.getFinally().statements();
				assert finallyStatements != null;
				transformSequence(catchStruct, handler, finallyStatements);
				// remove temporary throwable declaration from finally block, could be nested
				Statement firstStatement = finallyStatements.get(0);
				if (firstStatement instanceof TryStatement) {
					firstStatement = (Statement) ((TryStatement) firstStatement).getBody()
							.statements().get(0);
				}
				if (firstStatement instanceof VariableDeclarationStatement) {
					firstStatement.delete();
				}
				// remove final throws from finally block, should never be nested
				if (!finallyStatements.isEmpty() && finallyStatements
						.get(finallyStatements.size() - 1) instanceof ThrowStatement) {
					finallyStatements.remove(finallyStatements.size() - 1);
				}
				continue;
			}
			// normal typed catch handler
			final CatchClause catchClause = getAst().newCatchClause();

			final SingleVariableDeclaration singleVariableDeclaration = getAst()
					.newSingleVariableDeclaration();
			singleVariableDeclaration.setName((SimpleName) wrap(exceptionName));
			if (catchTypes.length == 1) {
				final T handlerType = catchTypes[0];
				assert handlerType != null;
				singleVariableDeclaration.setType(newType(handlerType, getM()));
			} else {
				// Multi-Catch
				final UnionType unionType = getAst().newUnionType();
				for (final T t : catchTypes) {
					assert t != null;
					unionType.types().add(newType(t, getM()));
				}
				singleVariableDeclaration.setType(unionType);
			}
			catchClause.setException(singleVariableDeclaration);

			tryStatement.catchClauses().add(catchClause);

			final List<Statement> handlerStatements = catchClause.getBody().statements();
			assert handlerStatements != null;
			transformSequence(catchStruct, handler, handlerStatements);
			// remove temporary throwable declaration from catch
			if (handlerStatements.isEmpty()) {
				log.warn(
						getM() + ": Missing temporary throwable declaration in handler statements for BB"
								+ handler.getPc() + ":\n" + catchStruct);
			} else {
				handlerStatements.remove(0);
			}
		}
		return tryStatement;
	}

	@Nullable
	private Statement transformCond(@Nonnull final Cond cond) {
		final BB head = cond.getHead();

		final IfStatement ifStatement = (IfStatement) head.getFinalStmt();
		if (ifStatement == null) {
			assert false;
			return null;
		}
		final Expression condExpression = ifStatement.getExpression();
		if (condExpression == null) {
			assert false;
			return null;
		}
		final E falseOut = head.getFalseOut();
		assert falseOut != null;
		final E trueOut = head.getTrueOut();
		assert trueOut != null;
		boolean negate = false;

		switch (cond.getKind()) {
		case IFNOT:
			ifStatement.setExpression(wrap(not(condExpression)));
			negate = true;
			// fall through
		case IF: {
			final AssertStatement assertStmt = transformAssert(negate ? falseOut : trueOut,
					condExpression);
			if (assertStmt != null) {
				return assertStmt;
			}
			ifStatement.setThenStatement(transformSequence(cond, negate ? falseOut : trueOut));
			return ifStatement;
		}
		case IFNOT_ELSE:
			ifStatement.setExpression(wrap(not(condExpression)));
			negate = true;
			// fall through
		case IF_ELSE:
			ifStatement.setThenStatement(transformSequence(cond, negate ? falseOut : trueOut));
			ifStatement.setElseStatement(transformSequence(cond, negate ? trueOut : falseOut));
			return ifStatement;
		default:
			log.warn(getM() + ": Unknown cond type '" + cond.getKind() + "'!");
			return null;
		}
	}

	@Nullable
	private Statement transformLoop(@Nonnull final Loop loop) {
		final BB head = loop.getHead();
		boolean negate = true;
		switch (loop.getKind()) {
		case WHILE:
			negate = false;
		case WHILENOT: {
			final IfStatement ifStatement = (IfStatement) head.getStmt(0);

			final WhileStatement whileStatement = setOp(getAst().newWhileStatement(),
					getOp(ifStatement));
			final Expression expression = ifStatement.getExpression();
			assert expression != null;
			whileStatement.setExpression(wrap(negate ? not(expression) : expression));

			final E out = negate ? head.getFalseOut() : head.getTrueOut();
			assert out != null;
			whileStatement.setBody(transformSequence(loop, out));
			return whileStatement;
		}
		case DO_WHILE:
			negate = false;
		case DO_WHILENOT: {
			final BB last = loop.getLast();
			final IfStatement ifStatement = (IfStatement) last.getFinalStmt();
			assert ifStatement != null;
			final DoStatement doStatement = setOp(getAst().newDoStatement(), getOp(ifStatement));

			final Expression expression = ifStatement.getExpression();
			assert expression != null;
			doStatement.setExpression(wrap(negate ? not(expression) : expression));

			final List<Statement> statements = ((org.eclipse.jdt.core.dom.Block) doStatement
					.getBody()).statements();
			assert statements != null;
			transformSequence(loop, head, statements);
			return doStatement;
		}
		case ENDLESS: {
			// this while statement hasn't an operation, line number is before first statement,
			// do { ... } while(true); wouldn't change the entry line part, always use while(true)
			final WhileStatement whileStatement = getAst().newWhileStatement();

			whileStatement.setExpression(getAst().newBooleanLiteral(true));

			whileStatement.setBody(transformSequence(loop, head));
			return whileStatement;
		}
		default:
			log.warn(getM() + ": Unknown loop type '" + loop.getKind() + "'!");
			return null;
		}
	}

	private Statement transformSequence(@Nullable final Struct struct, @Nonnull final BB firstBb) {
		final List<Statement> statements = Lists.newArrayList();
		transformSequence(struct, firstBb, statements);
		if (statements.isEmpty()) {
			return getAst().newEmptyStatement();
		}
		if (statements.size() == 1) {
			return statements.get(0);
		}
		final org.eclipse.jdt.core.dom.Block block = getAst().newBlock();
		block.statements().addAll(statements);
		return block;
	}

	/**
	 * Transform sequence of BBs in given struct into a list of AST statements, starting with given
	 * first BB and ending with struct change.
	 *
	 * Should only directly be called from outside for {@code struct.isHead(firstBb)}, else use
	 * {@link #transformSequence(Struct, E, List)}.
	 *
	 * @param struct
	 *            struct
	 * @param firstBb
	 *            first BB in struct
	 * @param statements
	 *            statement block
	 */
	private void transformSequence(@Nullable final Struct struct, @Nonnull final BB firstBb,
			@Nonnull final List<Statement> statements) {
		BB bb = firstBb;
		while (bb != null) {
			final Struct bbStruct = bb.getStruct();
			// ***************************
			// * check for struct change *
			// ***************************
			if (bbStruct != struct) {
				// BB leaves current struct or enters a new sub struct
				// (Java-structs are single-entry but multi-exit: must enter via struct head)
				if (bbStruct == null) {
					// ++++++++++++++++++++++++++
					// + leaving current struct +
					// ++++++++++++++++++++++++++
					return;
				}
				if (bbStruct.hasHead(bb)) {
					// entering new sub struct, but first also check leaving with priority, can
					// happen simultaneously with entering
					if (struct != null && !bbStruct.hasAncestor(struct)) {
						// ++++++++++++++++++++++++++
						// + leaving current struct +
						// ++++++++++++++++++++++++++
						return;
					}
					// +++++++++++++++++++++++++++++
					// + entering a new sub struct +
					// +++++++++++++++++++++++++++++
					// enter this sub struct and come back, continue loop afterwards:
					// multiple overlaying heads possible (e.g. endless-loop -> post-loop -> cond),
					// find topmost unused and use this as sub struct
					Struct subStruct = bbStruct;
					while (subStruct.getParent() != struct) {
						subStruct = subStruct.getParent();
						if (subStruct == null) {
							log.warn(getM() + ": Struct enter in BB" + bb.getPc()
									+ " without regular head encounter:\n" + struct);
							// assert false;
							return;
						}
					}
					bb = transformStruct(subStruct, statements);
					continue;
				}
				if (struct != null && struct.hasAncestor(bbStruct)) {
					// ++++++++++++++++++++++++++
					// + leaving current struct +
					// ++++++++++++++++++++++++++
					return;
				}
				log.warn(getM() + ": Irregular struct change in BB" + bb.getPc() + ":\n" + struct);
				return;
			}
			// ********************
			// * no struct change *
			// ********************
			// but check loop and switch ends
			if (struct instanceof Loop) {
				final Loop findLoop = (Loop) struct;
				// The continue statement skips the current iteration of a for, while or
				// do-while loop. The unlabeled form skips to the end of the innermost loop's
				// body and evaluates the boolean expression that controls the loop. A labeled
				// continue statement skips the current iteration of an outer loop marked with
				// the given label.
				if (findLoop.hasLast(bb) && findLoop.isPost()) {
					if (struct != findLoop) {
						// only from sub structure, e.g. embedded conditional contains head and
						// last
						statements.add(getAst().newContinueStatement());
					} else {
						final int size = bb.getStmts() - 1;
						for (int i = 0; i < size; ++i) {
							statements.add(bb.getStmt(i));
						}
					}
					return;
				}
			} else if (struct instanceof Switch) {
				final Switch findSwitch = (Switch) struct;
				if (findSwitch.getCaseValues(bb) != null && firstBb != bb) {
					// fall-through follow-case
					return;
				}
			}
			// simple sequence block, 0 statements possible with empty GOTO BBs
			for (int i = 0; i < bb.getStmts(); ++i) {
				statements.add(bb.getStmt(i));
			}
			final E out = bb.getSequenceOut();
			if (out == null) {
				return;
			}
			bb = transformBranching(out, statements);
		}
	}

	@Nonnull
	private Statement transformSequence(@Nullable final Struct struct, @Nonnull final E firstE) {
		final List<Statement> statements = Lists.newArrayList();
		transformSequence(struct, firstE, statements);
		if (statements.isEmpty()) {
			final EmptyStatement ret = getAst().newEmptyStatement();
			assert ret != null;
			return ret;
		}
		if (statements.size() == 1) {
			final Statement ret = statements.get(0);
			assert ret != null;
			return ret;
		}
		final org.eclipse.jdt.core.dom.Block block = getAst().newBlock();
		block.statements().addAll(statements);
		return block;
	}

	/**
	 * Transform first edge from current struct (and following BB sequence in same struct) into a
	 * list of AST statements.
	 *
	 * @param struct
	 *            current struct
	 * @param firstE
	 *            first edge in current struct
	 * @param statements
	 *            statement block
	 */
	private void transformSequence(@Nullable final Struct struct, @Nonnull final E firstE,
			@Nonnull final List<Statement> statements) {
		final BB end = transformBranching(firstE, statements);
		if (end != null) {
			transformSequence(struct, end, statements);
			return;
		}
	}

	@Nullable
	private BB transformStruct(@Nonnull final Struct struct,
			@Nonnull final List<Statement> statements) {
		if (!this.traversedStructs.add(struct)) {
			assert false : "Cannot transform struct twice:\n" + struct;
			log.warn(getM() + ": Cannot transform struct twice:\n" + struct);
			return null;
		}
		// decompile sub structure into a statement
		Statement structStatement;
		if (struct instanceof Block) {
			structStatement = transformBlock((Block) struct);
		} else if (struct instanceof Catch) {
			structStatement = transformCatch((Catch) struct);
		} else if (struct instanceof Loop) {
			structStatement = transformLoop((Loop) struct);
		} else {
			// possible statements before following structs in BB
			final int size = struct.getHead().getStmts() - 1;
			for (int i = 0; i < size; ++i) {
				statements.add(struct.getHead().getStmt(i));
			}
			if (struct instanceof Cond) {
				// condition or assert
				structStatement = transformCond((Cond) struct);
			} else if (struct instanceof Switch) {
				structStatement = transformSwitch((Switch) struct);
			} else if (struct instanceof Sync) {
				structStatement = transformSync((Sync) struct);
			} else {
				log.warn(getM() + ": Unknown struct:\n" + struct);
				structStatement = null;
			}
		}
		if (structStatement == null) {
			log.warn(getM() + ": Couldn't decompile struct:\n" + struct);
		} else {
			final String label = struct.getLabel();
			if (label != null) {
				assert!(structStatement instanceof LabeledStatement);
				final LabeledStatement labeledStatement = getAst().newLabeledStatement();
				labeledStatement.setLabel(newSimpleName(label, getAst()));
				labeledStatement.setBody(structStatement);
				structStatement = labeledStatement;
			}
			statements.add(structStatement);
		}
		return struct.getFollow();
	}

	@Nullable
	private Statement transformSwitch(@Nonnull final Switch switchStruct) {
		switch (switchStruct.getKind()) {
		case NO_DEFAULT:
		case WITH_DEFAULT: {
			final BB head = switchStruct.getHead();
			final SwitchStatement switchStatement = (SwitchStatement) head.getFinalStmt();
			if (switchStatement == null) {
				assert false;
				return null;
			}
			final Op op = getOp(switchStatement);

			for (final E out : head.getOuts()) {
				if (!out.isSwitchCase()) {
					continue;
				}
				boolean defaultAdded = false; // prevent [null, null] - double defaults
				final Object value = out.getValue();
				if (!(value instanceof Object[])) {
					assert false;
					continue;
				}
				for (final Object caseValue : (Object[]) value) {
					if (out.isSwitchDefault() && switchStruct.getKind() == Switch.Kind.NO_DEFAULT) {
						continue;
					}
					final SwitchCase switchCase = setOp(getAst().newSwitchCase(), op);
					if (caseValue == null) {
						// necessary: expression initialized to null for default case
						if (defaultAdded) {
							continue;
						}
						switchCase.setExpression(null);
						defaultAdded = true;
					} else {
						if (caseValue instanceof Character) {
							switchCase.setExpression(
									newLiteral(T.CHAR, caseValue, getCfg().getM(), op));
						} else if (caseValue instanceof String) {
							switchCase.setExpression(newLiteral(getCfg().getDu().getT(String.class),
									caseValue, getCfg().getM(), op));
						} else if (caseValue instanceof F) {
							switchCase.setExpression(
									newSimpleName(((F) caseValue).getName(), getAst()));
						} else {
							switchCase.setExpression(
									newLiteral(T.INT, caseValue, getCfg().getM(), op));
						}
					}
					switchStatement.statements().add(switchCase);
				}
				final List<Statement> switchStatements = switchStatement.statements();
				assert switchStatements != null;
				transformSequence(switchStruct, out, switchStatements);
			}
			// remove final break statement from final switch-case
			final Object object = switchStatement.statements()
					.get(switchStatement.statements().size() - 1);
			if (object instanceof BreakStatement) {
				((BreakStatement) object).delete();
			}
			return switchStatement;
		}
		default:
			log.warn(getM() + ": Unknown switch type '" + switchStruct.getKind() + "'!");
			return null;
		}
	}

	@Nullable
	private Statement transformSync(@Nonnull final Sync sync) {
		final BB head = sync.getHead();
		final SynchronizedStatement synchronizedStatement = (SynchronizedStatement) head
				.getFinalStmt();
		if (synchronizedStatement == null) {
			assert false;
			return null;
		}
		final E sequenceOut = head.getSequenceOut();
		if (sequenceOut == null || sequenceOut.isBack()) {
			assert false;
			return null;
		}
		final List<Statement> syncStatements = synchronizedStatement.getBody().statements();
		assert syncStatements != null;
		transformSequence(sync, sequenceOut, syncStatements);
		return synchronizedStatement;
	}

}