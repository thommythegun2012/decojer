package org.decojer.cavaj.test;

import java.io.Serializable;

public class DecTestFields {

	public final byte byteTest = 127;

	public char charTest = 'c';

	public double doubleTest = .5432154321D; // not final!

	public boolean dynamicBooleanTest = doubleTest > 1D;

	public final static float floatTest = .54321F;

	protected final int intTest = 1000000;

	protected final static long longTest = 1000000000L;

	protected final Class referenceClassTest = String.class;

	protected final Class referenceClassTest2 = int.class;

	protected final Class referenceClassTest3 = int[][].class.getClass();

	private final String referenceStringTest = "test";

	private final short shortTest = 32767;

	private static final boolean booleanTest = true;

	private static final int[] referenceIntArrayTest = { 1000000 };

	public static void main(String[] args) {
		System.out.println("TEST : " + String.class);
		System.out.println("TEST : " + String[].class);
		System.out.println("TEST : " + String[].class.getSuperclass());
		System.out.println("TEST : " + String[].class.getInterfaces().length);
		System.out.println("TEST : " + String[].class.getInterfaces()[0]);
		System.out.println("TEST : " + String[].class.getInterfaces()[1]);
		System.out.println("TEST : " + Class.class.getSuperclass());
		System.out.println("TEST : " + Object.class.getSuperclass());
		System.out.println("TEST : " + Object.class.getInterfaces().length);
		System.out.println("TEST : " + Serializable.class.getSuperclass());
		System.out.println("TEST : " + void.class);
		System.out.println("TEST : " + void.class.getSuperclass());
		System.out.println("TEST : " + int.class);
		System.out.println("TEST : " + int.class.getSuperclass());
		System.out.println("TEST : " + int[].class);
		System.out.println("TEST : " + int[].class.getSuperclass());
		System.out.println("TEST : " + int[][].class);
		System.out.println("TEST : " + int[][].class.getSuperclass());
		System.out.println("TEST : " + int.class.getClass());
		System.out.println("TEST : " + int.class.getClass().getSuperclass());
	}

	private final int[][] referenceIntArrayNullTest = null;

	public char getCharTest() {
		return charTest;
	}

}