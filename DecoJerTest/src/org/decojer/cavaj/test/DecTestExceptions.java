package org.decojer.cavaj.test;

public abstract class DecTestExceptions {

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

	void innerException() {
		System.out.println("PRE");
		try {
			try {
				System.out.println("IN");
			} catch (final RuntimeException e) {
				e.printStackTrace();
			} catch (final OutOfMemoryError e) {
				e.printStackTrace();
			}
		} catch (final RuntimeException e) {
			System.out.println("ERR: " + e);
			e.printStackTrace();
		}
		System.out.println("POST");
	}

	void simpleException() {
		System.out.println("PRE");
		try {
			System.out.println("IN");
		} catch (final Exception e) {
			e.printStackTrace();
		}
		System.out.println("POST");
	}

	void simpleFinally() {
		System.out.println("PRE");
		try {
			System.out.println("IN");
		} finally {
			System.out.println("FINALLY");
		}
		System.out.println("POST");
	}

}