package org.decojer.cavaj.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.types.IntersectionT;
import org.decojer.cavaj.model.types.T;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestT {

	private static DU du;

	@BeforeClass
	public static void _beforeClass() {
		du = DecoJer.createDu();
	}

	@Test
	public void assignTo() {
		assertSame(T.INT.assignTo(T.INT), T.INT);
		// allowed in JVM: assertNull(T.INT.assignTo(T.BYTE));
		assertSame(T.BYTE.assignTo(T.INT), T.BYTE);

		assertSame(T.AINT.assignTo(T.BOOLEAN), T.BOOLEAN);

		assertSame(T.SINGLE.assignTo(T.AINT), T.AINT);
		assertSame(T.SINGLE.assignTo(T.SINGLE), T.SINGLE);
		assertNull(T.WIDE.assignTo(T.SINGLE));

		// we can assign Object to interface, will be checked at runtime!
		// works: Comparable c = (Comparable) new Object();
		// works: Serializable s = (Serializable) new HashSet();
		// we have to add casting!
		// but we don't do it here or intersect etc. wount work

		// TODO must recognice interface here! may be even set it as recognized interface
		// assertSame(du.getObjectT().assignTo(du.getT(Comparable.class)),
		// du.getT(Comparable.class));
		// assertSame(du.getT(Set.class).assignTo(du.getT(Serializable.class)), du.getT(Set.class));
	}

	@Test
	public void getComponentT() {
		assertSame(int[].class.getComponentType(), int.class);
		assertSame(du.getT(int[].class).getComponentT(), T.INT);

		assertSame(Object[].class.getComponentType(), Object.class);
		assertSame(du.getT(Object[].class).getComponentT(), du.getObjectT());

		assertSame(Object[][].class.getComponentType(), Object[].class);
		assertEquals(du.getT(Object[][].class).getComponentT(), du.getT(Object[].class));

		assertNull(Object.class.getComponentType());
		assertNull(du.getObjectT().getComponentT());

		assertSame(du.getArrayT(T.BYTE).getComponentT(), T.BYTE);
		assertSame(du.getArrayT(T.SMALL).getComponentT(), T.SMALL);
	}

	@Test
	public void getEnclosingT() {
		T t = du.getDescT(
				"Lorg/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>;");
		assertEquals(t.toString(),
				"org.pushingpixels.trident.TimelinePropertyBuilder$AbstractFieldInfo<java.lang.Object>");
		assertEquals(t.getQualifierT().toString(),
				"org.pushingpixels.trident.TimelinePropertyBuilder<T>");
		assertEquals(t.getName(),
				"org.pushingpixels.trident.TimelinePropertyBuilder$AbstractFieldInfo");
		assertTrue(t.eraseTo(du.getDescT(
				"Lorg/pushingpixels/trident/TimelinePropertyBuilder$AbstractFieldInfo;")));
	}

	@Test
	public void getInterfaceTs() {
		assertEquals(int.class.getInterfaces().length, 0);
		assertEquals(T.INT.getInterfaceTs().length, 0);

		assertEquals(Object.class.getInterfaces().length, 0);
		assertEquals(du.getObjectT().getInterfaceTs().length, 0);

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
	public void getJvmIntT() {
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
	public void getName() {
		assertEquals(int.class.getName(), "int");
		assertEquals(du.getT(int.class).getName(), "int");

		assertEquals(Object.class.getName(), "java.lang.Object");
		assertEquals(du.getObjectT().getName(), "java.lang.Object");

		// strange rule for Class.getName(): just arrays with descriptor syntax,
		// but with dots
		assertEquals(Object[][].class.getName(), "[[Ljava.lang.Object;");
		// we handle that different
		assertEquals(du.getT(Object[][].class).getName(), "java.lang.Object[][]");

		// multi-types just for primitives / internal
		assertEquals(T.AINT.getName(), "{int,short,byte,char,boolean}");

		assertEquals(Map.Entry.class.getName(), "java.util.Map$Entry");
		assertEquals(du.getT(Map.Entry.class).getName(), "java.util.Map$Entry");
	}

	@Test
	public void getPName() {
		assertEquals(du.getT(Map.Entry.class).getPName(), "Map$Entry");
	}

	@Test
	public void getSimpleName() {
		assertEquals(Map.Entry.class.getSimpleName(), "Entry");
		assertEquals(du.getT(Map.Entry.class).getSimpleName(), "Entry");
	}

	@Test
	public void getSuperT() {
		assertNull(int.class.getSuperclass());
		assertNull(T.INT.getSuperT());
		assertNull(byte.class.getSuperclass());
		assertNull(T.BYTE.getSuperT());

		assertNull(Object.class.getSuperclass());
		assertNull(du.getObjectT().getSuperT());

		assertNull(Cloneable.class.getSuperclass());
		assertNull(du.getT(Cloneable.class).getSuperT());

		assertSame(int[].class.getSuperclass(), Object.class);
		assertSame(du.getT(int[].class).getSuperT(), du.getObjectT());
		// is not Number[], even though this would be nice because of array
		// covariance:
		assertSame(Integer[].class.getSuperclass(), Object.class);
		assertSame(Integer[].class.getSuperclass(), Object.class);
	}

	@Test
	public void getTypeParameters() {
		assertEquals(int.class.getTypeParameters().length, 0);
		assertEquals(T.INT.getTypeParams().length, 0);

		assertEquals(Object.class.getTypeParameters().length, 0);
		assertEquals(du.getObjectT().getTypeParams().length, 0);

		assertEquals(int[].class.getTypeParameters().length, 0);
		assertEquals(du.getT(int[].class).getTypeParams().length, 0);

		assertEquals(List.class.getTypeParameters().length, 1);
		assertEquals(du.getT(List.class).getTypeParams().length, 1);

		assertEquals(Map.class.getTypeParameters().length, 2);
		assertEquals(du.getT(Map.class).getTypeParams().length, 2);
	}

	@Test
	public void intersect() {
		assertSame(T.intersect(T.INT, T.INT), T.INT);
		assertSame(T.intersect(T.SHORT, T.SHORT), T.SHORT);
		assertSame(T.intersect(T.BYTE, T.BYTE), T.BYTE);
		assertSame(T.intersect(T.CHAR, T.CHAR), T.CHAR);

		assertSame(T.intersect(T.INT, T.SHORT), T.INT);
		assertSame(T.intersect(T.SHORT, T.INT), T.INT);
		assertSame(T.intersect(T.INT, T.BYTE), T.INT);
		assertSame(T.intersect(T.BYTE, T.INT), T.INT);
		assertSame(T.intersect(T.INT, T.CHAR), T.INT);
		assertSame(T.intersect(T.CHAR, T.INT), T.INT);
		assertSame(T.intersect(T.SHORT, T.BYTE), T.SHORT);
		assertSame(T.intersect(T.BYTE, T.SHORT), T.SHORT);

		assertSame(T.intersect(T.BOOLEAN, T.BOOLEAN), T.BOOLEAN);
		assertSame(T.intersect(T.FLOAT, T.FLOAT), T.FLOAT);
		assertSame(T.intersect(T.LONG, T.LONG), T.LONG);
		assertSame(T.intersect(T.DOUBLE, T.DOUBLE), T.DOUBLE);

		// even though not allowed in Java without casts: JVM allows this
		assertSame(T.intersect(T.INT, T.BOOLEAN), T.INT);
		assertSame(T.intersect(T.BOOLEAN, T.INT), T.INT);

		assertNull(T.intersect(T.INT, T.FLOAT));
		assertNull(T.intersect(T.FLOAT, T.INT));
		assertNull(T.intersect(T.INT, T.LONG));
		assertNull(T.intersect(T.LONG, T.INT));
		assertNull(T.intersect(T.INT, T.DOUBLE));
		assertNull(T.intersect(T.DOUBLE, T.INT));

		assertSame(T.intersect(T.INT, T.AINT), T.INT);
		assertSame(T.intersect(T.AINT, T.INT), T.INT);
		assertSame(T.intersect(T.WIDE, T.LONG), T.LONG);
		assertSame(T.intersect(T.LONG, T.WIDE), T.LONG);

		assertSame(T.intersect(du.getObjectT(), du.getObjectT()), du.getObjectT());

		assertNull(T.intersect(du.getObjectT(), T.INT));
		assertNull(T.intersect(T.INT, du.getObjectT()));

		assertSame(T.intersect(du.getObjectT(), du.getT(Integer.class)), du.getObjectT());
		assertSame(T.intersect(du.getT(Integer.class), du.getObjectT()), du.getObjectT());

		assertSame(T.intersect(du.getObjectT(), du.getT(Cloneable.class)), du.getObjectT());
		assertSame(T.intersect(du.getT(Cloneable.class), du.getObjectT()), du.getObjectT());

		assertSame(T.intersect(du.getT(Serializable.class), du.getT(Byte.class)),
				du.getT(Serializable.class));
		assertSame(T.intersect(du.getT(Byte.class), du.getT(Serializable.class)),
				du.getT(Serializable.class));

		assertSame(T.intersect(du.getT(Element.class), du.getT(TypeElement.class)),
				du.getT(Element.class));
		assertSame(T.intersect(du.getT(TypeElement.class), du.getT(Element.class)),
				du.getT(Element.class));

		assertSame(T.intersect(du.getT(javax.swing.JComponent.class),
				du.getT(javax.swing.MenuElement.class)), du.getObjectT());
		assertSame(T.intersect(du.getT(javax.swing.MenuElement.class),
				du.getT(javax.swing.JComponent.class)), du.getObjectT());

		T t = T.intersect(du.getT(Integer.class), du.getT(Long.class));
		assertSame(t.getSuperT(), du.getT(Number.class));
		assertEquals(t.getInterfaceTs().length, 1);
		assertSame(t.getInterfaceTs()[0], du.getT(Comparable.class));
		assertEquals(t.getName(), "{java.lang.Number,java.lang.Comparable}");
		assertEquals(t.getSimpleName(), "{java.lang.Number,java.lang.Comparable}");
		// not same:
		assertEquals(T.intersect(du.getT(Long.class), du.getT(Integer.class)), t);

		// covariant arrays, but super/interfaces are {Object,Cloneable,Serializable}, not
		// {superXY}[], and it doesn't work for primitives because no auto-conversion!
		assertEquals(T.intersect(du.getT(Integer[].class), du.getT(Long[].class)).getName(),
				"{java.lang.Number,java.lang.Comparable}[]");
		assertEquals(T.intersect(du.getT(Integer[].class), du.getT(Number[].class)),
				du.getT(Number[].class));
		t = T.intersect(du.getT(byte[].class), du.getT(char[].class));
		assertSame(t.getSuperT(), du.getObjectT());
		assertEquals(t.getInterfaceTs().length, 2);
		assertSame(t.getInterfaceTs()[0], du.getT(Cloneable.class));
		assertSame(t.getInterfaceTs()[1], du.getT(Serializable.class));
		// but if we cannot join component types...
		t = T.intersect(du.getT(byte[].class), du.getT(long[].class));
		assertSame(t.getSuperT(), du.getObjectT());
		assertEquals(t.getInterfaceTs().length, 2);
		assertSame(t.getInterfaceTs()[0], du.getT(Cloneable.class));
		assertSame(t.getInterfaceTs()[1], du.getT(Serializable.class));

		t = T.intersect(du.getT(ArrayList.class), du.getT(Vector.class));
		assertTrue(t.isIntersection());
		// TODO java.util.List is too much, reduce!
		assertEquals(t.getName(),
				"{java.util.AbstractList,java.util.List,java.util.RandomAccess,java.lang.Cloneable,java.io.Serializable}");
	}

	@Test
	public void is() {
		assertTrue(T.AINT.is(T.INT, T.CHAR));
		assertFalse(T.AINT.is(T.INT, T.FLOAT));
		assertTrue(du.getObjectT().is(du.getObjectT()));
		assertFalse(du.getObjectT().is(du.getT(String.class)));
		assertFalse(du.getT(String.class).is(du.getObjectT()));
	}

	@Test
	public void isArray() {
		assertFalse(int.class.isArray());
		assertFalse(T.INT.isArray());

		assertFalse(void.class.isArray());
		assertFalse(T.VOID.isArray());

		assertFalse(Object.class.isArray());
		assertFalse(du.getObjectT().isArray());

		assertTrue(int[].class.isArray());
		assertTrue(du.getT(int[].class).isArray());

		assertTrue(Object[][].class.isArray());
		assertTrue(du.getT(Object[][].class).isArray());
	}

	@Test
	public void isAssignableFrom() {
		assertTrue(int.class.isAssignableFrom(int.class));
		assertTrue(T.INT.isAssignableFrom(T.INT));

		assertFalse(Object.class.isAssignableFrom(byte.class));
		assertFalse(du.getObjectT().isAssignableFrom(du.getT(byte.class)));
		assertFalse(T.REF.isAssignableFrom(du.getT(byte.class)));
		assertFalse(T.AREF.isAssignableFrom(du.getT(byte.class)));

		assertTrue(Object.class.isAssignableFrom(Object.class));
		assertTrue(du.getObjectT().isAssignableFrom(du.getT(Object.class)));
		assertTrue(T.REF.isAssignableFrom(du.getT(Object.class)));
		assertTrue(T.AREF.isAssignableFrom(du.getT(Object.class)));
		assertTrue(Object.class.isAssignableFrom(Byte.class));
		assertTrue(du.getObjectT().isAssignableFrom(du.getT(Byte.class)));
		assertTrue(T.REF.isAssignableFrom(du.getT(Byte.class)));
		assertTrue(T.AREF.isAssignableFrom(du.getT(Byte.class)));
		assertTrue(Object.class.isAssignableFrom(Cloneable.class));
		assertTrue(du.getObjectT().isAssignableFrom(du.getT(Cloneable.class)));
		assertTrue(T.REF.isAssignableFrom(du.getT(Cloneable.class)));
		assertTrue(T.AREF.isAssignableFrom(du.getT(Cloneable.class)));

		assertTrue(Number.class.isAssignableFrom(Byte.class));
		assertTrue(du.getT(Number.class).isAssignableFrom(du.getT(Byte.class)));
		assertTrue(Comparable.class.isAssignableFrom(Byte.class));
		assertTrue(du.getT(Comparable.class).isAssignableFrom(du.getT(Byte.class)));
		assertTrue(Serializable.class.isAssignableFrom(Byte.class));
		assertTrue(du.getT(Serializable.class).isAssignableFrom(du.getT(Byte.class)));

		assertFalse(Cloneable.class.isAssignableFrom(Byte.class));
		assertFalse(du.getT(Cloneable.class).isAssignableFrom(du.getT(Byte.class)));

		// arrays are REFs with {Object,Cloneable,Serializable}
		assertTrue(Object.class.isAssignableFrom(byte[].class));
		assertTrue(du.getObjectT().isAssignableFrom(du.getT(byte[].class)));
		assertTrue(Cloneable.class.isAssignableFrom(byte[].class));
		assertTrue(du.getT(Cloneable.class).isAssignableFrom(du.getT(byte[].class)));
		assertTrue(Serializable.class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Serializable.class).isAssignableFrom(du.getT(byte[][][].class)));

		assertTrue(Object[].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Object[].class).isAssignableFrom(du.getT(byte[][][].class)));
		assertTrue(Object[][].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Object[][].class).isAssignableFrom(du.getT(byte[][][].class)));

		assertTrue(du.getArrayT(T.REF).isAssignableFrom(du.getT(byte[][][].class)));
		assertTrue(du.getArrayT(T.SMALL).isAssignableFrom(du.getT(byte[].class)));

		assertTrue(Serializable[][].class.isAssignableFrom(byte[][][].class));
		assertTrue(du.getT(Serializable[][].class).isAssignableFrom(du.getT(byte[][][].class)));
		assertFalse(Serializable[][][].class.isAssignableFrom(byte[][][].class));
		assertFalse(du.getT(Serializable[][][].class).isAssignableFrom(du.getT(byte[][][].class)));

		assertTrue(int[].class.isAssignableFrom(int[].class));
		assertTrue(du.getT(int[].class).isAssignableFrom(du.getT(int[].class)));
		assertFalse(int[].class.isAssignableFrom(int[][].class));
		assertFalse(du.getT(int[].class).isAssignableFrom(du.getT(int[][].class)));
		assertFalse(int[][][].class.isAssignableFrom(int[].class));
		assertFalse(du.getT(int[][][].class).isAssignableFrom(du.getT(int[].class)));

		// covariant arrays
		assertTrue(Number[].class.isAssignableFrom(Integer[].class));
		assertTrue(du.getT(Number[].class).isAssignableFrom(du.getT(Integer[].class)));
		// this is the Array-Serializable
		assertTrue(Serializable[][].class.isAssignableFrom(Byte[][][].class));
		assertTrue(du.getT(Serializable[][].class).isAssignableFrom(du.getT(Byte[][][].class)));
		assertTrue(Cloneable[][].class.isAssignableFrom(Byte[][][].class));
		assertTrue(du.getT(Cloneable[][].class).isAssignableFrom(du.getT(Byte[][][].class)));
		// this is the Number-Serializable...true!
		assertTrue(Serializable[][][].class.isAssignableFrom(Byte[][][].class));
		assertTrue(du.getT(Serializable[][][].class).isAssignableFrom(du.getT(Byte[][][].class)));
		assertFalse(Cloneable[][][].class.isAssignableFrom(Byte[][][].class));
		assertFalse(du.getT(Cloneable[][][].class).isAssignableFrom(du.getT(Byte[][][].class)));

		// even though arrays are covariant in the Java language, no auto-conversion is applied
		// here and "int[] is = new byte[1]" isn't allowed in Java:
		// isAssignableFrom() usually means "is-superclass-of" in JDK function, but even though
		// "int i = short/etc." is not an allowed assignment by inheritence (it's an
		// auto-conversion) we allow it here
		assertFalse(int.class.isAssignableFrom(byte.class));
		assertTrue(T.INT.isAssignableFrom(T.BYTE));
		// no auto-conversion for "int i = boolean"
		assertFalse(int.class.isAssignableFrom(boolean.class));
		assertFalse(T.INT.isAssignableFrom(T.BOOLEAN));
		// no auto-conversion for array components
		assertFalse(int[].class.isAssignableFrom(byte[].class));
		assertFalse(du.getT(int[].class).isAssignableFrom(du.getT(byte[].class)));

		assertTrue(new IntersectionT(du.getT(Object.class), du.getT(Cloneable.class),
				du.getT(Serializable.class)).isAssignableFrom(du.getArrayT(du.getObjectT())));
		assertFalse(new IntersectionT(du.getT(Object.class), du.getT(Cloneable.class),
				du.getT(Serializable.class)).isAssignableFrom(du.getObjectT()));

		// we can assign Object to interface, will be checked at runtime!
		// works: Comparable c = (Comparable) new Object();
		// works: Serializable s = (Serializable) new HashSet();
		// we have to add casting!
		// but we don't do it here or intersect etc. wount work
		assertFalse(Comparable.class.isAssignableFrom(Object.class));
		assertFalse(du.getT(Comparable.class).isAssignableFrom(du.getObjectT()));
		assertFalse(Serializable.class.isAssignableFrom(Set.class));
		assertFalse(du.getT(Serializable.class).isAssignableFrom(du.getT(Set.class)));
	}

	@Test
	public void isInterface() {
		assertFalse(Object.class.isInterface());
		assertFalse(du.getObjectT().isInterface());
		assertFalse(int.class.isInterface());
		assertFalse(T.INT.isInterface());
		assertFalse(String.class.isInterface());
		assertFalse(du.getT(String.class).isInterface());
		assertFalse(String[].class.isInterface());
		assertFalse(du.getT(String[].class).isInterface());
		assertTrue(Comparable.class.isInterface());
		assertTrue(du.getT(Comparable.class).isInterface());
	}

	@Test
	public void isMulti() {
		assertFalse(T.INT.isMulti());
		assertFalse(T.VOID.isMulti());
		assertFalse(T.REF.isMulti());
		assertFalse(T.RET.isMulti());

		assertTrue(T.AINT.isMulti());
		assertTrue(T.AREF.isMulti());
		assertTrue(T.SINGLE.isMulti());
	}

	@Test
	public void isObject() {
		assertTrue(du.getObjectT().isObject());
		assertFalse(T.INT.isObject());
		assertFalse(du.getT(String.class).isObject());
		assertFalse(du.getT(Comparable.class).isObject());
	}

	@Test
	public void isPrimitive() {
		assertTrue(int.class.isPrimitive());
		assertTrue(T.INT.isPrimitive());

		assertFalse(Object.class.isPrimitive());
		assertFalse(du.getObjectT().isPrimitive());

		assertFalse(int[].class.isPrimitive());
		assertFalse(du.getT(int[].class).isPrimitive());
	}

	@Test
	public void isUnresolvable() {
		assertFalse(du.getObjectT().isUnresolvable());
		assertFalse(T.INT.isUnresolvable());
		assertFalse(T.VOID.isUnresolvable());
		assertFalse(du.getT(Character.class).isUnresolvable());
		assertFalse(du.getT(Double[][].class).isUnresolvable());
		assertTrue(du.getT("Test").isUnresolvable());
	}

	@Test
	public void union() {
		assertSame(T.union(T.INT, T.INT), T.INT);
		assertSame(T.union(T.LONG, T.DOUBLE), T.WIDE);
	}

}