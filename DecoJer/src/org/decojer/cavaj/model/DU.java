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
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import lombok.Getter;
import lombok.Setter;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.types.ArrayT;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.model.types.ParamT.TypeArg;
import org.decojer.cavaj.readers.AsmReader;
import org.decojer.cavaj.readers.ClassReader;
import org.decojer.cavaj.readers.DexReader;
import org.decojer.cavaj.readers.SmaliReader;
import org.decojer.cavaj.transformers.TrInnerClassesAnalysis;
import org.decojer.cavaj.utils.Cursor;
import org.decojer.cavaj.utils.MagicNumbers;

/**
 * Decompilation unit.
 * 
 * Contains the global type pool (like {@code ClassLoader}) and loader.
 * 
 * @author André Pankraz
 */
public final class DU {

	private final static Logger LOGGER = Logger.getLogger(DU.class.getName());

	private static final Charset UTF8 = Charset.forName("utf-8");

	/**
	 * Get parameterized type for generic type and type arguments.
	 * 
	 * @param genericT
	 *            generic type with matching type parameters
	 * @param typeArgs
	 *            type arguments for matching type parameters
	 * @return parameterized type for generic type and type arguments
	 */
	public static ParamT getParamT(final T genericT, final TypeArg[] typeArgs) {
		return new ParamT(genericT, typeArgs);
	}

	@Getter
	private final T[] arrayInterfaceTs;

	// AsmReader is >10 times faster than JavassistReader!
	private final ClassReader classReader = new AsmReader(this);

	@Setter
	private List<CU> cus;

	private final DexReader dexReader = new SmaliReader(this);

	@Getter
	private final List<TD> selectedTds = new ArrayList<TD>();

