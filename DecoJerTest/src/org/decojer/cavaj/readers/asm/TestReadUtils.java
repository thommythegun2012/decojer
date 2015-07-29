package org.decojer.cavaj.readers.asm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.types.T;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReadUtils {

	private static DU du;

	@BeforeClass
	public static void _beforeClass() {
		du = DecoJer.createDu();
	}

	@Test
	public void annotate() {
		final A a = new A(du.getT("Nonnull"), RetentionPolicy.RUNTIME);

		T t = ReadUtils.annotateT(du.getObjectT(), a, null);
		assertEquals("@Nonnull java.lang.Object", t.getFullName());

		// this should annotate Map, not Entry:
		t = ReadUtils.annotateT(du.getT(Map.Entry.class), a, null);
		// outest enclosing / qualifier is annotated, not myself
		assertFalse(t.isAnnotated());
		assertTrue(t.isQualified());
		assertTrue(t.getQualifierT().isAnnotated());
		assertEquals("@Nonnull java.util.Map", t.getQualifierT().getFullName());
	}

}