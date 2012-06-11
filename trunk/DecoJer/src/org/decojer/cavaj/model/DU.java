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
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.decojer.DecoJerException;
import org.decojer.cavaj.reader.AsmReader;
import org.decojer.cavaj.reader.ClassReader;
import org.decojer.cavaj.reader.DexReader;
import org.decojer.cavaj.reader.SmaliReader;
import org.decojer.cavaj.util.MagicNumbers;

/**
 * Decompilation unit.
 * 
 * @author André Pankraz
 */
public class DU {

	private final static Logger LOGGER = Logger.getLogger(DU.class.getName());

	private T[] arrayInterfaceTs;

	private final ClassReader classReader = new AsmReader(this);

	private final DexReader dexReader = new SmaliReader(this);

	private final LinkedHashMap<String, TD> tds = new LinkedHashMap<String, TD>();

	private final HashMap<String, T> ts = new HashMap<String, T>();

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
	 * Get array type for component type.
	 * 
	 * @param componentT
	 *            component type (could be an array type)
	 * @return array type for component type
	 */
	public T getArrayT(final T componentT) {
		return getT(componentT + "[]");
	}

	/**
	 * Get type declaration.
	 * 
	 * @param desc
	 *            descriptor (package/subpackage/Type$Inner)
	 * @return type declaration
	 */
	public T getDescT(final String desc) {
		assert desc != null;

		switch (desc.charAt(0)) {
		case 'I':
			return T.INT;
		case 'S':
			return T.SHORT;
		case 'B':
			return T.BYTE;
		case 'C':
			return T.CHAR;
		case 'Z':
			return T.BOOLEAN;
		case 'F':
			return T.FLOAT;
		case 'J':
			return T.LONG;
		case 'D':
			return T.DOUBLE;
		case 'V':
			return T.VOID;
		case 'L': {
			final int pos = desc.indexOf(';', 1);
			if (pos == -1) {
				throw new DecoJerException("Missing ';' in reference descriptor: " + desc);
			}
			return getT(desc.substring(1, pos));
		}
		case '[':
			int dim = 0;
			while (desc.charAt(++dim) == '[') {
				;
			}
			final T componentT = getDescT(desc.substring(dim));
			final StringBuffer name = new StringBuffer(componentT.getName());
			for (int i = dim; i-- > 0;) {
				name.append("[]");
			}
			return getT(name.toString());
		}
		throw new DecoJerException("Unknown descriptor: " + desc);
	}

	/**
	 * Get type declaration.
	 * 
	 * @param clazz
	 *            class
	 * @return type declaration
	 */
	public T getT(final Class<?> clazz) {
		return getT(clazz.getName());
	}

