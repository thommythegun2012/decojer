/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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
import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Field declaration.
 * 
 * @author Andr� Pankraz
 */
public final class FD extends BD {

	private final F f;

	@Getter
	private String signature;

	/**
	 * AST field declaration.
	 */
	@Getter
	@Setter
	private BodyDeclaration fieldDeclaration;

	/**
	 * Value for constant attributes or <code>null</code>. Type Integer: int, short, byte, char,
	 * boolean.
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

	public boolean check(final AF af) {
		return this.f.check(af);
	}

	@Override
	public void clear() {
		this.fieldDeclaration = null;
		super.clear();
	}

	@Override
	public String getName() {
		return this.f.getName();
	}

	/**
	 * Get owner type declaration.
	 * 
	 * @return owner type declaration
	 */
	public TD getTd() {
		return ((ClassT) this.f.getT()).getTd();
	}

	public T getValueT() {
		return this.f.getValueT();
	}

	public void setAccessFlags(final int accessFlags) {
		this.f.setAccessFlags(accessFlags);
	}

	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		this.signature = signature;

		// TODO more checks for override:
		this.f.setValueT(getTd().getDu().getDescT(signature));
	}

	@Override
	public String toString() {
		return this.f.toString();
	}

}