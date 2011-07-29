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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.E;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;
import org.jf.dexlib.AnnotationDirectoryItem;
import org.jf.dexlib.AnnotationDirectoryItem.FieldAnnotationIteratorDelegate;
import org.jf.dexlib.AnnotationDirectoryItem.MethodAnnotationIteratorDelegate;
import org.jf.dexlib.AnnotationDirectoryItem.ParameterAnnotationIteratorDelegate;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.AnnotationSetRefList;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.EncodedArrayItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.Item;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Format.Instruction35c;
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

/**
 * Reader from Smali.
 * 
 * @author André Pankraz
 */
public class SmaliReader {

	private final static Logger LOGGER = Logger.getLogger(SmaliReader.class
			.getName());

	/**
	 * Analyse DEX input stream.
	 * 
	 * @param is
	 *            DEX input stream
	 * @return types
	 * @throws IOException
	 *             read exception
	 */
	public static Types analyse(final InputStream is) throws IOException {
		final byte[] bytes = IOUtils.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), false,
				true); // fast
		final Types types = new Types();
		@SuppressWarnings("unchecked")
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			System.out.println("TEST " + classDefItem + " : "
					+ classDefItem.getClassType().getTypeDescriptor());
			types.addType(new Type(classDefItem.getClassType()
					.getTypeDescriptor(), null));
		}
		return types;
	}

	private static A decodeAnnotationValue(
			final AnnotationEncodedSubValue encodedValue,
			final RetentionPolicy retentionPolicy, final DU du) {
		final T t = du
				.getDescT(encodedValue.annotationType.getTypeDescriptor());
		final A a = new A(t, retentionPolicy);
		final StringIdItem[] names = encodedValue.names;
		final EncodedValue[] values = encodedValue.values;
		for (int i = 0; i < names.length; ++i) {
			a.addMember(names[i].getStringValue(), decodeValue(values[i], du));
		}
		return a;
	}

	private static A decodeAnnotationValue(final AnnotationItem annotationItem,
			final DU du) {
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
		return decodeAnnotationValue(annotationItem.getEncodedAnnotation(),
				retentionPolicy, du);
	}

	private static Object decodeValue(final EncodedValue encodedValue,
			final DU du) {
		if (encodedValue instanceof AnnotationEncodedSubValue) {
			return decodeAnnotationValue((AnnotationEncodedValue) encodedValue,
					null, du);
		}
		if (encodedValue instanceof ArrayEncodedValue) {
			final EncodedValue[] values = ((ArrayEncodedValue) encodedValue).values;
			final Object[] objects = new Object[values.length];
			for (int i = values.length; i-- > 0;) {
				objects[i] = decodeValue(values[i], du);
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
			// TODO enum ... interesting parallel to MethodEncodedValue with
			// M? maybe use F here?
			final FieldIdItem fieldidItem = ((EnumEncodedValue) encodedValue).value;
			final String typeDescr = fieldidItem.getFieldType()
					.getTypeDescriptor();
			final String value = fieldidItem.getFieldName().getStringDataItem()
					.getStringValue();
			final T t = du.getDescT(typeDescr);
			return new E(t, value);
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
			// TODO for @dalvik.annotation.EnclosingMethod only? M?
			return ((MethodEncodedValue) encodedValue).value.getMethodString();
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
			return du.getDescT(((TypeEncodedValue) encodedValue).value
					.getTypeDescriptor());
		}
		LOGGER.warning("Unknown encoded value type '"
				+ encodedValue.getClass().getName() + "'!");
		return null;
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
				"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex");
		// final Types types = analyse(is);
		// System.out.println("Ana: " + types.getTypes().size());
		read(is, new DU());
	}

	/**
	 * Read DEX input stream.
	 * 
	 * @param is
	 *            DEX input stream
	 * @param du
	 *            decompilation unit
	 * @throws IOException
	 *             read exception
	 */
	@SuppressWarnings("unchecked")
	public static void read(final InputStream is, final DU du)
			throws IOException {
		final byte[] bytes = IOUtils.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), true,
				false);
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			final T t = du.getDescT(classDefItem.getClassType()
					.getTypeDescriptor());
			t.setSuperT(du.getDescT(classDefItem.getSuperclass()
					.getTypeDescriptor()));
			final TypeListItem interfaces = classDefItem.getInterfaces();
			if (interfaces != null && interfaces.getTypeCount() > 0) {
				final T[] interfaceTs = new T[interfaces.getTypeCount()];
				for (int i = interfaces.getTypeCount(); i-- > 0;) {
					interfaceTs[i] = du.getDescT(interfaces.getTypeIdItem(i)
							.getTypeDescriptor());
				}
				t.setInterfaceTs(interfaceTs);
			}

			final TD td = new TD(t);
			td.setAccessFlags(classDefItem.getAccessFlags());

			A annotationDefaultValues = null;
			final Map<FieldIdItem, String> fieldSignatures = new HashMap<FieldIdItem, String>();
			final Map<FieldIdItem, A[]> fieldAs = new HashMap<FieldIdItem, A[]>();
			final Map<MethodIdItem, String[]> methodExceptions = new HashMap<MethodIdItem, String[]>();
			final Map<MethodIdItem, String> methodSignatures = new HashMap<MethodIdItem, String>();
			final Map<MethodIdItem, A[]> methodAs = new HashMap<MethodIdItem, A[]>();
			final Map<MethodIdItem, A[][]> methodParamAs = new HashMap<MethodIdItem, A[][]>();

			final AnnotationDirectoryItem annotations = classDefItem
					.getAnnotations();
			if (annotations != null) {
				final AnnotationSetItem classAnnotations = annotations
						.getClassAnnotations();
				if (classAnnotations != null) {
					final List<A> as = new ArrayList<A>();
					for (final AnnotationItem annotation : classAnnotations
							.getAnnotations()) {
						final A a = decodeAnnotationValue(annotation, du);
						if ("dalvik.annotation.AnnotationDefault".equals(a
								.getT().getName())) {
							// annotation default values, not encoded in
							// method annotations, but in global annotation with
							// "field name" -> value
							annotationDefaultValues = (A) a.getMemberValue();
							continue;
						}
						if ("dalvik.annotation.Signature".equals(a.getT()
								.getName())) {
							// signature, is encoded as annotation with string
							// array value
							final Object[] signature = (Object[]) a
									.getMemberValue();
							final StringBuilder sb = new StringBuilder();
							for (int i = 0; i < signature.length; ++i) {
								sb.append(signature[i]);
							}
							td.getT().setSignature(sb.toString());
							continue;
						}
						if ("dalvik.annotation.EnclosingClass".equals(a.getT()
								.getName())) {
							// System.out.println("EnclosingClass '"
							// + td.getT().getName() + "': " + a);
							continue;
						}
						if ("dalvik.annotation.EnclosingMethod".equals(a.getT()
								.getName())) {
							// System.out.println("EnclosingMethod '"
							// + td.getT().getName() + "': " + a);
							continue;
						}
						if ("dalvik.annotation.InnerClass".equals(a.getT()
								.getName())) {
							// System.out.println("InnerClass '"
							// + td.getT().getName() + "': " + a);
							continue;
						}
						if ("dalvik.annotation.MemberClasses".equals(a.getT()
								.getName())) {
							// System.out.println("MemberClasses '"
							// + td.getT().getName() + "': " + a);
							continue;
						}
						as.add(a);
					}
					td.setAs(as.toArray(new A[as.size()]));
				}
				annotations
						.iterateFieldAnnotations(new FieldAnnotationIteratorDelegate() {

							@Override
							public void processFieldAnnotations(
									final FieldIdItem field,
									final AnnotationSetItem fieldAnnotations) {
								final List<A> as = new ArrayList<A>();
								for (final AnnotationItem annotationItem : fieldAnnotations
										.getAnnotations()) {
									final A a = decodeAnnotationValue(
											annotationItem, du);
									if ("dalvik.annotation.Signature".equals(a
											.getT().getName())) {
										// signature, is encoded as annotation
										// with string array value
										final Object[] signature = (Object[]) a
												.getMemberValue();
										final StringBuilder sb = new StringBuilder();
										for (int i = 0; i < signature.length; ++i) {
											sb.append(signature[i]);
										}
										fieldSignatures.put(field,
												sb.toString());
										continue;
									} else {
										as.add(a);
									}
								}
								if (as.size() > 0) {
									fieldAs.put(field,
											as.toArray(new A[as.size()]));
								}
							}

						});
				annotations
						.iterateMethodAnnotations(new MethodAnnotationIteratorDelegate() {

							@Override
							public void processMethodAnnotations(
									final MethodIdItem method,
									final AnnotationSetItem methodAnnotations) {
								final List<A> as = new ArrayList<A>();
								for (final AnnotationItem annotationItem : methodAnnotations
										.getAnnotations()) {
									final A a = decodeAnnotationValue(
											annotationItem, du);
									if ("dalvik.annotation.Signature".equals(a
											.getT().getName())) {
										// signature, is encoded as annotation
										// with string array value
										final Object[] signature = (Object[]) a
												.getMemberValue();
										final StringBuilder sb = new StringBuilder();
										for (int i = 0; i < signature.length; ++i) {
											sb.append(signature[i]);
										}
										methodSignatures.put(method,
												sb.toString());
										continue;
									} else if ("dalvik.annotation.Throws"
											.equals(a.getT().getName())) {
										// throws, is encoded as annotation with
										// type array value
										final Object[] throwables = (Object[]) a
												.getMemberValue();
										final String[] exceptions = new String[throwables.length];
										for (int i = throwables.length; i-- > 0;) {
											exceptions[i] = ((T) throwables[i])
													.getName();
										}
										methodExceptions
												.put(method, exceptions);
										continue;
									} else {
										as.add(a);
									}
								}
								if (as.size() > 0) {
									methodAs.put(method,
											as.toArray(new A[as.size()]));
								}
							}

						});
				annotations
						.iterateParameterAnnotations(new ParameterAnnotationIteratorDelegate() {

							@Override
							public void processParameterAnnotations(
									final MethodIdItem method,
									final AnnotationSetRefList parameterAnnotations) {
								final AnnotationSetItem[] annotationSets = parameterAnnotations
										.getAnnotationSets();
								final A[][] paramAss = new A[annotationSets.length][];
								for (int i = annotationSets.length; i-- > 0;) {
									final AnnotationItem[] annotationItems = annotationSets[i]
											.getAnnotations();
									final A[] paramAs = paramAss[i] = new A[annotationItems.length];
									for (int j = annotationItems.length; j-- > 0;) {
										paramAs[j] = decodeAnnotationValue(
												annotationItems[j], du);
									}
								}
								methodParamAs.put(method, paramAss);
							}

						});
			}

			if (classDefItem.getSourceFile() != null) {
				td.setSourceFileName(classDefItem.getSourceFile()
						.getStringValue());
			}

			final ClassDataItem classData = classDefItem.getClassData();
			if (classData != null) {
				readFields(td, classData.getStaticFields(),
						classData.getInstanceFields(), fieldSignatures,
						classDefItem.getStaticFieldInitializers(), fieldAs);
				readMethods(td, classData.getDirectMethods(),
						classData.getVirtualMethods(), methodSignatures,
						methodExceptions, annotationDefaultValues, methodAs,
						methodParamAs);
			}

			du.addTd(td);
		}
	}

	private static void readCode(final CodeItem codeItem) {
		final Instruction[] instructions = codeItem.getInstructions();
		for (int j = 0; j < instructions.length; ++j) {
			final Instruction instruction = instructions[j];
			switch (instruction.opcode) {
			case INVOKE_DIRECT: {
				final Item referencedItem = ((Instruction35c) instruction)
						.getReferencedItem();
				System.out.println("  refItem: " + referencedItem + " : "
						+ ((Instruction35c) instruction).getRegCount());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterD());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterE());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterF());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterG());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterA());
				break;
			}
			case INVOKE_VIRTUAL: {
				final Item referencedItem = ((Instruction35c) instruction)
						.getReferencedItem();
				System.out.println("  refItem: " + referencedItem + " : "
						+ ((Instruction35c) instruction).getRegCount());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterD());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterE());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterF());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterG());
				System.out.println("    : "
						+ ((Instruction35c) instruction).getRegisterA());
				break;
			}
			}
			System.out.println("I: " + instructions[j].opcode + "     "
					+ instructions[j].getClass().getName());
		}
	}

	private static void readFields(final TD td,
			final EncodedField[] staticFields,
			final EncodedField[] instanceFields,
			final Map<FieldIdItem, String> fieldSignatures,
			final EncodedArrayItem staticFieldInitializers,
			final Map<FieldIdItem, A[]> fieldAs) {
		// static field initializer values are packed away into a different
		// section, both arrays (encoded fields and static field values) are
		// sorted in same order, there could be less static field values if
		// not all static fields have an initializer, but there is also a
		// null value as placeholder
		final EncodedValue[] staticFieldValues = staticFieldInitializers == null ? new EncodedValue[0]
				: staticFieldInitializers.getEncodedArray().values;

		for (int i = 0; i < staticFields.length; ++i) {
			final EncodedField encodedField = staticFields[i];
			final FieldIdItem field = encodedField.field;

			Object value = null;
			if (staticFieldValues.length > i) {
				value = decodeValue(staticFieldValues[i], td.getT().getDu());
			}
			final FD fd = new FD(td, encodedField.accessFlags, field
					.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor(), fieldSignatures.get(field), value);
			fd.setAs(fieldAs.get(field));
			td.getBds().add(fd);
		}
		for (int i = 0; i < instanceFields.length; ++i) {
			final EncodedField encodedField = instanceFields[i];
			final FieldIdItem field = encodedField.field;
			// there is no field initializer section for instance fields,
			// only via constructor
			final FD fd = new FD(td, encodedField.accessFlags, field
					.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor(), fieldSignatures.get(field), null);
			fd.setAs(fieldAs.get(field));
			td.getBds().add(fd);
		}
	}

	private static void readMethods(final TD td,
			final EncodedMethod[] directMethods,
			final EncodedMethod[] virtualMethods,
			final Map<MethodIdItem, String> methodSignatures,
			final Map<MethodIdItem, String[]> methodException,
			final A annotationDefaultValues,
			final Map<MethodIdItem, A[]> methodAs,
			final Map<MethodIdItem, A[][]> methodParamAs) {
		for (int i = 0; i < directMethods.length; ++i) {
			final EncodedMethod encodedMethod = directMethods[i];
			final MethodIdItem method = encodedMethod.method;

			// getResourceAsStream :
			// (Ljava/lang/String;)Ljava/io/InputStream;
			final MD md = new MD(td, encodedMethod.accessFlags, method
					.getMethodName().getStringValue(), method.getPrototype()
					.getPrototypeString(), methodSignatures.get(method),
					methodException.get(method));
			// no annotation default values
			md.setAs(methodAs.get(method));

			final CodeItem codeItem = encodedMethod.codeItem;
			if (codeItem != null && false) {
				System.out.println("M " + method.getMethodString());
				readCode(codeItem);
			}

			td.getBds().add(md);
		}
		for (int i = 0; i < virtualMethods.length; ++i) {
			final EncodedMethod encodedMethod = virtualMethods[i];
			final MethodIdItem method = encodedMethod.method;

			// getResourceAsStream :
			// (Ljava/lang/String;)Ljava/io/InputStream;
			final MD md = new MD(td, encodedMethod.accessFlags, method
					.getMethodName().getStringValue(), method.getPrototype()
					.getPrototypeString(), methodSignatures.get(method),
					methodException.get(method));
			if (annotationDefaultValues != null) {
				md.setAnnotationDefaultValue(annotationDefaultValues
						.getMemberValue(md.getName()));
			}
			md.setAs(methodAs.get(method));
			md.setParamAs(methodParamAs.get(method));

			final CodeItem codeItem = encodedMethod.codeItem;
			if (codeItem != null && false) {
				System.out.println("M " + method.getMethodString());
				readCode(codeItem);
			}

			td.getBds().add(md);
		}
	}

}