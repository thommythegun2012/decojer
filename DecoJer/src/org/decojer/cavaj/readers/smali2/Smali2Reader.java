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
package org.decojer.cavaj.readers.smali2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.DexReader;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.iface.value.BooleanEncodedValue;
import org.jf.dexlib2.iface.value.ByteEncodedValue;
import org.jf.dexlib2.iface.value.CharEncodedValue;
import org.jf.dexlib2.iface.value.DoubleEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.EnumEncodedValue;
import org.jf.dexlib2.iface.value.FieldEncodedValue;
import org.jf.dexlib2.iface.value.FloatEncodedValue;
import org.jf.dexlib2.iface.value.IntEncodedValue;
import org.jf.dexlib2.iface.value.LongEncodedValue;
import org.jf.dexlib2.iface.value.MethodEncodedValue;
import org.jf.dexlib2.iface.value.ShortEncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;
import org.jf.dexlib2.iface.value.TypeEncodedValue;
import org.jf.util.ExceptionWithContext;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

/**
 * Reader from Smali.
 *
 * @author André Pankraz
 */
@Slf4j
public class Smali2Reader implements DexReader {

	/**
	 * Build method descriptor of method reference.
	 *
	 * @param method
	 *            method reference
	 * @return method descriptor
	 */
	public static String desc(final MethodReference method) {
		final StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (final CharSequence cs : method.getParameterTypes()) {
			sb.append(cs);
		}
		sb.append(')');
		sb.append(method.getReturnType());
		return sb.toString();
	}

	@Nonnull
	private final DU du;

	private final ReadMethodImplementation readMethodImplementation = new ReadMethodImplementation();

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public Smali2Reader(@Nonnull final DU du) {
		this.du = du;
	}

