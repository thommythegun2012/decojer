package org.decojer.cavaj.utils;

import static org.testng.Assert.assertEquals;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.transformers.TrInnerClassesAnalysis;
import org.decojer.cavaj.transformers.TrOutline;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
class TestExpressions {

	private DU du;

	private T context;

	@BeforeClass
	void _beforeClass() {
		du = DecoJer.createDu();
		du.read(getClass().getResource("TestExpressions.class").getFile());
		TrInnerClassesAnalysis.transform(du);
		context = du.getT("org.decojer.cavaj.utils.TestExpressions");
		TrOutline.transform(context);
	}

	@Test
	void newType() {
		assertEquals(Expressions.newType(T.AINT, context).toString(), "boolean");
	}

}