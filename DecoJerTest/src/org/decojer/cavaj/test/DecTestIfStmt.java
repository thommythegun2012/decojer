package org.decojer.cavaj.test;

public abstract class DecTestIfStmt {

	public static void ifConditionalCompound(final boolean a, final boolean b,
			final boolean c, final boolean d, final boolean e) {
		if (a && d ? b : c && e)
			System.out.println("T1");
		if (a && (d ? b : c) && e)
			System.out.println("T2");
		if (a || d ? b : c || e)
			System.out.println("T3");
		if (a || (d ? b : c) || e)
			System.out.println("T4");

		if (a ? b && d : c || e)
			System.out.println("T5");
		if (!a ? b || d : c && e)
			System.out.println("T6");
		if (a ? b || d && e : c)
			System.out.println("T7");
		if (!a ? b : c || d && e)
			System.out.println("T8");

		if (a && d ? b && e : c || e)
			System.out.println("T9");
		if (a || d ? b || e : c && e)
			System.out.println("T10");

		if (a && d ? true : c || e)
			System.out.println("T11");
		if (a && d ? b && e : false)
			System.out.println("T12");
		if (a || d ? false : c && e)
			System.out.println("T13");
		if (a || d ? b || e : true)
			System.out.println("T14");

		if (a && (d ? true : c || e))
			System.out.println("T15");
		if (a && (d ? b && e : false))
			System.out.println("T16");
		if (a || (d ? false : c && e))
			System.out.println("T17");
		if (a || (d ? b || e : true))
			System.out.println("T18");

		if (a && d ? a || e ? b && e : c || e : !b && c || e)
			System.out.println("T19");
	}

	public static void ifShortCircuitCompound(final boolean a, final boolean b,
			final boolean c) {
		if (a && b && c)
			System.out.println("T1");
		if (a && !b && c)
			System.out.println("T2");
		if (!(a && b && c))
			System.out.println("T3");
		if (a && b || c)
			System.out.println("T4");
		if (a && (b || c))
			System.out.println("T5");
		if (a && !(b || c))
			System.out.println("T6");

		if (a || b && c)
			System.out.println("T7");
		if ((a || b) && c)
			System.out.println("T8");
		if (!(a || b) && c)
			System.out.println("T9");
		if (a || b || c)
			System.out.println("T10");
		if (a || !b || c)
			System.out.println("T11");
		if (!(a || b || c))
			System.out.println("T12");
	}

	public static void simpleIf(final int a, final int b) {
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