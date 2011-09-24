package org.decojer.cavaj.test.jdk7;

public abstract class DecTestSwitchStmt {

	public static void testSwitchString(final String s) {
		switch (s) {
		case "STR2":
			System.out.println("STRING2");
			break;
		case "STR3":
		case "STR4":
			System.out.println("STRING3/4");
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