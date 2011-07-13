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
import java.util.List;
import java.util.Set;

import org.decojer.web.util.EntityConstants;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

public class BlobAnalyser {

	public static BlobInfo uniqueBlobInfo(final DatastoreService datastoreService,
			final BlobKey blobKey) throws EntityNotFoundException {
		final BlobInfo blobInfo = new BlobInfo(getBlobInfoEntity(
				datastoreService, blobKey));
		// query duplicates via md5 and size
		final Query duplicateQuery = new Query(EntityConstants.KIND_BLOBINFO);
		duplicateQuery.addFilter(EntityConstants.PROP_MD5HASH,
				Query.FilterOperator.EQUAL, blobInfo.getMd5Hash());
		duplicateQuery.addFilter(EntityConstants.PROP_SIZE,
				Query.FilterOperator.EQUAL, blobInfo.getSize());
		final List<Entity> duplicateEntities = datastoreService.prepare(
				duplicateQuery).asList(FetchOptions.Builder.withLimit(10));
		// could be 0 or 1 - even if 1 or 2 blobs are in store, HA metadata...
		Entity oldestEntity = blobInfo.getBlobInfoEntity();
		Date oldestDate = blobInfo.getCreation();
		Date newestDate = oldestDate;
		final Set<BlobKey> deleteBlobKeys = blobInfo.getDeleteBlobKeys();
		// find oldest...
		for (final Entity duplicateEntity : duplicateEntities) {
			// init
			if (oldestEntity.equals(duplicateEntity)) {
				continue;
			}
			final Date creationDate = (Date) duplicateEntity
					.getProperty(EntityConstants.PROP_CREATION);
			// one must die now...
			if (newestDate.compareTo(creationDate) < 0) {
				newestDate = creationDate;
			}
			Entity dieEntity;
			if (oldestDate.compareTo(creationDate) < 0) {
				dieEntity = duplicateEntity;
			} else {
				dieEntity = oldestEntity;
				oldestDate = creationDate;
				oldestEntity = duplicateEntity;
			}
			// change and delete newer entry
			deleteBlobKeys.add(new BlobKey(dieEntity.getKey().getName()));
		}
		blobInfo.setBlobInfoEntity(oldestEntity);
		blobInfo.setNewestDate(newestDate);
		blobInfo.setOldestDate(oldestDate);
		return blobInfo;
	}

	public static Entity getBlobInfoEntity(
			final DatastoreService datastoreService, final BlobKey blobKey)
			throws EntityNotFoundException {
		final Entity blobInfoEntity = datastoreService.get(KeyFactory
				.createKey(EntityConstants.KIND_BLOBINFO,
						blobKey.getKeyString()));
		String md5Hash = (String) blobInfoEntity
				.getProperty(EntityConstants.PROP_MD5HASH);
		// in development mode no property "md5_hash" is created (for
		// recognition of duplicate entities), luckily we can change this
		// entities here (not possible on production)
		if (md5Hash == null) {
			md5Hash = "_"
					+ blobInfoEntity.getProperty(EntityConstants.PROP_FILENAME)
					+ "_"
					+ blobInfoEntity.getProperty(EntityConstants.PROP_SIZE);
			blobInfoEntity.setProperty(EntityConstants.PROP_MD5HASH, md5Hash);
			// flush for following query
			final Transaction tx = datastoreService.beginTransaction();
			// not allowed on production!!!
			datastoreService.put(blobInfoEntity);
			tx.commit();
		}
		return blobInfoEntity;
	}
}