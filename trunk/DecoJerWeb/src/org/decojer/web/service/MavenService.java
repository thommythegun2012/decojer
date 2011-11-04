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

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

/**
 * Maven RSS.
 * 
 * @author André Pankraz
 */
public class MavenService {

	private static Logger LOGGER = Logger.getLogger(MavenService.class.getName());

	private static final URL MAVEN_CENTRAL;

	private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

	private static final URLFetchService urlFetchService = URLFetchServiceFactory
			.getURLFetchService();

	static {
		try {
			MAVEN_CENTRAL = new URL("http://search.maven.org/remotecontent?filepath=rss.xml");
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static String read() {
		final List<String> artefacts = readMavenCentralRss();
		final StringBuilder sb = new StringBuilder();
		for (final String artefact : artefacts) {
			sb.append("<br>").append(artefact).append("</br>");
		}
		return sb.toString();
	}

	public static List<String> readMavenCentralRss() {
		try {
			final HTTPResponse fetch = urlFetchService.fetch(MAVEN_CENTRAL);
			if (fetch.getResponseCode() != HttpServletResponse.SC_OK) {
				return null;
			}
			final ArrayList<String> artefacts = new ArrayList<String>();
			final SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
			parser.parse(new ByteArrayInputStream(fetch.getContent()), new DefaultHandler() {

				boolean title = false;

				@Override
				public void characters(final char[] ch, final int start, final int length)
						throws SAXException {
					if (!this.title) {
						return;
					}
					artefacts.add(new String(ch, start, length));
				}

				@Override
				public void endElement(final String uri, final String localName, final String qName)
						throws SAXException {
					if ("title".equals(qName)) {
						this.title = false;
						return;
					}
				}

				@Override
				public void startElement(final String uri, final String localName,
						final String qName, final Attributes attributes) throws SAXException {
					if ("title".equals(qName)) {
						this.title = true;
						return;
					}
				}

			});
			if (artefacts.size() == 0) {
				LOGGER.log(Level.WARNING, "Couldn't read Maven Central RSS!");
				return null;
			}
			final String artefact = artefacts.get(0);
			if ("Maven Central Update".equals(artefact)) {
				artefacts.remove(0);
			}
			return artefacts;
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Couldn't read Maven Central RSS!", e);
			return null;
		}
	}

}