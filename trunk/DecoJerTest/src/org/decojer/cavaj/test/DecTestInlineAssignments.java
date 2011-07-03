package org.decojer.cavaj.test;

public abstract class DecTestInlineAssignments {

	private static int a;

	private int b;

	private static int[] ar;

	private int[] br;

	public int assgnAddIntTest(int c, final int d, final int[] cr) {
		return (c += 2 * d) + (a += 2 * d) + (this.b += 2 * d)
				+ (cr[10] += 2 * d) + (ar[10] += 2 * d)
				+ (this.br[10] += 2 * d);
	}

	public int assgnIntTest(int c, final int d, final int[] cr) {
		return (c = 2 * d) + (a = 2 * d) + (this.b = 2 * d) + (cr[10] = 2 * d)
				+ (ar[10] = 2 * d) + (this.br[10] = 2 * d);
	}

	public int decIntTest(int c, final int[] cr) {
		return --c - c-- - --a - a-- - --this.b - this.b-- - --cr[10]
				- cr[10]-- - --ar[10] - ar[10]-- - --this.br[10]
				- this.br[10]--;
	}

	public int incAssgnIntTest() {
		return ++a + (a += 1) + ++ar[10] + (ar[10] += 1);
	}

	public int incIntTest(int c, final int[] cr) {
		return ++c + c++ + ++a + a++ + ++this.b + this.b++ + ++cr[10]
				+ cr[10]++ + ++ar[10] + ar[10]++ + ++this.br[10]
				+ this.br[10]++;
	}

}