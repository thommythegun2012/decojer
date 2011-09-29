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
package org.decojer.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.decojer.web.analyser.AnalyseException;
import org.decojer.web.analyser.ClassAnalyser;
import org.decojer.web.analyser.DexAnalyser;
import org.decojer.web.analyser.DexInfo;
import org.decojer.web.analyser.JarAnalyser;
import org.decojer.web.analyser.JarInfo;
import org.decojer.web.analyser.TypeInfo;
import org.decojer.web.model.Upload;
import org.decojer.web.util.IOUtils;
import org.decojer.web.util.Messages;
import org.decojer.web.util.Uploads;

import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.blobstore.BlobInfo;
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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

/**
 * Upload servlet.
 * 
 * @author André Pankraz
 */
public class UploadServlet extends HttpServlet {

	private static final BlobstoreService BLOBSTORE_SERVICE = BlobstoreServiceFactory
			.getBlobstoreService();

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private static final BlobInfoFactory DATASTORE_SERVICE_BLOBINFO_FACTORY = new BlobInfoFactory(
			DATASTORE_SERVICE);

	private static Logger LOGGER = Logger.getLogger(UploadServlet.class
			.getName());

	private static final long serialVersionUID = -6567596163814017159L;

	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		// same target page in all cases
		resp.sendRedirect("/");

