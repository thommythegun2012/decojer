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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * String Storage for Eclipse Editor Framework.
 * 
 * @see StringInput
 * @author André Pankraz
 */
public class StringStorage implements IStorage {

	private final String string;

	/**
	 * Constructor.
	 * 
	 * @param input
	 *            String input
	 */
	public StringStorage(final String input) {
		this.string = input;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class adapter) {
		return null;
	}

	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(this.string.getBytes());
	}

	public IPath getFullPath() {
		return null;
	}

	public String getName() {
		final int len = Math.min(5, this.string.length());
		return this.string.substring(0, len).concat("..."); //$NON-NLS-1$
	}

	public boolean isReadOnly() {
		return true;
	}

}