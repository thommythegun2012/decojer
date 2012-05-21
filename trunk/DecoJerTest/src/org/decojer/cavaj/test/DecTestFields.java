package org.decojer.cavaj.test;

import java.io.Serializable;

public class DecTestFields {

	public static void main(String[] args) {
		System.out.println("TEST : " + String.class);
		System.out.println("TEST : " + String.class.getSuperclass());
		System.out.println("TEST : " + String.class.getInterfaces().length);
		System.out.println("TEST : " + String[].class);
		System.out.println("TEST : " + String[].class.getSuperclass());
		System.out.println("TEST : " + String[].class.getInterfaces().length);
		System.out.println("TEST : " + String[].class.getInterfaces()[0]);
		System.out.println("TEST : " + String[].class.getInterfaces()[1]);
		System.out.println("TEST : " + Object.class.getSuperclass());
		System.out.println("TEST : " + Object.class.getInterfaces().length);
		System.out.println("TEST : " + Serializable.class.getSuperclass());
		System.out.println("TEST : " + void.class);
		System.out.println("TEST : " + void.class.getSuperclass());
		System.out.println("TEST : " + byte.class);
		System.out.println("TEST : " + byte.class.getSuperclass());
		System.out.println("TEST : " + byte.class.getInterfaces().length);
		System.out.println("TEST : " + int.class);
		System.out.println("TEST : " + int[].class);
		System.out.println("TEST : " + int[][].class);
		System.out.println("TEST : " + int.class.getClass());
		System.out.println("TEST : "
				+ int.class.getClass().getInterfaces().length);
	}

	boolean booleanTest = true;

	final boolean booleanTestFinal = true;

	static boolean booleanTestStatic = true;

	static final boolean booleanTestStaticFinal = true;

	byte byteTest = 127;

	final byte byteTestFinal = 127;

	static byte byteTestStatic = 127;

	static final byte byteTestStaticFinal = 127;

	char charTest = 'c';

	final char charTestFinal = 'c';

	static char charTestStatic = 'c';

	static final char charTestStaticFinal = 'c';

	double doubleTest = .5432154321D;

	final double doubleTestFinal = .5432154321D;

	static public double doubleTestStatic = .5432154321D;

	static public final double doubleTestStaticFinal = .5432154321D;

	// following 4 only precalculated for final in expression!
	private boolean dynamicBooleanTest = doubleTestFinal > 1D;

	private final boolean dynamicBooleanTestFinal = doubleTestFinal > 1D;

	private static boolean dynamicBooleanTestStatic = doubleTestStaticFinal > 1D;

	private static final boolean dynamicBooleanTestStaticFinal = doubleTestStaticFinal > 1D;

	public float floatTest = .54321F;

	public final float floatTestFinal = .54321F;

	public static float floatTestStatic = .54321F;

	public static final float floatTestStaticFinal = .54321F;

	public int intTest = 1000000;

	public final int intTestFinal = 1000000;

	public static int intTestStatic = 1000000;

	public static final int intTestStaticFinal = 1000000;

	public long longTest = 1000000000L;

	public final long longTestFinal = 1000000000L;

	public static long longTestStatic = 1000000000L;

	public static final long longTestStaticFinal = 1000000000L;

	protected Class referenceClassTest = String.class;

	protected final Class referenceClassTestFinal = int.class;

	protected static Class referenceClassTestStatic = int[][].class.getClass();

	protected static final Class referenceClassTestStaticFinal = void.class;

	protected int[] referenceIntArrayTest = { 1000000 };

	protected final int[] referenceIntArrayTestFinal = { 1000000 };

	protected static int[] referenceIntArrayTestStatic = { 1000000 };

	protected static final int[] referenceIntArrayTestStaticFinal = { 1000000 };

	protected int[][] referenceIntArrayNullTest = null;

	protected final int[][] referenceIntArrayNullTestFinal = null;

	protected static int[][] referenceIntArrayNullTestStatic = null;

	protected static final int[][] referenceIntArrayNullTestStaticFinal = null;

	protected String referenceStringTest = "test";

	protected final String referenceStringTestFinal = "test";

	protected static String referenceStringTestStatic = "test";

	protected static final String referenceStringTestStaticFinal = "test";

	public short shortTest = 32767;

	public final short shortTestFinal = 32767;

	public static short shortTestStatic = 32767;

	public static final short shortTestStaticFinal = 32767;

	public char getCharTest() {
		return charTest;
	}

}