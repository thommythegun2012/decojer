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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;

/**
 * 
 * @author André Pankraz
 */
public class EntityUtils {

	public static Entity getBlobInfoEntity(
			final DatastoreService datastoreService, final BlobKey blobKey)
			throws EntityNotFoundException {

		final Entity blobInfoEntity = datastoreService.get(KeyFactory
				.createKey("__BlobInfo__", blobKey.getKeyString()));

		// in development mode no property "md5_hash" is created (for
		// recognition of duplicate entities), luckily we can change this
		// entities here (not possible on production)
		String md5Hash = (String) blobInfoEntity.getProperty(PropertyName.MD5_HASH);
		if (md5Hash == null) {
			md5Hash = "_" + blobInfoEntity.getProperty(PropertyName.FILENAME) + "_"
					+ blobInfoEntity.getProperty(PropertyName.SIZE);
			blobInfoEntity.setProperty(PropertyName.MD5_HASH, md5Hash);
			// flush for following query
			final Transaction tx = datastoreService.beginTransaction();
			// not allowed on production!!!
			datastoreService.put(blobInfoEntity);
			tx.commit();
		}
		return blobInfoEntity;
	}

}