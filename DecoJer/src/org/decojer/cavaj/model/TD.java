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
package org.decojer.cavaj.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.util.Cursor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Name;

/**
 * Type Declaration.
 * 
 * Names consist of dot-separated package names (for full name) and dollar-separated type names.
 * 
 * @author André Pankraz
 */
public final class TD extends T implements BD, PD {

	private final static Logger LOGGER = Logger.getLogger(TD.class.getName());

	private static String toString(final T superT, final T[] interfaceTs) {
		final StringBuilder sb = new StringBuilder("{");
		if (superT != null) {
			sb.append(superT.getName()).append(',');
		}
		for (final T interfaceT : interfaceTs) {
			sb.append(interfaceT.getName()).append(",");
		}
		sb.setCharAt(sb.length() - 1, '}');
		return sb.toString();
	}

	/**
	 * AST Type Declaration.
	 */
	@Getter
	@Setter
	private ASTNode typeDeclaration;

	/**
	 * All Body Declarations: inner Type/Method/Field Declarations.
	 */
	@Getter
	private final List<BD> bds = new ArrayList<BD>();

	/**
	 * Access Flags.
	 */
	@Setter
	private int accessFlags;

	@Getter
	private final DU du;

	/**
	 * Type Parameters. (They define the useable Type Variables)
	 */
	@Getter
	private T[] typeParams;

	/**
	 * Super Type.
	 */
	@Setter
	private T superT;

	/**
	 * Member Types (really contained Inner Classes).
	 */
	@Getter
	@Setter
	private T[] memberTs;

	/**
	 * Annotations.
	 */
	@Getter
	@Setter
	private A[] as;

	/**
	 * Synthetic State (from Synthetic Attribute).
	 */
	@Setter
	@Getter
	private boolean synthetic;

	/**
	 * Enclosing Method for this Anonymous Inner Class.
	 */
	@Getter
	@Setter
	private M enclosingM;

	/**
	 * Class file version.
	 * 
	 * 1.0: 45.0, 1.1: 45.3, 1.2: 46, 1.3: 47, 1.4: 48, 5: 49, 6: 50, 7: 51
	 * 
	 * JDK 1.2 and 1.3 creates versions 1.1 if no target option given. JDK 1.4 creates 1.2 if no
	 * target option given.
	 */
	@Getter
	@Setter
	private int version;

	/**
	 * Enclosing Type for this Anynomous Inner Class (must be in Field Initializer).
	 * 
	 * TODO combine both enclosing, only one possible...need TD and M(D?!) supertype for this
	 */
	@Getter
	@Setter
	private T enclosingT;

	/**
	 * Deprecated State (from Deprecated Attribute).
	 */
	@Getter
	@Setter
	private boolean deprecated;

	/**
	 * Source File Name (from Source File Attribute).
	 */
	@Getter
	@Setter
	private String sourceFileName;

	/**
	 * Parent Declaration.
	 */
	@Getter
	@Setter
	private PD pd;

	@Setter
	private T[] interfaceTs; // anonymousClassDeclaration?

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Type Name
	 * @param du
	 *            Decompilation Unit
	 */
	protected TD(final String name, final DU du) {
		super(name);

		assert du != null;

		this.du = du;
	}

	protected TD(final T superT, final T[] interfaceTs) {
		super(toString(superT, interfaceTs));

		this.du = superT.getDu();
		this.superT = superT;
		this.interfaceTs = interfaceTs;
	}

