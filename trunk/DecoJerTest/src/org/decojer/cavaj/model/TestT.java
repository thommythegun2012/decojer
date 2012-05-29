package org.decojer.cavaj.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;

import org.decojer.DecoJer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
class TestT {

	DU du;

	T objectT;

	@BeforeClass
	void beforeClass() {
		// a decompilation unit is like a class loader, just for references with
		// real classes / type declarations
		du = DecoJer.createDu();
		objectT = du.getT(Object.class);
	}

	@Test
	void testInterfaces() {
		assertEquals(int.class.getInterfaces().length, 0);
		assertEquals(T.INT.getInterfaceTs().length, 0);

		assertEquals(Object.class.getInterfaces().length, 0);
		assertEquals(objectT.getInterfaceTs().length, 0);

		// Interface order is relevant
		Class<?>[] clazzes = String.class.getInterfaces();
		assertEquals(clazzes.length, 3);
		assertSame(clazzes[0], Serializable.class);
		assertSame(clazzes[1], Comparable.class);
		assertSame(clazzes[2], CharSequence.class);
		T[] ts = du.getT(String.class).getInterfaceTs();
		assertEquals(ts.length, 3);
		assertSame(ts[0], du.getT(Serializable.class));
		assertSame(ts[1], du.getT(Comparable.class));
		assertSame(ts[2], du.getT(CharSequence.class));

		// all Arrays have Cloneable & Serializable as Interfaces
		clazzes = int[].class.getInterfaces();
		assertEquals(clazzes.length, 2);
		assertSame(clazzes[0], Cloneable.class);
		assertSame(clazzes[1], Serializable.class);
		ts = du.getT(int[].class).getInterfaceTs();
		assertEquals(ts.length, 2);
		assertSame(ts[0], du.getT(Cloneable.class));
		assertSame(ts[1], du.getT(Serializable.class));
	}

	@Test
	void testIsAssignableFrom() {
		assertTrue(int.class.isAssignableFrom(int.class));
		assertTrue(T.INT.isAssignableFrom(T.INT));
		// missleading, assignableFrom() means is-superclass, for primitives
		// too! even though int=short/byte etc. is possible, false is returned!
		// we copy this behavior for better testability
		assertFalse(int.class.isAssignableFrom(byte.class));
		assertFalse(T.INT.isAssignableFrom(T.BYTE));
		assertTrue(int[].class.isAssignableFrom(int[].class));
		assertTrue(du.getT(int[].class).isAssignableFrom(du.getT(int[].class)));
		assertFalse(int[].class.isAssignableFrom(byte[].class));
		assertFalse(du.getT(int[].class)
				.isAssignableFrom(du.getT(byte[].class)));
		assertFalse(int[].class.isAssignableFrom(int[][].class));
		assertFalse(du.getT(int[].class).isAssignableFrom(
				du.getT(int[][].class)));

		assertFalse(Object.class.isAssignableFrom(byte.class));
		assertFalse(objectT.isAssignableFrom(du.getT(byte.class)));
		assertFalse(T.REF.isAssignableFrom(du.getT(byte.class)));
		assertFalse(T.AREF.isAssignableFrom(du.getT(byte.class)));

		assertTrue(Object.class.isAssignableFrom(Object.class));
		assertTrue(objectT.isAssignableFrom(du.getT(Object.class)));
		assertTrue(T.REF.isAssignableFrom(du.getT(Object.class)));
		assertTrue(T.AREF.isAssignableFrom(du.getT(Object.class)));
		assertTrue(Object.class.isAssignableFrom(Byte.class));
		assertTrue(objectT.isAssignableFrom(du.getT(Byte.class)));
		assertTrue(T.REF.isAssignableFrom(du.getT(Byte.class)));
		assertTrue(T.AREF.isAssignableFrom(du.getT(Byte.class)));
		assertTrue(Object.class.isAssignableFrom(Cloneable.class));
		assertTrue(objectT.isAssignableFrom(du.getT(Cloneable.class)));
		assertTrue(T.REF.isAssignableFrom(du.getT(Cloneable.class)));
		assertTrue(T.AREF.isAssignableFrom(du.getT(Cloneable.class)));

		assertTrue(Number.class.isAssignableFrom(Byte.class));
		assertTrue(du.getT(Number.class).isAssignableFrom(du.getT(Byte.class)));
		assertTrue(Comparable.class.isAssignableFrom(Byte.class));
		assertTrue(du.getT(Comparable.class).isAssignableFrom(
				du.getT(Byte.class)));
		assertTrue(Serializable.class.isAssignableFrom(Byte.class));
		assertTrue(du.getT(Serializable.class).isAssignableFrom(
				du.getT(Byte.class)));

		assertFalse(Cloneable.class.isAssignableFrom(Byte.class));
		assertFalse(du.getT(Cloneable.class).isAssignableFrom(
				du.getT(Byte.class)));

		// arrays are REFs with {Object,Cloneable,Serializable}
		assertTrue(Object.class.isAssignableFrom(byte[].class));
		assertTrue(objectT.isAssignableFrom(du.getT(byte[].class)));
		assertTrue(Cloneable.class.isAssignableFrom(byte[].class));
		assertTrue(du.getT(Cloneable.class).isAssignableFrom(
				du.getT(byte[].class)));
		assertTrue(Serializable.class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Serializable.class).isAssignableFrom(
				du.getT(byte[][][].class)));

