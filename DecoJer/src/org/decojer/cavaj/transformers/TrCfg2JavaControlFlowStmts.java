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
import static org.decojer.cavaj.utils.Expressions.not;
import static org.decojer.cavaj.utils.Expressions.setOp;
import static org.decojer.cavaj.utils.Expressions.wrap;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.ops.Op;
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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Transformer: Structured CFG to Java Control Flow Statements ASTs.
 *
 * @author André Pankraz
 */
@Slf4j
public final class TrCfg2JavaControlFlowStmts {

	/**
	 * Transform CFG.
	 *
	 * @param cfg
	 *            CFG
	 */
	public static void transform(@Nonnull final CFG cfg) {
		new TrCfg2JavaControlFlowStmts(cfg).transform();
	}

	@Getter(AccessLevel.PROTECTED)
	@Nonnull
	private final CFG cfg;

	private final Set<Struct> traversedStructs = Sets.newHashSet();

	private TrCfg2JavaControlFlowStmts(@Nonnull final CFG cfg) {
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

		// remove final empty return statement
		if (!statements.isEmpty()) {
			final Object object = statements.get(statements.size() - 1);
			if (object instanceof ReturnStatement
					&& ((ReturnStatement) object).getExpression() == null) {
				((ReturnStatement) object).delete();
			}
		}
	}

	@Nullable
	private IfStatement transformCatch(@Nonnull final Catch catchStruct) {
		log.warn(getM() + ": TODO: " + catchStruct);
		// final BB head = catchStruct.getHead();
		return null;
	}

