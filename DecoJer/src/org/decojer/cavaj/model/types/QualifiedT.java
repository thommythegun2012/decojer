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
package org.decojer.cavaj.model.types;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.DU;

/**
 * Qualified type. This is used for references with parameterization and annotations of enclosing
 * classes. We cannot set it in the unmodified class type enclosing info, because it can differ
 * between references.
 *
 * @author André Pankraz
 */
@Slf4j
public class QualifiedT extends ExtendedT {

	/**
	 * Type qualifier, is like enclosing type in references.
	 */
	@Nonnull
	private T qualifierT;

	/**
	 * Constructor.
	 *
	 * @param qualifierT
	 *            type qualifier
	 * @param t
	 *            type
	 * @throws DecoJerException
	 *             no valid qualifier type
	 */
	public QualifiedT(@Nonnull final T qualifierT, @Nonnull final T t) throws DecoJerException {
		super(t); // the qualified t is the raw t, because we inherit its properties

		assert !t.isAnnotated() : this
		+ "Anno(Qual(qual, t)) is same like Qual(qual, Anno(t)), prefer first";
		assert !t.isQualified() : this + "Qual(qual, Qual(qual, t)) is not allowed";
		assert validateQualifierName(qualifierT.getName()) : this
		+ ": qualifier type cannot be set to not matching type '" + qualifierT + "'";

		this.qualifierT = qualifierT;
	}

	@Nonnull
	@Override
	public T getQualifierT() {
		final T enclosingT = getEnclosingT();
		if (enclosingT == null
				|| enclosingT.getName().length() <= this.qualifierT.getName().length()) {
			// this qualifier is most relevant
			return this.qualifierT;
		}
		// enclosing is more relevant, fix the chain
		setQualifierT(DU.getQualifiedT(this.qualifierT, enclosingT));
		return this.qualifierT;
	}

	@Override
	public boolean isQualified() {
		return true;
	}

	@Override
	public void setQualifierT(@Nonnull final T qualifierT) {
		if (!validateQualifierName(qualifierT.getName())) {
			log.warn("Qualifier type for '" + this + "' cannot be set to not matching type '"
					+ qualifierT + "'!");
			return;
		}
		this.qualifierT = qualifierT;
	}

}