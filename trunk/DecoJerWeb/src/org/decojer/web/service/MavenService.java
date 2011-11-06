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

import org.decojer.web.model.Pom;
import org.decojer.web.util.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

/**
 * Maven Service.
 * 
 * @author André Pankraz
 */
public class MavenService {

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private static final MavenService INSTANCE = new MavenService();

	private static Logger LOGGER = Logger.getLogger(MavenService.class.getName());

	private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

	private static final String URL_MAVEN_CENTRAL_FILE = "http://search.maven.org/remotecontent?filepath=";

	private static final URL URL_MAVEN_CENTRAL_RSS;

	private static final URLFetchService urlFetchService = URLFetchServiceFactory
			.getURLFetchService();

	static {
		try {
			URL_MAVEN_CENTRAL_RSS = new URL(
					"http://search.maven.org/remotecontent?filepath=rss.xml");
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static MavenService getInstance() {
		return INSTANCE;
	}

	public List<Pom> findPoms(final String groupId, final String artifactId) {
		final Query pomVersionQuery = new Query(Pom.KIND);

		final Key pomKey = KeyFactory.createKey(Pom.KIND, groupId + ":" + artifactId + ":");
		final Key pomKey2 = KeyFactory.createKey(Pom.KIND, groupId + ":" + artifactId + ":\uFFFD");

		pomVersionQuery.addFilter(Entity.KEY_RESERVED_PROPERTY,
				Query.FilterOperator.GREATER_THAN_OR_EQUAL, pomKey);
		pomVersionQuery.addFilter(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.LESS_THAN,
				pomKey2);

		final List<Entity> entities = DATASTORE_SERVICE.prepare(pomVersionQuery).asList(
				FetchOptions.Builder.withDefaults());
		final ArrayList<Pom> poms = new ArrayList<Pom>(entities.size());
		for (final Entity entity : entities) {
			poms.add(new Pom(entity));
		}
		return poms;
	}

	public int importMavenCentralRss() {
		final List<String> pomIds = readMavenCentralRss();
		if (pomIds == null) {
			return 0;
		}
		int nr = 0;
		for (final String pomId : pomIds) {
			final int artifactIdPos = pomId.indexOf(':');
			if (artifactIdPos == -1) {
				LOGGER.log(Level.WARNING, "Couldn't parse '" + pomId + "' from Maven Central RSS!");
				continue;
			}
			final String groupId = pomId.substring(0, artifactIdPos);
			final int versionPos = pomId.indexOf(':', artifactIdPos + 1);
			if (versionPos == -1) {
				LOGGER.log(Level.WARNING, "Couldn't parse '" + pomId + "' from Maven Central RSS!");
				continue;
			}
			final String artifactId = pomId.substring(artifactIdPos + 1, versionPos);
			final String version = pomId.substring(versionPos + 1);

			final Key pomKey = KeyFactory.createKey(Pom.KIND, pomId);
			try {
				DATASTORE_SERVICE.get(pomKey);
				continue; // is known, happy for now
			} catch (final EntityNotFoundException e) {
				// fall through
			}
			byte[] md5Hash;
			long size;
			try {
				final String fileName = artifactId + '-' + version + ".jar";
				final byte[] jarContent = readMavenCentralFile(groupId, artifactId, version,
						fileName);
				md5Hash = IOUtils.md5(jarContent);
				size = jarContent.length;

				final List<Entity> blobInfoEntities = BlobService.getInstance()
						.findBlobInfoEntities(IOUtils.hexEncode(md5Hash), size);
				if (blobInfoEntities.isEmpty()) {
					BlobService.getInstance().createBlob("application/java-archive", fileName,
							jarContent);
				}
			} catch (final Exception e1) {
				LOGGER.log(Level.WARNING, "Couldn't import POM JAR '" + pomId + "'!", e1);
				continue;
			}
			try {
				final byte[] pomContent = readMavenCentralFile(groupId, artifactId, version,
						artifactId + '-' + version + ".pom");
				final Pom pom = new Pom(new Entity(pomKey));
				pom.setContent(pomContent);
				pom.setJar(IOUtils.toKey(md5Hash, size));
				DATASTORE_SERVICE.put(pom.getWrappedEntity());
				++nr;
			} catch (final Exception e1) {
				LOGGER.log(Level.WARNING, "Couldn't import POM '" + pomId + "'!", e1);
				continue;
			}
		}
		return nr;
	}

	public String read() {
		final List<String> artifacts = readMavenCentralRss();
		final StringBuilder sb = new StringBuilder();
		for (final String artifact : artifacts) {
			sb.append("<br>").append(artifact).append("</br>");
		}
		return sb.toString();
	}

	public byte[] readMavenCentralFile(final String groupId, final String artifactId,
			final String version, final String fileName) {
		final String url = URL_MAVEN_CENTRAL_FILE + groupId.replace('.', '/') + '/' + artifactId
				+ '/' + version + '/' + fileName;
		try {
			final HTTPResponse fetch = urlFetchService.fetch(new HTTPRequest(new URL(url),
					HTTPMethod.GET, com.google.appengine.api.urlfetch.FetchOptions.Builder
							.withDeadline(300))); // currently capped at 60 seconds
			if (fetch.getResponseCode() != HttpServletResponse.SC_OK) {
				throw new RuntimeException("Couldn't read Maven Central file '" + url
						+ "'! Response code was '" + fetch.getResponseCode() + "'.");
			}
			return fetch.getContent();
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't read Maven Central file '" + url + "'!", e);
		}
	}

	public List<String> readMavenCentralRss() {
		try {
			final HTTPResponse fetch = urlFetchService.fetch(new HTTPRequest(URL_MAVEN_CENTRAL_RSS,
					HTTPMethod.GET, com.google.appengine.api.urlfetch.FetchOptions.Builder
							.withDeadline(20)));
			if (fetch.getResponseCode() != HttpServletResponse.SC_OK) {
				throw new RuntimeException("Couldn't read Maven Central RSS! Response code was '"
						+ fetch.getResponseCode() + "'.");
			}
			final ArrayList<String> pomIds = new ArrayList<String>();
			final SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
			parser.parse(new ByteArrayInputStream(fetch.getContent()), new DefaultHandler() {

				boolean title = false;

				@Override
				public void characters(final char[] ch, final int start, final int length)
						throws SAXException {
					if (!this.title) {
						return;
					}
					final String pomId = new String(ch, start, length);
					if (pomIds.isEmpty() && "Maven Central Update".equals(pomId)) {
						return;
					}
					// TODO regexp pattern check groupId:artifactId:version
					pomIds.add(pomId);
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
			if (pomIds.size() == 0) {
				throw new RuntimeException("Couldn't read Maven Central RSS! Is empty.");
			}
			return pomIds;
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't read Maven Central RSS!", e);
		}
	}

}