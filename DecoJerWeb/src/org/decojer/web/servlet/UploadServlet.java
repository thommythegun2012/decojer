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
import org.decojer.web.analyser.BlobAnalyser;
import org.decojer.web.analyser.BlobInfo;
import org.decojer.web.analyser.ClassAnalyser;
import org.decojer.web.analyser.DexAnalyser;
import org.decojer.web.analyser.DexInfo;
import org.decojer.web.analyser.JarAnalyser;
import org.decojer.web.analyser.JarInfo;
import org.decojer.web.analyser.TypeInfo;
import org.decojer.web.util.EntityConstants;
import org.decojer.web.util.Messages;
import org.decojer.web.util.Uploads;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

/**
 * @author André Pankraz
 */
public class UploadServlet extends HttpServlet {

	private static Logger LOGGER = Logger.getLogger(UploadServlet.class
			.getName());

	private static final long serialVersionUID = -6567596163814017159L;

	private final BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();

	private final DatastoreService datastoreService = DatastoreServiceFactory
			.getDatastoreService();

	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {
		// check given upload BlobKey
		final Map<String, BlobKey> blobs = this.blobstoreService
				.getUploadedBlobs(req);
		final BlobKey blobKey = blobs.get("file");
		if (blobKey == null) {
			Messages.addMessage(req, "File was empty!");
			res.sendRedirect("/");
			return;
		}
		try {
			// read blob meta data for upload and find all duplicates;
			// attention: this servlet can rely on the existence of the
			// current uploads blob meta data via datastoreService.get(), but
			// the results from other queries are HA write lag dependend!
			final BlobInfo blobInfo = BlobAnalyser.uniqueBlobInfo(
					this.datastoreService, blobKey);
			final List<TypeInfo> typeInfos = new ArrayList<TypeInfo>();
			try {
				// check file name extension
				final int pos = blobInfo.getFilename().lastIndexOf('.');
				if (pos == -1) {
					throw new AnalyseException("The file extension is missing.");
				}
				final String ext = blobInfo.getFilename().substring(pos + 1)
						.toLowerCase();
				if ("class".equals(ext)) {
					final TypeInfo typeInfo;
					try {
						typeInfo = ClassAnalyser
								.analyse(new BlobstoreInputStream(blobInfo
										.getBlobKey()));
					} catch (final Exception e) {
						throw new AnalyseException(
								"This isn't a valid Java Class like the file extension suggests.");
					}
					typeInfos.add(typeInfo);
				} else if ("jar".equals(ext)) {
					final JarInfo jarInfo;
					try {
						jarInfo = JarAnalyser.analyse(new BlobstoreInputStream(
								blobInfo.getBlobKey()));
					} catch (final Exception e) {
						throw new AnalyseException(
								"This isn't a valid Java Class like the file extension suggests.");
					}
					LOGGER.info("JAR Check failures: " + jarInfo.checkFailures);
					if (jarInfo.typeInfos.size() == 0) {
						throw new AnalyseException(
								"This isn't a valid Java Archive like the file extension suggests.");
					}
					typeInfos.addAll(jarInfo.typeInfos);
				} else if ("dex".equals(ext)) {
					final DexInfo dexInfo;
					try {
						dexInfo = DexAnalyser.analyse(new BlobstoreInputStream(
								blobInfo.getBlobKey()));
					} catch (final Exception e) {
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
					throw new AnalyseException("Could not find any classes!");
				}
				// should appear as download link to user
				Uploads.addUpload(req, blobInfo);
			} catch (final AnalyseException e) {
				LOGGER.log(Level.INFO, e.getMessage());
				Messages.addMessage(
						req,
						e.getMessage()
								+ "  Please upload valid Java Classes or Archives (JAR) respectively Android / Dalvik Executable File (DEX).");
				// delete found unique too
				blobInfo.setError(e.getMessage());
				res.sendRedirect("/");
				return;
			} finally {
				final Set<BlobKey> deleteBlobKeys = blobInfo
						.getDeleteBlobKeys();
				if (blobInfo.getError() != null) {
					deleteBlobKeys.add(blobInfo.getBlobKey());
				}
				LOGGER.info("Deleting '" + deleteBlobKeys.size() + "' uploads.");
				this.blobstoreService.delete(deleteBlobKeys
						.toArray(new BlobKey[deleteBlobKeys.size()]));
				// log all uploads (original blob key);
				// this key is automatically unique
				final Entity entity = new Entity(EntityConstants.KIND_UPLOAD,
						blobKey.getKeyString());
				entity.setProperty(EntityConstants.PROP_FILENAME,
						blobInfo.getFilename());
				entity.setProperty(EntityConstants.PROP_MD5HASH,
						blobInfo.getMd5Hash());
				entity.setProperty(EntityConstants.PROP_SIZE,
						blobInfo.getSize());
				entity.setUnindexedProperty(EntityConstants.PROP_NEWEST,
						blobInfo.getNewestDate());
				entity.setUnindexedProperty(EntityConstants.PROP_OLDEST,
						blobInfo.getOldestDate());
				entity.setUnindexedProperty(EntityConstants.PROP_ERROR,
						blobInfo.getError());
				entity.setUnindexedProperty(EntityConstants.PROP_DELETED,
						deleteBlobKeys.size());
				entity.setUnindexedProperty(EntityConstants.PROP_TYPES,
						typeInfos.size());
				this.datastoreService.put(entity);
			}
			// now save type info refs
			System.out.println("TY: " + typeInfos.size());
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING,
					"Unexpected problem, couldn't evaluate upload: " + blobKey,
					e);
			Messages.addMessage(req,
					"Unexpected problem, couldn't evaluate upload!");
		}
		res.sendRedirect("/");
	}
}