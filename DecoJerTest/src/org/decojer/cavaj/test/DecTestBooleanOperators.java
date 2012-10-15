package org.decojer.cavaj.test;

public abstract class DecTestBooleanOperators {

	public static void compound(final boolean a, final boolean b,
			final boolean c) {
		System.out.println(" !&&");
		System.out.println(!a && b && c);
		System.out.println(!(a && b) && c);
		System.out.println(!(a && b && c));
		System.out.println(" !||");
		System.out.println(!a || b || c);
		System.out.println(!(a || b) || c);
		System.out.println(!(a || b || c));
	}

	public void conditionalAndCompound(boolean a, boolean b, boolean c, int d) {
		System.out.println("T1: " + (a ? b : c));
		System.out.println("T2: " + (a || b ? c : d > 2));
		System.out.println("T3: " + (a && b ? c : d > 2));
		System.out.println("T4: " + ((b ? c : d > 2 || a)));
		System.out.println("T5: " + (a ? d > 2 : d + 1 > 3));
		// not flat graph in >= JDK 1.4 code...optimization
		System.out.println("T6: " + (a || (b ? c : d > 2)));
		System.out.println("T7: " + (a && (b ? c : d > 2)));
		System.out.println("T8: "
				+ (a && (b ? c && d < 1 : c || d > 2) ? 1L : 2D));
	}

}