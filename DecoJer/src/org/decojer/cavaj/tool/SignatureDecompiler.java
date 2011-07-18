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
package org.decojer.cavaj.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.bytecode.SignatureAttribute;

import org.decojer.cavaj.model.TD;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.WildcardType;

/**
 * Signature decompiler.
 * 
 * @author Andre Pankraz
 */
public class SignatureDecompiler {

	private final static Logger LOGGER = Logger
			.getLogger(SignatureDecompiler.class.getName());

	private final AST ast;

	private int posFull = 0;

	private int posSimple = 0;

	private final String signatureFull;

	private final String signatureSimple;

	private final TD td;

	/**
	 * Construct signature decompiler.
	 * 
	 * @param td
	 *            type declaration
	 * @param signatureSimple
	 *            simple signature
	 * @param signatureFullAttribute
	 *            full signature attribute
	 */
	public SignatureDecompiler(final TD td, final String signatureSimple,
			final SignatureAttribute signatureFullAttribute) {
		assert td != null;
		assert td.getCu() != null;
		assert td.getCu().getAst() != null;
		assert signatureSimple != null;

		this.td = td;
		this.ast = td.getCu().getAst();
		this.signatureSimple = signatureSimple;
		this.signatureFull = signatureFullAttribute == null ? null
				: signatureFullAttribute.getSignature();
	}

	/**
	 * Decompile class types.
	 * 
	 * @param typeDeclaration
	 *            type declaration
	 * @param interfaces
	 *            simple signatures for interfaces
	 */
	@SuppressWarnings("unchecked")
	public void decompileClassTypes(final TypeDeclaration typeDeclaration,
			final String[] interfaces) {
		// class type parameters
		decompileTypeParameters(typeDeclaration.typeParameters());
		// super class
		final Type type = decompileType();
		if (type != null && !"Object".equals(type.toString())) {
			typeDeclaration.setSuperclassType(type);
		}
		// interfaces
		for (final String interfaceName : interfaces) {
			final Type interfaceType = decompileTypeFull();
			if (interfaceType != null) {
				typeDeclaration.superInterfaceTypes().add(interfaceType);
				continue;
			}
			typeDeclaration.superInterfaceTypes().add(
					getAST().newSimpleType(getTD().newTypeName(interfaceName)));
		}
	}

	/**
	 * Decompile field type.
	 * 
	 * @param fieldDeclaration
	 *            field declaration
	 */
	public void decompileFieldType(final FieldDeclaration fieldDeclaration) {
		fieldDeclaration.setType(decompileType());
	}

	/**
	 * Decompile method parameter types.
	 * 
	 * @return method parameter types or null
	 */
	public List<Type> decompileMethodParameterTypes() {
		// decompile full and simple signature, compare results
		final List<Type> decompileMethodParameterTypesFull = decompileMethodParameterTypesFull();
		final List<Type> decompileMethodParameterTypesSimple = decompileMethodParameterTypesSimple();
		if (decompileMethodParameterTypesFull != null
				&& decompileMethodParameterTypesFull.size() == decompileMethodParameterTypesSimple
						.size()) {
			return decompileMethodParameterTypesFull;
		}
		return decompileMethodParameterTypesSimple;
	}

	private List<Type> decompileMethodParameterTypesFull() {
		if (this.posFull < 0 || this.signatureFull == null
				|| this.posFull >= this.signatureFull.length()) {
			return null;
		}
		if (this.signatureFull.charAt(this.posFull) != '(') {
			LOGGER.log(Level.WARNING, "No method parameter types at position '"
					+ this.posFull + "' in full signature '"
					+ this.signatureFull + "'!");
			this.posFull = -1;
			return null;
		}
		final List<Type> methodParameterTypes = new ArrayList<Type>();
		++this.posFull;
		while (this.signatureFull.charAt(this.posFull) != ')'
				&& this.posFull < this.signatureFull.length()) {
			final Type type = decompileTypeFull();
			if (type == null) {
				continue;
			}
			methodParameterTypes.add(type);
		}
		if (this.signatureFull.charAt(this.posFull) != ')') {
			LOGGER.log(Level.WARNING,
					"No correct closing of method parameter types at position '"
							+ this.posFull + "' in full signature '"
							+ this.signatureFull + "'!");
			this.posFull = -1;
			return methodParameterTypes; // not perfect, but... ;)
		}
		++this.posFull;
		return methodParameterTypes;
	}

