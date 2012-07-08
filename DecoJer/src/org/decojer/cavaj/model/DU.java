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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Getter;

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
 * Contains the global type pool (like <code>ClassLoader</code>) and loader.
 * 
 * @author André Pankraz
 */
public final class DU {

	private final static Logger LOGGER = Logger.getLogger(DU.class.getName());

	private T[] arrayInterfaceTs;

	private final ClassReader classReader = new AsmReader(this);

	@Getter
	private final HashMap<String, CU> cus = new HashMap<String, CU>();

	private final DexReader dexReader = new SmaliReader(this);

	private final Map<String, T> ts = new HashMap<String, T>();

	@Getter
	private final List<TD> tds = new ArrayList<TD>();

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
	public ArrayT getArrayT(final T componentT) {
		return new ArrayT(this, componentT);
	}

	/**
	 * Get type for descriptor.
	 * 
	 * @param desc
	 *            descriptor (package/subpackage/Type$Inner)
	 * @return type
	 */
	public T getDescT(final String desc) {
		return parseT(desc, new Cursor());
	}

	/**
	 * Get type declaration for descriptor.
	 * 
	 * @param desc
	 *            descriptor (package/subpackage/Type$Inner)
	 * @return type declaration
	 */
	public TD getDescTd(final String desc) {
		return (TD) getDescT(desc);
	}

	/**
	 * Get type for class.
	 * 
	 * @param clazz
	 *            class
	 * @return type
	 */
	public T getT(final Class<?> clazz) {
		return getT(clazz.getName());
	}

	/**
	 * Get type for type name.
	 * 
	 * Works for basic types, predefined multi-type constants, type declarations and array types,
	 * but not for Parameterized Types.
	 * 
	 * Works also for class names, where array types are presented by internal descriptor names.
	 * 
	 * @param name
	 *            type name
	 * @return type
	 * @see java.lang.Class#getName()
	 */
	public T getT(final String name) {
		assert name.charAt(0) != 'L' : name;

		if (name.charAt(0) == '[') {
			// java.lang.Class#getName() Javadoc explains this trick, fall back to descriptor
			return getDescT(name);
		}

		if (name.charAt(name.length() - 1) == ']' && name.charAt(name.length() - 2) == '[') {
			return getArrayT(getT(name.substring(0, name.length() - 2)));
		}
		// some calling bytecode libraries don't convert this, homogenize
		final String normName = name.replace('/', '.');

		T t = this.ts.get(normName);
		if (t == null) {
			// can only be a TD...no int etc.
			t = new TD(normName, this);
			this.ts.put(normName, t);
		}
		return t;
	}

	/**
	 * Get type declaration for type name.
	 * 
	 * @param name
	 *            type name
	 * @return type declaration
	 */
	public TD getTd(final String name) {
		return (TD) getT(name);
	}

	/**
	 * Parse array type from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return array type
	 */
	public T parseArrayT(final String s, final Cursor c) {
		assert s.charAt(c.pos) == '[';

		++c.pos;
		return getArrayT(parseT(s, c));
	}

	/**
	 * Parse class type from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return class type
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
	 * Parse type from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return type or <code>null</code> for signature end
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
			final T t = new TD(s.substring(c.pos, pos), this); // TODO, really reuse?
			c.pos = pos + 1;
			return t;
		}
		throw new DecoJerException("Unknown type in '" + s + "' (" + c.pos + ")!");
	}

	/**
	 * Parse type arguments from signature.
	 * 
	 * We don't follow the often used <code>WildcardType</code> paradigma. Wildcards are only
	 * allowed in the context of parameterized types and aren't useable as standalone types.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return type arguments or null
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
	 * Parse type parameters from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return type parameters or <code>null</code>
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
			final TD typeParam = new TD(s.substring(c.pos, pos), this); // TODO really reuse?
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
	 * @return type declarations
	 * @throws IOException
	 *             read exception
	 */
	public List<TD> read(final File file, final String selector) throws IOException {
		final String fileName = file.getName();
		if (fileName.endsWith(".class")) {
			final List<TD> tds = new ArrayList<TD>();

			// load full type declarations from complete package, to complex to decide here if
			// really not part of the compilation unit
			// TODO later load all type declarations, but not all bytecode details
			for (final File entry : file.getParentFile().listFiles()) {
				final String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				try {
					tds.addAll(read(new FileInputStream(entry), name, fileName));
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Couldn't read '" + name + "'!", e);
				}
			}
			return tds;
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
	 * @return type declarations
	 * @throws IOException
	 *             read exception
	 */
	public List<TD> read(final InputStream is, final String fileName, final String selector)
			throws IOException {
		final byte[] magicNumber = new byte[MagicNumbers.LENGTH];
		final int read = is.read(magicNumber, 0, magicNumber.length);
		if (read < magicNumber.length) {
			return null;
		}
		if (Arrays.equals(magicNumber, MagicNumbers.CLASS)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			// selector has no meaning here
			final TD td = this.classReader.read(pis);
			if (selector == null || fileName.equals(selector)) {
				this.tds.add(td);
				return Collections.singletonList(td);
			}
			return Collections.emptyList();
		}
		if (Arrays.equals(magicNumber, MagicNumbers.DEX)
				|| Arrays.equals(magicNumber, MagicNumbers.ODEX)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			final List<TD> tds = this.dexReader.read(pis, selector);
			this.tds.addAll(tds);
			return tds;
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
			final List<TD> tds = new ArrayList<TD>();

			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			final ZipInputStream zip = new ZipInputStream(pis);
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				final String name = zipEntry.getName();
				// load full type declarations from complete package, to complex to decide here if
				// really not part of the compilation unit
				// TODO later load all type declarations, but not all bytecode details
				if (name.endsWith(".class")
						&& selectorPrefix != null
						&& (!name.startsWith(selectorPrefix) || name.indexOf('/',
								selectorPrefix.length()) != -1)) {
					continue;
				}
				try {
					final List<TD> readTds = read(zip, name, null);
					if (readTds != null && (selectorMatch == null || selectorMatch.equals(name))) {
						tds.addAll(readTds);
					}
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Couldn't read '" + name + "'!", e);
				}
			}
			return tds;
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
	 * @return type declarations
	 * @throws IOException
	 *             read exception
	 */
	public List<TD> read(final String fileName) throws IOException {
		final int pos = fileName.indexOf('!');
		if (pos == -1) {
			return read(new File(fileName), null);
		}
		// ...\jdk1.6.0_26\jre\lib\rt.jar!/com/sun/xml/internal/fastinfoset/Decoder.class
		return read(new File(fileName.substring(0, pos)), fileName.substring(pos + 1));
	}

}