package org.decojer.cavaj.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.text.edits.TextEdit;
import org.junit.Test;

public class TestCU {

	@Test
	public void testBytecodeClosed() throws IOException {
		StringBuffer src = new StringBuffer();
		for (String line : Files.readAllLines(Paths.get(
				"D:/Data/Decomp/workspace/DecoJerTest/src/org/decojer/cavaj/model/reformatExample.txt"))) {
			src.append(line);
		}
		String source = src.toString();
		@SuppressWarnings("unchecked")
		final Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, "1.5");
		// Eclipse-Bug with low values and long lines like in com.ibm.icu.util.LocaleMatcher,
		// prevent this anyway and do it manually because of line number preservation?
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "10000");
		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
		final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0,
				source.length(), 0, "\r\n");
		assert edit != null;
	}

}