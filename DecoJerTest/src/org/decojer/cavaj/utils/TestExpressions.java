package org.decojer.cavaj.utils;

import static org.testng.Assert.assertEquals;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestExpressions {

	private DU du;

	private T objectT;

	@BeforeClass
	void _beforeClass() {
		du = DecoJer.createDu();
		objectT = du.getObjectT();
	}

	@Test
	void testNewType() {
		assertEquals(Expressions.newType(T.AINT, objectT.getTd()).toString(),
				"");
	}

}