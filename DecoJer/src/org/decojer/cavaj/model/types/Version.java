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
package org.decojer.cavaj.model.types;

import lombok.Getter;

/**
 * JVM Bytecode Version.
 * 
 * @author André Pankraz
 */
public enum Version {

	JVM_1(45), // don't differentiate: 1.0 is minor 0, 1.1 is minor 3

	JVM_2(46),

	JVM_3(47),

	JVM_4(48),

	JVM_5(49),

	JVM_6(50),

	JVM_7(51),

	JVM_8(52),

	JVM_9(53),

	JVM_10(54);

	/**
	 * Get version.
	 * 
	 * @param major
	 *            major version
	 * @return version
	 */
	public static Version versionFor(final int major) {
		return Version.values()[major - 45];
	}

	@Getter
	private final int major;

	/**
	 * Constructor.
	 * 
	 * @param major
	 *            major version
	 */
	private Version(final int major) {
		this.major = major;
	}

}