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
package org.decojer.cavaj.utils;

/**
 * File magic numbers.
 * 
 * @author André Pankraz
 */
public interface MagicNumbers {

	int LENGTH = 4;

	byte[] CLASS = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

	// 64 65 79 0A 30 33 35 00 for odex file or
	// 64 65 78 0A 30 33 35 00 for dex file ("dex\n035\0")
	byte[] DEX = { (byte) 0x64, (byte) 0x65, (byte) 0x78, (byte) 0x0a };

	byte[] ODEX = { (byte) 0x64, (byte) 0x65, (byte) 0x79, (byte) 0x0a };

	byte[] ZIP = { (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04 };

}