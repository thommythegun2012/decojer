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
package org.decojer.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.io.Files;
import com.nothome.delta.Delta;
import com.nothome.delta.GDiffPatcher;

/**
 * @author andre
 */
public class DeltaTest {

	public static void main(final String[] args) throws IOException {
		final byte source[] = Files
				.toByteArray(new File(
						"C:/Users/andre/.m2/repository/org/infinispan/infinispan-core/5.0.0.FINAL/infinispan-core-5.0.0.FINAL.jar"));
		final byte target[] = Files
				.toByteArray(new File(
						"C:/Users/andre/.m2/repository/org/infinispan/infinispan-core/5.0.1.FINAL/infinispan-core-5.0.1.FINAL.jar"));
		final Delta d = new Delta();
		final byte patch[] = d.compute(source, target);

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ZipOutputStream zos = new ZipOutputStream(bos);
		zos.putNextEntry(new ZipEntry("t"));
		IO.copy(new ByteArrayInputStream(patch), zos);
		zos.finish();

		System.out.println("Patch: " + target.length + " -> " + patch.length + " (" + patch.length
				* 100 / target.length + "%)");
		System.out.println("PatchZ: " + bos.toByteArray().length);

		final GDiffPatcher p = new GDiffPatcher();
		final byte patchedSource[] = p.patch(source, patch);

		System.out.println("EQUALS: " + java.util.Arrays.equals(target, patchedSource));
	}

}