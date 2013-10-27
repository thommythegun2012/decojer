package org.decojer.cavaj.test.jdk8;

public abstract class DecTestLambdas {

	public static void thread() {
		new Thread(() -> {
			System.out.println("Hello from a thread");
		}).start();
	}

}