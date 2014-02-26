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
package org.decojer.cavaj.readers.asm;

import static org.decojer.cavaj.readers.asm.ReadUtils.annotateT;

import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.Getter;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.T;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

/**
 * ASM read class visitor.
 * 
 * @author André Pankraz
 */
public class ReadClassVisitor extends ClassVisitor {

	private final static Logger LOGGER = Logger.getLogger(ReadClassVisitor.class.getName());

	private A[] as;

	private final DU du;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	private final ReadFieldVisitor readFieldVisitor;

	private final ReadMethodVisitor readMethodVisitor;

	@Getter(AccessLevel.PROTECTED)
	private T t;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadClassVisitor(final DU du) {
		super(Opcodes.ASM5);
		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
		this.readFieldVisitor = new ReadFieldVisitor(du);
		this.readMethodVisitor = new ReadMethodVisitor(du);
	}

	/**
	 * Init.
	 */
	public void init() {
		this.as = null;
		this.t = null;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName, final String[] interfaces) {
		final T t = this.du.getT(name);
		if (t.isDeclaration()) {
			LOGGER.warning(this.t + ": Type '" + t + "' already read!");
			throw new ReadException();
		}
		this.t = t.createTd();
		this.t.setAccessFlags(access);
		this.t.setSuperT(this.du.getT(superName));
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = this.du.getT(interfaces[i]);
			}
			this.t.setInterfaceTs(interfaceTs);
		}
		this.t.setSignature(signature);

		// fix ASM bug: mixup of minor and major (which is 196653),
		// only JDK 1.1 class files use a minor number (45.3),
		// JDK 1.1 - JDK 1.3 create this version without a target option
		this.t.setVersion(version & 0xffff);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readAnnotationMemberVisitor.init(desc,
				visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		if ("Scala".equals(attr.type) || "ScalaSig".equals(attr.type)) {
			this.t.setScala();
			return;
		}
		LOGGER.warning(this.t + ": Unknown class attribute tag '" + attr.type + "'!");
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.t.setAs(this.as);
		}
		this.t.resolve();
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String desc,
			final String signature, final Object value) {
		final F f = this.t.createFd(name, desc);

		f.setAccessFlags(access);
		f.setSignature(signature);

		f.setValue(value);

		this.readFieldVisitor.init(f);
		return this.readFieldVisitor;
	}

	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName,
			final int access) {
		final ClassT innerT = (ClassT) this.du.getT(name);
		if (outerName != null) {
			// set enclosing first for better inner name check
			innerT.setEnclosingT(this.du.getT(outerName));
		}
		innerT.setInnerInfo(innerName, access);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc,
			final String signature, final String[] exceptions) {
		final M m = this.t.createMd(name, desc);

		m.setAccessFlags(access);
		if (exceptions != null && exceptions.length > 0) {
			final T[] throwsTs = new T[exceptions.length];
			for (int i = exceptions.length; i-- > 0;) {
				throwsTs[i] = this.du.getT(exceptions[i]);
			}
			m.setThrowsTs(throwsTs);
		}
		m.setSignature(signature);

		this.readMethodVisitor.init(m);
		return this.readMethodVisitor;
	}

	@Override
	public void visitOuterClass(final String owner, final String name, final String desc) {
		final ClassT enclosingT = (ClassT) this.du.getT(owner);
		if (name == null) {
			this.t.setEnclosingT(enclosingT);
		} else {
			this.t.setEnclosingM(enclosingT.getM(name, desc));
		}
	}

	@Override
	public void visitSource(final String source, final String debug) {
		if (debug != null) {
			// TODO need an example, really useful in the wild?
			// JVM spec: 4.7.11 The SourceDebugExtension Attribute
			LOGGER.warning(this.t + ": " + debug);
		}
		this.t.setSourceFileName(source);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath,
			final String desc, final boolean visible) {
		final A a = this.readAnnotationMemberVisitor.init(desc, visible ? RetentionPolicy.RUNTIME
				: RetentionPolicy.CLASS);
		final TypeReference typeReference = new TypeReference(typeRef);
		switch (typeReference.getSort()) {
		case TypeReference.CLASS_EXTENDS: {
			final int superTypeIndex = typeReference.getSuperTypeIndex();
			if (superTypeIndex == -1) {
				// -1: annotation targets extends type
				this.t.setSuperT(annotateT(this.t.getSuperT(), a, typePath));
			} else {
				// 0-based interface index
				final T[] interfaceTs = this.t.getInterfaceTs();
				if (superTypeIndex < interfaceTs.length) {
					interfaceTs[superTypeIndex] = annotateT(interfaceTs[superTypeIndex], a,
							typePath);
				} else {
					LOGGER.warning("Super type index '" + superTypeIndex + "' is to large for '"
							+ this.t + "'!");
				}
			}
			break;
		}
		case TypeReference.CLASS_TYPE_PARAMETER: {
			final int typeParameterIndex = typeReference.getTypeParameterIndex();
			final T[] typeParams = this.t.getTypeParams();
			typeParams[typeParameterIndex] = annotateT(typeParams[typeParameterIndex], a, typePath);
			break;
		}
		case TypeReference.CLASS_TYPE_PARAMETER_BOUND: {
			final int typeParameterIndex = typeReference.getTypeParameterIndex();
			final int typeParameterBoundIndex = typeReference.getTypeParameterBoundIndex();
			final T t = this.t.getTypeParams()[typeParameterIndex];
			if (typeParameterBoundIndex == 0) {
				// 0: annotation targets extends type
				t.setSuperT(annotateT(t.getSuperT(), a, typePath));
			} else {
				// 1-based interface index
				final T[] interfaceTs = t.getInterfaceTs();
				if (typeParameterBoundIndex - 1 < interfaceTs.length) {
					interfaceTs[typeParameterBoundIndex - 1] = annotateT(
							interfaceTs[typeParameterBoundIndex - 1], a, typePath);
				} else {
					LOGGER.warning("Type parameter bound index '" + (typeParameterBoundIndex - 1)
							+ "' is to large for '" + this.t + "'!");
				}
			}
			break;
		}
		default:
			LOGGER.warning(this.t + ": Unknown type annotation ref sort '0x"
					+ Integer.toHexString(typeReference.getSort()) + "' : " + typeRef + " : "
					+ typePath + " : " + desc + " : " + visible);
		}
		return this.readAnnotationMemberVisitor;
	}

}