	@Nullable
	private IfStatement transformCond(@Nonnull final Cond cond) {
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
		case IF:
			ifStatement.setThenStatement(transformSequence(cond, negate ? falseOut : trueOut));
			return ifStatement;
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
	private BB transformContinue(@Nullable final Struct struct, @Nonnull final E e,
			@Nonnull final List<Statement> statements) {
		if (!e.isBack()) {
			return e.getEnd();
		}
		if (!(struct instanceof Loop) || !((Loop) struct).isHead(e.getEnd())
				|| !((Loop) struct).isLast(e.getStart())) {
			statements.add(getAst().newContinueStatement());
		}
		return null;
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
			assert ifStatement != null;

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

			final List<Statement> doWhileStatements = ((Block) doStatement.getBody()).statements();
			assert doWhileStatements != null;
			transformSequence(loop, head, doWhileStatements);
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
		final Block block = getAst().newBlock();
		block.statements().addAll(statements);
		return block;
	}

	/**
	 * Transform first BB from current struct (and following BB sequence in same struct) into a list
	 * of AST statements.
	 *
	 * Should only directly be called from outside for {@code struct.isHead(firstBb)}, else use
	 * {@link #transformSequence(Struct, E, List)}.
	 *
	 * @param struct
	 *            current struct
	 * @param firstBb
	 *            first BB in current struct
	 * @param statements
	 *            statement block
	 */
	private void transformSequence(@Nullable final Struct struct, @Nonnull final BB firstBb,
			@Nonnull final List<Statement> statements) {
		BB currentBb = firstBb; // iterate over BB
		while (currentBb != null) {
			final Struct currentBbStruct = currentBb.getStruct();
			// ***************************
			// * check for struct change *
			// ***************************
			if (currentBbStruct != struct) {
				// BB leaves current struct or enters a new sub struct:
				// check leaving with priority, can happen together with entering in one BB
				for (Struct findStruct = struct; findStruct != null; findStruct = findStruct
						.getParent()) {
					if (findStruct.hasBreakTarget(currentBb)) {
						// ++++++++++++++++++++++++++
						// + leaving current struct +
						// ++++++++++++++++++++++++++
						// an unlabeled break statement terminates the innermost loop or switch
						// statement, but a labeled break terminates any outer struct
						if (findStruct.getLabel() != null) {
							final BreakStatement breakStatement = getAst().newBreakStatement();
							breakStatement.setLabel(newSimpleName(findStruct.getLabel(), getAst()));
							statements.add(breakStatement);
						} else if (findStruct instanceof Loop) {
							statements.add(getAst().newBreakStatement());
						} else if (findStruct instanceof Switch) {
							// final case-break in switches is removed later
							statements.add(getAst().newBreakStatement());
						}
						return;
					}
					if (currentBbStruct == findStruct.getParent()) {
						// ++++++++++++++++++++++++++
						// + leaving current struct +
						// ++++++++++++++++++++++++++
						if (currentBbStruct instanceof Loop) {
							final Loop loop = (Loop) currentBbStruct;
							if (loop.isPost() ? loop.isLast(currentBb) : loop.isHead(currentBb)) {
								assert !loop.isHead(currentBb) : "should only find forward continues";
								statements.add(getAst().newContinueStatement());
								return;
							}
						}
						log.warn(getM() + ": Struct leave in BB " + currentBb.getPc()
								+ " without regular follow encounter:\n" + struct);
						return;
					}
				}
				// +++++++++++++++++++++++++++++
				// + entering a new sub struct +
				// +++++++++++++++++++++++++++++
				// Java is struct-single-entry/-multi-exit: must enter via struct head
				if (currentBbStruct == null || !currentBbStruct.isHead(currentBb)) {
					log.warn(getM() + ": Struct change in BB " + currentBb.getPc()
							+ " without regular follow or head encounter:\n" + struct);
					return;
				}
				// enter this sub struct and come back:
				// multiple overlaying heads possible (e.g. endless-loop -> post-loop -> cond),
				// find topmost unused and use this as sub struct
				Struct subStruct = currentBbStruct;
				while (subStruct.getParent() != struct) {
					subStruct = subStruct.getParent();
					if (subStruct == null) {
						log.warn(getM() + ": Struct enter in BB " + currentBb.getPc()
								+ " without regular head encounter:\n" + struct);
						// assert false;
						return;
					}
				}
				currentBb = transformStruct(subStruct, statements);
				continue;
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
				if (findLoop.isLast(currentBb) && findLoop.isPost()) {
					if (struct != findLoop) {
						// only from sub structure, e.g. embedded conditional contains head and
						// last
						statements.add(getAst().newContinueStatement());
					} else {
						final int size = currentBb.getStmts() - 1;
						for (int i = 0; i < size; ++i) {
							statements.add(currentBb.getStmt(i));
						}
					}
					return;
				}
			} else if (struct instanceof Switch) {
				final Switch findSwitch = (Switch) struct;
				if (findSwitch.isCase(currentBb) && firstBb != currentBb) {
					// fall-through follow-case
					return;
				}
			}
			// simple sequence block, 0 statements possible with empty GOTO BBs
			for (int i = 0; i < currentBb.getStmts(); ++i) {
				statements.add(currentBb.getStmt(i));
			}
			final E out = currentBb.getSequenceOut();
			if (out == null) {
				return;
			}
			currentBb = transformContinue(struct, out, statements);
		}
	}

	private Statement transformSequence(@Nullable final Struct struct, @Nonnull final E firstE) {
		final List<Statement> statements = Lists.newArrayList();
		transformSequence(struct, firstE, statements);
		if (statements.isEmpty()) {
			return getAst().newEmptyStatement();
		}
		if (statements.size() == 1) {
			return statements.get(0);
		}
		final Block block = getAst().newBlock();
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
		final BB end = transformContinue(struct, firstE, statements);
		if (end != null) {
			transformSequence(struct, end, statements);
			return;
		}
	}

	@Nullable
	private BB transformStruct(@Nonnull final Struct struct,
			@Nonnull final List<Statement> statements) {
		if (!this.traversedStructs.add(struct)) {
			log.warn(getM() + ": Cannot transform struct twice:\n" + struct);
			assert false;
			return null;
		}
		// decompile sub structure into a statement
		Statement structStatement;
		if (struct instanceof Catch) {
			structStatement = transformCatch((Catch) struct);
		} else if (struct instanceof Loop) {
			structStatement = transformLoop((Loop) struct);
		} else {
			// possible statements before cond in BB
			final int size = struct.getHead().getStmts() - 1;
			for (int i = 0; i < size; ++i) {
				statements.add(struct.getHead().getStmt(i));
			}
			if (struct instanceof Cond) {
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
							switchCase.setExpression(newLiteral(T.CHAR, caseValue, getCfg().getM(),
									op));
						} else if (caseValue instanceof String) {
							switchCase.setExpression(newLiteral(
									getCfg().getDu().getT(String.class), caseValue,
									getCfg().getM(), op));
						} else if (caseValue instanceof F) {
							switchCase.setExpression(newSimpleName(((F) caseValue).getName(),
									getAst()));
						} else {
							switchCase.setExpression(newLiteral(T.INT, caseValue, getCfg().getM(),
									op));
						}
					}
					switchStatement.statements().add(switchCase);
				}
				final List<Statement> switchStatements = switchStatement.statements();
				assert switchStatements != null;
				transformSequence(switchStruct, out, switchStatements);
			}
			// remove final break
			final Object object = switchStatement.statements().get(
					switchStatement.statements().size() - 1);
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
		if (sequenceOut == null) {
			assert false;
			return null;
		}
		final List<Statement> syncStatements = synchronizedStatement.getBody().statements();
		assert syncStatements != null;
		transformSequence(sync, sequenceOut, syncStatements);

		final BB follow = sync.getFollow(); // can happen with final throw in sync block
		if (follow != null && follow.getStmt(0) instanceof SynchronizedStatement) {
			follow.removeStmt(0);
		}
		return synchronizedStatement;
	}

}