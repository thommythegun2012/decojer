package org.decojer.cavaj.test.jdk5;

public abstract class DecTestSwitchStmt {

	public static void testSwitchEnum(final DecTestEnum e) {
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

	public static void testSwitchEnumDefault(final DecTestEnum e) {
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

	public static void testSwitchEnumRev(final DecTestEnum e) {
		switch (e) {
		// no ENUM1
		case ENUM4:
			System.out.println("ENUM4");
			break;
		case ENUM3:
		case ENUM2:
			System.out.println("ENUM2/3");
		}
	}

	public static void testSwitchEnumRevDefault(final DecTestEnum e) {
		switch (e) {
		// no ENUM1
		case ENUM4:
			System.out.println("ENUM4");
			break;
		default:
			System.out.println("default");
			break;
		case ENUM3:
			System.out.println("ENUM3");
			break;
		case ENUM2:
			System.out.println("ENUM2");
		}
	}

}