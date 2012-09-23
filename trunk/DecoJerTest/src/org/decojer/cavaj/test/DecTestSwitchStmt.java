package org.decojer.cavaj.test;

public abstract class DecTestSwitchStmt {

	public static void testDefaultIfBreak(final int a) {
		switch (a) {
		default:
			if (a > 10) {
				break;
			}
		case 3000:
			System.out.println(3000);
		case 3001:
			System.out.println(3001);
		}
		System.out.println("END SWITCH");
	}

	public static void testLookup(final int a) {
		switch (a) {
		case 0:
		case 1:
			System.out.println(1);
			break;
		case -2:
			System.out.println(-1);
			break;
		case 2:
			System.out.println(2);
		case 3000:
			System.out.println(3000);
		}
	}

	public static void testLookupDefault(final int a) {
		switch (a) {
		case 1:
			System.out.println(1);
			break;
		case -2:
			System.out.println(-1);
			break;
		case 2:
			System.out.println(2);
		case 3000:
			System.out.println(3000);
			break;
		case 4000:
		default:
			System.out.println("TEST");
		}
	}

	public static void testTable(final int a) {
		switch (a) {
		case 0:
		case 1:
			System.out.println(1);
			break;
		case -2:
			System.out.println(-1);
			break;
		case 2:
			System.out.println(2);
		case 3:
			System.out.println(3);
			break;
		}
	}

	public static void testTableDefaultBreak(final int a) {
		muh: {
			switch (a) {
			case 1:
				System.out.println(1);
			case 2:
				System.out.println(2);
				break;
			case 4:
			case 5:
				System.out.println(3);
				break;
			default:
				System.out.println("TEST");
				break muh;
			}
			System.out.println("TEST END");
		}
		System.out.println("TEST OUTER END");
	}

}