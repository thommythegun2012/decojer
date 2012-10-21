package org.decojer.cavaj.test;

public abstract class DecTestBooleanOperators {

	public static void conditionalCompound(final boolean a, final boolean b,
			final boolean c, final boolean d, final boolean e) {
		// creates none-flat forward-CFGs since JDK 4
		System.out.println(a && d ? b : c && e);
		System.out.println(a && (d ? b : c) && e);
		System.out.println(a || d ? b : c || e);
		System.out.println(a || (d ? b : c) || e);

		System.out.println(a ? b && d : c || e);
		System.out.println(!a ? b || d : c && e);
		System.out.println(a ? b || d && e : c);
		System.out.println(!a ? b : c || d && e);

		System.out.println(a && d ? b && e : c || e);
		System.out.println(a || d ? b || e : c && e);

		System.out.println(a && d ? true : c || e);
		System.out.println(a && d ? b && e : false);
		System.out.println(a || d ? false : c && e);
		System.out.println(a || d ? b || e : true);

		System.out.println(a && (d ? true : c || e));
		System.out.println(a && (d ? b && e : false));
		System.out.println(a || (d ? false : c && e));
		System.out.println(a || (d ? b || e : true));

		System.out.println(a && d ? a || e ? b && e : c || e : !b && c || e);
	}

	public static void conditionalCompoundValue(final boolean a,
			final boolean b, final boolean c, final boolean d) {
		System.out.println(a ? 1 : 2);

		System.out.println(a && (d ? b : c) ? 1 : 2);
		// System.out.println(a && d ? b : c ? 1 : 2); // Bug in < JDK 1.4.0_19
		System.out.println(a && (d ? true : c) ? 1 : 2);
		// System.out.println(a && d ? true : c ? 1 : 2);
		System.out.println(a && (d ? b : false) ? 1 : 2);
		// System.out.println(a && d ? b : false ? 1 : 2);

		System.out.println(a && d ? b || d ? c && !d ? 1.0D : 2.0D : 3.0D
				: 4.0D);
	}

	public static void shortCircuitCompound(final boolean a, final boolean b,
			final boolean c) {
		System.out.println(a && b && c);
		System.out.println(a && !b && c);
		System.out.println(!(a && b && c));
		System.out.println(a && b || c);
		System.out.println(a && (b || c));
		System.out.println(a && !(b || c));

		System.out.println(a || b && c);
		System.out.println((a || b) && c);
		System.out.println(!(a || b) && c);
		System.out.println(a || b || c);
		System.out.println(a || !b || c);
		System.out.println(!(a || b || c));
	}

}