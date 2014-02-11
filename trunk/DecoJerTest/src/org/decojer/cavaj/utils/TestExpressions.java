package org.decojer.cavaj.utils;

import static org.testng.Assert.assertEquals;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.transformers.TrInnerClassesAnalysis;
import org.decojer.cavaj.transformers.TrJvmStruct2JavaAst;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
class TestExpressions {

	private DU du;

	private TD context;

	@BeforeClass
	void _beforeClass() {
		du = DecoJer.createDu();
		du.read(getClass().getResource("TestDU.class").getFile());
		TrInnerClassesAnalysis.transform(du);
		context = du.getTd("org.decojer.cavaj.test.DecTestArrays");
		TrJvmStruct2JavaAst.transform(context);
	}

	@Test
	void newType() {
		assertEquals(Expressions.newType(T.AINT, context).toString(), "boolean");
	}

}