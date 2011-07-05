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
package org.decojer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Package class stream provider.
 * 
 * Names consist of dollar-separated type names.
 * 
 * @author André Pankraz
 */
public class PackageClassStreamProvider {

	protected final static Logger LOGGER = Logger
			.getLogger(PackageClassStreamProvider.class.getName());

	protected final Map<String, DataInputStream> name2classStream = new HashMap<String, DataInputStream>();

	/**
	 * Constructor.
	 * 
	 * TODO later use a direct folder iterator
	 * 
	 * @param path
	 *            package path
	 */
	public PackageClassStreamProvider(final String path) {
		if (path == null) {
			return;
		}

		// create package Map<type name, ClassFile>
		final int pos = path.indexOf('!');
		if (pos != -1) {
			// is from JAR...
			final JarFile jarFile;
			try {
				jarFile = new JarFile(path.substring(0, pos));
			} catch (final IOException e) {
				throw new DecoJerException("Couldn't get jar file from path '"
						+ path + "'!", e);
			}
			String prefix = path.substring(pos + 1).replace(
					File.pathSeparatorChar, '/');
			if (prefix.charAt(0) == '/') {
				prefix = prefix.substring(1);
			}
			final int lastPos = prefix.lastIndexOf('/');
			if (lastPos != -1) {
				prefix = prefix.substring(0, lastPos + 1);
			}
			final int prefixLength = prefix.length();
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (!name.startsWith(prefix)
						|| name.indexOf('/', prefixLength) != -1
						|| !name.endsWith(".class")) {
					continue;
				}
				name = name.substring(prefixLength, name.length() - 6);
				try {
					addClassStream(name, jarFile.getInputStream(entry));
				} catch (final IOException e) {
					LOGGER.log(Level.WARNING, "Couldn't get entry '" + name
							+ "' from jar file '" + jarFile.getName() + "'!", e);
				}
			}
		} else {
			File file = new File(path);
			if (!file.isDirectory()) {
				file = file.getParentFile();
			}
			for (final File entry : file.listFiles()) {
				String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				name = name.substring(0, name.length() - 6);
				try {
					addClassStream(name, new FileInputStream(entry));
				} catch (final FileNotFoundException e) {
					LOGGER.log(Level.WARNING, "Couldn't get entry '" + name
							+ "' from package directory '" + file.getName()
							+ "'!", e);
				}
			}
		}
	}

	/**
	 * Add input stream. The name must not be the type name, e.g. usage for
	 * unknown bytestreams from uploads.
	 * 
	 * @param name
	 *            type name (prefered) or other handle
	 * @param inputStream
	 *            data input stream or input stream
	 * @return data input stream
	 */
	public DataInputStream addClassStream(final String name,
			final InputStream inputStream) {
		assert name != null;

		final DataInputStream dataInputStream = inputStream instanceof DataInputStream ? (DataInputStream) inputStream
				: new DataInputStream(inputStream);
		this.name2classStream.put(name, dataInputStream);
		return dataInputStream;
	}

	/**
	 * Get class stream for full type name.
	 * 
	 * @param name
	 *            full type name
	 * @return class file
	 */
	public DataInputStream getClassStream(final String name) {
		return this.name2classStream.get(name);
	}

	/**
	 * Get type name to class stream mapping.
	 * 
	 * TODO later use a direct folder iterator
	 * 
	 * @return type name to class stream mapping
	 */
	public Map<String, DataInputStream> getClassStreams() {
		return this.name2classStream;
	}

}