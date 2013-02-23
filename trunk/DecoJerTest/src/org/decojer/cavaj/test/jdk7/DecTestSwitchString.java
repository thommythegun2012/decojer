package org.decojer.cavaj.test.jdk7;

public abstract class DecTestSwitchString {

	public static void testSwitchString(final String s) {
		switch (s) {
		case "BB":
		case "STR2":
			System.out.println("STRING2");
			break;
		case "STR3":
		case "STR4":
			System.out.println("STRING3/4");
		case "Aa":
			System.out.println("Hash Collision with BB and Fall Through...");
		}
	}

	public static void testSwitchStringDefault(final String s) {
		switch (s) {
		case "STR2":
			System.out.println("STRING2");
			break;
		default:
			System.out.println("default");
			break;
		case "STR3":
			System.out.println("STRING3");
			break;
		case "STR4":
			System.out.println("STRING4");
		}
	}

	public static void testSwitchStringDefault2(final String s) {
		switch (s) {
		case "STR0":
			System.out.println("STRING3");
		case "STR1":
		case "STR2":
		default:
			System.out.println("default");
		case "STR4":
			System.out.println("STRING4");
		}
	}

}