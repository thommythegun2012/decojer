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

import java.io.ByteArrayInputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.methods.ClassM;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.AnnotatedT;
import org.decojer.cavaj.model.types.ArrayT;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.model.types.ParameterizedT;
import org.decojer.cavaj.model.types.QualifiedT;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.model.types.VarT;
import org.decojer.cavaj.model.types.WildcardT;
import org.decojer.cavaj.readers.ClassReader;
import org.decojer.cavaj.readers.DexReader;
import org.decojer.cavaj.readers.asm.AsmReader;
import org.decojer.cavaj.readers.smali2.Smali2Reader;
import org.decojer.cavaj.transformers.TrInnerClassesAnalysis;
import org.decojer.cavaj.utils.Cursor;
import org.decojer.cavaj.utils.MagicNumbers;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Decompilation unit.
 *
 * Contains the global type pool (like {@code ClassLoader}) and loader.
 *
 * @author André Pankraz
 */
@Slf4j
public final class DU {

	/**
	 * Get type with added type annotation for given type and type annotation.
	 *
	 * @param t
	 *            type
	 * @param a
	 *            annotation
	 * @return annotated type
	 */
	@Nonnull
	public static AnnotatedT getAnnotatedT(@Nonnull final T t, @Nonnull final A a) {
		if (!t.isAnnotated()) {
			return new AnnotatedT(t, new A[] { a });
		}
		final AnnotatedT annotT = (AnnotatedT) t;
		final A[] as = annotT.getAs();
		assert as != null : "cannot be null for annotated";
		for (final A checkA : as) {
			if (checkA.getT().equals(a.getT())) {
				log.warn("Type '" + t + "' already has the type annotation '" + a + "'!");
				return annotT;
			}
		}
		// don't change annotation array (changes name), recreate type
		final A[] newAs = new A[as.length + 1];
		System.arraycopy(as, 0, newAs, 0, as.length);
		newAs[as.length] = a;
		return new AnnotatedT(annotT.getRawT(), newAs);
	}

	/**
	 * Get wildcard type {@code <?>}.
	 *
	 * This type is used as type argument, but other types can be type arguments too.
	 *
	 * @return wildcard type
	 */
	@Nonnull
	public static WildcardT getMatchesWildcardT() {
		return new WildcardT(null, false);
	}

	/**
	 * Get parameterized type for generic type and type arguments.
	 *
	 * @param genericT
	 *            generic type with matching type parameters
	 * @param typeArgs
	 *            type arguments for matching type parameters
	 * @return parameterized type for generic type and type arguments
	 */
	@Nonnull
	public static ParameterizedT getParameterizedT(@Nonnull final T genericT,
			@Nonnull final T[] typeArgs) {
		// cannot cache because of type variables
		return new ParameterizedT(genericT, typeArgs);
	}

	/**
	 * Get default qualified type for given type, converting type parameters to type arguments with
	 * variables.
	 *
	 * This is for instance used for generating fully qualified types for
	 * {@link ClassM#setReceiverT(T)}.
	 *
	 * TODO doesn't yet work this way because we would have to read outer types first
	 *
	 * @param t
	 *            type
	 * @return qualified type
	 */
	@Nonnull
	public static T getQualifiedT(@Nonnull final T t) {
		if (t.isQualified()) {
			return t;
		}
		T qualifiedT = null;
		for (T currentT : t.getEnclosingTs()) {
			final T[] typeParams = currentT.getTypeParams();
			if (typeParams.length > 0) {
				final T[] typeArgs = new T[typeParams.length];
				for (int i = typeParams.length; i-- > 0;) {
					typeArgs[i] = getVarT(typeParams[i].getName(), t);
				}
				currentT = getParameterizedT(currentT, typeArgs);
			}
			if (qualifiedT == null) {
				qualifiedT = currentT;
				continue;
			}
			qualifiedT = getQualifiedT(qualifiedT, currentT);
		}
		assert qualifiedT != null;
		return qualifiedT;
	}

