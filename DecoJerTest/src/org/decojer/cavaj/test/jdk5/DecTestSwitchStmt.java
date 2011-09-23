package org.decojer.cavaj.test.jdk5;

public abstract class DecTestSwitchStmt {

	public static void testSwitchEnum(final DecTestEnums e) {
		switch (e) {
		// no ENUM1
		case ENUM2:
			System.out.println("ENUM2");
			break;
		case ENUM3:
		case ENUM4:
			System.out.println("ENUM3/4");
		}
	}

	public static void testSwitchEnumDefault(final DecTestEnums e) {
		switch (e) {
		// no ENUM1
		case ENUM2:
			System.out.println("ENUM2");
			break;
		default:
			System.out.println("default");
			break;
		case ENUM3:
			System.out.println("ENUM3");
			break;
		case ENUM4:
			System.out.println("ENUM4");
		}
	}

}