package org.decojer.cavaj.model;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.types.T;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.beust.jcommander.internal.Lists;

class RecursiveRead extends RecursiveTask<Integer> {

	private static final long serialVersionUID = 1L;

	private File file;

	public RecursiveRead(final File file) {
		this.file = file;
	}

	@Override
	protected Integer compute() {
		if (!file.exists() || !file.canRead()) {
			return null;
		}
		if (file.isDirectory()) {
			final List<RecursiveRead> reads = Lists.newArrayList();
			final File[] listFiles = file.listFiles();
			if (listFiles != null) {
				for (final File child : listFiles) {
					if (!child.exists() || !child.canRead()
							|| child.getName().startsWith("jaxb-xjc-")) {
						continue;
					}
					reads.add(new RecursiveRead(child));
				}
				invokeAll(reads);
			}
			return null;
		}
		try {
			final DU du = DecoJer.createDu();
			final List<T> read = du.read(file.getAbsolutePath());
			if (read == null || read.isEmpty()) {
				return null;
			}
			// doesn't make sense for parallel dedompiling tests:
			// log.info("######### Decompiling: " + file + " (" + read.size() + ") #########");
			for (final CU cu : du.getCus()) {
				try {
					cu.decompile(false);
				} finally {
					cu.clear();
				}
			}
		} catch (final Throwable e) {
			throw new RuntimeException("File: " + file + "\n" + e.getMessage(), e);
		}
		return null;
	}

}

@Test(singleThreaded = true)
// internal parallelization
class TestDU {

	private File projectFolder;

	@BeforeClass
	void _beforeClass() throws URISyntaxException {
		projectFolder = new File(getClass().getResource("TestDU.class").toURI()).getParentFile()
				.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
	}

	private void read(final File file) {
		final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		pool.invoke(new RecursiveRead(file));
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
		read(new File("C:/Users/andre/.m2"));
		read(new File("C:/Users/André Pankraz/.m2"));
		read(new File("F:/.m2"));
	}

	@Test
	void testBytecodeOracle() {
		read(new File("C:/Oracle"));
		read(new File("D:/Oracle"));
		read(new File("E:/Oracle"));
	}

	@Test
	void testDecojerJar() {
		read(new File(new File(projectFolder, "dex"), "classes.jar"));
	}

	@Test
	void testEclipsePlugins() {
		read(new File("D:/Software/eclipse-rcp-luna-SR1-64/plugins"));
	}

}