	@Override
	public List<T> read(final InputStream is, final String selector) throws IOException {
		String selectorPrefix = null;
		String selectorMatch = null;
		if (selector != null && selector.endsWith(".class")) {
			selectorMatch = "L"
					+ selector.substring(selector.charAt(0) == '/' ? 1 : 0, selector.length() - 6)
					+ ";";
			final int pos = selectorMatch.lastIndexOf('/');
			if (pos != -1) {
				selectorPrefix = selectorMatch.substring(0, pos + 1);
			}
		}
		final List<T> ts = Lists.newArrayList();

		@SuppressWarnings("null")
		final byte[] bytes = ByteStreams.toByteArray(is);
		final DexBackedDexFile dexFile = new DexBackedDexFile(new Opcodes(15), bytes);
		for (final DexBackedClassDef classDefItem : dexFile.getClasses()) {
			final String typeDescriptor = classDefItem.getType();
			// load full type declarations from complete package, to complex to decide here if
			// really not part of the compilation unit
			// TODO later load all type declarations, but not all bytecode details
			if (selectorPrefix != null
					&& (!typeDescriptor.startsWith(selectorPrefix) || typeDescriptor.indexOf('/',
							selectorPrefix.length()) != -1)) {
				continue;
			}
			final T t = this.du.getDescT(typeDescriptor);
			if (t == null) {
				log.warn("Cannot read type descriptor '" + typeDescriptor + "'!");
				continue;
			}
			if (!t.createTd()) {
				log.warn("Type '" + t + "' already read!");
				continue;
			}
			t.setAccessFlags(classDefItem.getAccessFlags());
			t.setSuperT(this.du.getDescT(classDefItem.getSuperclass()));
			final Set<String> interfaces = classDefItem.getInterfaces();
			if (!interfaces.isEmpty()) {
				final T[] interfaceTs = new T[interfaces.size()];
				int i = 0;
				for (final String interfaceDesc : interfaces) {
					interfaceTs[i++] = this.du.getDescT(interfaceDesc);
				}
				t.setInterfaceTs(interfaceTs);
			}
			if (selectorMatch == null || selectorMatch.equals(typeDescriptor)) {
				ts.add(t);
			}
			A annotationDefaultValues = null;
			final Set<? extends DexBackedAnnotation> annotations = classDefItem.getAnnotations();
			if (!annotations.isEmpty()) {
				final List<A> as = Lists.newArrayList();
				for (final DexBackedAnnotation annotation : annotations) {
					final A a = readAnnotation(t, annotation);
					if ("dalvik.annotation.AnnotationDefault".equals(a.getT().getName())) {
						// annotation default values, not encoded in
						// method annotations, but in global annotation with
						// "field name" -> value
						annotationDefaultValues = (A) a.getValueMember();
						continue;
					}
					if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
						// signature, is encoded as annotation with string array value
						final Object[] signature = (Object[]) a.getValueMember();
						final StringBuilder sb = new StringBuilder();
						for (final Object element : signature) {
							sb.append(element);
						}
						t.setSignature(sb.toString());
						continue;
					}
					if ("dalvik.annotation.EnclosingClass".equals(a.getT().getName())) {
						t.setEnclosingT((ClassT) a.getValueMember());
						continue;
					}
					if ("dalvik.annotation.EnclosingMethod".equals(a.getT().getName())) {
						t.setEnclosingM((M) a.getValueMember());
						continue;
					}
					if ("dalvik.annotation.InnerClass".equals(a.getT().getName())) {
						t.setInnerInfo((String) a.getMember("name"),
								(Integer) a.getMember("accessFlags"));
						continue;
					}
					if ("dalvik.annotation.MemberClasses".equals(a.getT().getName())) {
						for (final Object v : (Object[]) a.getValueMember()) {
							((T) v).setEnclosingT(t);
						}
						continue;
					}
					as.add(a);
				}
				if (as.size() > 0) {
					t.setAs(as.toArray(new A[as.size()]));
				}
			}
			if (classDefItem.getSourceFile() != null) {
				t.setSourceFileName(classDefItem.getSourceFile());
			}
			readFields(t, classDefItem.getStaticFields(), classDefItem.getInstanceFields());
			readMethods(t, classDefItem.getDirectMethods(), classDefItem.getVirtualMethods(),
					annotationDefaultValues);
			t.resolve();
		}
		return ts;
	}

	private A readAnnotation(@Nonnull final Object context, final Annotation annotation) {
		RetentionPolicy retentionPolicy;
		switch (annotation.getVisibility()) {
		case AnnotationVisibility.BUILD:
			retentionPolicy = RetentionPolicy.SOURCE;
			break;
		case AnnotationVisibility.RUNTIME:
			retentionPolicy = RetentionPolicy.RUNTIME;
			break;
		case AnnotationVisibility.SYSTEM:
			retentionPolicy = RetentionPolicy.CLASS;
			break;
		default:
			retentionPolicy = null;
			log.warn("Unknown annotation visibility '" + annotation.getVisibility() + "'!");
		}
		return readAnnotation(context, annotation.getType(), annotation.getElements(),
				retentionPolicy);
	}

	@Nullable
	private A readAnnotation(@Nonnull final Object context, final String type,
			final Set<? extends AnnotationElement> elements, final RetentionPolicy retentionPolicy) {
		final T aT = this.du.getDescT(type);
		if (aT == null) {
			log.warn(context + ": Cannot read annotation type descriptor '" + type + "'!");
			return null;
		}
		final A a = new A(aT, retentionPolicy);
		for (final AnnotationElement element : elements) {
			a.addMember(element.getName(), readValue(context, element.getValue(), this.du));
		}
		return a;
	}

	private void readField(final T t, final DexBackedField field) {
		final F f = t.getF(field.getName(), field.getType());
		f.createFd();
		f.setAccessFlags(field.getAccessFlags());

		final Set<? extends Annotation> annotations = field.getAnnotations();
		if (!annotations.isEmpty()) {
			final List<A> as = Lists.newArrayList();
			for (final Annotation annotation : annotations) {
				final A a = readAnnotation(f, annotation);
				if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
					// signature, is encoded as annotation
					// with string array value
					final Object[] signatures = (Object[]) a.getValueMember();
					final StringBuilder sb = new StringBuilder();
					for (final Object element : signatures) {
						sb.append(element);
					}
					f.setSignature(sb.toString());
					continue;
				}
				as.add(a);
			}
			if (!as.isEmpty()) {
				f.setAs(as.toArray(new A[as.size()]));
			}
		}
		if (field.getInitialValue() != null) {
			f.setValue(readValue(f, field.getInitialValue(), this.du));
		}
	}

	private void readFields(final T t, final Iterable<? extends DexBackedField> staticFields,
			final Iterable<? extends DexBackedField> instanceFields) {
		for (final DexBackedField field : staticFields) {
			readField(t, field);
		}
		for (final DexBackedField field : instanceFields) {
			readField(t, field);
		}
	}

	private void readMethod(final T t, final DexBackedMethod method, final A annotationDefaultValues) {
		final M m = t.getM(method.getName(), desc(method));
		m.createMd();
		m.setAccessFlags(method.getAccessFlags());

		final Set<? extends Annotation> annotations = method.getAnnotations();
		if (!annotations.isEmpty()) {
			final List<A> as = Lists.newArrayList();
			// remember values, because we must set throws before signature
			T[] throwsTs = null;
			String signature = null;
			for (final Annotation annotation : annotations) {
				final A a = readAnnotation(m, annotation);
				if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
					// signature, is encoded as annotation
					// with string array value
					final Object[] signatures = (Object[]) a.getValueMember();
					final StringBuilder sb = new StringBuilder();
					for (final Object element : signatures) {
						sb.append(element);
					}
					signature = sb.toString();
					continue;
				}
				if ("dalvik.annotation.Throws".equals(a.getT().getName())) {
					// throws, is encoded as annotation with type array value
					final Object[] throwables = (Object[]) a.getValueMember();
					throwsTs = new T[throwables.length];
					for (int i = throwables.length; i-- > 0;) {
						throwsTs[i] = (T) throwables[i];
					}
					continue;
				}
				as.add(a);
			}
			m.setThrowsTs(throwsTs);
			m.setSignature(signature);
			if (!as.isEmpty()) {
				m.setAs(as.toArray(new A[as.size()]));
			}
		}
		if (annotationDefaultValues != null) {
			m.setAnnotationDefaultValue(annotationDefaultValues.getMember(m.getName()));
		}
		final List<? extends Set<? extends DexBackedAnnotation>> parameterAnnotations = method
				.getParameterAnnotations();
		if (!parameterAnnotations.isEmpty()) {
			final A[][] paramAss = new A[parameterAnnotations.size()][];
			for (int i = paramAss.length; i-- > 0;) {
				final Set<? extends DexBackedAnnotation> iParameterAnnotations = parameterAnnotations
						.get(i);
				final A[] paramAs = paramAss[i] = new A[iParameterAnnotations.size()];
				int j = 0;
				for (final Annotation annotation : iParameterAnnotations) {
					paramAs[j++] = readAnnotation(m, annotation);
				}
			}
			m.setParamAss(paramAss);
		}
		try {
			this.readMethodImplementation.initAndVisit(m, method.getImplementation());
		} catch (final ExceptionWithContext e) {
			log.warn("Bytecode problems in method '" + m + "'! " + e.getMessage());
		}
	}

	private void readMethods(final T t, final Iterable<? extends DexBackedMethod> directMethods,
			final Iterable<? extends DexBackedMethod> virtualMethods,
			final A annotationDefaultValues) {
		for (final DexBackedMethod method : directMethods) {
			readMethod(t, method, annotationDefaultValues);
		}
		for (final DexBackedMethod method : virtualMethods) {
			readMethod(t, method, annotationDefaultValues);
		}
	}

	@Nullable
	private Object readValue(@Nonnull final Object context, final EncodedValue encodedValue,
			final DU du) {
		switch (encodedValue.getValueType()) {
		case ValueType.ANNOTATION:
			// retention unknown for annotation constant
			return readAnnotation(context, ((AnnotationEncodedValue) encodedValue).getType(),
					((AnnotationEncodedValue) encodedValue).getElements(), null);
		case ValueType.ARRAY: {
			final List<? extends EncodedValue> values = ((ArrayEncodedValue) encodedValue)
					.getValue();
			final Object[] objects = new Object[values.size()];
			for (int i = values.size(); i-- > 0;) {
				objects[i] = readValue(context, values.get(i), du);
			}
			return objects;
		}
		case ValueType.BOOLEAN:
			return ((BooleanEncodedValue) encodedValue).getValue();
		case ValueType.BYTE:
			return ((ByteEncodedValue) encodedValue).getValue();
		case ValueType.CHAR:
			return ((CharEncodedValue) encodedValue).getValue();
		case ValueType.DOUBLE:
			return ((DoubleEncodedValue) encodedValue).getValue();
		case ValueType.ENUM: {
			final FieldReference fieldReference = ((EnumEncodedValue) encodedValue).getValue();
			final T ownerT = du.getDescT(fieldReference.getDefiningClass());
			final F f = ownerT.getF(fieldReference.getName(), fieldReference.getType());
			f.setEnum();
			return f;
		}
		case ValueType.FIELD: {
			final FieldReference fieldReference = ((FieldEncodedValue) encodedValue).getValue();
			final T t = du.getDescT(fieldReference.getDefiningClass());
			return t.getF(fieldReference.getName(), fieldReference.getType());
		}
		case ValueType.FLOAT:
			return ((FloatEncodedValue) encodedValue).getValue();
		case ValueType.INT:
			return ((IntEncodedValue) encodedValue).getValue();
		case ValueType.LONG:
			return ((LongEncodedValue) encodedValue).getValue();
		case ValueType.METHOD: {
			final MethodReference methodReference = ((MethodEncodedValue) encodedValue).getValue();
			final T ownerT = du.getDescT(methodReference.getDefiningClass());
			return ownerT.getM(methodReference.getName(), desc(methodReference));
		}
		case ValueType.NULL:
			return null; // placeholder in constant array
		case ValueType.SHORT:
			return ((ShortEncodedValue) encodedValue).getValue();
		case ValueType.STRING:
			return ((StringEncodedValue) encodedValue).getValue();
		case ValueType.TYPE:
			return du.getDescT(((TypeEncodedValue) encodedValue).getValue());
		default:
			log.warn(context + ": Unknown encoded value type '" + encodedValue.getClass().getName()
					+ "'!");
			return null;
		}
	}

}