package org.decojer.cavaj.test.jdk5;

import java.util.HashMap;

public class DecTestFields<E> extends HashMap<String, E> {

	protected final Class referenceClassTest = String.class;

	@DecTestAnnotations(byteNoDefault = 1, byteArrayTest = {})
	protected final static Class<String> referenceGenericClassTest = String.class;

	@DecTestAnnotations(byteNoDefault = 1, stringArrayTest = {})
	protected final Class<? extends E> referenceGenericClassTest2 = null;

	protected final Class<? super E> referenceGenericClassTest3 = null;

}