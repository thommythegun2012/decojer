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
package org.decojer.cavaj.model;

import java.io.DataInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.decojer.DecoJerException;
import org.decojer.PackageClassStreamProvider;

/**
 * Package fragment.
 * 
 * Names consist of dollar-separated type names.
 * 
 * @author André Pankraz
 */
public class PF {

	private final Map<String, TD> name2td = new HashMap<String, TD>();

	private final PackageClassStreamProvider packageClassStreamProvider;

	private String packageName;

	/**
	 * Constructor.
	 * 
	 * @param packageClassStreamProvider
	 *            package class stream provider
	 */
	public PF(final PackageClassStreamProvider packageClassStreamProvider) {
		assert packageClassStreamProvider != null;

		this.packageClassStreamProvider = packageClassStreamProvider;
		// try prefetching all package type declarations if package class file
		// provider supports listing all entries
		// TODO later use a direct folder iterator
		for (final Entry<String, DataInputStream> classFileEntry : this.packageClassStreamProvider
				.getClassStreams().entrySet()) {
			addTd(classFileEntry.getKey(), classFileEntry.getValue());
		}
	}

	private TD addTd(final String name, final DataInputStream classStream) {
		assert name != null;
		assert classStream != null;

		final TD td = new TD(this, classStream);
		if (!name.equals(td.getName())) {
			throw new DecoJerException("Type name from file '" + name
					+ "' not equal to type name in class '" + td.getName()
					+ "'!");
		}
		if (this.packageName == null) {
			this.packageName = td.getPackageName();
		} else if (!this.packageName.equals(td.getPackageName())) {
			throw new DecoJerException("Package name from package fragment '"
					+ this.packageName
					+ "' not equal to package name in class '"
					+ td.getPackageName() + "'!");
		}
		this.name2td.put(name, td);
		return td;
	}

	/**
	 * Get package name.
	 * 
	 * @return package name
	 */
	public String getPackageName() {
		return this.packageName;
	}

	/**
	 * Get type declaration.
	 * 
	 * @param name
	 *            name
	 * @return type declaration or null
	 */
	public TD getTd(final String name) {
		if (this.name2td.containsKey(name)) {
			return this.name2td.get(name);
		}
		// try postfetching a package type definition if package class file
		// provider didn't find entry in prefetch listing
		final DataInputStream classStream = this.packageClassStreamProvider
				.getClassStream(name);
		if (classStream == null) {
			// cache non-availability
			this.name2td.put(name, null);
			return null;
		}
		try {
			return addTd(name, classStream);
		} catch (final RuntimeException e) {
			// cache non-availability
			this.name2td.put(name, null);
			throw e;
		}
	}

	/**
	 * Get type name to type declarations mapping.
	 * 
	 * @return type name to type declarations mapping
	 */
	public Map<String, TD> getTds() {
		return this.name2td;
	}

}