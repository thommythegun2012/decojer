package org.decojer.cavaj.test.jdk8;

public class DecTestTypeAnnotations {

	public String test() {
		CharSequence str = "test";
		return (@Nonnull String) str;
	}

	public String test2() {
		CharSequence str = "test";
		return (@Nonnull @Nonempty String) str;
	}

}