package org.decojer.cavaj.test.jdk7;

import java.io.File;
import java.io.FileInputStream;
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

	void tryWithRessource() throws IOException {
		System.out.println("PRE");
		try (FileInputStream file = new FileInputStream("C:/")) {
			System.out.println("READ: " + file.read());
		}
		System.out.println("POST");
	}

	void tryWithRessourceCatch() {
		System.out.println("PRE");
		try (FileInputStream file = new FileInputStream("C:/")) {
			System.out.println("READ: " + file.read());
		} catch (IOException e) {
			System.out.println("EXC: " + e);
		}
		System.out.println("POST");
	}

}