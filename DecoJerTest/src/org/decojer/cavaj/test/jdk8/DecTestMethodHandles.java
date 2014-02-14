package org.decojer.cavaj.test.jdk8;

import static org.decojer.cavaj.test.jdk8.DecTestLambdas.test;

public class DecTestMethodHandles {

	void test() {
		Runnable o = DecTestLambdas::new;
		o = DecTestLambdas::test;

		DecTestLambdas l = new DecTestLambdas();
		o = l::testThread;
	}

}