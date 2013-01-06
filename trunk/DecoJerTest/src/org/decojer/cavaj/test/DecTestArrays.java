package org.decojer.cavaj.test;

public abstract class DecTestArrays {

	public static void testSimple() {
		boolean[] boolsFull0 = new boolean[4];
		boolsFull0[0] = true;
		boolsFull0[1] = false;
		boolsFull0[2] = true;
		boolsFull0[3] = false;

		boolean[] boolsFull1 = new boolean[4];
		boolsFull1[0] = true;
		boolsFull1[2] = true;

		boolean[] boolsFull2 = new boolean[4];
		boolsFull2[0] = boolsFull2[2] = true;

		// length not allowed here
		boolean[] boolsFull3 = new boolean[] { true, false, true, false };

		boolean[] bools = { true, false, true, false };
		boolean[] boolsEmpty = {};

		boolean[][][][] boolNew = new boolean[10][][][];

		char[] chars = { 'a', 'b', 'c' };
		// 0x80 is int!
		byte[] bytes = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0x7f };
		// 0x8000 is short!
		// 'a' not in jdk < 1.3
		short[] shorts = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xff, 0x7fff };
		int[] ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 'a', 0x80000000,
				0xffffffff };
		long[] longs = { 0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9, 'a' };
		float[] floats = { 0F, 1F, 2F, 3 };
		double[] doubles = { 0D, 1D, 2D, 3, 4F };
		String[] strings = { "test0", null, "test1", null, "test2" };
	}

	public void testMultiFill() {
		int[][] intssEmpty = { {} };
		int[][] intss = { { 0, 1, 2, 3, 4 }, { 5, 6, 7, 8, 9, 0 }, { -1, 1 } };
	}

	public void testMultiNewArray() {
		byte[][][][] a_2 = new byte[0][5][][];
		char[][][][] a_3 = new char[-1][5][-100][];
		Object[][][][] a_4 = new String[1][2][3][4];
	}

}