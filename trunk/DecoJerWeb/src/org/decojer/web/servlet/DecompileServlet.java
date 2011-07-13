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
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.decojer.DecoJer;
import org.decojer.PackageClassStreamProvider;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.PF;
import org.decojer.cavaj.model.TD;
import org.decojer.web.analyser.BlobInfo;
import org.decojer.web.util.IOUtils;
import org.decojer.web.util.Messages;
import org.decojer.web.util.Uploads;

import com.google.appengine.api.blobstore.BlobstoreInputStream;

public class DecompileServlet extends HttpServlet {

	private static Logger LOGGER = Logger.getLogger(DecompileServlet.class
			.getName());

	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {
		final BlobInfo upload;
		try {
			final List<BlobInfo> uploads = Uploads.getUploads(req.getSession());
			upload = uploads.get(Integer.parseInt(req.getParameter("u")));
		} catch (final Exception e) {
			Messages.addMessage(req, "Please Ma - Don't hack me!");
			res.sendRedirect("/");
			return;
		}
		final String filename = upload.getFilename();
		final int pos = filename.lastIndexOf('.');
		final String sourcename = filename.substring(0, pos) + ".java";
		// application/java-archive
		res.setContentType("text/x-java-source");
		res.setContentLength(-1);
		res.setHeader("Content-Disposition", "attachment; filename=\""
				+ sourcename + "\"");
		final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
				upload.getBlobKey());
		final byte[] bytes = IOUtils.toBytes(blobstoreInputStream);
		try {
			final PackageClassStreamProvider packageClassStreamProvider = new PackageClassStreamProvider(
					null);
			packageClassStreamProvider.addClassStream("TEST",
					new ByteArrayInputStream(bytes));
			final PF pf = DecoJer.createPF(packageClassStreamProvider);
			final Entry<String, TD> next = pf.getTds().entrySet().iterator()
					.next();
			final CU cu = DecoJer.createCU(next.getValue());
			final String source = DecoJer.decompile(cu);
			res.getWriter().write(source);
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
			Messages.addMessage(req, "Decompilation problems!");
			res.sendRedirect("/");
			return;
		}
	}
}