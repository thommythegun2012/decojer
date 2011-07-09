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
package org.decojer.web.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.decojer.web.util.IOUtils;
import org.objectweb.asm.ClassReader;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;

/**
 * 
 * @author André Pankraz
 */
public class StreamAnalyzer {

	private static Logger LOGGER = Logger.getLogger(StreamAnalyzer.class
			.getName());

	private static int MAGIC_NUMBER_MAX_LENGTH = 4;

	private static byte[] MAGIC_NUMBER = new byte[MAGIC_NUMBER_MAX_LENGTH];

	private static final byte[] MAGIC_NUMBER_DEX_CLASS = { (byte) 0x64,
			(byte) 0x65, (byte) 0x78, (byte) 0x0A };

	private static final byte[] MAGIC_NUMBER_JAVA_CLASS = { (byte) 0xCA,
			(byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

	private static final byte[] MAGIC_NUMBER_ZIP = { (byte) 0x50, (byte) 0x4B,
			(byte) 0x03, (byte) 0x04 };

	private final DatastoreService datastoreService;

	public byte[] debug;

	public List<Entity> classEntities = new ArrayList<Entity>();

	public StreamAnalyzer(final DatastoreService datastoreService) {
		this.datastoreService = datastoreService;
	}

	public void visitStream(final InputStream is, final String name,
			final MessageDigest digest) throws IOException,
			NoSuchAlgorithmException {
		final int read = is.read(MAGIC_NUMBER, 0, MAGIC_NUMBER_MAX_LENGTH);
		if (read < MAGIC_NUMBER_MAX_LENGTH) {
			// System.out.println("  MN too small: " + read);
			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_JAVA_CLASS, MAGIC_NUMBER)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(MAGIC_NUMBER, 0, MAGIC_NUMBER_MAX_LENGTH);

			// rough test and statistics
			final ClassReader classReader = new ClassReader(
					digest == null ? pis : new DigestInputStream(pis, digest));

			final StatClassVisitor statClassVisitor = new StatClassVisitor();
			classReader.accept(statClassVisitor, ClassReader.SKIP_FRAMES);

			final Entity entity = new Entity("Class", statClassVisitor.name
					+ "_" + IOUtils.toHexString(digest.digest()));
			this.debug = classReader.b;
			entity.setProperty("content", new Blob(classReader.b));
			for (final Entry<String, Object> next : statClassVisitor.entityProperties
					.entrySet()) {
				entity.setProperty(next.getKey(), next.getValue());
			}
			this.classEntities.add(entity);
			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_DEX_CLASS, MAGIC_NUMBER)) {
			// LOGGER.warning("  Dex: " + name);

			// use http://code.google.com/p/dex2jar/

			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_ZIP, MAGIC_NUMBER)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(MAGIC_NUMBER, 0, MAGIC_NUMBER_MAX_LENGTH);

			final MessageDigest subDigest = MessageDigest.getInstance("MD5");

			final ZipInputStream zip = new ZipInputStream(digest == null ? pis
					: new DigestInputStream(pis, digest));
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				try {
					visitStream(zip, zipEntry.getName(), subDigest);
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Zip entry problems: ", e);
				}
			}
			// LOGGER.warning("  Zip: " + name
			// + IOUtils.toHexString(digest.digest()));
			return;
		}
		// System.out.println("  Unknown!");
		return;
	}

}