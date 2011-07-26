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
package org.decojer.cavaj.reader;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ConstantAttribute;
import javassist.bytecode.DeprecatedAttribute;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.InnerClassesAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SourceFileAttribute;
import javassist.bytecode.SyntheticAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.E;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;

/**
 * @author André Pankraz
 */
public class JavassistReader {

	private final static Logger LOGGER = Logger.getLogger(JavassistReader.class
			.getName());

	/**
	 * Analyse class input stream.
	 * 
	 * @param is
	 *            input stream
	 * @return type
	 * @throws IOException
	 *             problems
	 */
	public static Type analyse(final InputStream is) throws IOException {
		final ClassFile classFile = new ClassFile(new DataInputStream(is));
		final SignatureAttribute signatureAttribute = (SignatureAttribute) classFile
				.getAttribute(SignatureAttribute.tag);
		String signature;
		if (signatureAttribute == null) {
			final StringBuilder sb = new StringBuilder("L");
			sb.append(classFile.getSuperclass()).append(";");
			for (final String interfaceName : classFile.getInterfaces()) {
				sb.append("L").append(interfaceName).append(";");
			}
			signature = sb.toString();
		} else {
			signature = signatureAttribute.getSignature();
		}
		final Type type = new Type(classFile.getName(), signature);
		return type;
	}

