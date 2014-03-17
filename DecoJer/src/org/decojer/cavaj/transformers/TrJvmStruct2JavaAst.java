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
package org.decojer.cavaj.transformers;

import static org.decojer.cavaj.utils.Expressions.newLiteral;
import static org.decojer.cavaj.utils.Expressions.newSimpleName;
import static org.decojer.cavaj.utils.Expressions.newSingleVariableDeclaration;
import static org.decojer.cavaj.utils.Expressions.newType;
import static org.decojer.cavaj.utils.Expressions.newTypeName;

import java.lang.annotation.Annotation;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.model.types.Version;
import org.decojer.cavaj.utils.Annotations;
import org.decojer.cavaj.utils.Expressions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Transformer: JVM Struct to AST.
 * 
 * @author André Pankraz
 */
@Slf4j
public final class TrJvmStruct2JavaAst {

	private static void decompileField(final F f, final CU cu) {
		final String name = f.getName();
		final T t = f.getT();

		if (f.isStatic()) {
			// enum synthetic fields
			if (("$VALUES".equals(name) || "ENUM$VALUES".equals(name)) && t.isEnum()
					&& !cu.check(DFlag.IGNORE_ENUM)) {
				// TODO could extract this field name from initializer for more robustness
				return;
			}
		} else {
			if (name.startsWith("this$") && t.isInner() && f.getValueT().is(t.getEnclosingT())
					&& !cu.check(DFlag.IGNORE_CONSTRUCTOR_THIS)) {
				// TODO could extract this field name from constructor for more robustness
				return;
			}
		}
		if (f.isSynthetic() && !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}
		final AST ast = cu.getAst();

		final boolean isEnum = f.isEnum();

		// decompile BodyDeclaration, possible subtypes:
		// FieldDeclaration, EnumConstantDeclaration
		final BodyDeclaration fieldDeclaration;
		if (isEnum) {
			fieldDeclaration = ast.newEnumConstantDeclaration();
			((EnumConstantDeclaration) fieldDeclaration).setName(newSimpleName(name, ast));
		} else {
			final VariableDeclarationFragment variableDeclarationFragment = ast
					.newVariableDeclarationFragment();
			variableDeclarationFragment.setName(Expressions.newSimpleName(name, ast));
			fieldDeclaration = ast.newFieldDeclaration(variableDeclarationFragment);
		}
		f.setAstNode(fieldDeclaration);

		// decompile deprecated Javadoc-tag if no annotation set
		if (f.check(AF.DEPRECATED) && !Annotations.isDeprecatedAnnotation(f.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			fieldDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations, add annotation modifiers before other modifiers, order preserved
		// in source code generation through Eclipse JDT
		Annotations.decompileAnnotations(f.getAs(), fieldDeclaration.modifiers(), t);

		final boolean isInterfaceMember = t.isInterface();

		// decompile modifier flags, public is default for enum and interface
		if (f.check(AF.PUBLIC) && !isEnum && !isInterfaceMember) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (f.check(AF.PRIVATE)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (f.check(AF.PROTECTED)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		// static is default for enum and interface
		if (f.isStatic() && !isEnum && !isInterfaceMember) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		// final is default for enum and interface
		if (f.check(AF.FINAL) && !isEnum && !isInterfaceMember) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (f.check(AF.VOLATILE)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));
		}
		if (f.check(AF.TRANSIENT)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.TRANSIENT_KEYWORD));
		}

		// not for enum constant declaration
		if (fieldDeclaration instanceof FieldDeclaration) {
			((FieldDeclaration) fieldDeclaration).setType(newType(f.getValueT(), t));
			final Object value = f.getValue();
			if (value != null) {
				// only final, non static - no arrays, class types
				final Expression expr = newLiteral(f.getValueT(), value, t, null);
				if (expr != null) {
					final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ((FieldDeclaration) fieldDeclaration)
							.fragments().get(0);
					variableDeclarationFragment.setInitializer(expr);
				}
			}
		}
	}

