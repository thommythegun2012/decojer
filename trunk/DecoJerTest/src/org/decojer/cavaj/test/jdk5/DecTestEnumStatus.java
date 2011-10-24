package org.decojer.cavaj.test.jdk5;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum DecTestEnumStatus {

	WAITING(0), READY(1), SKIPPED(-1), COMPLETED(5);

	private static final Map<Integer, DecTestEnumStatus> lookup = new HashMap<Integer, DecTestEnumStatus>();

	static {
		for (DecTestEnumStatus s : EnumSet.allOf(DecTestEnumStatus.class))
			lookup.put(s.getCode(), s);
	}

	public static DecTestEnumStatus get(int code) {
		return lookup.get(code);
	}

	private int code;

	private DecTestEnumStatus(int code) {
		// cannot reference static fields in constructor, resulting synthetic
		// code is using static initializer for itself
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	// cannot overwrite valueOf(String) or values()

}