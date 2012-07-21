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
package org.decojer.editor.eclipse.utils;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

/**
 * Memory Storage Editor Input for Eclipse Editor Framework.
 * 
 * This wraps for instance a String Storage element which delivers an in-memory String Input Stream.
 * 
 * This way it isn't necassary to create a local temporary file containing the decompiled source
 * code. I don't know yet if this really works, there are problems with empty outline views from the
 * Compilation Unit Editor and with gotos.
 * 
 * @author André Pankraz
 */
public class MemoryStorageEditorInput implements IStorageEditorInput {

	private final IStorage storage;

	/**
	 * Constructor.
	 * 
	 * @param storage
	 *            String storage.
	 */
	public MemoryStorageEditorInput(final IStorage storage) {
		this.storage = storage;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public Object getAdapter(final Class adapter) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return this.storage.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public IStorage getStorage() {
		return this.storage;
	}

	@Override
	public String getToolTipText() {
		return "StorageEditorInput: " + this.storage.getName();
	}

}