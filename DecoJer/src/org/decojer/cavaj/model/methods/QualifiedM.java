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
package org.decojer.cavaj.model.methods;

import java.util.logging.Logger;

import org.decojer.cavaj.model.types.T;

/**
 * Qualified method.
 * 
 * @author André Pankraz
 */
public class QualifiedM extends ModM {

	private final static Logger LOGGER = Logger.getLogger(QualifiedM.class.getName());

	/**
	 * Method qualifier.
	 */
	private T qualifierT;

	/**
	 * Constructor.
	 * 
	 * @param qualifierT
	 *            type qualifier
	 * @param m
	 *            method
	 */
	public QualifiedM(final T qualifierT, final M m) {
		super(m);
		setQualifierT(qualifierT);
	}

	@Override
	public T getT() {
		return this.qualifierT;
	}

	@Override
	public void setQualifierT(final T qualifierT) {
		if (!super.getT().validateQualifierName(qualifierT.getName())) {
			LOGGER.warning("Qualifier type for '" + this + "' cannot be set to not matching type '"
					+ qualifierT + "'!");
			return;
		}
		this.qualifierT = qualifierT;
	}

}