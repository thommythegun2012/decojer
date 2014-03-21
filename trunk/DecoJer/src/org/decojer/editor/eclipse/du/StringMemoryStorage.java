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
package org.decojer.editor.eclipse.du;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.runtime.IPath;

/**
 * Memory Storage for Eclipse Editor Framework.
 *
 * @author André Pankraz
 */
public class StringMemoryStorage implements IEncodedStorage {

	private static final Charset UTF8 = Charset.forName("utf-8");

	private final byte[] contents;

	@Nonnull
	private final IPath fullPath;

	/**
	 * Constructor.
	 *
	 * @param content
	 *            Content
	 * @param fullPath
	 *            full path, important for faked compilation unit and outline
	 */
	public StringMemoryStorage(final String content, @Nonnull final IPath fullPath) {
		this.fullPath = fullPath;
		this.contents = content.getBytes(UTF8);
	}

	@Nullable
	@Override
	public Object getAdapter(final Class adapter) {
		return null;
	}

	@Override
	public String getCharset() {
		return UTF8.name();
	}

	@Override
	public InputStream getContents() {
		return new ByteArrayInputStream(this.contents);
	}

	@Override
	public IPath getFullPath() {
		return this.fullPath;
	}

	@Nullable
	@Override
	public String getName() {
		final String lastSegment = this.fullPath.lastSegment();
		return lastSegment == null ? this.fullPath.toString() : lastSegment;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

}