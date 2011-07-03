package org.decojer.cavaj.test;

public abstract class DecTestIfStmt {

	public static void testIf(final int a, final int b) {
		if (a > b) {
			System.out.println("TEST a");
		}
		System.out.println("TEST END");
	}

	public static void testIfBreak(final int a, final int b) {
		muh: if (a > b) {
			System.out.println("TEST a");
			break muh;
		}
		System.out.println("TEST END");
	}

	public static void testIfBreakOuter(final int a, final int b) {
		muh: if (a > b) {
			System.out.println("TEST a");
			if (2 * a > b) {
				System.out.println("TEST 2a");
				break muh;
			}
			System.out.println("TEST END");
		}
		System.out.println("TEST OUTER END");
		muh: {
			if (a > b) {
				System.out.println("TEST a");
				if (2 * a > b) {
					System.out.println("TEST 2a");
					break muh;
				}
				System.out.println("TEST 3a");
			}
			System.out.println("TEST END");
		}
		System.out.println("TEST OUTER END");
	}

	public static void testIfDoubleBoolean(final boolean b) {
		if (b) {
			System.out.println("TEST b");
		}
		if (b) {
			System.out.println("TEST b");
		}
		System.out.println("TEST END");
	}

	public static void testIfElse(final int a, final int b) {
		if (a > b) {
			System.out.println("TEST a");
		} else {
			System.out.println("TEST b");
		}
		System.out.println("TEST END");
	}

	public static void testIfElseBreak(final int a, final int b) {
		muh: if (a > b) {
			System.out.println("TEST a");
			break muh;
		} else {
			System.out.println("TEST b");
			break muh;
		}
		System.out.println("TEST END");
	}

	public static void testIfElseBreakOuter(final int a, final int b) {
		muh: {
			if (a > b) {
				System.out.println("TEST a");
				break muh;
			} else {
				System.out.println("TEST b");
				if (2 * a > b) {
					break muh;
				}
				System.out.println("TEST 2b");
			}
			System.out.println("TEST END");
		}
		System.out.println("TEST OUTER END");
	}

}