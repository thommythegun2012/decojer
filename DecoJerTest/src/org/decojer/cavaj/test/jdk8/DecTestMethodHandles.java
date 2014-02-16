package org.decojer.cavaj.test.jdk8;

import static org.decojer.cavaj.test.jdk8.DecTestDefaultMethods.compareByLength;
import static org.decojer.cavaj.test.jdk8.DecTestLambdas.testCapture;

import java.util.Arrays;
import java.util.Comparator;

public class DecTestMethodHandles {

	static class Test<S> extends DecTestMethodHandles {

		public static <T> void testStatic() {
			System.out.println("test");
		}

		public <T> Test() {
			System.out.println("Contstructor");
		}

		public <T> void test() {
			System.out.println("test");
		}

		void testInnerGenericHandles() {
			Runnable r = @Nonnull Test<@Nonnull String>::<@Nonnull Integer> new;
			r = @Nonnull Test::<@Nonnull Integer> testStatic;
			r = new @Nonnull Test<@Nonnull String>()::<@Nonnull Integer> test;
			r = super::<@Nonnull String> testSimpleHandles;
		}

	}

	static void testInterfaceHandles() {
		Comparator<@Nonnull String> c = @Nonnull DecTestDefaultMethods::compareByLength;
		Arrays.sort(new String[] { "1", "2" }, c);

		Arrays.sort(new @Nonnull String[] { "1", "2" },
				DecTestDefaultMethods::<@Nonnull String> compareByLength);
	}

	static void testLocalHandles() {
		DecTestMethodHandles decTestMethodHandles = new DecTestMethodHandles();
		Runnable r = decTestMethodHandles::testSimpleHandles;
		// direct:
		r = new @Nonnull DecTestMethodHandles()::testSimpleHandles;
	}

	<T> void testSimpleHandles() {
		Runnable r = @Nonnull DecTestLambdas::new;
		r = @Nonnull DecTestLambdas::testCapture;
		r = new @Nonnull DecTestLambdas()::testThread;
	}

}