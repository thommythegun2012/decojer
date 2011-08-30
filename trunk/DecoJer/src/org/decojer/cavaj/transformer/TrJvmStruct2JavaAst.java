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
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.tool.AnnotationsDecompiler;
import org.decojer.cavaj.tool.SignatureDecompiler;
import org.decojer.cavaj.tool.Types;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
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
		final F f = fd.getF();
		final TD td = fd.getTd();

		final AbstractTypeDeclaration typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		if (f.checkAf(AF.SYNTHETIC) || fd.isSynthetic()) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		final boolean isEnum = f.checkAf(AF.ENUM);

		// decompile BodyDeclaration, possible subtypes:
		// FieldDeclaration, EnumConstantDeclaration
		final BodyDeclaration fieldDeclaration;
		final String name = f.getName();
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
		if (f.checkAf(AF.PUBLIC)
				&& !isEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (f.checkAf(AF.PRIVATE)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (f.checkAf(AF.PROTECTED)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		// static is default for enum and interface
		if (f.checkAf(AF.STATIC)
				&& !isEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		// final is default for enum and interface
		if (f.checkAf(AF.FINAL)
				&& !isEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (f.checkAf(AF.VOLATILE)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));
		}
		if (f.checkAf(AF.TRANSIENT)) {
			fieldDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.TRANSIENT_KEYWORD));
		}

		if (fieldDeclaration instanceof EnumConstantDeclaration) {
			if (!(typeDeclaration instanceof EnumDeclaration)) {
				LOGGER.warning("No enum declaration for enum constant declaration '"
						+ f.getName() + "'!");
			} else {
				((EnumDeclaration) typeDeclaration).enumConstants().add(
						fieldDeclaration);
			}
		} else if (fieldDeclaration instanceof FieldDeclaration) {
			if (f.getSignature() != null) {
				new SignatureDecompiler(td, null, f.getSignature())
						.decompileFieldType((FieldDeclaration) fieldDeclaration);
			} else {
				((FieldDeclaration) fieldDeclaration).setType(Types
						.convertType(f.getValueT(), td, ast));
			}
			final Object value = fd.getValue();
			if (value != null) {
				// only final, non static - no arrays, class types
				final Expression expr = Types.convertLiteral(f.getValueT(),
						value, td, ast);
				if (expr != null) {
					final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ((FieldDeclaration) fieldDeclaration)
							.fragments().get(0);
					variableDeclarationFragment.setInitializer(expr);
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

		if (m.checkAf(AF.SYNTHETIC) || md.isSynthetic()) {
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
		if (m.checkAf(AF.PUBLIC)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (m.checkAf(AF.PRIVATE)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (m.checkAf(AF.PROTECTED)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		if (m.checkAf(AF.STATIC)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		if (m.checkAf(AF.FINAL)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (m.checkAf(AF.SYNCHRONIZED)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.SYNCHRONIZED_KEYWORD));
		}
		if (m.checkAf(AF.BRIDGE)) {
			return; // TODO
		}
		if (m.checkAf(AF.NATIVE)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.NATIVE_KEYWORD));
		}
		// abstract is default for interface and annotation type declarations
		if (m.checkAf(AF.ABSTRACT)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
		}
		if (m.checkAf(AF.STRICTFP)) {
			methodDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
		}
		/*
		 * if (m.checkAf(AF.CONSTRUCTOR)) { // nothing, Dalvik only? } if
		 * (m.checkAf(AF.DECLARED_SYNCHRONIZED)) { // nothing, Dalvik only? }
		 */
		// decompile method signature (not necessary for Initializer)
		if (methodDeclaration instanceof MethodDeclaration) {
			new SignatureDecompiler(td, m.getDescriptor(), m.getSignature())
					.decompileMethodTypes(
							(MethodDeclaration) methodDeclaration,
							m.getThrowsTs(), m.checkAf(AF.VARARGS));
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
		// get method block,
		// abstract and native methods have no block
		// TODO double check this way?
		if (md.getCfg() != null && !m.checkAf(AF.ABSTRACT)
				&& !m.checkAf(AF.NATIVE)) {
			if (methodDeclaration instanceof MethodDeclaration) {
				final Block block = ast.newBlock();
				((MethodDeclaration) methodDeclaration).setBody(block);
				md.getCfg().setBlock(block);
			} else if (methodDeclaration instanceof Initializer) {
				// Initializer (static{}) has block per default
				md.getCfg().setBlock(
						((Initializer) methodDeclaration).getBody());
			}
		}
		if (methodDeclaration instanceof MethodDeclaration) {
			// decompile method parameter annotations and names
			final A[][] paramAs = md.getParamAss();
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

		if (t.checkAf(AF.SYNTHETIC) || td.isSynthetic()) {
			if (cu.isIgnoreSynthetic()) {
				return; // no source code ?
			}
		}

		AbstractTypeDeclaration typeDeclaration = null;

		// annotation type declaration
		if (t.checkAf(AF.ANNOTATION)) {
			if (t.getSuperT() == null
					|| !Object.class.getName().equals(t.getSuperT().getName())) {
				LOGGER.warning("Classfile with AccessFlag.ANNOTATION has no super class '"
						+ Object.class.getName()
						+ "' but has '"
						+ t.getSuperT().getName() + "'!");
			}
			if (t.getInterfaceTs().length != 1
					|| !Annotation.class.getName().equals(
							t.getInterfaceTs()[0].getName())) {
				LOGGER.warning("Classfile with AccessFlag.ANNOTATION has no interface '"
						+ Annotation.class.getName()
						+ "' but has '"
						+ t.getInterfaceTs()[0].getName() + "'!");
			}
			typeDeclaration = ast.newAnnotationTypeDeclaration();
		}
		// enum declaration
		if (t.checkAf(AF.ENUM)) {
			if (typeDeclaration != null) {
				LOGGER.warning("Enum declaration cannot be an annotation type declaration! Ignoring.");
			} else {
				if (t.getSuperT() == null
						|| !Enum.class.getName()
								.equals(t.getSuperT().getName())) {
					LOGGER.warning("Classfile with AccessFlag.ENUM has no super class '"
							+ Enum.class.getName()
							+ "' but has '"
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
		td.setTypeDeclaration(typeDeclaration);

		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through eclipse.jdt
		if (td.getAs() != null) {
			AnnotationsDecompiler.decompileAnnotations(td,
					typeDeclaration.modifiers(), td.getAs());
		}

		// decompile remaining modifier flags
		if (t.checkAf(AF.PUBLIC)) {
			typeDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (t.checkAf(AF.FINAL)
				&& !(typeDeclaration instanceof EnumDeclaration)) {
			// enum declaration is final by default
			typeDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (t.checkAf(AF.INTERFACE)) {
			if (typeDeclaration instanceof TypeDeclaration) {
				((TypeDeclaration) typeDeclaration).setInterface(true);
			}
		} else if (!t.checkAf(AF.SUPER) && !td.isDalvik()) {
			// modern invokesuper syntax, is always set in current JVM, but not
			// in Dalvik
			LOGGER.warning("Modern invokesuper syntax flag not set in type '"
					+ td + "'!");
		}
		if (t.checkAf(AF.ABSTRACT)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			typeDeclaration.modifiers().add(
					ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
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