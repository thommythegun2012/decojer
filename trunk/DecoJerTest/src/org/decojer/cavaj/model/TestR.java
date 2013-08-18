package org.decojer.cavaj.model;

import static org.testng.Assert.assertSame;

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
		// a decompilation unit is like a class loader, just for references with
		// real classes / type declarations
		du = DecoJer.createDu();
	}

	@Test
	void testProps() {
		R r = new R(1, T.INT, Kind.CONST);

		assertSame(r.getPc(), 1);
		assertSame(r.getT(), T.INT);
		assertSame(r.getKind(), Kind.CONST);
	}

}