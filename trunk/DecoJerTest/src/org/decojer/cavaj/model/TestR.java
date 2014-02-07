package org.decojer.cavaj.model;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.R.Kind;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
class TestR {

	private DU du;

	@BeforeClass
	void _beforeClass() {
		du = DecoJer.createDu();
	}

	@Test
	void isMethodParam() {
		R r = new R(0, 1, T.INT, Kind.CONST);

		assertTrue(r.isMethodParam());
	}

	@Test
	void properties() {
		R r = new R(1, 1, T.INT, Kind.CONST);

		assertSame(r.getPc(), 1);
		assertSame(r.getT(), T.INT);
		assertSame(r.getKind(), Kind.CONST);
	}

}