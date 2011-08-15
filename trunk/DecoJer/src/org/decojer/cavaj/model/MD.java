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

import java.util.logging.Logger;

import org.decojer.cavaj.model.vm.intermediate.Exc;
import org.decojer.cavaj.model.vm.intermediate.Var;
import org.eclipse.jdt.core.dom.BodyDeclaration;

/**
 * Method declaration.
 * 
 * @author André Pankraz
 */
public class MD implements BD, PD {

	private final static Logger LOGGER = Logger.getLogger(MD.class.getName());

	private Object annotationDefaultValue;

	private A[] as;

	private CFG cfg;

	// deprecated state (from deprecated attribute)
	private boolean deprecated;

	private Exc[] excs;

	private final M m;

	private BodyDeclaration methodDeclaration;

	private A[][] paramAss;

	// synthetic state (from synthetic attribute)
	private boolean synthetic;

	private final TD td;

	private Var[][] varss;

	/**
	 * Constructor.
	 * 
	 * @param m
	 *            method
	 * @param td
	 *            type declaration
	 */
	public MD(final M m, final TD td) {
		assert m != null;
		assert td != null;

		this.m = m;
		this.td = td;
	}

	/**
	 * Add exception handler.
	 * 
	 * @param t
	 *            catch type
	 * @param startPc
	 *            start pc
	 * @param endPc
	 *            end pc
	 * @param handlerPc
	 *            handler pc
	 */
	public void addExc(final T t, final int startPc, final int endPc,
			final int handlerPc) {
		if (this.excs == null) {
			this.excs = new Exc[1];
		} else {
			final Exc[] newExcs = new Exc[this.excs.length + 1];
			System.arraycopy(this.excs, 0, newExcs, 0, this.excs.length);
			this.excs = newExcs;
		}
		this.excs[this.excs.length - 1] = new Exc(t, startPc, endPc, handlerPc);
	}

	/**
	 * Add local variable.
	 * 
	 * Only basic checks, compare later with method parameters.
	 * 
	 * @param reg
	 *            register
	 * @param desc
	 *            descriptor
	 * @param signature
	 *            signature
	 * @param name
	 *            variable name
	 * @param startPc
	 *            start pc
	 * @param endPc
	 *            endpc
	 */
	public void addVar(final int reg, final String desc,
			final String signature, final String name, final int startPc,
			final int endPc) {
		assert name != null;
		assert desc != null;

		final T varT = getTd().getT().getDu().getDescT(desc);
		if (signature != null) {
			varT.setSignature(signature);
		}
		final Var var = new Var(varT);
		var.setName(name);
		var.setStartPc(startPc);
		var.setEndPc(endPc == 0 ? Integer.MAX_VALUE : endPc);

		Var[] vars = null;
		if (this.varss == null) {
			this.varss = new Var[reg + 1][];
		} else if (reg >= this.varss.length) {
			final Var[][] newVarss = new Var[reg + 1][];
			System.arraycopy(this.varss, 0, newVarss, 0, this.varss.length);
			this.varss = newVarss;
		} else {
			vars = this.varss[reg];
		}
		if (vars == null) {
			vars = new Var[1];
		} else {
			final Var[] newVars = new Var[vars.length + 1];
			System.arraycopy(vars, 0, newVars, 0, vars.length);
			vars = newVars;
		}
		vars[vars.length - 1] = var;
		this.varss[reg] = vars;
	}

	/**
	 * Get annotation default value.
	 * 
	 * @return annotation default value
	 */
	public Object getAnnotationDefaultValue() {
		return this.annotationDefaultValue;
	}

	/**
	 * Get annotations.
	 * 
	 * @return annotations
	 */
	public A[] getAs() {
		return this.as;
	}

	/**
	 * Get control flow graph.
	 * 
	 * @return control flow graph or null
	 */
	public CFG getCfg() {
		return this.cfg;
	}

	/**
	 * Get exception handlers.
	 * 
	 * @return exception handlers
	 */
	public Exc[] getExcs() {
		return this.excs;
	}

	/**
	 * Get method.
	 * 
	 * @return method
	 */
	public M getM() {
		return this.m;
	}

	/**
	 * Get Eclipse method declaration.
	 * 
	 * @return Eclipse method declaration
	 */
	public BodyDeclaration getMethodDeclaration() {
		return this.methodDeclaration;
	}

