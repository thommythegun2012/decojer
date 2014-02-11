package org.decojer.cavaj.model;

import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.decojer.DecoJer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestDU {

	private final static Logger LOGGER = Logger.getLogger(TestDU.class.getName());

	private File testdataFolder;

	@BeforeClass
	void _beforeClass() throws URISyntaxException {
		testdataFolder = new File(new File(getClass().getResource("TestDU.class").toURI())
				.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile()
				.getParentFile(), "testdata");
	}

	@Test
	void testAll() {
		for (File file : testdataFolder.listFiles()) {
			LOGGER.info("######### Decompiling: " + file + " #########");
			DU du = DecoJer.createDu();
			du.read(file.getAbsolutePath());
			for (final CU cu : du.getCus()) {
				cu.decompile(false);
			}
		}
	}

}