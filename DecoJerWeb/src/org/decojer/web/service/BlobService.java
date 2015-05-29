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
import java.util.logging.Logger;

import lombok.Getter;

import org.decojer.web.util.DB;
import org.decojer.web.util.IO;

import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

/**
 * Blob service.
 *
 * @author André Pankraz
 */
public class BlobService {

	@Getter
	public class Stats {

		private int calculatedHashes;

		private String doubleHashes;

		private int number;

		private long size;

	}

	private static final BlobService INSTANCE = new BlobService();

	private static Logger LOGGER = Logger.getLogger(BlobService.class.getName());

	public static BlobService getInstance() {
		return INSTANCE;
	}

	/**
	 * Calculate Blob statistics. Generate and persist MD5 if necessary (sometimes not generated
	 * through backend at local development environment).
	 *
	 * @return Blob statistics
	 */
	public Stats calculateStats() {
		final Stats stats = new Stats();
		final StringBuffer doubleHashes = new StringBuffer();
		final HashSet<String> hashes = new HashSet<String>();

		DB.iterate(BlobInfoFactory.KIND, new DB.Processor() {

			@Override
			public boolean process(final Entity entity) {
				final long size = (Long) entity.getProperty(BlobInfoFactory.SIZE);
				if (size > 30000000) {
					LOGGER.info("Will delete JAR '" + entity.getProperty(BlobInfoFactory.FILENAME)
							+ "'. This is with " + size
							+ " bytes too large to be useful (max 30 MB).");
					BlobstoreServiceFactory.getBlobstoreService().delete(
							new BlobKey(entity.getKey().getName()));
					return true;
				}
				stats.size += size;
				String md5Hash = (String) entity.getProperty(BlobInfoFactory.MD5_HASH);
				if (md5Hash == null) {
					// can only happen and only work with development environment
					try {
						final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
								new BlobKey(entity.getKey().getName()));
						md5Hash = IO.hexEncode(IO.md5(IO.toBytes(blobstoreInputStream)));
						entity.setProperty(BlobInfoFactory.MD5_HASH, md5Hash);
						DatastoreServiceFactory.getDatastoreService().put(entity);
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
		final GcsService gcsService = GcsServiceFactory.createGcsService();
		try {
			final GcsFilename gcsFilename = new GcsFilename("decojer", fileName);
			final GcsFileOptions options = new GcsFileOptions.Builder().mimeType(mimeType)
					// .acl("public-read")
					// .addUserMetadata("myfield1", "my field value")
					.build();
			final GcsOutputChannel writeChannel = gcsService.createOrReplace(gcsFilename, options);

			final OutputStream fileOutputStream = Channels.newOutputStream(writeChannel);
			// don't hold file open for too long (around max. 30 seconds), else:
			// "Caused by: com.google.apphosting.api.ApiProxy$ApplicationException: ApplicationError: 10: Unknown",
			// don't write byte array directly (e.g. via nice
			// writeChannel.write(ByteBuffer.wrap(content))), else file write request too large:
			// "The request to API call file.Append() was too large."
			// (wrapping with BufferedOutputStream doesn't help - writes big data directly)
			IO.copy(new ByteArrayInputStream(content), fileOutputStream);
			/*
			 * try { Thread.sleep(5000); } catch (final InterruptedException ie) { // OK - sleep 5s
			 * for index updates }
			 */
			writeChannel.close();

			final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			final BlobKey blobKey = blobstoreService.createGsBlobKey("/gs/"
					+ gcsFilename.getBucketName() + "/" + gcsFilename.getObjectName());

			if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
				try {
					if (blobKey == null) {
						System.out.println("BlobKey is null? Severe Bug!");
						return null;
					}
					final DatastoreService datastoreService = DatastoreServiceFactory
							.getDatastoreService();
					final Entity entity = datastoreService.get(KeyFactory.createKey(
							BlobInfoFactory.KIND, blobKey.getKeyString()));
					if (entity == null) {
						System.out.println("NO INFO?");
						return blobKey;
					}
					final String md5Hash = (String) entity.getProperty(BlobInfoFactory.MD5_HASH);
					if (md5Hash == null || md5Hash.length() == 0) {
						entity.setProperty(BlobInfoFactory.MD5_HASH, IO.hexEncode(IO.md5(content)));
						datastoreService.put(entity);
						System.out.println("WRITTEN HASH!");
					}
				} catch (final EntityNotFoundException e) {
					// old method to solve local dev problems
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
		return DatastoreServiceFactory.getDatastoreService().prepare(duplicateQuery)
				.asList(FetchOptions.Builder.withDefaults());
	}

}