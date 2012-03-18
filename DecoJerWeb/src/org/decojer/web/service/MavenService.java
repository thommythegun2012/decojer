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

import lombok.Getter;

import org.decojer.web.model.POM;
import org.decojer.web.util.DB;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;

/**
 * Maven Service.
 * 
 * @author André Pankraz
 */
public class MavenService {

	@Getter
	public class ImportResults {

		private int checked;

		private int imported;

		private int known;

	}

	@Getter
	public class Stats {

		private int number;

		private long size;

	}

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
	 * Calculate POM statistics. Delete POMs without JAR.
	 * 
	 * @return POM statistics
	 */
	public Stats calculateStats() {
		final Stats stats = new Stats();
		final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		final BlobInfoFactory blobInfoFactory = new BlobInfoFactory(datastoreService);
		DB.iterate(POM.KIND, new DB.Processor() {

			@Override
			public boolean process(final Entity entity) {
				final POM pom = new POM(entity);
				final BlobKey jar = pom.getJar();
				final BlobInfo blobInfo = jar == null ? null : blobInfoFactory.loadBlobInfo(jar);
				if (blobInfo == null) {
					LOGGER.info("No JAR for POM '" + pom.getId() + "'! Delete.");
					datastoreService.delete(entity.getKey());
					return true;
				}
				stats.size += blobInfo.getSize();
				++stats.number;
				return true;
			}

		});
		return stats;
	}

