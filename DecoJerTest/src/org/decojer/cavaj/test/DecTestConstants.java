package org.decojer.cavaj.test;

public class DecTestConstants {

	public static void byteConstants() {
		byte minValue = Byte.MAX_VALUE;
		byte maxValue = Byte.MIN_VALUE;

		System.out.println("TEST: " + minValue + " : " + maxValue);
		System.out.println("TEST: " + (-129 < minValue) + " : "
				+ (128 > maxValue));
		System.out.println("TEST: " + (-128 >= minValue) + " : "
				+ (127 <= maxValue));
	}

	public static void charConstants() {
		char minValue = Character.MIN_VALUE;
		char maxValue = Character.MAX_VALUE;

		System.out.println("TEST: " + minValue + " : " + maxValue);
		System.out.println("TEST: " + (-1 < minValue) + " : "
				+ (65536 > maxValue));
		System.out.println("TEST: " + (0 >= minValue) + " : "
				+ (65535 <= maxValue));
	}

	public static void doubleConstants() {
		double val0 = 0D;
		double val1 = 1D;
		double minValue = Double.MIN_VALUE;
		double maxValue = Double.MAX_VALUE;
		double nan = Double.NaN;
		double negativeInfinity = Double.NEGATIVE_INFINITY;
		double positiveInfinity = Double.POSITIVE_INFINITY;

		System.out.println("TEST: " + val0 + " : " + val1 + " : " + minValue
				+ " : " + maxValue + " : " + nan + " : " + negativeInfinity
				+ " : " + positiveInfinity);
	}

	public static void floatConstants() {
		float val0 = 0F;
		float val1 = 1F;
		float minValue = Float.MIN_VALUE;
		float maxValue = Float.MAX_VALUE;
		float nan = Float.NaN;
		float negativeInfinity = Float.NEGATIVE_INFINITY;
		float positiveInfinity = Float.POSITIVE_INFINITY;

		System.out.println("TEST: " + val0 + " : " + val1 + " : " + minValue
				+ " : " + maxValue + " : " + nan + " : " + negativeInfinity
				+ " : " + positiveInfinity);
	}

	public static void longConstants() {
		long minValue = Long.MIN_VALUE;
		long maxValue = Long.MAX_VALUE;

		System.out.println("TEST: " + minValue + " : " + maxValue);
	}

	public static void shortConstants() {
		short minValue = Short.MIN_VALUE;
		short maxValue = Short.MAX_VALUE;

		System.out.println("TEST: " + minValue + " : " + maxValue);
		System.out.println("TEST: " + (-32769 < minValue) + " : "
				+ (32768 > maxValue));
		System.out.println("TEST: " + (-32768 >= minValue) + " : "
				+ (32767 <= maxValue));
	}

}