	/**
	 * Get parameter annotations.
	 * 
	 * @return parameter annotations
	 */
	public A[][] getParamAss() {
		return this.paramAss;
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
	 * Get local variable.
	 * 
	 * @param reg
	 *            register
	 * @param pc
	 *            pc
	 * 
	 * @return local variable
	 */
	public Var getVar(final int reg, final int pc) {
		if (this.varss == null || reg >= this.varss.length) {
			return null;
		}
		final Var[] vars = this.varss[reg];
		if (vars == null) {
			return null;
		}
		for (int i = vars.length; i-- > 0;) {
			final Var var = vars[i];
			if (var.getStartPc() <= pc && pc < var.getEndPc()) {
				return var;
			}
		}
		return null;
	}

	/**
	 * Get deprecated state (from deprecated attribute).
	 * 
	 * @return true - deprecated
	 */
	public boolean isDeprecated() {
		return this.deprecated;
	}

	/**
	 * Get synthetic state (from synthetic attribute).
	 * 
	 * @return true - synthetic
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * Post process local variables, e.g. extract method parameter. Better in
	 * read step than in transformator for generic base model.
	 */
	public void postProcessVars() {
		if (this.cfg == null) {
			return;
		}
		final int maxRegs = this.cfg.getMaxRegs();
		final T[] paramTs = this.m.getParamTs();

		if (this.varss == null) {
			this.varss = new Var[maxRegs][];
		} else if (maxRegs < this.varss.length) {
			LOGGER.warning("Max registers less than biggest register with local variable info!");
		} else if (maxRegs > this.varss.length) {
			final Var[][] newVarss = new Var[maxRegs][];
			System.arraycopy(this.varss, 0, newVarss, 0, this.varss.length);
			this.varss = newVarss;
		}
		if (this.td.isDalvik()) {
			// Dalvik...function parameters right aligned
			int reg = maxRegs;
			for (int i = paramTs.length; i-- > 0;) {
				final T paramT = paramTs[i];
				// parameter name was encoded in extra debug info, copy names
				// and parameter types to local vars
				Var[] vars = this.varss[--reg];
				if (vars != null) {
					LOGGER.warning("Found local variable info for method parameter '"
							+ reg + "'!");
				}
				// check
				vars = new Var[1];
				final Var var = new Var(paramT);
				var.setName(this.m.getParamName(i));
				vars[0] = var;
				this.varss[reg] = vars;
			}
			if (!this.m.checkAf(AF.STATIC)) {
				Var[] vars = this.varss[--reg];
				if (vars != null) {
					LOGGER.warning("Found local variable info for method parameter 'this'!");
				}
				// check
				vars = new Var[1];
				final Var var = new Var(this.td.getT());
				var.setName("this");
				vars[0] = var;
				this.varss[reg] = vars;
			}
			return;
		}
		// JVM...function parameters left aligned
		int reg = 0;
		if (!this.m.checkAf(AF.STATIC)) {
			Var[] vars = this.varss[reg];
			if (vars != null) {
				if (vars.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter 'this'!");
				}
				++reg;
			} else {
				vars = new Var[1];
				final Var var = new Var(this.td.getT());
				var.setName("this");
				vars[0] = var;
				this.varss[reg++] = vars;
			}
		}
		for (int i = 0; i < paramTs.length; ++i) {
			final T paramT = paramTs[i];
			Var[] vars = this.varss[reg];
			if (vars != null) {
				if (vars.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter '"
							+ reg + "'!");
				}
				this.m.setParamName(i, vars[0].getName());
				++reg;
			} else {
				vars = new Var[1];
				final Var var = new Var(paramT);
				var.setName(this.m.getParamName(i));
				vars[0] = var;
				this.varss[reg++] = vars;
			}
			if (paramT == T.LONG || paramT == T.DOUBLE) {
				reg++;
			}
		}
	}

	/**
	 * Set annotation default value.
	 * 
	 * @param annotationDefaultValue
	 *            annotation default value
	 */
	public void setAnnotationDefaultValue(final Object annotationDefaultValue) {
		this.annotationDefaultValue = annotationDefaultValue;
	}

	/**
	 * Set annotations.
	 * 
	 * @param as
	 *            annotations
	 */
	public void setAs(final A[] as) {
		this.as = as;
	}

	/**
	 * Set control flow graph.
	 * 
	 * @param cfg
	 *            control flow graph
	 */
	public void setCFG(final CFG cfg) {
		this.cfg = cfg;
	}

	/**
	 * Set deprecated state (from deprecated attribute).
	 * 
	 * @param deprecated
	 *            true - deprecated
	 */
	public void setDeprecated(final boolean deprecated) {
		this.deprecated = deprecated;
	}

	/**
	 * Set Eclipse method declaration.
	 * 
	 * @param methodDeclaration
	 *            Eclipse method declaration
	 */
	public void setMethodDeclaration(final BodyDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}

	/**
	 * Set parameter annotations.
	 * 
	 * @param paramAss
	 *            parameter annotations
	 */
	public void setParamAss(final A[][] paramAss) {
		this.paramAss = paramAss;
	}

	/**
	 * Set synthetic state (from synthetic attribute).
	 * 
	 * @param synthetic
	 *            true - synthetic
	 */
	public void setSynthetic(final boolean synthetic) {
		this.synthetic = synthetic;
	}

	@Override
	public String toString() {
		return getM().toString();
	}

}