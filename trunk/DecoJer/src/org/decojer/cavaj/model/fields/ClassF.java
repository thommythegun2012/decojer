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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
	@Nullable
	private FD fd;

	@Getter
	@Nonnull
	private final String name;

	@Getter
	@Nonnull
	private final T t;

	/**
	 * Value Type.
	 */
	@Getter
	@Setter
	@Nonnull
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
	public ClassF(@Nonnull final T t, @Nonnull final String name, @Nonnull final String descriptor) {
		this.t = t;
		this.name = name;
		final T valueT = getDu().getDescT(descriptor);
		if (valueT == null) {
			log.warn(t + ": Cannot read field value type from descriptor '" + descriptor + "'!");
		}
		this.valueT = valueT == null ? T.ANY : valueT;
	}

	@Override
	public void clear() {
		final FD fd = getFd();
		if (fd != null) {
			fd.clear();
		}
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
	public boolean getAf(@Nonnull final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	@Override
	public A[] getAs() {
		final FD fd = getFd();
		return fd == null ? null : fd.getAs();
	}

	@Override
	public Object getAstNode() {
		final FD fd = getFd();
		return fd == null ? null : fd.getAstNode();
	}

	@Override
	public CU getCu() {
		final FD fd = getFd();
		return fd == null ? null : fd.getCu();
	}

	@Override
	public Element getDeclarationForNode(final ASTNode node) {
		final FD fd = getFd();
		return fd == null ? null : fd.getDeclarationForNode(node);
	}

	@Nullable
	@Override
	public Container getDeclarationOwner() {
		final FD fd = getFd();
		return fd == null ? null : fd.getDeclarationOwner();
	}

	@Override
	public List<Element> getDeclarations() {
		final FD fd = getFd();
		return fd == null ? null : fd.getDeclarations();
	}

	@Override
	public Object getValue() {
		final FD fd = getFd();
		return fd == null ? null : fd.getValue();
	}

	@Override
	public boolean isDeclaration() {
		return getFd() != null;
	}

	@Override
	public boolean isEnum() {
		return getAf(AF.ENUM);
	}

	@Override
	public boolean isStatic() {
		return getAf(AF.STATIC);
	}

	@Override
	public boolean isSynthetic() {
		return getAf(AF.SYNTHETIC);
	}

	@Override
	public boolean isUnresolvable() {
		return true;
	}

	@Override
	public void setAf(@Nonnull final AF... af) {
		for (final AF v : af) {
			this.accessFlags |= v.getValue();
		}
	}

	@Override
	public void setAs(final A[] as) {
		final FD fd = getFd();
		if (fd != null) {
			fd.setAs(as);
		}
	}

	@Override
	public void setAstNode(@Nullable final Object fieldDeclaration) {
		final FD fd = getFd();
		if (fd != null) {
			fd.setAstNode(fieldDeclaration);
		}
	}

	@Override
	public void setDeclarationOwner(@Nonnull final Container declarationOwner) {
		final FD fd = getFd();
		if (fd == null) {
			return;
		}
		final Container previousDeclarationOwner = fd.getDeclarationOwner();
		if (previousDeclarationOwner != null) {
			previousDeclarationOwner.getDeclarations().remove(this);
		}
		declarationOwner.getDeclarations().add(this);
		fd.setDeclarationOwner(declarationOwner);
	}

	@Override
	public void setDeprecated() {
		setAf(AF.DEPRECATED);
	}

	@Override
	public void setEnum() {
		getT().setInterface(false); // TODO we know even more, must be from Enum
		setAf(AF.PUBLIC, AF.STATIC, AF.FINAL, AF.ENUM);
	}

	@Override
	public void setSignature(@Nullable final String signature) {
		final T valueT = getDu().parseT(signature, new Cursor(), this);
		if (valueT == null) {
			return;
		}
		if (!valueT.eraseTo(getValueT())) {
			log.info("Cannot reduce signature '" + signature + "' to type '" + getValueT()
					+ "' for field value: " + this);
		} else {
			setValueT(valueT);
		}
	}

	@Override
	public void setStatic(final boolean isStatic) {
		if (isStatic) {
			if (getAf(AF.STATIC)) {
				return;
			}
			assert !getAf(AF.STATIC_CONFIRMED) : this;
			setAf(AF.STATIC, AF.STATIC_CONFIRMED);
			return;
		}
		if (!getAf(AF.STATIC)) {
			return;
		}
		assert !getAf(AF.STATIC_CONFIRMED) : this;
		setAf(AF.STATIC_CONFIRMED);
		return;
	}

	@Override
	public void setSynthetic() {
		setAf(AF.SYNTHETIC);
	}

	@Override
	public void setValue(final Object value) {
		final FD fd = getFd();
		if (fd != null) {
			fd.setValue(value);
		}
	}

	@Override
	public String toString() {
		return getT() + "." + getName();
	}

}