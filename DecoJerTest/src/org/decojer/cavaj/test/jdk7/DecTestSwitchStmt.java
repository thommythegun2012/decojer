package org.decojer.cavaj.test.jdk7;

public abstract class DecTestSwitchStmt {

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

}