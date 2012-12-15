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
package org.decojer.cavaj.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.readers.smali.ReadCodeItem;
import org.jf.dexlib.AnnotationDirectoryItem;
import org.jf.dexlib.AnnotationDirectoryItem.FieldAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.MethodAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.ParameterAnnotation;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.EncodedArrayItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;
import org.jf.dexlib.EncodedValue.AnnotationEncodedValue;
import org.jf.dexlib.EncodedValue.ArrayEncodedValue;
import org.jf.dexlib.EncodedValue.BooleanEncodedValue;
import org.jf.dexlib.EncodedValue.ByteEncodedValue;
import org.jf.dexlib.EncodedValue.CharEncodedValue;
import org.jf.dexlib.EncodedValue.DoubleEncodedValue;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.EnumEncodedValue;
import org.jf.dexlib.EncodedValue.FloatEncodedValue;
import org.jf.dexlib.EncodedValue.IntEncodedValue;
import org.jf.dexlib.EncodedValue.LongEncodedValue;
import org.jf.dexlib.EncodedValue.MethodEncodedValue;
import org.jf.dexlib.EncodedValue.NullEncodedValue;
import org.jf.dexlib.EncodedValue.ShortEncodedValue;
import org.jf.dexlib.EncodedValue.StringEncodedValue;
import org.jf.dexlib.EncodedValue.TypeEncodedValue;
import org.jf.dexlib.Util.ByteArrayInput;

import com.google.common.io.ByteStreams;

/**
 * Reader from Smali.
 * 
 * @author André Pankraz
 */
public class SmaliReader implements DexReader {

	private final static Logger LOGGER = Logger.getLogger(SmaliReader.class.getName());

	private final DU du;

	private final ReadCodeItem readCodeItem = new ReadCodeItem();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public SmaliReader(final DU du) {
		assert du != null;

		this.du = du;
	}

