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
package org.decojer.web.util;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;

/**
 * Database utilities.
 * 
 * @author André Pankraz
 */
public class DB {

	public interface Processor {

		boolean process(Entity entity);

	}

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	public static Cursor iterate(final String kind, final int pageSize, final Processor processor) {
		final Query q = new Query(kind);
		final PreparedQuery pq = DATASTORE_SERVICE.prepare(q);
		// pagination because of max. 30s database operation timeout
		final FetchOptions fetchOptions = FetchOptions.Builder.withLimit(pageSize);
		while (true) {
			final QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
			for (final Entity entity : results) {
				if (!processor.process(entity)) {
					return results.getCursor();
				}
			}
			if (results.size() < pageSize || results.getCursor() == null) {
				return null;
			}
			fetchOptions.startCursor(results.getCursor());
		}
	}

	public static Cursor iterate(final String kind, final Processor processor) {
		return iterate(kind, 100, processor);
	}

}