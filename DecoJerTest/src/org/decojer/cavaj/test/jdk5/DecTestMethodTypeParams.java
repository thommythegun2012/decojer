package org.decojer.cavaj.test.jdk5;

import javax.accessibility.Accessible;

public class DecTestMethodTypeParams<T extends Integer & Cloneable & Accessible> {

	private static <T extends Integer> void parameterizedStaticClassMethod(
			final T a, final T b) {
		System.out.println(a);
		System.out.println(b);
	}

	private <T extends Integer, U extends Long> void parameterizedClassMethod(
			final T a, final U b) {
		System.out.println(a);
		System.out.println(b);
	}

	private <U extends Long> U parameterizedClassMethod2(final T a, final U b) {
		System.out.println(a);
		System.out.println(b);
		return (U) new Long(1L);
	}

	// wouldn't work with static!
	private <U extends T> void parameterizedClassMethod3(final T a, final U b) {
		System.out.println(a);
		System.out.println(b);
	}

}