	/**
	 * Get qualified type for type and qualifier type.
	 *
	 * Final result should be a chain like for T0.T1.T2.T3 this: Q(Q(Q(T0, T1), T2, T3))
	 *
	 * Enclosing info might yet be unknown! Chain is fixed lazy in
	 * {@link QualifiedT#getQualifierT()}.
	 *
	 * @param t
	 *            type
	 * @param qualifierT
	 *            qualifier type
	 * @return qualified type
	 */
	@Nonnull
	public static T getQualifiedT(@Nonnull final T qualifierT, @Nonnull final T t) {
		if (!t.getName().startsWith(qualifierT.getName() + "$")) {
			log.warn("Cannot get qualified type for '" + t + "' with qualifier '" + qualifierT
					+ "'!");
			return t;
		}
		if (!t.isQualified()) {
			if (t.isAnnotated()) {
				// Anno(Qual(qual,t)) is same like Qual(qual,Anno(t)), prefer first
				t.setRawT(new QualifiedT(qualifierT, t.getRawT()));
				return t;
			}
			return new QualifiedT(qualifierT, t);
		}
		final T currentQualifierT = t.getQualifierT();
		assert currentQualifierT != null : "cannot be null for qualified";

		// both names must intersect from beginning till somewhere:
		final int qNl = qualifierT.getName().length();
		final int cqNl = currentQualifierT.getName().length();

		// now we have 3 possebilities comparing lengths: qNl == cqNl, qNl > cqNl, qNl < cqNl
		if (qNl == cqNl) {
			// replace qualifier and done
			t.setQualifierT(qualifierT);
			return t;
		}
		if (qNl > cqNl) {
			t.setQualifierT(getQualifiedT(currentQualifierT, qualifierT));
			return t;
		}
		// qNl < cqNl
		t.setQualifierT(getQualifiedT(qualifierT, currentQualifierT));
		return t;
	}

	/**
	 * Get wildcard type {@code <? extends t>}.
	 *
	 * This type is used as type argument, but other types can be type arguments too.
	 *
	 * @param t
	 *            type bound
	 *
	 * @return wildcard type
	 */
	@Nonnull
	public static WildcardT getSubclassOfWildcardT(@Nonnull final T t) {
		return new WildcardT(t, true);
	}

	/**
	 * Get wildcard type {@code <? super t>}.
	 *
	 * This type is used as type argument, but other types can be type arguments too.
	 *
	 * @param t
	 *            type bound
	 *
	 * @return wildcard type
	 */
	@Nonnull
	public static WildcardT getSuperOfWildcardT(@Nonnull final T t) {
		return new WildcardT(t, false);
	}

	/**
	 * Get type variable.
	 *
	 * This type is used as type argument, but other types can be type arguments too, e.g. a ClassT
	 * or an extension by WildcardT.
	 *
	 * Is not used in declaration like ParamT but is used for referencing it, should be resolved to
	 * a ParamT, but can only be done lazy.
	 *
	 * @param name
	 *            type name
	 * @param context
	 *            enclosing type context
	 * @return type variable
	 */
	@Nonnull
	public static VarT getVarT(@Nonnull final String name, final Object context) {
		return new VarT(name, context);
	}

	@Getter
	private final T[] arrayInterfaceTs;

	// AsmReader is >3 times faster than JavassistReader!
	private final ClassReader classReader = new AsmReader(this);

	@Setter
	private List<CU> cus;

	private final DexReader dexReader = new Smali2Reader(this);

