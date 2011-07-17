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
package org.decojer.web.analyser;

public class TypeInfo {

	private String[] interfaces;

	// hash from as many infos as possible
	private String md5Hash;

	// without L...;
	private String name;

	// signature with L...;
	private String signature;

	private int size;

	// without L...;
	private String superName;

	public String[] getInterfaces() {
		return this.interfaces;
	}

	public String getMd5Hash() {
		return this.md5Hash;
	}

	public String getName() {
		return this.name;
	}

	public String getSignature() {
		if (this.signature == null) {
			final StringBuilder sb = new StringBuilder("L");
			sb.append(getSuperName());
			sb.append(";");
			final String[] interfaces = getInterfaces();
			if (interfaces != null) {
				for (int i = 0; i < interfaces.length; ++i) {
					sb.append("L").append(interfaces[i]).append(";");
				}
			}
			this.signature = sb.toString();
		}
		return this.signature;
	}

	public int getSize() {
		return this.size;
	}

	public String getSuperName() {
		return this.superName;
	}

	private String normalize(final String string) {
		if (string.length() > 2 && string.charAt(0) == 'L'
				&& string.charAt(string.length() - 1) == ';') {
			return string.substring(1, string.length() - 1);
		}
		return string;
	}

	public void setInterfaces(final String[] interfaces) {
		if (interfaces == null || interfaces.length == 0) {
			return;
		}
		for (int i = interfaces.length; i-- > 0;) {
			interfaces[i] = normalize(interfaces[i]);
		}
		this.interfaces = interfaces;
	}

	public void setMd5Hash(final String md5Hash) {
		this.md5Hash = md5Hash;
	}

	public void setName(final String name) {
		this.name = normalize(name);
	}

	public void setSignature(final String signature) {
		this.signature = signature;
	}

	public void setSize(final int size) {
		this.size = size;
	}

	public void setSuperName(final String superName) {
		this.superName = normalize(superName);
	}
}