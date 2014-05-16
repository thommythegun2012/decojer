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
		if (statements.size() > 0) {
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
		final BB falseSucc = head.getFalseSucc();
		final BB trueSucc = head.getTrueSucc();
		boolean negate = false;

		switch (cond.getKind()) {
		case IFNOT:
			ifStatement.setExpression(wrap(not(ifStatement.getExpression())));
			negate = true;
		case IF: {
			final List<Statement> subStatements = Lists.newArrayList();
			final BB firstBb = negate ? falseSucc : trueSucc;
			assert firstBb != null;
			transformSequence(cond, firstBb, subStatements);

			if (subStatements.size() == 1) {
				ifStatement.setThenStatement(subStatements.get(0));
			} else {
				final Block block = getAst().newBlock();
				block.statements().addAll(subStatements);
				ifStatement.setThenStatement(block);
			}
			return ifStatement;
		}
		case IFNOT_ELSE:
			ifStatement.setExpression(wrap(not(ifStatement.getExpression())));
			negate = true;
		case IF_ELSE: {
			if (falseSucc == trueSucc) {
				assert false; // is handled in control flow analysis
				return null;
			}
			{
				final List<Statement> subStatements = Lists.newArrayList();
				final BB firstBb = negate ? falseSucc : trueSucc;
				assert firstBb != null;
				transformSequence(cond, firstBb, subStatements);

				if (subStatements.size() == 1) {
					ifStatement.setThenStatement(subStatements.get(0));
				} else {
					final Block block = getAst().newBlock();
					block.statements().addAll(subStatements);
					ifStatement.setThenStatement(block);
				}
			}
			{
				final List<Statement> subStatements = Lists.newArrayList();
				final BB firstBb = negate ? trueSucc : falseSucc;
				assert firstBb != null;
				transformSequence(cond, firstBb, subStatements);

				if (subStatements.size() == 1) {
					ifStatement.setElseStatement(subStatements.get(0));
				} else {
					final Block block = getAst().newBlock();
					block.statements().addAll(subStatements);
					ifStatement.setElseStatement(block);
				}
			}
			return ifStatement;
		}
		default:
			log.warn(getM() + ": Unknown cond type '" + cond.getKind() + "'!");
			return null;
		}
	}

	@Nullable
	private Statement transformLoop(@Nonnull final Loop loop) {
		final BB head = loop.getHead();
		final BB last = loop.getLast();

		boolean negate = true;
		switch (loop.getKind()) {
		case WHILE:
			negate = false;
		case WHILENOT: {
			final IfStatement ifStatement = (IfStatement) head.getStmt(0);
			if (ifStatement == null) {
				assert false;
				return null;
			}
			final WhileStatement whileStatement = setOp(getAst().newWhileStatement(),
					getOp(ifStatement));

			final Expression expression = ifStatement.getExpression();
			whileStatement.setExpression(wrap(negate ? not(expression) : expression));

			final List<Statement> subStatements = Lists.newArrayList();
			final BB firstLoopBb = negate ? head.getFalseSucc() : head.getTrueSucc();
			assert firstLoopBb != null;
			if (!loop.isHead(firstLoopBb)) {
				transformSequence(loop, firstLoopBb, subStatements);
				if (subStatements.size() == 1) {
					whileStatement.setBody(subStatements.get(0));
				} else {
					final Block block = getAst().newBlock();
					block.statements().addAll(subStatements);
					whileStatement.setBody(block);
				}
			}
			return whileStatement;
		}
		case DO_WHILE:
			negate = false;
		case DO_WHILENOT: {
			final IfStatement ifStatement = (IfStatement) last.getFinalStmt();
			if (ifStatement == null) {
				assert false;
				return null;
			}
			final DoStatement doStatement = setOp(getAst().newDoStatement(), getOp(ifStatement));

			final List<Statement> subStatements = Lists.newArrayList();
			transformSequence(loop, head, subStatements);

			final Expression expression = ifStatement.getExpression();
			doStatement.setExpression(wrap(negate ? not(expression) : expression));

			// has always block
			((Block) doStatement.getBody()).statements().addAll(subStatements);
			return doStatement;
		}
		case ENDLESS: {
			// this while statement hasn't an operation, line number is before first statement,
			// do { ... } while(true); wouldn't change the entry line part, always use while(true)
			final WhileStatement whileStatement = getAst().newWhileStatement();

			whileStatement.setExpression(getAst().newBooleanLiteral(true));

			final List<Statement> subStatements = Lists.newArrayList();
			transformSequence(loop, head, subStatements);

			if (subStatements.size() == 1) {
				whileStatement.setBody(subStatements.get(0));
			} else {
				final Block block = getAst().newBlock();
				block.statements().addAll(subStatements);
				whileStatement.setBody(block);
			}
			return whileStatement;
		}
		default:
			log.warn(getM() + ": Unknown loop type '" + loop.getKind() + "'!");
			return null;
		}
	}

	/**
	 * Transform current BB from current struct (and following BB sequence in same struct) into a
	 * list of AST statements.
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
			// does BB leave current struct or enter a new sub struct?
			if (currentBbStruct != struct) {
				// check leaving with priority, can both happen in one BB
				for (Struct findStruct = struct; findStruct != null; findStruct = findStruct
						.getParent()) {
					if (findStruct.hasBreakTarget(currentBb)) {
						// leaving struct!
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
						log.warn(getM() + ": Struct leave in BB " + currentBb.getPc()
								+ " without regular follow encounter:\n" + struct);
						return;
					}
				}
				if (!currentBbStruct.isHead(currentBb)) {
					log.warn(getM() + ": Struct change in BB " + currentBb.getPc()
							+ " without regular follow or head encounter:\n" + struct);
					return;
				}
				// entering a new sub struct! enter this sub struct and come back...

				// multiple overlaying heads possible (e.g. endless-loop -> post-loop -> cond),
				// find topmost unused and use this as sub struct
				Struct subStruct = currentBbStruct;
				while (subStruct.getParent() != struct) {
					subStruct = subStruct.getParent();
					if (subStruct == null) {
						log.warn(getM() + ": Struct enter in BB " + currentBb.getPc()
								+ " without regular head encounter:\n" + struct);
						assert false;
						return;
					}
				}
				currentBb = transformStruct(subStruct, statements);
				continue;
			}
			// no struct change, but check loop and switch ends
			for (Struct findStruct = struct; findStruct != null; findStruct = findStruct
					.getParent()) {
				if (findStruct instanceof Loop) {
					final Loop loop = (Loop) findStruct;
					// The continue statement skips the current iteration of a
					// for, while or do-while loop. The unlabeled form skips
					// to the end of the innermost loop's body and evaluates the
					// boolean expression that controls the loop. A labeled
					// continue statement skips the current iteration of an
					// outer loop marked with the given label.
					if (loop.isHead(currentBb)) {
						if (struct != loop && !loop.isPost()) {
							// only from sub structure
							statements.add(getAst().newContinueStatement());
							return;
						}
						if (currentBb != firstBb) {
							// encountered loop head through direct backjump through sub sequence
							return;
						}
						// head == last possible for self loops => fall through to next checks...
					}
					if (!loop.isLast(currentBb)) {
						continue;
					}
					if (loop.isPost()) {
						if (struct == loop) {
							final int size = currentBb.getStmts() - 1;
							for (int i = 0; i < size; ++i) {
								statements.add(currentBb.getStmt(i));
							}
						} else {
							// only from sub structure
							statements.add(getAst().newContinueStatement());
						}
						return;
					}
					for (int i = 0; i < currentBb.getStmts(); ++i) {
						statements.add(currentBb.getStmt(i));
					}
					return;
				} else if (findStruct instanceof Switch) {
					final Switch findSwitch = (Switch) findStruct;
					if (findSwitch.isCase(currentBb)) {
						if (firstBb != currentBb) {
							// fall-through follow-case
							return;
						}
					}
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
			// follow back edges for continues
			currentBb = out.getEnd();
		}
	}

	@Nullable
	private BB transformStruct(@Nonnull final Struct struct,
			@Nonnull final List<Statement> statements) {
		if (!this.traversedStructs.add(struct)) {
			log.warn(getM() + ": Cannot transform struct twice:\n" + struct);
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
							switchCase.setExpression(newLiteral(T.CHAR, caseValue, getCfg().getT(),
									op));
						} else if (caseValue instanceof String) {
							switchCase.setExpression(newLiteral(
									getCfg().getDu().getT(String.class), caseValue,
									getCfg().getT(), op));
						} else if (caseValue instanceof F) {
							switchCase.setExpression(newSimpleName(((F) caseValue).getName(),
									getAst()));
						} else {
							switchCase.setExpression(newLiteral(T.INT, caseValue, getCfg().getT(),
									op));
						}
					}
					switchStatement.statements().add(switchCase);
				}
				final List<Statement> subStatements = Lists.newArrayList();
				transformSequence(switchStruct, out.getEnd(), subStatements);
				switchStatement.statements().addAll(subStatements);
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
		transformSequence(sync, sequenceOut.getEnd(), syncStatements);

		final BB follow = sync.getFollow(); // can happen with final throw in sync block
		if (follow != null && follow.getStmt(0) instanceof SynchronizedStatement) {
			follow.removeStmt(0);
		}
		return synchronizedStatement;
	}

}