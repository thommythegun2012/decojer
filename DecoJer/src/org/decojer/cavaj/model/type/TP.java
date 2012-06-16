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
package org.decojer.cavaj.model.type;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Type parameter.
 * 
 * From class signature: &lt;T:Lsuper;:Linterface;U::Li1;:Li2;V:TT;&gt;Ljava/lang/Object;
 * 
 * TODO very similar to object types without access flags and algebraic merge-types, use?!
 * 
 * TODO separate TV - type variable for later resolving?!
 * 
 * @author André Pankraz
 */
@Getter
public class TP {

	/**
	 * Type parameter name (e.g. 'T', 'U').
	 */
	private final String name;

	/**
	 * Super type bound.
	 */
	private final T superT;

	/***
	 * Interface type bounds.
	 */
	private final T[] interfaceTs;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            type parameter name (e.g. 'T', 'U')
	 * @param superT
	 *            super type bound
	 * @param interfaceTs
	 *            interface type bounds
	 */
	public TP(final String name, final T superT, final T[] interfaceTs) {
		this.name = name;
		this.superT = superT;
		this.interfaceTs = interfaceTs;
	}

}