	private static void decompileMethod(final M m, final CU cu, final boolean strictFp) {
		final String name = m.getName();
		final T t = m.getT();

		// enum synthetic methods
		if (m.isStatic()
				&& ("values".equals(name) && m.getParamTs().length == 0 || "valueOf".equals(name)
						&& m.getParamTs().length == 1 && m.getParamTs()[0].is(String.class))
				&& t.isEnum() && !cu.check(DFlag.IGNORE_ENUM)) {
			return;
		}
		if (m.isSynthetic() && !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}
		final AST ast = cu.getAst();

		final boolean isAnnotationMember = t.check(AF.ANNOTATION);

		// decompile BodyDeclaration, possible subtypes:
		// MethodDeclaration (method or constructor),
		// AnnotationTypeMemberDeclaration (all methods in @interface) or
		// Initializer (static {})
		final BodyDeclaration methodDeclaration;
		if (m.isInitializer()) {
			methodDeclaration = ast.newInitializer();
		} else if (m.isConstructor()) {
			// MethodDeclaration with type declaration name as name
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setConstructor(true);
			((MethodDeclaration) methodDeclaration).setName(newSimpleName(
					cu.check(DFlag.START_TD_ONLY) || t.isAnonymous() ? t.getPName() : t
							.getSimpleName(), ast));
		} else if (isAnnotationMember) {
			// AnnotationTypeMemberDeclaration
			methodDeclaration = ast.newAnnotationTypeMemberDeclaration();
			((AnnotationTypeMemberDeclaration) methodDeclaration).setName(newSimpleName(name, ast));
			// check if default value (e.g.: byte byteTest() default 2;)
			if (m.getAnnotationDefaultValue() != null) {
				final Expression expression = Annotations.decompileAnnotationDefaultValue(t,
						m.getAnnotationDefaultValue());
				if (expression != null) {
					((AnnotationTypeMemberDeclaration) methodDeclaration).setDefault(expression);
				}
			}
		} else {
			// MethodDeclaration
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setName(newSimpleName(name, ast));
		}
		m.setAstNode(methodDeclaration);

		// decompile deprecated Javadoc-tag if no annotation set
		if (m.check(AF.DEPRECATED) && !Annotations.isDeprecatedAnnotation(m.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			methodDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations:
		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through Eclipse JDT
		Annotations.decompileAnnotations(m.getAs(), methodDeclaration.modifiers(), t);

		final boolean isInterfaceMember = t.isInterface();

		// decompile modifier flags:
		// interfaces can have default methods since JVM 8
		if (isInterfaceMember && m.getCfg() != null && !m.isStatic()) {
			if (m.getT().isBelow(Version.JVM_8)) {
				log.warn("Default methods are not known before JVM 8! Adding default keyword anyway, check this.");
			}
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.DEFAULT_KEYWORD));
		}
		// public is default for interface and annotation type declarations
		if (m.check(AF.PUBLIC) && !isAnnotationMember && !isInterfaceMember) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (m.check(AF.PRIVATE)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (m.check(AF.PROTECTED)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		if (m.isStatic()) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		if (m.check(AF.FINAL)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (m.check(AF.SYNCHRONIZED)) {
			methodDeclaration.modifiers()
					.add(ast.newModifier(ModifierKeyword.SYNCHRONIZED_KEYWORD));
		}
		if (m.check(AF.BRIDGE)) {
			// TODO
		}
		if (m.check(AF.NATIVE)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.NATIVE_KEYWORD));
		}
		// abstract is default for interface and annotation type declarations
		if (m.check(AF.ABSTRACT) && !isAnnotationMember && !isInterfaceMember) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
		}
		if (m.check(AF.STRICTFP) && !strictFp) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
		}
		/*
		 * AF.CONSTRUCTOR, AF.DECLARED_SYNCHRONIZED nothing, Dalvik only?
		 */
		if (methodDeclaration instanceof MethodDeclaration) {
			decompileTypeParams(m.getTypeParams(),
					((MethodDeclaration) methodDeclaration).typeParameters(), t);
			decompileMethodParams(m);
			if (!m.check(AF.ABSTRACT) && !m.check(AF.NATIVE)) {
				// create method block for valid syntax, abstract and native methods have none
				final Block block = ast.newBlock();
				((MethodDeclaration) methodDeclaration).setBody(block);
				if (m.getCfg() != null) {
					// could have no CFG because of empty or incomplete read code attribute
					m.getCfg().setBlock(block);
				}
			}
		} else if (methodDeclaration instanceof Initializer) {
			// Initializer (static{}) has block per default
			assert ((Initializer) methodDeclaration).getBody() != null;

			if (m.getCfg() != null) {
				// could have no CFG because of empty or incomplete read code attribute
				m.getCfg().setBlock(((Initializer) methodDeclaration).getBody());
			}
		} else if (methodDeclaration instanceof AnnotationTypeMemberDeclaration) {
			assert m.getParamTs().length == 0;

			((AnnotationTypeMemberDeclaration) methodDeclaration)
					.setType(newType(m.getReturnT(), t));
		}
	}

	/**
	 * Decompile method parameters (parameter types, return type, exception types, annotations,
	 * names).
	 * 
	 * @param m
	 *            method declaration
	 */
	@SuppressWarnings("deprecation")
	private static void decompileMethodParams(final M m) {
		// method type parameters (full signature only):
		// <T:Ljava/lang/Integer;U:Ljava/lang/Long;>(TT;TU;)V
		// <U:TT;>(TT;TU;)V
		final MethodDeclaration methodDeclaration = (MethodDeclaration) m.getAstNode();
		final T t = m.getT();
		final AST ast = t.getCu().getAst();

		final T[] paramTs = m.getParamTs();
		if (m.getReceiverT() != null) {
			methodDeclaration.setReceiverType(newType(m.getReceiverT(), t));
		}
		final A[][] paramAss = m.getParamAss();
		for (int i = 0; i < paramTs.length; ++i) {
			if (m.isConstructor()) {
				if (i <= 1 && t.isEnum() && !t.getCu().check(DFlag.IGNORE_ENUM)) {
					// enum constructors have two leading synthetic parameters,
					// enum classes are static and can not be anonymous or inner method
					if (i == 0 && m.getParamTs()[0].is(String.class)) {
						continue;
					}
					if (i == 1 && m.getParamTs()[1] == T.INT) {
						continue;
					}
				}
				if (i == 0 && t.isInner() && !t.getCu().check(DFlag.IGNORE_CONSTRUCTOR_THIS)) {
					// inner class constructor has synthetic this reference as first argument: skip
					if (m.getParamTs()[0].is(t.getEnclosingT())) {
						continue;
					}
					log.warn(m
							+ ": Inner class constructor has no synthetic this reference as first argument!");
				}
				// anonymous inner classes cannot have visible Java constructors, don't handle
				// here but ignore in merge all

				// method inner classes have extra trailing parameters for visible outer finals,
				// that are used in other methods

				// static inner classes can only have top-level or static outer,
				// anonymous inner classes are static if context is static,
				// enums are static and can not be anonymous or inner method
			}
			methodDeclaration.parameters().add(
					newSingleVariableDeclaration(m, paramTs, paramAss, i, t));
		}
		// decompile return type
		methodDeclaration.setReturnType2(newType(m.getReturnT(), t));
		// decompile exceptions
		final T[] throwsTs = m.getThrowsTs();
		if (throwsTs != null) {
			for (final T throwT : throwsTs) {
				// Eclipse AST expects a List<Name> for thrownExceptions, not a List<Type>:
				// is OK - thrownExceptions cannot be generic
				if (ast.apiLevel() <= AST.JLS4) {
					methodDeclaration.thrownExceptions().add(newTypeName(throwT, t));
				} else {
					methodDeclaration.thrownExceptionTypes().add(newType(throwT, t));
				}
			}
		}
	}

	private static void decompileType(final T t, final CU cu) {
		if (t.isSynthetic() && !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}

		// AF.STRICTFP is no valid inner modifier for bytecode, strictfp modifier at class generates
		// strictfp modifier for all method in class -> check here and oppress then in methods
		boolean strictFp = false;
		for (final Element bd : t.getDeclarations()) {
			if (!(bd instanceof M)) {
				continue;
			}
			if (!((M) bd).check(AF.STRICTFP)) {
				break;
			}
			strictFp = true;
		}
		final AST ast = cu.getAst();

		final T superT = t.getSuperT();
		if (t.getAstNode() == null) {
			AbstractTypeDeclaration typeDeclaration = null;

			// annotation type declaration
			if (t.check(AF.ANNOTATION)) {
				if (superT == null || !superT.isObject()) {
					log.warn("Classfile with AccessFlag.ANNOTATION has no super class Object but has '"
							+ superT + "'!");
				}
				if (t.getInterfaceTs().length != 1 || !t.getInterfaceTs()[0].is(Annotation.class)) {
					log.warn("Classfile with AccessFlag.ANNOTATION has no interface '"
							+ Annotation.class.getName() + "' but has '" + t.getInterfaceTs()[0]
							+ "'!");
				}
				typeDeclaration = ast.newAnnotationTypeDeclaration();
			}
			// enum declaration
			if (t.isEnum() && !t.getCu().check(DFlag.IGNORE_ENUM)) {
				if (typeDeclaration != null) {
					log.warn("Enum declaration cannot be an annotation type declaration! Ignoring.");
				} else {
					if (superT == null || !superT.isParameterized() || !superT.is(Enum.class)) {
						log.warn("Enum type '" + t + "' has no super class '"
								+ Enum.class.getName() + "' but has '" + superT + "'!");
					}
					typeDeclaration = ast.newEnumDeclaration();
					// enums cannot extend other classes than Enum.class, but can have interfaces
					if (t.getInterfaceTs() != null) {
						for (final T interfaceT : t.getInterfaceTs()) {
							((EnumDeclaration) typeDeclaration).superInterfaceTypes().add(
									newType(interfaceT, t));
						}
					}
				}
			}
			// no annotation type declaration or enum declaration => normal class or interface type
			// declaration
			if (typeDeclaration == null) {
				typeDeclaration = ast.newTypeDeclaration();
				decompileTypeParams(t.getTypeParams(),
						((TypeDeclaration) typeDeclaration).typeParameters(), t);
				if (superT != null && !superT.isObject()) {
					((TypeDeclaration) typeDeclaration).setSuperclassType(newType(superT, t));
				}
				for (final T interfaceT : t.getInterfaceTs()) {
					((TypeDeclaration) typeDeclaration).superInterfaceTypes().add(
							newType(interfaceT, t));
				}
			}
			t.setAstNode(typeDeclaration);

			// add annotation modifiers before other modifiers, order preserved in source code
			// generation through eclipse.jdt
			Annotations.decompileAnnotations(t.getAs(), typeDeclaration.modifiers(), t);

			// decompile remaining modifier flags
			if (t.check(AF.PUBLIC)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			}
			// for inner classes
			if (t.check(AF.PRIVATE)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
			}
			// for inner classes
			if (t.check(AF.PROTECTED)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
			}
			// for inner classes
			if (t.isStatic() && !t.isInterface()) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
			}
			if (t.check(AF.FINAL) && !(typeDeclaration instanceof EnumDeclaration)) {
				// enum declaration is final by default
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
			}
			if (t.isInterface()) {
				if (typeDeclaration instanceof TypeDeclaration) {
					((TypeDeclaration) typeDeclaration).setInterface(true);
				}
			} else if (!t.check(AF.SUPER) && !t.isDalvik()) {
				// modern invokesuper syntax, is always set in current JVM, but not in Dalvik or
				// inner classes info flags
				log.warn("Modern invokesuper syntax flag not set in type '" + t + "'!");
			}
			if (t.check(AF.ABSTRACT)
					&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
					&& !(typeDeclaration instanceof EnumDeclaration)
					&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
							.isInterface())) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
			}
			if (strictFp) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
			}

			final String simpleName = t.getSimpleName();
			typeDeclaration.setName(newSimpleName(
					simpleName.length() > 0 ? simpleName : t.getPName(), ast));

			if (t.check(AF.DEPRECATED) && !Annotations.isDeprecatedAnnotation(t.getAs())) {
				final Javadoc newJavadoc = ast.newJavadoc();
				final TagElement newTagElement = ast.newTagElement();
				newTagElement.setTagName("@deprecated");
				newJavadoc.tags().add(newTagElement);
				typeDeclaration.setJavadoc(newJavadoc);
			}
		}
		for (final Element bd : t.getDeclarations()) {
			if (bd instanceof F) {
				decompileField((F) bd, cu);
			}
			if (bd instanceof M) {
				decompileMethod((M) bd, cu, strictFp);
			}
		}
	}

	/**
	 * Decompile Type Parameters.
	 * 
	 * @param typeParams
	 *            Type Parameters
	 * @param typeParameters
	 *            AST Type Parameters
	 * @param contextT
	 *            Type Declaration
	 */
	private static void decompileTypeParams(final T[] typeParams,
			final List<TypeParameter> typeParameters, final T contextT) {
		if (typeParams == null) {
			return;
		}
		final AST ast = contextT.getCu().getAst();
		for (final T typeParam : typeParams) {
			final TypeParameter typeParameter = ast.newTypeParameter();
			typeParameter.setName(newSimpleName(typeParam.getName(), ast));
			Annotations
					.decompileAnnotations(typeParam.getAs(), typeParameter.modifiers(), contextT);
			final T superT = typeParam.getSuperT();
			if (superT != null && !superT.isObject()) {
				typeParameter.typeBounds().add(newType(superT, contextT));
			}
			for (final T interfaceT : typeParam.getInterfaceTs()) {
				typeParameter.typeBounds().add(newType(interfaceT, contextT));
			}
			typeParameters.add(typeParameter);
		}
	}

	/**
	 * Transform type declaration.
	 * 
	 * @param t
	 *            type declaration
	 */
	public static void transform(final T t) {
		final CU cu = t.getCu();

		if (cu.getAstNode() == null) {
			// initializes AST for compilation unit if still uninitialized
			final ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setSource(new char[0]);
			final CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
			compilationUnit.recordModifications();
			// decompile package name
			final String packageName = t.getPackageName();
			if (packageName != null) {
				final PackageDeclaration packageDeclaration = compilationUnit.getAST()
						.newPackageDeclaration();
				packageDeclaration.setName(compilationUnit.getAST().newName(packageName));
				compilationUnit.setPackage(packageDeclaration);
			}
			cu.setAstNode(compilationUnit);
		}
		if ("package-info".equals(t.getPName())) {
			// this is not a valid Java type name and is used for package annotations, we must
			// handle this here, is "interface" in JDK 5, is "abstract synthetic interface" in JDK 7
			if (!t.isInterface()) {
				log.warn("Type declaration with name 'package-info' is not an interface!");
			}
			Annotations.decompileAnnotations(t.getAs(), cu.getCompilationUnit().getPackage()
					.annotations(), t);
			return;
		}
		decompileType(t, cu);
	}

}