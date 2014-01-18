package org.decojer.cavaj.test.jdk8;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecTestTypeAnnotations<U, @Nonnull V extends @Nonnull @Size(max = 1) @Size(min = 1, max = 10) HashMap<String, @Nonnull @Size(max = 11) Integer> & @Size(max = 12) @Nonnull CharSequence>
		extends
		@Nonnull HashMap<@Nonnull U, @Size(max = 13) @Nonnull List<List<@Nonnull List<@Nonnull ? extends @Nonnull String>>>>
		implements @Nonnull @Size(max = 14) Serializable {

	class Outer {

		class Middle {

			class Inner extends @Nonnull Middle {

				@Nonnull
				private final Integer arg;

				public Inner(@Nonnull Integer arg) {
					this.arg = arg;
				}

				@Nonnull
				Inner testInnerNew(
						@Size(max = 15) Outer.@Nonnull @Size(max = 16) Middle.@Size(max = 17) Inner this,
						@Nonnull Integer arg) {
					return new @Nonnull Middle.@Size(max = 18) Inner(arg);
				}

			}

		}

	}

	public static @Deprecated
	@Size(max = 19)
	Map<@Nonnull ? super Short, /* TODO bug eclipse ? super */@Nonnull @Size(max = 20) String> testStatic;

	public static String testArrays() {
		CharSequence[] @Size(max = 21) [] @Nonnull @Size(max = 22) [][] test = new @Nonnull CharSequence @Nonnull [10][][] @Nonnull [];
		return (@Nonnull String) test[0][1][2][3];
	}

	public static @Nonnull
	String testParam(@Nonnull @Size(max = 23) CharSequence str) {
		return (@Nonnull @Size(max = 24) String) str;
	}

	public static <U, @Nonnull @Size(max = 25) V extends @Size(max = 26) @Nonnull String, @Nonnull W> @Nonnull @Size(max = 27) V testTypeParam(
			final @Nonnull U muh) {
		@Size(max = 28)
		@Nonnull
		CharSequence str = "test";
		return (@Nonnull @Size(max = 29) V) str;
	}

	public @Deprecated
	@Size(max = 30)
	Map<Short, @Nonnull @Size(max = 31) String> test;

	public void testThrows() throws IOException, @Nonnull RuntimeException,
			@Size(max = 32) @Nonnull NullPointerException {
		System.out.println("TEST");
	}

	public void testTryCatch() {
		try {
			System.out.println("TEST");
		} catch (final @Size(max = 33) NullPointerException e) {
			System.out.println("CATCH");
		} catch (final @Size(max = 34) RuntimeException
				| @Size(max = 35) @Nonnull Error e) {
			System.out.println("MULTI_CATCH");
		}
	}

}