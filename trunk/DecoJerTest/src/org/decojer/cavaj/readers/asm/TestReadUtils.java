package org.decojer.cavaj.readers.asm;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.types.T;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestReadUtils {

	private DU du;

	@BeforeClass
	void _beforeClass() {
		du = DecoJer.createDu();
	}

	@Test
	void annotate() {
		final A a = new A(du.getT("Nonnull"), RetentionPolicy.RUNTIME);

		T t = ReadUtils.annotateT(du.getObjectT(), a, null);
		assertEquals(t.getFullName(), "@Nonnull java.lang.Object");

		// this should annotate Map, not Entry:
		t = ReadUtils.annotateT(du.getT(Map.Entry.class), a, null);
		// outest enclosing / qualifier is annotated, not myself
		assertFalse(t.isAnnotated());
		assertTrue(t.isQualified());
		assertTrue(t.getQualifierT().isAnnotated());
		assertEquals(t.getQualifierT().getFullName(), "@Nonnull java.util.Map");
	}

}