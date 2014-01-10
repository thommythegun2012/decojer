package org.decojer.cavaj.test.jdk8;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecTestTypeAnnotations<U, @Nonnull V extends @Nonnull @Size(min = 1, max = 10) HashMap<String, @Nonnull @Size(max = 11) Integer> & @Size(max = 12) @Nonnull CharSequence>
		extends
		@Nonnull HashMap<@Nonnull U, @Size(max = 13) @Nonnull List<List<@Nonnull List<String>>>>
		implements @Nonnull @Size(max = 14) Serializable {

	public static @Deprecated
	@Size(max = 15)
	Map<Short, @Nonnull @Size(max = 16) String> testStatic;

	public static String testArrays() {
		CharSequence[] @Size(max = 17) [] @Nonnull @Size(max = 18) [][] test = new CharSequence @Nonnull [10][][] @Nonnull [];
		return (@Nonnull String) test[0][1][2][3];
	}

	public static @Nonnull
	String testParam(@Nonnull @Size(max = 19) CharSequence str) {
		return (@Nonnull @Size(min = 1, max = 17) String) str;
	}

	public static <U, @Nonnull @Size(max = 20) V extends @Size(max = 21) @Nonnull String, @Nonnull W> @Nonnull @Size(max = 22) V testTypeParam(
			final @Nonnull U muh) {
		@Size(max = 23)
		@Nonnull
		CharSequence str = "test";
		return (@Nonnull @Size(max = 24) V) str;
	}

	public @Deprecated
	@Size(max = 25)
	Map<Short, @Nonnull @Size(max = 26) String> test;

	public void testThrows() throws IOException, @Nonnull RuntimeException,
			@Size(max = 27) @Nonnull NullPointerException {
		System.out.println("TEST");
	}

	public void testTryCatch() {
		try {
			System.out.println("TEST");
		} catch (final @Size(max = 28) NullPointerException e) {
			System.out.println("CATCH");
		} catch (final @Size(max = 29) RuntimeException
				| @Size(max = 30) @Nonnull Error e) {
			System.out.println("MULTI_CATCH");
		}
	}

}