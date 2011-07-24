package org.decojer.cavaj.test.jdk5;

public abstract class DecTestMethods<E extends Appendable> {

	@DecTestAnnotations(byteTest = 1)
	public final byte getByteTest(
			@DecTestAnnotations(intTest = 1, charTest = 'c') final E test)
			throws IllegalAccessError {
		return 1;
	}

	@DecTestAnnotations(intTest = 1, charTest = 'c')
	public abstract int getIntTest(final String test) throws Throwable;

	public int varargsTest(final int a, final Object... b) {
		return 1;
	}

}