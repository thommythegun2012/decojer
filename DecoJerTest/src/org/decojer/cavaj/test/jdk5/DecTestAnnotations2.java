package org.decojer.cavaj.test.jdk5;

import java.lang.Thread.State;
import java.util.List;

@DecTestAnnotations(charTest = 'x', shortTest = 1)
public @interface DecTestAnnotations2 {

	DecTestAnnotations annotationTest() default @DecTestAnnotations(intTest = 4, stringTest = "drei");

	byte[] arrayByteTest() default { 1, 2, 3 };

	boolean booleanTest() default true;

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

	String stringTest() default "zwei";

}