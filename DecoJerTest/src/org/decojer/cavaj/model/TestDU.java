package org.decojer.cavaj.model;

import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.decojer.DecoJer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
class TestDU {

	private final static Logger LOGGER = Logger.getLogger(TestDU.class.getName());

	private File projectFolder;

	@BeforeClass
	void _beforeClass() throws URISyntaxException {
		projectFolder = new File(getClass().getResource("TestDU.class").toURI()).getParentFile()
				.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
	}

	@Test
	void testBytecodeClosed() {
		for (File file : new File(projectFolder, "test_bytecode_closed").listFiles()) {
			LOGGER.info("######### Decompiling: " + file + " #########");
			DU du = DecoJer.createDu();
			du.read(file.getAbsolutePath());
			for (final CU cu : du.getCus()) {
				cu.decompile(false);
			}
		}
	}

	@Test
	void testBytecodeFree() {
		for (File file : new File(projectFolder, "test_bytecode_free").listFiles()) {
			LOGGER.info("######### Decompiling: " + file + " #########");
			DU du = DecoJer.createDu();
			du.read(file.getAbsolutePath());
			for (final CU cu : du.getCus()) {
				cu.decompile(false);
			}
		}
	}

	@Test
	void testDecojerJar() {
		DU du = DecoJer.createDu();
		du.read(new File(new File(projectFolder, "dex"), "classes.jar").getAbsolutePath());
		for (final CU cu : du.getCus()) {
			cu.decompile(false);
		}
	}

	@Test
	void testEclipsePlugins() {
		for (File file : new File("D:/Software/eclipse-rcp-kepler-64-jdk8/plugins").listFiles()) {
			LOGGER.info("######### Decompiling: " + file + " #########");
			DU du = DecoJer.createDu();
			du.read(file.getAbsolutePath());
			for (final CU cu : du.getCus()) {
				cu.decompile(false);
			}
		}
	}

}