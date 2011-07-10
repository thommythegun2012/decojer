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
package org.decojer.web.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

public class BlobUniqueChecker {

	private final DatastoreService datastoreService;

	private List<BlobKey> deleteBlobKeys;

	private final String filename;

	private final String md5Hash;

	private Date newestDate;

	private Date oldestDate;

	private BlobKey uniqueBlobKey;

	private int uploadedSum;

	public BlobUniqueChecker(final DatastoreService datastoreService,
			final String filename, final String md5Hash) {
		this.datastoreService = datastoreService;
		this.filename = filename;
		this.md5Hash = md5Hash;
	}

	private void check() {
		if (this.uniqueBlobKey != null) {
			return;
		}
		this.deleteBlobKeys = new ArrayList<BlobKey>();

		final Query duplicateQuery = new Query("__BlobInfo__");
		duplicateQuery.addFilter(Property.MD5_HASH, Query.FilterOperator.EQUAL,
				this.md5Hash);
		duplicateQuery.addFilter(Property.FILENAME, Query.FilterOperator.EQUAL,
				this.filename);
		final List<Entity> duplicateEntities = this.datastoreService.prepare(
				duplicateQuery).asList(FetchOptions.Builder.withLimit(10));

		// find oldest...
		Entity oldestEntity = null;
		this.oldestDate = null;
		this.newestDate = null;
		this.uploadedSum = 0;
		for (final Entity duplicateEntity : duplicateEntities) {
			final Date creationDate = (Date) duplicateEntity
					.getProperty("creation");
			Date updatedDate = (Date) duplicateEntity.getProperty("updated");
			if (updatedDate == null) {
				updatedDate = creationDate;
			}
			++this.uploadedSum;
			// init
			if (this.oldestDate == null) {
				oldestEntity = duplicateEntity;
				this.oldestDate = creationDate;
				this.newestDate = updatedDate;
				continue;
			}
			// one must die now...
			if (this.newestDate.compareTo(updatedDate) < 0) {
				this.newestDate = updatedDate;
			}
			Entity dieEntity;
			if (this.oldestDate.compareTo(creationDate) < 0) {
				dieEntity = duplicateEntity;
			} else {
				dieEntity = oldestEntity;
				this.oldestDate = creationDate;
				oldestEntity = duplicateEntity;
			}
			// change and delete newer entry
			this.deleteBlobKeys.add(new BlobKey(dieEntity.getKey().getName()));
		}
		this.uniqueBlobKey = new BlobKey(oldestEntity.getKey().getName());
	}

	public List<BlobKey> getDeleteBlobKeys() {
		check();
		return this.deleteBlobKeys;
	}

	public BlobKey getUniqueBlobKey() {
		check();
		return this.uniqueBlobKey;
	}

}