		assertTrue(Serializable[][].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Serializable[][].class).isAssignableFrom(
				du.getT(byte[][][].class)));
		assertFalse(Serializable[][][].class.isAssignableFrom(byte[][][].class));
		assertFalse(du.getT(Serializable[][][].class).isAssignableFrom(
				du.getT(byte[][][].class)));

		// covariant arrays
		assertTrue(Number[].class.isAssignableFrom(Integer[].class));
		assertTrue(du.getT(Number[].class).isAssignableFrom(
				du.getT(Integer[].class)));
		// this is the Array-Serializable
		assertTrue(Serializable[][].class.isAssignableFrom(Byte[][][].class));
		assertTrue(du.getT(Serializable[][].class).isAssignableFrom(
				du.getT(Byte[][][].class)));
		assertTrue(Cloneable[][].class.isAssignableFrom(Byte[][][].class));
		assertTrue(du.getT(Cloneable[][].class).isAssignableFrom(
				du.getT(Byte[][][].class)));
		// this is the Number-Serializable...true!
		assertTrue(Serializable[][][].class.isAssignableFrom(Byte[][][].class));
		assertTrue(du.getT(Serializable[][][].class).isAssignableFrom(
				du.getT(Byte[][][].class)));
		assertFalse(Cloneable[][][].class.isAssignableFrom(Byte[][][].class));
		assertFalse(du.getT(Cloneable[][][].class).isAssignableFrom(
				du.getT(Byte[][][].class)));
	}

	@Test
	void testIsPrimitive() {
		assertTrue(int.class.isPrimitive());
		assertTrue(T.INT.isPrimitive());

		assertFalse(Object.class.isPrimitive());
		assertFalse(objectT.isPrimitive());

		assertFalse(int[].class.isPrimitive());
		assertFalse(du.getT(int[].class).isPrimitive());
	}

	@Test
	void testMergeRead() {
		assertSame(T.mergeRead(T.INT, T.INT), T.INT);
		assertSame(T.mergeRead(T.LONG, T.DOUBLE), T.WIDE);
	}

	@Test
	void testMergeStore() {
		assertSame(T.mergeStore(T.INT, T.INT), T.INT);
		assertSame(T.mergeStore(T.WIDE, T.LONG), T.LONG);

		assertNull(T.mergeStore(T.INT, T.SHORT));
		assertSame(T.mergeStore(T.INT, T.AINT), T.INT);
		assertSame(T.mergeStore(T.AINT, T.HINT), T.INT);

		assertSame(T.mergeStore(objectT, objectT), objectT);
		assertNull(T.mergeStore(objectT, T.INT));
		assertSame(T.mergeStore(objectT, du.getT(Integer.class)), objectT);
	}

	@Test
	void testName() {
		assertEquals(int.class.getName(), "int");
		assertEquals(du.getT(int.class).getName(), "int");

		assertEquals(Object.class.getName(), "java.lang.Object");
		assertEquals(objectT.getName(), "java.lang.Object");

		// strange rule for Class.getName(): just arrays with descriptor syntax,
		// but with dots
		assertEquals(Object[][].class.getName(), "[[Ljava.lang.Object;");
		// we handle that different
		assertEquals(du.getT(Object[][].class).getName(),
				"java.lang.Object[][]");

		// multi-types just for primitives / internal
		assertEquals(T.AINT.getName(), "{boolean,char,byte,short,int}");
	}

	@Test
	void testRead() {
		assertSame(T.HINT.read(T.INT), T.INT);
	}

	@Test
	void testSuperclass() {
		assertNull(int.class.getSuperclass());
		assertNull(T.INT.getSuperT());

		assertNull(Object.class.getSuperclass());
		assertNull(objectT.getSuperT());

		// {Interface}.class.getSuperclass() returns null, but
		// Object.class.isAssignableFrom({Interface}.class) is true!
		// Generic Signatures contain Object as Super -> hence
		// getSuperclass() has a historical glitch...we change that for us
		assertNull(Cloneable.class.getSuperclass());
		assertSame(du.getT(Cloneable.class).getSuperT(), objectT);

		assertSame(int[].class.getSuperclass(), Object.class);
		assertSame(du.getT(int[].class).getSuperT(), objectT);
	}

}