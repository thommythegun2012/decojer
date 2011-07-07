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
package org.decojer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.decojer.cavaj.tool.InputStreamReader;

/**
 * 
 * @author André Pankraz
 */
public class ZipTests {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		try {
			new InputStreamReader()
					.visitStream(new FileInputStream(
							new File(
									"E:/Decomp/workspace/DecoJerTest/uploaded_test/xrepositoryApp.ear")));
		} catch (final NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}