package org.decojer.cavaj.test.jdk5;

import java.util.AbstractList;
import java.util.List;

import org.decojer.cavaj.test.jdk5.DecTestInner.Inner1.Inner11;

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

	protected static class Inner2 {

		protected static final class Inner21 {

			// static classes only in static or top-level outer classes, enums
			// are static
			enum InnerEnum {

				TEST

			}

			public Inner21(final Inner2 innerO2,
					final DecTestInner.Inner2 inner2) {
				System.out.println("INNER21 CONSTRUCTOR " + Inner1.class
						+ innerO2 + inner2 + RUNNER.getClass());
			}

		}

		public final class Inner22 {

			Inner22() {
				System.out.println("INNER22 CONSTRUCTOR " + InnerEnum.TEST);
			}

		}

		public Inner2() {
			System.out.println("INNER2 CONSTRUCTOR " + Inner1.class);
		}

	}

	enum InnerEnum {

		TEST

	}

	// inner has no this$0 because static field, but class is not static
	private static Runnable RUNNER = new Runnable() {

		// cannot contain static fields because anonymous classes not static
		private final List<Runnable> RUNNER = new AbstractList<Runnable>() {

			private final Runnable RUNNER = new Thread() {

				private final Runnable RUNNER = new Thread() {

					public void run() {
						System.out.println("INNER RUNNER" + Inner11.class
								+ InnerEnum.TEST);
					}

				};

				public void run() {
					System.out.println("INNER RUNNER" + Inner11.class);
				}

			};

			public Runnable get(final int index) {
				System.out.println("INNER RUNNER");
				return null;
			}

			public int size() {
				System.out.println("INNER RUNNER");
				return 0;
			};

		};

		public void run() {
			System.out.println("INNER RUNNER");
		}

	};

	public DecTestInner() {
		final Runnable RUNNER = new Runnable() {

			public void run() {
				System.out.println("INNER RUNNER" + Inner2.Inner22.class);
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

	public class Inner1 {

		protected final class Inner11 {

			public Inner11(final Inner1 inner1) {
				System.out.println("InnerSInner1" + DecTestInner.Inner2.class);
			}

		}

	}

}