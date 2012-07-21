package org.decojer.cavaj.test.jdk3;

public abstract class DecTestInner {

	public class Inner1 {

		protected final class Inner11 {

			public String test;

			public Inner11(final Inner1 inner1) {
				System.out.println(DecTestInner.this.test);
				System.out.println(Inner1.this.test);
			}

			private void test() {
				System.out.println(this.test);
				System.out.println(Inner1.this.test);
			}

		}

		public String test;

		public Inner1() {
			Inner11 inner11 = new Inner11(this);
			System.out.println(inner11);
		}

	}

	private static final class Inner2 {

		protected final class Inner21 {

			public String test;

			public Inner21(final Inner2 inner2) {
				// not possible DecTestInner.this.test
				System.out.println(Inner2.this.test);
			}

			private void test() {
				System.out.println(this.test);
				System.out.println(Inner2.this.test);
			}

		}

		public String test;

		public Inner2() {
			Inner21 inner21 = new Inner21(this);
			System.out.println(inner21);
		}

	}

	public String test;

	private static Runnable RUNNER = new Runnable() {

		private final Runnable RUNNER = new Thread() {

			// sttaic not possible in non-static inner
			private final Runnable RUNNER = new Thread() {

				public void run() {
					// not in 1.2
					System.out.println(this);
				}

			};

			public void run() {
				// not in 1.2
				System.out.println(Inner2.Inner21.class);
			}

		};

		public void run() {
			System.out.println(this);
		}

	};

	public DecTestInner() {
		final Runnable RUNNER = new Runnable() {

			public void run() {
				System.out.println(this);
			}

		};
		final Object emptyObject = new Object() {

		};
		final Object overrideMdObject = new Object() {

			public String toString() {
				return super.toString() + " TEST";
			}

		};
		final Object addMdObject = new Object() {

			int[] test = new int[] { 1 };

			public String test() {
				return "TEST";
			}

		};
	}

	public void testInnerAnonymous() {
		new Runnable() {

			public void run() {
				System.out.println("INNER RUN");
			}

		}.run();
	}

}

class DecTestInnerS {

	public static class Inner1 {

		protected static final class Inner11 {

			Object o2 = new Object() {

				// not in 1.2
				class AInner {
				}

			};

			public Inner11(final Inner1 inner1) {
				System.out.println(inner1);
				System.out.println(Inner11.class);
			}

			void test(final int a) {

				class AInner {

					void test() {
						System.out.println("a=" + a);
						// not in 1.2
						System.out.println(DecTestInner.Inner1.Inner11.class);
					}

				}

			}

			void test2(final int a) {

				class AInner {

					void test() {
						System.out.println("a=" + a);
						// not in 1.2
						System.out.println(DecTestInner.Inner1.Inner11.class);
					}

				}

				class AInner2 {

					void test() {
						System.out.println("a=" + a);
						// not in 1.2
						System.out.println(DecTestInner.Inner1.Inner11.class);
					}

				}

			}

		}

	}

}