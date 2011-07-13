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
package org.decojer.web.analyser;

import org.decojer.web.util.EntityConstants;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.twmacinta.util.MD5;

public class TypeInfo {

	public String md5Hash;

	public String name;

	public String signature;

	public int size; // for child ref

	public String superName;

	public Entity createEntity(final Key key) {
		final Entity entity = new Entity(key);
		entity.setProperty(EntityConstants.PROP_NAME, this.name);
		entity.setUnindexedProperty(EntityConstants.PROP_SIGNATURE,
				this.signature);
		return entity;
	}

	public Key createKey() {
		final String id = this.name + this.signature;
		return KeyFactory.createKey(EntityConstants.KIND_TYPE,
				new MD5(id).asHex() + id.length());
	}
}