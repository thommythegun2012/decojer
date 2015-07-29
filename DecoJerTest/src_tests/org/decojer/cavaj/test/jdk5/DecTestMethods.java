package org.decojer.cavaj.test.jdk5;

import java.lang.Thread.State;

public abstract class DecTestMethods {

	@DecTestAnnotations(charTest = 'x', stringTest = "test")
	public final byte getByteTest(
			@DecTestAnnotations(classTest = Appendable.class) final String test)
			throws IllegalAccessError {
		return 1;
	}

	@DecTestAnnotations(booleanTest = false, byteTest = 2, shortTest = 3, intTest = 4, longTest = 5)
	public abstract int getIntTest(final String test) throws Throwable;

	public int varargsTest(final int a,
			@DecTestAnnotations(enumTest = State.NEW) final Class<?>... b) {
		varargsTest(1);
		varargsTest(2, DecTestAnnotations.class);
		varargsTest(3, String.class, Object.class);
		return -1;
	}

}