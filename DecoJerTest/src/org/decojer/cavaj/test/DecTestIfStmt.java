package org.decojer.cavaj.test;

public abstract class DecTestIfStmt {

	public static void ifConditionalAndCompound(boolean a, boolean b,
			boolean c, int d) {
		if (a ? b : c)
			System.out.println("T1");
		if (a || b ? c : d > 2)
			System.out.println("T2");
		if (a && b ? c : d > 2)
			System.out.println("T3");
		if (b ? c : d > 2 || a)
			System.out.println("T4");
		if (a ? d > 2 : d + 1 > 3)
			System.out.println("T5");
		// not flat graph in >= JDK 1.4 code...optimization
		if (a || (b ? c : d > 2))
			System.out.println("T6");
		if (a && (b ? c : d > 2))
			System.out.println("T7");
		if (a && (b ? c && d < 1 : c || d > 2))
			System.out.println("T8");
	}

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