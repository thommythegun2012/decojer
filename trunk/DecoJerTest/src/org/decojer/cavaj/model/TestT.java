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
		assertSame(T.INT, T.INT.assignTo(T.INT));
		// allowed in JVM: assertNull(T.INT.assignTo(T.BYTE));
		assertSame(T.BYTE, T.BYTE.assignTo(T.INT));

		assertSame(T.BOOLEAN, T.AINT.assignTo(T.BOOLEAN));

		assertSame(T.AINT, T.SINGLE.assignTo(T.AINT));
		assertSame(T.SINGLE, T.SINGLE.assignTo(T.SINGLE));
		assertNull(T.WIDE.assignTo(T.SINGLE));

		// we can assign Object to interface, will be checked at runtime!
		// works: Comparable c = (Comparable) new Object();
		// works: Serializable s = (Serializable) new HashSet();
		// we have to add casting!
		// but we don't do it here or intersect etc. wount work

		// TODO must recognice interface here! may be even set it as recognized interface
		// assertSame(du.getT(Comparable.class),
		// du.getObjectT().assignTo(du.getT(Comparable.class)));
		// assertSame(du.getT(Set.class), du.getT(Set.class).assignTo(du.getT(Serializable.class)));
	}

	@Test
	public void getComponentT() {
		assertSame(int.class, int[].class.getComponentType());
		assertSame(T.INT, du.getT(int[].class).getComponentT());

		assertSame(Object.class, Object[].class.getComponentType());
		assertSame(du.getObjectT(), du.getT(Object[].class).getComponentT());

		assertSame(Object[].class, Object[][].class.getComponentType());
		assertEquals(du.getT(Object[].class), du.getT(Object[][].class).getComponentT());

		assertNull(Object.class.getComponentType());
		assertNull(du.getObjectT().getComponentT());

		assertSame(T.BYTE, du.getArrayT(T.BYTE).getComponentT());
		assertSame(T.SMALL, du.getArrayT(T.SMALL).getComponentT());
	}

	@Test
	public void getEnclosingT() {
		T t = du.getDescT(
				"Lorg/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>;");
		assertEquals(
				"org.pushingpixels.trident.TimelinePropertyBuilder$AbstractFieldInfo<java.lang.Object>",
				t.toString());
		assertEquals("org.pushingpixels.trident.TimelinePropertyBuilder<T>",
				t.getQualifierT().toString());
		assertEquals("org.pushingpixels.trident.TimelinePropertyBuilder$AbstractFieldInfo",
				t.getName());
		assertTrue(t.eraseTo(du.getDescT(
				"Lorg/pushingpixels/trident/TimelinePropertyBuilder$AbstractFieldInfo;")));
	}

	@Test
	public void getInterfaceTs() {
		assertEquals(0, int.class.getInterfaces().length);
		assertEquals(0, T.INT.getInterfaceTs().length);

		assertEquals(0, Object.class.getInterfaces().length);
		assertEquals(0, du.getObjectT().getInterfaceTs().length);

		// Interface order is relevant
		Class<?>[] clazzes = String.class.getInterfaces();
		assertEquals(3, clazzes.length);
		assertSame(Serializable.class, clazzes[0]);
		assertSame(Comparable.class, clazzes[1]);
		assertSame(CharSequence.class, clazzes[2]);
		T[] ts = du.getT(String.class).getInterfaceTs();
		assertEquals(3, ts.length);
		assertSame(du.getT(Serializable.class), ts[0]);
		assertSame(du.getT(Comparable.class), ts[1]);
		assertSame(du.getT(CharSequence.class), ts[2]);

		// all Arrays have Cloneable & Serializable as Interfaces
		clazzes = int[].class.getInterfaces();
		assertEquals(2, clazzes.length);
		assertSame(Cloneable.class, clazzes[0]);
		assertSame(Serializable.class, clazzes[1]);
		ts = du.getT(int[].class).getInterfaceTs();
		assertEquals(2, ts.length);
		assertSame(du.getT(Cloneable.class), ts[0]);
		assertSame(du.getT(Serializable.class), ts[1]);
	}

	@Test
	public void getJvmIntT() {
		assertSame(T.INT, T.getJvmIntT(Short.MIN_VALUE - 1));
		assertSame(T.SHORT, T.getJvmIntT(Short.MIN_VALUE));
		assertSame(T.SHORT, T.getJvmIntT(Byte.MIN_VALUE - 1));
		assertSame(T.BYTE, T.getJvmIntT(Byte.MIN_VALUE));
		assertSame(T.BYTE, T.getJvmIntT(-1));
		assertEquals("{byte,char,boolean}", T.getJvmIntT(0).getName());
		assertEquals("{byte,char,boolean}", T.getJvmIntT(1).getName());
		assertEquals("{byte,char}", T.getJvmIntT(2).getName());
		assertEquals("{byte,char}", T.getJvmIntT(Byte.MAX_VALUE).getName());
		assertEquals("{short,char}", T.getJvmIntT(Byte.MAX_VALUE + 1).getName());
		assertEquals("{short,char}", T.getJvmIntT(Short.MAX_VALUE).getName());
		assertEquals("{int,char}", T.getJvmIntT(Short.MAX_VALUE + 1).getName());
		assertEquals("{int,char}", T.getJvmIntT(Character.MAX_VALUE).getName());
		assertSame(T.INT, T.getJvmIntT(Character.MAX_VALUE + 1));
	}

	@Test
	public void getName() {
		assertEquals("int", int.class.getName());
		assertEquals("int", du.getT(int.class).getName());

		assertEquals("java.lang.Object", Object.class.getName());
		assertEquals("java.lang.Object", du.getObjectT().getName());

		// strange rule for Class.getName(): just arrays with descriptor syntax,
		// but with dots
		assertEquals("[[Ljava.lang.Object;", Object[][].class.getName());
		// we handle that different
		assertEquals("java.lang.Object[][]", du.getT(Object[][].class).getName());

		// multi-types just for primitives / internal
		assertEquals("{int,short,byte,char,boolean}", T.AINT.getName());

		assertEquals("java.util.Map$Entry", Map.Entry.class.getName());
		assertEquals("java.util.Map$Entry", du.getT(Map.Entry.class).getName());
	}

	@Test
	public void getPName() {
		assertEquals("Map$Entry", du.getT(Map.Entry.class).getPName());
	}

	@Test
	public void getSimpleName() {
		assertEquals("Entry", Map.Entry.class.getSimpleName());
		assertEquals("Entry", du.getT(Map.Entry.class).getSimpleName());
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

		assertSame(Object.class, int[].class.getSuperclass());
		assertSame(du.getObjectT(), du.getT(int[].class).getSuperT());
		// is not Number[], even though this would be nice because of array
		// covariance:
		assertSame(Object.class, Integer[].class.getSuperclass());
		assertSame(Object.class, Integer[].class.getSuperclass());
	}

	@Test
	public void getTypeParameters() {
		assertEquals(0, int.class.getTypeParameters().length);
		assertEquals(0, T.INT.getTypeParams().length);

		assertEquals(0, Object.class.getTypeParameters().length);
		assertEquals(0, du.getObjectT().getTypeParams().length);

		assertEquals(0, int[].class.getTypeParameters().length);
		assertEquals(0, du.getT(int[].class).getTypeParams().length);

		assertEquals(1, List.class.getTypeParameters().length);
		assertEquals(1, du.getT(List.class).getTypeParams().length);

		assertEquals(2, Map.class.getTypeParameters().length);
		assertEquals(2, du.getT(Map.class).getTypeParams().length);
	}

	@Test
	public void intersect() {
		assertSame(T.INT, T.intersect(T.INT, T.INT));
		assertSame(T.SHORT, T.intersect(T.SHORT, T.SHORT));
		assertSame(T.BYTE, T.intersect(T.BYTE, T.BYTE));
		assertSame(T.CHAR, T.intersect(T.CHAR, T.CHAR));

		assertSame(T.INT, T.intersect(T.INT, T.SHORT));
		assertSame(T.INT, T.intersect(T.SHORT, T.INT));
		assertSame(T.INT, T.intersect(T.INT, T.BYTE));
		assertSame(T.INT, T.intersect(T.BYTE, T.INT));
		assertSame(T.INT, T.intersect(T.INT, T.CHAR));
		assertSame(T.INT, T.intersect(T.CHAR, T.INT));
		assertSame(T.SHORT, T.intersect(T.SHORT, T.BYTE));
		assertSame(T.SHORT, T.intersect(T.BYTE, T.SHORT));

		assertSame(T.BOOLEAN, T.intersect(T.BOOLEAN, T.BOOLEAN));
		assertSame(T.FLOAT, T.intersect(T.FLOAT, T.FLOAT));
		assertSame(T.LONG, T.intersect(T.LONG, T.LONG));
		assertSame(T.DOUBLE, T.intersect(T.DOUBLE, T.DOUBLE));

		// even though not allowed in Java without casts: JVM allows this
		assertSame(T.INT, T.intersect(T.INT, T.BOOLEAN));
		assertSame(T.INT, T.intersect(T.BOOLEAN, T.INT));

		assertNull(T.intersect(T.INT, T.FLOAT));
		assertNull(T.intersect(T.FLOAT, T.INT));
		assertNull(T.intersect(T.INT, T.LONG));
		assertNull(T.intersect(T.LONG, T.INT));
		assertNull(T.intersect(T.INT, T.DOUBLE));
		assertNull(T.intersect(T.DOUBLE, T.INT));

		assertSame(T.INT, T.intersect(T.INT, T.AINT));
		assertSame(T.INT, T.intersect(T.AINT, T.INT));
		assertSame(T.LONG, T.intersect(T.WIDE, T.LONG));
		assertSame(T.LONG, T.intersect(T.LONG, T.WIDE));

		assertSame(du.getObjectT(), T.intersect(du.getObjectT(), du.getObjectT()));

		assertNull(T.intersect(du.getObjectT(), T.INT));
		assertNull(T.intersect(T.INT, du.getObjectT()));

		assertSame(du.getObjectT(), T.intersect(du.getObjectT(), du.getT(Integer.class)));
		assertSame(du.getObjectT(), T.intersect(du.getT(Integer.class), du.getObjectT()));

		assertSame(du.getObjectT(), T.intersect(du.getObjectT(), du.getT(Cloneable.class)));
		assertSame(du.getObjectT(), T.intersect(du.getT(Cloneable.class), du.getObjectT()));

		assertSame(du.getT(Serializable.class),
				T.intersect(du.getT(Serializable.class), du.getT(Byte.class)));
		assertSame(du.getT(Serializable.class),
				T.intersect(du.getT(Byte.class), du.getT(Serializable.class)));

		assertSame(du.getT(Element.class),
				T.intersect(du.getT(Element.class), du.getT(TypeElement.class)));
		assertSame(du.getT(Element.class),
				T.intersect(du.getT(TypeElement.class), du.getT(Element.class)));

		assertSame(du.getObjectT(), T.intersect(du.getT(javax.swing.JComponent.class),
				du.getT(javax.swing.MenuElement.class)));
		assertSame(du.getObjectT(), T.intersect(du.getT(javax.swing.MenuElement.class),
				du.getT(javax.swing.JComponent.class)));

		T t = T.intersect(du.getT(Integer.class), du.getT(Long.class));
		assertSame(du.getT(Number.class), t.getSuperT());
		assertEquals(1, t.getInterfaceTs().length);
		assertSame(du.getT(Comparable.class), t.getInterfaceTs()[0]);
		assertEquals("{java.lang.Number,java.lang.Comparable}", t.getName());
		assertEquals("{java.lang.Number,java.lang.Comparable}", t.getSimpleName());
		// not same:
		assertEquals(t, T.intersect(du.getT(Long.class), du.getT(Integer.class)));

		// covariant arrays, but super/interfaces are {Object,Cloneable,Serializable}, not
		// {superXY}[], and it doesn't work for primitives because no auto-conversion!
		assertEquals("{java.lang.Number,java.lang.Comparable}[]",
				T.intersect(du.getT(Integer[].class), du.getT(Long[].class)).getName());
		assertEquals(du.getT(Number[].class),
				T.intersect(du.getT(Integer[].class), du.getT(Number[].class)));
		t = T.intersect(du.getT(byte[].class), du.getT(char[].class));
		assertSame(du.getObjectT(), t.getSuperT());
		assertEquals(2, t.getInterfaceTs().length);
		assertSame(du.getT(Cloneable.class), t.getInterfaceTs()[0]);
		assertSame(du.getT(Serializable.class), t.getInterfaceTs()[1]);
		// but if we cannot join component types...
		t = T.intersect(du.getT(byte[].class), du.getT(long[].class));
		assertSame(du.getObjectT(), t.getSuperT());
		assertEquals(2, t.getInterfaceTs().length);
		assertSame(du.getT(Cloneable.class), t.getInterfaceTs()[0]);
		assertSame(du.getT(Serializable.class), t.getInterfaceTs()[1]);

		t = T.intersect(du.getT(ArrayList.class), du.getT(Vector.class));
		assertTrue(t.isIntersection());
		// TODO java.util.List is too much, reduce!
		assertEquals(
				"{java.util.AbstractList,java.util.List,java.util.RandomAccess,java.lang.Cloneable,java.io.Serializable}",
				t.getName());
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
		assertSame(T.INT, T.union(T.INT, T.INT));
		assertSame(T.WIDE, T.union(T.LONG, T.DOUBLE));
	}

}