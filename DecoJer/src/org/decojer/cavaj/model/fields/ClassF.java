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
package org.decojer.cavaj.model.fields;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Container;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Class field.
 * 
 * @author André Pankraz
 */
@Slf4j
public class ClassF extends F {

	@Setter
	private int accessFlags;

	@Getter(AccessLevel.PRIVATE)
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
	public ClassF(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.valueT = t.getDu().getDescT(descriptor);
	}

	@Override
	public boolean check(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	@Override
	public void clear() {
		getFd().clear();
	}

	@Override
	public boolean createFd() {
		if (isDeclaration()) {
			return false;
		}
		this.fd = new FD();
		setDeclarationOwner(getT());
		return true;
	}

	@Override
	public A[] getAs() {
		return getFd().getAs();
	}

	@Override
	public Object getAstNode() {
		return getFd().getAstNode();
	}

	@Override
	public CU getCu() {
		return getFd().getCu();
	}

	@Override
	public Element getDeclarationForNode(final ASTNode node) {
		return getFd().getDeclarationForNode(node);
	}

	@Override
	public Container getDeclarationOwner() {
		return getFd().getDeclarationOwner();
	}

	@Override
	public List<Element> getDeclarations() {
		return getFd().getDeclarations();
	}

	@Override
	public Object getValue() {
		return getFd().getValue();
	}

	@Override
	public boolean isDeclaration() {
		return getFd() != null;
	}

	@Override
	public boolean isEnum() {
		return check(AF.ENUM);
	}

	@Override
	public boolean isStatic() {
		return check(AF.STATIC);
	}

	@Override
	public boolean isSynthetic() {
		return check(AF.SYNTHETIC);
	}

	@Override
	public boolean isUnresolvable() {
		return true;
	}

	@Override
	public void setAs(final A[] as) {
		getFd().setAs(as);
	}

	@Override
	public void setAstNode(final Object fieldDeclaration) {
		getFd().setAstNode(fieldDeclaration);
	}

	@Override
	public void setDeclarationOwner(final Container declarationOwner) {
		final Container previousDeclarationOwner = getFd().getDeclarationOwner();
		if (previousDeclarationOwner != null) {
			previousDeclarationOwner.getDeclarations().remove(this);
		}
		declarationOwner.getDeclarations().add(this);
		getFd().setDeclarationOwner(declarationOwner);
	}

	@Override
	public void setDeprecated() {
		this.accessFlags |= AF.DEPRECATED.getValue();
	}

	@Override
	public void setEnum() {
		getT().setInterface(false); // TODO we know even more, must be from Enum
		this.accessFlags |= AF.PUBLIC.getValue() | AF.STATIC.getValue() | AF.FINAL.getValue()
				| AF.ENUM.getValue();
	}

	@Override
	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		final T valueT = getT().getDu().parseT(signature, new Cursor(), this);
		if (!valueT.eraseTo(getValueT())) {
			log.info("Cannot reduce signature '" + signature + "' to type '" + getValueT()
					+ "' for field value: " + this);
		} else {
			setValueT(valueT);
		}
	}

	@Override
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

	@Override
	public void setSynthetic() {
		this.accessFlags |= AF.SYNTHETIC.getValue();
	}

	@Override
	public void setValue(final Object value) {
		getFd().setValue(value);
	}

	@Override
	public String toString() {
		return getT() + "." + getName();
	}

}