	@Getter
	private final List<T> selectedTs = Lists.newArrayList();

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
			throw new DecoJerException("Couldn't init decompilation unit!", e);
		} catch (final IllegalAccessException e) {
			throw new DecoJerException("Couldn't init decompilation unit!", e);
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
		return t.getCu().decompile();
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
				log.warn("Decompilation problems for '" + cu + "'!", t);
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
	@Nonnull
	public ArrayT getArrayT(@Nonnull final T componentT) {
		return new ArrayT(this, componentT);
	}

	/**
	 * Get compilaton unit for name.
	 *
	 * @param name
	 *            compilation unit name
	 * @return compilation unit
	 */
	@Nullable
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
	@Nullable
	public T getDescT(@Nullable final String desc) {
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
	@Nonnull
	public M getDynamicM(@Nonnull final String name, @Nonnull final String descriptor) {
		return new ClassM(this, name, descriptor);
	}

	/**
	 * Get object type.
	 *
	 * @return object type
	 */
	@Nonnull
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
	@Nonnull
	public T getT(@Nonnull final Class<?> klass) {
		final String name = klass.getName();
		assert name != null;
		return getT(name);
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
	@Nonnull
	public T getT(@Nonnull final String name) {
		final T ret = getT(name, true);
		assert ret != null;
		return ret;
	}

	// Lorg/pushingpixels/substance/internal/animation/StateTransitionMultiTracker.1;
	// org/pushingpixels/substance/internal/animation/StateTransitionMultiTracker.1
	// org/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>
	// org/infinispan/util/InfinispanCollections$EmptyReversibleOrderedSet.1
	// [I
	// java.lang.String[]
	@Nullable
	private T getT(@Nonnull final String name, final boolean create) {
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
			final String componentName = name.substring(0, name.length() - 2);
			assert componentName != null;
			return getArrayT(getT(componentName));
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
	 * Get all types.
	 *
	 * @return types
	 */
	public List<T> getTs() {
		final Collection<T> values = this.ts.values();
		assert values != null;
		return Lists.newArrayList(values);
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
	 * @param enclosingT
	 *            parent type (for recursion)
	 * @return class type
	 */
	@Nullable
	private T parseClassT(@Nonnull final String s, @Nonnull final Cursor c, final Object context,
			final T enclosingT) {
		// ClassTypeSignature: L PackageSpecifier_opt SimpleClassTypeSignature
		// ClassTypeSignatureSuffix_* ;
		// PackageSpecifier: Identifier / PackageSpecifier_*
		// SimpleClassTypeSignature: Identifier TypeArguments_opt
		// ClassTypeSignatureSuffix: . SimpleClassTypeSignature
		final int start = c.pos;
		// PackageSpecifier_opt Identifier
		typeName: for (; c.pos < s.length(); ++c.pos) {
			// $ can be a regular identifier char, we cannot do anything about this here
			switch (s.charAt(c.pos)) {
			case ';': // type name end
			case '.': // inner type
			case '<': // type argument
				break typeName;
			}
		}
		T t;
		final String typeName = s.substring(start, c.pos).replace('/', '.');
		assert typeName != null;
		if (enclosingT != null) {
			// can just happen for signatures, they have . instead of $ for enclosing
			t = getQualifiedT(enclosingT, getT(enclosingT.getName() + "$" + typeName));
		} else {
			t = getT(typeName);
		}
		// TypeArguments_opt
		final T[] typeArgs = parseTypeArgs(s, c, context);
		if (typeArgs != null) {
			t = getParameterizedT(t, typeArgs);
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
	@Nullable
	public T[] parseMethodParamTs(@Nullable final String s, @Nonnull final Cursor c,
			final Object context) {
		if (s == null || s.length() <= c.pos || s.charAt(c.pos) != '(') {
			return null;
		}
		++c.pos;
		final List<T> ts = Lists.newArrayList();
		while (s.length() > c.pos) {
			if (s.charAt(c.pos) == ')') {
				++c.pos;
				if (ts.size() == 0) {
					return null;
				}
				return ts.toArray(new T[ts.size()]);
			}
			final T t = parseT(s, c, context);
			if (t == null) {
				break;
			}
			ts.add(t);
		}
		log.warn(context + ": Cannot read method parameter types in '" + s + "' (" + c.pos + ")!");
		return null;
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
	 * @return type
	 */
	@Nullable
	public T parseT(@Nullable final String s, @Nonnull final Cursor c, final Object context) {
		if (s == null || s.length() <= c.pos) {
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
			if (t == null) {
				log.warn(context + ": Cannot read class type in '" + s + "' (" + c.pos + ")!");
				return null;
			}
			if (s.length() <= c.pos || s.charAt(c.pos) != ';') {
				log.warn(context + ": Cannot read class type ';' in '" + s + "' (" + c.pos + ")!");
				return null;
			}
			++c.pos;
			return t;
		}
		case '[': {
			// ArrayTypeSignature
			final T t = parseT(s, c, context);
			if (t == null) {
				log.warn(context + ": Cannot read array component type in '" + s + "' (" + c.pos
						+ ")!");
				return null;
			}
			return getArrayT(t);
		}
		case 'T': {
			final int pos = s.indexOf(';', c.pos);
			final String tName = s.substring(c.pos, pos);
			assert tName != null;
			final T t = getVarT(tName, context);
			c.pos = pos + 1;
			return t;
		}
		default:
			log.warn(context + ": Unknown type kind in '" + s + "' (" + c.pos + ")!");
			return null;
		}
	}

	/**
	 * Parse type arguments from signature.
	 *
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @param context
	 *            enclosing type context
	 * @return type arguments
	 */
	@Nullable
	private T[] parseTypeArgs(@Nullable final String s, @Nonnull final Cursor c,
			final Object context) {
		// TypeArguments_opt
		if (s == null || s.length() <= c.pos || s.charAt(c.pos) != '<') {
			return null;
		}
		++c.pos;
		final List<T> ts = Lists.newArrayList();
		while (s.length() > c.pos) {
			final char ch = s.charAt(c.pos);
			if (ch == '>') {
				++c.pos;
				if (ts.size() == 0) {
					return null;
				}
				return ts.toArray(new T[ts.size()]);
			}
			switch (ch) {
			case '*':
				++c.pos;
				ts.add(getMatchesWildcardT());
				break;
			case '+': {
				++c.pos;
				final T t = parseT(s, c, context);
				if (t == null) {
					log.warn(context + ": Cannot read + type arg in '" + s + "' (" + c.pos + ")!");
					return null;
				}
				ts.add(getSubclassOfWildcardT(t));
				break;
			}
			case '-': {
				++c.pos;
				final T t = parseT(s, c, context);
				if (t == null) {
					log.warn(context + ": Cannot read - type arg in '" + s + "' (" + c.pos + ")!");
					return null;
				}
				ts.add(getSuperOfWildcardT(t));
				break;
			}
			default: {
				final T t = parseT(s, c, context);
				if (t == null) {
					log.warn(context + ": Cannot read type arg in '" + s + "' (" + c.pos + ")!");
					return null;
				}
				ts.add(t);
			}
			}
		}
		log.warn(context + ": Cannot read type args in '" + s + "' (" + c.pos + ")!");
		return null;
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
	 * @return type parameters
	 */
	@Nullable
	public T[] parseTypeParams(@Nullable final String s, @Nonnull final Cursor c,
			final Object context) {
		// TypeParams_opt
		if (s == null || s.length() <= c.pos || s.charAt(c.pos) != '<') {
			return null; // optional
		}
		++c.pos;
		final List<T> ts = Lists.newArrayList();
		while (s.length() > c.pos) {
			if (s.charAt(c.pos) == '>') {
				++c.pos;
				if (ts.size() == 0) {
					return null;
				}
				return ts.toArray(new T[ts.size()]);
			}
			final int pos = s.indexOf(':', c.pos);
			// reuse ClassT for type parameter
			final String typeParamName = s.substring(c.pos, pos);
			assert typeParamName != null : s;
			final ParamT typeParam = new ParamT(this, typeParamName);
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
				final T[] interfaceTsArray = interfaceTs.toArray(new T[interfaceTs.size()]);
				assert interfaceTsArray != null : s;
				typeParam.setInterfaceTs(interfaceTsArray);
			}
			ts.add(typeParam);
		}
		log.warn(context + ": Cannot read type params in '" + s + "' (" + c.pos + ")!");
		return null;
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
	@Nullable
	private List<T> read(final File file, final String selector) {
		final String fileName = file.getName();
		if (fileName.endsWith(".class")) {
			final List<T> ts = Lists.newArrayList();

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
					final List<T> readTds = read(is, name, fileName);
					if (readTds != null) {
						ts.addAll(readTds);
					}
				} catch (final Throwable e) {
					log.warn("Couldn't read file '" + name + "'!", e);
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
			return ts;
		}
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			return read(fileInputStream, fileName, selector);
		} catch (final IOException e) {
			log.warn("Couldn't read file '" + file + "'!");
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
	@Nullable
	public List<T> read(final InputStream is, final String fileName, final String selector)
			throws IOException {
		final byte[] magicNumber = new byte[MagicNumbers.LENGTH];
		final int read = is.read(magicNumber);
		if (read < magicNumber.length) {
			return null;
		}
		if (Arrays.equals(magicNumber, MagicNumbers.CLASS)) {
			final PushbackInputStream pis = new PushbackInputStream(is, magicNumber.length);
			pis.unread(magicNumber);
			// selector has no meaning here
			final T t = this.classReader.read(pis);
			if (t != null && (selector == null || fileName.equals(selector))) {
				this.selectedTs.add(t);
				return Collections.singletonList(t);
			}
			return Collections.emptyList();
		} else if (fileName.endsWith(".class")) {
			log.warn("Wrong magic number for file '" + fileName + "', isn't a JVM-Class!");
		}
		if (Arrays.equals(magicNumber, MagicNumbers.DEX)
				|| Arrays.equals(magicNumber, MagicNumbers.ODEX)) {
			final PushbackInputStream pis = new PushbackInputStream(is, magicNumber.length);
			pis.unread(magicNumber);
			final List<T> ts = this.dexReader.read(pis, selector);
			this.selectedTs.addAll(ts);
			return ts;
		} else if (fileName.endsWith(".dex") || fileName.endsWith(".odex")) {
			log.warn("Wrong magic number for file '" + fileName + "', isn't a Dalvik-Class!");
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
			final List<T> ts = Lists.newArrayList();

			final PushbackInputStream pis = new PushbackInputStream(is, magicNumber.length);
			pis.unread(magicNumber);
			final ZipInputStream zip = new ZipInputStream(pis);
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				final String name = zipEntry.getName();
				// load full type declarations from complete package, to complex to decide here if
				// really not part of the compilation unit
				// TODO later load all type declarations, but not all bytecode details
				if (name.endsWith(".class") && selectorPrefix != null
						&& (!name.startsWith(selectorPrefix)
								|| name.indexOf('/', selectorPrefix.length()) != -1)) {
					continue;
				}
				try {
					// nested ZipStreams have bugs and skip some entries, hence copy the stream
					final byte[] buf = ByteStreams.toByteArray(zip);
					final List<T> readTds = read(new ByteArrayInputStream(buf), name, null);
					if (readTds != null && (selectorMatch == null || selectorMatch.equals(name))) {
						ts.addAll(readTds);
					}
				} catch (final Exception e) {
					log.warn("Couldn't read '" + name + "'!", e);
				}
			}
			return ts;
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
	public List<T> read(final String fileName) {
		final int pos = fileName.indexOf('!');
		if (pos == -1) {
			return read(new File(fileName), null);
		}
		// ...\jdk1.6.0_26\jre\lib\rt.jar!/com/sun/xml/internal/fastinfoset/Decoder.class
		return read(new File(fileName.substring(0, pos)), fileName.substring(pos + 1));
	}

}