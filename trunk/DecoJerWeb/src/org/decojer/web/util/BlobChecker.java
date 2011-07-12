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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

public class BlobChecker {

	private BlobKey blobKey;

	private final DatastoreService datastoreService;

	private Set<BlobKey> deleteBlobKeys;

	private String filename;

	private String md5Hash;

	private Date newestDate;

	private Date oldestDate;

	private Long size;

	public BlobChecker(final DatastoreService datastoreService,
			final BlobKey blobKey) throws EntityNotFoundException {
		this.datastoreService = datastoreService;
		this.blobKey = blobKey;
		check();
	}

	private void check() throws EntityNotFoundException {
		if (this.deleteBlobKeys != null) {
			return;
		}
		final Entity blobInfoEntity = EntityUtils.getBlobInfoEntity(
				this.datastoreService, this.blobKey);
		this.filename = (String) blobInfoEntity
				.getProperty(EntityConstants.PROP_FILENAME);
		this.md5Hash = (String) blobInfoEntity
				.getProperty(EntityConstants.PROP_MD5HASH);
		this.size = (Long) blobInfoEntity
				.getProperty(EntityConstants.PROP_SIZE);
		final Query duplicateQuery = new Query("__BlobInfo__");
		duplicateQuery.addFilter(EntityConstants.PROP_MD5HASH,
				Query.FilterOperator.EQUAL, this.md5Hash);
		duplicateQuery.addFilter(EntityConstants.PROP_SIZE,
				Query.FilterOperator.EQUAL, this.size);
		final List<Entity> duplicateEntities = this.datastoreService.prepare(
				duplicateQuery).asList(FetchOptions.Builder.withLimit(10));
		// could be 0 or 1 - even if 1 or 2 blobs are in store, HA metadata...
		Entity oldestEntity = blobInfoEntity;
		this.oldestDate = (Date) blobInfoEntity
				.getProperty(EntityConstants.PROP_CREATION);
		this.newestDate = this.oldestDate;
		this.deleteBlobKeys = new HashSet<BlobKey>();
		// find oldest...
		for (final Entity duplicateEntity : duplicateEntities) {
			// init
			if (oldestEntity.equals(duplicateEntity)) {
				continue;
			}
			final Date creationDate = (Date) duplicateEntity
					.getProperty(EntityConstants.PROP_CREATION);
			// one must die now...
			if (this.newestDate.compareTo(creationDate) < 0) {
				this.newestDate = creationDate;
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
		this.blobKey = new BlobKey(oldestEntity.getKey().getName());
	}

	public Set<BlobKey> getDeleteBlobKeys() {
		return this.deleteBlobKeys;
	}

	public String getFilename() {
		return this.filename;
	}

	public String getMd5Hash() {
		return this.md5Hash;
	}

	public Long getSize() {
		return this.size;
	}

	public BlobKey getUniqueBlobKey() {
		return this.blobKey;
	}
}