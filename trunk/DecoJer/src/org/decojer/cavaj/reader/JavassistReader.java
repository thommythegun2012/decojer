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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
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
import javassist.bytecode.Opcode;
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
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.ALOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.AND;
import org.decojer.cavaj.model.vm.intermediate.operations.ARRAYLENGTH;
import org.decojer.cavaj.model.vm.intermediate.operations.ASTORE;
import org.decojer.cavaj.model.vm.intermediate.operations.CHECKCAST;
import org.decojer.cavaj.model.vm.intermediate.operations.CMP;
import org.decojer.cavaj.model.vm.intermediate.operations.CONVERT;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.DUP;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;
import org.decojer.cavaj.model.vm.intermediate.operations.INC;
import org.decojer.cavaj.model.vm.intermediate.operations.INSTANCEOF;
import org.decojer.cavaj.model.vm.intermediate.operations.INVOKE;
import org.decojer.cavaj.model.vm.intermediate.operations.JCMP;
import org.decojer.cavaj.model.vm.intermediate.operations.JCND;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.MONITOR;
import org.decojer.cavaj.model.vm.intermediate.operations.MUL;
import org.decojer.cavaj.model.vm.intermediate.operations.NEG;
import org.decojer.cavaj.model.vm.intermediate.operations.NEW;
import org.decojer.cavaj.model.vm.intermediate.operations.NEWARRAY;
import org.decojer.cavaj.model.vm.intermediate.operations.OR;
import org.decojer.cavaj.model.vm.intermediate.operations.POP;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.PUT;
import org.decojer.cavaj.model.vm.intermediate.operations.REM;
import org.decojer.cavaj.model.vm.intermediate.operations.RET;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWAP;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.THROW;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;

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
				LOGGER.warning("Unknown class attribute tag '" + attributeTag
						+ "'!");
			}
		}

		final T t = du.getT(classFile.getName());
		t.setAccessFlags(classFile.getAccessFlags());
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
		final DU du = md.getTd().getT().getDu();

		final boolean isStatic = m.checkAf(AF.STATIC);

		if (localVariableAttribute != null) {
			final int params = m.getParamTs().length;
			final String[] paramNames = new String[params];
			for (int i = localVariableAttribute.tableLength(); i-- > 0;) {
				int index = localVariableAttribute.index(i);
				if (!isStatic) {
					// index == 0 is "this" for non-statics
					--index;
				}
				if (index < 0) {
					continue;
				}
				if (index < paramNames.length) {
					paramNames[index] = localVariableAttribute.variableName(i);
				}
				// TODO where goes the other stuff, local variables?
			}
			m.setParamNames(paramNames);
		}

		class CodeReader {

			final byte[] code;

			int pc;

			/**
			 * Constructor.
			 * 
			 */
			CodeReader(final byte[] code) {
				this.code = code;
			}

			int readSignedByte() {
				return this.code[this.pc++];
			}

			int readSignedInt() {
				return this.code[this.pc++] << 24
						| (this.code[this.pc++] & 0xff) << 16
						| (this.code[this.pc++] & 0xff) << 8
						| this.code[this.pc++] & 0xff;
			}

			int readSignedShort() {
				return this.code[this.pc++] << 8 | this.code[this.pc++] & 0xff;
			}

			int readUnsignedByte() {
				return this.code[this.pc++] & 0xff;
			}

			int readUnsignedInt() {
				return (this.code[this.pc++] & 0xff) << 24
						| (this.code[this.pc++] & 0xff) << 16
						| (this.code[this.pc++] & 0xff) << 8
						| this.code[this.pc++] & 0xff;
			}

			int readUnsignedShort() {
				return (this.code[this.pc++] & 0xff) << 8
						| this.code[this.pc++] & 0xff;
			}

		}

		final CodeReader codeReader = new CodeReader(codeAttribute.getCode());

		// init CFG with start BB
		final CFG cfg = new CFG(md);
		md.setCFG(cfg);

		final ConstPool constPool = codeAttribute.getConstPool();

		// setup loop

		// start with pc = 0
		codeReader.pc = 0;
		// start with this basic block, may not remain the start basic block
		// (splitting)
		BB bb = cfg.getStartBb();
		// remember visited pcs via BBNode
		final Map<Integer, BB> pcBbs = new HashMap<Integer, BB>();
		// remember open pcs
		final Stack<Integer> openPcs = new Stack<Integer>();

		// wide operation following?
		// one of: iload, fload, aload, lload, dload, istore, fstore, astore,
		// lstore, dstore, or ret
		boolean wide = false;

		while (true) {
			// next open pc?
			if (codeReader.pc >= codeReader.code.length) {
				if (openPcs.isEmpty()) {
					break;
				}
				codeReader.pc = openPcs.pop();
				bb = pcBbs.get(codeReader.pc);
			} else {
				// next pc allready in flow?
				final BB nextBB = cfg.getTargetBb(codeReader.pc, pcBbs);
				if (nextBB != null) {
					bb.addSucc(nextBB, null);
					codeReader.pc = codeReader.code.length; // next open pc
					continue;
				}
				pcBbs.put(codeReader.pc, bb);
			}
			final int opPc = codeReader.pc;
			final int opCode = codeReader.readUnsignedByte();
			final int opLine = lineNumberAttribute == null ? -1
					: lineNumberAttribute.toLineNumber(opPc);

			int type = -1;
			int iValue = Integer.MIN_VALUE;
			Object oValue = null;

			switch (opCode) {
			case Opcode.NOP:
				// nothing to do, ignore
				break;
			/*******
			 * ADD *
			 *******/
			case Opcode.DADD:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FADD:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IADD:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LADD:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new ADD(opPc, opCode, opLine, type));
				break;
			/*********
			 * ALOAD *
			 *********/
			case Opcode.AALOAD:
				type = DataType.T_AREF;
				// fall through
			case Opcode.BALOAD:
				if (type < 0) {
					type = DataType.T_BOOLEAN;
				}
				// fall through
			case Opcode.CALOAD:
				if (type < 0) {
					type = DataType.T_CHAR;
				}
				// fall through
			case Opcode.DALOAD:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FALOAD:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IALOAD:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LALOAD:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// fall through
			case Opcode.SALOAD:
				if (type < 0) {
					type = DataType.T_SHORT;
				}
				bb.addOperation(new ALOAD(opPc, opCode, opLine, type));
				break;
			/*******
			 * AND *
			 *******/
			case Opcode.IAND:
				type = DataType.T_INT;
				// fall through
			case Opcode.LAND:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new AND(opPc, opCode, opLine, type));
				break;
			/***************
			 * ARRAYLENGTH *
			 ***************/
			case Opcode.ARRAYLENGTH:
				bb.addOperation(new ARRAYLENGTH(opPc, opCode, opLine));
				break;
			/**********
			 * ASTORE *
			 **********/
			case Opcode.AASTORE:
				type = DataType.T_AREF;
				// fall through
			case Opcode.BASTORE:
				if (type < 0) {
					type = DataType.T_BOOLEAN;
				}
				// fall through
			case Opcode.CASTORE:
				if (type < 0) {
					type = DataType.T_CHAR;
				}
				// fall through
			case Opcode.DASTORE:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FASTORE:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IASTORE:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LASTORE:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// fall through
			case Opcode.SASTORE:
				if (type < 0) {
					type = DataType.T_SHORT;
				}
				bb.addOperation(new ASTORE(opPc, opCode, opLine, type));
				break;
			/***********
			 * CONVERT *
			 ***********/
			case Opcode.D2F:
				type = DataType.T_DOUBLE;
				iValue = DataType.T_FLOAT;
				// fall through
			case Opcode.D2I:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = DataType.T_INT;
				}
				// fall through
			case Opcode.D2L:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = DataType.T_LONG;
				}
				// fall through
			case Opcode.F2D:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.F2I:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = DataType.T_INT;
				}
				// fall through
			case Opcode.F2L:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = DataType.T_LONG;
				}
				// fall through
			case Opcode.I2B:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_BYTE;
				}
				// fall through
			case Opcode.I2C:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_CHAR;
				}
				// fall through
			case Opcode.I2D:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.I2F:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.I2L:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_LONG;
				}
				// fall through
			case Opcode.I2S:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = DataType.T_SHORT;
				}
				// fall through
			case Opcode.L2D:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.L2F:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.L2I:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = DataType.T_INT;
				}
				bb.addOperation(new CONVERT(opPc, opCode, opLine, type, iValue));
				break;
			/*******
			 * CMP *
			 *******/
			case Opcode.DCMPG:
				type = DataType.T_DOUBLE;
				iValue = CMP.T_G;
				// fall through
			case Opcode.DCMPL:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = CMP.T_L;
				}
				// fall through
			case Opcode.FCMPG:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = CMP.T_G;
				}
				// fall through
			case Opcode.FCMPL:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = CMP.T_L;
				}
				// fall through
			case Opcode.LCMP:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = CMP.T_0;
				}
				bb.addOperation(new CMP(opPc, opCode, opLine, type, iValue));
				break;
			/*******
			 * DIV *
			 *******/
			case Opcode.DDIV:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FDIV:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IDIV:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LDIV:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new DIV(opPc, opCode, opLine, type));
				break;
			/*******
			 * DUP *
			 *******/
			case Opcode.DUP:
				type = DUP.T_DUP;
				// fall through
			case Opcode.DUP_X1:
				if (type < 0) {
					type = DUP.T_DUP_X1;
				}
				// fall through
			case Opcode.DUP_X2:
				if (type < 0) {
					type = DUP.T_DUP_X2;
				}
				// fall through
			case Opcode.DUP2:
				if (type < 0) {
					type = DUP.T_DUP2;
				}
				// fall through
			case Opcode.DUP2_X1:
				if (type < 0) {
					type = DUP.T_DUP2_X1;
				}
				// fall through
			case Opcode.DUP2_X2:
				if (type < 0) {
					type = DUP.T_DUP2_X2;
				}
				bb.addOperation(new DUP(opPc, opCode, opLine, type));
				break;
			/*******
			 * GET *
			 *******/
			case Opcode.GETFIELD:
				type = 0;
				// fall through
			case Opcode.GETSTATIC:
				if (type < 0) {
					type = 1;
				}
				{
					final int cpFieldIndex = codeReader.readUnsignedShort();

					final T fieldRefT = du.getT(constPool
							.getFieldrefClassName(cpFieldIndex));
					final T fieldValueT = du.getDescT(constPool
							.getFieldrefType(cpFieldIndex));
					final F f = fieldRefT.getF(
							constPool.getFieldrefName(cpFieldIndex),
							fieldValueT);
					if (type == 1) {
						f.markAf(AF.STATIC);
					}
					bb.addOperation(new GET(opPc, opCode, opLine, f));
				}
				break;
			/********
			 * GOTO *
			 ********/
			case Opcode.GOTO:
				type = 0;
				iValue = codeReader.readSignedShort();
				// fall through
			case Opcode.GOTO_W:
				if (type < 0) {
					iValue = codeReader.readSignedInt();
				}
				// not really necessary, but important for
				// 1) correct opPc blocks
				// 2) line numbers
				bb.addOperation(new GOTO(opPc, opCode, opLine));
				codeReader.pc = opPc + iValue;
				break;
			/*******
			 * INC *
			 *******/
			case Opcode.IINC: {
				final int varIndex = codeReader.readUnsignedByte();
				final int constValue = codeReader.readUnsignedByte();
				bb.addOperation(new INC(opPc, opCode, opLine, DataType.T_INT,
						varIndex, constValue));
			}
				break;
			/**********
			 * INVOKE *
			 **********/
			case Opcode.INVOKEINTERFACE: {
				type = INVOKE.T_INTERFACE;
				final int cpMethodIndex = codeReader.readUnsignedShort();
				codeReader.readUnsignedByte(); // count, unused
				codeReader.readUnsignedByte(); // reserved, unused

				final T invokeT = du.getT(constPool
						.getInterfaceMethodrefClassName(cpMethodIndex));
				final M invokeM = invokeT.getM(
						constPool.getInterfaceMethodrefName(cpMethodIndex),
						constPool.getInterfaceMethodrefType(cpMethodIndex));

				bb.addOperation(new INVOKE(opPc, opCode, opLine, type, invokeM));
			}
				break;
			case Opcode.INVOKESPECIAL:
				type = INVOKE.T_SPECIAL;
				// fall through
			case Opcode.INVOKESTATIC:
				if (type < 0) {
					type = INVOKE.T_STATIC;
				}
				// fall through
			case Opcode.INVOKEVIRTUAL:
				if (type < 0) {
					type = INVOKE.T_VIRTUAL;
				}
				{
					final int cpMethodIndex = codeReader.readUnsignedShort();

					final T invokeT = du.getT(constPool
							.getMethodrefClassName(cpMethodIndex));
					final M invokeM = invokeT.getM(
							constPool.getMethodrefName(cpMethodIndex),
							constPool.getMethodrefType(cpMethodIndex));

					bb.addOperation(new INVOKE(opPc, opCode, opLine, type,
							invokeM));
				}
				break;
			/********
			 * JCMP *
			 ********/
			case Opcode.IF_ACMPEQ:
				type = DataType.T_AREF;
				iValue = CompareType.T_EQ;
				// fall through
			case Opcode.IF_ACMPNE:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = CompareType.T_NE;
				}
				// fall through
			case Opcode.IF_ICMPEQ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_EQ;
				}
				// fall through
			case Opcode.IF_ICMPGE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case Opcode.IF_ICMPGT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case Opcode.IF_ICMPLE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case Opcode.IF_ICMPLT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case Opcode.IF_ICMPNE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_NE;
				}
				{
					final int branch = codeReader.readSignedShort();
					bb.addOperation(new JCMP(opPc, opCode, opLine, type, iValue));
					final int targetPc = opPc + branch;
					if (targetPc == codeReader.pc) {
						System.out.println("### BRANCH_IFCMP (Empty): "
								+ targetPc);
					} else {
						BB targetBB = cfg.getTargetBb(targetPc, pcBbs);
						if (targetBB == null) {
							targetBB = cfg.newBb(targetPc);
							pcBbs.put(targetPc, targetBB);
							openPcs.add(targetPc);
						}
						bb.addSucc(targetBB, Boolean.TRUE);
						BB nextBB = cfg.getTargetBb(codeReader.pc, pcBbs);
						if (nextBB == null) {
							nextBB = cfg.newBb(codeReader.pc);
						} else {
							codeReader.pc = codeReader.code.length; // next open
																	// pc
						}
						bb.addSucc(nextBB, Boolean.FALSE);
						bb = nextBB;
					}
				}
				break;
			/********
			 * JCND *
			 ********/
			case Opcode.IFNULL:
				type = DataType.T_AREF;
				iValue = CompareType.T_EQ;
				// fall through
			case Opcode.IFNONNULL:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = CompareType.T_NE;
				}
				// fall through
			case Opcode.IFEQ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_EQ;
				}
				// fall through
			case Opcode.IFGE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case Opcode.IFGT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case Opcode.IFLE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case Opcode.IFLT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case Opcode.IFNE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_NE;
				}
				{
					final int branch = codeReader.readSignedShort();
					bb.addOperation(new JCND(opPc, opCode, opLine, type, iValue));
					final int targetPc = opPc + branch;
					if (targetPc == codeReader.pc) {
						System.out
								.println("### BRANCH_IF (Empty): " + targetPc);
					} else {
						BB targetBB = cfg.getTargetBb(targetPc, pcBbs);
						if (targetBB == null) {
							targetBB = cfg.newBb(targetPc);
							pcBbs.put(targetPc, targetBB);
							openPcs.add(targetPc);
						}
						bb.addSucc(targetBB, Boolean.TRUE);
						BB nextBB = cfg.getTargetBb(codeReader.pc, pcBbs);
						if (nextBB == null) {
							nextBB = cfg.newBb(codeReader.pc);
						} else {
							codeReader.pc = codeReader.code.length; // next open
																	// pc
						}
						bb.addSucc(nextBB, Boolean.FALSE);
						bb = nextBB;
					}
				}
				break;
			/*******
			 * JSR *
			 *******/
			case Opcode.JSR:
				type = 0;
				iValue = codeReader.readUnsignedShort();
				// fall through
			case Opcode.JSR_W:
				if (type < 0) {
					iValue = codeReader.readUnsignedInt();
				}
				// TODO
				System.out.println("### JSR: " + iValue + " : "
						+ (opPc + iValue));
				break;
			/********
			 * LOAD *
			 ********/
			case Opcode.ALOAD:
				type = DataType.T_AREF;
				// fall through
			case Opcode.DLOAD:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FLOAD:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.ILOAD:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LLOAD:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// for all above
				iValue = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
				// fall through
			case Opcode.ALOAD_0:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 0;
				}
				// fall through
			case Opcode.DLOAD_0:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 0;
				}
				// fall through
			case Opcode.FLOAD_0:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 0;
				}
				// fall through
			case Opcode.ILOAD_0:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 0;
				}
				// fall through
			case Opcode.LLOAD_0:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 0;
				}
				// fall through
			case Opcode.ALOAD_1:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 1;
				}
				// fall through
			case Opcode.DLOAD_1:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 1;
				}
				// fall through
			case Opcode.FLOAD_1:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 1;
				}
				// fall through
			case Opcode.ILOAD_1:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 1;
				}
				// fall through
			case Opcode.LLOAD_1:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 1;
				}
				// fall through
			case Opcode.ALOAD_2:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 2;
				}
				// fall through
			case Opcode.DLOAD_2:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 2;
				}
				// fall through
			case Opcode.FLOAD_2:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 2;
				}
				// fall through
			case Opcode.ILOAD_2:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 2;
				}
				// fall through
			case Opcode.LLOAD_2:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 2;
				}
				// fall through
			case Opcode.ALOAD_3:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 3;
				}
				// fall through
			case Opcode.DLOAD_3:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 3;
				}
				// fall through
			case Opcode.FLOAD_3:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 3;
				}
				// fall through
			case Opcode.ILOAD_3:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 3;
				}
				// fall through
			case Opcode.LLOAD_3: {
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 3;
				}

				String varName = null;
				String varDescriptor = null;
				if (localVariableAttribute != null) {
					for (int i = 0; i < localVariableAttribute.tableLength(); ++i) {
						if (localVariableAttribute.index(i) == iValue
								&& localVariableAttribute.startPc(i) <= opPc
								&& localVariableAttribute.startPc(i)
										+ localVariableAttribute.codeLength(i) >= opPc) {
							varName = localVariableAttribute.variableName(i);
							varDescriptor = localVariableAttribute
									.descriptor(i);
							break;
						}
					}
				}
				if (varName == null) {
					if (iValue == 0 && !isStatic) {
						varName = "this";
					} else {
						varName = "arg" + iValue;
					}
				}
				final T varT = varDescriptor == null ? null : du
						.getDescT(varDescriptor);

				bb.addOperation(new LOAD(opPc, opCode, opLine, type, iValue,
						varName, varT));
				break;
			}
			/*******
			 * NEW *
			 *******/
			case Opcode.NEW: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				bb.addOperation(new NEW(opPc, opCode, opLine, readType(
						constPool, cpClassIndex, du)));
			}
				break;
			/************
			 * NEWARRAY *
			 ************/
			case Opcode.ANEWARRAY: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				bb.addOperation(new NEWARRAY(opPc, opCode, opLine, readType(
						constPool, cpClassIndex, du), 1));
			}
				break;
			case Opcode.NEWARRAY: {
				type = codeReader.readUnsignedByte();
				final String typeName = new String[] { null, null, null, null,
						boolean.class.getName(), char.class.getName(),
						float.class.getName(), double.class.getName(),
						byte.class.getName(), short.class.getName(),
						int.class.getName(), long.class.getName() }[type];
				bb.addOperation(new NEWARRAY(opPc, opCode, opLine, du
						.getT(typeName), 1));
			}
				break;
			case Opcode.MULTIANEWARRAY: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				final int dimensions = codeReader.readUnsignedByte();
				bb.addOperation(new NEWARRAY(opPc, opCode, opLine, readType(
						constPool, cpClassIndex, du), dimensions));
			}
				break;
			/********
			 * PUSH *
			 ********/
			case Opcode.ACONST_NULL:
				type = DataType.T_AREF;
				oValue = null;
				// fall through
			case Opcode.BIPUSH:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = codeReader.readSignedByte();
				}
				// fall through
			case Opcode.SIPUSH:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = codeReader.readSignedShort();
				}
				// fall through
			case Opcode.LDC:
				if (type < 0) {
					final int ldcValueIndex = codeReader.readUnsignedByte();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						type = DataType.T_CLASS;
						oValue = readType(constPool, ldcValueIndex, du);
						break;
					case ConstPool.CONST_Double:
						// Double / Long only with LDC2_W, but is OK here too
						type = DataType.T_DOUBLE;
						// fall through
					case ConstPool.CONST_Float:
						if (type < 0) {
							type = DataType.T_FLOAT;
						}
						// fall through
					case ConstPool.CONST_Integer:
						if (type < 0) {
							type = DataType.T_INT;
						}
						// fall through
					case ConstPool.CONST_Long:
						// Double / Long only with LDC2_W, but is OK here too
						if (type < 0) {
							type = DataType.T_LONG;
						}
						// fall through
					case ConstPool.CONST_String:
						if (type < 0) {
							type = DataType.T_STRING;
						}
						oValue = constPool.getLdcValue(ldcValueIndex);
						break;
					default:
						throw new RuntimeException("Unknown Const Pool Tag "
								+ tag + " for LDC!");
					}
				}
				// fall through
			case Opcode.LDC_W:
				// fall through
			case Opcode.LDC2_W:
				if (type < 0) {
					final int ldcValueIndex = codeReader.readUnsignedShort();
					final int tag = constPool.getTag(ldcValueIndex);
					switch (constPool.getTag(ldcValueIndex)) {
					case ConstPool.CONST_Class:
						type = DataType.T_CLASS;
						oValue = readType(constPool, ldcValueIndex, du);
						break;
					case ConstPool.CONST_Double:
						type = DataType.T_DOUBLE;
						// fall through
					case ConstPool.CONST_Float:
						if (type < 0) {
							type = DataType.T_FLOAT;
						}
						// fall through
					case ConstPool.CONST_Integer:
						if (type < 0) {
							type = DataType.T_INT;
						}
						// fall through
					case ConstPool.CONST_Long:
						if (type < 0) {
							type = DataType.T_LONG;
						}
						// fall through
					case ConstPool.CONST_String:
						if (type < 0) {
							type = DataType.T_STRING;
						}
						oValue = constPool.getLdcValue(ldcValueIndex);
						break;
					default:
						throw new RuntimeException("Unknown Const Pool Tag "
								+ tag + " for LDC!");
					}
				}
				// fall through
			case Opcode.DCONST_0:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					oValue = 0D;
				}
				// fall through
			case Opcode.FCONST_0:
				if (type < 0) {
					type = DataType.T_FLOAT;
					oValue = 0;
				}
				// fall through
			case Opcode.ICONST_0:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 0;
				}
				// fall through
			case Opcode.LCONST_0:
				if (type < 0) {
					type = DataType.T_LONG;
					oValue = 0L;
				}
				// fall through
			case Opcode.DCONST_1:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					oValue = 1D;
				}
				// fall through
			case Opcode.FCONST_1:
				if (type < 0) {
					type = DataType.T_FLOAT;
					oValue = 1;
				}
				// fall through
			case Opcode.ICONST_1:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 1;
				}
				// fall through
			case Opcode.LCONST_1:
				if (type < 0) {
					type = DataType.T_LONG;
					oValue = 1L;
				}
				// fall through
			case Opcode.FCONST_2:
				if (type < 0) {
					type = DataType.T_FLOAT;
					oValue = 2;
				}
				// fall through
			case Opcode.ICONST_2:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 2;
				}
				// fall through
			case Opcode.ICONST_3:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 3;
				}
				// fall through
			case Opcode.ICONST_4:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 4;
				}
				// fall through
			case Opcode.ICONST_5:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = 5;
				}
				// fall through
			case Opcode.ICONST_M1:
				if (type < 0) {
					type = DataType.T_INT;
					oValue = -1;
				}
				bb.addOperation(new PUSH(opPc, opCode, opLine, type, oValue));
				break;
			/*******
			 * PUT *
			 *******/
			case Opcode.PUTFIELD:
				type = PUT.T_DYNAMIC;
				// fall through
			case Opcode.PUTSTATIC:
				if (type < 0) {
					type = PUT.T_STATIC;
				}
				{
					final int cpFieldIndex = codeReader.readUnsignedShort();
					final String fieldrefClassName = constPool
							.getFieldrefClassName(cpFieldIndex);
					final String fieldrefName = constPool
							.getFieldrefName(cpFieldIndex);
					final String fieldrefType = constPool
							.getFieldrefType(cpFieldIndex);
					bb.addOperation(new PUT(opPc, opCode, opLine, type,
							fieldrefClassName, fieldrefName, fieldrefType));
				}
				break;
			/*******
			 * RET *
			 *******/
			case Opcode.RET: {
				final int varIndex = wide ? codeReader.readUnsignedShort()
						: codeReader.readUnsignedByte();
				bb.addOperation(new RET(opPc, opCode, opLine, varIndex));
				codeReader.pc = codeReader.code.length; // next open pc
			}
				break;
			/**********
			 * RETURN *
			 **********/
			case Opcode.ARETURN:
				type = DataType.T_AREF;
				// fall through
			case Opcode.DRETURN:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FRETURN:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IRETURN:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LRETURN:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// fall through
			case Opcode.RETURN:
				if (type < 0) {
					type = DataType.T_VOID;
				}
				bb.addOperation(new RETURN(opPc, opCode, opLine, type));
				codeReader.pc = codeReader.code.length; // next open pc
				break;
			/*********
			 * STORE *
			 *********/
			case Opcode.ASTORE:
				type = DataType.T_AREF;
				// fall through
			case Opcode.DSTORE:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FSTORE:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.ISTORE:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LSTORE:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				// for all above
				iValue = wide ? codeReader.readUnsignedShort() : codeReader
						.readUnsignedByte();
				// fall through
			case Opcode.ASTORE_0:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 0;
				}
				// fall through
			case Opcode.DSTORE_0:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 0;
				}
				// fall through
			case Opcode.FSTORE_0:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 0;
				}
				// fall through
			case Opcode.ISTORE_0:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 0;
				}
				// fall through
			case Opcode.LSTORE_0:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 0;
				}
				// fall through
			case Opcode.ASTORE_1:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 1;
				}
				// fall through
			case Opcode.DSTORE_1:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 1;
				}
				// fall through
			case Opcode.FSTORE_1:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 1;
				}
				// fall through
			case Opcode.ISTORE_1:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 1;
				}
				// fall through
			case Opcode.LSTORE_1:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 1;
				}
				// fall through
			case Opcode.ASTORE_2:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 2;
				}
				// fall through
			case Opcode.DSTORE_2:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 2;
				}
				// fall through
			case Opcode.FSTORE_2:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 2;
				}
				// fall through
			case Opcode.ISTORE_2:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 2;
				}
				// fall through
			case Opcode.LSTORE_2:
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 2;
				}
				// fall through
			case Opcode.ASTORE_3:
				if (type < 0) {
					type = DataType.T_AREF;
					iValue = 3;
				}
				// fall through
			case Opcode.DSTORE_3:
				if (type < 0) {
					type = DataType.T_DOUBLE;
					iValue = 3;
				}
				// fall through
			case Opcode.FSTORE_3:
				if (type < 0) {
					type = DataType.T_FLOAT;
					iValue = 3;
				}
				// fall through
			case Opcode.ISTORE_3:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = 3;
				}
				// fall through
			case Opcode.LSTORE_3: {
				if (type < 0) {
					type = DataType.T_LONG;
					iValue = 3;
				}

				String varName = null;
				String varDescriptor = null;
				if (localVariableAttribute != null) {
					for (int i = 0; i < localVariableAttribute.tableLength(); ++i) {
						if (localVariableAttribute.index(i) == iValue
								&& localVariableAttribute.startPc(i) <= opPc
								&& localVariableAttribute.startPc(i)
										+ localVariableAttribute.codeLength(i) >= opPc) {
							varName = localVariableAttribute.variableName(i);
							varDescriptor = localVariableAttribute
									.descriptor(i);
							break;
						}
					}
				}
				if (varName == null) {
					if (iValue == 0 && !isStatic) {
						varName = "this";
					} else {
						varName = "arg" + iValue;
					}
				}
				final T varT = varDescriptor == null ? null : du
						.getDescT(varDescriptor);

				bb.addOperation(new STORE(opPc, opCode, opLine, type, iValue,
						varName, varT));
				break;
			}
			/*********
			 * THROW *
			 *********/
			case Opcode.ATHROW:
				bb.addOperation(new THROW(opPc, opCode, opLine));
				// next open pc
				codeReader.pc = codeReader.code.length; // next open pc
				break;
			/**************
			 * CHECKCAST *
			 **************/
			case Opcode.CHECKCAST: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				// cp arrays: "[L<classname>;" instead of "<classname>"!!!
				bb.addOperation(new CHECKCAST(opPc, opCode, opLine, readType(
						constPool, cpClassIndex, du)));
			}
				break;
			/*******
			 * MUL *
			 *******/
			case Opcode.DMUL:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FMUL:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IMUL:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LMUL:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new MUL(opPc, opCode, opLine, type));
				break;
			/*******
			 * NEG *
			 *******/
			case Opcode.DNEG:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FNEG:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.INEG:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LNEG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new NEG(opPc, opCode, opLine, type));
				break;
			/*******
			 * REM *
			 *******/
			case Opcode.DREM:
				if (type < 0) {
					type = DataType.T_DOUBLE;
				}
				// fall through
			case Opcode.FREM:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.IREM:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LREM:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new REM(opPc, opCode, opLine, type));
				break;
			/*******
			 * SUB *
			 *******/
			case Opcode.DSUB:
				type = DataType.T_DOUBLE;
				// fall through
			case Opcode.FSUB:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case Opcode.ISUB:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case Opcode.LSUB:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new SUB(opPc, opCode, opLine, type));
				break;
			// *** INSTANCEOF ***
			case Opcode.INSTANCEOF: {
				final int cpClassIndex = codeReader.readUnsignedShort();
				bb.addOperation(new INSTANCEOF(opPc, opCode, opLine, readType(
						constPool, cpClassIndex, du)));
			}
				break;
			/******
			 * OR *
			 ******/
			case Opcode.IOR:
				type = DataType.T_INT;
				// fall through
			case Opcode.LOR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new OR(opPc, opCode, opLine, type));
				break;
			case Opcode.ISHL:
				// fall through
			case Opcode.LSHL:
				break;
			case Opcode.ISHR:
				// fall through
			case Opcode.LSHR:
				break;
			case Opcode.IUSHR:
				// fall through
			case Opcode.LUSHR:
				break;
			/*******
			 * XOR *
			 *******/
			case Opcode.IXOR:
				type = DataType.T_INT;
				// fall through
			case Opcode.LXOR: {
				if (type < 0) {
					type = DataType.T_LONG;
				}
				bb.addOperation(new XOR(opPc, opCode, opLine, type));
			}
				break;
			/**********
			 * SWITCH *
			 **********/
			case Opcode.LOOKUPSWITCH: {
				// align
				if (codeReader.pc % 4 > 0) {
					codeReader.pc += 4 - codeReader.pc % 4;
				}
				// defaultBranch
				final int defaultBranch = codeReader.readUnsignedInt();
				// map entries number
				final int npairs = codeReader.readUnsignedInt();
				// case pc -> values
				final HashMap<Integer, List<Integer>> casePc2Values = new HashMap<Integer, List<Integer>>();
				for (int i = 0; i < npairs; ++i) {
					final int caseValue = codeReader.readUnsignedInt();
					final int caseBranch = codeReader.readUnsignedInt();
					List<Integer> values = casePc2Values.get(caseBranch);
					if (values == null) {
						values = new ArrayList<Integer>();
						casePc2Values.put(caseBranch, values);
					}
					values.add(caseValue);
				}
				// add default branch, can overlay with other cases, even JDK 6
				// doesn't optimize this
				List<Integer> values = casePc2Values.get(defaultBranch);
				if (values == null) {
					values = new ArrayList<Integer>();
					casePc2Values.put(defaultBranch, values);
				}
				values.add(null);
				oValue = casePc2Values;
			}
			// fall through
			case Opcode.TABLESWITCH: {
				if (oValue == null) {
					// align
					if (codeReader.pc % 4 > 0) {
						codeReader.pc += 4 - codeReader.pc % 4;
					}
					// defaultBranch
					final int defaultBranch = codeReader.readUnsignedInt();
					// map key boundaries
					final int caseLow = codeReader.readUnsignedInt();
					final int caseHigh = codeReader.readUnsignedInt();
					// case pc -> values
					final HashMap<Integer, List<Integer>> casePc2Values = new HashMap<Integer, List<Integer>>();
					for (int caseValue = caseLow; caseValue <= caseHigh; ++caseValue) {
						final int caseBranch = codeReader.readUnsignedInt();
						List<Integer> values = casePc2Values.get(caseBranch);
						if (values == null) {
							values = new ArrayList<Integer>();
							casePc2Values.put(caseBranch, values);
						}
						values.add(caseValue);
					}
					// add default branch, can overlay with other cases, even
					// JDK 6
					// doesn't optimize this
					List<Integer> values = casePc2Values.get(defaultBranch);
					if (values == null) {
						values = new ArrayList<Integer>();
						casePc2Values.put(defaultBranch, values);
					}
					values.add(null);
					oValue = casePc2Values;
				}
				// case pc -> values
				final HashMap<Integer, List<Integer>> casePc2Values = (HashMap<Integer, List<Integer>>) oValue;
				for (final Map.Entry<Integer, List<Integer>> casePc2ValuesEntry : casePc2Values
						.entrySet()) {
					final int caseBranch = casePc2ValuesEntry.getKey();
					final List<Integer> values = casePc2ValuesEntry.getValue();
					final int casePc = opPc + caseBranch;

					BB caseBb = cfg.getTargetBb(casePc, pcBbs);
					if (caseBb == null) {
						caseBb = cfg.newBb(casePc);
						pcBbs.put(casePc, caseBb);
						openPcs.add(casePc);
					}
					bb.addSucc(caseBb, values);
				}
				bb.addOperation(new SWITCH(opPc, opCode, opLine));
				// next open pc
				codeReader.pc = codeReader.code.length; // next open pc
			}
				break;
			/***********
			 * MONITOR *
			 ***********/
			case Opcode.MONITORENTER:
				type = MONITOR.T_ENTER;
				// fall through
			case Opcode.MONITOREXIT:
				if (type < 0) {
					type = MONITOR.T_EXIT;
				}
				bb.addOperation(new MONITOR(opPc, opCode, opLine, type));
				break;
			/*******
			 * POP *
			 *******/
			case Opcode.POP:
				type = POP.T_POP;
				// fall through
			case Opcode.POP2:
				if (type < 0) {
					type = POP.T_POP2;
				}
				bb.addOperation(new POP(opPc, opCode, opLine, type));
				break;
			/********
			 * SWAP *
			 ********/
			case Opcode.SWAP:
				bb.addOperation(new SWAP(opPc, opCode, opLine));
				break;
			/*******
			 * WIDE *
			 *******/
			case Opcode.WIDE:
				wide = true;
				// just for once! reset wide after switch
				continue;
			default:
				throw new RuntimeException("Unknown jvm operation code '"
						+ opCode + "'!");
			}
			// reset wide
			wide = false;
		}
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

		final T t = td.getT();
		final DU du = t.getDu();

		final T fieldT = du.getDescT(fieldInfo.getDescriptor());
		final F f = t.getF(fieldInfo.getName(), fieldT);
		f.setAccessFlags(fieldInfo.getAccessFlags());
		if (signatureAttribute != null
				&& signatureAttribute.getSignature() != null) {
			f.setSignature(signatureAttribute.getSignature());
		}

		final FD fd = new FD(f, td);

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
				LOGGER.warning("Unknown constant attribute '" + tag
						+ "' for field info '" + fieldInfo.getName() + "'!");
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
		m.setAccessFlags(methodInfo.getAccessFlags());
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

	private static T readType(final ConstPool constPool,
			final int cpClassIndex, final DU du) {
		// no primitives here: for checkcast, instanceof etc.
		final String classInfo = constPool.getClassInfo(cpClassIndex);
		// strange Javassist behaviour:
		if (classInfo.charAt(0) == '[') {
			// Javassist only replaces '/' through '.' for arrays
			// desc: [[I, [Ljava.lang.String;
			return du.getDescT(classInfo.replace('.', '/'));
		}
		// org.decojer.cavaj.test.DecTestInner$1$1$1
		return du.getT(classInfo);
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
			final T enumT = du.getT(((EnumMemberValue) memberValue).getType());
			final F enumF = enumT.getF(
					((EnumMemberValue) memberValue).getValue(), enumT);
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
		LOGGER.warning("Unknown member value type '"
				+ memberValue.getClass().getName() + "'!");
		return null;
	}

}