		// check uploaded blob from GAE upload service
		final Map<String, BlobKey> uploadedBlobs = BLOBSTORE_SERVICE
				.getUploadedBlobs(req);
		if (uploadedBlobs.get("file") == null) {
			Messages.addMessage(req, "Upload was empty!");
			return;
		}
		// uses DatastoreService.get(), seems to be no HA-lagging here
		BlobInfo uploadBlobInfo = DATASTORE_SERVICE_BLOBINFO_FACTORY
				.loadBlobInfo(uploadedBlobs.get("file"));
		if (uploadBlobInfo == null) {
			LOGGER.warning("Missing upload information for '"
					+ uploadedBlobs.get("file") + "'!");
			Messages.addMessage(req, "Missing upload information!");
			return;
		}
		try {
			// blob is new, but content might be duplication -> find oldest,
			// duplication indicated through equal hash and size
			final String md5Hash = uploadBlobInfo.getMd5Hash();
			final Long size = uploadBlobInfo.getSize();
			// because of lagging HA writes following query could even be empty
			final Query duplicateQuery = new Query(BlobInfoFactory.KIND);
			duplicateQuery.addFilter(BlobInfoFactory.MD5_HASH,
					Query.FilterOperator.EQUAL, md5Hash);
			duplicateQuery.addFilter(BlobInfoFactory.SIZE,
					Query.FilterOperator.EQUAL, size);
			final List<Entity> blobInfoEntities = DATASTORE_SERVICE.prepare(
					duplicateQuery).asList(FetchOptions.Builder.withDefaults());

			// now find and keep oldest entity, start with myself
			// (uploadBlobInfo)
			final Set<BlobKey> duplicateBlobKeys = new HashSet<BlobKey>();
			Date lastAccess = uploadBlobInfo.getCreation();
			for (final Entity blobInfoEntity : blobInfoEntities) {
				final BlobInfo blobInfo = DATASTORE_SERVICE_BLOBINFO_FACTORY
						.createBlobInfo(blobInfoEntity);
				if (uploadBlobInfo.equals(blobInfo)) {
					continue;
				}
				// one must die now...
				if (lastAccess.compareTo(blobInfo.getCreation()) < 0) {
					lastAccess = blobInfo.getCreation();
				}
				if (uploadBlobInfo.getCreation().compareTo(
						blobInfo.getCreation()) < 0) {
					duplicateBlobKeys.add(blobInfo.getBlobKey());
				} else {
					// change upload
					duplicateBlobKeys.add(uploadBlobInfo.getBlobKey());
					uploadBlobInfo = blobInfo;
				}
			}
			// short unique upload-entity key: base91(hash|size)
			final Key uploadKey = KeyFactory.createKey(Upload.KIND,
					IOUtils.toKey(md5Hash, size));
			// none-transactional quick-check if upload-entity already exists
			Upload upload;
			try {
				upload = new Upload(DATASTORE_SERVICE.get(uploadKey));
			} catch (final EntityNotFoundException e) {
				upload = new Upload(new Entity(uploadKey));
				upload.setFilename(uploadBlobInfo.getFilename());
				upload.setRequests(0L);
			}
			if (upload.getSourceBlobKey() == null) {
				upload.setError(null);
				// read blob meta data for upload and find all duplicates;
				// attention: this servlet can rely on the existence of the
				// current uploads blob meta data via datastoreService.get(),
				// but results from other queries are HA write lag dependend!
				final List<TypeInfo> typeInfos = new ArrayList<TypeInfo>();
				try {
					// check file name extension
					final int pos = upload.getFilename().lastIndexOf('.');
					if (pos == -1) {
						throw new AnalyseException(
								"The file extension is missing.");
					}
					final String ext = upload.getFilename().substring(pos + 1)
							.toLowerCase();
					if ("class".equals(ext)) {
						final TypeInfo typeInfo;
						try {
							typeInfo = ClassAnalyser
									.analyse(new BlobstoreInputStream(
											uploadBlobInfo.getBlobKey()));
						} catch (final Exception e) {
							throw new AnalyseException(
									"This isn't a valid Java Class like the file extension suggests.");
						}
						typeInfos.add(typeInfo);
					} else if ("jar".equals(ext)) {
						final JarInfo jarInfo;
						try {
							jarInfo = JarAnalyser
									.analyse(new BlobstoreInputStream(
											uploadBlobInfo.getBlobKey()));
						} catch (final Exception e) {
							throw new AnalyseException(
									"This isn't a valid Java Archive like the file extension suggests.");
						}
						LOGGER.info("JAR analyzed with "
								+ jarInfo.typeInfos.size() + " types and "
								+ jarInfo.checkFailures + " check failures.");
						if (jarInfo.typeInfos.size() == 0) {
							throw new AnalyseException(
									"This isn't a valid Java Archive like the file extension suggests.");
						}
						typeInfos.addAll(jarInfo.typeInfos);
					} else if ("dex".equals(ext)) {
						final DexInfo dexInfo;
						try {
							dexInfo = DexAnalyser
									.analyse(new BlobstoreInputStream(
											uploadBlobInfo.getBlobKey()));
						} catch (final Exception e) {
							throw new AnalyseException(
									"This isn't a valid Android / Dalvik Executable like the file extension suggests.");
						}
						LOGGER.info("DEX analyzed with "
								+ dexInfo.typeInfos.size() + " types.");
						if (dexInfo.typeInfos.size() == 0) {
							throw new AnalyseException(
									"This isn't a valid Android / Dalvik Executable like the file extension suggests.");
						}
						typeInfos.addAll(dexInfo.typeInfos);
					} else if ("apk".equals(ext)) {
						throw new AnalyseException(
								"Sorry, because of quota limits the online version doesn't support the direct decompilation of Android Package Archives (APK). Please unzip and upload the contained 'classes.dex' Dalvik Executable File (DEX).");
					} else if ("war".equals(ext)) {
						throw new AnalyseException(
								"Sorry, because of quota limits the online version doesn't support the direct decompilation of Web Application Archives (WAR), often containing multiple embedded Java Archives (JAR).");
					} else if ("ear".equals(ext)) {
						throw new AnalyseException(
								"Sorry, because of quota limits the online version doesn't support the direct decompilation of Enterprise Application Archives (EAR), often containing multiple embedded Java Archives (JAR).");
					} else {
						throw new AnalyseException("The file extension '" + ext
								+ "' is unknown.");
					}
					if (typeInfos.size() == 0) {
						throw new AnalyseException(
								"Could not find any classes!");
					}
					upload.setTds((long) typeInfos.size());
				} catch (final AnalyseException e) {
					LOGGER.log(Level.INFO, e.getMessage());
					Messages.addMessage(
							req,
							e.getMessage()
									+ "  Please upload valid Java Classes or Archives (JAR) respectively Android / Dalvik Executable File (DEX).");
					upload.setError(e.getMessage());
				}
			}
			upload.setUploadBlobKey(uploadBlobInfo.getBlobKey());
			upload.setCreated(uploadBlobInfo.getCreation());
			upload.setRequested(lastAccess);
			upload.setRequests(upload.getRequests() + 1L
					+ duplicateBlobKeys.size());

			final Transaction tx = DATASTORE_SERVICE.beginTransaction();
			try {
				DATASTORE_SERVICE.put(upload.getWrappedEntity());

				if (upload.getError() == null
						&& upload.getSourceBlobKey() == null) {
					QueueFactory
							.getQueue("decoJer")
							.add(TaskOptions.Builder
									.withMethod(Method.GET)
									.param("uploadKey", uploadKey.getName())
									.param("channelKey",
											Uploads.getChannelKey(req
													.getSession()))
									.countdownMillis(2000)
									.header("Host",
											BackendServiceFactory
													.getBackendService()
													.getBackendAddress("worker")));
				}
			} finally {
				tx.commit();
			}
			BLOBSTORE_SERVICE.delete(duplicateBlobKeys
					.toArray(new BlobKey[duplicateBlobKeys.size()]));

			if (upload.getError() != null) {
				return;
			}
			// add message and link
			Messages.addMessage(req, "Found "
					+ upload.getTds()
					+ (upload.getTds().longValue() == 1L ? " class."
							: " classes."));
			Uploads.addUploadKey(req, uploadKey);
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING,
					"Unexpected problem, couldn't evaluate upload: "
							+ uploadBlobInfo.getBlobKey(), e);
			Messages.addMessage(req,
					"Unexpected problem, couldn't evaluate upload!");
		}
	}

}