	@Override
	public List<TD> read(final InputStream is, final String selector) throws IOException {
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
		final List<TD> tds = new ArrayList<TD>();

		final byte[] bytes = ByteStreams.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), true, false);
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			final String typeDescriptor = classDefItem.getClassType().getTypeDescriptor();
			// load full type declarations from complete package, to complex to decide here if
			// really not part of the compilation unit
			// TODO later load all type declarations, but not all bytecode details
			if (selectorPrefix != null
					&& (!typeDescriptor.startsWith(selectorPrefix) || typeDescriptor.indexOf('/',
							selectorPrefix.length()) != -1)) {
				continue;
			}
			final ClassT t = (ClassT) this.du.getDescT(typeDescriptor);
			final TD td = t.createTd();
			td.setAccessFlags(classDefItem.getAccessFlags());
			td.setSuperT(this.du.getDescT(classDefItem.getSuperclass().getTypeDescriptor()));
			final TypeListItem interfaces = classDefItem.getInterfaces();
			if (interfaces != null && interfaces.getTypeCount() > 0) {
				final T[] interfaceTs = new T[interfaces.getTypeCount()];
				for (int i = interfaces.getTypeCount(); i-- > 0;) {
					interfaceTs[i] = this.du.getDescT(interfaces.getTypeIdItem(i)
							.getTypeDescriptor());
				}
				td.setInterfaceTs(interfaceTs);
			}
			if (selectorMatch == null || selectorMatch.equals(typeDescriptor)) {
				tds.add(td);
			}
			A annotationDefaultValues = null;
			final Map<FieldIdItem, String> fieldSignatures = new HashMap<FieldIdItem, String>();
			final Map<FieldIdItem, A[]> fieldAs = new HashMap<FieldIdItem, A[]>();
			final Map<MethodIdItem, T[]> methodThrowsTs = new HashMap<MethodIdItem, T[]>();
			final Map<MethodIdItem, String> methodSignatures = new HashMap<MethodIdItem, String>();
			final Map<MethodIdItem, A[]> methodAs = new HashMap<MethodIdItem, A[]>();
			final Map<MethodIdItem, A[][]> methodParamAs = new HashMap<MethodIdItem, A[][]>();

			final AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
			if (annotations != null) {
				final AnnotationSetItem classAnnotations = annotations.getClassAnnotations();
				if (classAnnotations != null) {
					final List<A> as = new ArrayList<A>();
					for (final AnnotationItem annotation : classAnnotations.getAnnotations()) {
						final A a = readAnnotation(annotation);
						if ("dalvik.annotation.AnnotationDefault".equals(a.getT().getName())) {
							// annotation default values, not encoded in
							// method annotations, but in global annotation with
							// "field name" -> value
							annotationDefaultValues = (A) a.getMemberValue();
							continue;
						}
						if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
							// signature, is encoded as annotation with string array value
							final Object[] signature = (Object[]) a.getMemberValue();
							final StringBuilder sb = new StringBuilder();
							for (final Object element : signature) {
								sb.append(element);
							}
							td.setSignature(sb.toString());
							continue;
						}
						if ("dalvik.annotation.EnclosingClass".equals(a.getT().getName())) {
							t.setEnclosingT((ClassT) a.getMemberValue());
							continue;
						}
						if ("dalvik.annotation.EnclosingMethod".equals(a.getT().getName())) {
							t.setEnclosingM((M) a.getMemberValue());
							continue;
						}
						if ("dalvik.annotation.InnerClass".equals(a.getT().getName())) {
							t.setInnerInfo((String) a.getMemberValue("name"),
									(Integer) a.getMemberValue("accessFlags"));
							continue;
						}
						if ("dalvik.annotation.MemberClasses".equals(a.getT().getName())) {
							for (final Object v : (Object[]) a.getMemberValue()) {
								((ClassT) v).setEnclosingT(td.getT());
							}
							continue;
						}
						as.add(a);
					}
					if (as.size() > 0) {
						td.setAs(as.toArray(new A[as.size()]));
					}
				}
				for (final FieldAnnotation fieldAnnotation : annotations.getFieldAnnotations()) {
					final List<A> as = new ArrayList<A>();
					for (final AnnotationItem annotationItem : fieldAnnotation.annotationSet
							.getAnnotations()) {
						final A a = readAnnotation(annotationItem);
						if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
							// signature, is encoded as annotation
							// with string array value
							final Object[] signature = (Object[]) a.getMemberValue();
							final StringBuilder sb = new StringBuilder();
							for (final Object element : signature) {
								sb.append(element);
							}
							fieldSignatures.put(fieldAnnotation.field, sb.toString());
							continue;
						}
						as.add(a);
					}
					if (as.size() > 0) {
						fieldAs.put(fieldAnnotation.field, as.toArray(new A[as.size()]));
					}
				}
				for (final MethodAnnotation methodAnnotation : annotations.getMethodAnnotations()) {
					final List<A> as = new ArrayList<A>();
					for (final AnnotationItem annotationItem : methodAnnotation.annotationSet
							.getAnnotations()) {
						final A a = readAnnotation(annotationItem);
						if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
							// signature, is encoded as annotation
							// with string array value
							final Object[] signature = (Object[]) a.getMemberValue();
							final StringBuilder sb = new StringBuilder();
							for (final Object element : signature) {
								sb.append(element);
							}
							methodSignatures.put(methodAnnotation.method, sb.toString());
							continue;
						} else if ("dalvik.annotation.Throws".equals(a.getT().getName())) {
							// throws, is encoded as annotation with
							// type array value
							final Object[] throwables = (Object[]) a.getMemberValue();
							final T[] throwsTs = new T[throwables.length];
							for (int i = throwables.length; i-- > 0;) {
								throwsTs[i] = (T) throwables[i];
							}
							methodThrowsTs.put(methodAnnotation.method, throwsTs);
							continue;
						} else {
							as.add(a);
						}
					}
					if (as.size() > 0) {
						methodAs.put(methodAnnotation.method, as.toArray(new A[as.size()]));
					}
				}
				for (final ParameterAnnotation paramAnnotation : annotations
						.getParameterAnnotations()) {
					final AnnotationSetItem[] annotationSets = paramAnnotation.annotationSet
							.getAnnotationSets();
					final A[][] paramAss = new A[annotationSets.length][];
					for (int i = annotationSets.length; i-- > 0;) {
						final AnnotationItem[] annotationItems = annotationSets[i].getAnnotations();
						final A[] paramAs = paramAss[i] = new A[annotationItems.length];
						for (int j = annotationItems.length; j-- > 0;) {
							paramAs[j] = readAnnotation(annotationItems[j]);
						}
					}
					methodParamAs.put(paramAnnotation.method, paramAss);
				}
			}
			if (classDefItem.getSourceFile() != null) {
				td.setSourceFileName(classDefItem.getSourceFile().getStringValue());
			}
			final ClassDataItem classData = classDefItem.getClassData();
			if (classData != null) {
				readFields(td, classData.getStaticFields(), classData.getInstanceFields(),
						fieldSignatures, classDefItem.getStaticFieldInitializers(), fieldAs);
				readMethods(td, classData.getDirectMethods(), classData.getVirtualMethods(),
						methodSignatures, methodThrowsTs, annotationDefaultValues, methodAs,
						methodParamAs);
			}
			td.resolve();
		}
		return tds;
	}

	private A readAnnotation(final AnnotationEncodedSubValue encodedValue,
			final RetentionPolicy retentionPolicy) {
		final T t = this.du.getDescT(encodedValue.annotationType.getTypeDescriptor());
		final A a = new A(t, retentionPolicy);
		final StringIdItem[] names = encodedValue.names;
		final EncodedValue[] values = encodedValue.values;
		for (int i = 0; i < names.length; ++i) {
			a.addMember(names[i].getStringValue(), readValue(values[i], this.du));
		}
		return a;
	}

	private A readAnnotation(final AnnotationItem annotationItem) {
		RetentionPolicy retentionPolicy;
		switch (annotationItem.getVisibility()) {
		case BUILD:
			retentionPolicy = RetentionPolicy.SOURCE;
			break;
		case RUNTIME:
			retentionPolicy = RetentionPolicy.RUNTIME;
			break;
		case SYSTEM:
			retentionPolicy = RetentionPolicy.CLASS;
			break;
		default:
			retentionPolicy = null;
			LOGGER.warning("Unknown annotation visibility '"
					+ annotationItem.getVisibility().visibility + "'!");
		}
		return readAnnotation(annotationItem.getEncodedAnnotation(), retentionPolicy);
	}

	private void readFields(final TD td, final List<EncodedField> staticFields,
			final List<EncodedField> instanceFields,
			final Map<FieldIdItem, String> fieldSignatures,
			final EncodedArrayItem staticFieldInitializers, final Map<FieldIdItem, A[]> fieldAs) {
		// static field initializer values are packed away into a different
		// section, both arrays (encoded fields and static field values) are
		// sorted in same order, there could be less static field values if
		// not all static fields have an initializer, but there is also a
		// null value as placeholder
		final EncodedValue[] staticFieldValues = staticFieldInitializers == null ? new EncodedValue[0]
				: staticFieldInitializers.getEncodedArray().values;

		for (int i = 0; i < staticFields.size(); ++i) {
			final EncodedField encodedField = staticFields.get(i);
			final FieldIdItem field = encodedField.field;

			final FD fd = td.createFd(field.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor());

			fd.setAccessFlags(encodedField.accessFlags);
			if (fieldSignatures.get(field) != null) {
				fd.setSignature(fieldSignatures.get(field));
			}

			if (staticFieldValues.length > i) {
				fd.setValue(readValue(staticFieldValues[i], this.du));
			}

			fd.setAs(fieldAs.get(field));
		}
		for (final EncodedField encodedField : instanceFields) {
			final FieldIdItem field = encodedField.field;

			final FD fd = td.createFd(field.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor());

			fd.setAccessFlags(encodedField.accessFlags);
			if (fieldSignatures.get(field) != null) {
				fd.setSignature(fieldSignatures.get(field));
			}

			// there is no field initializer section for instance fields,
			// only via constructor

			fd.setAs(fieldAs.get(field));
		}
	}

	private void readMethods(final TD td, final List<EncodedMethod> directMethods,
			final List<EncodedMethod> virtualMethods,
			final Map<MethodIdItem, String> methodSignatures,
			final Map<MethodIdItem, T[]> methodThrowsTs, final A annotationDefaultValues,
			final Map<MethodIdItem, A[]> methodAs, final Map<MethodIdItem, A[][]> methodParamAs) {
		for (final EncodedMethod encodedMethod : directMethods) {
			final MethodIdItem method = encodedMethod.method;

			final MD md = td.createMd(method.getMethodName().getStringValue(), method
					.getPrototype().getPrototypeString());

			md.setAccessFlags(encodedMethod.accessFlags);
			md.setThrowsTs(methodThrowsTs.get(method));
			md.setSignature(methodSignatures.get(method));

			// no annotation default values

			md.setAs(methodAs.get(method));
			md.setParamAss(methodParamAs.get(method));

			if (encodedMethod.codeItem != null) {
				this.readCodeItem.initAndVisit(md, encodedMethod.codeItem);
			}
		}
		for (final EncodedMethod encodedMethod : virtualMethods) {
			final MethodIdItem method = encodedMethod.method;

			final MD md = td.createMd(method.getMethodName().getStringValue(), method
					.getPrototype().getPrototypeString());

			md.setAccessFlags(encodedMethod.accessFlags);
			md.setThrowsTs(methodThrowsTs.get(method));
			md.setSignature(methodSignatures.get(method));

			if (annotationDefaultValues != null) {
				md.setAnnotationDefaultValue(annotationDefaultValues.getMemberValue(md.getName()));
			}

			md.setAs(methodAs.get(method));
			md.setParamAss(methodParamAs.get(method));

			final CodeItem codeItem = encodedMethod.codeItem;
			if (codeItem != null) {
				this.readCodeItem.initAndVisit(md, encodedMethod.codeItem);
			}
		}
	}

	private Object readValue(final EncodedValue encodedValue, final DU du) {
		if (encodedValue instanceof AnnotationEncodedSubValue) {
			// retention unknown for annotation constant
			return readAnnotation((AnnotationEncodedValue) encodedValue, null);
		}
		if (encodedValue instanceof ArrayEncodedValue) {
			final EncodedValue[] values = ((ArrayEncodedValue) encodedValue).values;
			final Object[] objects = new Object[values.length];
			for (int i = values.length; i-- > 0;) {
				objects[i] = readValue(values[i], du);
			}
			return objects;
		}
		if (encodedValue instanceof BooleanEncodedValue) {
			return ((BooleanEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof ByteEncodedValue) {
			return ((ByteEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof CharEncodedValue) {
			return ((CharEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof DoubleEncodedValue) {
			return ((DoubleEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof EnumEncodedValue) {
			final FieldIdItem fieldidItem = ((EnumEncodedValue) encodedValue).value;
			final String desc = fieldidItem.getFieldType().getTypeDescriptor();
			final T ownerT = du.getDescT(desc);
			final F f = ownerT.getF(
					fieldidItem.getFieldName().getStringDataItem().getStringValue(), desc);
			f.setEnum();
			return f;
		}
		if (encodedValue instanceof FloatEncodedValue) {
			return ((FloatEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof IntEncodedValue) {
			return ((IntEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof LongEncodedValue) {
			return ((LongEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof MethodEncodedValue) {
			final MethodIdItem methodIdItem = ((MethodEncodedValue) encodedValue).value;
			final T t = du.getDescT(methodIdItem.getContainingClass().getTypeDescriptor());
			return t.getM(methodIdItem.getMethodName().getStringValue(), methodIdItem
					.getPrototype().getPrototypeString());
		}
		if (encodedValue instanceof NullEncodedValue) {
			return null; // placeholder in constant array
		}
		if (encodedValue instanceof ShortEncodedValue) {
			return ((ShortEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof StringEncodedValue) {
			return ((StringEncodedValue) encodedValue).value.getStringValue();
		}
		if (encodedValue instanceof TypeEncodedValue) {
			return du.getDescT(((TypeEncodedValue) encodedValue).value.getTypeDescriptor());
		}
		LOGGER.warning("Unknown encoded value type '" + encodedValue.getClass().getName() + "'!");
		return null;
	}

}