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
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.EncodedArrayItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.EncodedValue.BooleanEncodedValue;
import org.jf.dexlib.EncodedValue.ByteEncodedValue;
import org.jf.dexlib.EncodedValue.CharEncodedValue;
import org.jf.dexlib.EncodedValue.DoubleEncodedValue;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.FloatEncodedValue;
import org.jf.dexlib.EncodedValue.IntEncodedValue;
import org.jf.dexlib.EncodedValue.LongEncodedValue;
import org.jf.dexlib.EncodedValue.NullEncodedValue;
import org.jf.dexlib.EncodedValue.ShortEncodedValue;
import org.jf.dexlib.EncodedValue.StringEncodedValue;
import org.jf.dexlib.Util.ByteArrayInput;

/**
 * Reader from Smali.
 * 
 * @author André Pankraz
 */
public class SmaliReader {

	private final static Logger LOGGER = Logger.getLogger(SmaliReader.class
			.getName());

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

	private static Object decodeValue(final EncodedValue encodedValue) {
		if (encodedValue instanceof BooleanEncodedValue) {
			return ((BooleanEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof ByteEncodedValue) {
			return ((ByteEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof CharEncodedValue) {
			return ((CharEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof DoubleEncodedValue) {
			return ((DoubleEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof FloatEncodedValue) {
			return ((FloatEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof IntEncodedValue) {
			return ((IntEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof LongEncodedValue) {
			return ((LongEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof NullEncodedValue) {
			return null; // placeholder in constant array
		}
		if (encodedValue instanceof ShortEncodedValue) {
			return ((ShortEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof StringEncodedValue) {
			return ((StringEncodedValue) encodedValue).value;
		}
		LOGGER.warning("Unknown encoded value type '"
				+ encodedValue.getClass().getName() + "'!");
		return null;
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
				readFields(td, classDefItem.getStaticFieldInitializers(),
						classData.getStaticFields(),
						classData.getInstanceFields());

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

	private static void readFields(final TD td,
			final EncodedArrayItem staticFieldInitializers,
			final EncodedField[] staticFields,
			final EncodedField[] instanceFields) {
		final EncodedValue[] staticFieldValues = staticFieldInitializers == null ? new EncodedValue[0]
				: staticFieldInitializers.getEncodedArray().values;

		for (int i = 0; i < staticFields.length; ++i) {
			final EncodedField encodedField = staticFields[i];
			final FieldIdItem field = encodedField.field;

			// static field initializer values are packed away into a different
			// section, both arrays (encoded fields and static field values) are
			// sorted in same order, there could be less static field values if
			// not all static fields have an initializer, but there is also a
			// null value as placeholder
			Object value = null;
			if (staticFieldValues.length > i) {
				value = decodeValue(staticFieldValues[i]);
			}
			final FD fd = new FD(td, encodedField.accessFlags, field
					.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor(), null, value);
			td.getBds().add(fd);
		}

		for (int i = 0; i < instanceFields.length; ++i) {
			final EncodedField encodedField = instanceFields[i];
			final FieldIdItem field = encodedField.field;

			// there is no field initializer section for instance fields,
			// only via constructor
			final FD fd = new FD(td, encodedField.accessFlags, field
					.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor(), null, null);
			td.getBds().add(fd);
		}
	}

}