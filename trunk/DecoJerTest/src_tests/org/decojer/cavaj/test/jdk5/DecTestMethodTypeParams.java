package org.decojer.cavaj.test.jdk5;

import java.util.ArrayList;

import javax.accessibility.Accessible;

public class DecTestMethodTypeParams<T extends Integer & Cloneable & Accessible> {

	private static <T extends Integer, E extends RuntimeException> void parameterizedStaticClassMethod(
			final T a, final T b) throws E, RuntimeException {
		System.out.println(a);
		System.out.println(b);
	}

	private <T extends Integer, U extends Long> void parameterizedClassMethod(
			final T a, final U b) throws RuntimeException {
		System.out.println(a);
		System.out.println(b);
	}

	private <U extends Long & Accessible> U parameterizedClassMethod2(
			final T a, final U b) {
		System.out.println(a);
		System.out.println(b);
		return null;
	}

	// wouldn't work with static!
	private <U extends T> void parameterizedClassMethod3(final T a, final U b) {
		System.out.println(a);
		System.out.println(b);
		class Test extends ArrayList<U> {

		}
	}

}