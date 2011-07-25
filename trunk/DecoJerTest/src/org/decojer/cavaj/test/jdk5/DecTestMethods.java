package org.decojer.cavaj.test.jdk5;

import java.lang.Thread.State;

public abstract class DecTestMethods {

	@DecTestAnnotations(byteNoDefault = 1, charTest = 'x', stringTest = "test")
	public final byte getByteTest(
			@DecTestAnnotations(byteNoDefault = 1, classTest = Appendable.class) final String test)
			throws IllegalAccessError {
		return 1;
	}

	@DecTestAnnotations(byteNoDefault = 1, booleanTest = false, byteTest = 2, shortTest = 3, intTest = 4, longTest = 5)
	public abstract int getIntTest(final String test) throws Throwable;

	public int varargsTest(
			final int a,
			@DecTestAnnotations(byteNoDefault = 1, enumTest = State.NEW) final Object... b) {
		return 1;
	}

}