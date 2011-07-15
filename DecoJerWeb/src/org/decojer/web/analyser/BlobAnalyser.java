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

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.decojer.web.util.EntityConstants;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.twmacinta.util.MD5InputStream;

public class BlobAnalyser {

	private static Entity getBlobInfoEntity(
			final DatastoreService datastoreService, final BlobKey blobKey)
			throws EntityNotFoundException {
		final Entity blobInfoEntity = datastoreService.get(KeyFactory
				.createKey(EntityConstants.KIND_BLOBINFO,
						blobKey.getKeyString()));
		// in development mode no property "md5_hash" is created (for
		// recognition of duplicate entities), luckily we can change this
		// entities here (not possible on production)
		if (blobInfoEntity.getProperty(EntityConstants.PROP_MD5HASH) == null) {
			String md5hash;
			try {
				final MD5InputStream md5InputStream = new MD5InputStream(
						new BlobstoreInputStream(blobKey));
				final byte[] buf = new byte[65536];
				int num_read;
				while ((num_read = md5InputStream.read(buf)) != -1) {
					;
				}
				md5hash = md5InputStream.getMD5().asHex();
			} catch (final IOException e) {
				throw new RuntimeException(
						"Could not generate blobstore input stream MD5 - should only happen in development mode!");
			}
			blobInfoEntity.setProperty(EntityConstants.PROP_MD5HASH, md5hash);
			// flush for following query
			final Transaction tx = datastoreService.beginTransaction();
			// not allowed on production!!!
			datastoreService.put(blobInfoEntity);
			tx.commit();
		}
		return blobInfoEntity;
	}

	public static BlobInfo uniqueBlobInfo(
			final DatastoreService datastoreService, final BlobKey blobKey)
			throws EntityNotFoundException {
		final Entity blobInfoEntity = getBlobInfoEntity(datastoreService,
				blobKey);
		// query duplicates via md5 and size;
		// because of lagging HA writes - could be less, even _0_ or other
		final Query duplicateQuery = new Query(EntityConstants.KIND_BLOBINFO);
		duplicateQuery.addFilter(EntityConstants.PROP_MD5HASH,
				Query.FilterOperator.EQUAL,
				blobInfoEntity.getProperty(EntityConstants.PROP_MD5HASH));
		duplicateQuery.addFilter(EntityConstants.PROP_SIZE,
				Query.FilterOperator.EQUAL,
				blobInfoEntity.getProperty(EntityConstants.PROP_SIZE));
		final List<Entity> duplicateEntities = datastoreService.prepare(
				duplicateQuery).asList(FetchOptions.Builder.withDefaults());
		// find oldest entity, start with myself
		Entity oldestEntity = blobInfoEntity;
		Date oldestDate = (Date) blobInfoEntity
				.getProperty(EntityConstants.PROP_CREATION);
		Date newestDate = oldestDate;
		final Set<BlobKey> duplicateBlobs = new HashSet<BlobKey>();
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
			Entity duplicateBlob;
			if (oldestDate.compareTo(creationDate) < 0) {
				duplicateBlob = duplicateEntity;
			} else {
				duplicateBlob = oldestEntity;
				oldestDate = creationDate;
				oldestEntity = duplicateEntity;
			}
			// change and delete newer entry
			duplicateBlobs.add(new BlobKey(duplicateBlob.getKey().getName()));
		}
		final BlobInfo blobInfo = new BlobInfo(oldestEntity, duplicateBlobs);
		blobInfo.setNewestDate(newestDate);
		blobInfo.setOldestDate(oldestDate);
		return blobInfo;
	}
}