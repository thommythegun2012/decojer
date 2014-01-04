package org.decojer.cavaj.test.jdk8;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DecTestTypeAnnotations<U, @Nonnull V extends @Size(min = 1, max = 11) String>
		extends
		@Nonnull HashMap<@Nonnull U, @Size(min = 1, max = 10) @Nonnull String>
		implements @Nonnull Serializable {

	public static @Deprecated
	Map<Short, @Nonnull @Size(max = 2) String> testStatic;

	public static String test() {
		CharSequence str = "test";
		return (@Nonnull String) str;
	}

	public static @Nonnull
	String testParam(@Nonnull @Size(max = 5) CharSequence str) {
		return (@Nonnull @Size(min = 1, max = 10) String) str;
	}

	public static <U, @Nonnull @Size(max = 1) V extends @Size(max = 2) @Nonnull String, @Nonnull W> @Nonnull @Size(max = 3) V testTypeParam(
			final @Nonnull U muh) {
		@Size(max = 4)
		@Nonnull
		CharSequence str = "test";
		return (@Nonnull @Size(max = 5) V) str;
	}

	public @Deprecated
	Map<Short, @Nonnull @Size(max = 2) String> test;

	public void testThrows() throws @Nonnull RuntimeException {
		System.out.println("TEST");
	}

	public void testTryCatch() {
		try {
			System.out.println("TEST");
		} catch (final @Size(max = 2) Exception e) {
			System.out.println("CATCH");
		}
	}

}