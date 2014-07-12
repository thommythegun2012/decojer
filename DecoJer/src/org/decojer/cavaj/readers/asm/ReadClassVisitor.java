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

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.ReadVisitor;
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
@Slf4j
public class ReadClassVisitor extends ClassVisitor implements ReadVisitor {

	private A[] as;

	@Getter
	@Nonnull
	private final DU du;

	@Nonnull
	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	@Nonnull
	private final ReadFieldVisitor readFieldVisitor;

	@Nonnull
	private final ReadMethodVisitor readMethodVisitor;

	@Getter
	private T t;

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public ReadClassVisitor(@Nonnull final DU du) {
		super(Opcodes.ASM5);
		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(this);
		this.readFieldVisitor = new ReadFieldVisitor(this);
		this.readMethodVisitor = new ReadMethodVisitor(this);
	}

	@Override
	public ReadVisitor getParentVisitor() {
		return null;
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
		if (name == null) {
			log.warn("Cannot read type name '" + name + "'!");
			// We can only stop further type reading via an exception in this visitor based system.
			throw new ReadClassStopException();
		}
		final T t = this.du.getT(name);
		if (!t.createTd()) {
			log.warn("Type '" + t + "' already read!");
			// We can only stop further type reading via an exception in this visitor based system.
			throw new ReadClassStopException();
		}
		this.t = t;
		getT().setAccessFlags(access);
		if (superName != null) {
			getT().setSuperT(this.du.getT(superName));
		}
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				final String interfaceName = interfaces[i];
				assert interfaceName != null;
				interfaceTs[i] = this.du.getT(interfaceName);
			}
			getT().setInterfaceTs(interfaceTs);
		}
		getT().setSignature(signature);

		// fix ASM bug: mixup of minor and major (which is 196653),
		// only JVM 1.1 class files use a minor number (45.3),
		// JDK 1.1 - JDK 1.3 create this version without a target option
		getT().setVersion(version & 0xffff);
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
			getT().setScala();
			return;
		}
		log.warn(this.t + ": Unknown class attribute tag '" + attr.type + "'!");
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			getT().setAs(this.as);
		}
		getT().resolve();
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String desc,
			final String signature, final Object value) {
		if (name == null || desc == null) {
			return null;
		}
		final F f = getT().getF(name, desc);
		f.createFd();

		f.setAccessFlags(access);
		f.setSignature(signature);

		f.setValue(value);

		this.readFieldVisitor.init(f);
		return this.readFieldVisitor;
	}

	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName,
			final int access) {
		if (name == null) {
			return;
		}
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
		if (name == null || desc == null) {
			return null;
		}
		final M m = getT().getM(name, desc);
		m.createMd();

		m.setAccessFlags(access);
		if (exceptions != null && exceptions.length > 0) {
			final T[] throwsTs = new T[exceptions.length];
			for (int i = exceptions.length; i-- > 0;) {
				final String exception = exceptions[i];
				assert exception != null;
				throwsTs[i] = this.du.getT(exception);
			}
			m.setThrowsTs(throwsTs);
		}
		m.setSignature(signature);

		this.readMethodVisitor.init(m);
		return this.readMethodVisitor;
	}

	@Override
	public void visitOuterClass(final String owner, final String name, final String desc) {
		if (owner == null) {
			assert false;
			return;
		}
		final ClassT enclosingT = (ClassT) this.du.getT(owner);
		if (name == null || desc == null) {
			getT().setEnclosingT(enclosingT);
		} else {
			getT().setEnclosingM(enclosingT.getM(name, desc));
		}
	}

	@Override
	public void visitSource(final String source, final String debug) {
		if (debug != null) {
			// TODO need an example, really useful in the wild?
			// JVM spec: 4.7.11 The SourceDebugExtension Attribute
			log.warn(this.t + ": " + debug);
		}
		getT().setSourceFileName(source);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath,
			final String desc, final boolean visible) {
		final A a = this.readAnnotationMemberVisitor.init(desc, visible ? RetentionPolicy.RUNTIME
				: RetentionPolicy.CLASS);
		if (a == null) {
			log.warn(getT() + ": Cannot read annotation for descriptor '" + desc + "'!");
			return null;
		}
		final TypeReference typeReference = new TypeReference(typeRef);
		switch (typeReference.getSort()) {
		case TypeReference.CLASS_EXTENDS: {
			final int superTypeIndex = typeReference.getSuperTypeIndex();
			if (superTypeIndex == -1) {
				// -1: annotation targets extends type
				final T superT = getT().getSuperT();
				if (superT == null) {
					log.warn(getT() + ": Cannot apply type annotation '" + a
							+ "' to missing super type!");
					break;
				}
				getT().setSuperT(annotateT(superT, a, typePath));
				break;
			}
			// 0-based interface index
			final T[] interfaceTs = getT().getInterfaceTs();
			if (superTypeIndex <= interfaceTs.length) {
				log.warn(getT() + ": Cannot apply type annotation '" + a
						+ "' to missing interface type at index '" + superTypeIndex + "'!");
				break;
			}
			final T interfaceT = interfaceTs[superTypeIndex];
			assert interfaceT != null;
			interfaceTs[superTypeIndex] = annotateT(interfaceT, a, typePath);
			break;
		}
		case TypeReference.CLASS_TYPE_PARAMETER: {
			final int typeParameterIndex = typeReference.getTypeParameterIndex();
			final T[] typeParams = getT().getTypeParams();
			if (typeParams.length <= typeParameterIndex) {
				log.warn(getT() + ": Cannot apply type annotation '" + a
						+ "' to missing type parameter at index '" + typeParameterIndex + "'!");
				break;
			}
			final T typeParam = typeParams[typeParameterIndex];
			assert typeParam != null;
			typeParams[typeParameterIndex] = annotateT(typeParam, a, typePath);
			break;
		}
		case TypeReference.CLASS_TYPE_PARAMETER_BOUND: {
			final int typeParameterIndex = typeReference.getTypeParameterIndex();
			final int typeParameterBoundIndex = typeReference.getTypeParameterBoundIndex();
			final T t = getT().getTypeParams()[typeParameterIndex];
			if (typeParameterBoundIndex == 0) {
				// 0: annotation targets extends type
				final T superT = t.getSuperT();
				if (superT == null) {
					log.warn(getT() + ": Cannot apply type annotation '" + a
							+ "' to missing type parameter bound super type!");
					break;
				}
				t.setSuperT(annotateT(superT, a, typePath));
				break;
			}
			// 1-based interface index
			final T[] interfaceTs = t.getInterfaceTs();
			if (interfaceTs.length <= typeParameterBoundIndex - 1) {
				log.warn(getT() + ": Cannot apply type annotation '" + a
						+ "' to missing interface type at index '" + (typeParameterBoundIndex - 1)
						+ "'!");
				break;
			}
			final T interfaceT = interfaceTs[typeParameterBoundIndex - 1];
			assert interfaceT != null;
			interfaceTs[typeParameterBoundIndex - 1] = annotateT(interfaceT, a, typePath);
			break;
		}
		default:
			log.warn(this.t + ": Unknown type annotation ref sort '0x"
					+ Integer.toHexString(typeReference.getSort()) + "' : " + typeRef + " : "
					+ typePath + " : " + desc + " : " + visible);
		}
		return this.readAnnotationMemberVisitor;
	}

}