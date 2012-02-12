package org.decojer.cavaj.test;

public class DecTestVirtual extends DecTestFields {

	public void test() {
		// getfield org.decojer.cavaj.test.DecTestVirtual.charTest
		System.out.println("TEST: " + charTest);
		// invokevirtual org.decojer.cavaj.test.DecTestVirtual.getCharTest() :
		// char
		System.out.println("TEST: " + getCharTest());
	}

}