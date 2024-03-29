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
package org.decojer.cavaj.readers.javassist;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ConstantAttribute;
import javassist.bytecode.DeprecatedAttribute;
import javassist.bytecode.Descriptor;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ClassReader;

/**
 * Reader from Javassist.
 *
 * @author André Pankraz
 */
@Slf4j
public class JavassistReader implements ClassReader {

	@Nonnull
	private final DU du;

	@Nonnull
	private final ReadCodeAttribute readCodeAttribute = new ReadCodeAttribute();

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public JavassistReader(@Nonnull final DU du) {
		this.du = du;
	}

	/**
	 * Helper function to avoid constPool.getClassInfo(i), which replaces '/' with '.'. This leads
	 * to problems with new signatures that contain '.' for nested classes.
	 *
	 * @param constPool
	 *            constant pool
	 * @param index
	 *            constant pool index for descriptor
	 * @return type
	 */
	private T getT(final ConstPool constPool, final int index) {
		final String desc = constPool.getClassInfoByDescriptor(index);
		// TODO return type can also be arrayT, may be arrayT should extend classT?
		return desc == null ? null : this.du.getDescT(desc);
	}

	@Nullable
	@Override
	public T read(final InputStream is) throws IOException {
		final ClassFile classFile = new ClassFile(new DataInputStream(is));
		final String name = classFile.getName();
		if (name == null) {
			log.warn("Cannot read type name '" + name + "'!");
			return null;
		}
		final T t = this.du.getT(name);
		if (!t.createTd()) {
			log.warn("Type '" + t + "' already read!");
			return null;
		}
		t.setAccessFlags(classFile.getAccessFlags());
		final ConstPool constPool = classFile.getConstPool();
		t.setSuperT(getT(constPool, classFile.getSuperclassId()));
		// FIXME problem with / (avoid getClassInfo in Javassist)
		final String[] interfaces = classFile.getInterfaces();
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				final String interfaceName = interfaces[i];
				assert interfaceName != null;
				interfaceTs[i] = this.du.getT(interfaceName);
			}
			t.setInterfaceTs(interfaceTs);
		}
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
		boolean scalaAttributes = false;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) classFile.getAttributes()) {
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
			} else if ("Scala".equals(attributeTag) || "ScalaSig".equals(attributeTag)) {
				scalaAttributes = true;
			} else if (!attributeTag.equals("org.aspectj.weaver.WeaverState")
					&& !attributeTag.equals("OJC")) {
				log.warn("Unknown class attribute tag '" + attributeTag + "'!");
			}
		}
		if (signatureAttribute != null) {
			t.setSignature(signatureAttribute.getSignature());
		}
		t.setVersion(classFile.getMajorVersion());
		final A[] as = readAnnotations(annotationsAttributeRuntimeInvisible,
				annotationsAttributeRuntimeVisible);
		if (as != null) {
			t.setAs(as);
		}
		if (deprecatedAttribute != null) {
			t.setDeprecated();
		}
		if (enclosingMethodAttribute != null) {
			final ClassT enclosingT = (ClassT) getT(constPool,
					enclosingMethodAttribute.classIndex());
			if (enclosingMethodAttribute.methodIndex() == 0) {
				t.setEnclosingT(enclosingT);
			} else {
				final String methodName = enclosingMethodAttribute.methodName();
				final String methodDescriptor = enclosingMethodAttribute.methodDescriptor();
				if (methodName == null || methodDescriptor == null) {
					assert false;
					t.setEnclosingT(enclosingT);
				} else {
					t.setEnclosingM(enclosingT.getM(methodName, methodDescriptor));
				}
			}
		}
		if (innerClassesAttribute != null) {
			final int tableLength = innerClassesAttribute.tableLength();
			for (int i = 0; i < tableLength; ++i) {
				final ClassT innerT = (ClassT) getT(constPool,
						innerClassesAttribute.innerClassIndex(i));
				if (innerClassesAttribute.outerClassIndex(i) != 0) {
					// set enclosing first for better inner name check
					innerT.setEnclosingT(getT(constPool, innerClassesAttribute.outerClassIndex(i)));
				}
				innerT.setInnerInfo(innerClassesAttribute.innerName(i),
						innerClassesAttribute.accessFlags(i));
			}
		}
		if (sourceFileAttribute != null) {
			t.setSourceFileName(sourceFileAttribute.getFileName());
		}
		if (syntheticAttribute != null) {
			t.setSynthetic();
		}
		if (scalaAttributes) {
			t.setScala();
		}
		for (final FieldInfo fieldInfo : (List<FieldInfo>) classFile.getFields()) {
			if (fieldInfo != null) {
				readField(t, fieldInfo);
			}
		}
		for (final MethodInfo methodInfo : (List<MethodInfo>) classFile.getMethods()) {
			if (methodInfo != null) {
				readMethod(t, methodInfo);
			}
		}
		t.resolve();
		return t;
	}

	@Nullable
	private A readAnnotation(final Annotation annotation, final RetentionPolicy retentionPolicy) {
		final String typeName = annotation.getTypeName();
		if (typeName == null) {
			assert false;
			return null;
		}
		final T t = this.du.getT(typeName);
		final A a = new A(t, retentionPolicy);
		if (annotation.getMemberNames() != null) {
			for (final String name : (Set<String>) annotation.getMemberNames()) {
				final MemberValue memberValue = annotation.getMemberValue(name);
				assert memberValue != null;
				a.addMember(name, readValue(memberValue));
			}
		}
		return a;
	}

	private A[] readAnnotations(final AnnotationsAttribute annotationsAttributeRuntimeInvisible,
			final AnnotationsAttribute annotationsAttributeRuntimeVisible) {
		A[] as = null;
		// Visible comes first in bytecode, but here we start with invisible
		// because of array extension trick
		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible.getAnnotations();
			as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.CLASS);
			}
		}
		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible.getAnnotations();
			if (as == null) {
				as = new A[annotations.length];
			} else {
				final A[] newAs = new A[annotations.length + as.length];
				System.arraycopy(as, 0, newAs, annotations.length, as.length);
				as = newAs;
			}
			// trick
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.RUNTIME);
			}
		}
		return as;
	}

	@Nullable
	private F readField(@Nonnull final T t, @Nonnull final FieldInfo fieldInfo) {
		final String name = fieldInfo.getName();
		final String descriptor = fieldInfo.getDescriptor();
		if (name == null || descriptor == null) {
			return null;
		}

		AnnotationsAttribute annotationsAttributeRuntimeInvisible = null;
		AnnotationsAttribute annotationsAttributeRuntimeVisible = null;
		ConstantAttribute constantAttribute = null;
		DeprecatedAttribute deprecatedAttribute = null;
		SignatureAttribute signatureAttribute = null;
		SyntheticAttribute syntheticAttribute = null;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) fieldInfo.getAttributes()) {
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
				log.warn("Unknown field attribute tag '" + attributeTag + "' for field info '"
						+ fieldInfo.getName() + "'!");
			}
		}
		final ConstPool constPool = fieldInfo.getConstPool();

		final F f = t.getF(name, descriptor);
		f.createFd();
		f.setAccessFlags(fieldInfo.getAccessFlags());
		if (signatureAttribute != null && signatureAttribute.getSignature() != null) {
			f.setSignature(signatureAttribute.getSignature());
		}
		final A[] as = readAnnotations(annotationsAttributeRuntimeInvisible,
				annotationsAttributeRuntimeVisible);
		if (as != null) {
			f.setAs(as);
		}
		if (constantAttribute != null) {
			Object value = null;
			// only final, non static - no arrays, class types
			final int index = constantAttribute.getConstantValue();
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
				log.warn("Unknown constant attribute '" + tag + "' for field info '"
						+ fieldInfo.getName() + "'!");
			}
			f.setValue(value);
		}
		if (deprecatedAttribute != null) {
			f.setDeprecated();
		}
		if (syntheticAttribute != null) {
			f.setSynthetic();
		}
		return f;
	}

	@Nullable
	private M readMethod(@Nonnull final T t, @Nonnull final MethodInfo methodInfo) {
		final String name = methodInfo.getName();
		final String descriptor = methodInfo.getDescriptor();
		if (name == null || descriptor == null) {
			return null;
		}

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
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) methodInfo.getAttributes()) {
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
			} else if (ParameterAnnotationsAttribute.invisibleTag.equals(attributeTag)) {
				parameterAnnotationsAttributeRuntimeInvisible = (ParameterAnnotationsAttribute) attributeInfo;
			} else if (ParameterAnnotationsAttribute.visibleTag.equals(attributeTag)) {
				parameterAnnotationsAttributeRuntimeVisible = (ParameterAnnotationsAttribute) attributeInfo;
			} else if (SignatureAttribute.tag.equals(attributeTag)) {
				signatureAttribute = (SignatureAttribute) attributeInfo;
			} else if (SyntheticAttribute.tag.equals(attributeTag)) {
				syntheticAttribute = (SyntheticAttribute) attributeInfo;
			} else if (!attributeTag.equals("org.aspectj.weaver.MethodDeclarationLineNumber")) {
				log.warn("Unknown method attribute tag '" + attributeTag + "' for method info '"
						+ methodInfo.getName() + "'!");
			}
		}
		final ConstPool constPool = methodInfo.getConstPool();

		final M m = t.getM(name, descriptor);
		m.createMd();
		m.setAccessFlags(methodInfo.getAccessFlags());
		if (exceptionsAttribute != null) {
			final int[] exceptions = exceptionsAttribute.getExceptionIndexes();
			if (exceptions != null && exceptions.length > 0) {
				final T[] throwsTs = new T[exceptions.length];
				for (int i = exceptions.length; i-- > 0;) {
					throwsTs[i] = getT(constPool, exceptions[i]);
				}
				m.setThrowsTs(throwsTs);
			}
		}
		if (signatureAttribute != null) {
			m.setSignature(signatureAttribute.getSignature());
		}
		if (annotationDefaultAttribute != null) {
			final MemberValue defaultMemberValue = annotationDefaultAttribute.getDefaultValue();
			if (defaultMemberValue != null) {
				final Object annotationDefaultValue = readValue(defaultMemberValue);
				m.setAnnotationDefaultValue(annotationDefaultValue);
			}
		}
		m.setAs(readAnnotations(annotationsAttributeRuntimeInvisible,
				annotationsAttributeRuntimeVisible));
		if (codeAttribute != null) {
			this.readCodeAttribute.initAndVisit(m, codeAttribute);
		}
		if (deprecatedAttribute != null) {
			m.setDeprecated();
		}
		A[][] paramAss = null;
		// Visible comes first in bytecode, but here we start with invisible
		// because of array extension trick
		if (parameterAnnotationsAttributeRuntimeInvisible != null) {
			final Annotation[][] annotationss = parameterAnnotationsAttributeRuntimeInvisible
					.getAnnotations();
			paramAss = new A[annotationss.length][];
			for (int i = annotationss.length; i-- > 0;) {
				final Annotation[] annotations = annotationss[i];
				final A[] paramAs = paramAss[i] = new A[annotations.length];
				for (int j = annotations.length; j-- > 0;) {
					paramAs[j] = readAnnotation(annotations[j], RetentionPolicy.CLASS);
				}
			}
		}
		if (parameterAnnotationsAttributeRuntimeVisible != null) {
			final Annotation[][] annotationss = parameterAnnotationsAttributeRuntimeVisible
					.getAnnotations();
			if (paramAss == null) {
				paramAss = new A[annotationss.length][];
			} else if (annotationss.length > paramAss.length) {
				final A[][] newParamAss = new A[annotationss.length][];
				System.arraycopy(paramAss, 0, newParamAss, 0, paramAss.length);
				paramAss = newParamAss;
			}
			for (int i = annotationss.length; i-- > 0;) {
				final Annotation[] annotations = annotationss[i];
				A[] paramAs = paramAss[i];
				if (paramAs == null) {
					paramAs = new A[annotations.length];
				} else {
					final A[] newParamAs = new A[annotations.length + paramAs.length];
					System.arraycopy(paramAs, 0, newParamAs, annotations.length, paramAs.length);
					paramAs = newParamAs;
				}
				paramAss[i] = paramAs;
				for (int j = annotations.length; j-- > 0;) {
					paramAs[j] = readAnnotation(annotations[j], RetentionPolicy.RUNTIME);
				}
			}
		}
		m.setParamAss(paramAss);
		if (syntheticAttribute != null) {
			m.setSynthetic();
		}
		return m;
	}

	@Nullable
	private Object readValue(@Nonnull final MemberValue memberValue) {
		if (memberValue instanceof AnnotationMemberValue) {
			// retention unknown for annotation constant
			return readAnnotation(((AnnotationMemberValue) memberValue).getValue(), null);
		}
		if (memberValue instanceof ArrayMemberValue) {
			final MemberValue[] values = ((ArrayMemberValue) memberValue).getValue();
			final Object[] objects = new Object[values.length];
			for (int i = values.length; i-- > 0;) {
				final MemberValue value = values[i];
				assert value != null;
				objects[i] = readValue(value);
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
			final String value = ((ClassMemberValue) memberValue).getValue();
			assert value != null;
			return this.du.getT(value);
		}
		if (memberValue instanceof DoubleMemberValue) {
			return ((DoubleMemberValue) memberValue).getValue();
		} else if (memberValue instanceof EnumMemberValue) {
			// TODO better direct access, but missing method in Javassist API
			final String desc = Descriptor.of(((EnumMemberValue) memberValue).getType());
			assert desc != null;
			final T ownerT = this.du.getDescT(desc);
			assert ownerT != null;
			final String value = ((EnumMemberValue) memberValue).getValue();
			assert value != null;
			final F f = ownerT.getF(value, desc);
			f.setEnum();
			return f;
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
		log.warn("Unknown member value type '" + memberValue.getClass().getName() + "'!");
		return null;
	}

}