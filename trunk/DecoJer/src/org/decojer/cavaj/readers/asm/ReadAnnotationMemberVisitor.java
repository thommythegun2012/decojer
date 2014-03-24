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
package org.decojer.cavaj.readers.asm;

import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ReadVisitor;

/**
 * ASM read annotation member visitor.
 *
 * @author André Pankraz
 */
@Slf4j
public class ReadAnnotationMemberVisitor extends ReadAnnotationVisitor {

	@Nullable
	private A a;

	/**
	 * Constructor.
	 *
	 * @param parentVisitor
	 *            parent visitor
	 */
	public ReadAnnotationMemberVisitor(@Nonnull final ReadVisitor parentVisitor) {
		super(parentVisitor);
	}

	@Override
	protected void add(final String name, final Object value) {
		if (this.a != null) {
			this.a.addMember(name, value);
		}
	}

	/**
	 * Init and set annotation.
	 *
	 * @param desc
	 *            annotation descriptor
	 * @param retentionPolicy
	 *            retention policy
	 * @return annotation
	 */
	@Nullable
	public A init(@Nullable final String desc, final RetentionPolicy retentionPolicy) {
		final T t = getDu().getDescT(desc);
		if (t == null) {
			log.warn(getT() + ": Cannot read annotation descriptor '" + desc + "'!");
			this.a = null;
			return null;
		}
		this.a = new A(t, retentionPolicy);
		return this.a;
	}

}