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
	void testDecojerTests() {
		DU du = DecoJer.createDu();
		du.read(new File(new File(projectFolder, "dex"), "classes.jar").getAbsolutePath());
		for (final CU cu : du.getCus()) {
			cu.decompile(false);
		}
	}

	@Test
	void testOpenSourceBytecode() {
		for (File file : new File(projectFolder, "testdata").listFiles()) {
			LOGGER.info("######### Decompiling: " + file + " #########");
			DU du = DecoJer.createDu();
			du.read(file.getAbsolutePath());
			for (final CU cu : du.getCus()) {
				cu.decompile(false);
			}
		}
	}

}