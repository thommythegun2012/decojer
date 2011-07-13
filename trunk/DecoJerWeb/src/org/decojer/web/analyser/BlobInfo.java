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

import java.util.Date;
import java.util.HashSet;

import org.decojer.web.util.EntityConstants;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class BlobInfo {

	public BlobKey blobKey;

	public HashSet<BlobKey> deleteBlobKeys = new HashSet<BlobKey>();

	public String filename;

	public String kind;

	public String md5Hash;

	public String name;

	public Date newestDate;

	public Date oldestDate;

	public Long size;

	public Entity createEntity(final Key key) {
		final Entity entity = new Entity(key);
		entity.setProperty(EntityConstants.PROP_UPLOAD, this.blobKey);
		return entity;
	}

	public Key createKey() {
		return KeyFactory.createKey(this.kind, this.md5Hash + this.size);
	}
}