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
package org.decojer.cavaj.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.decojer.DecoJerException;
import org.decojer.cavaj.reader.JavassistReader;
import org.decojer.cavaj.reader.SmaliReader;

/**
 * Decompilation unit.
 * 
 * @author André Pankraz
 */
public class DU {

	private final static Logger LOGGER = Logger.getLogger(DU.class.getName());

	private final Map<String, TD> tds = new HashMap<String, TD>();

	private final Map<String, T> ts = new HashMap<String, T>();

	/**
	 * Constructor.
	 * 
	 */
	public DU() {
		this.ts.put(void.class.getName(), new T(this, void.class.getName()));
	}

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void addTd(final TD td) {
		assert td != null;

		this.tds.put(td.getT().getName(), td);
	}

	/**
	 * Get type declaration.
	 * 
	 * @param desc
	 *            descriptor
	 * @return type declaration
	 */
	public T getDescT(final String desc) {
		assert desc != null;

		switch (desc.charAt(0)) {
		case '[':
			int i = 0;
			while (desc.charAt(++i) == '[') {
				;
			}
			final T descT = getDescT(desc.substring(i));
			final StringBuilder sb = new StringBuilder(descT.getName());
			while (i-- > 0) {
				sb.append("[]");
			}
			return getT(sb.toString());
		case 'B':
			return getT(byte.class.getName());
		case 'V':
			return getT(void.class.getName());
		case 'L':
			return getT(desc.substring(1, desc.length() - 1).replace('/', '.'));
		}
		LOGGER.warning("Unknown desc: " + desc);
		return getT(desc);
	}

	/**
	 * Get type declaration.
	 * 
	 * @param name
	 *            name
	 * @return type declaration
	 */
	public T getT(final String name) {
		assert name != null;

		T t = this.ts.get(name);
		if (t == null) {
			t = new T(this, name);
		}
		return t;
	}

	/**
	 * Get type declaration.
	 * 
	 * @param name
	 *            name
	 * @return type declaration
	 */
	public TD getTd(final String name) {
		return this.tds.get(name);
	}

	public TD read(final File file, final String selector) throws IOException {
		final String fileName = file.getName();
		if (fileName.endsWith(".class")) {
			// try reading whole package first
			for (final File entry : file.getParentFile().listFiles()) {
				final String name = entry.getName();
				if (!name.endsWith(".class") || name.equals(fileName)) {
					continue;
				}
				try {
					read(name, new FileInputStream(entry), null);
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Couldn't read '" + name + "'!",
							e);
				}
			}
		}
		return read(fileName, new FileInputStream(file), selector);
	}

	public TD read(final String fileName) throws IOException {
		final int pos = fileName.indexOf('!');
		if (pos == -1) {
			return read(new File(fileName), null);
		}
		// ...\jdk1.6.0_26\jre\lib\rt.jar!/com/sun/xml/internal/fastinfoset/Decoder.class
		return read(new File(fileName.substring(0, pos)),
				fileName.substring(pos + 1));
	}

	public TD read(final String fileName, final InputStream is,
			final String selector) throws IOException {
		if (fileName.endsWith(".class")) {
			return JavassistReader.read(is, this);
		} else if (fileName.endsWith(".dex")) {
			SmaliReader.read(is, this);
		} else if (fileName.endsWith(".jar")) {
			String selectorPrefix = null;
			String selectorFileName = null;
			if (selector != null && selector.endsWith(".class")) {
				selectorFileName = selector.charAt(0) == '/' ? selector
						.substring(1) : selector;
				final int pos = selectorFileName.lastIndexOf('/');
				if (pos != -1) {
					selectorPrefix = selectorFileName.substring(0, pos + 1);
				}
			}
			TD td = null;

			final ZipInputStream zip = new ZipInputStream(is);
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				final String name = zipEntry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				if (selectorPrefix != null
						&& (!name.startsWith(selectorPrefix) || name.indexOf(
								'/', selectorPrefix.length()) != -1)) {
					continue;
				}
				try {
					if (name.equals(selectorFileName)) {
						td = read(name, zip, null);
					} else {
						read(name, zip, null);
					}
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Couldn't read '" + name + "'!",
							e);
				}
			}
			return td;
		} else {
			throw new DecoJerException("Unknown file extension '" + fileName
					+ "'!");
		}
		return null;
	}

}