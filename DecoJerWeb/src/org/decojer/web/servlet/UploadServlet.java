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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.decojer.DecoJer;
import org.decojer.PackageClassStreamProvider;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.PF;
import org.decojer.cavaj.model.TD;
import org.decojer.web.stream.StreamAnalyzer;
import org.decojer.web.util.BlobChecker;
import org.decojer.web.util.ClassChecker;
import org.decojer.web.util.DexChecker;
import org.decojer.web.util.IOUtils;
import org.decojer.web.util.Messages;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

/**
 * 
 * @author André Pankraz
 */
public class UploadServlet extends HttpServlet {

	class UploadTypeException extends RuntimeException {

		public UploadTypeException(final String message) {
			super(message);
		}

	}

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

		// read uploaded blob
		final Map<String, BlobKey> blobs = this.blobstoreService
				.getUploadedBlobs(req);
		BlobKey blobKey = blobs.get("file");
		if (blobKey == null) {
			Messages.addMessage(req, "File was empty!");
			res.sendRedirect("/");
			return;
		}

		final List<BlobKey> deleteBlobKeys = new ArrayList<BlobKey>();

		final String filename;
		final String md5Hash;
		final int size;

		// transaction only for transaction level SERIALIZABLE, always commit
		// Transaction tx = this.datastoreService.beginTransaction();
		try {
			final BlobChecker blobChecker = new BlobChecker(
					this.datastoreService, blobKey);
			blobKey = blobChecker.getUniqueBlobKey();
			deleteBlobKeys.addAll(blobChecker.getDeleteBlobKeys());

			filename = blobChecker.getFilename();
			md5Hash = blobChecker.getMd5Hash();
			size = blobChecker.getSize().intValue();

			if (!deleteBlobKeys.isEmpty()) {
				Messages.addMessage(req,
						"Duplicate! Delete " + deleteBlobKeys.size()
								+ " entries!");
			}

			final int pos = filename.lastIndexOf('.');
			if (pos == -1) {
				throw new UploadTypeException("The file extension is missing.");
			}
			final String ext = filename.substring(pos + 1).toLowerCase();

			if ("class".equals(ext)) {
				// asm.ClassReader reads streams into byte array with
				// available() sized buffer, which is 0!
				// better read fully now...
				final byte[] content = this.blobstoreService.fetchData(blobKey,
						0, size);
				final ClassChecker classChecker;
				try {
					classChecker = new ClassChecker(content);
				} catch (final Exception e) {
					throw new UploadTypeException(
							"This isn't a valid Java Class like the file extension suggests.");
				}
				// className:
				// org/decojer/cavaj/test/jdk6/DecTestParametrizedMethods
				// fileName: DecTestParametrizedMethods.class
			} else if ("jar".equals(ext)) {
				final MessageDigest digest = MessageDigest.getInstance("MD5");
				int checkFailures = 0;

				final ZipInputStream zip = new ZipInputStream(
						new BlobstoreInputStream(blobKey));
				for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
						.getNextEntry()) {
					final String name = zipEntry.getName();
					if (!name.endsWith(".class")) {
						continue;
					}
					// asm.ClassReader reads streams into byte array with
					// available() sized buffer, which is 0!
					// better read fully now...
					final byte[] bytes = IOUtils.toBytes(new DigestInputStream(
							zip, digest));
					final ClassChecker classChecker;
					try {
						classChecker = new ClassChecker(bytes);
					} catch (final Exception e) {
						++checkFailures;
						continue;
					}
					classChecker.getName();
					classChecker.getSignature();
				}
			} else if ("dex".equals(ext)) {
				// asm.ClassReader reads streams into byte array with
				// available() sized buffer, which is 0!
				// better read fully now...
				final byte[] content = this.blobstoreService.fetchData(blobKey,
						0, size);
				final DexChecker dexChecker;
				try {
					dexChecker = new DexChecker(content);
				} catch (final Exception e) {
					throw new UploadTypeException(
							"This isn't a valid Android / Dalvik Executable like the file extension suggests.");
				}
				Messages.addMessage(req, dexChecker.getClasses()
						+ " Classes found. Sry not ready yet!");
				res.sendRedirect("/");
				return;
			} else if ("apk".equals(ext)) {
				throw new UploadTypeException(
						"Sorry, because of quota limits the online version doesn't support the direct decompilation of Android Package Archives (APK). Please unzip and upload the contained 'classes.dex' Dalvik Executable File (DEX).");
			} else if ("war".equals(ext)) {
				throw new UploadTypeException(
						"Sorry, because of quota limits the online version doesn't support the direct decompilation of Web Application Archives (WAR), often containing multiple embedded Java Archives (JAR).");
			} else if ("ear".equals(ext)) {
				throw new UploadTypeException(
						"Sorry, because of quota limits the online version doesn't support the direct decompilation of Enterprise Application Archives (EAR), often containing multiple embedded Java Archives (JAR).");
			} else {
				throw new UploadTypeException(
						"This is an unknown file extension.");
			}
		} catch (final UploadTypeException e) {
			LOGGER.log(Level.INFO, e.getMessage());
			Messages.addMessage(
					req,
					e.getMessage()
							+ "  Please upload Java Classes or Archives (JAR) respectively Android / Dalvik Executable File (DEX).");
			res.sendRedirect("/");
			deleteBlobKeys.add(blobKey);
			return;
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Couldn't evaluate upload meta data!", e);
			Messages.addMessage(req,
					"Couldn't evaluate upload meta data! Upload deleted.");
			res.sendRedirect("/");
			deleteBlobKeys.add(blobKey);
			return;
		} finally {
			LOGGER.info("Deleting '" + deleteBlobKeys.size() + "' uploads.");
			this.blobstoreService.delete(deleteBlobKeys
					.toArray(new BlobKey[deleteBlobKeys.size()]));
		}

		final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
				blobKey);

		// transaction only for transaction level SERIALIZABLE, always commit
		// tx = this.datastoreService.beginTransaction();
		try {
			// TODO
			final MessageDigest subDigest = MessageDigest.getInstance("MD5");
			final StreamAnalyzer streamAnalyzer = new StreamAnalyzer(
					this.datastoreService);
			streamAnalyzer.visitStream(blobstoreInputStream, filename,
					subDigest);

			// 2011-07-09 05:02:03.668 /upload 302 30443ms 1757400cpu_ms
			// 1747537api_cpu_ms 0kb Mozilla/5.0 (Windows NT 6.0; rv:5.0)
			// Gecko/20100101 Firefox/5.0,gzip(gfe),gzip(gfe),gzip(gfe)

			// if (!duplicateBlobKeys.isEmpty()) {
			// 4.3 MB JAR => 290 s, Rewrite 180 s,
			// following batch get costs 12 s and reduces rewrite to 15 s,
			// with single gets and Key-Only-Queries reduced to 18 s
			Messages.addMessage(req, "Check uploaded Java classes: "
					+ streamAnalyzer.classEntities.size());

			final List<Key> keys = new ArrayList<Key>();
			for (final Entity entity : streamAnalyzer.classEntities) {
				keys.add(entity.getKey());
			}
			final Map<Key, Entity> map = this.datastoreService.get(keys);
			streamAnalyzer.classEntities.removeAll(map.values());
			// }

			// 20 MB EAR with 9886 Classes
			// 29.29 min!
			// only 10 seconds not in API stuff
			// without binary 15 minutes:
			// 10 entities pro CPU second, 1.000.000 => 27 CPU hours
			final List<Key> put = this.datastoreService
					.put(streamAnalyzer.classEntities);

			Messages.addMessage(req, "Updated Java classes: "
					+ streamAnalyzer.classEntities.size());
			try {
				final PackageClassStreamProvider packageClassStreamProvider = new PackageClassStreamProvider(
						null);
				packageClassStreamProvider.addClassStream("TEST",
						new ByteArrayInputStream(streamAnalyzer.debug));
				final PF pf = DecoJer.createPF(packageClassStreamProvider);
				final Entry<String, TD> next = pf.getTds().entrySet()
						.iterator().next();
				final CU cu = DecoJer.createCU(next.getValue());
				final String source = DecoJer.decompile(cu);

				Messages.addMessage(req,
						"This is currently only a pre-alpha test-version!");
				Messages.addMessage(req, "<pre>" + source + "</pre>");
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
				Messages.addMessage(req, "Decompilation problems!");
				res.sendRedirect("/");
				return;
			}

		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Problems with stream visitor.", e);
			Messages.addMessage(req, "Internal system problem!");
			res.sendRedirect("/");
			return;
		}// finally {
			// if (tx.isActive()) {
			// tx.commit();
			// }
			// }
		finally {
		}

		res.sendRedirect("/");
	}
}