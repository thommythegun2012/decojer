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
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BB;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;
import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.DataType;
import org.decojer.cavaj.model.vm.intermediate.Try;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.INVOKE;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.reader.smali.ReadDebugInfo;
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
import org.jf.dexlib.CodeItem.EncodedTypeAddrPair;
import org.jf.dexlib.CodeItem.TryItem;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.EncodedArrayItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.Instruction10t;
import org.jf.dexlib.Code.Format.Instruction11n;
import org.jf.dexlib.Code.Format.Instruction11x;
import org.jf.dexlib.Code.Format.Instruction12x;
import org.jf.dexlib.Code.Format.Instruction21c;
import org.jf.dexlib.Code.Format.Instruction21s;
import org.jf.dexlib.Code.Format.Instruction21t;
import org.jf.dexlib.Code.Format.Instruction22b;
import org.jf.dexlib.Code.Format.Instruction22s;
import org.jf.dexlib.Code.Format.Instruction22t;
import org.jf.dexlib.Code.Format.Instruction23x;
import org.jf.dexlib.Code.Format.Instruction35c;
import org.jf.dexlib.Debug.DebugInstructionIterator;
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
		read(is, new DU(), null);
	}

	/**
	 * Read DEX input stream.
	 * 
	 * @param is
	 *            DEX input stream
	 * @param du
	 *            decompilation unit
	 * @param selector
	 *            selector
	 * @return type declaration for selector
	 * @throws IOException
	 *             read exception
	 */
	@SuppressWarnings("unchecked")
	public static TD read(final InputStream is, final DU du,
			final String selector) throws IOException {
		String selectorPrefix = null;
		String selectorMatch = null;
		if (selector != null && selector.endsWith(".class")) {
			selectorMatch = "L"
					+ selector.substring(selector.charAt(0) == '/' ? 1 : 0,
							selector.length() - 6) + ";";
			final int pos = selectorMatch.lastIndexOf('/');
			if (pos != -1) {
				selectorPrefix = selectorMatch.substring(0, pos + 1);
			}
		}
		TD selectorTd = null;

		final byte[] bytes = IOUtils.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), true,
				false);
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			final String typeDescriptor = classDefItem.getClassType()
					.getTypeDescriptor();
			if (selectorPrefix != null
					&& (!typeDescriptor.startsWith(selectorPrefix) || typeDescriptor
							.indexOf('/', selectorPrefix.length()) != -1)) {
				continue;
			}
			final T t = du.getDescT(classDefItem.getClassType()
					.getTypeDescriptor());
			t.setAccessFlags(classDefItem.getAccessFlags());
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

			if (typeDescriptor.equals(selectorMatch)) {
				selectorTd = td;
			}

			A annotationDefaultValues = null;
			final Map<FieldIdItem, String> fieldSignatures = new HashMap<FieldIdItem, String>();
			final Map<FieldIdItem, A[]> fieldAs = new HashMap<FieldIdItem, A[]>();
			final Map<MethodIdItem, T[]> methodThrowsTs = new HashMap<MethodIdItem, T[]>();
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
						final A a = readAnnotation(annotation, du);
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
							t.setSignature(sb.toString());
							continue;
						}
						if ("dalvik.annotation.EnclosingClass".equals(a.getT()
								.getName())) {
							// is anonymous class, is in field initializer
							td.setEnclosingT((T) a.getMemberValue());
							continue;
						}
						if ("dalvik.annotation.EnclosingMethod".equals(a.getT()
								.getName())) {
							// is anonymous class, is in method
							td.setEnclosingM((M) a.getMemberValue());
							continue;
						}
						// Dalvik has not all inner class info from JVM:
						// outer class info not known in Dalvik and derivable,
						// no access flags for member classes,
						// no info for arbitrary accessed inner classes

						// InnerClass for
						// 'org.decojer.cavaj.test.DecTestInner$Inner1$Inner11':
						// dalvik.annotation.InnerClass accessFlags=20
						// name=Inner11
						// InnerClass for
						// 'org.decojer.cavaj.test.DecTestInner$Inner1':
						// dalvik.annotation.InnerClass accessFlags=1
						// name=Inner1
						// MemberClasses for
						// 'org.decojer.cavaj.test.DecTestInner$Inner1':
						// dalvik.annotation.MemberClasses
						// value=[Ljava.lang.Object;@170bea5
						// org.decojer.cavaj.test.DecTestInner$Inner1$Inner11
						if ("dalvik.annotation.InnerClass".equals(a.getT()
								.getName())) {
							// is inner type, this attributes is senseless?
							// inner name from naming rules and flags are known
							continue;
						}
						if ("dalvik.annotation.MemberClasses".equals(a.getT()
								.getName())) {
							// has member types (really contained inner classes)
							final Object[] memberValue = (Object[]) a
									.getMemberValue();
							final T[] memberTs = new T[memberValue.length];
							System.arraycopy(memberValue, 0, memberTs, 0,
									memberValue.length);
							td.setMemberTs(memberTs);
							continue;
						}
						as.add(a);
					}
					if (as.size() > 0) {
						td.setAs(as.toArray(new A[as.size()]));
					}
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
									final A a = readAnnotation(annotationItem,
											du);
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
									final A a = readAnnotation(annotationItem,
											du);
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
										final T[] throwsTs = new T[throwables.length];
										for (int i = throwables.length; i-- > 0;) {
											throwsTs[i] = (T) throwables[i];
										}
										methodThrowsTs.put(method, throwsTs);
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
										paramAs[j] = readAnnotation(
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
						methodThrowsTs, annotationDefaultValues, methodAs,
						methodParamAs);
			}

			du.addTd(td);
		}
		return selectorTd;
	}

	private static A readAnnotation(
			final AnnotationEncodedSubValue encodedValue,
			final RetentionPolicy retentionPolicy, final DU du) {
		final T t = du
				.getDescT(encodedValue.annotationType.getTypeDescriptor());
		final A a = new A(t, retentionPolicy);
		final StringIdItem[] names = encodedValue.names;
		final EncodedValue[] values = encodedValue.values;
		for (int i = 0; i < names.length; ++i) {
			a.addMember(names[i].getStringValue(), readValue(values[i], du));
		}
		return a;
	}

	private static A readAnnotation(final AnnotationItem annotationItem,
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
		return readAnnotation(annotationItem.getEncodedAnnotation(),
				retentionPolicy, du);
	}

	private static void readCode(final MD md, final CodeItem codeItem) {
		final M m = md.getM();
		final DU du = m.getT().getDu();

		HashMap<Integer, Integer> opLines = null;

		final DebugInfoItem debugInfo = codeItem.getDebugInfo();
		if (debugInfo != null) {
			final StringIdItem[] parameterNames = debugInfo.getParameterNames();
			if (parameterNames != null && parameterNames.length > 0) {
				final String[] paramNames = new String[parameterNames.length];
				for (int i = parameterNames.length; i-- > 0;) {
					if (parameterNames[i] == null) {
						// could happen, e.g. synthetic methods, inner <init>
						// with outer type param
						continue;
					}
					paramNames[i] = parameterNames[i].getStringValue();
				}
				m.setParamNames(paramNames);
			}
			final ReadDebugInfo readDebugInfo = new ReadDebugInfo(du);
			DebugInstructionIterator.DecodeInstructions(debugInfo,
					codeItem.getRegisterCount(), readDebugInfo);
			opLines = readDebugInfo.getOpLines();
			if (readDebugInfo.getVars() != null) {
				// TODO end local sometimes not called, optimization? set end pc
				md.setVars(readDebugInfo.getVars());
			}
		}

		// init CFG with start BB
		final CFG cfg = new CFG(md, codeItem.getRegisterCount(), -1);
		md.setCFG(cfg);

		final Instruction[] instructions = codeItem.getInstructions();

		// 2 free to use work register, 3 parameter
		// static: (5 register)
		// work_register1...work_register_2...param1...param2...param3
		// dynamic: (6 register)
		// work_register1...work_register_2...this...param1...param2...param3

		final BB bb = cfg.getStartBb();

		for (int opPc = 0, opLine = -1, i = 0; i < instructions.length; ++i) {
			final Instruction instruction = instructions[i];

			final int opCode = instruction.opcode.value;
			if (opLines != null && opLines.containsKey(opPc)) {
				// opLine remains constant with increasing opPc till new info is
				// available
				opLine = opLines.get(opPc);
			}

			System.out.println("I" + opPc + " (" + opLine + "): "
					+ instruction.opcode + "     "
					+ instruction.getClass().getName());

			int type = -1;
			int iValue = Integer.MIN_VALUE;
			final Object oValue = null;

			switch (instruction.opcode) {
			case ADD_DOUBLE:
			case ADD_FLOAT:
			case ADD_INT:
			case ADD_LONG: {
				// C := A + B
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case ADD_DOUBLE_2ADDR:
			case ADD_FLOAT_2ADDR:
			case ADD_INT_2ADDR:
			case ADD_LONG_2ADDR: {
				// A := A + B
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			case ADD_INT_LIT8: {
				// B := A + literal
				final Instruction22b instr = (Instruction22b) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case ADD_INT_LIT16: {
				// B := A + literal
				final Instruction22s instr = (Instruction22s) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case AGET:
			case AGET_BOOLEAN:
			case AGET_BYTE:
			case AGET_CHAR:
			case AGET_OBJECT:
			case AGET_SHORT:
			case AGET_WIDE: {
				// A := B[C]
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case APUT:
			case APUT_BOOLEAN:
			case APUT_BYTE:
			case APUT_CHAR:
			case APUT_OBJECT:
			case APUT_SHORT:
			case APUT_WIDE: {
				// B[C] := A
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case CONST_4: {
				// A := literal
				final Instruction11n instr = (Instruction11n) instruction;
				System.out.println("  refItem: " + instr.getLiteral() + "  A: "
						+ instr.getRegisterA());
				bb.addOperation(new PUSH(opPc, opCode, opLine, DataType.T_INT,
						(int) instr.getLiteral()));
				bb.addOperation(new STORE(opPc, opCode, opLine, DataType.T_INT,
						instr.getRegisterA(), null, null));
				break;
			}
			case CONST_16:
			case CONST_WIDE_16: /* long */{
				// A := literal
				final Instruction21s instr = (Instruction21s) instruction;
				System.out.println("  refItem: " + instr.getLiteral() + "  A: "
						+ instr.getRegisterA());
				break;
			}
			case CONST_STRING: {
				// A := refItem
				final Instruction21c instr = (Instruction21c) instruction;
				System.out.println("  refItem: " + instr.getReferencedItem()
						+ "  A: " + instr.getRegisterA());
				final StringIdItem stringIdItem = (StringIdItem) instr
						.getReferencedItem();
				bb.addOperation(new PUSH(opPc, opCode, opLine,
						DataType.T_STRING, stringIdItem.getStringValue()));
				bb.addOperation(new STORE(opPc, opCode, opLine,
						DataType.T_STRING, instr.getRegisterA(), null, null));
				break;
			}
			case DIV_DOUBLE:
			case DIV_FLOAT:
			case DIV_INT:
			case DIV_LONG: {
				// C := A / B
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case DIV_DOUBLE_2ADDR:
			case DIV_FLOAT_2ADDR:
			case DIV_INT_2ADDR:
			case DIV_LONG_2ADDR: {
				// A := A / B
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			case DIV_INT_LIT8: {
				// B := A / literal
				final Instruction22b instr = (Instruction22b) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case DIV_INT_LIT16: {
				// B := A / literal
				final Instruction22s instr = (Instruction22s) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case GOTO: {
				final Instruction10t instr = (Instruction10t) instruction;
				if (instr.getTargetAddressOffset() >= 0) {
					LOGGER.warning("Positive GOTO offset is uncommon in dalvik?!");
				}
				System.out.println("  targetOff: "
						+ instr.getTargetAddressOffset());
				break;
			}
			case IF_EQ:
			case IF_GE:
			case IF_GT:
			case IF_LE:
			case IF_LT:
			case IF_NE: {
				// IF A cond B JMP offset
				final Instruction22t instr = (Instruction22t) instruction;
				// offset can be negative and positive
				System.out
						.println("  targetOff: "
								+ instr.getTargetAddressOffset() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case IF_EQZ:
				type = DataType.T_INT;
				iValue = CompareType.T_EQ;
				// fall through
			case IF_GEZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case IF_GTZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case IF_LEZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case IF_LTZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case IF_NEZ: {
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_NE;
				}
				// IF A cond 0 JMP offset
				final Instruction21t instr = (Instruction21t) instruction;
				// offset can be negative and positive
				System.out.println("  targetOff: "
						+ instr.getTargetAddressOffset() + "  A: "
						+ instr.getRegisterA());

				// TODO this and GOTO...than test(ZZZ) should work

				break;
			}
			case INT_TO_BYTE:
			case INT_TO_CHAR:
			case INT_TO_DOUBLE:
			case INT_TO_FLOAT:
			case INT_TO_LONG:
			case INT_TO_SHORT: {
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			case INVOKE_DIRECT:
				// constructor or supermethod callout
			case INVOKE_INTERFACE:
			case INVOKE_STATIC:
			case INVOKE_VIRTUAL: {
				final Instruction35c instr = (Instruction35c) instruction;
				System.out.print("  " + instr.getReferencedItem() + " : "
						+ instr.getRegCount());
				System.out.print("  (D: " + instr.getRegisterD());
				System.out.print("  E: " + instr.getRegisterE());
				System.out.print("  F: " + instr.getRegisterF());
				System.out.print("  G: " + instr.getRegisterG());
				System.out.println("  A: " + instr.getRegisterA() + ")");
				if (instruction.opcode != Opcode.INVOKE_STATIC) {

				}
				final MethodIdItem methodIdItem = (MethodIdItem) instr
						.getReferencedItem();
				final T invokeT = du.getDescT(methodIdItem.getContainingClass()
						.getTypeDescriptor());
				if (instruction.opcode == Opcode.INVOKE_INTERFACE) {
					invokeT.markAf(AF.INTERFACE);
				}
				final M invokeM = invokeT.getM(methodIdItem.getMethodName()
						.getStringValue(), methodIdItem.getPrototype()
						.getPrototypeString());
				if (instruction.opcode == Opcode.INVOKE_STATIC) {
					invokeM.markAf(AF.STATIC);
				}
				if (instr.getRegCount() > 0) {
					bb.addOperation(new LOAD(opPc, opCode, opLine, -1, instr
							.getRegisterD(), null, null));
				}
				if (instr.getRegCount() > 1) {
					bb.addOperation(new LOAD(opPc, opCode, opLine, -1, instr
							.getRegisterE(), null, null));
				}
				if (instr.getRegCount() > 2) {
					bb.addOperation(new LOAD(opPc, opCode, opLine, -1, instr
							.getRegisterF(), null, null));
				}
				if (instr.getRegCount() > 3) {
					bb.addOperation(new LOAD(opPc, opCode, opLine, -1, instr
							.getRegisterG(), null, null));
				}
				if (instr.getRegCount() > 4) {
					bb.addOperation(new LOAD(opPc, opCode, opLine, -1, instr
							.getRegisterA(), null, null));
				}
				bb.addOperation(new INVOKE(opPc, opCode, opLine, invokeM,
						instruction.opcode == Opcode.INVOKE_DIRECT));
				break;
			}
			case MOVE: {
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.print("  A: " + instr.getRegisterA());
				System.out.println("  B: " + instr.getRegisterB());
				bb.addOperation(new LOAD(opPc, opCode, opLine, -1, instr
						.getRegisterA(), null, null));
				bb.addOperation(new STORE(opPc, opCode, opLine, -1, instr
						.getRegisterB(), null, null));
				break;
			}
			case MOVE_RESULT:
			case MOVE_RESULT_OBJECT:
			case MOVE_RESULT_WIDE: {
				// A := resultRegister
				final Instruction11x instr = (Instruction11x) instruction;
				System.out.println("  A: " + instr.getRegisterA());
				break;
			}
			case MUL_DOUBLE:
			case MUL_FLOAT:
			case MUL_INT:
			case MUL_LONG: {
				// C := A * B
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case MUL_DOUBLE_2ADDR:
			case MUL_FLOAT_2ADDR:
			case MUL_INT_2ADDR:
			case MUL_LONG_2ADDR: {
				// A := A * B
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			case MUL_INT_LIT8: {
				// B := A * literal
				final Instruction22b instr = (Instruction22b) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case MUL_INT_LIT16: {
				// B := A * literal
				final Instruction22s instr = (Instruction22s) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case NEW_INSTANCE: {
				final Instruction21c instr = (Instruction21c) instruction;
				System.out.println("  refItem: " + instr.getReferencedItem()
						+ "  A: " + instr.getRegisterA());
				break;
			}
			case RETURN:
			case RETURN_OBJECT:
			case RETURN_WIDE: {
				final Instruction11x instr = (Instruction11x) instruction;
				System.out.println("  A: " + instr.getRegisterA());
				break;
			}
			case RETURN_VOID: {
				cfg.getStartBb().addOperation(
						new RETURN(opPc, opCode, opLine, DataType.T_VOID));
				break;
			}
			case SGET_OBJECT: {
				final Instruction21c instr = (Instruction21c) instruction;
				System.out.println("  " + instr.getReferencedItem() + "  A: "
						+ instr.getRegisterA());
				final FieldIdItem fieldIdItem = (FieldIdItem) instr
						.getReferencedItem();
				final T fieldRefT = du.getDescT(fieldIdItem
						.getContainingClass().getTypeDescriptor());
				final T fieldValueT = du.getDescT(fieldIdItem.getFieldType()
						.getTypeDescriptor());
				final F f = fieldRefT.getF(fieldIdItem.getFieldName()
						.getStringValue(), fieldValueT);
				f.markAf(AF.STATIC);
				bb.addOperation(new GET(opPc, opCode, opLine, f));
				bb.addOperation(new STORE(opPc, opCode, opLine,
						DataType.T_AREF, instr.getRegisterA(), null, null));
				break;
			}
			case SUB_DOUBLE:
			case SUB_FLOAT:
			case SUB_INT:
			case SUB_LONG: {
				// C := A - B
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case SUB_DOUBLE_2ADDR:
			case SUB_FLOAT_2ADDR:
			case SUB_INT_2ADDR:
			case SUB_LONG_2ADDR: {
				// A := A - B
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			case XOR_INT:
			case XOR_LONG: {
				// C := A ^ B
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case XOR_INT_2ADDR:
			case XOR_LONG_2ADDR: {
				// A := A ^ B
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			case XOR_INT_LIT8: {
				// B := A ^ literal
				final Instruction22b instr = (Instruction22b) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			case XOR_INT_LIT16: {
				// B := A ^ literal
				final Instruction22s instr = (Instruction22s) instruction;
				System.out
						.println("  literal: " + instr.getLiteral() + "  A: "
								+ instr.getRegisterA() + "  B: "
								+ instr.getRegisterB());
				break;
			}
			}
			opPc += instruction.getSize(opPc);
		}
		cfg.calculatePostorder();

		final TryItem[] tryItems = codeItem.getTries();
		if (tryItems != null && tryItems.length > 0) {
			final Try[] tries = new Try[tryItems.length];
			for (int i = tryItems.length; i-- > 0;) {
				final TryItem tryItem = tryItems[i];
				final Try tryy = new Try(tryItem.getStartCodeAddress(),
						tryItem.getStartCodeAddress() + tryItem.getTryLength());
				final HashMap<T, Integer> catches = tryy.getCatches();
				for (final EncodedTypeAddrPair handler : tryItem.encodedCatchHandler.handlers) {
					final T catchT = du.getDescT(handler.exceptionType
							.getTypeDescriptor());
					catches.put(catchT, handler.getHandlerAddress());
				}
				if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() != -1) {
					catches.put(null, tryItem.encodedCatchHandler
							.getCatchAllHandlerAddress());
				}
				tries[i] = tryy;
			}
			md.setTries(tries);
		}
	}

	private static void readFields(final TD td,
			final EncodedField[] staticFields,
			final EncodedField[] instanceFields,
			final Map<FieldIdItem, String> fieldSignatures,
			final EncodedArrayItem staticFieldInitializers,
			final Map<FieldIdItem, A[]> fieldAs) {
		final T t = td.getT();
		final DU du = t.getDu();

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

			final T fieldT = du.getDescT(field.getFieldType()
					.getTypeDescriptor());
			final F f = t.getF(field.getFieldName().getStringValue(), fieldT);
			f.setAccessFlags(encodedField.accessFlags);
			if (fieldSignatures.get(field) != null) {
				f.setSignature(fieldSignatures.get(field));
			}

			final FD fd = new FD(f, td);
			if (staticFieldValues.length > i) {
				fd.setValue(readValue(staticFieldValues[i], du));
			}

			fd.setAs(fieldAs.get(field));
			td.getBds().add(fd);
		}
		for (int i = 0; i < instanceFields.length; ++i) {
			final EncodedField encodedField = instanceFields[i];
			final FieldIdItem field = encodedField.field;

			final T fieldT = du.getDescT(field.getFieldType()
					.getTypeDescriptor());
			final F f = t.getF(field.getFieldName().getStringValue(), fieldT);
			f.setAccessFlags(encodedField.accessFlags);
			if (fieldSignatures.get(field) != null) {
				f.setSignature(fieldSignatures.get(field));
			}

			final FD fd = new FD(f, td);
			// there is no field initializer section for instance fields,
			// only via constructor

			fd.setAs(fieldAs.get(field));
			td.getBds().add(fd);
		}
	}

	private static void readMethods(final TD td,
			final EncodedMethod[] directMethods,
			final EncodedMethod[] virtualMethods,
			final Map<MethodIdItem, String> methodSignatures,
			final Map<MethodIdItem, T[]> methodThrowsTs,
			final A annotationDefaultValues,
			final Map<MethodIdItem, A[]> methodAs,
			final Map<MethodIdItem, A[][]> methodParamAs) {
		final T t = td.getT();

		for (int i = 0; i < directMethods.length; ++i) {
			final EncodedMethod encodedMethod = directMethods[i];
			final MethodIdItem method = encodedMethod.method;

			// getResourceAsStream :
			// (Ljava/lang/String;)Ljava/io/InputStream;
			final M m = t.getM(method.getMethodName().getStringValue(), method
					.getPrototype().getPrototypeString());
			m.setAccessFlags(encodedMethod.accessFlags);
			m.setThrowsTs(methodThrowsTs.get(method));
			m.setSignature(methodSignatures.get(method));

			final MD md = new MD(m, td);

			// no annotation default values

			md.setAs(methodAs.get(method));
			md.setParamAs(methodParamAs.get(method));

			final CodeItem codeItem = encodedMethod.codeItem;
			if (codeItem != null) {
				System.out.println("##### " + method.getMethodString());
				readCode(md, codeItem);
			}

			td.getBds().add(md);
		}
		for (int i = 0; i < virtualMethods.length; ++i) {
			final EncodedMethod encodedMethod = virtualMethods[i];
			final MethodIdItem method = encodedMethod.method;

			// getResourceAsStream :
			// (Ljava/lang/String;)Ljava/io/InputStream;
			final M m = t.getM(method.getMethodName().getStringValue(), method
					.getPrototype().getPrototypeString());
			m.setAccessFlags(encodedMethod.accessFlags);
			m.setThrowsTs(methodThrowsTs.get(method));
			m.setSignature(methodSignatures.get(method));

			final MD md = new MD(m, td);

			if (annotationDefaultValues != null) {
				md.setAnnotationDefaultValue(annotationDefaultValues
						.getMemberValue(md.getM().getName()));
			}

			md.setAs(methodAs.get(method));
			md.setParamAs(methodParamAs.get(method));

			final CodeItem codeItem = encodedMethod.codeItem;
			if (codeItem != null) {
				System.out.println("##### " + method.getMethodString());
				readCode(md, codeItem);
			}

			td.getBds().add(md);
		}
	}

	private static Object readValue(final EncodedValue encodedValue, final DU du) {
		if (encodedValue instanceof AnnotationEncodedSubValue) {
			// retention unknown for annotation constant
			return readAnnotation((AnnotationEncodedValue) encodedValue, null,
					du);
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
			final T enumT = du.getDescT(fieldidItem.getFieldType()
					.getTypeDescriptor());
			final F enumF = enumT.getF(fieldidItem.getFieldName()
					.getStringDataItem().getStringValue(), enumT);
			enumF.markAf(AF.ENUM);
			return enumF;
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
			final T t = du.getDescT(methodIdItem.getContainingClass()
					.getTypeDescriptor());
			return t.getM(methodIdItem.getMethodName().getStringValue(),
					methodIdItem.getPrototype().getPrototypeString());
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

}