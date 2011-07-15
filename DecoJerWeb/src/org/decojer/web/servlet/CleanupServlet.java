/*
 * $Id: UploadServlet.java 57 2011-07-09 12:51:55Z andrePankraz@googlemail.com $
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
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

/**
 * @author André Pankraz
 */
public class CleanupServlet extends HttpServlet {

	private static final long serialVersionUID = -6567596163814017159L;

	private final DatastoreService datastoreService = DatastoreServiceFactory
			.getDatastoreService();

	@Override
	public void doGet(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {
		final Query query = new Query("TYPE").setKeysOnly();
		final Iterator<Entity> asIterable = this.datastoreService
				.prepare(query).asIterable().iterator();
		final List<Key> list = new ArrayList<Key>();
		while (asIterable.hasNext()) {
			list.add(asIterable.next().getKey());
		}
		this.datastoreService.delete(list);
		res.getOutputStream().println("DELETED: " + list.size());
	}
}