	/**
	 * Fetch Maven central RSS, deliver list with POM Ids (groupId:artifactId:version).
	 * 
	 * @return list with POM Ids (groupId:artifactId:version), not null
	 */
	public List<String> fetchCentralRss() {
		final byte[] content = URLFetchService.getInstance().fetchContent(URL_CENTRAL_RSS, true);
		final ArrayList<String> pomIds = new ArrayList<String>();
		if (content == null) {
			LOGGER.warning("Couldn't read Maven Central RSS!");
			return pomIds;
		}
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
			LOGGER.log(Level.WARNING, "Couldn't parse Maven Central RSS!", e);
			return pomIds;
		}
		if (pomIds.size() == 0) {
			LOGGER.warning("Couldn't parse Maven Central RSS! Is empty.");
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
	 * @return file content or null
	 */
	public byte[] fetchFile(final String groupId, final String artifactId, final String version,
			final String fileName) {
		final String url = URL_REPO1_FILE + groupId.replace('.', '/') + '/' + artifactId + '/'
				+ version + '/' + fileName;
		return URLFetchService.getInstance().fetchContent(url, false);
	}

	/**
	 * Fetch available POM versions.
	 * 
	 * @param groupId
	 *            group id
	 * @param artifactId
	 *            artifact id
	 * @return versions versions, not null
	 */
	public List<String> fetchVersions(final String groupId, final String artifactId) {
		final String url = URL_REPO1_FILE + groupId.replace('.', '/') + '/' + artifactId
				+ "/maven-metadata.xml";
		final byte[] content = URLFetchService.getInstance().fetchContent(url, true);
		final ArrayList<String> versions = new ArrayList<String>();
		if (content == null) {
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
			LOGGER.log(Level.INFO, "Couldn't read Maven Metadata!", e);
			return versions;
		}
		if (versions.size() == 0) {
			LOGGER.info("Couldn't read Maven Metadata! Is empty");
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
	public List<POM> findPOMs(final String groupId, final String artifactId) {
		final Query pomVersionQuery = new Query(POM.KIND);

		final Key pomKey = KeyFactory.createKey(POM.KIND, groupId + ":" + artifactId + ":");
		final Key pomKey2 = KeyFactory.createKey(POM.KIND, groupId + ":" + artifactId + ":\uFFFD");

		pomVersionQuery.addFilter(Entity.KEY_RESERVED_PROPERTY,
				Query.FilterOperator.GREATER_THAN_OR_EQUAL, pomKey);
		pomVersionQuery.addFilter(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.LESS_THAN,
				pomKey2);

		final List<Entity> entities = DatastoreServiceFactory.getDatastoreService()
				.prepare(pomVersionQuery).asList(FetchOptions.Builder.withDefaults());
		final ArrayList<POM> poms = new ArrayList<POM>(entities.size());
		for (final Entity entity : entities) {
			poms.add(new POM(entity));
		}
		return poms;
	}

	/**
	 * Import all versions for POM.
	 * 
	 * @return imported POMs
	 */
	public ImportResults importCentralAll() {
		final HashSet<String> done = new HashSet<String>();
		final ImportResults importResults = new ImportResults();

		DB.iterate(POM.KIND, new DB.Processor() {

			@Override
			public boolean process(final Entity entity) {
				final POM pom = new POM(entity);
				final String groupId = pom.getGroupId();
				final String artifactId = pom.getArtifactId();

				final String doneId = groupId + ':' + artifactId;
				if (done.contains(doneId)) {
					return true;
				}
				done.add(doneId);
				final List<String> versions = fetchVersions(groupId, artifactId);
				importResults.checked += versions.size();
				for (final POM knownPOM : findPOMs(groupId, artifactId)) {
					versions.remove(knownPOM.getVersion());
					++importResults.known;
				}
				for (final String version : versions) {
					if (importPOM(groupId, artifactId, version) != null) {
						if (++importResults.imported >= 500) {
							return false; // import max. 500
						}
					}
				}
				return true;
			}

		});
		final String msg = "Checked " + importResults.checked + " entries, " + importResults.known
				+ " known entries.\nImported " + importResults.imported + " POMs and JARs.";
		LOGGER.info(msg);
		return importResults;
	}

	/**
	 * Fetch and import Central RSS.
	 * 
	 * @return imported files
	 */
	public ImportResults importCentralRss() {
		final ImportResults importResults = new ImportResults();
		final List<String> centralRss = fetchCentralRss();
		importResults.checked = centralRss.size();
		for (final String pomId : centralRss) {
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
			} else {
				++importResults.known;
			}
			for (final POM knownPOM : findPOMs(groupId, artifactId)) {
				versions.remove(knownPOM.getVersion());
			}
			for (final String version : versions) {
				if (importPOM(groupId, artifactId, version) != null) {
					++importResults.imported;
				}
			}
		}
		final String msg = "Central RSS with " + importResults.checked + " entries, "
				+ importResults.known + " known entries.\nImported " + importResults.imported
				+ " POMs and JARs.";
		LOGGER.info(msg);
		try {
			// sendToAdmin with or without "to" doesn't work for me in 1.5.4
			MailServiceFactory.getMailService().send(
					new MailService.Message("andrePankraz@decojer.org", "andrePankraz@gmail.com",
							"DecoJer Maven Import", msg));
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Could not send email!", e);
		}
		return importResults;
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
	public POM importPOM(final String groupId, final String artifactId, final String version) {
		final String pomId = groupId + ':' + artifactId + ':' + version;
		final Key pomKey = KeyFactory.createKey(POM.KIND, pomId);
		final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		try {
			datastoreService.get(pomKey);
			return null; // is known, happy for now
		} catch (final EntityNotFoundException e) {
			// fall through
		}
		try {
			final byte[] pomContent = fetchFile(groupId, artifactId, version, artifactId + '-'
					+ version + ".pom");
			if (pomContent == null) {
				// even if JAR exists...cannot do anything with that
				// (repository error, e.g. org.ujoframework:ujo-orm:0.92)
				return null;
			}
			final String fileName = artifactId + '-' + version + ".jar";
			final byte[] jarContent = fetchFile(groupId, artifactId, version, fileName);
			if (jarContent == null) {
				return null; // don't import JAR-less POMs for now
			}
			if (jarContent.length > 30000000) {
				LOGGER.info("Will ignore POM JAR '" + pomId + "'. This is with "
						+ jarContent.length + " bytes too large to be useful (max 30 MB).");
				return null;
			}
			final Entity blobInfo = BlobService.getInstance().findBlobInfo(jarContent);
			BlobKey blobKey;
			if (blobInfo == null) {
				blobKey = BlobService.getInstance().createBlob("application/java-archive",
						fileName, jarContent);
			} else {
				blobKey = new BlobKey(blobInfo.getKey().getName());
			}
			final POM pom = new POM(new Entity(pomKey));
			pom.setContent(pomContent);
			pom.setJar(blobKey);
			datastoreService.put(pom.getWrappedEntity());
			return pom;
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Couldn't import POM JAR '" + pomId + "'!", e);
			return null;
		}
	}

}