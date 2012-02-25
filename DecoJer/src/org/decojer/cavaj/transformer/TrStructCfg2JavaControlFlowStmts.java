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

import static org.decojer.cavaj.util.Expressions.newPrefixExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.code.E;
import org.decojer.cavaj.model.code.struct.Catch;
import org.decojer.cavaj.model.code.struct.Cond;
import org.decojer.cavaj.model.code.struct.Loop;
import org.decojer.cavaj.model.code.struct.Struct;
import org.decojer.cavaj.model.code.struct.Switch;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * Transform final CFG Basic Block to AST Block. Ignore final empty return statements.
 * 
 * @author André Pankraz
 */
public final class TrStructCfg2JavaControlFlowStmts {

	private final static Logger LOGGER = Logger.getLogger(TrStructCfg2JavaControlFlowStmts.class
			.getName());

	/**
	 * Transform CFG.
	 * 
	 * @param cfg
	 *            CFG
	 */
	public static void transform(final CFG cfg) {
		new TrStructCfg2JavaControlFlowStmts(cfg).transform();
	}

	private final CFG cfg;

	// TODO hack
	private final HashSet<Struct> consumedStruct = new HashSet<Struct>();

	private TrStructCfg2JavaControlFlowStmts(final CFG cfg) {
		this.cfg = cfg;
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

	private void log(final String message) {
		LOGGER.warning(getMd().toString() + ": " + message);
	}

	@SuppressWarnings("unchecked")
	public void transform() {
		if (getCfg().getBlock() == null) {
			// TODO can happen, e.g. if synthethic
			return;
		}
		final List<Statement> statements = getCfg().getBlock().statements();
		statements.clear(); // possible in debug mode

		transformSequence(null, getCfg().getStartBb(), statements);

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
		// final BB head = catchStruct.getHead();
		return null;
	}

	@SuppressWarnings("unchecked")
	private IfStatement transformCond(final Cond cond) {
		final BB head = cond.getHead();

		final IfStatement statement = (IfStatement) head.getFinalStmt();
		final Expression expression = (Expression) ASTNode.copySubtree(getAst(),
				statement.getExpression());

		final BB falseSucc = head.getFalseSucc();
		final BB trueSucc = head.getTrueSucc();

		boolean negate = true;
		switch (cond.getType()) {
		case Cond.IF:
			negate = false;
		case Cond.IFNOT: {
			final IfStatement ifStatement = getAst().newIfStatement();
			ifStatement.setExpression(negate ? newPrefixExpression(PrefixExpression.Operator.NOT,
					expression) : expression);

			final List<Statement> subStatements = new ArrayList<Statement>();
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
		case Cond.IF_ELSE:
			negate = false;
		case Cond.IFNOT_ELSE: {
			final IfStatement ifStatement = getAst().newIfStatement();
			ifStatement.setExpression(negate ? newPrefixExpression(PrefixExpression.Operator.NOT,
					expression) : expression);
			{
				final List<Statement> subStatements = new ArrayList<Statement>();
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
				final List<Statement> subStatements = new ArrayList<Statement>();
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
			log("Unknown cond type '" + cond.getType() + "'!");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Statement transformLoop(final Loop loop) {
		final BB head = loop.getHead();
		final BB tail = loop.getLast();

		boolean negate = true;
		switch (loop.getType()) {
		case Loop.WHILE:
			negate = false;
		case Loop.WHILENOT: {
			final WhileStatement whileStatement = getAst().newWhileStatement();

			final IfStatement statement = (IfStatement) head.getStmt(0);
			final Expression expression = (Expression) ASTNode.copySubtree(getAst(),
					statement.getExpression());
			whileStatement.setExpression(negate ? newPrefixExpression(
					PrefixExpression.Operator.NOT, expression) : expression);

			final List<Statement> subStatements = new ArrayList<Statement>();
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
		case Loop.DO_WHILE:
			negate = false;
		case Loop.DO_WHILENOT: {
			final DoStatement doStatement = getAst().newDoStatement();

			final List<Statement> subStatements = new ArrayList<Statement>();
			transformSequence(loop, head, subStatements);

			final Statement statement = tail.getFinalStmt();
			final Expression expression = (Expression) ASTNode.copySubtree(getAst(),
					((IfStatement) statement).getExpression());
			doStatement.setExpression(negate ? newPrefixExpression(PrefixExpression.Operator.NOT,
					expression) : expression);

			// has always block
			((Block) doStatement.getBody()).statements().addAll(subStatements);
			return doStatement;
		}
		case Loop.ENDLESS: {
			final WhileStatement whileStatement = getAst().newWhileStatement();

			whileStatement.setExpression(getAst().newBooleanLiteral(true));

			final List<Statement> subStatements = new ArrayList<Statement>();
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
			log("Unknown loop type '" + loop.getType() + "'!");
			return null;
		}
	}

	// struct as parameter is necessary, given basic block may allready be an
	// outer element
	private void transformSequence(final Struct struct, final BB bb,
			final List<Statement> statements) {
		BB succ = bb;
		int endlessHack = 0;
		while (succ != null && endlessHack++ < 100) {
			Struct succStruct = succ.getStruct();
			// struct change? => sequence change!
			if (struct != succStruct) {
				// handle follow or new head, follow and next struct head can be
				// same node, handle follow first!
				for (Struct findStruct = struct; findStruct != null; findStruct = findStruct
						.getParent()) {
					if (findStruct.isFollow(succ)) {
						// An unlabeled break statement terminates the innermost
						// switch, for, while, or do-while statement, but a
						// labeled break terminates an outer statement.
						if (findStruct.getLabel() != null) {
							final BreakStatement breakStatement = getAst().newBreakStatement();
							breakStatement.setLabel(getAst().newSimpleName(findStruct.getLabel()));
							statements.add(breakStatement);
						} else if (findStruct instanceof Loop) {
							statements.add(getAst().newBreakStatement());
						} else if (findStruct instanceof Switch) {
							// not default fall-through
							statements.add(getAst().newBreakStatement());
						}
						return;
					}
				}
				// succStruct == null possible for top level structure leaves
				if (succStruct != null && succStruct.isHead(succ)) {
					// TODO quick hack
					if (succStruct.getParent() != null
							&& struct == succStruct.getParent().getParent()) {
						succStruct = succStruct.getParent();
					}
					// TODO hack
					if (this.consumedStruct.contains(succStruct)) {
						return;
					}
					this.consumedStruct.add(succStruct);

					// decompile sub structure into a statement
					final Statement structStatement;
					if (succStruct instanceof Catch) {
						structStatement = transformCatch((Catch) succStruct);
					} else if (succStruct instanceof Cond) {
						// possible statements before cond in basic block
						final int size = succ.getStmts() - 1;
						for (int i = 0; i < size; ++i) {
							statements.add(succ.getStmt(i));
						}
						structStatement = transformCond((Cond) succStruct);
					} else if (succStruct instanceof Loop) {
						structStatement = transformLoop((Loop) succStruct);
					} else if (succStruct instanceof Switch) {
						// possible statements before switch in basic block
						final int size = succ.getStmts() - 1;
						for (int i = 0; i < size; ++i) {
							statements.add(succ.getStmt(i));
						}
						structStatement = transformSwitch((Switch) succStruct);
					} else {
						log("Unknown struct '" + succStruct.getClass().getSimpleName() + "'!");
						structStatement = null;
					}
					if (structStatement == null) {
						log("Couldn't decompile struct:\n" + succStruct);
					} else {
						statements.add(structStatement);
					}
					// follow is empty or direct back edge (e.g. an optimization
					// for pre / endless loops with final if)?
					// => sequence end
					if (succStruct.getFollow() == null
							|| succStruct.getFollow().getPostorder() > succStruct.getHead()
									.getPostorder()) {
						return;
					}
					succ = succStruct.getFollow();
					continue;
				}
				log("Struct change without regular follow or head encounter:\n" + struct);
			}

			for (Struct findStruct = struct; findStruct != null; findStruct = findStruct
					.getParent()) {
				if (findStruct instanceof Loop) {
					final Loop findLoop = (Loop) findStruct;
					// The continue statement skips the current iteration of a
					// for, while , or do-while loop. The unlabeled form skips
					// to the end of the innermost loop's body and evaluates the
					// boolean expression that controls the loop. A labeled
					// continue statement skips the current iteration of an
					// outer loop marked with the given label.

					if (findLoop.isHead(succ)) {
						if (struct != findLoop && (findLoop.isEndless() || findLoop.isPre())) {
							// only from sub structure
							statements.add(getAst().newContinueStatement());
							return;
						}
					}
					if (findLoop.isLast(succ)) {
						if (findLoop.isPost()) {
							if (struct == findLoop) {
								final int size = succ.getStmts() - 1;
								for (int i = 0; i < size; ++i) {
									statements.add(succ.getStmt(i));
								}
							} else {
								statements.add(getAst().newContinueStatement());
							}
							return;
						} else if (findLoop.isPre() || findLoop.isEndless()) {
							final int size = succ.getStmts();
							for (int i = 0; i < size; ++i) {
								statements.add(succ.getStmt(i));
							}
						}
						return;
					}
				} else if (findStruct instanceof Switch) {
					final Switch findSwitch = (Switch) findStruct;
					if (findSwitch.isCase(succ)) {
						if (bb != succ) {
							// fall-through follow-case
							return;
						}
					}
				}
			}
			// simple sequence block, 0 statements possible with empty GOTO basic blocks
			final int size = succ.getStmts();
			for (int i = 0; i < size; ++i) {
				statements.add(succ.getStmt(i));
			}
			succ = succ.getSucc();
		}
	}

	@SuppressWarnings("unchecked")
	private Statement transformSwitch(final Switch switchStruct) {
		final BB head = switchStruct.getHead();

		final SwitchStatement statement = (SwitchStatement) head.getFinalStmt();
		final Expression expression = (Expression) ASTNode.copySubtree(getAst(),
				statement.getExpression());

		boolean defaultCase = true;
		switch (switchStruct.getType()) {
		case Switch.SWITCH:
			defaultCase = false;
		case Switch.SWITCH_DEFAULT: {
			final SwitchStatement switchStatement = getAst().newSwitchStatement();
			switchStatement.setExpression(expression);

			final List<E> outs = head.getSwitchOuts();

			int size = outs.size();
			if (!defaultCase) {
				--size;
			}
			for (int i = 0; i < size; ++i) {
				for (final Integer value : (Integer[]) outs.get(i).getValue()) {
					final SwitchCase switchCase = getAst().newSwitchCase();
					if (value == null) {
						// necessary: expression initialized to null
						switchCase.setExpression(null);
					} else if (value instanceof Integer) {
						switchCase.setExpression(getAst().newNumberLiteral(value.toString()));
					}
					switchStatement.statements().add(switchCase);
				}

				final List<Statement> subStatements = new ArrayList<Statement>();
				transformSequence(switchStruct, outs.get(i).getEnd(), subStatements);
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
			log("Unknown switch type '" + switchStruct.getType() + "'!");
			return null;
		}
	}

}