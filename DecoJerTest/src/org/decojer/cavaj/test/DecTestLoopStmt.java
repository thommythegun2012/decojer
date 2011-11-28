package org.decojer.cavaj.test;

public abstract class DecTestLoopStmt {

	// from org.eclipse.jdt.core.compiler.CharOperation
	public static final boolean contains(char character, char[][] array) {
		for (int i = array.length; --i >= 0;) {
			char[] subarray = array[i];
			for (int j = subarray.length; --j >= 0;)
				if (subarray[j] == character)
					return true;
		}
		return false;
	}

	public static void testFor(final int a) {
		for (int i = 0, j = 1; i < 10 && j < 10; i /= 2, j++) {
			System.out.println(i += a);
		}
	}

	public static void testHeadTailBreak(final int a, final int b) {
		// following 2 loops have identical CFGs if we ignore goto basic blocks
		do {
			if (a > b) {
				break;
			}
			System.out.println("TEST");
		} while (a > b);
		System.out.println("NEXT");

		while (a <= b) {
			System.out.println("TEST");
			if (a <= b) {
				break;
			}
		}
		System.out.println("NEXT");

		do {
			if (a > b) {
				break;
			}
			System.out.println("TEST");
			if (a <= b) {
				break;
			}
		} while (true);
		System.out.println("NEXT");
	}

	public static void testIfBreakElseReturn(final int a, final int b) {
		do {
			System.out.println("START");
			if (a > b) {
				System.out.println("TEST a");
				break;
			} else {
				return;
			}
		} while (true);
		System.out.println("NEXT");

		do {
			System.out.println("START");
			if (a > b) {
				System.out.println("TEST a");
				break;
			} else {
				System.out.println("TEST b");
				return;
			}
		} while (true);
		System.out.println("NEXT");
	}

	public static void testIfElseBreak(final int a, final int b) {
		do {
			System.out.println("START");
			if (a > b) {
				System.out.println("TEST a");
			} else {
				break;
			}
			System.out.println("TAIL");
		} while (true);
		System.out.println("NEXT");

		do {
			System.out.println("START");
			if (a > b) {
				System.out.println("TEST a");
			} else {
				System.out.println("TEST b");
				break;
			}
			System.out.println("TAIL");
		} while (true);
		System.out.println("NEXT");
	}

	public static void testPost(final int a) {
		int i = 0;
		do {
			System.out.println(i += a);
		} while (i < 10);
	}

	public static void testPostEndless() {
		do {
			System.out.println("TEST");
		} while (true);
	}

	public static void testPre(final int a) {
		int i = 0;
		while (i < 10) {
			System.out.println(i += a);
		}
	}

	public static void testPreEndless() {
		while (true) {
			System.out.println("TEST");
		}
	}

}