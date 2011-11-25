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
package org.decojer.web.service;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.ResponseTooLargeException;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

/**
 * Wrapper for URL fetch service.
 * 
 * @author André Pankraz
 */
public class URLFetchService {

	private static final URLFetchService INSTANCE = new URLFetchService();

	private static Logger LOGGER = Logger.getLogger(URLFetchService.class.getName());

	/**
	 * Get instance.
	 * 
	 * @return instance
	 */
	public static URLFetchService getInstance() {
		return INSTANCE;
	}

	/**
	 * Fetch content from URL.
	 * 
	 * @param url
	 *            URL
	 * @param logNotFound
	 *            true - log not found
	 * @return content or null
	 */
	public byte[] fetchContent(final String url, final boolean logNotFound) {
		final HTTPResponse fetch;
		try {
			// default is 5 seconds, currently capped at 60 seconds
			fetch = URLFetchServiceFactory.getURLFetchService().fetch(
					new HTTPRequest(new URL(url), HTTPMethod.GET,
							com.google.appengine.api.urlfetch.FetchOptions.Builder
									.withDeadline(300)));
		} catch (final ResponseTooLargeException e) {
			LOGGER.info("Couldn't read URL '" + url + "'! Response too large.");
			return null;
		} catch (final Exception e) {
			LOGGER.log(Level.INFO, "Couldn't read URL '" + url + "'!", e);
			return null;
		}
		final int responseCode = fetch.getResponseCode();
		if (responseCode == HttpServletResponse.SC_NOT_FOUND) {
			if (logNotFound) {
				LOGGER.info("Couldn't read URL '" + url + "'! Not found (404).");
			}
			return null;
		}
		if (responseCode != HttpServletResponse.SC_OK) {
			LOGGER.info("Couldn't read URL '" + url + "'! Response code was '"
					+ fetch.getResponseCode() + "'.");
		}
		return fetch.getContent();
	}

}