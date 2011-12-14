package org.decojer.cavaj.test;

public abstract class DecTestExceptions {

	boolean breakNestedFinallyExceptionFinally(int a) {
		System.out.println("PRE");
		out: try {
			System.out.println("TRY");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} finally {
			System.out.println("FIN");
			in: try {
				System.out.println("IN_TRY");
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
				if (a > 10) {
					System.out.println("IN_ERR_BREAK_OUT");
					break out;
				}
				if (a > 9) {
					break out;
				}
				if (a > 5) {
					System.out.println("IN_ERR_BREAK");
					break in;
				}
				if (a > 0) {
					System.out.println("IN_ERR_RET");
					return true;
				}
				System.out.println("IN_ERR_TAIL");
			} finally {
				System.out.println("IN_FIN");
				if (a > 10) {
					System.out.println("IN_FIN_BREAK_OUT");
					break out;
				}
				if (a > 9) {
					break out;
				}
				if (a > 5) {
					System.out.println("IN_FIN_BREAK");
					break in;
				}
				if (a > 0) {
					System.out.println("IN_FIN_RET");
					return true;
				}
				System.out.println("IN_FIN_TAIL");
			}
			System.out.println("FIN_TAIL");
		}
		System.out.println("POST");
		return false;
	}

	boolean breakNestedTryExceptionFinally(int a) {
		System.out.println("PRE");
		out: try {
			System.out.println("TRY");
			in: try {
				System.out.println("IN_TRY");
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
				if (a > 10) {
					System.out.println("IN_ERR_BREAK_OUT");
					break out;
				}
				if (a > 9) {
					break out;
				}
				if (a > 5) {
					System.out.println("IN_ERR_BREAK");
					break in;
				}
				if (a > 0) {
					System.out.println("IN_ERR_RET");
					return true;
				}
				System.out.println("IN_ERR_TAIL");
			} finally {
				System.out.println("IN_FIN");
				if (a > 10) {
					System.out.println("IN_FIN_BREAK_OUT");
					break out;
				}
				if (a > 9) {
					break out;
				}
				if (a > 5) {
					System.out.println("IN_FIN_BREAK");
					break in;
				}
				if (a > 0) {
					System.out.println("IN_FIN_RET");
					return true;
				}
				System.out.println("IN_FIN_TAIL");
			}
			System.out.println("TRY_TAIL");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
		return false;
	}

	boolean breakTrySimpleException(int a) {
		System.out.println("PRE");
		out: try {
			System.out.println("TRY");
			if (a > 10) {
				System.out.println("TRY_BREAK");
				break out;
			}
			if (a > 9) {
				break out;
			}
			if (a > 0) {
				System.out.println("TRY_RET");
				return true;
			}
			System.out.println("TRY_TAIL");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		}
		System.out.println("POST");
		return false;
	}

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

	void nestedTryException() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
			try {
				System.out.println("IN_TRY");
			} catch (final Exception e) {
				System.out.println("IN_EXC" + e);
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
			}
			System.out.println("TRY_TAIL");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		}
		System.out.println("POST");
	}

	void nestedTryExceptionFinally() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
			try {
				System.out.println("IN_TRY");
			} catch (final Exception e) {
				System.out.println("IN_EXC" + e);
			} catch (final Error e) {
				System.out.println("IN_ERR" + e);
			} finally {
				System.out.println("IN_FIN");
			}
			System.out.println("TRY_TAIL");
		} catch (final Exception e) {
			System.out.println("EXC" + e);
		} catch (final Error e) {
			System.out.println("ERR" + e);
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
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

	void simpleNestedFinallyFinally() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
		} finally {
			try {
				System.out.println("FIN");
			} finally {
				System.out.println("IN_FINALLY");
			}
		}
		System.out.println("POST");
	}

	void simpleNestedTryFinally() {
		System.out.println("PRE");
		try {
			System.out.println("TRY");
			try {
				System.out.println("IN_TRY");
			} finally {
				System.out.println("IN_FINALLY");
			}
			System.out.println("TRY_TAIL");
		} finally {
			System.out.println("FIN");
		}
		System.out.println("POST");
	}

}