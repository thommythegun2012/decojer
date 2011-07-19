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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.DeprecatedAttribute;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.InnerClassesAttribute;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SourceFileAttribute;
import javassist.bytecode.SyntheticAttribute;

import org.decojer.cavaj.model.TD2;
import org.decojer.cavaj.model.type.Type;
import org.decojer.cavaj.model.type.Types;

/**
 * @author André Pankraz
 */
public class JavassistReader {

	private final static Logger LOGGER = Logger.getLogger(JavassistReader.class
			.getName());

	public static Type analyse(final InputStream is) throws IOException {
		final ClassFile classFile = new ClassFile(new DataInputStream(is));
		final SignatureAttribute signatureAttribute = (SignatureAttribute) classFile
				.getAttribute(SignatureAttribute.tag);
		String signature;
		if (signatureAttribute == null) {
			final StringBuilder sb = new StringBuilder("L");
			sb.append(classFile.getSuperclass()).append(";");
			for (final String interfaceName : classFile.getInterfaces()) {
				sb.append("L").append(interfaceName).append(";");
			}
			signature = sb.toString();
		} else {
			signature = signatureAttribute.getSignature();
		}
		final Type type = new Type(classFile.getName(), signature);
		return type;
	}

	public static Types analyseJar(final InputStream is) throws IOException {
		final ZipInputStream zip = new ZipInputStream(is);
		final Types types = new Types();
		int errors = 0;
		for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
				.getNextEntry()) {
			final String name = zipEntry.getName();
			if (!name.endsWith(".class")) {
				continue;
			}
			try {
				types.addType(analyse(zip));
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Couldn't analyse '" + name + "'!", e);
				++errors;
			}
		}
		types.setErrors(errors);
		return types;
	}

	public static void main(final String[] args) throws IOException {
		// final FileInputStream is = new FileInputStream(
		// "D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/org.eclipse.jdt.core_3.7.0.v_B61.jar");
		// final Types types = analyseJar(is);
		// System.out.println("Ana: " + types.getTypes().size());
		final FileInputStream is = new FileInputStream(
				"D:/Data/Decomp/workspace/DecoJerTest/uploaded_test/DecTestBooleanOperators.class");
		final TD2 td = read(is);
		System.out.println("Read: " + td);
	}

	public static TD2 read(final InputStream is) throws IOException {
		final ClassFile classFile = new ClassFile(new DataInputStream(is));

		// only annotations with RetentionPolicy.CLASS or RUNTIME are visible
		// here and can be decompiled, e.g. @SuppressWarnings not visible here,
		// has @Retention(RetentionPolicy.SOURCE)
		AnnotationsAttribute annotationsAttributeRuntimeInvisible = null;
		AnnotationsAttribute annotationsAttributeRuntimeVisible = null;
		DeprecatedAttribute deprecatedAttribute = null;
		// for local or inner classes the innermost enclosing class or method
		EnclosingMethodAttribute enclosingMethodAttribute = null;
		InnerClassesAttribute innerClassesAttribute = null;
		SignatureAttribute signatureAttribute = null;
		SourceFileAttribute sourceFileAttribute = null;
		SyntheticAttribute syntheticAttribute = null;
		for (final AttributeInfo attributeInfo : (List<AttributeInfo>) classFile
				.getAttributes()) {
			final String attributeTag = attributeInfo.getName();
			if (AnnotationsAttribute.invisibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeInvisible = (AnnotationsAttribute) attributeInfo;
			} else if (AnnotationsAttribute.visibleTag.equals(attributeTag)) {
				annotationsAttributeRuntimeVisible = (AnnotationsAttribute) attributeInfo;
			} else if (DeprecatedAttribute.tag.equals(attributeTag)) {
				deprecatedAttribute = (DeprecatedAttribute) attributeInfo;
			} else if (EnclosingMethodAttribute.tag.equals(attributeTag)) {
				enclosingMethodAttribute = (EnclosingMethodAttribute) attributeInfo;
			} else if (InnerClassesAttribute.tag.equals(attributeTag)) {
				innerClassesAttribute = (InnerClassesAttribute) attributeInfo;
			} else if (SignatureAttribute.tag.equals(attributeTag)) {
				signatureAttribute = (SignatureAttribute) attributeInfo;
			} else if (SourceFileAttribute.tag.equals(attributeTag)) {
				sourceFileAttribute = (SourceFileAttribute) attributeInfo;
			} else if (SyntheticAttribute.tag.equals(attributeTag)) {
				syntheticAttribute = (SyntheticAttribute) attributeInfo;
			} else {
				LOGGER.log(Level.WARNING, "Unknown class file attribute tag '"
						+ attributeTag + "'!");
			}
		}

		String signature;
		if (signatureAttribute == null) {
			final StringBuilder sb = new StringBuilder("L");
			sb.append(classFile.getSuperclass()).append(";");
			for (final String interfaceName : classFile.getInterfaces()) {
				sb.append("L").append(interfaceName).append(";");
			}
			signature = sb.toString();
		} else {
			signature = signatureAttribute.getSignature();
		}

		final TD2 td = new TD2(classFile.getName(), signature);

		return td;
	}

}