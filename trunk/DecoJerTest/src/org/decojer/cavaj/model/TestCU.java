package org.decojer.cavaj.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.text.edits.TextEdit;
import org.testng.annotations.Test;

@Test
public class TestCU {

	@Test
	void testBytecodeClosed() throws IOException {
		StringBuffer src = new StringBuffer();
		for (String line : Files
				.readAllLines(Paths
						.get("D:/Data/Decomp/workspace/DecoJerTest/src/org/decojer/cavaj/model/reformatExample.txt"))) {
			src.append(line);
		}
		String source = src.toString();
		final Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, "1.5");
		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
		final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0,
				source.length(), 0, "\r\n");
		assert edit != null;
	}

}