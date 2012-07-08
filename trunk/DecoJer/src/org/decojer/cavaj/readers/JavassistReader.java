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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.readers.javassist.ReadCodeAttribute;

/**
 * Reader from Javassist.
 * 
 * @author André Pankraz
 */
public class JavassistReader implements ClassReader {

	private final static Logger LOGGER = Logger.getLogger(JavassistReader.class.getName());

	private final DU du;

	private final ReadCodeAttribute readCodeAttribute;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public JavassistReader(final DU du) {
		assert du != null;

		this.du = du;
		this.readCodeAttribute = new ReadCodeAttribute(du);
	}

	@Override
	@SuppressWarnings("unchecked")
	public TD read(final InputStream is) throws IOException {
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
			} else {
				LOGGER.warning("Unknown class attribute tag '" + attributeTag + "'!");
			}
		}
		final TD td = this.du.getTd(classFile.getName());
		td.setAccessFlags(classFile.getAccessFlags());
		td.setSuperT(this.du.getT(classFile.getSuperclass()));
		final String[] interfaces = classFile.getInterfaces();
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = this.du.getT(interfaces[i]);
			}
			td.setInterfaceTs(interfaceTs);
		}
		if (signatureAttribute != null) {
			td.setSignature(signatureAttribute.getSignature());
		}
		td.setVersion(classFile.getMajorVersion());
		final A[] as = readAnnotations(annotationsAttributeRuntimeInvisible,
				annotationsAttributeRuntimeVisible);
		if (as != null) {
			td.setAs(as);
		}
		if (deprecatedAttribute != null) {
			td.setDeprecated(true);
		}
		if (enclosingMethodAttribute != null) {
			if (enclosingMethodAttribute.classIndex() > 0) {
				td.setEnclosingT(this.du.getT(enclosingMethodAttribute.className()));
			}
			if (enclosingMethodAttribute.methodIndex() > 0) {
				final T ownerT = this.du.getT(enclosingMethodAttribute.className());
				td.setEnclosingM(ownerT.getM(enclosingMethodAttribute.methodName(),
						enclosingMethodAttribute.methodDescriptor()));
			}
		}
		if (innerClassesAttribute != null) {
			final List<T> memberTs = new ArrayList<T>();
			// preserve order
			final int tableLength = innerClassesAttribute.tableLength();
			for (int i = 0; i < tableLength; ++i) {
				// Dalvik has not all inner class info from JVM Bytecode:
				// outer class info not known in Dalvik and is derivable anyway,
				// no access flags for member classes,
				// no info for arbitrarily accessed and nested inner classes
				if (td.getName().equals(innerClassesAttribute.outerClass(i))) {
					// Attribute describes direct Member / Inner Class
					memberTs.add(this.du.getT(innerClassesAttribute.innerClass(i)));
					continue;
				}
			}
			if (memberTs.size() > 0) {
				td.setMemberTs(memberTs.toArray(new T[memberTs.size()]));
			}
		}
		if (sourceFileAttribute != null) {
			td.setSourceFileName(sourceFileAttribute.getFileName());
		}
		if (syntheticAttribute != null) {
			td.setSynthetic(true);
		}
		for (final FieldInfo fieldInfo : (List<FieldInfo>) classFile.getFields()) {
			td.getBds().add(readField(td, fieldInfo));
		}
		for (final MethodInfo methodInfo : (List<MethodInfo>) classFile.getMethods()) {
			td.getBds().add(readMethod(td, methodInfo));
		}
		return td;
	}

	@SuppressWarnings("unchecked")
	private A readAnnotation(final Annotation annotation, final RetentionPolicy retentionPolicy) {
		final T t = this.du.getT(annotation.getTypeName());
		final A a = new A(t, retentionPolicy);
		if (annotation.getMemberNames() != null) {
			for (final String name : (Set<String>) annotation.getMemberNames()) {
				a.addMember(name, readValue(annotation.getMemberValue(name), this.du));
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

	@SuppressWarnings("unchecked")
	private FD readField(final TD td, final FieldInfo fieldInfo) {
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
				LOGGER.warning("Unknown field attribute tag '" + attributeTag
						+ "' for field info '" + fieldInfo.getName() + "'!");
			}
		}

		final T valueT = this.du.getDescT(fieldInfo.getDescriptor());
		final F f = td.getF(fieldInfo.getName(), valueT);
		f.setAccessFlags(fieldInfo.getAccessFlags());
		if (signatureAttribute != null && signatureAttribute.getSignature() != null) {
			f.setSignature(signatureAttribute.getSignature());
		}

		final FD fd = new FD(f, td);

		final A[] as = readAnnotations(annotationsAttributeRuntimeInvisible,
				annotationsAttributeRuntimeVisible);
		if (as != null) {
			fd.setAs(as);
		}

		if (constantAttribute != null) {
			Object value = null;
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
				LOGGER.warning("Unknown constant attribute '" + tag + "' for field info '"
						+ fieldInfo.getName() + "'!");
			}
			fd.setValue(value);
		}

		if (deprecatedAttribute != null) {
			fd.setDeprecated(true);
		}

		if (syntheticAttribute != null) {
			fd.setSynthetic(true);
		}

		return fd;
	}

	@SuppressWarnings("unchecked")
	private MD readMethod(final TD td, final MethodInfo methodInfo) {
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
			} else {
				LOGGER.warning("Unknown method attribute tag '" + attributeTag
						+ "' for method info '" + methodInfo.getName() + "'!");
			}
		}

		final M m = td.getM(methodInfo.getName(), methodInfo.getDescriptor());
		m.setAccessFlags(methodInfo.getAccessFlags());
		if (exceptionsAttribute != null) {
			final String[] exceptions = exceptionsAttribute.getExceptions();
			if (exceptions != null && exceptions.length > 0) {
				final T[] throwsTs = new T[exceptions.length];
				for (int i = exceptions.length; i-- > 0;) {
					throwsTs[i] = this.du.getT(exceptions[i]);
				}
				m.setThrowsTs(throwsTs);
			}
		}
		if (signatureAttribute != null) {
			m.setSignature(signatureAttribute.getSignature());
		}

		final MD md = new MD(m, td);

		if (annotationDefaultAttribute != null) {
			final MemberValue defaultMemberValue = annotationDefaultAttribute.getDefaultValue();
			final Object annotationDefaultValue = readValue(defaultMemberValue, this.du);
			md.setAnnotationDefaultValue(annotationDefaultValue);
		}

		final A[] as = readAnnotations(annotationsAttributeRuntimeInvisible,
				annotationsAttributeRuntimeVisible);
		if (as != null) {
			md.setAs(as);
		}

		if (codeAttribute != null) {
			this.readCodeAttribute.initAndVisit(md, codeAttribute);
		}

		if (deprecatedAttribute != null) {
			md.setDeprecated(true);
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
		md.setParamAss(paramAss);

		if (syntheticAttribute != null) {
			md.setSynthetic(true);
		}

		return md;
	}

	private Object readValue(final MemberValue memberValue, final DU du) {
		if (memberValue instanceof AnnotationMemberValue) {
			// retention unknown for annotation constant
			return readAnnotation(((AnnotationMemberValue) memberValue).getValue(), null);
		}
		if (memberValue instanceof ArrayMemberValue) {
			final MemberValue[] values = ((ArrayMemberValue) memberValue).getValue();
			final Object[] objects = new Object[values.length];
			for (int i = values.length; i-- > 0;) {
				objects[i] = readValue(values[i], du);
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
			final T enumT = du.getT(((EnumMemberValue) memberValue).getType());
			final F enumF = enumT.getF(((EnumMemberValue) memberValue).getValue(), enumT);
			enumF.markAf(AF.ENUM);
			return enumF;
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
		LOGGER.warning("Unknown member value type '" + memberValue.getClass().getName() + "'!");
		return null;
	}

}