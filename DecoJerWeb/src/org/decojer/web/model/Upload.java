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

import java.util.Date;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Entity;

/**
 * Upload entity.
 * 
 * @author André Pankraz
 */
public class Upload {

	private static final String PROP_CREATED = "created";

	private static final String PROP_ERROR = "error";

	private static final String PROP_FILENAME = "filename";

	private static final String PROP_REQUESTED = "requested";

	private static final String PROP_REQUESTS = "requests";

	private static final String PROP_SOURCE_BLOB_KEY = "sourceBlobKey";

	private static final String PROP_TDS = "tds";

	private static final String PROP_UPLOAD_BLOB_KEY = "uploadBlobKey";

	private final Entity entity;

	/**
	 * Constructor.
	 * 
	 * @param entity
	 *            wrapped entity
	 */
	public Upload(final Entity entity) {
		this.entity = entity;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Upload
				&& this.entity.equals(((Upload) obj).entity);
	}

	public Date getCreated() {
		return (Date) this.entity.getProperty(PROP_CREATED);
	}

	public String getError() {
		return (String) this.entity.getProperty(PROP_ERROR);
	}

	public String getFilename() {
		return (String) this.entity.getProperty(PROP_FILENAME);
	}

	public Date getRequested() {
		return (Date) this.entity.getProperty(PROP_REQUESTED);
	}

	public Long getRequests() {
		return (Long) this.entity.getProperty(PROP_REQUESTS);
	}

	public BlobKey getSourceBlobKey() {
		return (BlobKey) this.entity.getProperty(PROP_SOURCE_BLOB_KEY);
	}

	public Long getTds() {
		return (Long) this.entity.getProperty(PROP_TDS);
	}

	public BlobKey getUploadBlobKey() {
		return (BlobKey) this.entity.getProperty(PROP_UPLOAD_BLOB_KEY);
	}

	public Entity getWrappedEntity() {
		return this.entity;
	}

	@Override
	public int hashCode() {
		return this.entity.hashCode();
	}

	public void setCreated(final Date created) {
		this.entity.setUnindexedProperty(PROP_CREATED, created);
	}

	public void setError(final String error) {
		this.entity.setUnindexedProperty(PROP_ERROR, error);
	}

	public void setFilename(final String filename) {
		this.entity.setUnindexedProperty(PROP_FILENAME, filename);
	}

	public void setRequested(final Date requested) {
		this.entity.setUnindexedProperty(PROP_REQUESTED, requested);
	}

	public void setRequests(final Long requests) {
		this.entity.setUnindexedProperty(PROP_REQUESTS, requests);
	}

	public void setSourceBlobKey(final BlobKey sourceBlobKey) {
		this.entity.setUnindexedProperty(PROP_SOURCE_BLOB_KEY, sourceBlobKey);
	}

	public void setTds(final Long tds) {
		this.entity.setUnindexedProperty(PROP_TDS, tds);
	}

	public void setUploadBlobKey(final BlobKey uploadBlobKey) {
		this.entity.setUnindexedProperty(PROP_UPLOAD_BLOB_KEY, uploadBlobKey);
	}

}