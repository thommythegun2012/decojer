package org.decojer.cavaj.model;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.types.T;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
@Slf4j
class TestDU {

	private File projectFolder;

	@BeforeClass
	void _beforeClass() throws URISyntaxException {
		projectFolder = new File(getClass().getResource("TestDU.class").toURI()).getParentFile()
				.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
	}

	private void read(final File file) {
		if (file.isDirectory()) {
			final File[] listFiles = file.listFiles();
			if (listFiles != null) {
				for (File child : listFiles) {
					read(child);
				}
			}
		}
		final DU du = DecoJer.createDu();
		final List<T> read = du.read(file.getAbsolutePath());
		if (read == null || read.isEmpty()) {
			return;
		}
		log.info("######### Decompiling: " + file + " (" + read.size() + ") #########");
		for (final CU cu : du.getCus()) {
			try {
				cu.decompile(false);
			} finally {
				cu.clear();
			}
		}
	}

	@Test
	void testBytecodeClosed() {
		read(new File(projectFolder, "test_bytecode_closed"));
	}

	@Test
	void testBytecodeFree() {
		read(new File(projectFolder, "test_bytecode_free"));
	}

	@Test
	void testBytecodeMaven() {
		read(new File("C:/Users/andre/.m2/repository"));
	}

	@Test
	void testBytecodeOracle() {
		read(new File("D:/Oracle"));
	}

	@Test
	void testDecojerJar() {
		read(new File(new File(projectFolder, "dex"), "classes.jar"));
	}

	@Test
	void testEclipsePlugins() {
		read(new File("D:/Software/eclipse-rcp-kepler-SR2-64-jdk8/plugins"));
	}

}