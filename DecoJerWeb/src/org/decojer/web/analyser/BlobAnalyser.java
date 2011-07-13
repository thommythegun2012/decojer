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

import org.decojer.web.util.EntityConstants;
import org.decojer.web.util.EntityUtils;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

public class BlobAnalyser {

	public static BlobInfo analyse(final DatastoreService datastoreService,
			final BlobKey blobKey) throws EntityNotFoundException {
		final BlobInfo blobInfo = new BlobInfo();
		final Entity blobInfoEntity = EntityUtils.getBlobInfoEntity(
				datastoreService, blobKey);
		blobInfo.filename = (String) blobInfoEntity
				.getProperty(EntityConstants.PROP_FILENAME);
		blobInfo.md5Hash = (String) blobInfoEntity
				.getProperty(EntityConstants.PROP_MD5HASH);
		blobInfo.size = (Long) blobInfoEntity
				.getProperty(EntityConstants.PROP_SIZE);
		final Query duplicateQuery = new Query(EntityConstants.KIND_BLOBINFO);
		duplicateQuery.addFilter(EntityConstants.PROP_MD5HASH,
				Query.FilterOperator.EQUAL, blobInfo.md5Hash);
		duplicateQuery.addFilter(EntityConstants.PROP_SIZE,
				Query.FilterOperator.EQUAL, blobInfo.size);
		final List<Entity> duplicateEntities = datastoreService.prepare(
				duplicateQuery).asList(FetchOptions.Builder.withLimit(10));
		// could be 0 or 1 - even if 1 or 2 blobs are in store, HA metadata...
		Entity oldestEntity = blobInfoEntity;
		blobInfo.oldestDate = (Date) blobInfoEntity
				.getProperty(EntityConstants.PROP_CREATION);
		blobInfo.newestDate = blobInfo.oldestDate;
		// find oldest...
		for (final Entity duplicateEntity : duplicateEntities) {
			// init
			if (oldestEntity.equals(duplicateEntity)) {
				continue;
			}
			final Date creationDate = (Date) duplicateEntity
					.getProperty(EntityConstants.PROP_CREATION);
			// one must die now...
			if (blobInfo.newestDate.compareTo(creationDate) < 0) {
				blobInfo.newestDate = creationDate;
			}
			Entity dieEntity;
			if (blobInfo.oldestDate.compareTo(creationDate) < 0) {
				dieEntity = duplicateEntity;
			} else {
				dieEntity = oldestEntity;
				blobInfo.oldestDate = creationDate;
				oldestEntity = duplicateEntity;
			}
			// change and delete newer entry
			blobInfo.deleteBlobKeys.add(new BlobKey(dieEntity.getKey()
					.getName()));
		}
		blobInfo.blobKey = new BlobKey(oldestEntity.getKey().getName());
		return blobInfo;
	}
}