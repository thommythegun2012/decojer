package org.decojer.cavaj.test;

public class DecTestConstants {

	public static void doubleConstants() {
		double maxValue = Double.MAX_VALUE;
		double minValue = Double.MIN_VALUE;
		double nan = Double.NaN;
		double negativeInfinity = Double.NEGATIVE_INFINITY;
		double positiveInfinity = Double.POSITIVE_INFINITY;

		System.out.println("TEST: " + maxValue + " : " + minValue + " : " + nan
				+ " : " + negativeInfinity + " : " + positiveInfinity);
	}

	public static void floadConstants() {
		float maxValue = Float.MAX_VALUE;
		float minValue = Float.MIN_VALUE;
		float nan = Float.NaN;
		float negativeInfinity = Float.NEGATIVE_INFINITY;
		float positiveInfinity = Float.POSITIVE_INFINITY;

		System.out.println("TEST: " + maxValue + " : " + minValue + " : " + nan
				+ " : " + negativeInfinity + " : " + positiveInfinity);
	}

}