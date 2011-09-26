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

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;
import org.decojer.web.analyser.UploadInfo;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import com.google.appengine.api.blobstore.BlobstoreInputStream;

/**
 * Uploads.
 * 
 * @author André Pankraz
 */
public class Uploads {

	private static Logger LOGGER = Logger.getLogger(Uploads.class.getName());

	public static void addUpload(final HttpServletRequest req,
			final UploadInfo uploadInfo) {
		List<UploadInfo> uploads = getUploads(req.getSession());
		if (uploads == null) {
			uploads = new ArrayList<UploadInfo>();
		} else {
			// to list end
			uploads.remove(uploadInfo);
		}
		uploads.add(uploadInfo);
		req.getSession().setAttribute("blobKeys", uploads); // trigger update
	}

	public static List<UploadInfo> getUploads(final HttpSession httpSession) {
		return (List<UploadInfo>) httpSession.getAttribute("blobKeys");
	}

	public static String getUploadsHtml(final HttpServletRequest req,
			final HttpSession httpSession) {
		final List<UploadInfo> uploads = getUploads(httpSession);
		if (uploads == null || uploads.size() == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder("<ul>");
		for (int i = 0; i < uploads.size(); ++i) {
			final UploadInfo blobInfo = uploads.get(i);
			sb.append("<li><a href='/decompile?u=").append(i)
					.append("' target='_blank'>")
					.append(blobInfo.getFilename()).append("</a>");
			if (blobInfo.getTypes() > 1) {
				sb.append(" (").append(blobInfo.getTypes()).append(" classes)");
			} else {
				sb.append(" (<a href='/?u=").append(i).append("'>View</a>)");
			}
			sb.append("</li>");
		}
		sb.append("</ul>");
		int u;
		try {
			u = Integer.parseInt(req.getParameter("u"));
			if (u >= uploads.size()) {
				return sb.toString();
			}
		} catch (final NumberFormatException e) {
			u = uploads.size() - 1;
		}
		final UploadInfo upload = uploads.get(u);
		if (!upload.getFilename().endsWith(".class")) {
			return sb.toString();
		}
		try {
			final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
					upload.getBlobKey());
			final DU du = DecoJer.createDu();
			final TD td = du.read(upload.getFilename(),
					new BufferedInputStream(blobstoreInputStream), null);
			final CU cu = DecoJer.createCu(td);
			final String source = DecoJer.decompile(cu);
			sb.append("<hr /><pre class=\"brush: java\">")
					.append(source.replace("<", "&lt;"))
					.append("</pre><script type=\"text/javascript\">SyntaxHighlighter.all()</script>");
		} catch (final Throwable e) {
			LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
			final CompilerOptions compilerOptions = new CompilerOptions();
			LOGGER.log(Level.WARNING, "  ### " + compilerOptions);
			LOGGER.log(Level.WARNING, "  ### " + compilerOptions.getClass());
			LOGGER.log(Level.WARNING, "  ### "
					+ compilerOptions.ignoreMethodBodies);
		}
		return sb.toString();
	}

}