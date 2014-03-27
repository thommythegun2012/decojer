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

import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.ED;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.types.T;

/**
 * Method declaration.
 * 
 * @author André Pankraz
 */
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
public final class MD extends ED {

	/**
	 * Annotation default value.
	 */
	@Nullable
	private Object annotationDefaultValue;

	/**
	 * Control flow graph.
	 */
	@Nullable
	private CFG cfg;

	/**
	 * Method parameter annotations.
	 */
	@Nullable
	private A[][] paramAss;

	/**
	 * Method parameter names.
	 */
	@Nullable
	private String[] paramNames;

	/**
	 * Signature.
	 * 
	 * For Eclipse method finding.
	 */
	@Nullable
	private String signature;

	/**
	 * Throws types.
	 */
	@Nullable
	private T[] throwsTs;

	/**
	 * Type parameters.
	 */
	@Nullable
	private T[] typeParams;

	@Override
	public void clear() {
		if (this.cfg != null) {
			this.cfg.clear();
		}
		super.clear();
	}

}