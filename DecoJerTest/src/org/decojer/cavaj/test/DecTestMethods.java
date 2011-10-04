package org.decojer.cavaj.test;

import java.io.IOException;
import java.io.NotSerializableException;

public abstract class DecTestMethods {

	static {
		System.out.println("TEST");
	}

	public static final boolean getBooleanTest(final int a, final int b) {
		return a < b;
	}

	public DecTestMethods() {
		System.out.println("Inner " + DecTestInner.class);
		return;
	}

	public int arrayLengthTest(final int[] intArray) {
		return intArray.length;
	}

	public float divFloatTest(final float a, final float b, final float c) {
		return a / (b / c);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof DecTestMethods))
			return false;
		DecTestMethods that = (DecTestMethods) obj;
		if (that.hashCode() == this.hashCode())
			return true;
		return false;
	}

	public abstract int getIntTest(final String test) throws IOException,
			IllegalArgumentException, NotSerializableException;

	public int subIntTest(int a, int b, int c) {
		return (a = b - c) - (b++ - --c);
	}

}