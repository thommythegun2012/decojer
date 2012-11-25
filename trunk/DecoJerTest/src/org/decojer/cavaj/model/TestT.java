package org.decojer.cavaj.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.types.ClassT;
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
	void getComponentT() {
		assertSame(int[].class.getComponentType(), int.class);
		assertSame(du.getT(int[].class).getComponentT(), T.INT);

		assertSame(Object[].class.getComponentType(), Object.class);
		assertSame(du.getT(Object[].class).getComponentT(), objectT);

		assertSame(Object[][].class.getComponentType(), Object[].class);
		assertEquals(du.getT(Object[][].class).getComponentT(),
				du.getT(Object[].class)); // EQUALS!!!

		assertNull(Object.class.getComponentType());
		assertNull(objectT.getComponentT());

		assertSame(du.getArrayT(T.BYTE).getComponentT(), T.BYTE);
		assertSame(du.getArrayT(T.SMALL).getComponentT(), T.SMALL);
	}

	@Test
	void getInterfaceTs() {
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
	void getJvmIntT() {
		assertSame(T.getJvmIntT(Short.MIN_VALUE - 1), T.INT);
		assertSame(T.getJvmIntT(Short.MIN_VALUE), T.SHORT);
		assertSame(T.getJvmIntT(Byte.MIN_VALUE - 1), T.SHORT);
		assertSame(T.getJvmIntT(Byte.MIN_VALUE), T.BYTE);
		assertSame(T.getJvmIntT(-1), T.BYTE);
		assertEquals(T.getJvmIntT(0).getName(), "{byte,char,boolean}");
		assertEquals(T.getJvmIntT(1).getName(), "{byte,char,boolean}");
		assertEquals(T.getJvmIntT(2).getName(), "{byte,char}");
		assertEquals(T.getJvmIntT(Byte.MAX_VALUE).getName(), "{byte,char}");
		assertEquals(T.getJvmIntT(Byte.MAX_VALUE + 1).getName(), "{short,char}");
		assertEquals(T.getJvmIntT(Short.MAX_VALUE).getName(), "{short,char}");
		assertEquals(T.getJvmIntT(Short.MAX_VALUE + 1).getName(), "{int,char}");
		assertEquals(T.getJvmIntT(Character.MAX_VALUE).getName(), "{int,char}");
		assertSame(T.getJvmIntT(Character.MAX_VALUE + 1), T.INT);
	}

	@Test
	void getName() {
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
	void getSuperT() {
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
		// is not Number[], even though this would be nice because of array
		// covariance:
		assertSame(Integer[].class.getSuperclass(), Object.class);
		assertSame(Integer[].class.getSuperclass(), Object.class);
	}

	@Test
	void getTypeParameters() {
		assertEquals(int.class.getTypeParameters().length, 0);
		assertEquals(T.INT.getTypeParams().length, 0);

		assertEquals(Object.class.getTypeParameters().length, 0);
		assertEquals(objectT.getTypeParams().length, 0);

		assertEquals(int[].class.getTypeParameters().length, 0);
		assertEquals(du.getT(int[].class).getTypeParams().length, 0);

		assertEquals(List.class.getTypeParameters().length, 1);
		assertEquals(du.getT(List.class).getTypeParams().length, 1);

		assertEquals(Map.class.getTypeParameters().length, 2);
		assertEquals(du.getT(Map.class).getTypeParams().length, 2);
	}

	@Test
	void is() {
		assertTrue(T.AINT.is(T.INT, T.CHAR));
		assertFalse(T.AINT.is(T.INT, T.FLOAT));
		assertTrue(objectT.is(objectT));
		assertFalse(objectT.is(du.getT(String.class)));
		assertFalse(du.getT(String.class).is(objectT));
	}

	@Test
	void isArray() {
		assertFalse(int.class.isArray());
		assertFalse(T.INT.isArray());

		assertFalse(void.class.isArray());
		assertFalse(T.VOID.isArray());

		assertFalse(Object.class.isArray());
		assertFalse(objectT.isArray());

		assertTrue(int[].class.isArray());
		assertTrue(du.getT(int[].class).isArray());

		assertTrue(Object[][].class.isArray());
		assertTrue(du.getT(Object[][].class).isArray());
	}

	@Test
	void isAssignableFrom() {
		assertTrue(int.class.isAssignableFrom(int.class));
		assertTrue(T.INT.isAssignableFrom(T.INT));

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

		assertTrue(Object[].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Object[].class).isAssignableFrom(
				du.getT(byte[][][].class)));
		assertTrue(Object[][].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Object[][].class).isAssignableFrom(
				du.getT(byte[][][].class)));

		assertTrue(du.getArrayT(T.REF).isAssignableFrom(
				du.getT(byte[][][].class)));
		assertTrue(du.getArrayT(T.SMALL)
				.isAssignableFrom(du.getT(byte[].class)));

		assertTrue(Serializable[][].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Serializable[][].class).isAssignableFrom(
				du.getT(byte[][][].class)));
		assertFalse(Serializable[][][].class.isAssignableFrom(byte[][][].class));
		assertFalse(du.getT(Serializable[][][].class).isAssignableFrom(
				du.getT(byte[][][].class)));

		// FIXME to assertTrue!? anonymous join type
		assertFalse(new ClassT(du.getT(Object.class), du.getT(Cloneable.class),
				du.getT(Serializable.class)).isAssignableFrom(du
				.getArrayT(objectT)));

		assertTrue(int[].class.isAssignableFrom(int[].class));
		assertTrue(du.getT(int[].class).isAssignableFrom(du.getT(int[].class)));
		assertFalse(int[].class.isAssignableFrom(int[][].class));
		assertFalse(du.getT(int[].class).isAssignableFrom(
				du.getT(int[][].class)));
		assertFalse(int[][][].class.isAssignableFrom(int[].class));
		assertFalse(du.getT(int[][][].class).isAssignableFrom(
				du.getT(int[].class)));

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

		// assignableFrom() means is-superclass-of in JDK function, for
		// primitives too! even though int=short/byte/char etc. is possible,
		// false is returned! we change this behavior for the decompiler:
		assertFalse(int.class.isAssignableFrom(byte.class));
		assertTrue(T.INT.isAssignableFrom(T.BYTE));
		// but cannot int=boolean
		assertFalse(int.class.isAssignableFrom(boolean.class));
		assertFalse(T.INT.isAssignableFrom(T.BOOLEAN));
		// but this isn't covariant in the Java language:
		assertFalse(int[].class.isAssignableFrom(byte[].class));
		// FIXME assertFalse!
		assertTrue(du.getT(int[].class).isAssignableFrom(du.getT(byte[].class)));
	}

	@Test
	void isInterface() {
		assertFalse(objectT.isInterface());
		assertFalse(T.INT.isInterface());
		assertFalse(du.getT(String.class).isInterface());
		assertTrue(du.getT(Comparable.class).isInterface());
	}

	@Test
	void isMulti() {
		assertFalse(T.INT.isMulti());
		assertFalse(T.VOID.isMulti());
		assertFalse(T.REF.isMulti());
		assertFalse(T.RET.isMulti());

		assertTrue(T.AINT.isMulti());
		assertTrue(T.AREF.isMulti());
		assertTrue(T.SINGLE.isMulti());
	}

	@Test
	void isObject() {
		assertTrue(objectT.isObject());
		assertFalse(T.INT.isObject());
		assertFalse(du.getT(String.class).isObject());
		assertFalse(du.getT(Comparable.class).isObject());
	}

	@Test
	void isPrimitive() {
		assertTrue(int.class.isPrimitive());
		assertTrue(T.INT.isPrimitive());

		assertFalse(Object.class.isPrimitive());
		assertFalse(objectT.isPrimitive());

		assertFalse(int[].class.isPrimitive());
		assertFalse(du.getT(int[].class).isPrimitive());
	}

	@Test
	void isResolvable() {
		assertTrue(objectT.isResolvable());
		assertTrue(T.INT.isResolvable());
		assertTrue(T.VOID.isResolvable());
		assertTrue(du.getT(Character.class).isResolvable());
		assertTrue(du.getT(Double[][].class).isResolvable());
		assertFalse(du.getT("Test").isResolvable());
	}

	@Test
	void join() {
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

		T t = T.join(du.getT(Integer.class), du.getT(Long.class));
		assertSame(t.getSuperT(), du.getT(Number.class));
		assertEquals(t.getInterfaceTs().length, 1);
		assertSame(t.getInterfaceTs()[0], du.getT(Comparable.class));
		assertEquals(t.getName(), "{java.lang.Number,java.lang.Comparable}");
		assertEquals(t.getSimpleName(),
				"{java.lang.Number,java.lang.Comparable}");

		// join shouldn't be Object, Cloneable, Serializable:
		// FIXME assertSame would be nice
		assertEquals(T.join(du.getT(Integer[].class), du.getT(Number[].class)),
				du.getT(Number[].class));
	}

	@Test
	void read() {
		assertSame(T.INT.read(T.INT), T.INT);
		assertNull(T.INT.read(T.BYTE));
		assertSame(T.BYTE.read(T.INT), T.BYTE);

		assertSame(T.SINGLE.read(T.AINT), T.AINT);
		assertSame(T.SINGLE.read(T.SINGLE), T.SINGLE);
		assertNull(T.WIDE.read(T.SINGLE));
	}

	@Test
	void test() {
		// FIXME not yet finalized...should keep upper type parameters
		T t = du.getDescT("Lorg/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>;");
		assertEquals(
				t.getName(),
				"org.pushingpixels.trident.TimelinePropertyBuilder$AbstractFieldInfo<java.lang.Object>");
	}

	@Test
	void union() {
		assertSame(T.union(T.INT, T.INT), T.INT);
		assertSame(T.union(T.LONG, T.DOUBLE), T.WIDE);
	}

}