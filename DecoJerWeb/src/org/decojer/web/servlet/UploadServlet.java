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

import org.decojer.DecoJer;
import org.decojer.web.model.Upload;
import org.decojer.web.service.BlobService;
import org.decojer.web.util.IO;
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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
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

	private static Logger LOGGER = Logger.getLogger(UploadServlet.class.getName());

	private static final long serialVersionUID = -6567596163814017159L;

	@Override
	public void doPost(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		// same target page in all cases
		resp.sendRedirect("/");

		final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		final BlobInfoFactory blobInfoFactory = new BlobInfoFactory(datastoreService);

		// check uploaded blob from GAE upload service
		final Map<String, BlobKey> uploadedBlobs = blobstoreService.getUploadedBlobs(req);
		if (uploadedBlobs.get("file") == null) {
			Messages.addMessage(req, "Upload was empty!");
			return;
		}
		// uses DatastoreService.get(), seems to be no HA-lagging here
		BlobInfo uploadBlobInfo = blobInfoFactory.loadBlobInfo(uploadedBlobs.get("file"));
		if (uploadBlobInfo == null) {
			LOGGER.warning("Missing upload information for '" + uploadedBlobs.get("file") + "'!");
			Messages.addMessage(req, "Missing upload information!");
			return;
		}

		try {
			// blob is new, but content might be duplication -> find oldest, duplication indicated
			// through equal hash and size
			final String md5Hash = uploadBlobInfo.getMd5Hash();
			final long size = uploadBlobInfo.getSize();

			// because of lagging HA writes following query could even be empty
			final List<Entity> blobInfoEntities = BlobService.getInstance().findBlobInfoEntities(
					md5Hash, size);

			// now find and keep oldest entity, start with myself (uploadBlobInfo)
			final Set<BlobKey> duplicateBlobKeys = new HashSet<BlobKey>();
			Date lastAccess = uploadBlobInfo.getCreation();
			for (final Entity blobInfoEntity : blobInfoEntities) {
				final BlobInfo blobInfo = blobInfoFactory.createBlobInfo(blobInfoEntity);
				if (uploadBlobInfo.equals(blobInfo)) {
					continue;
				}
				// one must die now...
				if (lastAccess.compareTo(blobInfo.getCreation()) < 0) {
					lastAccess = blobInfo.getCreation();
				}
				if (uploadBlobInfo.getCreation().compareTo(blobInfo.getCreation()) < 0) {
					duplicateBlobKeys.add(blobInfo.getBlobKey());
				} else {
					// change upload
					duplicateBlobKeys.add(uploadBlobInfo.getBlobKey());
					uploadBlobInfo = blobInfo;
				}
			}
			// short unique upload-entity key: base91(hash|size)
			final Key uploadKey = KeyFactory.createKey(Upload.KIND, IO.toKey(md5Hash, size));
			// none-transactional quick-check if upload-entity already exists
			Upload upload;
			try {
				upload = new Upload(datastoreService.get(uploadKey));
			} catch (final EntityNotFoundException e) {
				upload = new Upload(new Entity(uploadKey));
				upload.setFilename(uploadBlobInfo.getFilename());
				upload.setRequests(0L);
			}
			if (upload.getSourceBlobKey() != null) {
				try {
					blobstoreService.fetchData(upload.getSourceBlobKey(), 0L, 3L);
				} catch (final IllegalArgumentException e) {
					upload.setSourceBlobKey(null);
				}
			}
			if (upload.getSourceBlobKey() == null) {
				upload.setError(null);
				// read blob meta data for upload and find all duplicates;
				// attention: this servlet can rely on the existence of the
				// current uploads blob meta data via datastoreService.get(),
				// but results from other queries are HA write lag dependend!
				final int artefacts = DecoJer.analyze(new BlobstoreInputStream(uploadBlobInfo
						.getBlobKey()));
				if (artefacts == 0) {
					LOGGER.log(Level.INFO, "No artefacts.");
					Messages.addMessage(
							req,
							"Please upload valid Java Classes or Archives (CLASS, JAR, EAR) respectively Android / Dalvik Executable File (DEX, APK).");
					upload.setError("No artefacts.");
				}
				upload.setTds((long) artefacts);
			}
			upload.setUploadBlobKey(uploadBlobInfo.getBlobKey());
			upload.setCreated(uploadBlobInfo.getCreation());
			upload.setRequested(lastAccess);
			upload.setRequests(upload.getRequests() + 1L + duplicateBlobKeys.size());

			final Transaction tx = datastoreService.beginTransaction();
			try {
				datastoreService.put(upload.getWrappedEntity());

				if (upload.getError() == null && upload.getSourceBlobKey() == null) {
					QueueFactory.getQueue("decoJer").add(
							TaskOptions.Builder
									.withMethod(Method.GET)
									.param("uploadKey", uploadKey.getName())
									.param("channelKey", Uploads.getChannelKey(req.getSession()))
									.countdownMillis(2000)
									.header("Host",
											BackendServiceFactory.getBackendService()
													.getBackendAddress("worker256")));
				}
			} finally {
				tx.commit();
			}
			blobstoreService
					.delete(duplicateBlobKeys.toArray(new BlobKey[duplicateBlobKeys.size()]));

			if (upload.getError() != null) {
				return;
			}
			// add message and link
			Messages.addMessage(req, "Found "
					+ upload.getTds()
					+ (upload.getTds().longValue() == 1L ? " readable artefact."
							: " readable artefacts."));
			Uploads.addUploadKey(req, uploadKey);
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Unexpected problem, couldn't evaluate upload: "
					+ uploadBlobInfo.getBlobKey(), e);
			Messages.addMessage(req, "Unexpected problem, couldn't evaluate upload!");
		}
	}

}