/*
 * $Id$
 *
 * This file is part of the DecoJer projectd.
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

import static org.decojer.cavaj.utils.Expressions.decompileLiteral;
import static org.decojer.cavaj.utils.Expressions.decompileSimpleName;
import static org.decojer.cavaj.utils.Expressions.decompileType;
import static org.decojer.cavaj.utils.Expressions.decompileTypeName;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.model.types.ParamT;
import org.decojer.cavaj.utils.Annotations;
import org.decojer.cavaj.utils.Expressions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
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
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Transformer: JVM Struct to AST.
 * 
 * @author André Pankraz
 */
public final class TrJvmStruct2JavaAst {

	private final static Logger LOGGER = Logger.getLogger(TrJvmStruct2JavaAst.class.getName());

	private static void decompileField(final FD fd, final CU cu) {
		if (fd.check(AF.SYNTHETIC) && !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}
		final String name = fd.getName();
		final TD td = fd.getTd();

		// enum synthetic fields
		if (("$VALUES".equals(name) || "ENUM$VALUES".equals(name)) && td.check(AF.ENUM)
				&& !cu.check(DFlag.IGNORE_ENUM)) {
			// could extract this field name from initializer for more robustness
			return;
		}
		if (name.startsWith("this$") && !cu.check(DFlag.START_TD_ONLY)) {
			// cache for outer none-static context
			return;
		}

		final ASTNode typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		final boolean isFieldEnum = fd.check(AF.ENUM);

		// decompile BodyDeclaration, possible subtypes:
		// FieldDeclaration, EnumConstantDeclaration
		final BodyDeclaration fieldDeclaration;
		if (isFieldEnum) {
			fieldDeclaration = ast.newEnumConstantDeclaration();
			((EnumConstantDeclaration) fieldDeclaration).setName(decompileSimpleName(name, ast));
		} else {
			final VariableDeclarationFragment variableDeclarationFragment = ast
					.newVariableDeclarationFragment();
			variableDeclarationFragment.setName(Expressions.decompileSimpleName(name, ast));
			fieldDeclaration = ast.newFieldDeclaration(variableDeclarationFragment);
		}
		fd.setFieldDeclaration(fieldDeclaration);

		// decompile deprecated Javadoc-tag if no annotation set
		if (fd.check(AF.DEPRECATED) && !Annotations.isDeprecatedAnnotation(fd.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			fieldDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations, add annotation modifiers before other modifiers, order preserved
		// in source code generation through Eclipse JDT
		if (fd.getAs() != null) {
			Annotations.decompileAnnotations(td, fieldDeclaration.modifiers(), fd.getAs());
		}

		// decompile modifier flags, public is default for enum and interface
		if (fd.check(AF.PUBLIC)
				&& !isFieldEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (fd.check(AF.PRIVATE)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (fd.check(AF.PROTECTED)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		// static is default for enum and interface
		if (fd.check(AF.STATIC)
				&& !isFieldEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		// final is default for enum and interface
		if (fd.check(AF.FINAL)
				&& !isFieldEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (fd.check(AF.VOLATILE)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));
		}
		if (fd.check(AF.TRANSIENT)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.TRANSIENT_KEYWORD));
		}

		// not for enum constant declaration
		if (fieldDeclaration instanceof FieldDeclaration) {
			((FieldDeclaration) fieldDeclaration).setType(decompileType(fd.getValueT(), td));
			final Object value = fd.getValue();
			if (value != null) {
				// only final, non static - no arrays, class types
				final Expression expr = decompileLiteral(fd.getValueT(), value, td, null);
				if (expr != null) {
					final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ((FieldDeclaration) fieldDeclaration)
							.fragments().get(0);
					variableDeclarationFragment.setInitializer(expr);
				}
			}
		}
	}

	private static void decompileMethod(final MD md, final CU cu, final boolean strictFp) {
		if (md.check(AF.SYNTHETIC) && !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}
		final String name = md.getName();
		final TD td = md.getTd();

		// enum synthetic methods
		if (("values".equals(name) && md.getParamTs().length == 0 || "valueOf".equals(name)
				&& md.getParamTs().length == 1 && md.getParamTs()[0].is(String.class))
				&& td.check(AF.ENUM) && !cu.check(DFlag.IGNORE_ENUM)) {
			return;
		}

		final ASTNode typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		// decompile BodyDeclaration, possible subtypes:
		// MethodDeclaration (method or constructor),
		// AnnotationTypeMemberDeclaration (all methods in @interface) or
		// Initializer (static {})
		final BodyDeclaration methodDeclaration;
		if (md.isInitializer()) {
			methodDeclaration = ast.newInitializer();
		} else if (md.isConstructor()) {
			// MethodDeclaration with type declaration name as name
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setConstructor(true);
			((MethodDeclaration) methodDeclaration).setName(decompileSimpleName(
					cu.check(DFlag.START_TD_ONLY) || td.isAnonymous() ? td.getPName() : td
							.getSimpleName(), ast));
		} else if (typeDeclaration instanceof AnnotationTypeDeclaration) {
			// AnnotationTypeMemberDeclaration
			methodDeclaration = ast.newAnnotationTypeMemberDeclaration();
			((AnnotationTypeMemberDeclaration) methodDeclaration).setName(decompileSimpleName(name,
					ast));
			// check if default value (e.g.: byte byteTest() default 2;)
			if (md.getAnnotationDefaultValue() != null) {
				final Expression expression = Annotations.decompileAnnotationDefaultValue(td,
						md.getAnnotationDefaultValue());
				if (expression != null) {
					((AnnotationTypeMemberDeclaration) methodDeclaration).setDefault(expression);
				}
			}
		} else {
			// MethodDeclaration
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setName(decompileSimpleName(name, ast));
		}
		md.setMethodDeclaration(methodDeclaration);

		// decompile deprecated Javadoc-tag if no annotation set
		if (md.check(AF.DEPRECATED) && !Annotations.isDeprecatedAnnotation(md.getAs())) {
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
			Annotations.decompileAnnotations(td, methodDeclaration.modifiers(), md.getAs());
		}

		// decompile modifier flags,
		// public is default for interface and annotation type declarations
		if (md.check(AF.PUBLIC)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (md.check(AF.PRIVATE)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (md.check(AF.PROTECTED)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		if (md.check(AF.STATIC)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		if (md.check(AF.FINAL)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (md.check(AF.SYNCHRONIZED)) {
			methodDeclaration.modifiers()
					.add(ast.newModifier(ModifierKeyword.SYNCHRONIZED_KEYWORD));
		}
		if (md.check(AF.BRIDGE)) {
			// TODO
		}
		if (md.check(AF.NATIVE)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.NATIVE_KEYWORD));
		}
		// abstract is default for interface and annotation type declarations
		if (md.check(AF.ABSTRACT)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
		}
		if (md.check(AF.STRICTFP) && !strictFp) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
		}
		/*
		 * AF.CONSTRUCTOR, AF.DECLARED_SYNCHRONIZED nothing, Dalvik only?
		 */
		if (methodDeclaration instanceof MethodDeclaration) {
			decompileTypeParams(md.getTypeParams(),
					((MethodDeclaration) methodDeclaration).typeParameters(), td);
			decompileMethodParams(md);
			if (!md.check(AF.ABSTRACT) && !md.check(AF.NATIVE)) {
				// create method block for valid syntax, abstract and native methods have none
				final Block block = ast.newBlock();
				((MethodDeclaration) methodDeclaration).setBody(block);
				if (md.getCfg() != null) {
					// could have no CFG because of empty or incomplete read code attribute
					md.getCfg().setBlock(block);
				}
			}
		} else if (methodDeclaration instanceof Initializer) {
			// Initializer (static{}) has block per default
			assert ((Initializer) methodDeclaration).getBody() != null;

			if (md.getCfg() != null) {
				// could have no CFG because of empty or incomplete read code attribute
				md.getCfg().setBlock(((Initializer) methodDeclaration).getBody());
			}
		} else if (methodDeclaration instanceof AnnotationTypeMemberDeclaration) {
			assert md.getParamTs().length == 0;

			((AnnotationTypeMemberDeclaration) methodDeclaration).setType(decompileType(
					md.getReturnT(), td));
		}
	}

	/**
	 * Decompile method parameters (parameter types, return type, exception types, annotations,
	 * names).
	 * 
	 * @param md
	 *            method declaration
	 */
	private static void decompileMethodParams(final MD md) {
		// method type parameters (full signature only):
		// <T:Ljava/lang/Integer;U:Ljava/lang/Long;>(TT;TU;)V
		// <U:TT;>(TT;TU;)V
		final MethodDeclaration methodDeclaration = (MethodDeclaration) md.getMethodDeclaration();
		final TD td = md.getTd();
		final AST ast = td.getCu().getAst();

		final T[] paramTs = md.getParamTs();
		final A[][] paramAs = md.getParamAss();
		for (int i = 0; i < paramTs.length; ++i) {
			final Type methodParameterType = decompileType(paramTs[i], td);
			if (methodDeclaration.isConstructor()) {

				if (i <= 1 && td.check(AF.ENUM) && !td.getCu().check(DFlag.IGNORE_ENUM)) {
					// enum constructors have two leading synthetic parameters,
					// enum classes are static and can not be anonymous or inner method
					if (i == 0 && md.getParamTs()[0].is(String.class)) {
						continue;
					}
					if (i == 1 && md.getParamTs()[1] == T.INT) {
						continue;
					}
				}
				// anonymous inner classes cannot have visible Java constructors, don't handle
				// here but ignore in merge all

				// method inner classes have extra trailing parameters for visible outer finals,
				// that are used in other methods

				// static inner classes can only have top-level or static outer,
				// anonymous inner classes are static if context is static,
				// enums are static and can not be anonymous or inner method
			}
			final SingleVariableDeclaration singleVariableDeclaration = ast
					.newSingleVariableDeclaration();
			if (paramAs != null && i < paramAs.length) {
				Annotations.decompileAnnotations(td, singleVariableDeclaration.modifiers(),
						paramAs[i]);
			}
			// decompile varargs (flag set, ArrayType and last method param)
			if (i == paramTs.length - 1 && md.check(AF.VARARGS)) {
				if (methodParameterType instanceof ArrayType) {
					singleVariableDeclaration.setVarargs(true);
					// must copy because we cannot delete mandatory ArrayType.componentType
					singleVariableDeclaration.setType((Type) ASTNode.copySubtree(ast,
							((ArrayType) methodParameterType).getComponentType()));
				} else {
					log("Last method parameter is no ArrayType, but method '"
							+ methodDeclaration.getName() + "' has vararg attribute!");
					// try handling as normal type
					singleVariableDeclaration.setType(methodParameterType);
				}
			} else {
				singleVariableDeclaration.setType(methodParameterType);
			}
			singleVariableDeclaration.setName(decompileSimpleName(md.getParamName(i), ast));
			methodDeclaration.parameters().add(singleVariableDeclaration);
		}
		// decompile return type
		methodDeclaration.setReturnType2(decompileType(md.getReturnT(), td));
		// decompile exceptions
		final T[] throwsTs = md.getThrowsTs();
		if (throwsTs != null) {
			for (final T throwT : throwsTs) {
				// Eclipse AST expects a List<Name> for thrownExceptions, not a List<Type>:
				// is OK - thrownExceptions cannot be generic
				methodDeclaration.thrownExceptions().add(decompileTypeName(throwT, td));
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
	 * @param td
	 *            Type Declaration
	 */
	private static void decompileTypeParams(final T[] typeParams,
			final List<TypeParameter> typeParameters, final TD td) {
		if (typeParams == null) {
			return;
		}
		final AST ast = td.getCu().getAst();
		for (final T typeParam : typeParams) {
			final TypeParameter typeParameter = ast.newTypeParameter();
			typeParameter.setName(decompileSimpleName(typeParam.getName(), ast));
			final T superT = typeParam.getSuperT();
			if (!superT.isObject()) {
				typeParameter.typeBounds().add(decompileType(typeParam.getSuperT(), td));
			}
			for (final T interfaceT : typeParam.getInterfaceTs()) {
				typeParameter.typeBounds().add(decompileType(interfaceT, td));
			}
			typeParameters.add(typeParameter);
		}
	}

	private static void log(final String message) {
		LOGGER.warning(message);
	}

	/**
	 * Transform type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public static void transform(final TD td) {
		final CU cu = td.getCu();

		if (cu.getCompilationUnit() == null) {
			// initializes AST for compilation unit if still uninitialized
			final ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setSource(new char[0]);
			final CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
			compilationUnit.recordModifications();
			// decompile package name
			final String packageName = td.getPackageName();
			if (packageName != null) {
				final PackageDeclaration packageDeclaration = compilationUnit.getAST()
						.newPackageDeclaration();
				packageDeclaration.setName(compilationUnit.getAST().newName(packageName));
				compilationUnit.setPackage(packageDeclaration);
			}
			cu.setCompilationUnit(compilationUnit);
		}
		final AST ast = cu.getAst();

		if ("package-info".equals(td.getPName())) {
			// this is not a valid Java type name and is used for package annotations, we must
			// handle this here, is "interface" in JDK 5, is "abstract synthetic interface" in JDK 7
			if (!td.isInterface()) {
				log("Type declaration with name 'package-info' is not an interface!");
			}
			if (td.getAs() != null) {
				Annotations.decompileAnnotations(td, cu.getCompilationUnit().getPackage()
						.annotations(), td.getAs());
			}
			return;
		}
		if (td.check(AF.DEPRECATED) && !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}

		// AF.STRICTFP is no valid inner modifier for bytecode, strictfp modifier at class generates
		// strictfp modifier for all method in class -> check here and oppress then in methods
		boolean strictFp = false;
		for (final BD bd : td.getBds()) {
			if (!(bd instanceof MD)) {
				continue;
			}
			if (!((MD) bd).check(AF.STRICTFP)) {
				break;
			}
			strictFp = true;
		}

		if (td.getTypeDeclaration() == null) {
			AbstractTypeDeclaration typeDeclaration = null;

			// annotation type declaration
			if (td.check(AF.ANNOTATION)) {
				if (td.getSuperT() == null || !td.getSuperT().isObject()) {
					log("Classfile with AccessFlag.ANNOTATION has no super class Object but has '"
							+ td.getSuperT() + "'!");
				}
				if (td.getInterfaceTs().length != 1 || !td.getInterfaceTs()[0].is(Annotation.class)) {
					log("Classfile with AccessFlag.ANNOTATION has no interface '"
							+ Annotation.class.getName() + "' but has '" + td.getInterfaceTs()[0]
							+ "'!");
				}
				typeDeclaration = ast.newAnnotationTypeDeclaration();
			}
			// enum declaration
			if (td.check(AF.ENUM)) {
				if (typeDeclaration != null) {
					log("Enum declaration cannot be an annotation type declaration! Ignoring.");
				} else {
					if (td.getSuperT() == null || !(td.getSuperT() instanceof ParamT)
							|| !((ParamT) td.getSuperT()).getGenericT().is(Enum.class)) {
						log("Type '" + td + "' with AccessFlag.ENUM has no super class '"
								+ Enum.class.getName() + "' but has '" + td.getSuperT() + "'!");
					}
					typeDeclaration = ast.newEnumDeclaration();
					// enums cannot extend other classes than Enum.class, but can have interfaces
					if (td.getInterfaceTs() != null) {
						for (final T interfaceT : td.getInterfaceTs()) {
							((EnumDeclaration) typeDeclaration).superInterfaceTypes().add(
									decompileType(interfaceT, td));
						}
					}
				}
			}
			// no annotation type declaration or enum declaration => normal class or interface type
			// declaration
			if (typeDeclaration == null) {
				typeDeclaration = ast.newTypeDeclaration();
				decompileTypeParams(td.getTypeParams(),
						((TypeDeclaration) typeDeclaration).typeParameters(), td);
				if (td.getSuperT() != null && !td.getSuperT().isObject()) {
					((TypeDeclaration) typeDeclaration).setSuperclassType(decompileType(
							td.getSuperT(), td));
				}
				for (final T interfaceT : td.getInterfaceTs()) {
					((TypeDeclaration) typeDeclaration).superInterfaceTypes().add(
							decompileType(interfaceT, td));
				}
			}
			td.setTypeDeclaration(typeDeclaration);

			// add annotation modifiers before other modifiers, order preserved in source code
			// generation through eclipse.jdt
			if (td.getAs() != null) {
				Annotations.decompileAnnotations(td, typeDeclaration.modifiers(), td.getAs());
			}

			// decompile remaining modifier flags
			if (td.check(AF.PUBLIC)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			}
			// for inner classes
			if (td.check(AF.PRIVATE)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
			}
			// for inner classes
			if (td.check(AF.PROTECTED)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
			}
			// for inner classes
			if (td.check(AF.STATIC)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
			}
			if (td.check(AF.FINAL) && !(typeDeclaration instanceof EnumDeclaration)) {
				// enum declaration is final by default
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
			}
			if (td.isInterface()) {
				if (typeDeclaration instanceof TypeDeclaration) {
					((TypeDeclaration) typeDeclaration).setInterface(true);
				}
			} else if (!td.check(AF.SUPER) && !td.isDalvik()) {
				// modern invokesuper syntax, is always set in current JVM, but not in Dalvik or
				// inner classes info flags
				log("Modern invokesuper syntax flag not set in type '" + td + "'!");
			}
			if (td.check(AF.ABSTRACT)
					&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
					&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
							.isInterface())) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
			}
			if (strictFp) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
			}

			final String simpleName = td.getSimpleName();
			typeDeclaration.setName(decompileSimpleName(
					simpleName.length() > 0 ? simpleName : td.getPName(), ast));

			if (td.check(AF.DEPRECATED) && !Annotations.isDeprecatedAnnotation(td.getAs())) {
				final Javadoc newJavadoc = ast.newJavadoc();
				final TagElement newTagElement = ast.newTagElement();
				newTagElement.setTagName("@deprecated");
				newJavadoc.tags().add(newTagElement);
				typeDeclaration.setJavadoc(newJavadoc);
			}
		}
		for (final BD bd : td.getBds()) {
			if (bd instanceof FD) {
				decompileField((FD) bd, cu);
			}
			if (bd instanceof MD) {
				decompileMethod((MD) bd, cu, strictFp);
			}
		}
	}

}