package org.decojer.cavaj.test.jdk5;

public abstract class DecTestSwitchEnum {

	public static void testChar(final char c) {
		switch (c) {
		case 'a':
			System.out.println('a');
			break;
		case 'b':
			System.out.println('b');
			break;
		case Character.MAX_HIGH_SURROGATE:
		case Character.MAX_LOW_SURROGATE:
		case Character.MAX_VALUE:
		case Character.MIN_HIGH_SURROGATE:
		case Character.MIN_LOW_SURROGATE:
		case Character.MIN_VALUE:
			System.out.println('?');
			break;
		default:
			System.out.println("??");
		}
	}

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

	public static void testSwitchEnum2(DecTestEnumToy e) {
		switch (e) {
		case DOLL:
		default:
			System.out.println("DOLL");
		case SOLDIER:
			System.out.println("SOLDIER");
		}
		System.out.println("DONE");
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