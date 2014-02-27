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
package org.decojer.cavaj.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;

import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.fields.FD;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.methods.MD;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.model.types.TD;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Lists;

/**
 * Container for Declarations.
 * 
 * @author André Pankraz
 */
public abstract class Container {

	private final static Logger LOGGER = Logger.getLogger(Container.class.getName());

	/**
	 * All body declarations: inner type / method / field declarations.
	 */
	@Getter
	private final List<ED> bds = new ArrayList<ED>(4);

	protected void _getAllTds(final List<TD> tds) {
		for (final ED bd : this.bds) {
			if (bd instanceof TD) {
				tds.add((TD) bd);
			}
			bd._getAllTds(tds);
		}
	}

	/**
	 * Add body declaration.
	 * 
	 * @param bd
	 *            bode declaration
	 */
	public void addBd(final ED bd) {
		if (bd.getParent() != null) {
			if (bd.getParent() != this) {
				LOGGER.warning("Cannot change parent declaration for '" + bd + "' from '"
						+ bd.getParent() + "' to '" + this + "'!");
			}
			return;
		}
		bd.setParent(this);
		this.bds.add(bd);
	}

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void addTd(final TD td) {
		addBd(td);
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		for (final ED bd : this.bds) {
			bd.clear();
		}
	}

	public List<TD> getAllTds() {
		final List<TD> tds = Lists.newArrayList();
		_getAllTds(tds);
		return tds;
	}

	public Element getDeclarationForNode(final ASTNode node) {
		for (final Element bd : getDeclarations()) {
			// could also work with polymorphism here...but why pollute subclasses with helper
			if (bd instanceof F) {
				if (((F) bd).getAstNode() == node) {
					return bd;
				}
			} else if (bd instanceof M) {
				if (((M) bd).getAstNode() == node) {
					return bd;
				}
			} else if (bd instanceof T) {
				if (((T) bd).getAstNode() == node) {
					return bd;
				}
			}
			final Element retBd = bd.getDeclarationForNode(node);
			if (retBd != null) {
				return retBd;
			}
		}
		return null;
	}

	public List<Element> getDeclarations() {
		final List<Element> declarations = Lists.newArrayList();
		for (final ED bd : getBds()) {
			declarations.add(bd.getElement());
		}
		return declarations;
	}

	public Element getElement() {
		if (this instanceof TD) {
			return ((TD) this).getT();
		}
		if (this instanceof MD) {
			return ((MD) this).getM();
		}
		if (this instanceof FD) {
			return ((FD) this).getF();
		}
		return null;
	}

	public abstract String getName();

}