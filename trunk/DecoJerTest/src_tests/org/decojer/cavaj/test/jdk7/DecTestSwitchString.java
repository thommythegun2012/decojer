package org.decojer.cavaj.test.jdk7;

public abstract class DecTestSwitchString {

	public static void testSwitchString(final String s) {
		switch (s) {
		case "BB":
		case "STR0":
			System.out.println("BB");
			break;
		case "STR1":
		case "STR2":
			System.out.println("STR1/2");
		case "Aa":
			System.out
					.println("AA ... Hash Collision with BB and Fall Through...");
		}
	}

	public static void testSwitchStringDefault(final String s) {
		switch (s) {
		case "STR0":
			System.out.println("STR0");
			break;
		default:
			System.out.println("default");
			break;
		case "STR1":
			System.out.println("STR1");
			break;
		case "STR2":
			System.out.println("STR2");
		}
	}

	public static void testSwitchStringDefault2(final String s) {
		switch (s) {
		case "STR0":
			System.out.println("STR0");
		case "STR1":
		case "STR2":
		default:
			System.out.println("default");
		case "STR3":
			System.out.println("STR3");
		}
	}

}