package org.decojer.cavaj.test;


public class DecTestMerge {

	public static byte testByte(byte b) {
		return b;
	}

	public static char testChar(char c) {
		return c;
	}

	public static int testInt(int i) {
		return i;
	}

	public static short testShort(short i) {
		return i;
	}

	public void testExplicitConversations() {
		double d = 2.0D;
		float f = (float) d; // d2f
		testInt((int) f);

		long l = (long) d; // d2l
		testInt((int) l);
		int i = (int) l; // i2l
		short s = (short) i; // i2s
		byte b = (byte) s; // i2b (would be d2i->i2b with d)

		boolean bool = true;
	}

	public void testImplicitConversations() {
		boolean bool = true;

		byte b = 2; // not bool
		testInt(b);
		testChar((char) b); // i2c
		testShort(b);
		testByte(b);
		short s = b;
		testInt(s);
		testChar((char) s); // i2c
		testShort(s);
		testByte((byte) s);
		char c = (char) s; // i2c
		testInt(c);
		testChar(c);
		testShort((short) c); // i2s
		testByte((byte) c);
		int i = c; // or s, b
		testInt(i); // or c, s, b
		testChar((char) i); // i2c
		testShort((short) i); // i2s
		testByte((byte) i);

		long l = i; // i2l, also for byte...
		float f = i; // i2f, also for byte...

		double d = l; // l2d
	}

}