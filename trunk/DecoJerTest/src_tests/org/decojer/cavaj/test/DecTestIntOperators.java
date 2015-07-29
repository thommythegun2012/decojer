package org.decojer.cavaj.test;

public abstract class DecTestIntOperators {

	public static void test(final int a, final int b, final int c) {
		System.out.println(~a + b);
		System.out.println((a ^ -1) + b); // + precedence over ^
		System.out.println(a ^ -1 + b);
		System.out.println(~(a + b));
		System.out.println(a + b ^ -1);

		System.out.println(" --");
		System.out.println(-a - b - c);
		System.out.println(-(a - b) - c);
		System.out.println(-a - (b - c));
		System.out.println(-(a - b - c));

		System.out.println(" ~&");
		System.out.println(~a & b & c);
		System.out.println(~(a & b) & c);
		System.out.println(~(a & b & c));

		System.out.println(" ~^");
		System.out.println(~a ^ b ^ c);
		System.out.println(~(a ^ b) ^ c);
		System.out.println(~(a ^ b ^ c));

		System.out.println(" ~|");
		System.out.println(~a | b | c);
		System.out.println(~(a | b) | c);
		System.out.println(~(a | b | c));

		System.out.println(" -~+");
		System.out.println(-~a - ~-a);
		System.out.println(+~a - ~+a);
		System.out.println(+-a - -+a);
	}

}