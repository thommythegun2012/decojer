package org.decojer.cavaj.test.jdk8;

import static org.decojer.cavaj.test.jdk8.DecTestDefaultMethods.compareByLength;

import java.util.Arrays;
import java.util.Comparator;

public class DecTestMethodHandles extends DecTestLambdas {

	static void testInterfaceHandles() {
		Comparator<@Nonnull String> c = @Nonnull DecTestDefaultMethods::compareByLength;
		Arrays.sort(new String[] { "1", "2" }, c);

		Arrays.sort(new @Nonnull String[] { "1", "2" }, DecTestDefaultMethods::compareByLength);
	}

	static void testLocalHandles() {
		DecTestMethodHandles decTestMethodHandles = new DecTestMethodHandles();
		Runnable r = decTestMethodHandles::testSimpleHandles;
		// direct:
		r = new @Nonnull DecTestMethodHandles()::testSimpleHandles;
	}

	void testSimpleHandles() {
		Runnable r = @Nonnull DecTestLambdas::new;
		r = @Nonnull DecTestLambdas::testCapture;
		r = new @Nonnull DecTestLambdas()::testThread;
		r = super::testThread;
	}

}