	/**
	 * Add Eclipse body declarations.
	 * 
	 * @param bodyDeclaration
	 *            Eclipse body declaration
	 * 
	 * @return true - success
	 */
	@SuppressWarnings("unchecked")
	public boolean addBodyDeclaration(final BodyDeclaration bodyDeclaration) {
		assert bodyDeclaration != null;

		if (this.typeDeclaration instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration) this.typeDeclaration).bodyDeclarations().add(
					bodyDeclaration);
		}
		if (bodyDeclaration instanceof EnumConstantDeclaration) {
			if (this.typeDeclaration instanceof EnumDeclaration) {
				return ((EnumDeclaration) this.typeDeclaration).enumConstants()
						.add(bodyDeclaration);
			}
			return false;
		}
		return ((AbstractTypeDeclaration) this.typeDeclaration).bodyDeclarations().add(
				bodyDeclaration);
	}

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return true - is access flag
	 */
	public boolean check(final AF af) {
		return isResolveable() && (this.accessFlags & af.getValue()) != 0;
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		this.pd = null;
		this.typeDeclaration = null;
		for (int i = this.bds.size(); i-- > 0;) {
			final BD bd = this.bds.get(i);
			if (bd instanceof TD) {
				((TD) bd).clear();
				this.bds.remove(i);
				continue;
			}
			if (bd instanceof FD) {
				((FD) bd).setFieldDeclaration(null);
				continue;
			}
			if (bd instanceof MD) {
				((MD) bd).setMethodDeclaration(null);
				final CFG cfg = ((MD) bd).getCfg();
				if (cfg != null) {
					cfg.clear();
				}
			}
		}
	}

	/**
	 * Get Compilation Unit.
	 * 
	 * @return Compilation Unit
	 */
	public CU getCu() {
		final PD pd = getPd();
		if (pd instanceof CU) {
			return (CU) pd;
		}
		if (pd instanceof TD) {
			return ((TD) pd).getCu();
		}
		if (pd instanceof MD) {
			return ((MD) pd).getTd().getCu();
		}
		if (pd instanceof FD) {
			return ((FD) pd).getTd().getCu();
		}
		return null;
	}

	/**
	 * Get Field Declaration for name.
	 * 
	 * @param name
	 *            name
	 * @return field declaration
	 */
	public FD getFd(final String name) {
		for (final BD bd : this.bds) {
			if (bd instanceof FD && name.equals(((FD) bd).getF().getName())) {
				return (FD) bd;
			}
		}
		return null;
	}

	@Override
	public T[] getInterfaceTs() {
		return isResolveable() ? this.interfaceTs : T.NO_INTERFACES;
	}

	@Override
	public int getKind() {
		return Kind.REF.getKind();
	}

	@Override
	public T getSuperT() {
		return isResolveable() ? this.superT : null;
	}

	/**
	 * Is anonymous type declaration?
	 * 
	 * @return true - is anonymous type declaration
	 */
	public boolean isAnonymous() {
		return this.typeDeclaration instanceof AnonymousClassDeclaration;
	}

	/**
	 * Is Dalvik?
	 * 
	 * @return true is Dalvik
	 */
	public boolean isDalvik() {
		return this.version == 0;
	}

	@Override
	public boolean isInterface() {
		return check(AF.INTERFACE);
	}

	@Override
	public boolean isObject() {
		return Object.class.getName().equals(getName());
	}

	/**
	 * Is unresolveable?
	 * 
	 * @return true - is unresolveable
	 */
	@Override
	public boolean isResolveable() {
		if (this.interfaceTs != null) {
			// don't use check(AF.UNRESOLVEABLE) -> endless loop
			return (this.accessFlags & AF.UNRESOLVEABLE.getValue()) == 0;
		}
		if (!isRef()) {
			this.interfaceTs = NO_INTERFACES;
			return true;
		}
		// setSuper() in class read doesn't set interfaces if not known
		if (this.superT != null) {
			this.interfaceTs = NO_INTERFACES;
			return true;
		}
		// try simple class loading, may be we are lucky ;)
		// TODO later ask DecoJer-online and local type cache with context info
		try {
			final Class<?> clazz = getClass().getClassLoader().loadClass(getName());
			this.accessFlags = clazz.getModifiers();
			final Class<?> superclass = clazz.getSuperclass();
			if (superclass != null) {
				this.superT = getDu().getT(superclass.getName());
			}
			final Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces.length == 0) {
				this.interfaceTs = NO_INTERFACES;
			} else {
				final T[] interfaceTs = new T[interfaces.length];
				for (int i = interfaces.length; i-- > 0;) {
					interfaceTs[i] = getDu().getT(interfaces[i].getName());
				}
				this.interfaceTs = interfaceTs;
			}
			return true;
		} catch (final ClassNotFoundException e) {
			LOGGER.warning("Couldn't load type : " + this);
			this.interfaceTs = NO_INTERFACES;
			markAf(AF.UNRESOLVEABLE);
			return false;
		}
	}

	/**
	 * Mark access flag.
	 * 
	 * @param af
	 *            access flag
	 */
	public void markAf(final AF af) {
		this.accessFlags |= af.getValue();
	}

	/**
	 * New type name (currently only for signatures).
	 * 
	 * @param fullName
	 *            full type name
	 * @return Eclipse type name
	 */
	public Name newTypeName(final String fullName) {
		assert fullName != null;

		return getCu().getTypeNameManager().newTypeName(fullName);
	}

	/**
	 * New type name.
	 * 
	 * @param t
	 *            type
	 * @return Eclipse type name
	 */
	public Name newTypeName(final T t) {
		assert t != null;

		return getCu().getTypeNameManager().newTypeName(t.getName());
	}

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		final Cursor c = new Cursor();
		this.typeParams = getDu().parseTypeParams(signature, c);

		// TODO more checks for following overrides:
		final T superT = getDu().parseT(signature, c);
		if (superT != null) {
			this.superT = superT;
		}
		final ArrayList<T> interfaceTs = new ArrayList<T>();
		while (true) {
			final T interfaceT = getDu().parseT(signature, c);
			if (interfaceT == null) {
				break;
			}
			interfaceTs.add(interfaceT);
		}
		if (!interfaceTs.isEmpty()) {
			if (this.interfaceTs.length != interfaceTs.size()) {
				LOGGER.info("Not matching Signature '" + signature + "' for Type Declaration: "
						+ this);
			} else {
				this.interfaceTs = interfaceTs.toArray(new T[interfaceTs.size()]);
			}
		}
	}

}