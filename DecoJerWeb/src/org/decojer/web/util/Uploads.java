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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.decojer.web.analyser.BlobInfo;

/**
 * @author André Pankraz
 */
public class Uploads {

	public static void addUpload(final HttpServletRequest req,
			final BlobInfo blobInfo) {
		List<BlobInfo> uploads = getUploads(req.getSession());
		if (uploads == null) {
			uploads = new ArrayList<BlobInfo>();
		} else {
			// to list end
			uploads.remove(blobInfo);
		}
		uploads.add(blobInfo);
		req.getSession().setAttribute("blobKeys", uploads); // trigger update
	}

	public static List<BlobInfo> getUploads(final HttpSession httpSession) {
		return (List<BlobInfo>) httpSession.getAttribute("blobKeys");
	}

	public static String getUploadsHtml(final HttpSession httpSession) {
		final List<BlobInfo> uploads = getUploads(httpSession);
		if (uploads == null || uploads.size() == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder("<ul>");
		for (int i = 0; i < uploads.size(); ++i) {
			final BlobInfo blobInfo = uploads.get(i);
			sb.append("<li><a href='/decompile?u=").append(i)
					.append("' target='_blank'>")
					.append(blobInfo.getFilename()).append("</a>");
			if (blobInfo.getTypes() > 1) {
				sb.append(" (").append(blobInfo.getTypes()).append(" classes)");
			}
			sb.append("</li>");
		}
		httpSession.removeAttribute("messages");
		sb.append("</ul>");
		return sb.toString();
	}
}