	private List<Type> decompileMethodParameterTypesSimple() {
		if (this.posSimple < 0 || this.signatureSimple == null
				|| this.posSimple >= this.signatureSimple.length()) {
			return null;
		}
		if (this.signatureSimple.charAt(this.posSimple) != '(') {
			LOGGER.log(Level.WARNING, "No method parameter types at position '"
					+ this.posSimple + "' in simple signature '"
					+ this.signatureSimple + "'!");
			this.posSimple = -1;
			return null;
		}
		final List<Type> methodParameterTypes = new ArrayList<Type>();
		++this.posSimple;
		while (this.signatureSimple.charAt(this.posSimple) != ')'
				&& this.posSimple < this.signatureSimple.length()) {
			final Type type = decompileTypeSimple();
			if (type == null) {
				continue;
			}
			methodParameterTypes.add(type);
		}
		if (this.signatureSimple.charAt(this.posSimple) != ')') {
			LOGGER.log(Level.WARNING,
					"No correct closing of method parameter types at position '"
							+ this.posSimple + "' in simple signature '"
							+ this.signatureSimple + "'!");
			this.posSimple = -1;
			return methodParameterTypes; // not perfect, but... ;)
		}
		++this.posSimple;
		return methodParameterTypes;
	}

	/**
	 * Decompile method types (parameter types, return type, exception types).
	 * 
	 * @param methodDeclaration
	 *            method declaration
	 * @param exceptions
	 *            simple signatures for exceptions
	 * @param varargs
	 *            last method parameter is vararg
	 */
	@SuppressWarnings("unchecked")
	public void decompileMethodTypes(final MethodDeclaration methodDeclaration,
			final String[] exceptions, final boolean varargs) {
		// method type parameters (full signature only):
		// <T:Ljava/lang/Integer;U:Ljava/lang/Long;>(TT;TU;)V
		// <U:TT;>(TT;TU;)V
		decompileTypeParameters(methodDeclaration.typeParameters());
		final List<Type> methodParameterTypes = decompileMethodParameterTypes();
		if (methodParameterTypes != null) {
			for (int i = 0; i < methodParameterTypes.size(); ++i) {
				final Type methosParameterType = methodParameterTypes.get(i);
				final SingleVariableDeclaration singleVariableDeclaration = getAST()
						.newSingleVariableDeclaration();
				// decompile varargs (flag set, ArrayType and last method param)
				if (varargs && i == methodParameterTypes.size() - 1) {
					if (methosParameterType instanceof ArrayType) {
						singleVariableDeclaration.setVarargs(true);
						singleVariableDeclaration.setType((Type) ASTNode
								.copySubtree(getAST(),
										((ArrayType) methosParameterType)
												.getComponentType()));
					} else {
						LOGGER.warning("Last method parameter is no ArrayType, but method '"
								+ methodDeclaration.getName()
								+ "' has vararg attribute!");
						// try handling as normal type
						singleVariableDeclaration.setType(methosParameterType);
					}
				} else {
					singleVariableDeclaration.setType(methosParameterType);
				}
				methodDeclaration.parameters().add(singleVariableDeclaration);
			}
		}
		// decompile return type
		final Type returnType = decompileType();
		if (returnType != null) {
			methodDeclaration.setReturnType2(returnType);
		}
		// decompile exceptions
		if (exceptions != null) {
			for (final String exception : exceptions) {
				if (this.posFull > 0 && this.signatureFull != null
						&& this.signatureFull.length() > this.posFull
						&& this.signatureFull.charAt(this.posFull++) == '^') {
					final Type exceptionType = decompileTypeFull();
					if (exceptionType != null) {
						methodDeclaration.thrownExceptions().add(
								getTD().newTypeName(exceptionType.toString()));
						continue;
					}
				}
				methodDeclaration.thrownExceptions().add(
						getTD().newTypeName(exception));
			}
		}
	}

