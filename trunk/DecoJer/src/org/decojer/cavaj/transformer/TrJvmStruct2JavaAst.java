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
package org.decojer.cavaj.transformer;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.bytecode.AccessFlag;
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

import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.tool.AnnotationsDecompiler;
import org.decojer.cavaj.tool.SignatureDecompiler;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Transform JVM Struct to AST.
 * 
 * @author André Pankraz
 */
public class TrJvmStruct2JavaAst {

	private final static Logger LOGGER = Logger
			.getLogger(TrJvmStruct2JavaAst.class.getName());

	@SuppressWarnings("unchecked")
	private static void decompileField(final FD fd, final CU cu) {
		final TD td = fd.getTd();
		final FieldInfo fieldInfo = fd.getFieldInfo();
		final AbstractTypeDeclaration typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

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
						"Unknown field info attribute tag '" + attributeTag
								+ "' for field info '" + fieldInfo.getName()
								+ "'!");
			}
		}

		int accessFlags = fieldInfo.getAccessFlags();

		if (accessFlags != (accessFlags &= ~AccessFlag.SYNTHETIC)
				|| syntheticAttribute != null) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		final boolean isEnum = accessFlags != (accessFlags &= ~AccessFlag.ENUM);

		// decompile BodyDeclaration, possible subtypes:
		// FieldDeclaration, EnumConstantDeclaration
		final BodyDeclaration fieldDeclaration;
		final String name = fieldInfo.getName();
		if (isEnum) {
			fieldDeclaration = ast.newEnumConstantDeclaration();
			((EnumConstantDeclaration) fieldDeclaration).setName(ast
					.newSimpleName(name));
		} else {
			final VariableDeclarationFragment variableDeclarationFragment = ast
					.newVariableDeclarationFragment();
			variableDeclarationFragment.setName(ast.newSimpleName(name));
			fieldDeclaration = ast
					.newFieldDeclaration(variableDeclarationFragment);
		}

		// decompile deprecated Javadoc-tag if no annotation set
		if (deprecatedAttribute != null
				&& !AnnotationsDecompiler
						.isDeprecatedAnnotation(annotationsAttributeRuntimeVisible)) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			fieldDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations,
		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through Eclipse JDT
		if (annotationsAttributeRuntimeInvisible != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					fieldDeclaration.modifiers(),
					annotationsAttributeRuntimeInvisible.getAnnotations());
		}
		if (annotationsAttributeRuntimeVisible != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					fieldDeclaration.modifiers(),
					annotationsAttributeRuntimeVisible.getAnnotations());
		}

		// decompile modifier flags,
		// public is default for enum and interface
		if (accessFlags != (accessFlags &= ~AccessFlag.PUBLIC)
				&& !isEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.PRIVATE)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.PROTECTED)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		// static is default for enum and interface
		if (accessFlags != (accessFlags &= ~AccessFlag.STATIC)
				&& !isEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		// final is default for enum and interface
		if (accessFlags != (accessFlags &= ~AccessFlag.FINAL)
				&& !isEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.VOLATILE)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.TRANSIENT)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.TRANSIENT_KEYWORD));
		}
		if (accessFlags != 0) {
			LOGGER.log(Level.WARNING, "Unknown field info modifier flags '"
					+ accessFlags + "' for field info '" + fieldInfo.getName()
					+ "'!");
		}

		if (fieldDeclaration instanceof EnumConstantDeclaration) {
			if (!(typeDeclaration instanceof EnumDeclaration)) {
				LOGGER.log(Level.WARNING,
						"No enum declaration for enum constant declaration '"
								+ fieldInfo.getName() + "'!");
			} else {
				((EnumDeclaration) typeDeclaration).enumConstants().add(
						fieldDeclaration);
			}
		} else if (fieldDeclaration instanceof FieldDeclaration) {
			new SignatureDecompiler(td, fieldInfo.getDescriptor(),
					signatureAttribute)
					.decompileFieldType((FieldDeclaration) fieldDeclaration);

			if (constantAttribute != null) {
				final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ((FieldDeclaration) fieldDeclaration)
						.fragments().get(0);
				// only final, non static - no arrays, class types
				final int index = constantAttribute.getConstantValue();
				final ConstPool constPool = constantAttribute.getConstPool();
				final int tag = constPool.getTag(index);
				switch (tag) {
				case ConstPool.CONST_Double:
					variableDeclarationFragment.setInitializer(ast
							.newNumberLiteral(Double.toString(constPool
									.getDoubleInfo(index)) + 'D'));
					break;
				case ConstPool.CONST_Float:
					variableDeclarationFragment.setInitializer(ast
							.newNumberLiteral(Float.toString(constPool
									.getFloatInfo(index)) + 'F'));
					break;
				case ConstPool.CONST_Integer:
					// also: int, short, byte, char, boolean
					final int integerInfo = constPool.getIntegerInfo(index);
					// TODO better check decompiled type
					if ("C".equals(fieldInfo.getDescriptor())) {
						final CharacterLiteral newCharacterLiteral = ast
								.newCharacterLiteral();
						newCharacterLiteral.setCharValue((char) integerInfo);
						variableDeclarationFragment
								.setInitializer(newCharacterLiteral);
					} else if ("Z".equals(fieldInfo.getDescriptor())) {
						variableDeclarationFragment.setInitializer(ast
								.newBooleanLiteral(integerInfo == 1));
					} else {
						variableDeclarationFragment
								.setInitializer(ast.newNumberLiteral(Integer
										.toString(integerInfo)));
					}
					break;
				case ConstPool.CONST_Long:
					variableDeclarationFragment.setInitializer(ast
							.newNumberLiteral(Long.toString(constPool
									.getLongInfo(index)) + 'L'));
					break;
				case ConstPool.CONST_String:
					final StringLiteral newStringLiteral = ast
							.newStringLiteral();
					newStringLiteral.setLiteralValue(constPool
							.getStringInfo(index));
					variableDeclarationFragment
							.setInitializer(newStringLiteral);
					break;
				default:
					LOGGER.log(Level.WARNING, "Unknown constant attribute '"
							+ tag + "' for field info '" + fieldInfo.getName()
							+ "'!");
				}
			}
			fd.setFieldDeclaration(fieldDeclaration);
		}
	}

	@SuppressWarnings("unchecked")
	private static void decompileMethod(final MD md, final CU cu) {
		final TD td = md.getTd();
		final MethodInfo methodInfo = md.getMethodInfo();
		final AbstractTypeDeclaration typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

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
						"Unknown method info attribute tag '" + attributeTag
								+ "' for method info '" + methodInfo.getName()
								+ "'!");
			}
		}

		int accessFlags = methodInfo.getAccessFlags();

		if (accessFlags != (accessFlags &= ~AccessFlag.SYNTHETIC)
				|| syntheticAttribute != null) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		// decompile BodyDeclaration, possible subtypes:
		// MethodDeclaration (method or constructor),
		// AnnotationTypeMemberDeclaration (all methods in @interface) or
		// Initializer (static {})
		final BodyDeclaration methodDeclaration;
		final String name = methodInfo.getName();
		if ("<clinit>".equals(name)) {
			// this is the static initializer "static {}" => Initializer
			methodDeclaration = ast.newInitializer();
		} else if ("<init>".equals(name)) {
			// this is the constructor => MethodDeclaration with type
			// declaration name as name
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setConstructor(true);
			((MethodDeclaration) methodDeclaration).setName(ast
					.newSimpleName(cu.isStartTdOnly() ? td.getName() : td
							.getIName()));
		} else if (typeDeclaration instanceof AnnotationTypeDeclaration) {
			// AnnotationTypeMemberDeclaration
			methodDeclaration = ast.newAnnotationTypeMemberDeclaration();
			((AnnotationTypeMemberDeclaration) methodDeclaration).setName(ast
					.newSimpleName(name));
			// check if default value (byte byteTest() default 2;)
			if (annotationDefaultAttribute != null) {
				final Expression expression = AnnotationsDecompiler
						.decompileAnnotationMemberValue(td,
								annotationDefaultAttribute.getDefaultValue());
				if (expression != null) {
					((AnnotationTypeMemberDeclaration) methodDeclaration)
							.setDefault(expression);
				}
			}
		} else {
			// MethodDeclaration
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setName(ast
					.newSimpleName(name));
		}

		// decompile deprecated Javadoc-tag if no annotation set
		if (deprecatedAttribute != null
				&& !AnnotationsDecompiler
						.isDeprecatedAnnotation(annotationsAttributeRuntimeVisible)) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			methodDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations,
		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through Eclipse JDT
		if (annotationsAttributeRuntimeInvisible != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					methodDeclaration.modifiers(),
					annotationsAttributeRuntimeInvisible.getAnnotations());
		}
		if (annotationsAttributeRuntimeVisible != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					methodDeclaration.modifiers(),
					annotationsAttributeRuntimeVisible.getAnnotations());
		}

		// decompile modifier flags,
		// public is default for interface and annotation type declarations
		if (accessFlags != (accessFlags &= ~AccessFlag.PUBLIC)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.PRIVATE)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.PROTECTED)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.STATIC)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.FINAL)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.SYNCHRONIZED)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.SYNCHRONIZED_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.BRIDGE)) {
			return; // TODO
		}
		// don't really need it here, but have to check because of access flags
		// warn message:
		final boolean varargs = accessFlags != (accessFlags &= ~AccessFlag.VARARGS);
		if (accessFlags != (accessFlags &= ~AccessFlag.NATIVE)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.NATIVE_KEYWORD));
		}
		// abstract is default for interface and annotation type declarations
		if (accessFlags != (accessFlags &= ~AccessFlag.ABSTRACT)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.STRICT)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
		}
		if (accessFlags != 0) {
			LOGGER.log(Level.WARNING, "Unknown method info modifier flags '0x"
					+ Integer.toHexString(accessFlags) + "' for method info '"
					+ methodInfo.getName() + "'!");
		}
		// decompile method signature (not necessary for Initializer)
		if (methodDeclaration instanceof MethodDeclaration) {
			new SignatureDecompiler(td, methodInfo.getDescriptor(),
					signatureAttribute).decompileMethodTypes(
					(MethodDeclaration) methodDeclaration,
					exceptionsAttribute == null ? null : exceptionsAttribute
							.getExceptions(), varargs);
		} else if (methodDeclaration instanceof AnnotationTypeMemberDeclaration) {
			final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(
					td, methodInfo.getDescriptor(), signatureAttribute);
			// should be empty, skip "()"
			signatureDecompiler.decompileMethodParameterTypes();
			final Type returnType = signatureDecompiler.decompileType();
			if (returnType != null) {
				((AnnotationTypeMemberDeclaration) methodDeclaration)
						.setType(returnType);
			}
		}
		// get method block
		final Block block;
		// abstract and native methods have no block
		if ((methodInfo.getAccessFlags() & AccessFlag.ABSTRACT) == 0
				&& (methodInfo.getAccessFlags() & AccessFlag.NATIVE) == 0) {
			if (methodDeclaration instanceof MethodDeclaration) {
				block = ast.newBlock();
				((MethodDeclaration) methodDeclaration).setBody(block);
			} else if (methodDeclaration instanceof Initializer) {
				// Initializer (static{}) has block per default
				block = ((Initializer) methodDeclaration).getBody();
			} else {
				block = null;
			}
		} else {
			block = null;
		}
		// block == null => helper for variable name only
		final CFG cfg = new CFG(md, block, codeAttribute);

		if (methodDeclaration instanceof MethodDeclaration) {
			// decompile method parameter annotations and names
			final javassist.bytecode.annotation.Annotation[][] annotationsInvisible = parameterAnnotationsAttributeRuntimeInvisible == null ? null
					: parameterAnnotationsAttributeRuntimeInvisible
							.getAnnotations();
			final javassist.bytecode.annotation.Annotation[][] annotationsVisible = parameterAnnotationsAttributeRuntimeVisible == null ? null
					: parameterAnnotationsAttributeRuntimeVisible
							.getAnnotations();

			int annotation = 0;
			int test = (methodInfo.getAccessFlags() & AccessFlag.STATIC) != 0 ? 0
					: 1;
			for (final SingleVariableDeclaration singleVariableDeclaration : (List<SingleVariableDeclaration>) ((MethodDeclaration) methodDeclaration)
					.parameters()) {
				// decompile parameter annotations
				if (annotationsInvisible != null
						&& annotationsInvisible.length > annotation) {
					AnnotationsDecompiler.decompileAnnotations(td,
							singleVariableDeclaration.modifiers(),
							annotationsInvisible[annotation]);
				}
				if (annotationsVisible != null
						&& annotationsVisible.length > annotation) {
					AnnotationsDecompiler.decompileAnnotations(td,
							singleVariableDeclaration.modifiers(),
							annotationsVisible[annotation++]);
				}
				singleVariableDeclaration.setName(ast.newSimpleName(cfg
						.getVariableName(test++)));
			}
		}
		if (codeAttribute != null) {
			md.setCFG(cfg);
		}
		md.setMethodDeclaration(methodDeclaration);
	}

	@SuppressWarnings("unchecked")
	public static void transform(final TD td) {
		final CU cu = td.getCu();
		final AST ast = cu.getAst();
		final ClassFile classFile = td.getClassFile();

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
				LOGGER.log(Level.WARNING, "Unknown class file attribute tag '"
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

		int accessFlags = classFile.getAccessFlags();

		if (accessFlags != (accessFlags &= ~AccessFlag.SYNTHETIC)
				|| syntheticAttribute != null) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		AbstractTypeDeclaration typeDeclaration = null;

		// annotation type declaration
		if (accessFlags != (accessFlags &= ~AccessFlag.ANNOTATION)) {
			if (classFile.getSuperclass() == null
					|| !Object.class.getName()
							.equals(classFile.getSuperclass())) {
				LOGGER.log(Level.WARNING,
						"Classfile with AccessFlag.ANNOTATION has no super class '"
								+ Object.class.getName() + "' but has '"
								+ classFile.getSuperclass() + "'! Repairing.");
				try {
					classFile.setSuperclass(Annotation.class.getName());
				} catch (final CannotCompileException e) {
					LOGGER.log(Level.WARNING, "...Failed!", e);
				}
			}
			if (classFile.getInterfaces().length != 1
					|| !Annotation.class.getName().equals(
							classFile.getInterfaces()[0])) {
				LOGGER.log(Level.WARNING,
						"Classfile with AccessFlag.ANNOTATION has no interface '"
								+ Annotation.class.getName() + "' but has '"
								+ classFile.getInterfaces() + "'! Repairing.");
				classFile.setInterfaces(new String[] { Annotation.class
						.getName() });
			}
			typeDeclaration = ast.newAnnotationTypeDeclaration();
		}
		// enum declaration
		if (accessFlags != (accessFlags &= ~AccessFlag.ENUM)) {
			if (typeDeclaration != null) {
				LOGGER.log(Level.WARNING,
						"Enum declaration cannot be an annotation type declaration! Ignoring.");
			} else {
				if (classFile.getSuperclass() == null
						|| !Enum.class.getName().equals(
								classFile.getSuperclass())) {
					LOGGER.log(Level.WARNING,
							"Classfile with AccessFlag.ENUM has no super class '"
									+ Enum.class.getName() + "' but has '"
									+ classFile.getSuperclass()
									+ "'! Repairing.");
					try {
						classFile.setSuperclass(Enum.class.getName());
					} catch (final CannotCompileException e) {
						LOGGER.log(Level.WARNING, "...Failed!", e);
					}
				}
				typeDeclaration = ast.newEnumDeclaration();
				// enum declarations cannot extent other classes but Enum.class,
				// but can have interfaces
				for (final String interfaceName : classFile.getInterfaces()) {
					((EnumDeclaration) typeDeclaration).superInterfaceTypes()
							.add(ast.newSimpleType(ast.newName(interfaceName)));
				}
			}
		}
		// no annotation type declaration or enum declaration => normal class or
		// interface type declaration
		if (typeDeclaration == null) {
			typeDeclaration = ast.newTypeDeclaration();

			final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(
					td, "L" + classFile.getSuperclass() + ";",
					signatureAttribute);
			signatureDecompiler.decompileClassTypes(
					(TypeDeclaration) typeDeclaration,
					classFile.getInterfaces());
		}

		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through eclipse.jdt
		if (annotationsAttributeRuntimeInvisible != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					typeDeclaration.modifiers(),
					annotationsAttributeRuntimeInvisible.getAnnotations());
		}
		if (annotationsAttributeRuntimeVisible != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					typeDeclaration.modifiers(),
					annotationsAttributeRuntimeVisible.getAnnotations());
		}

		// decompile remaining modifier flags
		if (accessFlags != (accessFlags &= ~AccessFlag.PUBLIC)) {
			typeDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.FINAL)
				&& !(typeDeclaration instanceof EnumDeclaration)) {
			// enum declaration is final by default
			typeDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.SUPER)) {
			; // modern invokesuper syntax, is allways set in current java
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.INTERFACE)) {
			if (typeDeclaration instanceof TypeDeclaration) {
				((TypeDeclaration) typeDeclaration).setInterface(true);
			}
		}
		if (accessFlags != (accessFlags &= ~AccessFlag.ABSTRACT)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			typeDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
		}
		if (accessFlags != 0) {
			LOGGER.log(Level.WARNING,
					"Unknown type declaration modifier flags '" + accessFlags
							+ "'!");
		}

		// multiple CompilationUnit.TypeDeclaration in same AST (source file)
		// possible, but only one of them is public and multiple class files are
		// necessary
		typeDeclaration.setName(ast.newSimpleName(cu.isStartTdOnly() ? td
				.getName() : td.getIName()));

		if (deprecatedAttribute != null
				&& !AnnotationsDecompiler
						.isDeprecatedAnnotation(annotationsAttributeRuntimeVisible)) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			typeDeclaration.setJavadoc(newJavadoc);
		}

		td.setTypeDeclaration(typeDeclaration);
		for (final FieldInfo fieldInfo : (List<FieldInfo>) classFile
				.getFields()) {
			final FD fd = new FD(td, fieldInfo);
			td.getBds().add(fd);
			decompileField(fd, cu);
		}
		for (final MethodInfo methodInfo : (List<MethodInfo>) classFile
				.getMethods()) {
			final MD md = new MD(td, methodInfo);
			td.getBds().add(md);
			decompileMethod(md, cu);
		}

		// build class decompilation comment
		final StringBuilder sb = new StringBuilder();
		sb.append("\n\n/*\n")
				.append(" * Generated by DecoJer ")
				.append(0.9)
				.append(", a Java-bytecode decompiler.\n")
				.append(" * DecoJer Copyright (C) 2009-2011 André Pankraz. All Rights Reserved.\n")
				.append(" *\n");
		final int majorVersion = classFile.getMajorVersion();
		final int minorVersion = classFile.getMinorVersion();
		sb.append(" * Class File Version: ").append(majorVersion).append('.')
				.append(minorVersion).append(" (Java ");
		if (majorVersion == ClassFile.JAVA_1) {
			sb.append("1.1");
		} else if (majorVersion == ClassFile.JAVA_2) {
			sb.append("1.2");
		} else if (majorVersion == ClassFile.JAVA_3) {
			sb.append("1.3");
		} else if (majorVersion == ClassFile.JAVA_5) {
			sb.append("5");
		} else if (majorVersion == ClassFile.JAVA_6) {
			sb.append("6");
		} else if (majorVersion == ClassFile.JAVA_7) {
			sb.append("7");
		} else {
			sb.append("TODO version unknown");
		}
		sb.append(")\n");
		if (sourceFileAttribute != null) {
			sb.append(" * Source File Name: ")
					.append(sourceFileAttribute.getFileName()).append('\n');
		}
		sb.append(" */");
		cu.setComment(sb.toString());
	}

}