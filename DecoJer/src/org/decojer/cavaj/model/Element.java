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

import javax.annotation.Nullable;

/**
 * Element.
 * 
 * @author André Pankraz
 */
public interface Element extends Container {

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return {@code true} - is access flag
	 */
	boolean check(final AF af);

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	CU getCu();

	/**
	 * Get annotations.
	 * 
	 * @return annotations
	 */
	@Nullable
	A[] getAs();

	/**
	 * Is declaration?
	 * 
	 * @return {@code true} - is declaration
	 */
	boolean isDeclaration();

	/**
	 * Get declaration owner.
	 * 
	 * @return declaration owner
	 */
	@Nullable
	Container getDeclarationOwner();

	/**
	 * Set declaration owner.
	 * 
	 * @param declarationOwner
	 *            declaration owner
	 */
	void setDeclarationOwner(final Container declarationOwner);

	/**
	 * Is static?
	 * 
	 * @return {@code true} - is static
	 */
	boolean isStatic();

	/**
	 * Is synthetic?
	 * 
	 * @return {@code true} - is synthetic
	 */
	boolean isSynthetic();

	/**
	 * Set access flags.
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	void setAccessFlags(final int accessFlags);

	/**
	 * Set annotations.
	 * 
	 * @param as
	 *            annotations
	 */
	void setAs(final A[] as);

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
	void setDeprecated();

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	void setSignature(final String signature);

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
	void setSynthetic();

}