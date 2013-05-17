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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.types.ClassT;
import org.jf.dexlib.AnnotationDirectoryItem;
import org.jf.dexlib.AnnotationDirectoryItem.FieldAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.MethodAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.ParameterAnnotation;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

/**
 * Reader from Smali.
 * 
 * @author André Pankraz
 */
public class Smali2Reader implements DexReader {

	private final static Logger LOGGER = Logger.getLogger(Smali2Reader.class.getName());

	public static void main(final String[] args) {
		try {
			new Smali2Reader(new DU()).read(new FileInputStream(
					"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/classes_130106.dex"), "");
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private final DU du;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public Smali2Reader(final DU du) {
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
		final List<TD> tds = Lists.newArrayList();

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
			final ClassT t = (ClassT) this.du.getDescT(typeDescriptor);
			if (t.getTd() != null) {
				LOGGER.warning("Type '" + t + "' already read!");
				continue;
			}
			final TD td = t.createTd();
			td.setAccessFlags(classDefItem.getAccessFlags());
			td.setSuperT(this.du.getDescT(classDefItem.getSuperclass()));
			final Set<String> interfaces = classDefItem.getInterfaces();
			if (!interfaces.isEmpty()) {
				final T[] interfaceTs = new T[interfaces.size()];
				for (int i = interfaces.size(); i-- > 0;) {
					interfaceTs[i] = this.du.getDescT(interfaces.getTypeIdItem(i)
							.getTypeDescriptor());
				}
				td.setInterfaceTs(interfaceTs);
			}
			if (selectorMatch == null || selectorMatch.equals(typeDescriptor)) {
				tds.add(td);
			}
			A annotationDefaultValues = null;
			final Map<FieldIdItem, String> fieldSignatures = Maps.newHashMap();
			final Map<FieldIdItem, A[]> fieldAs = Maps.newHashMap();
			final Map<MethodIdItem, T[]> methodThrowsTs = Maps.newHashMap();
			final Map<MethodIdItem, String> methodSignatures = Maps.newHashMap();
			final Map<MethodIdItem, A[]> methodAs = Maps.newHashMap();
			final Map<MethodIdItem, A[][]> methodParamAs = Maps.newHashMap();

			final AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
			if (annotations != null) {
				final AnnotationSetItem classAnnotations = annotations.getClassAnnotations();
				if (classAnnotations != null) {
					final List<A> as = Lists.newArrayList();
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
					final List<A> as = Lists.newArrayList();
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
					final List<A> as = Lists.newArrayList();
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

}