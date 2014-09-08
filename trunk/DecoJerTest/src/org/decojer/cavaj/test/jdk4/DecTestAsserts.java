package org.decojer.cavaj.test.jdk4;

public class DecTestAsserts {

	int i = 1;

	public void testAssert(final long l1, final long l2) {
		System.out.println("PRE");
		assert l1 != 0L;
		System.out.println("POST");
		assert l1 != l2;
		assert false;
		assert true;
	}

	public void testAssertCatch() {
		try {
			System.out.println("CATCH");
			assert i < 0 : i;
		} catch (RuntimeException e) {
			assert i > 0 : e;
		}
		System.out.println("POST");
	}

	public void testAssertMessage(final long l1, final long l2) {
		assert l1 < 0 : l1;
		assert l1 > 0 ? l1 < l2 : l1 > l2 : (l1 >>> 1) + l2 + "complex expression "
				+ (l1 > l2 && l2 > -1) + l1 * l2 + l1 + l2 + (l1 + l2);
	}

	public void testManualAssertBehindLoop() {
		for (int i = 1; i < 10; ++i) {
			System.out.println("I: " + i);
		}
		throw new AssertionError();
	}

}