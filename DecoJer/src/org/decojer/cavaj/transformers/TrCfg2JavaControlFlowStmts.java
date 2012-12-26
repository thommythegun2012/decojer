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

import static org.decojer.cavaj.utils.Expressions.not;
import static org.decojer.cavaj.utils.Expressions.wrap;

import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.structs.Catch;
import org.decojer.cavaj.model.code.structs.Cond;
import org.decojer.cavaj.model.code.structs.Loop;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.code.structs.Switch;
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
import org.eclipse.jdt.core.dom.WhileStatement;

import com.google.common.collect.Lists;

/**
 * Transformer: Structured CFG to Java Control Flow Statements ASTs.
 * 
 * Ignore final empty return statements.
 * 
 * @author André Pankraz
 */
public final class TrCfg2JavaControlFlowStmts {

	private final static Logger LOGGER = Logger.getLogger(TrCfg2JavaControlFlowStmts.class
			.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrCfg2JavaControlFlowStmts(cfg).transform();
	}

	private final CFG cfg;

	private TrCfg2JavaControlFlowStmts(final CFG cfg) {
		this.cfg = cfg;
	}

	private AST getAst() {
		return this.cfg.getCu().getAst();
	}

	private void log(final String message) {
		LOGGER.warning(this.cfg.getMd() + ": " + message);
	}

	public void transform() {
		if (this.cfg.getBlock() == null) {
			// can happen, e.g. if synthethic
			return;
		}
		final List<Statement> statements = this.cfg.getBlock().statements();
		statements.clear(); // possible in debug mode

		transformSequence(null, this.cfg.getStartBb(), statements);

		// remove final return
		if (statements.size() > 0) {
			final Object object = statements.get(statements.size() - 1);
			if (object instanceof ReturnStatement
					&& ((ReturnStatement) object).getExpression() == null) {
				((ReturnStatement) object).delete();
			}
		}
	}

	private IfStatement transformCatch(final Catch catchStruct) {
		log("TODO: " + catchStruct);
		// final BB head = catchStruct.getHead();
		return null;
	}

