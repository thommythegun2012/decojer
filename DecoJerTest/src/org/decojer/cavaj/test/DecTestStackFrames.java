package org.decojer.cavaj.test;

public class DecTestStackFrames {

	public void testBlocks() {
		{
			boolean neg = true;
			System.out.println("TEST: " + neg);
		}
		{
			boolean neg = false;
			System.out.println("TEST: " + neg);
		}
		Boolean neg = Boolean.TRUE;
		System.out.println("TEST: " + neg);
	}

	public boolean testCond(int b) {
		boolean neg;
		if (b < 0)
			neg = true;
		else
			neg = false;
		return neg;
	}

}