	/**
	 * Analyse JAR input stream.
	 * 
	 * @param is
	 *            input stream
	 * @return types
	 * @throws IOException
	 *             problems
	 */
	public static Types analyseJar(final InputStream is) throws IOException {
		final ZipInputStream zip = new ZipInputStream(is);
		final Types types = new Types();
		int errors = 0;
		for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
				.getNextEntry()) {
			final String name = zipEntry.getName();
			if (!name.endsWith(".class")) {
				continue;
			}
			try {
				types.addType(analyse(zip));
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Couldn't analyse '" + name + "'!", e);
				++errors;
			}
		}
		types.setErrors(errors);
		return types;
	}

	@SuppressWarnings("unchecked")
	private static A createA(final Annotation annotation, final DU du) {
		final T t = du.getT(annotation.getTypeName());
		final A a = new A(t);
		if (annotation.getMemberNames() != null) {
			for (final String name : (Set<String>) annotation.getMemberNames()) {
				a.addParameter(name,
						readMemberValue(annotation.getMemberValue(name), du));
			}
		}
		return a;
	}

	/**
	 * Test it...
	 * 
	 * @param args
	 *            args
	 * @throws IOException
	 *             problems
	 */
	public static void main(final String[] args) throws IOException {
		final FileInputStream is = new FileInputStream(
				"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/org.eclipse.jdt.core_3.7.0.v_B61.jar");
		final Types types = analyseJar(is);
		System.out.println("Ana: " + types.getTypes().size());
	}

	/**
	 * Read class input stream.
	 * 
	 * @param is
	 *            class input stream
	 * @param du
	 *            decompilation unit
	 * @return type declaration
	 * @throws IOException
	 *             read exception
	 */
	@SuppressWarnings("unchecked")
	public static TD read(final InputStream is, final DU du) throws IOException {
		final ClassFile classFile = new ClassFile(new DataInputStream(is));

		// only annotations with RetentionPolicy.CLASS or RUNTIME are visible
		// here and can be decompiled, e.g. @SuppressWarnings not visible here,
		// has @Retention(RetentionPolicy.SOURCE)
		AnnotationsAttribute annotationsAttributeRuntimeInvisible = null;
		AnnotationsAttribute annotationsAttributeRuntimeVisible = null;
		DeprecatedAttribute deprecatedAttribute = null;
		// for local or inner classes the innermost enclosing class or method
		EnclosingMethodAttribute enclosingMethodAttribute = null;
		InnerClassesAttribute innerClassesAttribute = null;
		SignatureAttribute signatureAttribute = null;
		SourceFileAttribute sourceFileAttribute = null;
		SyntheticAttribute syntheticAttribute = null;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) classFile
				.getAttributes()) {
			final String attributeTag = attributeInfo.getName();
			if (AnnotationsAttribute.invisibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeInvisible = (AnnotationsAttribute) attributeInfo;
			} else if (AnnotationsAttribute.visibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeVisible = (AnnotationsAttribute) attributeInfo;
			} else if (DeprecatedAttribute.tag.equals(attributeTag)) {
				deprecatedAttribute = (DeprecatedAttribute) attributeInfo;
			} else if (EnclosingMethodAttribute.tag.equals(attributeTag)) {
				enclosingMethodAttribute = (EnclosingMethodAttribute) attributeInfo;
			} else if (InnerClassesAttribute.tag.equals(attributeTag)) {
				innerClassesAttribute = (InnerClassesAttribute) attributeInfo;
			} else if (SignatureAttribute.tag.equals(attributeTag)) {
				signatureAttribute = (SignatureAttribute) attributeInfo;
			} else if (SourceFileAttribute.tag.equals(attributeTag)) {
				sourceFileAttribute = (SourceFileAttribute) attributeInfo;
			} else if (SyntheticAttribute.tag.equals(attributeTag)) {
				syntheticAttribute = (SyntheticAttribute) attributeInfo;
			} else {
				LOGGER.log(Level.WARNING, "Unknown class attribute tag '"
						+ attributeTag + "'!");
			}
		}

		if (enclosingMethodAttribute != null) {
			// is anonymous class,
			// innermost enclosing class or method
			if (enclosingMethodAttribute.classIndex() > 0) {
				System.out.println("Enclosing Method Attribute: className: "
						+ enclosingMethodAttribute.className());
			}
			if (enclosingMethodAttribute.methodIndex() > 0) {
				System.out.println("Enclosing Method Attribute: methodName: "
						+ enclosingMethodAttribute.methodName()
						+ ", methodDescriptor: "
						+ enclosingMethodAttribute.methodDescriptor());
			}
		}
		if (innerClassesAttribute != null) {
			// is or has inner classes
			for (int i = 0; i < innerClassesAttribute.tableLength(); ++i) {
				// innerClassesAttribute.getName() is "InnerClasses"
				System.out.println("Inner Classes Attribute: accessFlags: "
						+ innerClassesAttribute.accessFlags(i)
						+ ", innerClass: "
						+ innerClassesAttribute.innerClass(i) + ", innerName: "
						+ innerClassesAttribute.innerName(i) + ", outerClass: "
						+ innerClassesAttribute.outerClass(i));
			}
		}

		final T t = du.getT(classFile.getName());
		t.setSuperT(du.getT(classFile.getSuperclass()));
		final String[] interfaces = classFile.getInterfaces();
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = du.getT(interfaces[i]);
			}
			t.setInterfaceTs(interfaceTs);
		}
		if (signatureAttribute != null) {
			t.setSignature(signatureAttribute.getSignature());
		}

		final TD td = new TD(t);

		// TODO move to T?
		td.setAccessFlags(classFile.getAccessFlags());
		td.setVersion(classFile.getMajorVersion());

		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible
					.getAnnotations();
			final A[] as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = createA(annotations[i], td.getT().getDu());
			}
			td.setInvisibleAs(as);
		}

		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible
					.getAnnotations();
			final A[] as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = createA(annotations[i], td.getT().getDu());
			}
			td.setVisibleAs(as);
		}

		if (deprecatedAttribute != null) {
			td.setDeprecated(true);
		}

		if (sourceFileAttribute != null) {
			td.setSourceFileName(sourceFileAttribute.getFileName());
		}

		if (syntheticAttribute != null) {
			td.setSynthetic(true);
		}

		for (final FieldInfo fieldInfo : (List<FieldInfo>) classFile
				.getFields()) {
			td.getBds().add(readFieldInfo(td, fieldInfo));
		}

		for (final MethodInfo methodInfo : (List<MethodInfo>) classFile
				.getMethods()) {
			td.getBds().add(readMethodInfo(td, methodInfo));
		}

		return td;
	}

	@SuppressWarnings("unchecked")
	private static FD readFieldInfo(final TD td, final FieldInfo fieldInfo) {
		AnnotationsAttribute annotationsAttributeRuntimeInvisible = null;
		AnnotationsAttribute annotationsAttributeRuntimeVisible = null;
		ConstantAttribute constantAttribute = null;
		DeprecatedAttribute deprecatedAttribute = null;
		SignatureAttribute signatureAttribute = null;
		SyntheticAttribute syntheticAttribute = null;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) fieldInfo
				.getAttributes()) {
			final String attributeTag = attributeInfo.getName();
			if (AnnotationsAttribute.invisibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeInvisible = (AnnotationsAttribute) attributeInfo;
			} else if (AnnotationsAttribute.visibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeVisible = (AnnotationsAttribute) attributeInfo;
			} else if (ConstantAttribute.tag.equals(attributeTag)) {
				constantAttribute = (ConstantAttribute) attributeInfo;
			} else if (DeprecatedAttribute.tag.equals(attributeTag)) {
				deprecatedAttribute = (DeprecatedAttribute) attributeInfo;
			} else if (SignatureAttribute.tag.equals(attributeTag)) {
				signatureAttribute = (SignatureAttribute) attributeInfo;
			} else if (SyntheticAttribute.tag.equals(attributeTag)) {
				syntheticAttribute = (SyntheticAttribute) attributeInfo;
			} else {
				LOGGER.log(Level.WARNING,
						"Unknown field attribute tag '" + attributeTag
								+ "' for field info '" + fieldInfo.getName()
								+ "'!");
			}
		}

		Object value = null;
		if (constantAttribute != null) {
			// only final, non static - no arrays, class types
			final int index = constantAttribute.getConstantValue();
			final ConstPool constPool = constantAttribute.getConstPool();
			final int tag = constPool.getTag(index);
			switch (tag) {
			case ConstPool.CONST_Double:
				value = constPool.getDoubleInfo(index);
				break;
			case ConstPool.CONST_Float:
				value = constPool.getFloatInfo(index);
				break;
			case ConstPool.CONST_Integer:
				// also: int, short, byte, char, boolean
				value = constPool.getIntegerInfo(index);
				break;
			case ConstPool.CONST_Long:
				value = constPool.getLongInfo(index);
				break;
			case ConstPool.CONST_String:
				value = constPool.getStringInfo(index);
				break;
			default:
				LOGGER.log(Level.WARNING, "Unknown constant attribute '" + tag
						+ "' for field info '" + fieldInfo.getName() + "'!");
			}
		}

		final FD fd = new FD(td, fieldInfo.getAccessFlags(),
				fieldInfo.getName(), fieldInfo.getDescriptor(),
				signatureAttribute == null ? null
						: signatureAttribute.getSignature(), value);

		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible
					.getAnnotations();
			final A[] as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = createA(annotations[i], td.getT().getDu());
			}
			fd.setInvisibleAs(as);
		}

		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible
					.getAnnotations();
			final A[] as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = createA(annotations[i], td.getT().getDu());
			}
			fd.setVisibleAs(as);
		}

		if (deprecatedAttribute != null) {
			fd.setDeprecated(true);
		}

		if (syntheticAttribute != null) {
			fd.setSynthetic(true);
		}

		return fd;
	}

	private static Object readMemberValue(final MemberValue memberValue,
			final DU du) {
		if (memberValue instanceof AnnotationMemberValue) {
			return createA(((AnnotationMemberValue) memberValue).getValue(), du);
		}
		if (memberValue instanceof ArrayMemberValue) {
			final MemberValue[] values = ((ArrayMemberValue) memberValue)
					.getValue();
			final Object[] objects = new Object[values.length];
			for (int i = values.length; i-- > 0;) {
				objects[i] = readMemberValue(values[i], du);
			}
			return objects;
		}
		if (memberValue instanceof BooleanMemberValue) {
			return ((BooleanMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof ByteMemberValue) {
			return ((ByteMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof CharMemberValue) {
			return ((CharMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof ClassMemberValue) {
			return du.getT(((ClassMemberValue) memberValue).getValue());
		}
		if (memberValue instanceof DoubleMemberValue) {
			return ((DoubleMemberValue) memberValue).getValue();
		} else if (memberValue instanceof EnumMemberValue) {
			final String typeName = ((EnumMemberValue) memberValue).getType();
			final String value = ((EnumMemberValue) memberValue).getValue();
			final T t = du.getT(typeName); // java.lang.Thread$State
			final E e = new E(t, value);
			return e;
		}
		if (memberValue instanceof FloatMemberValue) {
			return ((FloatMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof IntegerMemberValue) {
			return ((IntegerMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof LongMemberValue) {
			return ((LongMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof ShortMemberValue) {
			return ((ShortMemberValue) memberValue).getValue();
		}
		if (memberValue instanceof StringMemberValue) {
			return ((StringMemberValue) memberValue).getValue();
		}
		LOGGER.log(Level.WARNING, "Unknown member value type '"
				+ memberValue.getClass().getName() + "'!");
		return null;
	}

	@SuppressWarnings("unchecked")
	private static MD readMethodInfo(final TD td, final MethodInfo methodInfo) {
		AnnotationDefaultAttribute annotationDefaultAttribute = null;
		AnnotationsAttribute annotationsAttributeRuntimeInvisible = null;
		AnnotationsAttribute annotationsAttributeRuntimeVisible = null;
		CodeAttribute codeAttribute = null;
		DeprecatedAttribute deprecatedAttribute = null;
		ExceptionsAttribute exceptionsAttribute = null;
		ParameterAnnotationsAttribute parameterAnnotationsAttributeRuntimeInvisible = null;
		ParameterAnnotationsAttribute parameterAnnotationsAttributeRuntimeVisible = null;
		SignatureAttribute signatureAttribute = null;
		SyntheticAttribute syntheticAttribute = null;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) methodInfo
				.getAttributes()) {
			final String attributeTag = attributeInfo.getName();
			if (AnnotationDefaultAttribute.tag.equals(attributeTag)) {
				annotationDefaultAttribute = (AnnotationDefaultAttribute) attributeInfo;
			} else if (AnnotationsAttribute.invisibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeInvisible = (AnnotationsAttribute) attributeInfo;
			} else if (AnnotationsAttribute.visibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeVisible = (AnnotationsAttribute) attributeInfo;
			} else if (CodeAttribute.tag.equals(attributeTag)) {
				codeAttribute = (CodeAttribute) attributeInfo;
			} else if (DeprecatedAttribute.tag.equals(attributeTag)) {
				deprecatedAttribute = (DeprecatedAttribute) attributeInfo;
			} else if (ExceptionsAttribute.tag.equals(attributeTag)) {
				exceptionsAttribute = (ExceptionsAttribute) attributeInfo;
			} else if (ParameterAnnotationsAttribute.invisibleTag
					.equals(attributeTag)) {
				parameterAnnotationsAttributeRuntimeInvisible = (ParameterAnnotationsAttribute) attributeInfo;
			} else if (ParameterAnnotationsAttribute.visibleTag
					.equals(attributeTag)) {
				parameterAnnotationsAttributeRuntimeVisible = (ParameterAnnotationsAttribute) attributeInfo;
			} else if (SignatureAttribute.tag.equals(attributeTag)) {
				signatureAttribute = (SignatureAttribute) attributeInfo;
			} else if (SyntheticAttribute.tag.equals(attributeTag)) {
				syntheticAttribute = (SyntheticAttribute) attributeInfo;
			} else {
				LOGGER.log(Level.WARNING,
						"Unknown method attribute tag '" + attributeTag
								+ "' for method info '" + methodInfo.getName()
								+ "'!");
			}
		}

		String mdSignature = null;
		if (signatureAttribute != null
				&& signatureAttribute.getSignature() != null) {
			mdSignature = signatureAttribute.getSignature();
		}

		String[] mdExceptions = null;
		if (exceptionsAttribute != null
				&& exceptionsAttribute.getExceptions() != null
				&& exceptionsAttribute.getExceptions().length > 0) {
			mdExceptions = exceptionsAttribute.getExceptions();
		}

		final MD md = new MD(td, methodInfo.getAccessFlags(),
				methodInfo.getName(), methodInfo.getDescriptor(), mdSignature,
				mdExceptions);

		if (annotationDefaultAttribute != null) {
			final MemberValue defaultMemberValue = annotationDefaultAttribute
					.getDefaultValue();
			final Object annotationDefaultValue = readMemberValue(
					defaultMemberValue, td.getT().getDu());
			md.setAnnotationDefaultValue(annotationDefaultValue);
		}

		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible
					.getAnnotations();
			final A[] as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = createA(annotations[i], td.getT().getDu());
			}
			md.setInvisibleAs(as);
		}

		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible
					.getAnnotations();
			final A[] as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = createA(annotations[i], td.getT().getDu());
			}
			md.setVisibleAs(as);
		}

		if (codeAttribute != null) {
			// TODO temporary
			md.setCodeAttribute(codeAttribute);
		}

		if (deprecatedAttribute != null) {
			md.setDeprecated(true);
		}

		if (parameterAnnotationsAttributeRuntimeInvisible != null) {
			final Annotation[][] annotationss = parameterAnnotationsAttributeRuntimeInvisible
					.getAnnotations();
			final A[][] ass = new A[annotationss.length][];
			for (int i = annotationss.length; i-- > 0;) {
				final Annotation[] annotations = annotationss[i];
				final A[] as = ass[i] = new A[annotations.length];
				for (int j = annotations.length; j-- > 0;) {
					as[j] = createA(annotations[j], td.getT().getDu());
				}
			}
			md.setInvisibleParamAs(ass);
		}

		if (parameterAnnotationsAttributeRuntimeVisible != null) {
			final Annotation[][] annotationss = parameterAnnotationsAttributeRuntimeVisible
					.getAnnotations();
			final A[][] ass = new A[annotationss.length][];
			for (int i = annotationss.length; i-- > 0;) {
				final Annotation[] annotations = annotationss[i];
				final A[] as = ass[i] = new A[annotations.length];
				for (int j = annotations.length; j-- > 0;) {
					as[j] = createA(annotations[j], td.getT().getDu());
				}
			}
			md.setVisibleParamAs(ass);
		}

		if (syntheticAttribute != null) {
			md.setSynthetic(true);
		}

		return md;
	}

}