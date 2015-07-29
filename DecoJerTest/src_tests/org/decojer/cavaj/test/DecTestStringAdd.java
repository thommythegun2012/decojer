package org.decojer.cavaj.test;

public class DecTestStringAdd {

	void test(String s, int i, double d) {
		System.out.println(i);
		System.out.println("test" + i);
		System.out.println("test" + i + d);
		System.out.println("test" + i * d);
		System.out.println("test" + (i + d));
		System.out.println("" + i);
		System.out.println("" + i + d);
		System.out.println("" + i * d);
		System.out.println("" + (i + d));
		System.out.println(s + i);
		System.out.println(s + i + d);
		System.out.println(s + i * d);
		System.out.println(s + (i + d));
		System.out.println(i + "");
		System.out.println(i + d + "");
		System.out.println(i * d + "");
		System.out.println(i + s);
		System.out.println(i + d + s);
		System.out.println(i * d + s);
	}

	void testComplex(String s, boolean b, int i, char c, long l) {
		System.out.println(s + b + i + c + l);
		System.out.println(s + "test" + l);
		System.out.println(b + "test2" + c);
		System.out.println(i + l + "test2" + i + l);
		System.out.println(i * l + "test2" + i * l);
		System.out.println(i * l + "test2" + (i + l));

		System.out.println("" + (b ? i > l && l > -1 : i < l || i < 0));
		System.out.println((i < l) + "test" + (i > l));
		System.out.println((i < l && b) + "test" + (b || i > l));
	}

}