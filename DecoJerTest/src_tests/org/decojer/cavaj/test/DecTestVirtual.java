package org.decojer.cavaj.test;

public class DecTestVirtual extends DecTestFields {

	public DecTestVirtual() {
		this(1);
	}

	public DecTestVirtual(int i) {
		super(i);
	}

	public void test() {
		// getfield org.decojer.cavaj.test.DecTestVirtual.charTest
		System.out.println("TEST: " + charTest);
		// invokevirtual org.decojer.cavaj.test.DecTestVirtual.getCharTest() :
		// char
		System.out.println("TEST: " + getCharTest());
	}

}