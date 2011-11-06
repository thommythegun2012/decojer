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
package org.decojer.web.service;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;

import org.decojer.web.util.IOUtils;

import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

/**
 * Blob service.
 * 
 * @author André Pankraz
 */
public class BlobService {

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private static final FileService FILE_SERVICE = FileServiceFactory.getFileService();

	private static final BlobService INSTANCE = new BlobService();

	public static BlobService getInstance() {
		return INSTANCE;
	}

	public final BlobKey createBlob(final String mimeType, final String fileName,
			final byte[] content) {
		try {
			final AppEngineFile file = FILE_SERVICE.createNewBlobFile(mimeType, fileName);
			final FileWriteChannel writeChannel = FILE_SERVICE.openWriteChannel(file, true);
			final OutputStream fileOutputStream = Channels.newOutputStream(writeChannel);
			// don't hold file open for too long (around max. 30 seconds), else:
			// "Caused by: com.google.apphosting.api.ApiProxy$ApplicationException: ApplicationError: 10: Unknown",
			// don't use byte array directly, else file write request too large
			// (BufferedOutputStream writes big data directly)
			IOUtils.copy(new ByteArrayInputStream(content), fileOutputStream);
			fileOutputStream.close();
			writeChannel.closeFinally();
			return FILE_SERVICE.getBlobKey(file);
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't write blob!", e);
		}
	}

	public List<Entity> findBlobInfoEntities(final String md5Hash, final Long size) {
		final Query duplicateQuery = new Query(BlobInfoFactory.KIND);
		duplicateQuery.addFilter(BlobInfoFactory.MD5_HASH, Query.FilterOperator.EQUAL, md5Hash);
		duplicateQuery.addFilter(BlobInfoFactory.SIZE, Query.FilterOperator.EQUAL, size);
		return DATASTORE_SERVICE.prepare(duplicateQuery)
				.asList(FetchOptions.Builder.withDefaults());
	}

}