	/**
	 * Decompile single type.
	 * 
	 * @return type
	 */
	public Type decompileType() {
		// decompile full and simple signature, compare results
		final Type typeFull = decompileTypeFull();
		final Type typeSimple = decompileTypeSimple();
		if (typeFull != null) {
			return typeFull;
		}
		return typeSimple;
	}

	/**
	 * Decompile single type from full signature.
	 * 
	 * @return type
	 */
	@SuppressWarnings("unchecked")
	private Type decompileTypeFull() {
		if (this.posFull < 0 || this.signatureFull == null
				|| this.posFull >= this.signatureFull.length()) {
			return null;
		}
		final char id = this.signatureFull.charAt(this.posFull++);
		switch (id) {
		case 'V':
			return getAST().newPrimitiveType(PrimitiveType.VOID);
		case 'B':
			return getAST().newPrimitiveType(PrimitiveType.BYTE);
		case 'C':
			return getAST().newPrimitiveType(PrimitiveType.CHAR);
		case 'D':
			return getAST().newPrimitiveType(PrimitiveType.DOUBLE);
		case 'F':
			return getAST().newPrimitiveType(PrimitiveType.FLOAT);
		case 'I':
			return getAST().newPrimitiveType(PrimitiveType.INT);
		case 'J':
			return getAST().newPrimitiveType(PrimitiveType.LONG);
		case 'S':
			return getAST().newPrimitiveType(PrimitiveType.SHORT);
		case 'Z':
			return getAST().newPrimitiveType(PrimitiveType.BOOLEAN);
		case '[': {
			final Type type = decompileTypeFull();
			if (type == null) {
				return null;
			}
			return getAST().newArrayType(type);
		}
		case 'L': {
			final int posFull1 = this.signatureFull.indexOf('<', this.posFull);
			final int posFull2 = this.signatureFull.indexOf(';', this.posFull);
			if (posFull1 != -1 && posFull1 < posFull2) {
				final String className = this.signatureFull.substring(
						this.posFull, posFull1).replace('/', '.');
				final ParameterizedType newParameterizedType = getAST()
						.newParameterizedType(
								getAST().newSimpleType(
										getTD().newTypeName(className)));
				this.posFull = posFull1 + 1;
				while (this.signatureFull.charAt(this.posFull) != '>') {
					final char wildcard = this.signatureFull
							.charAt(this.posFull);
					switch (wildcard) {
					case '+': {
						++this.posFull;
						final Type type = decompileTypeFull();
						if (type == null) {
							return null;
						}
						final WildcardType newWildcardType = getAST()
								.newWildcardType();
						newWildcardType.setUpperBound(true);
						newWildcardType.setBound(type);
						newParameterizedType.typeArguments().add(
								newWildcardType);
						break;
					}
					case '-': {
						++this.posFull;
						final Type type = decompileTypeFull();
						if (type == null) {
							return null;
						}
						final WildcardType newWildcardType = getAST()
								.newWildcardType();
						newWildcardType.setUpperBound(false);
						newWildcardType.setBound(type);
						newParameterizedType.typeArguments().add(
								newWildcardType);
						break;
					}
					default: {
						final Type type = decompileTypeFull();
						if (type == null) {
							return null;
						}
						newParameterizedType.typeArguments().add(type);
					}
					}
				}
				// should be '>;', could check more, but we are not restrictive
				this.posFull += 2;
				return newParameterizedType;
			}
			final String className = this.signatureFull.substring(this.posFull,
					posFull2).replace('/', '.');
			this.posFull = posFull2 + 1;
			return getAST().newSimpleType(getTD().newTypeName(className));
		}
		case 'T': {
			final int posFull1 = this.signatureFull.indexOf(';', this.posFull);
			final SimpleType newSimpleType = getAST().newSimpleType(
					getAST().newSimpleName(
							this.signatureFull
									.substring(this.posFull, posFull1).replace(
											'/', '.')));
			this.posFull = posFull1 + 1;
			return newSimpleType;
		}
		default:
			LOGGER.log(Level.WARNING, "Unknown type id '" + id
					+ "' at position '" + --this.posFull
					+ "' in full signature '" + this.signatureFull + "'!");
			this.posFull = -1;
			return null;
		}
	}

