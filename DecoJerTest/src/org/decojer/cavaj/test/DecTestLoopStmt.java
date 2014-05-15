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

	public static int ifElseForLoops() {
		if (Math.random() > .5) {
			for (int i = 0; i < 10; ++i) {
				System.out.println("TEST: " + i);
				if (i % 5 == 2)
					return i;
			}
		} else {
			for (int i = 10; i-- > 0;) {
				System.out.println("TEST2: " + i);
				if (i % 2 == 5)
					return i;
			}
		}
		return -1;
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
		int j = 0;
		do {
			// nothing
		} while (j++ < 10);
		j = 0;
		do {
			System.out.println(j += a);
			System.out.println("Multiple statements and following stuff");
		} while (j < 10);
		j = 0;
		do {
			System.out.println(j += a);
			System.out.println("Multiple statements");
		} while (j < 10);
	}

	public static void testPostDouble() {
		int a = 0;
		do {
			do {
				// nothing
			} while (a++ % 10 < 5);
			System.out.println("TEST");
		} while (a++ < 100);
		System.out.println("END");
	}

	public static void testPostEndless() {
		do {
			System.out.println("TEST");
		} while (true);
	}

	public static void testPostEndlessMultiple() {
		do {
			System.out.println("TEST");
			System.out.println("TEST Multiple");
		} while (true);
	}

	public static void testPre(final int a) {
		int i = 0;
		while (i < 10) {
			System.out.println(i += a);
		}
		int j = 0;
		while (+j < 10) {
			// nothing
		}
		j = 0;
		while (j < 10) {
			System.out.println(j += a);
			System.out.println("Multiple statements and following stuff");
		}
		j = 0;
		while (j < 10) {
			System.out.println(j += a);
			System.out.println("Multiple statements");
		}
	}

	public static void testPreEndless() {
		while (true) {
			System.out.println("TEST");
		}
	}

	public static void testPreEndlessMultiple() {
		while (true) {
			System.out.println("TEST");
			System.out.println("TEST Multiple");
		}
	}

}