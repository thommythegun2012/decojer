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

		// all Arrays have Cloneable & Serializable as Interfaces
		Class<?>[] clazzes = int[].class.getInterfaces();
		assertEquals(clazzes.length, 2);
		assertSame(Cloneable.class, clazzes[0]);
		assertSame(Serializable.class, clazzes[1]);
		T[] ts = du.getT(int[].class).getInterfaceTs();
		assertEquals(ts.length, 2);
		assertSame(du.getT(Cloneable.class), ts[0]);
		assertSame(du.getT(Serializable.class), ts[1]);
	}

	@Test
	void testIsAssignableFrom() {
		// missleading, assignableFrom() means is-superclass, for primitives too
		assertFalse(int.class.isAssignableFrom(byte.class));
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
		assertSame(T.INT, T.mergeRead(T.INT, T.INT));
		assertSame(T.WIDE, T.mergeRead(T.LONG, T.DOUBLE));
	}

	@Test
	void testMergeStore() {
		assertSame(T.INT, T.mergeStore(T.INT, T.INT));
		assertSame(T.LONG, T.mergeStore(T.WIDE, T.LONG));

		assertNull(T.mergeStore(T.INT, T.SHORT));
		assertSame(T.INT, T.mergeStore(T.INT, T.AINT));
		assertSame(T.INT, T.mergeStore(T.AINT, T.HINT));
	}

	@Test
	void testName() {
		assertEquals("int", int.class.getName());
		assertEquals("int", du.getT(int.class).getName());

		assertEquals("java.lang.Object", Object.class.getName());
		assertEquals("java.lang.Object", objectT.getName());

		// strange rule for Class.getName(): just arrays with descriptor syntax,
		// but with dots
		assertEquals("[[Ljava.lang.Object;", Object[][].class.getName());
		// we handle that different
		assertEquals("java.lang.Object[][]", du.getT(Object[][].class)
				.getName());

		// multi-types just for primitives / internal
		assertEquals("{boolean,char,byte,short,int}", T.AINT.getName());
	}

	@Test
	void testSuperclass() {
		assertNull(int.class.getSuperclass());
		assertNull(T.INT.getSuperT());

		assertNull(Object.class.getSuperclass());
		assertNull(objectT.getSuperT());

		assertSame(Object.class, int[].class.getSuperclass());
		assertSame(objectT, du.getT(int[].class).getSuperT());
	}

}