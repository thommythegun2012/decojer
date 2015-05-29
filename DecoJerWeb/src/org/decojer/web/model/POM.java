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
package org.decojer.web.model;

import java.util.logging.Logger;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;

/**
 * POM (version).
 *
 * @author André Pankraz
 */
public class POM extends BaseEntity {

	/**
	 * Entity kind.
	 */
	public static final String KIND = "POM";

	private static Logger LOGGER = Logger.getLogger(POM.class.getName());

	/**
	 * Blob property "content".
	 */
	public static final String PROP_CONTENT = "content";

	/**
	 * BlobKey property "upload".
	 */
	public static final String PROP_JAR = "jar";

	/**
	 * Constructor.
	 *
	 * @param entity
	 */
	public POM(final Entity entity) {
		super(entity);
	}

	/**
	 * Get artifact id.
	 *
	 * @return artifact id
	 */
	public String getArtifactId() {
		final String id = getId();
		final int pos = id.indexOf(':') + 1;
		final int pos2 = id.indexOf(':', pos);
		return id.substring(pos, pos2);
	}

	/**
	 * Get POM content.
	 *
	 * @return POM content
	 */
	public byte[] getContent() {
		final Blob blob = (Blob) this.entity.getProperty(PROP_CONTENT);
		return blob.getBytes();
	}

	/**
	 * Get group id.
	 *
	 * @return group id
	 */
	public String getGroupId() {
		final String id = getId();
		final int pos = id.indexOf(':');
		return id.substring(0, pos);
	}

	/**
	 * Get JAR Blob Key.
	 *
	 * @return JAR Blob Key
	 */
	public BlobKey getJarBlobKey() {
		final Object property = this.entity.getProperty(PROP_JAR);
		if (!(property instanceof BlobKey)) {
			LOGGER.warning("Property JAR = '" + property + "' isn't a BlobKey!");
			return null;
		}
		return (BlobKey) property;
	}

	@Override
	public String getKind() {
		return KIND;
	}

	/**
	 * Get version.
	 *
	 * @return version
	 */
	public String getVersion() {
		final String id = getId();
		int pos = id.indexOf(':') + 1;
		pos = id.indexOf(':', pos);
		return id.substring(pos + 1);
	}

	/**
	 * Set POM content.
	 *
	 * @param content
	 *            POM content
	 */
	public void setContent(final byte[] content) {
		this.entity.setUnindexedProperty(PROP_CONTENT, new Blob(content));
	}

	/**
	 * Set JAR Blob Key.
	 *
	 * @param blobKey
	 *            JAR Blob Key
	 */
	public void setJarBlobKey(final BlobKey blobKey) {
		this.entity.setProperty(PROP_JAR, blobKey);
	}

}