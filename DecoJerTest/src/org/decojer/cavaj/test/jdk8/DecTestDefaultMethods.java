package org.decojer.cavaj.test.jdk8;

public interface DecTestDefaultMethods {

	default String test() {
		return "test";
	}

	default int test(int a, int b) {
		return a + b;
	}

	int test(int a, short b);

}