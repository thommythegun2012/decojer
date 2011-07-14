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
package org.decojer.web.analyser;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import org.decojer.web.util.EntityConstants;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Entity;

public class BlobInfo implements Serializable {

	private static final long serialVersionUID = 1521504215210091647L;

	private final Entity blobInfoEntity;

	private final Set<BlobKey> duplicateBlobs;

	private String error;

	private Date newestDate;

	private Date oldestDate;

	private int types;

	public BlobInfo(final Entity blobInfoEntity,
			final Set<BlobKey> duplicateBlobs) {
		this.blobInfoEntity = blobInfoEntity;
		this.duplicateBlobs = duplicateBlobs;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof BlobInfo
				&& this.blobInfoEntity.equals(((BlobInfo) obj)
						.getBlobInfoEntity());
	}

	public Entity getBlobInfoEntity() {
		return this.blobInfoEntity;
	}

	public BlobKey getBlobKey() {
		return new BlobKey(this.blobInfoEntity.getKey().getName());
	}

	public Date getCreation() {
		return (Date) this.blobInfoEntity
				.getProperty(EntityConstants.PROP_CREATION);
	}

	public Set<BlobKey> getDuplicateBlobs() {
		return this.duplicateBlobs;
	}

	public String getError() {
		return this.error;
	}

	public String getFilename() {
		return (String) this.blobInfoEntity
				.getProperty(EntityConstants.PROP_FILENAME);
	}

	public String getMd5Hash() {
		return (String) this.blobInfoEntity
				.getProperty(EntityConstants.PROP_MD5HASH);
	}

	public Date getNewestDate() {
		return this.newestDate;
	}

	public Date getOldestDate() {
		return this.oldestDate;
	}

	public Long getSize() {
		return (Long) this.blobInfoEntity
				.getProperty(EntityConstants.PROP_SIZE);
	}

	public int getTypes() {
		return this.types;
	}

	@Override
	public int hashCode() {
		return this.blobInfoEntity.hashCode();
	}

	public void setError(final String error) {
		this.error = error;
	}

	public void setNewestDate(final Date newestDate) {
		this.newestDate = newestDate;
	}

	public void setOldestDate(final Date oldestDate) {
		this.oldestDate = oldestDate;
	}

	public void setTypes(final int types) {
		this.types = types;
	}
}