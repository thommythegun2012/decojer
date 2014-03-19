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
package org.decojer.cavaj.model.methods;

import java.util.List;

import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.Container;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.code.CFG;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.utils.Cursor;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Lists;

/**
 * Class method.
 *
 * @author André Pankraz
 */
@Slf4j
public class ClassM extends M {

	@Setter
	private int accessFlags;

	@Getter
	private final String descriptor;

	@Getter(AccessLevel.PRIVATE)
	private MD md;

	@Getter
	private final String name;

	@Getter
	@Setter
	private T[] paramTs;

	@Getter
	@Setter
	private T returnT;

	@Getter
	private T t;

	/**
	 * Constructor for dynamic method.
	 *
	 * @param du
	 *            DU
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	public ClassM(final DU du, final String name, final String descriptor) {
		assert name != null;
		assert descriptor != null;

		this.t = null; // dynamic method
		this.name = name;
		this.descriptor = descriptor;

		final Cursor c = new Cursor();
		this.paramTs = du.parseMethodParamTs(descriptor, c, this);
		this.returnT = du.parseT(descriptor, c, this);

		setStatic(true); // dynamic callsite resolution, never reference on stack
	}

	/**
	 * Constructor.
	 *
	 * @param t
	 *            type
	 * @param name
	 *            name
	 * @param descriptor
	 *            descriptor
	 */
	public ClassM(final T t, final String name, final String descriptor) {
		assert t != null;
		assert name != null;
		assert descriptor != null;

		this.t = t;
		this.name = name;
		this.descriptor = descriptor;

		final Cursor c = new Cursor();
		this.paramTs = t.getDu().parseMethodParamTs(descriptor, c, this);
		this.returnT = t.getDu().parseT(descriptor, c, this);
	}

	@Override
	public boolean check(final AF af) {
		return (this.accessFlags & af.getValue()) != 0;
	}

	@Override
	public void clear() {
		getMd().clear();
	}

	@Override
	public boolean createMd() {
		if (isDeclaration()) {
			return false;
		}
		this.md = new MD();
		setDeclarationOwner(getT());
		return true;
	}

	@Override
	public Object getAnnotationDefaultValue() {
		return getMd().getAnnotationDefaultValue();
	}

	@Override
	public A[] getAs() {
		return getMd().getAs();
	}

	@Override
	public Object getAstNode() {
		return getMd().getAstNode();
	}

	@Override
	public CFG getCfg() {
		return getMd().getCfg();
	}

	@Override
	public CU getCu() {
		return getMd().getCu();
	}

	@Override
	public Element getDeclarationForNode(final ASTNode node) {
		return getMd().getDeclarationForNode(node);
	}

	@Override
	public Container getDeclarationOwner() {
		return getMd().getDeclarationOwner();
	}

	@Override
	public List<Element> getDeclarations() {
		return getMd().getDeclarations();
	}

	@Override
	public A[][] getParamAss() {
		return getMd().getParamAss();
	}

	@Override
	public String getParamName(final int i) {
		final String[] paramNames = getMd().getParamNames();
		if (paramNames == null || i >= paramNames.length || paramNames[i] == null) {
			return "arg" + i;
		}
		return paramNames[i];
	}

	/**
	 * Get receiver-type (this) for none-static methods.
	 *
	 * @return receiver-type
	 *
	 * @see ClassM#setReceiverT(T)
	 */
	@Override
	public T getReceiverT() {
		return getT() instanceof ClassT ? null : getT();
	}

	@Override
	public String getSignature() {
		return getMd().getSignature();
	}

	@Override
	public T[] getThrowsTs() {
		return getMd().getThrowsTs();
	}

	@Override
	public T[] getTypeParams() {
		return getMd().getTypeParams();
	}

	@Override
	public boolean isConstructor() {
		return CONSTRUCTOR_NAME.equals(getName());
	}

	@Override
	public boolean isDeclaration() {
		return getMd() != null;
	}

	/**
	 * Is deprecated method, marked via Javadoc @deprecated?
	 *
	 * @return {@code true} - is deprecated method
	 */
	public boolean isDeprecated() {
		return check(AF.DEPRECATED);
	}

	@Override
	public boolean isDynamic() {
		return getT() == null;
	}

	@Override
	public boolean isInitializer() {
		return INITIALIZER_NAME.equals(getName());
	}

	@Override
	public boolean isStatic() {
		return check(AF.STATIC);
	}

	@Override
	public boolean isSynthetic() {
		return check(AF.SYNTHETIC);
	}

	@Override
	public boolean isVarargs() {
		return check(AF.VARARGS);
	}

	/**
	 * Parse throw types from signature.
	 *
	 * @param s
	 *            signature
	 * @param c
	 *            cursor
	 * @return throw types
	 */
	@Nullable
	private T[] parseThrowsTs(final String s, final Cursor c) {
		if (c.pos >= s.length() || s.charAt(c.pos) != '^') {
			return null;
		}
		final List<T> ts = Lists.newArrayList();
		do {
			++c.pos;
			final T throwT = getT().getDu().parseT(s, c, this);
			throwT.setInterface(false); // TODO we know even more, must be from Throwable
			ts.add(throwT);
		} while (c.pos < s.length() && s.charAt(c.pos) == '^');
		return ts.toArray(new T[ts.size()]);
	}

