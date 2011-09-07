package org.decojer.cavaj.test.jdk5;

public class DecTestConstants {

	public static void doubleConstants() {
		double maxValue = Double.MAX_VALUE;
		double minValue = Double.MIN_VALUE;
		double nan = Double.NaN;
		double negativeInfinity = Double.NEGATIVE_INFINITY;
		double positiveInfinity = Double.POSITIVE_INFINITY;
		int size = Double.SIZE;
		Class<Double> type = Double.TYPE;

		System.out.println("TEST: " + maxValue + " : " + minValue + " : " + nan
				+ " : " + negativeInfinity + " : " + positiveInfinity + " : "
				+ size + " : " + type);
	}

	public static void floadConstants() {
		float maxValue = Float.MAX_VALUE;
		float minValue = Float.MIN_VALUE;
		float nan = Float.NaN;
		float negativeInfinity = Float.NEGATIVE_INFINITY;
		float positiveInfinity = Float.POSITIVE_INFINITY;
		int size = Float.SIZE;
		Class<Float> type = Float.TYPE;

		System.out.println("TEST: " + maxValue + " : " + minValue + " : " + nan
				+ " : " + negativeInfinity + " : " + positiveInfinity + " : "
				+ size + " : " + type);
	}

}