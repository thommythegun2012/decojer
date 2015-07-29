package org.decojer.cavaj.test.jdk8;

import java.io.Serializable;

public class DecTestLambdas {

	interface Test {
		String hello(String firstname, String lastname);
	}

	interface TestVarargs {
		String hello(String firstname, String... lastname);
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

	public static void testInner() {
		final TestVarargs test = (final @Deprecated @Nonnull String firstname, String... lastname) -> {

			class Test implements Serializable {

				private static final long serialVersionUID = 6737746337135796923L;

				private String greetings = "Hello";

				public String getGreetings() {
					return greetings;
				}

			}

			Test greetings = new Test();

			return greetings.getGreetings() + " " + firstname + " " + lastname[0];
		};
		System.out.println(test.hello("André", "Pankraz", "TestVararg"));

	}

	public void testThread() {
		new Thread(() -> {
			System.out.println("Hello from a thread");
		}).start();
	}

}