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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.decojer.cavaj.model.types.VarT;
import org.decojer.cavaj.readers.AsmReader;
import org.decojer.cavaj.readers.ClassReader;
import org.decojer.cavaj.readers.DexReader;
import org.decojer.cavaj.readers.Smali2Reader;
import org.decojer.cavaj.transformers.TrInnerClassesAnalysis;
import org.decojer.cavaj.utils.Cursor;
import org.decojer.cavaj.utils.MagicNumbers;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Decompilation unit.
 * 
 * Contains the global type pool (like {@code ClassLoader}) and loader.
 * 
 * @author André Pankraz
 */
public final class DU {

	private final static Logger LOGGER = Logger.getLogger(DU.class.getName());

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
		// cannot cache because of type variables
		return new ParamT(genericT, typeArgs);
	}

	@Getter
	private final T[] arrayInterfaceTs;

	// AsmReader is >3 times faster than JavassistReader!
	private final ClassReader classReader = new AsmReader(this);

	@Setter
	private List<CU> cus;

	private final DexReader dexReader = new Smali2Reader(this);

	@Getter
	private final List<TD> selectedTds = Lists.newArrayList();

	private final Map<String, T> ts = Maps.newHashMap();

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
				zip.write(source.getBytes(Charsets.UTF_8));
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
		return parseT(desc, new Cursor(), null);
	}

	/**
	 * Get dynamic method.
	 * 
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 * @return method
	 */
	public M getDynamicM(final String name, final String descriptor) {
		return new M(this, name, descriptor);
	}

	/**
	 * Get object type.
	 * 
	 * @return object type
	 */
	public ClassT getObjectT() {
		return (ClassT) getT(Object.class);
	}

	/**
	 * Get type for class.
	 * 
	 * @param klass
	 *            class
	 * @return type
	 */
	public T getT(final Class<?> klass) {
		return getT(klass.getName());
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

	// Lorg/pushingpixels/substance/internal/animation/StateTransitionMultiTracker.1;
	// org/pushingpixels/substance/internal/animation/StateTransitionMultiTracker.1
	// org/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>
	// org/infinispan/util/InfinispanCollections$EmptyReversibleOrderedSet.1
	// [I
	// java.lang.String[]
	private T getT(final String name, final boolean create) {
		if (name == null) {
			// important for e.g. read Object.class: super is null
			return null;
		}
		final char c = name.charAt(0);
		if (c == '[') {
			// java.lang.Class#getName() Javadoc explains this trick, fall back to descriptor
			return getDescT(name.replace('.', '/')); // wrong descriptor with . instead of /
		}
		if (c == 'L' && name.lastIndexOf(';') != -1) {
			// shouldn't happen: but class attribute info can contain both variants (incompatible
			// bytecode generators), not allways fully validated through JVM, fallback
			return getDescT(name);
		}
		if (name.indexOf('/') != -1) {
			return parseClassT(name, new Cursor(), null, null);
		}
		if (name.charAt(name.length() - 1) == ']' && name.charAt(name.length() - 2) == '[') {
			return getArrayT(getT(name.substring(0, name.length() - 2)));
		}
		// cache...
		T t = this.ts.get(name);
		if (t == null && create) {
			// can only be a TD...no int etc.
			t = new ClassT(this, name);
			this.ts.put(name, t);
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

	/**
	 * Parse class type.
	 * 
	 * @param s
	 *            descriptor / signature
	 * @param c
	 *            cursor
	 * @param context
	 *            enclosing type context
	 * @param enclosing
	 *            parent type (for recursion)
	 * @return class type
	 */
	private T parseClassT(final String s, final Cursor c, final Object context, final T enclosing) {
		// ClassTypeSignature: L PackageSpecifier_opt SimpleClassTypeSignature
		// ClassTypeSignatureSuffix_* ;
		// PackageSpecifier: Identifier / PackageSpecifier_*
		// SimpleClassTypeSignature: Identifier TypeArguments_opt
		// ClassTypeSignatureSuffix: . SimpleClassTypeSignature
		final int start = c.pos;
		char ch;
		// PackageSpecifier_opt Identifier
		while (s.length() > c.pos && (ch = s.charAt(c.pos)) != '<' && ch != '.' && ch != ';') {
			// $ could be a regular identifier char, we cannot do anything about this here
			++c.pos;
		}
		T t;
		if (enclosing != null) {
			// can just happen for signatures, they have . instead of $ for enclosing
			t = getT(enclosing.getName() + "$" + s.substring(start, c.pos).replace('/', '.'));
			((ClassT) t).setEnclosingT(enclosing);
		} else {
			t = getT(s.substring(start, c.pos).replace('/', '.'));
		}
		// TypeArguments_opt
		final TypeArg[] typeArgs = parseTypeArgs(s, c, context);
		if (typeArgs != null) {
			t = getParamT(t, typeArgs);
		}
		// ClassTypeSignatureSuffix_*
		if (s.length() > c.pos && s.charAt(c.pos) == '.') {
			++c.pos;
			return parseClassT(s, c, context, t);
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
	 * @param context
	 *            enclosing type context
	 * @return method parameter types
	 */
	public T[] parseMethodParamTs(final String s, final Cursor c, final Object context) {
		assert s.charAt(c.pos) == '(' : "Signature '" + s + "', pos " + c.pos + ", char: "
				+ s.charAt(c.pos);
		++c.pos;
		final List<T> ts = Lists.newArrayList();
		while (s.charAt(c.pos) != ')') {
			ts.add(parseT(s, c, context));
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
	 * @param context
	 *            enclosing type context
	 * @return type or {@code null} for signature end
	 */
	public T parseT(final String s, final Cursor c, final Object context) {
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
			final T t = parseClassT(s, c, context, null);
			assert s.charAt(c.pos) == ';' : s.charAt(c.pos);
			++c.pos;
			return t;
		}
		case '[':
			// ArrayTypeSignature
			return getArrayT(parseT(s, c, context));
		case 'T': {
			final int pos = s.indexOf(';', c.pos);
			final T t = new VarT(s.substring(c.pos, pos), context);
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
	 * @param context
	 *            enclosing type context
	 * @return type arguments or {@code null}
	 */
	private TypeArg[] parseTypeArgs(final String s, final Cursor c, final Object context) {
		// TypeArguments_opt
		if (s.length() <= c.pos || s.charAt(c.pos) != '<') {
			return null;
		}
		++c.pos;
		final List<TypeArg> ts = Lists.newArrayList();
		char ch;
		while ((ch = s.charAt(c.pos)) != '>') {
			switch (ch) {
			case '+':
				++c.pos;
				ts.add(TypeArg.subclassOf(parseT(s, c, context)));
				break;
			case '-':
				++c.pos;
				ts.add(TypeArg.superOf(parseT(s, c, context)));
				break;
			case '*':
				++c.pos;
				ts.add(new TypeArg());
				break;
			default:
				ts.add(new TypeArg(parseT(s, c, context)));
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
	 * @param context
	 *            enclosing type context
	 * @return type parameters or {@code null}
	 */
	public T[] parseTypeParams(final String s, final Cursor c, final Object context) {
		// TypeParams_opt
		if (s.charAt(c.pos) != '<') {
			return null; // optional
		}
		++c.pos;
		final List<T> ts = Lists.newArrayList();
		while (s.charAt(c.pos) != '>') {
			final int pos = s.indexOf(':', c.pos);
			// reuse ClassT for type parameter
			final ClassT typeParam = new ClassT(this, s.substring(c.pos, pos));
			c.pos = pos + 1;
			if (s.charAt(c.pos) == ':') {
				typeParam.setSuperT(getObjectT());
			} else {
				final T superT = parseT(s, c, context);
				typeParam.setSuperT(superT);
			}
			if (s.charAt(c.pos) == ':') {
				final List<T> interfaceTs = Lists.newArrayList();
				do {
					++c.pos;
					final T interfaceT = parseT(s, c, context);
					interfaceTs.add(interfaceT);
				} while (s.charAt(c.pos) == ':');
				typeParam.setInterfaceTs(interfaceTs.toArray(new T[interfaceTs.size()]));
			}
			typeParam.resolved();
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
	 * @return type declarations, not null
	 */
	private List<TD> read(final File file, final String selector) {
		final String fileName = file.getName();
		if (fileName.endsWith(".class")) {
			final List<TD> tds = Lists.newArrayList();

			// load full type declarations from complete package, to complex to decide here if
			// really not part of the compilation unit
			// TODO later load all type declarations, but not all bytecode details
			for (final File entry : file.getParentFile().listFiles()) {
				final String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				FileInputStream is = null;
				try {
					is = new FileInputStream(entry);
					final List<TD> readTds = read(is, name, fileName);
					if (readTds != null) {
						tds.addAll(readTds);
					}
				} catch (final Throwable e) {
					LOGGER.log(Level.WARNING, "Couldn't read file '" + name + "'!", e);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (final IOException e) {
							// nothing
						}
					}
				}
			}
			return tds;
		}
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			return read(fileInputStream, fileName, selector);
		} catch (final IOException e) {
			LOGGER.warning("Couldn't read file '" + file + "'!");
			return Lists.newArrayList();
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (final IOException e) {
					// nothing
				}
			}
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
	 * @return type declarations or null
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
		} else if (fileName.endsWith(".class")) {
			LOGGER.warning("Wrong magic number for file '" + fileName + "', isn't a JVM-Class!");
		}
		if (Arrays.equals(magicNumber, MagicNumbers.DEX)
				|| Arrays.equals(magicNumber, MagicNumbers.ODEX)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(magicNumber, 0, magicNumber.length);
			final List<TD> tds = this.dexReader.read(pis, selector);
			this.selectedTds.addAll(tds);
			return tds;
		} else if (fileName.endsWith(".dex") || fileName.endsWith(".odex")) {
			LOGGER.warning("Wrong magic number for file '" + fileName + "', isn't a Dalvik-Class!");
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
			final List<TD> tds = Lists.newArrayList();

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
	 */
	public List<TD> read(final String fileName) {
		final int pos = fileName.indexOf('!');
		if (pos == -1) {
			return read(new File(fileName), null);
		}
		// ...\jdk1.6.0_26\jre\lib\rt.jar!/com/sun/xml/internal/fastinfoset/Decoder.class
		return read(new File(fileName.substring(0, pos)), fileName.substring(pos + 1));
	}

}