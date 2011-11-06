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
package org.decojer.web.model;

import com.google.appengine.api.datastore.Entity;

/**
 * @author André Pankraz
 */
public abstract class BaseEntity {

	protected final Entity entity;

	/**
	 * Constructor.
	 * 
	 * @param entity
	 *            entity
	 * 
	 */
	public BaseEntity(final Entity entity) {
		this.entity = entity;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof BaseEntity && this.entity.equals(((BaseEntity) obj).entity);
	}

	/**
	 * Get entity kind.
	 * 
	 * @return entity kind
	 */
	public abstract String getKind();

	/**
	 * Get wrapped entity.
	 * 
	 * @return wrapped entity
	 */
	public Entity getWrappedEntity() {
		return this.entity;
	}

	@Override
	public int hashCode() {
		return this.entity.hashCode();
	}

}