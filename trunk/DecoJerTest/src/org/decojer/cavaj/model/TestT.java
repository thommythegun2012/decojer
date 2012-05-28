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
		assertSame(clazzes[0], Cloneable.class);
		assertSame(clazzes[1], Serializable.class);
		T[] ts = du.getT(int[].class).getInterfaceTs();
		assertEquals(ts.length, 2);
		assertSame(ts[0], du.getT(Cloneable.class));
		assertSame(ts[1], du.getT(Serializable.class));
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

		assertSame(int[].class.getSuperclass(), Object.class);
		assertSame(du.getT(int[].class).getSuperT(), objectT);
	}

}