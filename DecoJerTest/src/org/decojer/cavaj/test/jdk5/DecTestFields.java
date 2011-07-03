package org.decojer.cavaj.test.jdk5;

import java.util.HashMap;

public class DecTestFields<E> extends HashMap<String, E> {

	protected final Class referenceClassTest = String.class;

	@DecTestAnnotations(byteTest = 1)
	protected final static Class<String> referenceGenericClassTest = String.class;

	@DecTestAnnotations(intTest = 1, charTest = 'c')
	protected final Class<? extends E> referenceGenericClassTest2 = null;

	protected final Class<? super E> referenceGenericClassTest3 = null;

}