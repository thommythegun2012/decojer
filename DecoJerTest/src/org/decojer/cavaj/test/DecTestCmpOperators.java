package org.decojer.cavaj.test;

public abstract class DecTestCmpOperators {

	public static void test(final double a, final double b) {
		System.out.println(a < b);
		System.out.println(a <= b);
		System.out.println(a == b);
		System.out.println(a != b);
		System.out.println(a >= b);
		System.out.println(a > b);
	}

	public static void test(final int a, final int b) {
		System.out.println(a < b);
		System.out.println(a <= b);
		System.out.println(a == b);
		System.out.println(a != b);
		System.out.println(a >= b);
		System.out.println(a > b);
	}

}