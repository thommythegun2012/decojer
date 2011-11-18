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
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.decojer.web.model.Pom;
import org.decojer.web.service.MavenService;
import org.decojer.web.util.DB;

import com.google.appengine.api.datastore.Entity;

/**
 * http://worker.decojer.appspot.com/admin/cleanup
 * 
 * @author André Pankraz
 */
public class CleanupServlet extends HttpServlet {

	private static final long serialVersionUID = -6567596163814017159L;

	@Override
	public void doGet(final HttpServletRequest req, final HttpServletResponse res)
			throws ServletException, IOException {
		final PrintWriter out = res.getWriter();
		out.println("<ul>");

		final MavenService mavenService = MavenService.getInstance();
		final HashSet<String> done = new HashSet<String>();

		DB.iterate(Pom.KIND, new DB.Processor() {

			@Override
			public void process(final Entity entity) {
				final Pom pom = new Pom(entity);
				final String groupId = pom.getGroupId();
				final String artifactId = pom.getArtifactId();

				final String doneId = groupId + ':' + artifactId;
				if (done.contains(doneId)) {
					return;
				}
				done.add(doneId);
				out.println("<li>" + doneId + "</li>");
				out.flush();
				final List<String> versions = mavenService.fetchVersions(groupId, artifactId);
				for (final String version : versions) {
					if (mavenService.importPom(groupId, artifactId, version)) {
						out.println("<li>Imported: " + doneId + ":" + version + "</li>");
					}
					out.flush();
				}
			}

		});
		/*
		 * final byte[] base91Decode = IOUtils.base91Decode(key); final byte[] md5bytes = new
		 * byte[16]; System.arraycopy(base91Decode, 0, md5bytes, 0, 16); final String md5 =
		 * IOUtils.hexEncode(md5bytes); final long size = new DataInputStream(new
		 * ByteArrayInputStream(base91Decode, 16, 8)) .readLong(); System.out.println("TEST: " + md5
		 * + " : " + size);
		 */
		out.println("</ul>");
		out.flush();
	}

}