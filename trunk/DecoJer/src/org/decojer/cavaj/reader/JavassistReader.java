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
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
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
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SourceFileAttribute;
import javassist.bytecode.StackMap;
import javassist.bytecode.StackMapTable;
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
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;

/**
 * Reader from Javassist.
 * 
 * @author André Pankraz
 */
public class JavassistReader {

	private final static Logger LOGGER = Logger.getLogger(JavassistReader.class
			.getName());

	/**
	 * Analyse class input stream.
	 * 
	 * @param is
	 *            class input stream
	 * @return type
	 * @throws IOException
	 *             read exception
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
	 *            JAR input stream
	 * @return types
	 * @throws IOException
	 *             read exception
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

	/**
	 * Test it...
	 * 
	 * @param args
	 *            args
	 * @throws IOException
	 *             read exception
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
		td.setAccessFlags(classFile.getAccessFlags());
		td.setVersion(classFile.getMajorVersion());

		A[] as = null;
		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible
					.getAnnotations();
			as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.CLASS,
						du);
			}
		}
		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible
					.getAnnotations();
			if (as == null) {
				as = new A[annotations.length];
			} else {
				final A[] newAs = new A[annotations.length + as.length];
				System.arraycopy(as, 0, newAs, annotations.length, as.length);
				as = newAs;
			}
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.RUNTIME,
						du);
			}
		}
		td.setAs(as);

		if (deprecatedAttribute != null) {
			td.setDeprecated(true);
		}

		if (enclosingMethodAttribute != null) {
			if (enclosingMethodAttribute.methodIndex() > 0) {
				// is anonymous class, is in method
				final T methodT = du.getT(enclosingMethodAttribute.className());
				td.setEnclosingM(methodT.getM(
						enclosingMethodAttribute.methodName(),
						enclosingMethodAttribute.methodDescriptor()));
			}
			if (enclosingMethodAttribute.classIndex() > 0) {
				// is anonymous class, is in field initializer
				td.setEnclosingT(du.getT(enclosingMethodAttribute.className()));
			}
		}
		if (innerClassesAttribute != null) {
			final List<T> memberTs = new ArrayList<T>();
			for (int i = 0; i < innerClassesAttribute.tableLength(); ++i) {
				// outer class info not known in Dalvik and derivable
				if (t.getName().equals(innerClassesAttribute.innerClass(i))) {
					// is inner type, this attributes is senseless?
					// inner name from naming rules and flags are known
					continue;
				}
				if (t.getName().equals(innerClassesAttribute.outerClass(i))) {
					// has member types (really contained inner classes)
					memberTs.add(du.getT(innerClassesAttribute.innerClass(i)));
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

		for (final FieldInfo fieldInfo : (List<FieldInfo>) classFile
				.getFields()) {
			td.getBds().add(readField(td, fieldInfo));
		}

		for (final MethodInfo methodInfo : (List<MethodInfo>) classFile
				.getMethods()) {
			td.getBds().add(readMethod(td, methodInfo));
		}

		du.addTd(td);

		return td;
	}

	@SuppressWarnings("unchecked")
	private static A readAnnotation(final Annotation annotation,
			final RetentionPolicy retentionPolicy, final DU du) {
		final T t = du.getT(annotation.getTypeName());
		final A a = new A(t, retentionPolicy);
		if (annotation.getMemberNames() != null) {
			for (final String name : (Set<String>) annotation.getMemberNames()) {
				a.addMember(name,
						readValue(annotation.getMemberValue(name), du));
			}
		}
		return a;
	}

	@SuppressWarnings("unchecked")
	private static void readCode(final MD md, final CodeAttribute codeAttribute) {
		LineNumberAttribute lineNumberAttribute = null;
		LocalVariableAttribute localVariableAttribute = null;
		LocalVariableAttribute localVariableTypeAttribute = null;
		StackMap stackMap;
		StackMapTable stackMapTable;

		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) codeAttribute
				.getAttributes()) {
			final String attributeTag = attributeInfo.getName();
			if (LineNumberAttribute.tag.equals(attributeTag)) {
				lineNumberAttribute = (LineNumberAttribute) attributeInfo;
			} else if (LocalVariableAttribute.tag.equals(attributeTag)) {
				localVariableAttribute = (LocalVariableAttribute) attributeInfo;
			} else if (LocalVariableAttribute.typeTag.equals(attributeTag)) {
				localVariableTypeAttribute = (LocalVariableAttribute) attributeInfo;
			} else if (StackMap.tag.equals(attributeTag)) {
				stackMap = (StackMap) attributeInfo;
			} else if (StackMapTable.tag.equals(attributeTag)) {
				stackMapTable = (StackMapTable) attributeInfo;
			} else {
				LOGGER.warning("Unknown code attribute tag '" + attributeTag
						+ "' in '" + md + "'!");
			}
		}

		final M m = md.getM();
		/*
		 * if (localVariableAttribute != null) { final int params =
		 * m.getParamTs().length; final String[] paramNames = new
		 * String[params]; // TODO static starts with 0, else 1 ... ignore this!
		 * for (int i = params; i-- > 0;) { if (i <
		 * localVariableAttribute.tableLength()) { paramNames[i] =
		 * localVariableAttribute.variableName(i); } }
		 * m.setParamNames(paramNames); }
		 */
		// TODO temporary
		md.setCodeAttribute(codeAttribute);
	}

	@SuppressWarnings("unchecked")
	private static FD readField(final TD td, final FieldInfo fieldInfo) {
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
				LOGGER.warning("Unknown field attribute tag '" + attributeTag
						+ "' for field info '" + fieldInfo.getName() + "'!");
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

		final T t = td.getT();
		final DU du = t.getDu();

		final FD fd = new FD(td, fieldInfo.getAccessFlags(),
				fieldInfo.getName(), fieldInfo.getDescriptor(),
				signatureAttribute == null ? null
						: signatureAttribute.getSignature(), value);

		A[] as = null;
		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible
					.getAnnotations();
			as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.RUNTIME,
						du);
			}
		}
		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible
					.getAnnotations();
			if (as == null) {
				as = new A[annotations.length];
			} else {
				final A[] newAs = new A[annotations.length + as.length];
				System.arraycopy(as, 0, newAs, annotations.length, as.length);
				as = newAs;
			}
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.CLASS,
						du);
			}
		}
		fd.setAs(as);

		if (deprecatedAttribute != null) {
			fd.setDeprecated(true);
		}

		if (syntheticAttribute != null) {
			fd.setSynthetic(true);
		}

		return fd;
	}

	@SuppressWarnings("unchecked")
	private static MD readMethod(final TD td, final MethodInfo methodInfo) {
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
				LOGGER.warning("Unknown method attribute tag '" + attributeTag
						+ "' for method info '" + methodInfo.getName() + "'!");
			}
		}

		final T t = td.getT();
		final DU du = t.getDu();

		final M m = t.getM(methodInfo.getName(), methodInfo.getDescriptor());
		if (exceptionsAttribute != null) {
			final String[] exceptions = exceptionsAttribute.getExceptions();
			if (exceptions != null && exceptions.length > 0) {
				final T[] throwsTs = new T[exceptions.length];
				for (int i = exceptions.length; i-- > 0;) {
					throwsTs[i] = du.getT(exceptions[i]);
				}
				m.setThrowsTs(throwsTs);
			}
		}
		if (signatureAttribute != null
				&& signatureAttribute.getSignature() != null) {
			m.setSignature(signatureAttribute.getSignature());
		}

		final MD md = new MD(m, td);
		md.setAccessFlags(methodInfo.getAccessFlags());

		if (annotationDefaultAttribute != null) {
			final MemberValue defaultMemberValue = annotationDefaultAttribute
					.getDefaultValue();
			final Object annotationDefaultValue = readValue(defaultMemberValue,
					du);
			md.setAnnotationDefaultValue(annotationDefaultValue);
		}

		A[] as = null;
		if (annotationsAttributeRuntimeVisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeVisible
					.getAnnotations();
			as = new A[annotations.length];
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.RUNTIME,
						du);
			}
		}
		if (annotationsAttributeRuntimeInvisible != null) {
			final Annotation[] annotations = annotationsAttributeRuntimeInvisible
					.getAnnotations();
			if (as == null) {
				as = new A[annotations.length];
			} else {
				final A[] newAs = new A[annotations.length + as.length];
				System.arraycopy(as, 0, newAs, annotations.length, as.length);
				as = newAs;
			}
			for (int i = annotations.length; i-- > 0;) {
				as[i] = readAnnotation(annotations[i], RetentionPolicy.CLASS,
						du);
			}
		}
		md.setAs(as);

		if (codeAttribute != null) {
			readCode(md, codeAttribute);
		}

		if (deprecatedAttribute != null) {
			md.setDeprecated(true);
		}

		A[][] paramAss = null;
		if (parameterAnnotationsAttributeRuntimeVisible != null) {
			final Annotation[][] annotationss = parameterAnnotationsAttributeRuntimeVisible
					.getAnnotations();
			paramAss = new A[annotationss.length][];
			for (int i = annotationss.length; i-- > 0;) {
				final Annotation[] annotations = annotationss[i];
				final A[] paramAs = paramAss[i] = new A[annotations.length];
				for (int j = annotations.length; j-- > 0;) {
					paramAs[j] = readAnnotation(annotations[j],
							RetentionPolicy.RUNTIME, du);
				}
			}
		}
		if (parameterAnnotationsAttributeRuntimeInvisible != null) {
			final Annotation[][] annotationss = parameterAnnotationsAttributeRuntimeInvisible
					.getAnnotations();
			if (paramAss == null) {
				paramAss = new A[annotationss.length][];
			} else if (paramAss.length < annotationss.length) {
				final A[][] newParamAss = new A[annotationss.length][];
				System.arraycopy(paramAss, 0, newParamAss, 0, paramAss.length);
				paramAss = newParamAss;
			}
			for (int i = annotationss.length; i-- > 0;) {
				final Annotation[] annotations = annotationss[i];
				A[] paramAs = paramAss[i];
				if (paramAs == null) {
					paramAs = paramAss[i] = new A[annotations.length];
				} else {
					paramAss[i] = new A[annotations.length + paramAs.length];
					System.arraycopy(paramAs, 0, paramAss[i],
							annotations.length, paramAs.length);
					paramAs = paramAss[i];
				}
				for (int j = annotations.length; j-- > 0;) {
					paramAs[j] = readAnnotation(annotations[j],
							RetentionPolicy.CLASS, du);
				}
			}
		}
		md.setParamAs(paramAss);

		if (syntheticAttribute != null) {
			md.setSynthetic(true);
		}

		return md;
	}

	private static Object readValue(final MemberValue memberValue, final DU du) {
		if (memberValue instanceof AnnotationMemberValue) {
			// retention unknown for annotation constant
			return readAnnotation(
					((AnnotationMemberValue) memberValue).getValue(), null, du);
		}
		if (memberValue instanceof ArrayMemberValue) {
			final MemberValue[] values = ((ArrayMemberValue) memberValue)
					.getValue();
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
		LOGGER.warning("Unknown member value type '"
				+ memberValue.getClass().getName() + "'!");
		return null;
	}

}