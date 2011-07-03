package org.decojer.cavaj.test;

public abstract class DecTestExceptions {

	void _finally() {
		System.out.println("PRE");
		try {
			System.out.println("IN");
		} finally {
			System.out.println("FINALLY");
		}
		System.out.println("POST");
	}

	void exception() {
		System.out.println("PRE");
		try {
			System.out.println("IN");
		} catch (final Exception e) {
			e.printStackTrace();
		}
		System.out.println("POST");
	}

	void exceptionFinally() {
		System.out.println("PRE");
		try {
			System.out.println("IN");
		} catch (final Exception e) {
			System.out.println("EXC: " + e);
			e.printStackTrace();
		} catch (final Error e) {
			System.out.println("ERR: " + e);
			e.printStackTrace();
		} finally {
			System.out.println("FINALLY");
		}
		System.out.println("POST");
	}

}