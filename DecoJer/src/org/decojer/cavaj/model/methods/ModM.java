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

import java.util.List;

import lombok.Getter;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.types.T;

/**
 * Modified method.
 * 
 * @author André Pankraz
 */
public abstract class ModM extends M {

	@Getter
	private M rawM;

	protected ModM(final M rawM) {
		setRawM(rawM);
	}

	@Override
	public void addTypeDeclaration(final T t) {
		getRawM().addTypeDeclaration(t);
	}

	@Override
	public boolean check(final AF af) {
		return getRawM().check(af);
	}

	@Override
	public MD createMd() {
		return getRawM().createMd();
	}

	@Override
	public Element getDeclarationOwner() {
		return getRawM().getDeclarationOwner();
	}

	@Override
	public List<Element> getDeclarations() {
		return getRawM().getDeclarations();
	}

	@Override
	public String getDescriptor() {
		return getRawM().getDescriptor();
	}

	@Override
	public MD getMd() {
		return getRawM().getMd();
	}

	@Override
	public String getName() {
		return getRawM().getName();
	}

	@Override
	public T[] getParamTs() {
		return getRawM().getParamTs();
	}

	@Override
	public T getReceiverT() {
		return getRawM().getReceiverT();
	}

	@Override
	public T getReturnT() {
		return getRawM().getReturnT();
	}

	@Override
	public T getT() {
		return getRawM().getT();
	}

	@Override
	public boolean isConstructor() {
		return getRawM().isConstructor();
	}

	@Override
	public boolean isDeclaration() {
		return getRawM().isDeclaration();
	}

	@Override
	public boolean isDynamic() {
		return getRawM().isConstructor();
	}

	@Override
	public boolean isStatic() {
		return getRawM().isStatic();
	}

	@Override
	public boolean isSynthetic() {
		return getRawM().isSynthetic();
	}

	@Override
	public boolean isVarargs() {
		return getRawM().isVarargs();
	}

	@Override
	public void setAccessFlags(final int accessFlags) {
		getRawM().setAccessFlags(accessFlags);
	}

	@Override
	public void setDeprecated() {
		getRawM().setDeprecated();
	}

	@Override
	public void setRawM(final M rawM) {
		assert rawM != null;

		this.rawM = rawM;
	}

	@Override
	public void setStatic(final boolean f) {
		getRawM().setStatic(f);
	}

	@Override
	public void setSynthetic() {
		getRawM().setSynthetic();
	}

	@Override
	public void setT(final T t) {
		getRawM().setT(t);
	}

}