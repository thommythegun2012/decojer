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
package org.decojer.cavaj.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.Util.ByteArrayInput;

/**
 * @author André Pankraz
 */
public class SmaliReader {

	/**
	 * Analyse DEX input stream.
	 * 
	 * @param is
	 *            DEX input stream
	 * @return types
	 * @throws IOException
	 *             read exception
	 */
	public static Types analyse(final InputStream is) throws IOException {
		final byte[] bytes = IOUtils.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), false,
				true); // fast
		final Types types = new Types();
		@SuppressWarnings("unchecked")
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			System.out.println("TEST " + classDefItem + " : "
					+ classDefItem.getClassType().getTypeDescriptor());
			types.addType(new Type(classDefItem.getClassType()
					.getTypeDescriptor(), null));
		}
		return types;
	}

	/**
	 * Test it...
	 * 
	 * @param args
	 *            args
	 * @throws IOException
	 *             read exception
	 */
	public static void main(final String[] args) throws IOException {
		final FileInputStream is = new FileInputStream(
				"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/ASTRO_File_Manager_2.5.2/classes.dex");
		// final Types types = analyse(is);
		// System.out.println("Ana: " + types.getTypes().size());
		read(is, new DU());
	}

	/**
	 * Read DEX input stream.
	 * 
	 * @param is
	 *            DEX input stream
	 * @param du
	 *            decompilation unit
	 * @throws IOException
	 *             read exception
	 */
	@SuppressWarnings("unchecked")
	public static void read(final InputStream is, final DU du)
			throws IOException {
		final byte[] bytes = IOUtils.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), true,
				false);
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			final T t = du.getDescT(classDefItem.getClassType()
					.getTypeDescriptor());
			t.setSuperT(du.getDescT(classDefItem.getSuperclass()
					.getTypeDescriptor()));
			final TypeListItem interfaces = classDefItem.getInterfaces();
			if (interfaces != null && interfaces.getTypeCount() > 0) {
				final T[] interfaceTs = new T[interfaces.getTypeCount()];
				for (int i = interfaces.getTypeCount(); i-- > 0;) {
					interfaceTs[i] = du.getDescT(interfaces.getTypeIdItem(i)
							.getTypeDescriptor());
				}
				t.setInterfaceTs(interfaceTs);
			}

			final TD td = new TD(t);
			td.setAccessFlags(classDefItem.getAccessFlags());

			if (classDefItem.getSourceFile() != null) {
				td.setSourceFileName(classDefItem.getSourceFile()
						.getStringValue());
			}

			final ClassDataItem classData = classDefItem.getClassData();
			if (classData != null) {
				final EncodedMethod[] directMethods = classData
						.getDirectMethods();
				for (int i = 0; i < directMethods.length; ++i) {
					final EncodedMethod encodedMethod = directMethods[i];
					final MethodIdItem method = encodedMethod.method;

					// getResourceAsStream :
					// (Ljava/lang/String;)Ljava/io/InputStream;
					final MD md = new MD(td, encodedMethod.accessFlags, method
							.getMethodName().getStringValue(), method
							.getPrototype().getPrototypeString(), null, null);
					td.getBds().add(md);
				}

				final EncodedMethod[] virtualMethods = classData
						.getVirtualMethods();
				for (int i = 0; i < virtualMethods.length; ++i) {
					final EncodedMethod encodedMethod = virtualMethods[i];
					final MethodIdItem method = encodedMethod.method;

					// getResourceAsStream :
					// (Ljava/lang/String;)Ljava/io/InputStream;
					final MD md = new MD(td, encodedMethod.accessFlags, method
							.getMethodName().getStringValue(), method
							.getPrototype().getPrototypeString(), null, null);
					td.getBds().add(md);
				}
			}

			du.addTd(td);
		}
	}

}