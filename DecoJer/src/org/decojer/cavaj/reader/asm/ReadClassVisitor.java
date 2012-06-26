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

import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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

	private TD td;

	private T[] memberTs;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadClassVisitor(final DU du) {
		super(Opcodes.ASM4);

		assert du != null;

		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
		this.readFieldVisitor = new ReadFieldVisitor(du);
		this.readMethodVisitor = new ReadMethodVisitor(du);
	}

	/**
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return this.du;
	}

	/**
	 * Get type declaration.
	 * 
	 * @return type declaration
	 */
	public TD getTd() {
		return this.td;
	}

	/**
	 * Init.
	 */
	public void init() {
		this.as = null;
		this.memberTs = null;
		this.td = null;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName, final String[] interfaces) {
		// visit:
		// com/thoughtworks/xstream/mapper/AnnotationMapper$UnprocessedTypesSet
		// : Ljava/util/LinkedHashSet<Ljava/lang/Class<*>;>; :
		// java/util/LinkedHashSet : [Ljava.lang.String;@1b9a2fd

		final TD td = (TD) this.du.getT(name);
		td.setAccessFlags(access);
		td.setSuperT(this.du.getT(superName));
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = this.du.getT(interfaces[i]);
			}
			td.setInterfaceTs(interfaceTs);
		}
		td.setSignature(signature);

		this.td = td;
		// fix ASM bug: mixup of minor and major (which is 196653),
		// only JDK 1.1 class files use a minor number (45.3),
		// JDK 1.1 - JDK 1.3 create this version without a target option
		this.td.setVersion(version & 0xffff);
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
		LOGGER.warning("Unknown class attribute tag '" + attr.type + "'!");
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.td.setAs(this.as);
		}
		if (this.memberTs != null) {
			this.td.setMemberTs(this.memberTs);
		}
		this.du.addTd(this.td);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String desc,
			final String signature, final Object value) {
		final T t = this.td.getT();
		// desc: Ljava/lang/Class;
		final T fieldT = this.du.getDescT(desc);
		final F f = t.getF(name, fieldT);
		f.setAccessFlags(access);
		f.setSignature(signature);

		final FD fd = new FD(f, this.td);
		fd.setValue(value);

		this.td.getBds().add(fd);

		this.readFieldVisitor.init(fd);
		return this.readFieldVisitor;
	}

	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName,
			final int access) {
		// Dalvik has not all Inner Class Info from JVM Bytecode:
		// Outer Class info not known in Dalvik and is derivable anyway,
		// no Access Flags for Member Classes,
		// no info for arbitrarily accessed and nested Inner Classes
		if (this.td.getName().equals(outerName)) {
			// has Member Classes (really contained Inner Classes)
			if (this.memberTs == null) {
				this.memberTs = new T[1];
			} else {
				final T[] newMemberTs = new T[this.as.length + 1];
				System.arraycopy(this.as, 0, newMemberTs, 0, this.as.length);
				this.memberTs = newMemberTs;
			}
			this.memberTs[this.memberTs.length - 1] = this.du.getT(name);
		}
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc,
			final String signature, final String[] exceptions) {
		final T t = this.td.getT();
		// desc: (Ljava/lang/String;)I
		final M m = t.getM(name, desc);
		m.setAccessFlags(access);
		if (exceptions != null && exceptions.length > 0) {
			final T[] throwsTs = new T[exceptions.length];
			for (int i = exceptions.length; i-- > 0;) {
				// e.g. java/io/IOException, without L...;
				throwsTs[i] = this.du.getT(exceptions[i]);
			}
			m.setThrowsTs(throwsTs);
		}
		m.setSignature(signature);

		final MD md = new MD(m, this.td);

		this.td.getBds().add(md);

		this.readMethodVisitor.init(md);
		return this.readMethodVisitor;
	}

	@Override
	public void visitOuterClass(final String owner, final String name, final String desc) {
		final T ownerT = this.du.getT(owner);
		if (name == null) {
			this.td.setEnclosingT(ownerT);
			return;
		}
		this.td.setEnclosingM(ownerT.getM(name, desc));
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