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
package org.decojer.editor.eclipse.util;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

/**
 * String Input for Eclipse Editor Framework. This wraps a String Storage
 * element which delivers an in-memory String Input Stream.
 * 
 * This way it isn't necassary to create a local temporary file containing the
 * decompiled source code. I don't know yet if this really works, there are
 * problems with empty outline views from the Compilation Unit Editor and with
 * gotos.
 * 
 * @author André Pankraz
 */
public class StringInput implements IStorageEditorInput {

	private final IStorage storage;

	/**
	 * Constructor.
	 * 
	 * @param storage
	 *            String storage.
	 */
	public StringInput(final IStorage storage) {
		this.storage = storage;
	}

	public boolean exists() {
		return true;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class adapter) {
		return null;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return this.storage.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public IStorage getStorage() {
		return this.storage;
	}

	public String getToolTipText() {
		return "String-based file: " + this.storage.getName();
	}

}