	private final Map<String, T> ts = new HashMap<String, T>();

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
		this.arrayInterfaceTs = new T[] { getT(Cloneable.class), getT(Serializable.class) };
	}

	protected void createCus() {
		TrInnerClassesAnalysis.transform(this);
	}

	public String decompile(final String typeDeclarationName) {
		final T t = this.ts.get(typeDeclarationName);
		if (t == null) {
			return "<UNKNOWN>";
		}
		return ((ClassT) t).getTd().getCu().decompile();
	}

	/**
	 * Decompile all type declarations from decompilation unit into output stream.
	 * 
	 * @param os
	 *            output stream
	 * @throws IOException
	 *             read exception
	 */
	public void decompileAll(final OutputStream os) throws IOException {
		final ZipOutputStream zip = new ZipOutputStream(os);
		for (final CU cu : getCus()) {
			try {
				final String source = cu.decompile();
				final String sourceFileName = cu.getSourceFileName();
				final String packageName = cu.getPackageName();
				String zipEntryName;
				if (packageName != null && packageName.length() != 0) {
					zipEntryName = packageName.replace('.', '/') + '/' + sourceFileName;
				} else {
					zipEntryName = sourceFileName;
				}
				final ZipEntry zipEntry = new ZipEntry(zipEntryName);
				zip.putNextEntry(zipEntry);
				zip.write(source.getBytes(UTF8));
			} catch (final Throwable t) {
				LOGGER.log(Level.WARNING, "Decompilation problems for '" + cu + "'!", t);
			} finally {
				cu.clear();
			}
		}
		zip.finish();
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
	 * Get compilaton unit for name.
	 * 
	 * @param name
	 *            compilation unit name
	 * @return compilation unit or {@code null}
	 */
	public CU getCu(final String name) {
		for (final CU cu : getCus()) {
			if (cu.getName().equals(name)) {
				return cu;
			}
		}
		return null;
	}

	public List<CU> getCus() {
		if (this.cus == null) {
			createCus();
		}
		return this.cus;
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
		return getT(name, true);
	}

	private T getT(final String name, final boolean create) {
		final char c = name.charAt(0);
		if (c == 'L') {
			// class attribute info can contain both variants (incompatible bytecode generators),
			// not allways fully validated through JVM
			return getDescT(name);
		}
		if (c == '[') {
			// java.lang.Class#getName() Javadoc explains this trick, fall back to descriptor
			return getDescT(name);
		}

		if (name.charAt(name.length() - 1) == ']' && name.charAt(name.length() - 2) == '[') {
			return getArrayT(getT(name.substring(0, name.length() - 2)));
		}
		// some calling bytecode libraries don't convert this, homogenize
		final String normName = name.replace('/', '.');

		T t = this.ts.get(normName);
		if (t == null && create) {
			// can only be a TD...no int etc.
			t = new ClassT(normName, this);
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
		return ((ClassT) getT(name, false)).getTd();
	}

	/**
	 * Get all types.
	 * 
	 * @return types
	 */
	public Collection<T> getTs() {
		return this.ts.values();
	}

	private T parseClassT(final String s, final Cursor c, final T parentT) {
		// PackageSpecifier_opt SimpleClassTypeSignature ClassTypeSignatureSuffix_*
		final int start = c.pos;
		char ch;
		while ((ch = s.charAt(c.pos)) != '<' && ch != ';') {
			++c.pos;
		}
		T t;
		if (parentT != null) {
			// TODO big hmmm
			t = getT(((ParamT) parentT).getGenericT().getName() + "$" + s.substring(start, c.pos));
			// ??? ((ClassT) t).setEnclosingT(parentT);
		} else {
			t = getT(s.substring(start, c.pos));
		}
		final TypeArg[] typeArgs = parseTypeArgs(s, c);
		if (typeArgs != null) {
			t = getParamT(t, typeArgs);
			// ClassTypeSignatureSuffix_*
			// e.g.:
			// Lorg/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>;
			while (s.charAt(c.pos) == '.') {
				++c.pos;
				t = parseClassT(s, c, t);
			}
		}
		return t;
	}

	/**
	 * Parse method parameter types from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return method parameter types
	 */
	public T[] parseMethodParamTs(final String s, final Cursor c) {
		assert s.charAt(c.pos) == '(' : s.charAt(c.pos);
		++c.pos;
		final ArrayList<T> ts = new ArrayList<T>();
		while (s.charAt(c.pos) != ')') {
			ts.add(parseT(s, c));
		}
		++c.pos;
		return ts.toArray(new T[ts.size()]);
	}

	/**
	 * Parse type from signature.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return type or {@code null} for signature end
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
		case 'L': {
			// ClassTypeSignature
			final T t = parseClassT(s, c, null);
			assert s.charAt(c.pos) == ';' : s.charAt(c.pos);
			++c.pos;
			return t;
		}
		case '[':
			// ArrayTypeSignature
			return getArrayT(parseT(s, c));
		case 'T': {
			final int pos = s.indexOf(';', c.pos);
			final T t = new ClassT(s.substring(c.pos, pos), this); // TODO hmmm
			c.pos = pos + 1;
			return t;
		}
		default:
			throw new DecoJerException("Unknown type in '" + s + "' (" + c.pos + ")!");
		}
	}

	/**
	 * Parse type arguments from signature.
	 * 
	 * We don't follow the often used {@code WildcardType} paradigma. Wildcards are only allowed in
	 * the context of parameterized types and aren't useable as standalone types.
	 * 
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return type arguments or {@code null}
	 */
	private TypeArg[] parseTypeArgs(final String s, final Cursor c) {
		// TypeArguments_opt
		if (s.charAt(c.pos) != '<') {
			return null;
		}
		++c.pos;
		final ArrayList<TypeArg> ts = new ArrayList<TypeArg>();
		char ch;
		while ((ch = s.charAt(c.pos)) != '>') {
			switch (ch) {
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
	 * @return type parameters or {@code null}
	 */
	public T[] parseTypeParams(final String s, final Cursor c) {
		// TypeParams_opt
		if (s.charAt(c.pos) != '<') {
			return null; // optional
		}
		++c.pos;
		final ArrayList<T> ts = new ArrayList<T>();
		while (s.charAt(c.pos) != '>') {
			final int pos = s.indexOf(':', c.pos);
			// TODO hmmm
			final ClassT typeParam = new ClassT(s.substring(c.pos, pos), this);
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
		final FileInputStream fileInputStream = new FileInputStream(file);
		try {
			return read(fileInputStream, fileName, selector);
		} finally {
			fileInputStream.close();
		}
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
				this.selectedTds.add(td);
				return Collections.singletonList(td);
			}
			return Collections.emptyList();
		}
		if (Arrays.equals(magicNumber, MagicNumbers.DEX)
				|| Arrays.equals(magicNumber, MagicNumbers.ODEX)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			final List<TD> tds = this.dexReader.read(pis, selector);
			this.selectedTds.addAll(tds);
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