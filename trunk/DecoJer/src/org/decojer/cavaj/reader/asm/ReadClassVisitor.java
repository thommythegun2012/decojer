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
package org.decojer.cavaj.reader.asm;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Read class visitor.
 * 
 * @author André Pankraz
 */
public class ReadClassVisitor implements ClassVisitor {

	private final static Logger LOGGER = Logger
			.getLogger(ReadClassVisitor.class.getName());

	private final DU du;

	private ReadFieldVisitor readFieldVisitor;

	private ReadMethodVisitor readMethodVisitor;

	private TD td;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadClassVisitor(final DU du) {
		this.du = du;
	}

	/**
	 * Get type declaration.
	 * 
	 * @return type declaration
	 */
	public TD getTd() {
		return this.td;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		// visit:
		// com/thoughtworks/xstream/mapper/AnnotationMapper$UnprocessedTypesSet
		// : Ljava/util/LinkedHashSet<Ljava/lang/Class<*>;>; :
		// java/util/LinkedHashSet : [Ljava.lang.String;@1b9a2fd

		final T t = this.du.getT(name.replace('/', '.'));
		t.setSuperT(this.du.getT(superName.replace('/', '.')));
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = this.du.getT(interfaces[i].replace('/', '.'));
			}
			t.setInterfaceTs(interfaceTs);
		}
		if (signature != null) {
			t.setSignature(signature.replace('/', '.'));
		}

		this.td = new TD(t);
		this.td.setAccessFlags(access);
		this.td.setVersion(version);

		this.readFieldVisitor = new ReadFieldVisitor(this);
		this.readMethodVisitor = new ReadMethodVisitor(this);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
			final boolean visible) {
		LOGGER.warning("### visitAnnotation ### " + desc + " : " + visible);
		return new ReadAnnotationVisitor();
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		LOGGER.log(Level.WARNING, "Unknown class attribute tag '" + attr.type
				+ "'!");
	}

	@Override
	public void visitEnd() {
		// nothing
	}

	@Override
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		System.out.println("FIELD DESC: " + desc);
		if (signature != null) {
			System.out.println("FIELD SIGN: " + signature);
		}
		final FD fd = new FD(this.td, access, name, desc, signature, value);
		this.td.getBds().add(fd);
		this.readFieldVisitor.setFd(fd);
		return this.readFieldVisitor;
	}

	@Override
	public void visitInnerClass(final String name, final String outerName,
			final String innerName, final int access) {
		LOGGER.warning("### visitInner ### " + name + " : " + outerName + " : "
				+ innerName + " : " + access);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		String mdSignature = null;
		if (signature != null) {
			mdSignature = signature.replace('/', '.');
		}

		String[] mdExceptions = null;
		if (exceptions != null && exceptions.length > 0) {
			mdExceptions = new String[exceptions.length];
			for (int i = exceptions.length; i-- > 0;) {
				mdExceptions[i] = exceptions[i].replace('/', '.');
			}
		}

		final MD md = new MD(this.td, access, name, desc.replace('/', '.'),
				mdSignature, mdExceptions);
		this.td.getBds().add(md);
		this.readMethodVisitor.setMd(md);
		return this.readMethodVisitor;
	}

	@Override
	public void visitOuterClass(final String owner, final String name,
			final String desc) {
		LOGGER.warning("### visitOuter ### " + owner + " : " + name + " : "
				+ desc);
	}

	@Override
	public void visitSource(final String source, final String debug) {
		if (debug != null) {
			// what is that?
			LOGGER.warning("### visitSource debug? ###: " + debug);
		}
		this.td.setSourceFileName(source);
	}

}