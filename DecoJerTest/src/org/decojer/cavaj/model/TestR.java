package org.decojer.cavaj.model;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.decojer.cavaj.model.code.R;
import org.decojer.cavaj.model.code.R.Kind;
import org.decojer.cavaj.model.types.T;
import org.junit.Test;

public class TestR {

	@Test
	public void isMethodParam() {
		R r = R.createConstR(0, 1, T.INT, null);

		assertTrue(r.isMethodParam());
	}

	@Test
	public void properties() {
		R r = R.createConstR(1, 1, T.INT, null);

		assertSame(1, r.getPc());
		assertSame(T.INT, r.getT());
		assertSame(Kind.CONST, r.getKind());
	}

}