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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Body declaration.
 * 
 * @author André Pankraz
 */
public abstract class BD extends D {

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Parent declaration.
	 */
	@Getter
	@Setter(AccessLevel.PROTECTED)
	private D parent;

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	@Override
	public void addTd(final TD td) {
		addBd(td);
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return {@code true} - is access flag
	 */
	public abstract boolean check(final AF af);

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	public CU getCu() {
		if (this.parent instanceof CU) {
			return (CU) this.parent;
		}
		if (this.parent instanceof BD) {
			return ((BD) this.parent).getCu();
		}
		return null;
	}

	/**
	 * Is synthetic?
	 * 
	 * @return {@code true} - is synthetic
	 */
	public abstract boolean isSynthetic();

	/**
	 * Set access flags.
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public abstract void setAccessFlags(final int accessFlags);

	/**
	 * Declaration must be deprecated (from Deprecated attribute, marked via Javadoc @deprecate).
	 * 
	 * The Deprecated attribute is an optional fixed-length attribute in the attributes table of a
	 * ClassFile, field_info or method_info structure. A class, interface, method, or field may be
	 * marked using a Deprecated attribute to indicate that the class, interface, method, or field
	 * has been superseded.
	 * 
	 * Since version 49 the Deprecated Annotation is the preferred solution and not the variant with
	 * Javadoc @deprecated. We simply put this information into the access flags as internal flag.
	 */
	public abstract void setDeprecated();

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public abstract void setSignature(final String signature);

	/**
	 * Declaration must be synthetic (from synthetic attribute).
	 * 
	 * The Synthetic attribute is a fixed-length attribute in the attributes table of a ClassFile,
	 * field_info or method_info structure. A class member that does not appear in the source code
	 * must be marked using a Synthetic attribute, or else it must have its ACC_SYNTHETIC flag set.
	 * 
	 * Since version 49 the ACC_SYNTHETIC attribute is the preferred solution. We simply put this
	 * information into the access flags in both cases.
	 */
	public abstract void setSynthetic();

}