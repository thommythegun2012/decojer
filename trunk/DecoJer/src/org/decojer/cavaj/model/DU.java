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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
import org.decojer.cavaj.model.type.ArrayT;
import org.decojer.cavaj.model.type.ParamT;
import org.decojer.cavaj.model.type.ParamT.TypeArg;
import org.decojer.cavaj.reader.AsmReader;
import org.decojer.cavaj.reader.ClassReader;
import org.decojer.cavaj.reader.DexReader;
import org.decojer.cavaj.reader.SmaliReader;
import org.decojer.cavaj.util.Cursor;
import org.decojer.cavaj.util.MagicNumbers;

/**
 * Decompilation unit.
 * 
 * Contains the global Type Pool (like <code>ClassLoader</code>) and Loader.
 * 
 * @author André Pankraz
 */
public final class DU {

	private final static Logger LOGGER = Logger.getLogger(DU.class.getName());

	private T[] arrayInterfaceTs;

	private final ClassReader classReader = new AsmReader(this);

	private final DexReader dexReader = new SmaliReader(this);

	private final LinkedHashMap<String, TD> tds = new LinkedHashMap<String, TD>();

	private final HashMap<String, T> ts = new HashMap<String, T>();

	public DU() {
		// init type pool with primitives-/multi-types
		try {
			for (final Field f : T.class.getFields()) {
				if ((f.getModifiers() & Modifier.PUBLIC) == 0 || f.getType() != T.class) {
					continue;
				}
				final T t = (T) f.get(null);
				this.ts.put(t.getName(), t);
			}
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException("Couldn't init decompilation unit!", e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException("Couldn't init decompilation unit!", e);
		}
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

	public T[] getArrayInterfaceTs() {
		if (this.arrayInterfaceTs == null) {
			this.arrayInterfaceTs = new T[] { getT(Cloneable.class), getT(Serializable.class) };
		}
		return this.arrayInterfaceTs;
	}

	/**
	 * Get array type for component type.
	 * 
	 * @param componentT
	 *            component type (could be an array type)
	 * @return array type for component type
	 */
	public T getArrayT(final T componentT) {
		return new ArrayT(this, componentT);
	}

	/**
	 * Get type declaration.
	 * 
	 * @param desc
	 *            descriptor (package/subpackage/Type$Inner)
	 * @return type declaration
	 */
	public T getDescT(final String desc) {
		return parseT(desc, new Cursor());
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
		if (name.charAt(name.length() - 1) == ']' && name.charAt(name.length() - 2) == '[') {
			return getArrayT(getT(name.substring(0, name.length() - 2)));
		}
		final String normName = name.replace('/', '.');

		T t = this.ts.get(normName);
		if (t == null) {
			t = new T(this, normName);
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
	 * Parse Array Type from Signature.
	 * 
	 * @param s
	 *            Signature
	 * @param c
	 *            Cursor
	 * @return Array Type
	 */
	public T parseArrayT(final String s, final Cursor c) {
		assert s.charAt(c.pos) == '[';

		++c.pos;
		return getArrayT(parseT(s, c));
	}

	/**
	 * Parse Class Type from Signature.
	 * 
	 * @param s
	 *            Signature
	 * @param c
	 *            Cursor
	 * @return Class Tyoe
	 */
	public T parseClassT(final String s, final Cursor c) {
		assert s.charAt(c.pos) == 'L';

		++c.pos;
		final int pos = c.pos;
		char t;
		do {
			t = s.charAt(c.pos++);
		} while (t != '<' && t != ';');
		final T rawT = getT(s.substring(pos, c.pos - 1));
		if (t != '<') {
			return rawT;
		}
		--c.pos;
		final TypeArg[] typeArgs = parseTypeArgs(s, c);
		++c.pos;

		// assert rawT.getTypeParams().length == typeArgs.length; // TODO

		return new ParamT(rawT, typeArgs);
	}

	/**
	 * Parse Type from Signature.
	 * 
	 * @param s
	 *            Signature
	 * @param c
	 *            Cursor
	 * @return Type or <code>null</code> for Signature end
	 */
	public T parseT(final String s, final Cursor c) {
		if (s.length() <= c.pos) {
			return null;
		}
		switch (s.charAt(c.pos++)) {
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
		case 'L':
			--c.pos;
			return parseClassT(s, c);
		case '[':
			--c.pos;
			return parseArrayT(s, c);
		case 'T':
			final int pos = s.indexOf(';', c.pos);
			if (pos == -1) {
				throw new DecoJerException("Type variable in '" + s + "' (" + c.pos
						+ ") must end with ';'!");
			}
			final T t = new T(this, s.substring(c.pos, pos)); // TODO
			c.pos = pos + 1;
			return t;
		}
		throw new DecoJerException("Unknown type in '" + s + "' (" + c.pos + ")!");
	}

	/**
	 * Parse Type Arguments from Signature.
	 * 
	 * We don't follow the often used <code>WildcardType</code> paradigma. Wildcards are only
	 * allowed in the context of Parameterized Types and aren't useable as standalone Types.
	 * 
	 * @param s
	 *            Signature
	 * @param c
	 *            Cursor
	 * @return Type Arguments or null
	 */
	private TypeArg[] parseTypeArgs(final String s, final Cursor c) {
		if (s.charAt(c.pos) != '<') {
			return null;
		}
		++c.pos;
		final ArrayList<TypeArg> ts = new ArrayList<TypeArg>();
		char t;
		while ((t = s.charAt(c.pos)) != '>') {
			switch (t) {
			case '+':
				++c.pos;
				ts.add(TypeArg.subclassOf(parseT(s, c)));
				break;
			case '-':
				++c.pos;
				ts.add(TypeArg.superOf(parseT(s, c)));
				break;
			case '*':
				++c.pos;
				ts.add(new TypeArg());
				break;
			default:
				ts.add(new TypeArg(parseT(s, c)));
			}
		}
		++c.pos;
		return ts.toArray(new TypeArg[ts.size()]);
	}

	/**
	 * Parse Type Parameters from Signature.
	 * 
	 * @param s
	 *            Signature
	 * @param c
	 *            Cursor
	 * @return Type Parameters or null
	 */
	public T[] parseTypeParams(final String s, final Cursor c) {
		if (s.charAt(c.pos) != '<') {
			return null;
		}
		++c.pos;
		final ArrayList<T> ts = new ArrayList<T>();
		while (s.charAt(c.pos) != '>') {
			final int pos = s.indexOf(':', c.pos);
			if (pos == -1) {
				throw new DecoJerException("Type parameter name '" + s + "' at position '" + c.pos
						+ "' must end with ':'!");
			}
			final T typeParam = new T(this, s.substring(c.pos, pos));
			c.pos = pos + 1;
			if (s.charAt(c.pos) != ':') {
				typeParam.setSuperT(parseT(s, c));
			}
			final ArrayList<T> interfaceTs = new ArrayList<T>();
			while (s.charAt(c.pos) == ':') {
				++c.pos;
				interfaceTs.add(parseT(s, c));
			}
			typeParam.setInterfaceTs(interfaceTs.toArray(new T[interfaceTs.size()]));
			ts.add(typeParam);
		}
		++c.pos;
		return ts.toArray(new T[ts.size()]);
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