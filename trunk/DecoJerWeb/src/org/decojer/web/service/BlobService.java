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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.HashSet;
import java.util.List;

import org.decojer.web.util.DB;
import org.decojer.web.util.IO;

import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.utils.SystemProperty;

/**
 * Blob service.
 * 
 * @author André Pankraz
 */
public class BlobService {

	public class Stats {

		private int calculatedHashes;

		private String doubleHashes;

		private int number;

		private long size;

		public int getCalculatedHashes() {
			return this.calculatedHashes;
		}

		public String getDoubleHashes() {
			return this.doubleHashes;
		}

		public int getNumber() {
			return this.number;
		}

		public long getSize() {
			return this.size;
		}

	}

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private static final FileService FILE_SERVICE = FileServiceFactory.getFileService();

	private static final BlobService INSTANCE = new BlobService();

	public static BlobService getInstance() {
		return INSTANCE;
	}

	/**
	 * Calculate blob statistics. Generate and persist MD5 if necessary (sometimes not generated
	 * through backend at local development environment).
	 * 
	 * @return blob statistics
	 */
	public Stats calculateStats() {
		final Stats stats = new Stats();
		final StringBuffer doubleHashes = new StringBuffer();
		final HashSet<String> hashes = new HashSet<String>();

		DB.iterate(BlobInfoFactory.KIND, Integer.MAX_VALUE, new DB.Processor() {

			@Override
			public boolean process(final Entity entity) {
				stats.size += (Long) entity.getProperty(BlobInfoFactory.SIZE);
				String md5Hash = (String) entity.getProperty(BlobInfoFactory.MD5_HASH);
				if (md5Hash == null) {
					try {
						final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
								new BlobKey(entity.getKey().getName()));
						md5Hash = IO.hexEncode(IO.md5(IO.toBytes(blobstoreInputStream)));
						entity.setProperty(BlobInfoFactory.MD5_HASH, md5Hash);
						DATASTORE_SERVICE.put(entity);
						++stats.calculatedHashes;
					} catch (final IOException e) {
						throw new RuntimeException(
								"Couldn't calculate md5 hash from blob stream for entity: "
										+ entity, e);
					}
				}
				if (hashes.contains(md5Hash)) {
					doubleHashes.append(md5Hash).append(", ");
					return true;
				}
				hashes.add(md5Hash);
				return true;
			}

		});
		stats.doubleHashes = doubleHashes.length() < 2 ? null : doubleHashes.substring(0,
				doubleHashes.length() - 2);
		stats.number = hashes.size();
		return stats;
	}

	public BlobKey createBlob(final String mimeType, final String fileName, final byte[] content) {
		try {
			final AppEngineFile file = FILE_SERVICE.createNewBlobFile(mimeType, fileName);
			final FileWriteChannel writeChannel = FILE_SERVICE.openWriteChannel(file, true);
			final OutputStream fileOutputStream = Channels.newOutputStream(writeChannel);
			// don't hold file open for too long (around max. 30 seconds), else:
			// "Caused by: com.google.apphosting.api.ApiProxy$ApplicationException: ApplicationError: 10: Unknown",
			// don't write byte array directly, else file write request too large
			// (wrapping with BufferedOutputStream doesn't help - writes big data directly)
			IO.copy(new ByteArrayInputStream(content), fileOutputStream);
			fileOutputStream.close();
			writeChannel.closeFinally();
			try {
				Thread.sleep(5000);
			} catch (final InterruptedException ie) {
				// OK - sleep 5s for index updates
			}
			final BlobKey blobKey = FILE_SERVICE.getBlobKey(file);
			if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
				if (blobKey == null) {
					System.out.println("BlobKey is null? Severe Bug!");
					return null;
				}
				final Entity entity = DATASTORE_SERVICE.get(KeyFactory.createKey(
						BlobInfoFactory.KIND, blobKey.getKeyString()));
				if (entity == null) {
					System.out.println("NO INFO?");
					return blobKey;
				}
				final String md5Hash = (String) entity.getProperty(BlobInfoFactory.MD5_HASH);
				if (md5Hash == null || md5Hash.length() == 0) {
					entity.setProperty(BlobInfoFactory.MD5_HASH, IO.hexEncode(IO.md5(content)));
					DATASTORE_SERVICE.put(entity);
					System.out.println("WRITTEN HASH!");
				}
			}
			return blobKey;
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't write blob!", e);
		}
	}

	public Entity findBlobInfo(final byte[] content) {
		final List<Entity> blobInfoEntities = findBlobInfoEntities(IO.hexEncode(IO.md5(content)),
				(long) content.length);
		if (blobInfoEntities.isEmpty()) {
			return null;
		}
		for (final Entity entity : blobInfoEntities) {
			try {
				final BlobstoreInputStream bos = new BlobstoreInputStream(new BlobKey(entity
						.getKey().getName()));
				if (IO.contentEquals(bos, new ByteArrayInputStream(content))) {
					return entity;
					// could be more...but we are optimistic ;)
				}
			} catch (final IOException e) {
				throw new RuntimeException("Couldn't read blobstore stream for: " + entity, e);
			}
		}
		return null;
	}

	public List<Entity> findBlobInfoEntities(final String md5Hash, final Long size) {
		final Query duplicateQuery = new Query(BlobInfoFactory.KIND);
		duplicateQuery.addFilter(BlobInfoFactory.MD5_HASH, Query.FilterOperator.EQUAL, md5Hash);
		duplicateQuery.addFilter(BlobInfoFactory.SIZE, Query.FilterOperator.EQUAL, size);
		return DATASTORE_SERVICE.prepare(duplicateQuery)
				.asList(FetchOptions.Builder.withDefaults());
	}

}