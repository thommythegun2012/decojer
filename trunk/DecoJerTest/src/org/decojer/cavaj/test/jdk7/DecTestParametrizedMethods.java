package org.decojer.cavaj.test.jdk7;

public class DecTestParametrizedMethods<T extends Integer> {

	private static <T extends Integer> void parameterizedStaticClassMethod(
			final T a, final T b) {
		System.out.println(a + b);
	}

	private <T extends Integer, U extends Long> void parameterizedClassMethod(
			final T a, final U b) {
		System.out.println(a + b);
	}

	private <U extends Long> U parameterizedClassMethod2(final T a, final U b) {
		return (U) new Long(a + b);
	}

	private <U extends T> void parameterizedClassMethod3(final T a, final U b) {
		System.out.println(a + b);
	}

	public void testParametrizedMethods() {
		parameterizedStaticClassMethod(1, 2);
		DecTestParametrizedMethods.<Integer> parameterizedStaticClassMethod(3,
				4);
		this.<Integer, Long> parameterizedClassMethod(5, 6L);
		System.out.println(((DecTestParametrizedMethods<Integer>) this)
				.<Long> parameterizedClassMethod2(7, 8L));
		((DecTestParametrizedMethods<Integer>) this).parameterizedClassMethod3(
				9, 10);
	}

}