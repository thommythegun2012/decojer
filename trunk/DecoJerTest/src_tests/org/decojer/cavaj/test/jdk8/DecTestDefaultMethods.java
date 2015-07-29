package org.decojer.cavaj.test.jdk8;

public interface DecTestDefaultMethods {

	// static method in interfaces
	public static <S extends String> int compareByLength(S in, S out) {
		return in.length() - out.length();
	}

	default String test() {
		return "test";
	}

	default int test(int a, int b) {
		return a + b;
	}

	int test(int a, short b);

}