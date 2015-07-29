package org.decojer.cavaj.test.jdk2;

public strictfp class DecTestStrictFp {

	public static final strictfp synchronized double tests(double a, double b) {
		return a + b;
	}

	protected strictfp double test(double a, double b) {
		return a + b;
	}

}