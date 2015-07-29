package org.decojer.cavaj.utils;

import static org.junit.Assert.assertEquals;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.transformers.TrInnerClassesAnalysis;
import org.decojer.cavaj.transformers.TrOutline;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestExpressions {

	private static DU du;

	private static T context;

	@BeforeClass
	public static void _beforeClass() {
		du = DecoJer.createDu();
		du.read(TestExpressions.class.getResource("TestExpressions.class").getFile());
		TrInnerClassesAnalysis.transform(du);
		context = du.getT("org.decojer.cavaj.utils.TestExpressions");
		TrOutline.transform(context);
	}

	@Test
	public void newType() {
		assertEquals("boolean", Expressions.newType(T.AINT, context).toString());
	}

}