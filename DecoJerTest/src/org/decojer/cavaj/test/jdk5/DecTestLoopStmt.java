package org.decojer.cavaj.test.jdk5;

import java.util.Collections;

public abstract class DecTestLoopStmt {

	public static void testForeach(final int a) {
		for (final Object test : Collections.singleton(1)) {
			System.out.println((Integer) test + a);
		}
	}

}