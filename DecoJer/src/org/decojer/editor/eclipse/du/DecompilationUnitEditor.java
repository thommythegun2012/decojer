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

import org.decojer.cavaj.model.CU;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.ui.IEditorInput;

/**
 * Decompilation Unit Editor.
 * 
 * @author André Pankraz
 */
@SuppressWarnings("restriction")
public class DecompilationUnitEditor extends CompilationUnitEditor {

	public static IEditorInput decompileToEditorInput(final CU cu) {
		// create editor input, in-memory string with decompiled source
		String sourceCode = null;
		try {
			sourceCode = cu.decompile();
		} catch (final Throwable t) {
			t.printStackTrace();
			sourceCode = "// Decompilation error!";
		}
		return new MemoryStorageEditorInput(new StringMemoryStorage(sourceCode, cu == null
				|| cu.getSourceFileName() == null ? null : new Path(cu.getSourceFileName())));
	}

	public void setInput(final CU cu) {
		setInputWithNotify(decompileToEditorInput(cu));
	}

}