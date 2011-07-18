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
package org.decojer.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.Section;
import org.objectweb.asm.AnnotationVisitor;

import com.googlecode.dex2jar.Field;
import com.googlecode.dex2jar.Method;
import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFieldVisitor;
import com.googlecode.dex2jar.visitors.DexFileVisitor;
import com.googlecode.dex2jar.visitors.DexMethodVisitor;

/**
 * 
 * @author André Pankraz
 */
public class DexReaderTest {

	public static void decompileDex(final InputStream is, final OutputStream os)
			throws IOException {
		final DexFileReader dexFileReader = new DexFileReader(is);
		dexFileReader.accept(new DexFileVisitor() {

			DexClassVisitor dexClassVisitor = new DexClassVisitor() {

				@Override
				public AnnotationVisitor visitAnnotation(final String name,
						final boolean visitable) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void visitEnd() {
					// TODO Auto-generated method stub

				}

				@Override
				public DexFieldVisitor visitField(final Field field,
						final Object value) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public DexMethodVisitor visitMethod(final Method method) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void visitSource(final String file) {
					// TODO Auto-generated method stub

				}
			};

			@Override
			public DexClassVisitor visit(final int access_flags,
					final String className, final String superClass,
					final String... interfaceNames) {
				return this.dexClassVisitor;
			}

			@Override
			public void visitEnd() {
			}
		});

	}

	public static void main(final String[] args) throws FileNotFoundException,
			IOException {

		new DexFile(
				"E:/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex")
				.getSectionForType(ItemType.TYPE_CODE_ITEM);

		decompileDex(
				new FileInputStream(
						new File(
								"E:/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex")),
				null);

		final long millis = System.currentTimeMillis();
		if (false) {
			final Section<CodeItem> sectionForItem = new DexFile(
					"E:/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex")
					.getSectionForType(ItemType.TYPE_CODE_ITEM);
			System.out.println("TEST: "
					+ sectionForItem.getItems().get(0).getInstructions()[0]);
		} else {
			decompileDex(
					new FileInputStream(
							new File(
									"E:/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex")),
					null);
		}
		System.out.println("TEST: " + (System.currentTimeMillis() - millis));
	}
}
