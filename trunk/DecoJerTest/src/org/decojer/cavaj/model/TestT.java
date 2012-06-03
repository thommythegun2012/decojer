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
	void testIsMulti() {
		assertFalse(T.INT.isMulti());
		assertFalse(T.VOID.isMulti());
		assertFalse(T.REF.isMulti());
		assertFalse(T.RET.isMulti());

		assertTrue(T.AINT.isMulti());
		assertTrue(T.AREF.isMulti());
		assertTrue(T.DINT.isMulti());
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
	void testJoin() {
		assertSame(T.join(T.INT, T.INT), T.INT);
		assertSame(T.join(T.SHORT, T.SHORT), T.SHORT);
		assertSame(T.join(T.BYTE, T.BYTE), T.BYTE);
		assertSame(T.join(T.CHAR, T.CHAR), T.CHAR);

		assertSame(T.join(T.INT, T.SHORT), T.INT);
		assertSame(T.join(T.SHORT, T.INT), T.INT);
		assertSame(T.join(T.INT, T.BYTE), T.INT);
		assertSame(T.join(T.BYTE, T.INT), T.INT);
		assertSame(T.join(T.INT, T.CHAR), T.INT);
		assertSame(T.join(T.CHAR, T.INT), T.INT);
		assertSame(T.join(T.SHORT, T.BYTE), T.SHORT);
		assertSame(T.join(T.BYTE, T.SHORT), T.SHORT);

		assertSame(T.join(T.BOOLEAN, T.BOOLEAN), T.BOOLEAN);
		assertSame(T.join(T.FLOAT, T.FLOAT), T.FLOAT);
		assertSame(T.join(T.LONG, T.LONG), T.LONG);
		assertSame(T.join(T.DOUBLE, T.DOUBLE), T.DOUBLE);

		assertNull(T.join(T.INT, T.BOOLEAN));
		assertNull(T.join(T.BOOLEAN, T.INT));
		assertNull(T.join(T.INT, T.FLOAT));
		assertNull(T.join(T.FLOAT, T.INT));
		assertNull(T.join(T.INT, T.LONG));
		assertNull(T.join(T.LONG, T.INT));
		assertNull(T.join(T.INT, T.DOUBLE));
		assertNull(T.join(T.DOUBLE, T.INT));

		assertSame(T.join(T.INT, T.AINT), T.INT);
		assertSame(T.join(T.AINT, T.INT), T.INT);
		assertSame(T.join(T.WIDE, T.LONG), T.LONG);
		assertSame(T.join(T.LONG, T.WIDE), T.LONG);

		assertSame(T.join(objectT, objectT), objectT);

		assertNull(T.join(objectT, T.INT));
		assertNull(T.join(T.INT, objectT));

		assertSame(T.join(objectT, du.getT(Integer.class)), objectT);
		assertSame(T.join(du.getT(Integer.class), objectT), objectT);
	}

	@Test
	void testMergeUnion() {
		assertSame(T.mergeUnion(T.INT, T.INT), T.INT);
		assertSame(T.mergeUnion(T.LONG, T.DOUBLE), T.WIDE);
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
		assertEquals(T.AINT.getName(), "{int,short,byte,char,boolean}");
	}

	@Test
	void testRead() {
		assertSame(T.INT.read(T.INT), T.INT);
		assertNull(T.INT.read(T.BYTE));
		assertSame(T.BYTE.read(T.INT), T.BYTE);
	}

	@Test
	void testSuperclass() {
		assertNull(int.class.getSuperclass());
		assertNull(T.INT.getSuperT());
		assertNull(byte.class.getSuperclass());
		assertNull(T.BYTE.getSuperT());

		assertNull(Object.class.getSuperclass());
		assertNull(objectT.getSuperT());

		assertNull(Cloneable.class.getSuperclass());
		assertNull(du.getT(Cloneable.class).getSuperT());

		assertSame(int[].class.getSuperclass(), Object.class);
		assertSame(du.getT(int[].class).getSuperT(), objectT);
	}

}