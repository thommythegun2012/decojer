package org.decojer.cavaj.test;

public abstract class DecTestExceptions {

	void exception() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		}
		System.out.println("POST");
	}

	void exceptionFinally() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		} finally {
			System.out.println("FINALLY");
		}
		System.out.println("POST");
	}

	void innerException() {
		System.out.println("PRE");
		try {
			try {
				System.out.println("TRY");
			} catch (final Exception e) {
				System.out.println("IN_EXC" + e);
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
			}
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		}
		System.out.println("POST");
	}

	void innerFinally() {
		System.out.println("PRE");
		try {
			try {
				System.out.println("TRY");
			} catch (final Exception e) {
				System.out.println("IN_EXC" + e);
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
			} finally {
				System.out.println("IN_FIN");
			}
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
	}

	void innerSimpleFinally() {
		System.out.println("PRE");
		try {
			try {
				System.out.println("TRY");
			} finally {
				System.out.println("IN_FINALLY");
			}
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
	}

	boolean returnInInnerFinally(int a) {
		System.out.println("PRE");
		out: try {
			try {
				if (a > -1) {
					return false;
				}
				if (a > 1) {
					System.out.println("INIF");
					return true;
				}
				System.out.println("TRY");
				return a > 10;
			} catch (final Exception e) {
				System.out.println("IN_EXC" + e);
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
			} finally {
				System.out.println("IN_FIN");
				if (a > 0)
					break out;
			}
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
		return false;
	}

	boolean returnInSimpleException(int a) {
		System.out.println("PRE");
		try {
			if (a > -1) {
				return false;
			}
			if (a > 1) {
				System.out.println("INIF");
				return true;
			}
			System.out.println("TRY");
			return a > 10;
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		}
		System.out.println("POST");
		return false;
	}

	void simpleException() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		}
		System.out.println("POST");
	}

	void simpleFinally() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
	}

}