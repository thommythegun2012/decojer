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

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.ClassT;

/**
 * Field.
 * 
 * Unique identifier is: "name + descriptor"<br>
 * Even though the Java language has the field name as unique identifier, obfuscated code could
 * utilize the same name for different descriptors (see e.g. ojdbc6.jar).
 * 
 * Also, an experiment:
 * 
 * Field descriptor #77 Ljava/util/ArrayList;<br>
 * Signature: Ljava/util/ArrayList<Ljava/lang/Integer;>;<br>
 * public java.util.ArrayList test;
 * 
 * works: getfield org.decojer.cavaj.test.DecTestFields.test : java.util.ArrayList [19]<br>
 * doesn't work: getfield org.decojer.cavaj.test.DecTestFields.test : java.util.List [19]<br>
 * throws: NoSuchFieldError extends IncompatibleClassChangeError
 * 
 * @author André Pankraz
 */
public class F {

	@Setter
	private int accessFlags;

	@Getter
	private FD fd;

	@Getter
	private final String name;

	@Getter
	private final T t;

	/**
	 * Value Type.
	 */
	@Getter
	@Setter
	private T valueT;

	/**
	 * Constructor.
	 * 
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	protected F(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.valueT = t.getDu().getDescT(descriptor);
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return {@code true} - is access flag
	 */
	public boolean check(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Create field declaration for this field.
	 * 
	 * @return field declaration
	 */
	public FD createFd() {
		assert this.fd == null;

		this.fd = new FD(this);
		((ClassT) getT()).getTd().addBd(this.fd);
		return this.fd;
	}

	/**
	 * Is static field?
	 * 
	 * @return {@code true} - is static field
	 */
	public boolean isStatic() {
		return check(AF.STATIC);
	}

	/**
	 * Is synthetic field?
	 * 
	 * @return {@code true} - is synthetic field
	 */
	public boolean isSynthetic() {
		return check(AF.SYNTHETIC);
	}

	public boolean isUnresolvable() {
		return true;
	}

	/**
	 * Field must be deprecated (from Deprecated attribute, marked via Javadoc @deprecate).
	 */
	public void setDeprecated() {
		this.accessFlags |= AF.DEPRECATED.getValue();
	}

	/**
	 * Field must be an enum (from Annotations attribute).
	 */
	public void setEnum() {
		getT().setInterface(false); // TODO we know even more, must be from Enum
		this.accessFlags |= AF.PUBLIC.getValue() | AF.STATIC.getValue() | AF.FINAL.getValue()
				| AF.ENUM.getValue();
	}

	/**
	 * Field must be static or dynamic (from usage, e.g. get/set).
	 * 
	 * @param f
	 *            {@code true} - is static
	 */
	public void setStatic(final boolean f) {
		if (f) {
			if ((this.accessFlags & AF.STATIC.getValue()) != 0) {
				return;
			}
			assert (this.accessFlags & AF.STATIC_ASSERTED.getValue()) == 0;

			this.accessFlags |= AF.STATIC.getValue() | AF.STATIC_ASSERTED.getValue();
			return;
		}
		assert (this.accessFlags & AF.STATIC.getValue()) == 0;

		getT().setInterface(false);
		this.accessFlags |= AF.STATIC_ASSERTED.getValue();
		return;
	}

	/**
	 * Field must be synthetic (from synthetic attribute).
	 */
	public void setSynthetic() {
		this.accessFlags |= AF.SYNTHETIC.getValue();
	}

	@Override
	public String toString() {
		return getT() + "." + this.name;
	}

}