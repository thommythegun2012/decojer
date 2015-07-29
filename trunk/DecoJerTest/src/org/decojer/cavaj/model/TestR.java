package org.decojer.cavaj.model;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.R.Kind;
import org.decojer.cavaj.model.types.T;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
class TestR {

	@Test
	void isMethodParam() {
		R r = R.createConstR(0, 1, T.INT, null);

		assertTrue(r.isMethodParam());
	}

	@Test
	void properties() {
		R r = R.createConstR(1, 1, T.INT, null);

		assertSame(r.getPc(), 1);
		assertSame(r.getT(), T.INT);
		assertSame(r.getKind(), Kind.CONST);
	}

}