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
package org.decojer.cavaj.model.code.structs;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.code.BB;

/**
 * Conditional struct.
 *
 * @author André Pankraz
 */
public class Cond extends Struct {

	/**
	 * Conditional kind.
	 *
	 * @author André Pankraz
	 */
	public enum Kind {

		/**
		 * If condition.
		 */
		IF,
		/**
		 * If-else condition.
		 */
		IF_ELSE,
		/**
		 * Ifnot-condition.
		 */
		IFNOT,
		/**
		 * Ifnot-else condition.
		 */
		IFNOT_ELSE

	}

	@Getter
	@Setter
	private Kind kind;

	/**
	 * Constructor.
	 *
	 * @param head
	 *            conditional head BB
	 */
	public Cond(@Nonnull final BB head) {
		super(head);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\nKind: " + this.kind);
		return sb.toString();
	}

}