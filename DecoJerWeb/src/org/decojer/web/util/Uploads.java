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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.decojer.web.model.Upload;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

/**
 * Uploads.
 * 
 * @author André Pankraz
 */
public class Uploads {

	private static final BlobstoreService BLOBSTORE_SERVICE = BlobstoreServiceFactory
			.getBlobstoreService();

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private static Logger LOGGER = Logger.getLogger(Uploads.class.getName());

	public static void addUploadKey(final HttpServletRequest req,
			final Key uploadKey) {
		List<Key> uploads = getUploadKeys(req.getSession());
		if (uploads == null) {
			uploads = new ArrayList<Key>();
		} else {
			// to list end
			uploads.remove(uploadKey);
		}
		uploads.add(uploadKey);
		req.getSession().setAttribute("uploadKeys", uploads); // trigger update
	}

	public static String getChannelKey(final HttpSession httpSession) {
		return httpSession.getId();
	}

	public static String getChannelToken(final HttpSession httpSession) {
		String channelToken = (String) httpSession.getAttribute("channelToken");
		if (channelToken == null) {
			channelToken = ChannelServiceFactory.getChannelService()
					.createChannel(getChannelKey(httpSession));
			httpSession.setAttribute("channelToken", channelToken);
		}
		return channelToken;
	}

	public static List<Key> getUploadKeys(final HttpSession httpSession) {
		return (List<Key>) httpSession.getAttribute("uploadKeys");
	}

	public static String getUploadsHtml(final HttpServletRequest req,
			final HttpSession httpSession) {
		final List<Key> uploadKeys = getUploadKeys(httpSession);
		if (uploadKeys == null || uploadKeys.size() == 0) {
			return "";
		}
		final Map<Key, Entity> uploadKeys2Entities = DATASTORE_SERVICE
				.get(uploadKeys);
		boolean channel = false;
		final StringBuilder sb = new StringBuilder("<ul>");
		for (final Key uploadKey : uploadKeys) {
			final Upload upload = new Upload(uploadKeys2Entities.get(uploadKey));
			final String filename = upload.getFilename();

			if (upload.getSourceBlobKey() != null) {
				String sourcename = filename;
				if (filename.endsWith(".class")) {
					sourcename = filename.substring(0, filename.length() - 5)
							+ "java";
				} else if (filename.endsWith(".jar")) {
					sourcename = filename.substring(0, filename.length() - 4)
							+ "_source.jar";
				} else if (filename.endsWith(".dex")) {
					sourcename = filename.substring(0, filename.length() - 4)
							+ "_android_source.jar";
				}

				sb.append("<li><a href='/download/")
						.append(URLEncoder.encode(sourcename))
						.append("?u=")
						.append(URLEncoder.encode(upload.getSourceBlobKey()
								.getKeyString())).append("' target='_blank'>")
						.append(upload.getFilename()).append("</a>");
			} else if (upload.getError() != null) {
				sb.append("<li>").append(filename).append(" ERROR");
			} else {
				channel = true;
				sb.append("<li>").append(filename).append(" ...decompiling...");
			}
			if (upload.getTds() > 1) {
				sb.append(" (").append(upload.getTds()).append(" classes)");
			} else if (upload.getSourceBlobKey() != null) {
				sb.append(" (<a href='/?u=")
						.append(URLEncoder.encode(upload.getSourceBlobKey()
								.getKeyString())).append("'>View</a>)");
			}
			sb.append("</li>");
		}
		sb.append("</ul>");

		if (channel) {
			sb.append(
					"<script type='text/javascript' src='/_ah/channel/jsapi'></script>")
					.append("<script>")
					.append("  channel = new goog.appengine.Channel('")
					.append(getChannelToken(httpSession))
					.append("');")
					.append("  socket = channel.open();")
					.append("  socket.onmessage = function(msg) { window.location.reload(); };")
					.append("</script>");
		}

		final String u = req.getParameter("u");
		if (u == null) {
			return sb.toString();
		}
		try {
			sb.append("<hr /><pre class=\"brush: java\">")
					.append(IOUtils.toString(new BlobstoreInputStream(
							new BlobKey(u)), "UTF-8"))
					.append("</pre><script type=\"text/javascript\">SyntaxHighlighter.all()</script>");
		} catch (final IOException e) {
			LOGGER.log(Level.WARNING, "Couldn't write source!", e);
		}
		return sb.toString();
	}

}