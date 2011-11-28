package org.decojer.cavaj.test.jdk7;

import java.io.File;
import java.io.IOException;

public abstract class DecTestExceptions {

	void multiCatch() {
		System.out.println("PRE");
		try {
			new File("C:/").createNewFile();
		} catch (final IOException | SecurityException e) {
			System.out.println("EXC: " + e);
			e.printStackTrace();
		}
		System.out.println("POST");
	}

}