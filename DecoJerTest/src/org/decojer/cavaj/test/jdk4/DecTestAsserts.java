package org.decojer.cavaj.test.jdk4;

public class DecTestAsserts {

	public void testAssert(final long l1, final long l2) {
		assert l1 != 0L;
		assert l1 != l2;
		assert false;
		assert true;
	}

	public void testMessageAssert(final long l1, final long l2) {
		assert l1 < 0 : l1;
		assert l1 > 0 ? l1 < l2 : l1 > l2 : "complex expression " + (l1 - l2);
	}

}