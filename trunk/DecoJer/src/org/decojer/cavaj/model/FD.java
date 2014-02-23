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

import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Field declaration.
 * 
 * @author André Pankraz
 */
public final class FD extends BD {

	private final static Logger LOGGER = Logger.getLogger(FD.class.getName());

	@Getter
	private final F f;

	/**
	 * AST field declaration.
	 */
	@Getter
	@Setter
	private BodyDeclaration fieldDeclaration;

	/**
	 * Value for constant attributes or {@code null}. Type Integer: int, short, byte, char, boolean.
	 */
	@Getter
	@Setter
	private Object value;

	/**
	 * Constructor.
	 * 
	 * @param f
	 *            field
	 */
	protected FD(final F f) {
		assert f != null;

		this.f = f;
	}

	@Override
	public boolean check(final AF af) {
		return getF().check(af);
	}

	@Override
	public void clear() {
		this.fieldDeclaration = null;
		super.clear();
	}

	@Override
	public String getName() {
		return getF().getName();
	}

	/**
	 * Get owner type declaration.
	 * 
	 * @return owner type declaration
	 */
	public TD getTd() {
		return ((ClassT) getF().getT()).getTd();
	}

	/**
	 * Get value type.
	 * 
	 * @return value type
	 */
	public T getValueT() {
		return getF().getValueT();
	}

	/**
	 * Is enum?
	 * 
	 * @return {@code true} - is enum
	 */
	public boolean isEnum() {
		return getF().isEnum();
	}

	@Override
	public boolean isStatic() {
		return getF().isStatic();
	}

	@Override
	public boolean isSynthetic() {
		return getF().isSynthetic();
	}

	/**
	 * Relocate type declaration to field declaration, e.g. anonymous enum fields.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void relocateTd(final TD td) {
		if (td.getParent() != null) {
			td.getParent().getBds().remove(td);
			td.setParent(null);
		}
		addTd(td);
	}

	@Override
	public void setAccessFlags(final int accessFlags) {
		getF().setAccessFlags(accessFlags);
	}

	@Override
	public void setDeprecated() {
		getF().setDeprecated();
	}

	@Override
	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		final T valueT = getTd().getDu().parseT(signature, new Cursor(), getF());
		if (!valueT.eraseTo(getValueT())) {
			LOGGER.info("Cannot reduce signature '" + signature + "' to type '" + getValueT()
					+ "' for field value: " + this);
		} else {
			getF().setValueT(valueT);
		}
	}

	@Override
	public void setSynthetic() {
		getF().setSynthetic();
	}

	@Override
	public String toString() {
		return getF().toString();
	}

}