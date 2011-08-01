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

import javassist.bytecode.AccessFlag;
import javassist.bytecode.CodeAttribute;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
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
		final AbstractTypeDeclaration typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		int accessFlags = fd.getAccessFlags();

		if (accessFlags != (accessFlags &= ~AccessFlag.SYNTHETIC)
				|| fd.isSynthetic()) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		final boolean isEnum = accessFlags != (accessFlags &= ~AccessFlag.ENUM);

		// decompile BodyDeclaration, possible subtypes:
		// FieldDeclaration, EnumConstantDeclaration
		final BodyDeclaration fieldDeclaration;
		final String name = fd.getName();
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
		if (fd.isDeprecated()
				&& !AnnotationsDecompiler.isDeprecatedAnnotation(fd.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			fieldDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations,
		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through Eclipse JDT
		if (fd.getAs() != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					fieldDeclaration.modifiers(), fd.getAs());
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
					+ accessFlags + "' for field info '" + fd.getName() + "'!");
		}

		if (fieldDeclaration instanceof EnumConstantDeclaration) {
			if (!(typeDeclaration instanceof EnumDeclaration)) {
				LOGGER.log(Level.WARNING,
						"No enum declaration for enum constant declaration '"
								+ fd.getName() + "'!");
			} else {
				((EnumDeclaration) typeDeclaration).enumConstants().add(
						fieldDeclaration);
			}
		} else if (fieldDeclaration instanceof FieldDeclaration) {
			new SignatureDecompiler(td, fd.getDescriptor(), fd.getSignature())
					.decompileFieldType((FieldDeclaration) fieldDeclaration);

			final Object value = fd.getValue();
			if (value != null) {
				final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ((FieldDeclaration) fieldDeclaration)
						.fragments().get(0);
				// only final, non static - no arrays, class types
				if (value instanceof Double) {
					variableDeclarationFragment.setInitializer(ast
							.newNumberLiteral(value.toString() + 'D'));
				} else if (value instanceof Float) {
					variableDeclarationFragment.setInitializer(ast
							.newNumberLiteral(value.toString() + 'F'));
				} else if (value instanceof Integer) {
					// also: int, short, byte, char, boolean
					if ("C".equals(fd.getDescriptor())) {
						final CharacterLiteral newCharacterLiteral = ast
								.newCharacterLiteral();
						newCharacterLiteral
								.setCharValue((char) ((Integer) value)
										.intValue());
						variableDeclarationFragment
								.setInitializer(newCharacterLiteral);
					} else if ("Z".equals(fd.getDescriptor())) {
						variableDeclarationFragment
								.setInitializer(ast
										.newBooleanLiteral(((Integer) value)
												.intValue() == 1));
					} else {
						variableDeclarationFragment.setInitializer(ast
								.newNumberLiteral(value.toString()));
					}
				} else if (value instanceof Long) {
					variableDeclarationFragment.setInitializer(ast
							.newNumberLiteral(value.toString() + 'L'));
				} else if (value instanceof String) {
					final StringLiteral newStringLiteral = ast
							.newStringLiteral();
					newStringLiteral.setLiteralValue((String) value);
					variableDeclarationFragment
							.setInitializer(newStringLiteral);
				} else {
					LOGGER.log(Level.WARNING,
							"Unknown constant attribute '" + value.getClass()
									+ "' for field info '" + fd.getName()
									+ "'!");
				}
			}
			fd.setFieldDeclaration(fieldDeclaration);
		}
	}

	@SuppressWarnings("unchecked")
	private static void decompileMethod(final MD md, final CU cu) {
		final M m = md.getM();
		final TD td = md.getTd();

		final AbstractTypeDeclaration typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		int accessFlags = md.getAccessFlags();

		if (accessFlags != (accessFlags &= ~AccessFlag.SYNTHETIC)
				|| md.isSynthetic()) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		// decompile BodyDeclaration, possible subtypes:
		// MethodDeclaration (method or constructor),
		// AnnotationTypeMemberDeclaration (all methods in @interface) or
		// Initializer (static {})
		final BodyDeclaration methodDeclaration;
		final String name = m.getName();
		if ("<clinit>".equals(name)) {
			// this is the static initializer "static {}" => Initializer
			methodDeclaration = ast.newInitializer();
		} else if ("<init>".equals(name)) {
			// this is the constructor => MethodDeclaration with type
			// declaration name as name
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setConstructor(true);
			((MethodDeclaration) methodDeclaration).setName(ast
					.newSimpleName(cu.isStartTdOnly() ? td.getT().getPName()
							: td.getT().getIName()));
		} else if (typeDeclaration instanceof AnnotationTypeDeclaration) {
			// AnnotationTypeMemberDeclaration
			methodDeclaration = ast.newAnnotationTypeMemberDeclaration();
			((AnnotationTypeMemberDeclaration) methodDeclaration).setName(ast
					.newSimpleName(name));

			// check if default value (byte byteTest() default 2;)
			if (md.getAnnotationDefaultValue() != null) {
				final Expression expression = AnnotationsDecompiler
						.decompileAnnotationDefaultValue(td,
								md.getAnnotationDefaultValue());
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
		if (md.isDeprecated()
				&& !AnnotationsDecompiler.isDeprecatedAnnotation(md.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			methodDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations,
		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through Eclipse JDT
		if (md.getAs() != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					methodDeclaration.modifiers(), md.getAs());
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
		// TODO only if is DEX?
		// DEX: ACC_CONSTRUCTOR = 0x10000
		if (accessFlags != (accessFlags &= ~0x10000)) {
			// nothing
		}
		// DEX: ACC_DECLARED_SYNCHRONIZED = 0x20000
		if (accessFlags != (accessFlags &= ~0x20000)) {
			// nothing
		}
		if (accessFlags != 0) {
			LOGGER.log(Level.WARNING, "Unknown method info modifier flags '0x"
					+ Integer.toHexString(accessFlags) + "' for method info '"
					+ m.getName() + "'!");
		}
		// decompile method signature (not necessary for Initializer)
		if (methodDeclaration instanceof MethodDeclaration) {
			new SignatureDecompiler(td, m.getDescriptor(), m.getSignature())
					.decompileMethodTypes(
							(MethodDeclaration) methodDeclaration,
							m.getThrowsTs(), varargs);
		} else if (methodDeclaration instanceof AnnotationTypeMemberDeclaration) {
			final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(
					td, m.getDescriptor(), m.getSignature());
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
		if ((md.getAccessFlags() & AccessFlag.ABSTRACT) == 0
				&& (md.getAccessFlags() & AccessFlag.NATIVE) == 0) {
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
		final CodeAttribute codeAttribute = md.getCodeAttribute();
		final CFG cfg = new CFG(md, block, codeAttribute);

		if (methodDeclaration instanceof MethodDeclaration) {
			// decompile method parameter annotations and names
			final A[][] paramAs = md.getParamAs();
			int param = 0;
			for (final SingleVariableDeclaration singleVariableDeclaration : (List<SingleVariableDeclaration>) ((MethodDeclaration) methodDeclaration)
					.parameters()) {
				// decompile parameter annotations
				if (paramAs != null && param < paramAs.length) {
					AnnotationsDecompiler.decompileAnnotations(td,
							singleVariableDeclaration.modifiers(),
							paramAs[param]);
				}
				singleVariableDeclaration.setName(ast.newSimpleName(m
						.getParamName(param++)));
			}
		}
		if (codeAttribute != null) {
			md.setCFG(cfg);
		}
		md.setMethodDeclaration(methodDeclaration);
	}

	/**
	 * Transform type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	@SuppressWarnings("unchecked")
	public static void transform(final TD td) {
		final T t = td.getT();
		final CU cu = td.getCu();
		final AST ast = cu.getAst();

		int accessFlags = td.getAccessFlags();

		if (accessFlags != (accessFlags &= ~AccessFlag.SYNTHETIC)
				|| td.isSynthetic()) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		AbstractTypeDeclaration typeDeclaration = null;

		// annotation type declaration
		if (accessFlags != (accessFlags &= ~AccessFlag.ANNOTATION)) {
			if (t.getSuperT() == null
					|| !Object.class.getName().equals(t.getSuperT().getName())) {
				LOGGER.log(Level.WARNING,
						"Classfile with AccessFlag.ANNOTATION has no super class '"
								+ Object.class.getName() + "' but has '"
								+ t.getSuperT().getName() + "'!");
			}
			if (t.getInterfaceTs().length != 1
					|| !Annotation.class.getName().equals(
							t.getInterfaceTs()[0].getName())) {
				LOGGER.log(Level.WARNING,
						"Classfile with AccessFlag.ANNOTATION has no interface '"
								+ Annotation.class.getName() + "' but has '"
								+ t.getInterfaceTs()[0].getName() + "'!");
			}
			typeDeclaration = ast.newAnnotationTypeDeclaration();
		}
		// enum declaration
		if (accessFlags != (accessFlags &= ~AccessFlag.ENUM)) {
			if (typeDeclaration != null) {
				LOGGER.log(Level.WARNING,
						"Enum declaration cannot be an annotation type declaration! Ignoring.");
			} else {
				if (t.getSuperT() == null
						|| !Enum.class.getName()
								.equals(t.getSuperT().getName())) {
					LOGGER.log(Level.WARNING,
							"Classfile with AccessFlag.ENUM has no super class '"
									+ Enum.class.getName() + "' but has '"
									+ t.getSuperT().getName() + "'!");
				}
				typeDeclaration = ast.newEnumDeclaration();
				// enum declarations cannot extent other classes but Enum.class,
				// but can have interfaces
				if (t.getInterfaceTs() != null) {
					for (final T interfaceT : t.getInterfaceTs()) {
						((EnumDeclaration) typeDeclaration)
								.superInterfaceTypes()
								.add(ast.newSimpleType(ast.newName(interfaceT
										.getName())));
					}
				}
			}
		}
		// no annotation type declaration or enum declaration => normal class or
		// interface type declaration
		if (typeDeclaration == null) {
			typeDeclaration = ast.newTypeDeclaration();

			final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(
					td, "L" + t.getSuperT().getName() + ";", t.getSignature());
			signatureDecompiler.decompileClassTypes(
					(TypeDeclaration) typeDeclaration, t.getInterfaceTs());
		}

		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through eclipse.jdt
		if (td.getAs() != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					typeDeclaration.modifiers(), td.getAs());
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
		typeDeclaration.setName(ast.newSimpleName(cu.isStartTdOnly() ? t
				.getPName() : t.getIName()));

		if (td.isDeprecated()
				&& !AnnotationsDecompiler.isDeprecatedAnnotation(td.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			typeDeclaration.setJavadoc(newJavadoc);
		}

		td.setTypeDeclaration(typeDeclaration);
		for (final BD bd : td.getBds()) {
			if (bd instanceof FD) {
				decompileField((FD) bd, cu);
			}
			if (bd instanceof MD) {
				decompileMethod((MD) bd, cu);
			}
		}

		// build class decompilation comment
		final StringBuilder sb = new StringBuilder();
		sb.append("\n\n/*\n")
				.append(" * Generated by DecoJer ")
				.append(0.9)
				.append(", a Java-bytecode decompiler.\n")
				.append(" * DecoJer Copyright (C) 2009-2011 André Pankraz. All Rights Reserved.\n")
				.append(" *\n");
		final int version = td.getVersion();
		sb.append(" * Class File Version: ").append(version).append(" (Java ");
		switch (version) {
		case 45:
			sb.append("1.1");
			break;
		case 46:
			sb.append("1.2");
			break;
		case 47:
			sb.append("1.3");
			break;
		case 48:
			sb.append("1.4");
			break;
		case 49:
			sb.append("5");
			break;
		case 50:
			sb.append("6");
			break;
		case 51:
			sb.append("7");
			break;
		case 52:
			sb.append("8");
			break;
		default:
			sb.append("TODO version unknown");
			break;
		}
		sb.append(")\n");
		if (td.getSourceFileName() != null) {
			sb.append(" * Source File Name: ").append(td.getSourceFileName())
					.append('\n');
		}
		sb.append(" */");
		cu.setComment(sb.toString());
	}

}