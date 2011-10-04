package org.decojer.cavaj.test;

public class DecTestConstants {

	public static void byteConstants() {
		byte maxValue = Byte.MAX_VALUE;
		byte minValue = Byte.MIN_VALUE;

		System.out.println("TEST: " + maxValue + " : " + minValue);
	}

	public static void charConstants() {
		char maxValue = Character.MAX_VALUE;
		char minValue = Character.MIN_VALUE;

		System.out.println("TEST: " + maxValue + " : " + minValue);
	}

	public static void doubleConstants() {
		double val0 = 0D;
		double val1 = 1D;
		double maxValue = Double.MAX_VALUE;
		double minValue = Double.MIN_VALUE;
		double nan = Double.NaN;
		double negativeInfinity = Double.NEGATIVE_INFINITY;
		double positiveInfinity = Double.POSITIVE_INFINITY;

		System.out.println("TEST: " + val0 + " : " + val1 + " : " + maxValue
				+ " : " + minValue + " : " + nan + " : " + negativeInfinity
				+ " : " + positiveInfinity);
	}

	public static void floatConstants() {
		float val0 = 0F;
		float val1 = 1F;
		float maxValue = Float.MAX_VALUE;
		float minValue = Float.MIN_VALUE;
		float nan = Float.NaN;
		float negativeInfinity = Float.NEGATIVE_INFINITY;
		float positiveInfinity = Float.POSITIVE_INFINITY;

		System.out.println("TEST: " + val0 + " : " + val1 + " : " + maxValue
				+ " : " + minValue + " : " + nan + " : " + negativeInfinity
				+ " : " + positiveInfinity);
	}

	public static void longConstants() {
		long maxValue = Long.MAX_VALUE;
		long minValue = Long.MIN_VALUE;

		System.out.println("TEST: " + maxValue + " : " + minValue);
	}

	public static void shortConstants() {
		short maxValue = Short.MAX_VALUE;
		short minValue = Short.MIN_VALUE;

		System.out.println("TEST: " + maxValue + " : " + minValue);
	}

}