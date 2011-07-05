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

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.ClassPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.decojer.DecoJer;
import org.decojer.PackageClassStreamProvider;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.PF;
import org.decojer.cavaj.model.TD;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

/**
 * 
 * @author André Pankraz
 */
public class UploadServlet extends HttpServlet {

	private static Logger LOGGER = Logger.getLogger(UploadServlet.class
			.getName());

	private static final long serialVersionUID = -6567596163814017159L;

	private final BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();

	private final FileService fileService = FileServiceFactory.getFileService();

	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {

		final Map<String, BlobKey> blobs = this.blobstoreService
				.getUploadedBlobs(req);

		BlobKey blobKey = null;
		for (final Entry<String, BlobKey> entry : blobs.entrySet()) {
			LOGGER.warning("TEST: " + entry.getKey());
			LOGGER.warning("TEST: " + entry.getValue().getKeyString());
			if (blobKey == null) {
				blobKey = entry.getValue();
			}
		}

		if (blobKey == null) {
			res.sendRedirect("/index.jsp");
			return;
		}
		try {

			final PackageClassStreamProvider packageClassStreamProvider = new PackageClassStreamProvider(
					null);
			packageClassStreamProvider.addClassStream(blobKey.getKeyString(),
					new DataInputStream(new BlobstoreInputStream(blobKey)));
			final PF pf = DecoJer.createPF(packageClassStreamProvider);
			final Entry<String, TD> next = pf.getTds().entrySet().iterator()
					.next();
			final CU cu = DecoJer.createCU(next.getValue());
			final String source = DecoJer.decompile(cu);

			final AppEngineFile file = this.fileService.createNewBlobFile(
					"text/plain", cu.getStartTd().getName() + ".java");
			final FileWriteChannel writeChannel = this.fileService
					.openWriteChannel(file, true);
			writeChannel.write(ByteBuffer.wrap(source.getBytes()));
			writeChannel.closeFinally();
			final BlobKey blobKeySource = this.fileService.getBlobKey(file);

			res.sendRedirect("/decompile?blob-key="
					+ blobKeySource.getKeyString());
			return;
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Upload problems for blob key: "
					+ blobKey, e);
		} finally {
			ClassPool.getDefault().clearImportedPackages();
		}
		res.sendRedirect("/index.jsp");
	}

}