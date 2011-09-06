package org.decojer.cavaj.test;

public class DecTestConstants {

	public static void doubleConstants() {
		double maxValue = Double.MAX_VALUE;
		double minNormal = Double.MIN_NORMAL;
		double minValue = Double.MIN_VALUE;
		double nan = Double.NaN;
		double negativeInfinity = Double.NEGATIVE_INFINITY;
		double positiveInfinity = Double.POSITIVE_INFINITY;
		int maxExponent = Double.MAX_EXPONENT;
		int minExponent = Double.MIN_EXPONENT;
		int size = Double.SIZE;
		Class<Double> type = Double.TYPE;

		System.out.println("TEST: " + maxValue + " : " + minNormal + " : "
				+ minValue + " : " + nan + " : " + negativeInfinity + " : "
				+ positiveInfinity + " : " + maxExponent + " : " + minExponent
				+ " : " + size + " : " + type);
	}

	public static void floadConstants() {
		float maxValue = Float.MAX_VALUE;
		float minNormal = Float.MIN_NORMAL;
		float minValue = Float.MIN_VALUE;
		float nan = Float.NaN;
		float negativeInfinity = Float.NEGATIVE_INFINITY;
		float positiveInfinity = Float.POSITIVE_INFINITY;
		int maxExponent = Float.MAX_EXPONENT;
		int minExponent = Float.MIN_EXPONENT;
		int size = Float.SIZE;
		Class<Float> type = Float.TYPE;

		System.out.println("TEST: " + maxValue + " : " + minNormal + " : "
				+ minValue + " : " + nan + " : " + negativeInfinity + " : "
				+ positiveInfinity + " : " + maxExponent + " : " + minExponent
				+ " : " + size + " : " + type);
	}

}