	private IfStatement transformCond(final Cond cond) {
		final BB head = cond.getHead();

		final IfStatement ifStatement = (IfStatement) head.getFinalStmt();

		final BB falseSucc = head.getFalseSucc();
		final BB trueSucc = head.getTrueSucc();
		boolean negate = false;

		switch (cond.getKind()) {
		case IFNOT:
			ifStatement.setExpression(wrap(not(ifStatement.getExpression())));
			negate = true;
		case IF: {
			final List<Statement> subStatements = Lists.newArrayList();
			transformSequence(cond, negate ? falseSucc : trueSucc, subStatements);

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
			{
				final List<Statement> subStatements = Lists.newArrayList();
				transformSequence(cond, negate ? falseSucc : trueSucc, subStatements);

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
				transformSequence(cond, negate ? trueSucc : falseSucc, subStatements);

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
			log("Unknown cond type '" + cond.getKind() + "'!");
			return null;
		}
	}

	private Statement transformLoop(final Loop loop) {
		final BB head = loop.getHead();
		final BB last = loop.getLast();

		boolean negate = true;
		switch (loop.getKind()) {
		case WHILE:
			negate = false;
		case WHILENOT: {
			final WhileStatement whileStatement = getAst().newWhileStatement();

			final Expression expression = ((IfStatement) head.getStmt(0)).getExpression();
			whileStatement.setExpression(wrap(negate ? not(expression) : expression));

			final List<Statement> subStatements = Lists.newArrayList();
			transformSequence(loop, negate ? head.getFalseSucc() : head.getTrueSucc(),
					subStatements);

			if (subStatements.size() == 1) {
				whileStatement.setBody(subStatements.get(0));
			} else {
				final Block block = getAst().newBlock();
				block.statements().addAll(subStatements);
				whileStatement.setBody(block);
			}
			return whileStatement;
		}
		case DO_WHILE:
			negate = false;
		case DO_WHILENOT: {
			final DoStatement doStatement = getAst().newDoStatement();

			final List<Statement> subStatements = Lists.newArrayList();
			transformSequence(loop, head, subStatements);

			final Expression expression = ((IfStatement) last.getFinalStmt()).getExpression();
			doStatement.setExpression(wrap(negate ? not(expression) : expression));

			// has always block
			((Block) doStatement.getBody()).statements().addAll(subStatements);
			return doStatement;
		}
		case ENDLESS: {
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
			log("Unknown loop type '" + loop.getKind() + "'!");
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
	 *            first BB
	 * @param statements
	 *            statement block
	 */
	private void transformSequence(final Struct struct, final BB firstBb,
			final List<Statement> statements) {
		BB bb = firstBb;
		int endlessHack = 0; // HACK
		while (bb != null && endlessHack++ < 100) {
			assert endlessHack < 100;

			if (struct != bb.getStruct()) {
				// leaving struct or entering a new sub struct!
				// check leaving with priority, can both happen in one BB
				for (Struct findStruct = struct; findStruct != null; findStruct = findStruct
						.getParent()) {
					if (findStruct.isBreakTarget(bb)) {
						// leaving struct!
						// an unlabeled break statement terminates the innermost loop or switch
						// statement, but a labeled break terminates any outer struct
						if (findStruct.getLabel() != null) {
							final BreakStatement breakStatement = getAst().newBreakStatement();
							breakStatement.setLabel(getAst().newSimpleName(findStruct.getLabel()));
							statements.add(breakStatement);
						} else if (findStruct instanceof Loop) {
							statements.add(getAst().newBreakStatement());
						} else if (findStruct instanceof Switch) {
							// final case-break in switches is removed later
							statements.add(getAst().newBreakStatement());
						}
						return;
					}
					if (findStruct.getParent() == bb.getStruct()) {
						log("Struct leave in BB " + bb.getPostorder()
								+ " without regular follow encounter:\n" + struct);
						return;
					}
				}
				if (!bb.getStruct().isHead(bb)) {
					log("Struct change in BB " + bb.getPostorder()
							+ " without regular follow or head encounter:\n" + struct);
					return;
				}
				// entering a new sub struct!
				// enter this sub struct and come back...

				// multiple overlaying heads possible (e.g. endless-loop -> post-loop -> cond),
				// find topmost unused and use this as sub struct
				Struct subStruct = bb.getStruct();
				while (struct != subStruct.getParent()) {
					subStruct = subStruct.getParent();
					if (subStruct == null) {
						log("Struct enter in BB " + bb.getPostorder()
								+ " without regular head encounter:\n" + struct);
						return;
					}
				}
				bb = transformStruct(subStruct, statements);
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
					if (loop.isHead(bb)) {
						if (struct != loop && (loop.isEndless() || loop.isPre())) {
							// only from sub structure
							statements.add(getAst().newContinueStatement());
							return;
						}
						if (bb != firstBb) {
							// encountered loop head through direct backjump through sub sequence
							return;
						}
						// head == last possible for self loops => fall through to next checks...
					}
					if (!loop.isLast(bb)) {
						continue;
					}
					if (loop.isPost()) {
						if (struct == loop) {
							final int size = bb.getStmts() - 1;
							for (int i = 0; i < size; ++i) {
								statements.add(bb.getStmt(i));
							}
						} else {
							statements.add(getAst().newContinueStatement());
						}
						return;
					} else if (loop.isPre() || loop.isEndless()) {
						for (int i = 0; i < bb.getStmts(); ++i) {
							statements.add(bb.getStmt(i));
						}
					}
					return;
				} else if (findStruct instanceof Switch) {
					final Switch findSwitch = (Switch) findStruct;
					if (findSwitch.isCase(bb)) {
						if (firstBb != bb) {
							// fall-through follow-case
							return;
						}
					}
				}
			}
			// simple sequence block, 0 statements possible with empty GOTO basic blocks
			for (int i = 0; i < bb.getStmts(); ++i) {
				statements.add(bb.getStmt(i));
			}
			final E out = bb.getSequenceOut();
			if (out == null) {
				return;
			}
			// follow back edges for continues
			bb = out.getEnd();
		}
	}

	private BB transformStruct(final Struct struct, final List<Statement> statements) {
		// decompile sub structure into a statement
		final Statement structStatement;
		if (struct instanceof Catch) {
			structStatement = transformCatch((Catch) struct);
		} else if (struct instanceof Cond) {
			// possible statements before cond in basic block
			final int size = struct.getHead().getStmts() - 1;
			for (int i = 0; i < size; ++i) {
				statements.add(struct.getHead().getStmt(i));
			}
			structStatement = transformCond((Cond) struct);
		} else if (struct instanceof Loop) {
			structStatement = transformLoop((Loop) struct);
		} else if (struct instanceof Switch) {
			// possible statements before switch in basic block
			final int size = struct.getHead().getStmts() - 1;
			for (int i = 0; i < size; ++i) {
				statements.add(struct.getHead().getStmt(i));
			}
			structStatement = transformSwitch((Switch) struct);
		} else {
			log("Unknown struct:\n" + struct);
			structStatement = null;
		}
		if (structStatement == null) {
			log("Couldn't decompile struct:\n" + struct);
		} else {
			statements.add(structStatement);
		}
		return struct.getFollow();
	}

	private Statement transformSwitch(final Switch switchStruct) {
		switch (switchStruct.getKind()) {
		case NO_DEFAULT:
		case WITH_DEFAULT: {
			final BB head = switchStruct.getHead();
			final SwitchStatement switchStatement = (SwitchStatement) head.getFinalStmt();

			for (final E out : head.getOuts()) {
				if (!out.isSwitchCase()) {
					continue;
				}
				for (final Integer value : (Integer[]) out.getValue()) {
					if (out.isSwitchDefault() && switchStruct.getKind() == Switch.Kind.NO_DEFAULT) {
						continue;
					}
					final SwitchCase switchCase = getAst().newSwitchCase();
					if (value == null) {
						// necessary: expression initialized to null for default case
						switchCase.setExpression(null);
					} else {
						// TODO convert to char for chars etc., where can we get this info? binding?
						switchCase.setExpression(getAst().newNumberLiteral(value.toString()));
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
			log("Unknown switch type '" + switchStruct.getKind() + "'!");
			return null;
		}
	}

}