	/**
	 * Decompile type parameters.
	 * 
	 * @param list
	 *            type paramater list
	 */
	@SuppressWarnings("unchecked")
	private void decompileTypeParameters(final List<TypeParameter> list) {
		if (this.posFull < 0 || this.signatureFull == null
				|| this.posFull >= this.signatureFull.length()) {
			return;
		}
		if (this.signatureFull.charAt(this.posFull) != '<') {
			return;
		}
		++this.posFull;
		while (this.signatureFull.charAt(this.posFull) != '>') {
			final TypeParameter newTypeParameter = getAST().newTypeParameter();
			// <T extends Map<? extends E, ? extends Object>, E>
			// compiled to:
			// <T::Ljava/util/Map<+TE;+Ljava/lang/Object;>;E:Ljava/lang/Object;>
			// <T extends Integer & Accessible>
			// compiled to:
			// <T:Ljava/lang/Integer;:Ljavax/accessibility/Accessible;>Ljava/lang/Object;
			final int posFull1 = this.signatureFull.indexOf(':', this.posFull);
			final int posFull2 = this.signatureFull.indexOf('>', this.posFull);
			if (posFull1 != -1 && posFull1 < posFull2) {
				newTypeParameter.setName(getAST().newSimpleName(
						this.signatureFull.substring(this.posFull, posFull1)));
				this.posFull = posFull1 + 1;
				if (this.signatureFull.charAt(this.posFull) == ':') {
					++this.posFull; // treat interface bounds like class bounds
				}
				final Type type = decompileTypeFull();
				// ignore simple type bounds <E extends java.lang.Object>
				if (type != null && !"Object".equals(type.toString())) {
					newTypeParameter.typeBounds().add(type);
				}
			} else {
				newTypeParameter.setName(getAST().newSimpleName(
						this.signatureFull.substring(this.posFull, posFull2)));
				this.posFull = posFull2 + 1;
			}
			list.add(newTypeParameter);
		}
		++this.posFull;
	}

	/**
	 * Decompile single type from simple signature.
	 * 
	 * @return type
	 */
	private Type decompileTypeSimple() {
		if (this.posSimple < 0 || this.signatureSimple == null
				|| this.posSimple >= this.signatureSimple.length()) {
			return null;
		}
		final char id = this.signatureSimple.charAt(this.posSimple++);
		switch (id) {
		case 'V':
			return getAST().newPrimitiveType(PrimitiveType.VOID);
		case 'B':
			return getAST().newPrimitiveType(PrimitiveType.BYTE);
		case 'C':
			return getAST().newPrimitiveType(PrimitiveType.CHAR);
		case 'D':
			return getAST().newPrimitiveType(PrimitiveType.DOUBLE);
		case 'F':
			return getAST().newPrimitiveType(PrimitiveType.FLOAT);
		case 'I':
			return getAST().newPrimitiveType(PrimitiveType.INT);
		case 'J':
			return getAST().newPrimitiveType(PrimitiveType.LONG);
		case 'S':
			return getAST().newPrimitiveType(PrimitiveType.SHORT);
		case 'Z':
			return getAST().newPrimitiveType(PrimitiveType.BOOLEAN);
		case '[': {
			final Type type = decompileTypeSimple();
			if (type == null) {
				return null;
			}
			return getAST().newArrayType(type);
		}
		case 'L': {
			final int pos = this.signatureSimple.indexOf(';', this.posSimple);
			final String className = this.signatureSimple.substring(
					this.posSimple, pos).replace('/', '.');
			this.posSimple = pos + 1;
			return getAST().newSimpleType(getTD().newTypeName(className));
		}
		default:
			LOGGER.log(Level.WARNING, "Unknown type id '" + id
					+ "' at position '" + --this.posSimple
					+ "' in simple signature '" + this.signatureSimple + "'!");
			this.posSimple = -1;
			return null;
		}
	}

	private AST getAST() {
		return this.ast;
	}

	private TD getTD() {
		return this.td;
	}

}