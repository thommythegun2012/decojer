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
package org.decojer.web;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;

/**
 * 
 * @author André Pankraz
 */
public class ListServlet extends HttpServlet {

	private static Logger LOGGER = Logger.getLogger(UploadServlet.class
			.getName());

	private static final long serialVersionUID = -6567596163814017159L;

	private final BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();

	private final DatastoreService datastoreService = DatastoreServiceFactory
			.getDatastoreService();

	private final FileService fileService = FileServiceFactory.getFileService();

	@Override
	public void doGet(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {

		final ServletOutputStream out = res.getOutputStream();

		final Query q = new Query("__BlobInfo__");

		final PreparedQuery pq = this.datastoreService.prepare(q);

		for (final Entity result : pq.asIterable()) {
			out.println("Class: " + result.getClass().getName());
			out.println("AppId: " + result.getAppId());
			out.println("Kind: " + result.getKind());
			out.println("Namespace: " + result.getNamespace());
			out.println("Key: " + result.getKey());
			out.println("Key: " + result.getKey().getName());
			out.println("Parent: " + result.getParent());
			out.println("Props: " + result.getProperties());
			out.println("---");

			if (result.getProperty("md5_hash") == null) {
				result.setProperty("md5_hash", "HURRAY");
				this.datastoreService.put(result);
			}

			final BlobKey blobKey = new BlobKey(result.getKey().getName());
			final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
					blobKey);
			final byte[] test = new byte[100];
			blobstoreInputStream.read(test);
			// out.write(test);

			out.println("---");

			final Query q2 = new Query("__BlobInfo__");
			final Key key = KeyFactory.createKey("__BlobInfo__",
					blobKey.getKeyString());
			q2.addFilter("__key__", Query.FilterOperator.EQUAL, key);

			out.println("Props####: "
					+ this.datastoreService.prepare(q2).asSingleEntity()
							.getProperties());

		}

	}
}