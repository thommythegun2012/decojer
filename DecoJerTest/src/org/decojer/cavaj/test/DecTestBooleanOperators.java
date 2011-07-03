package org.decojer.cavaj.test;

public abstract class DecTestBooleanOperators {

	public static void test(final boolean a, final boolean b, final boolean c) {
		System.out.println(" !&&");
		System.out.println(!a && b && c);
		System.out.println(!(a && b) && c);
		System.out.println(!(a && b && c));
		System.out.println(" !||");
		System.out.println(!a || b || c);
		System.out.println(!(a || b) || c);
		System.out.println(!(a || b || c));
	}

}