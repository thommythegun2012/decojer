package org.decojer.cavaj.test.jdk2;

import org.decojer.cavaj.test.jdk2.DecTestInner.Inner1.Inner11;

public abstract class DecTestInner {

	public class Inner1 {

		protected final class Inner11 {

			public Inner11(final Inner1 inner1) {
				System.out.println("INNER11 CONSTRUCTOR " + Inner1.class
						+ inner1 + RUNNER.getClass() + DecTestInnerS.class
						+ DecTestInnerS.Inner1.class);
			}

		}

		public Inner1() {
			System.out.println("INNER1 CONSTRUCTOR " + Inner11.class);
		}

	}

	protected class Inner2 {

		protected final class Inner11 {

			public Inner11(final Inner2 innerO2,
					final DecTestInner.Inner2 inner2) {
				System.out.println("INNER11 CONSTRUCTOR " + Inner1.class
						+ innerO2 + inner2 + RUNNER.getClass());
			}

		}

		public Inner2() {
			System.out.println("INNER2 CONSTRUCTOR " + Inner3.class);
		}

	}

	private static final class Inner3 {

		public Inner3() {
			System.out.println("INNER3 CONSTRUCTOR " + Inner1.Inner11.class);
		}

		Inner2.Inner11 test() {
			return null;
		}

	}

	private static Runnable RUNNER = new Runnable() {

		private final Runnable RUNNER = new Thread() {

			private final Runnable RUNNER = new Thread() {

				public void run() {
					System.out.println("INNER RUNNER" + Inner11.class);
				}

			};

			public void run() {
				System.out.println("INNER RUNNER" + Inner11.class);
			}

		};

		public void run() {
			System.out.println("INNER RUNNER");
		}

	};

	public DecTestInner() {
		final Runnable RUNNER = new Runnable() {

			public void run() {
				System.out.println("INNER RUNNER" + Inner2.Inner11.class);
			}

		};
	}

	public void testInnerAnonymous() {
		new Runnable() {

			class Test {

				void test() {
					run();
				}

			}

			public void run() {
				System.out.println("INNER RUN");
			}

		}.run();
	}

}

class DecTestInnerS {

	public static class Inner1 {

		protected static final class Inner11 {

			public Inner11(final Inner1 inner1) {
				System.out.println("InnerSInner1" + DecTestInner.Inner2.class);
			}

			public void innerMethod(final int a, final int b) {
				// final (and order var->class->instantiation) is important
				class MethodInner {

					public MethodInner(int c) {
						// public
						// DecTestInnerS$Inner1$Inner11$1MethodInner(org.decojer.cavaj.test.DecTestInnerS.Inner1.Inner11
						// arg0, int c, int arg2, int arg3);
						// constructor:
						// none-static method: additional leading Inner11.this
						// (cached as - $2 is inner level:
						// final synthetic...InnerS$Inner1$Inner11 this$2;)
						// used final outers: additional attached arguments
						// (used directly in constructor or cached as:
						// private final synthetic int val$a;)
						// without debug-info may be undecideable what is real
						// argument and what is compile-time added (if final
						// only used in constructor)
						System.out.println("MethodInner: " + (a + b + c));
					}

					private void syso() {
						System.out.println("Syso: " + a);
					}

				}
				// MethodInner.EnclosingMethod is set in each case,
				// Inner11.InnerClasses doesn't show enclosing method name
				new MethodInner(1);
			}

		}

	}

}