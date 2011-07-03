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
package org.decojer.editor.eclipse;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;

import org.decojer.PackageClassStreamProvider;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Eclipse package class stream provider.
 * 
 * @author André Pankraz
 */
public class EclipsePackageClassStreamProvider extends
		PackageClassStreamProvider {

	private static String extractPath(final IClassFile eclipseClassFile) {
		assert eclipseClassFile != null;

		if (eclipseClassFile.getResource() == null) {
			// is from JAR...
			// example: sun/org/mozilla/javascript/internal/
			final String jarPath = eclipseClassFile.getPath().toOSString();
			final String packageName = eclipseClassFile.getParent()
					.getElementName();
			final String typeName = eclipseClassFile.getElementName();
			return jarPath + "!/" + packageName.replace('.', '/') + '/'
					+ typeName;
		}
		return eclipseClassFile.getResource().getLocation().toOSString();
	}

	/**
	 * Constructor.
	 * 
	 * @param eclipseClassFile
	 *            Eclipse class file
	 */
	public EclipsePackageClassStreamProvider(final IClassFile eclipseClassFile) {
		super(extractPath(eclipseClassFile));
		String name = eclipseClassFile.getElementName();
		if (!name.endsWith(".class")) {
			LOGGER.log(Level.WARNING, "IClassFile name '" + name
					+ "' doesn't end with .class!");
			return;
		}
		name = name.substring(0, name.length() - 6);
		byte[] bytes;
		try {
			bytes = eclipseClassFile.getBytes();
		} catch (final JavaModelException e) {
			LOGGER.log(Level.WARNING,
					"Couldn't get bytes for Eclipse IClassFile!", e);
			return;
		}
		addClassStream(name, new ByteArrayInputStream(bytes));
	}

}