	@Override
	public void setAnnotationDefaultValue(final Object annotationDefaultValue) {
		getMd().setAnnotationDefaultValue(annotationDefaultValue);
	}

	@Override
	public void setAs(final A[] as) {
		getMd().setAs(as);
	}

	@Override
	public void setAstNode(final Object astNode) {
		getMd().setAstNode(astNode);
	}

	@Override
	public void setCfg(final CFG cfg) {
		getMd().setCfg(cfg);
	}

	@Override
	public void setDeclarationOwner(final Container declarationOwner) {
		final Container previousDeclarationOwner = getMd().getDeclarationOwner();
		if (previousDeclarationOwner != null) {
			previousDeclarationOwner.getDeclarations().remove(this);
		}
		declarationOwner.getDeclarations().add(this);
		getMd().setDeclarationOwner(declarationOwner);
	}

	@Override
	public void setDeprecated() {
		this.accessFlags |= AF.DEPRECATED.getValue();
	}

	@Override
	public void setParamAss(final A[][] paramAss) {
		getMd().setParamAss(paramAss);
	}

	@Override
	public void setParamName(final int i, final String name) {
		String[] paramNames = getMd().getParamNames();
		if (paramNames == null) {
			paramNames = new String[getParamTs().length];
			getMd().setParamNames(paramNames);
		}
		paramNames[i] = name;
	}

	@Override
	public boolean setReceiverT(final T receiverT) {
		if (isStatic() || isDynamic()) {
			return false;
		}
		if (!getT().equals(receiverT)) {
			return false;
		}
		this.t = receiverT;
		return true;
	}

	@Override
	public void setSignature(final String signature) {
		if (signature == null) {
			return;
		}
		// remember signature for Eclipse method finding...
		getMd().setSignature(signature);

		final Cursor c = new Cursor();
		// typeParams better in M, maybe later if necessary for static invokes
		getMd().setTypeParams(getT().getDu().parseTypeParams(signature, c, this));

		final T[] paramTs = getParamTs();
		final T[] signParamTs = getT().getDu().parseMethodParamTs(signature, c, this);
		if (signParamTs.length != 0) {
			if (paramTs.length != signParamTs.length) {
				// can happen with Sun JVM for constructor:
				// see org.decojer.cavaj.test.jdk2.DecTestInnerS.Inner1.Inner11.1.InnerMethod
				// or org.decojer.cavaj.test.jdk5.DecTestEnumStatus
				// Signature since JVM 5 exists but doesn't contain synthetic parameters,
				// e.g. outer context for methods in inner classes: (I)V instead of (Lthis;_I_II)V
				// or enum constructor parameters arg0: String, arg1: int
				if (!isConstructor()) {
					log.info("Cannot reduce signature '" + signature
							+ "' to types for method params: " + this);
				}
			} else {
				for (int i = 0; i < paramTs.length; ++i) {
					final T paramT = signParamTs[i];
					if (!paramT.eraseTo(paramTs[i])) {
						log.info("Cannot reduce signature '" + signature + "' to type '"
								+ paramTs[i] + "' for method param: " + this);
						break;
					}
					paramTs[i] = paramT;
				}
			}
		}
		final T returnT = getT().getDu().parseT(signature, c, this);
		if (!returnT.eraseTo(getReturnT())) {
			log.info("Cannot reduce signature '" + signature + "' to type '" + getReturnT()
					+ "' for method return: " + this);
		} else {
			setReturnT(returnT);
		}
		final T[] signThrowTs = parseThrowsTs(signature, c);
		if (signThrowTs != null) {
			final T[] throwsTs = getThrowsTs();
			if (throwsTs.length != signThrowTs.length) {
				log.info("Cannot reduce signature '" + signature + "' to types for method throws: "
						+ this);
			}
			for (int i = 0; i < throwsTs.length; ++i) {
				final T throwT = signThrowTs[i];
				if (!throwT.eraseTo(throwsTs[i])) {
					log.info("Cannot reduce signature '" + signature + "' to type '" + throwsTs[i]
							+ "' for method throw: " + this);
					break;
				}
				throwsTs[i] = throwT;
			}
		}
	}

	@Override
	public void setStatic(final boolean f) {
		if (f) {
			// static also possible in interface since JVM 8
			if ((this.accessFlags & AF.STATIC.getValue()) != 0) {
				return;
			}
			assert (this.accessFlags & AF.STATIC_ASSERTED.getValue()) == 0;

			this.accessFlags |= AF.STATIC.getValue() | AF.STATIC_ASSERTED.getValue();
			return;
		}
		assert (this.accessFlags & AF.STATIC.getValue()) == 0;

		this.accessFlags |= AF.STATIC_ASSERTED.getValue();
		return;
	}

	@Override
	public void setSynthetic() {
		this.accessFlags |= AF.SYNTHETIC.getValue();
	}

	@Override
	public void setThrowsTs(final T[] throwsTs) {
		getMd().setThrowsTs(throwsTs);
	}

	@Override
	public String toString() {
		return getT() + "." + getName() + getDescriptor();
	}

}