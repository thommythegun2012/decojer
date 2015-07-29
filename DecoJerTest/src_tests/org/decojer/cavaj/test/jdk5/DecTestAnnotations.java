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

	@DecTestAnnotations(value = 1, stringTest = "value is necessary here")
	Class[] classArrayTest() default { byte.class, byte[].class, Byte.class,
			Byte[][][].class, void.class, List.class };

	@DecTestAnnotations
	Class classTest() default void.class; // no array

	@DecTestAnnotations(1)
	Class classTest2() default List.class; // no generics

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