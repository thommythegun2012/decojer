package org.decojer.cavaj.test.jdk5;

import java.lang.Thread.State;
import java.util.List;

@DecTestAnnotations(byteNoDefault = 1, annotationTest = @SuppressWarnings("TEST_OVER"))
public @interface DecTestAnnotations {

	SuppressWarnings[] annotationArrayTest() default {
			@SuppressWarnings("TEST1"), @SuppressWarnings(value = "TEST2") };

	SuppressWarnings annotationTest() default @SuppressWarnings("TEST");

	Deprecated annotationTest2() default @Deprecated;

	@DecTestAnnotations(byteNoDefault = 1, annotationTest2 = @Deprecated)
	boolean booleanTest() default true;

	byte[] byteArrayTest() default { 1, 2, 3 };

	byte byteNoDefault();

	byte byteTest() default 2;

	char charTest() default 'b';

	Class classTest() default byte.class;

	Class classTest2() default List.class;

	Class classTest3() default Byte[][][].class; // no type arguments possible

	Class classTest4() default void.class;

	double doubleTest() default 2.1D;

	State enumTest() default State.BLOCKED;

	float floatTest() default 2.1F;

	int intTest() default 2;

	long longTest() default 2L;

	short shortTest() default 2;

	String[] stringArrayTest() default { "eins", "zwei" };

	String stringTest() default "zwei";

}