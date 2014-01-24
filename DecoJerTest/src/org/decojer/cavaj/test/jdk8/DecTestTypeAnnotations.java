package org.decojer.cavaj.test.jdk8;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @Nonnull java.util.HashMap not allowed...strange spec
public class DecTestTypeAnnotations<U, @Size(max = 1) @Nonnull V extends java.util.@Nonnull @Size(min = 1, max = 2) @Size(max = 3) HashMap<String, @Nonnull @Size(max = 4) Integer> & @Size(max = 5) @Nonnull CharSequence>
		extends
		@Size(max = 6) HashMap<@Size(max = 7) U, @Size(max = 8) @Nonnull List<List<@Size(max = 9) List<@Size(max = 10) ? extends @Size(max = 11) String>>>>
		implements @Nonnull @Size(max = 14) Serializable {

	class Outer<W> {

		class Middle {

			class Inner extends
					Outer<@Size(max = 21) String>.@Size(max = 22) Middle {

				@Nonnull
				private final Integer arg;

				public Inner(@Size(max = 23) Integer arg) {
					this.arg = arg;
				}

				@Nonnull
				Inner testInnerNew(
						@Size(max = 24) Outer<W>.@Nonnull @Size(max = 25) Middle.@Size(max = 26) Inner this,
						@Size(max = 27) Integer arg) {
					// Eclipse Bug: Outer<String> not allowed
					return new Outer.@Size(max = 28) Middle.@Size(max = 29) Inner(
							arg);
				}
			}

		}

	}

	public static @Deprecated
	@Size(max = 31)
	Map<@Size(max = 32) ? super Short, ? super @Nonnull @Size(max = 33) String> testStatic;

	public static String testArrays() {
		@Size(max = 41)
		@Nonnull
		List<@Size(max = 42) CharSequence>[] @Size(max = 43) [] @Nonnull @Size(max = 44) [][] test = new @Size(max = 45) ArrayList @Size(max = 45) [10][][] @Size(max = 46) @Size(max = 47) [];
		return (@Size(max = 49) String) test[0][1][2][3].get(0);
	}

	public static @Size(max = 51)
	String testParam(@Nonnull @Size(max = 52) CharSequence str) {
		return (@Nonnull @Size(max = 53) String) str;
	}

	public static <U, @Nonnull @Size(max = 61) V extends @Size(max = 62) @Nonnull String, @Size(max = 63) W> @Nonnull @Size(max = 64) V testTypeParam(
			final @Size(max = 65) U muh) {
		@Size(max = 66)
		@Nonnull
		CharSequence str = "test";
		return (@Nonnull @Size(max = 67) V) str;
	}

	public @Deprecated
	@Size(max = 34)
	Map<Short, @Nonnull @Size(max = 35) String> test;

	public void testThrows() throws IOException,
			@Size(max = 71) RuntimeException,
			@Size(max = 72) @Nonnull NullPointerException {
		System.out.println("TEST");
	}

	public void testTryCatch() {
		try {
			System.out.println("TEST");
		} catch (final @Size(max = 81) NullPointerException e) {
			System.out.println("CATCH");
		} catch (final @Size(max = 82) @Size(max = 83) RuntimeException
				| @Size(max = 84) @Nonnull Error e) {
			System.out.println("MULTI_CATCH");
		}
	}

}