	/**
	 * Get type declaration.
	 * 
	 * @param name
	 *            name (package.subpackage.Type$Inner)
	 * @return type declaration
	 */
	public T getT(final String name) {
		assert name != null && name.length() > 0;
		assert name.charAt(0) != 'L' : name;

		if (name.charAt(0) == '[') {
			// Class.getName() javadoc explains this trick, fall back to descriptor
			return getDescT(name);
		}

		if (name.equals(T.INT.getName())) {
			return T.INT;
		}
		if (name.equals(T.SHORT.getName())) {
			return T.SHORT;
		}
		if (name.equals(T.BYTE.getName())) {
			return T.BYTE;
		}
		if (name.equals(T.CHAR.getName())) {
			return T.CHAR;
		}
		if (name.equals(T.BOOLEAN.getName())) {
			return T.BOOLEAN;
		}
		if (name.equals(T.FLOAT.getName())) {
			return T.FLOAT;
		}
		if (name.equals(T.LONG.getName())) {
			return T.LONG;
		}
		if (name.equals(T.DOUBLE.getName())) {
			return T.DOUBLE;
		}
		if (name.equals(T.VOID.getName())) {
			return T.VOID;
		}
		if (name.equals(T.REF.getName())) {
			// for artificial array types REF[]
			return T.REF;
		}

		final String normName = name.replace('/', '.');
		T t = this.ts.get(normName);
		if (t == null) {
			t = new T(this, normName);
			if (normName.endsWith("[]")) {
				t.setSuperT(getT(name.substring(0, name.length() - 2)));
				if (this.arrayInterfaceTs == null) {
					this.arrayInterfaceTs = new T[] { getT(Cloneable.class),
							getT(Serializable.class) };
				}
				t.setInterfaceTs(this.arrayInterfaceTs);
			}
			this.ts.put(normName, t);
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

	/**
	 * Get type declarations.
	 * 
	 * @return type declarations
	 */
	public Set<Entry<String, TD>> getTds() {
		return this.tds.entrySet();
	}

	/**
	 * Read file. May be an archive with a file selector like this:
	 * 
	 * e.g. \jre\lib\rt.jar and /com/sun/xml/internal/fastinfoset/Decoder.class
	 * 
	 * @param file
	 *            file
	 * @param selector
	 *            selector (in case of an archive)
	 * @return type declaration (if single selector given)
	 * @throws IOException
	 *             read exception
	 */
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
					read(new FileInputStream(entry), name, null);
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Couldn't read '" + name + "'!", e);
				}
			}
		}
		return read(new FileInputStream(file), fileName, selector);
	}

	/**
	 * Read file. May be an archive with a file selector like this:
	 * 
	 * e.g. rt.jar and /com/sun/xml/internal/fastinfoset/Decoder.class
	 * 
	 * @param is
	 *            input stream
	 * @param fileName
	 *            file name (or null, optional - we prefere magic numbers)
	 * @param selector
	 *            selector (in case of an archive)
	 * @return type declaration (if single selector given)
	 * @throws IOException
	 *             read exception
	 */
	public TD read(final InputStream is, final String fileName, final String selector)
			throws IOException {
		final byte[] magicNumber = new byte[MagicNumbers.LENGTH];
		final int read = is.read(magicNumber, 0, magicNumber.length);
		if (read < magicNumber.length) {
			return null;
		}
		if (Arrays.equals(magicNumber, MagicNumbers.CLASS)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			return this.classReader.read(pis); // selector has no meaning here
		}
		if (Arrays.equals(magicNumber, MagicNumbers.DEX)
				|| Arrays.equals(magicNumber, MagicNumbers.ODEX)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			return this.dexReader.read(pis, selector);
		}
		if (Arrays.equals(magicNumber, MagicNumbers.ZIP)) {
			String selectorPrefix = null;
			String selectorMatch = null;
			if (selector != null && selector.endsWith(".class")) {
				selectorMatch = selector.charAt(0) == '/' ? selector.substring(1) : selector;
				final int pos = selectorMatch.lastIndexOf('/');
				if (pos != -1) {
					selectorPrefix = selectorMatch.substring(0, pos + 1);
				}
			}
			TD selectorTd = null;

			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			final ZipInputStream zip = new ZipInputStream(pis);
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				final String name = zipEntry.getName();
				if (name.endsWith(".class")
						&& selectorPrefix != null
						&& (!name.startsWith(selectorPrefix) || name.indexOf('/',
								selectorPrefix.length()) != -1)) {
					continue;
				}
				try {
					final TD td = read(zip, name, null);
					if (name.equals(selectorMatch)) {
						selectorTd = td;
					}
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Couldn't read '" + name + "'!", e);
				}
			}
			return selectorTd;
		}
		return null;
	}

	/**
	 * Read file. May be an archive with a file selector like this:
	 * 
	 * e.g. \jre\lib\rt.jar!/com/sun/xml/internal/fastinfoset/Decoder.class
	 * 
	 * @param fileName
	 *            file name & optional selector
	 * @return type declaration (if single selector given)
	 * @throws IOException
	 *             read exception
	 */
	public TD read(final String fileName) throws IOException {
		final int pos = fileName.indexOf('!');
		if (pos == -1) {
			return read(new File(fileName), null);
		}
		// ...\jdk1.6.0_26\jre\lib\rt.jar!/com/sun/xml/internal/fastinfoset/Decoder.class
		return read(new File(fileName.substring(0, pos)), fileName.substring(pos + 1));
	}

}