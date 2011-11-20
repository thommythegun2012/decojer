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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.decojer.web.model.Pom;
import org.decojer.web.util.DB;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

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

	private static final String URL_CENTRAL_FILE = "http://search.maven.org/remotecontent?filepath=";

	private static final String URL_CENTRAL_RSS = "http://search.maven.org/remotecontent?filepath=rss.xml";

	private static final String URL_REPO1_FILE = "http://repo1.maven.org/maven2/";

	private static final String URL_REPO2_FILE = "http://repo2.maven.org/maven2/";

	// same mirror: http://uk.maven.org/maven2/
	// http://download.java.net/maven/2/
	// http://www.jarvana.com/jarvana/browse

	/**
	 * Get instance.
	 * 
	 * @return instance
	 */
	public static MavenService getInstance() {
		return INSTANCE;
	}

	/**
	 * Fetch Maven central RSS, deliver list with POM Ids (groupId:artifactId:version).
	 * 
	 * @return list with POM Ids (groupId:artifactId:version)
	 */
	public List<String> fetchCentralRss() {
		final byte[] content;
		try {
			content = URLFetchService.getInstance().fetchContent(URL_CENTRAL_RSS);
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't read Maven Central RSS!", e);
		}
		if (content == null) {
			throw new RuntimeException("Couldn't read Maven Central RSS! 404 for: "
					+ URL_CENTRAL_RSS);
		}
		final ArrayList<String> pomIds = new ArrayList<String>();
		try {
			final SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
			parser.parse(new ByteArrayInputStream(content), new DefaultHandler() {

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
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't parse Maven Central RSS!", e);
		}
		if (pomIds.size() == 0) {
			throw new RuntimeException("Couldn't parse Maven Central RSS! Is empty.");
		}
		return pomIds;
	}

	/**
	 * Fetch file content from repository.
	 * 
	 * @param groupId
	 *            group id
	 * @param artifactId
	 *            artifact id
	 * @param version
	 *            version
	 * @param fileName
	 *            file name
	 * @return file content
	 */
	public byte[] fetchFile(final String groupId, final String artifactId, final String version,
			final String fileName) {
		final String url = URL_REPO1_FILE + groupId.replace('.', '/') + '/' + artifactId + '/'
				+ version + '/' + fileName;
		try {
			return URLFetchService.getInstance().fetchContent(url);
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't read Maven Central file!", e);
		}
	}

	/**
	 * Fetch available POM versions.
	 * 
	 * @param groupId
	 *            group id
	 * @param artifactId
	 *            artifact id
	 * @return versions
	 */
	public List<String> fetchVersions(final String groupId, final String artifactId) {
		final String url = URL_REPO1_FILE + groupId.replace('.', '/') + '/' + artifactId
				+ "/maven-metadata.xml";
		final byte[] content;
		try {
			content = URLFetchService.getInstance().fetchContent(url);
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't read Maven Metadata!", e);
		}
		final ArrayList<String> versions = new ArrayList<String>();
		if (content == null) {
			LOGGER.info("Couldn't read Maven Metadata! 404 for: " + url);
			return versions;
		}
		try {
			final SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
			parser.parse(new ByteArrayInputStream(content), new DefaultHandler() {

				boolean version = false;

				@Override
				public void characters(final char[] ch, final int start, final int length)
						throws SAXException {
					if (!this.version) {
						return;
					}
					final String version = new String(ch, start, length);
					versions.add(version);
				}

				@Override
				public void endElement(final String uri, final String localName, final String qName)
						throws SAXException {
					if ("version".equals(qName)) {
						this.version = false;
						return;
					}
				}

				@Override
				public void startElement(final String uri, final String localName,
						final String qName, final Attributes attributes) throws SAXException {
					if ("version".equals(qName)) {
						this.version = true;
						return;
					}
				}

			});
		} catch (final Exception e) {
			throw new RuntimeException("Couldn't parse Maven Metadata!", e);
		}
		if (versions.size() == 0) {
			throw new RuntimeException("Couldn't parse Maven Metadata! Is empty.");
		}
		return versions;
	}

	/**
	 * Find all POM entities for group id and artifact id.
	 * 
	 * @param groupId
	 *            group id
	 * @param artifactId
	 *            artifact id
	 * @return POM entities
	 */
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

	/**
	 * Import all versions for POM.
	 * 
	 * @return imported POMs
	 */
	public int importAllVersions() {
		final HashSet<String> done = new HashSet<String>();
		final int[] nr = new int[1];

		DB.iterate(Pom.KIND, new DB.Processor() {

			@Override
			public boolean process(final Entity entity) {
				final Pom pom = new Pom(entity);
				final String groupId = pom.getGroupId();
				final String artifactId = pom.getArtifactId();

				final String doneId = groupId + ':' + artifactId;
				if (done.contains(doneId)) {
					return true;
				}
				done.add(doneId);
				final List<String> versions = fetchVersions(groupId, artifactId);
				for (final String version : versions) {
					if (importPom(groupId, artifactId, version) != null) {
						if (++nr[0] >= 1000) {
							return false; // import max. 1000
						}
					}
				}
				return true;
			}

		});
		return nr[0];
	}

	/**
	 * Fetch and import Central RSS.
	 * 
	 * @return imported files
	 */
	public int importCentralRss() {
		final List<String> pomIds = fetchCentralRss();
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
			final String thisVersion = pomId.substring(versionPos + 1);

			final List<String> versions = fetchVersions(groupId, artifactId);
			if (!versions.contains(thisVersion)) {
				versions.add(thisVersion);
			}
			for (final String version : versions) {
				if (importPom(groupId, artifactId, version) != null) {
					++nr;
				}
			}
		}
		LOGGER.info("Imported " + nr + " POMs and JARs.");
		return nr;
	}

	/**
	 * Fetch and import POM.
	 * 
	 * @param groupId
	 *            group id
	 * @param artifactId
	 *            artifact id
	 * @param version
	 *            version
	 * @return imported POM or null
	 */
	public Pom importPom(final String groupId, final String artifactId, final String version) {
		final String pomId = groupId + ':' + artifactId + ':' + version;
		final Key pomKey = KeyFactory.createKey(Pom.KIND, pomId);
		try {
			DATASTORE_SERVICE.get(pomKey);
			return null; // is known, happy for now
		} catch (final EntityNotFoundException e) {
			// fall through
		}
		BlobKey blobKey;
		try {
			final String fileName = artifactId + '-' + version + ".jar";
			final byte[] jarContent = fetchFile(groupId, artifactId, version, fileName);
			if (jarContent == null) {
				return null; // don't import JAR-less POMs for now
			}
			final Entity blobInfo = BlobService.getInstance().findBlobInfo(jarContent);
			if (blobInfo == null) {
				blobKey = BlobService.getInstance().createBlob("application/java-archive",
						fileName, jarContent);
			} else {
				blobKey = new BlobKey(blobInfo.getKey().getName());
			}
		} catch (final Exception e1) {
			LOGGER.log(Level.WARNING, "Couldn't import POM JAR '" + pomId + "'!", e1);
			return null;
		}
		try {
			final byte[] pomContent = fetchFile(groupId, artifactId, version, artifactId + '-'
					+ version + ".pom");
			final Pom pom = new Pom(new Entity(pomKey));
			pom.setContent(pomContent);
			pom.setJar(blobKey);
			DATASTORE_SERVICE.put(pom.getWrappedEntity());
			return pom;
		} catch (final Exception e1) {
			LOGGER.log(Level.WARNING, "Couldn't import POM '" + pomId + "'!", e1);
			return null;
		}
	}

}