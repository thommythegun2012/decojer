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
package org.decojer.web.controller;

import java.util.HashSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;

/**
 * Merge.
 * 
 * @author André Pankraz
 */
public class BlobStats {

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private final static int PAGE_SIZE = 10;

	private String doubleHashes;

	private int number;

	private long size;

	public void calculateStats() {
		final StringBuffer sb = new StringBuffer();
		long size = 0;

		final Query q = new Query("__BlobInfo__");
		final PreparedQuery pq = DATASTORE_SERVICE.prepare(q);
		final FetchOptions fetchOptions = FetchOptions.Builder.withLimit(PAGE_SIZE);

		final HashSet<String> hashes = new HashSet<String>();

		while (true) {
			final QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
			for (final Entity entity : results) {
				final String md5Hash = (String) entity.getProperty("md5_hash");
				size += (Long) entity.getProperty("size");

				if (hashes.contains(md5Hash)) {
					sb.append(md5Hash).append(", ");
					continue;
				}
				hashes.add(md5Hash);
			}

			if (results.size() < PAGE_SIZE || results.getCursor() == null) {
				break;
			}
			fetchOptions.startCursor(results.getCursor());
		}
		this.doubleHashes = sb.length() < 2 ? null : sb.substring(0, sb.length() - 2);
		this.number = hashes.size();
		this.size = size;
	}

	public String getDoubleHashes() {
		return this.doubleHashes;
	}

	public int getNumber() {
		return this.number;
	}

	public long getSize() {
		return this.size;
	}

}