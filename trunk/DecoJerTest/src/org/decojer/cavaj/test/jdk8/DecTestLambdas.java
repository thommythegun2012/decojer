package org.decojer.cavaj.test.jdk8;

public abstract class DecTestLambdas {

	interface Test {
		String hello(String firstname, String lastname);
	}

	public static void test() {
		final Test test = (String firstname, String lastname) -> {
			return "Hello " + firstname + " " + lastname;
		};
		System.out.println(test.hello("André", "Pankraz"));

	}

	public static void testCapture() {
		final String greetings = "Hello";
		final String space = " ";
		final Test test = (String firstname, String lastname) -> {
			return greetings + space + firstname + space + lastname;
		};
		System.out.println(test.hello("André", "Pankraz"));
	}

	public static void thread() {
		new Thread(() -> {
			System.out.println("Hello from a thread");
		}).start();
	}

}