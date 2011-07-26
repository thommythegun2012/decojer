package org.decojer.cavaj.test.jdk5;

import java.lang.Thread.State;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
public @interface DecTestAnnotations {

	SuppressWarnings[] annotationArrayTest() default {
			@SuppressWarnings("TEST1"), @SuppressWarnings(value = "TEST2") };

	SuppressWarnings annotationTest() default @SuppressWarnings("TEST");

	Deprecated annotationTest2() default @Deprecated;

	boolean booleanTest() default true;

	byte[] byteArrayTest() default { 1, 2, 3 };

	byte byteTest() default 2;

	char charTest() default 'b';

	// runtime visible in bytecode allways after invisible
	@Deprecated
	@DecTestAnnotations
	Class classTest() default byte.class;

	@DecTestAnnotations(1)
	Class classTest2() default byte[].class;

	@DecTestAnnotations(value = 1, stringTest = "value is necessary here")
	Class classTest3() default Byte.class;

	Class classTest4() default Byte[][][].class;

	Class classTest5() default void.class; // no array

	Class classTest6() default List.class; // no generics

	double doubleTest() default 2.1D;

	State enumTest() default State.BLOCKED;

	float floatTest() default 2.1F;

	int intTest() default 2;

	long longTest() default 2L;

	short shortTest() default 2;

	String[] stringArrayTest() default { "eins", "zwei" };

	String stringTest() default "zwei";

	byte value() default 1;

}