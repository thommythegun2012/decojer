package org.decojer.cavaj.test.jdk5;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// no wildcards in super/interfaces "type arguments"
// Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Integer;>;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
public class DecTestTypeArguments extends HashMap<String, Integer> implements
		Map<String, Integer> {

	// wildcards with bounds allowed
	// ()Ljava/util/Map<+Ljava/lang/String;-Ljava/lang/String;>;
	private Map<? extends String, ? super String> getBothBounds() {
		return null;
	}

	// ()Ljava/util/List<+Ljava/util/List<Ljava/lang/Integer;>;>;
	private List<? extends List<Integer>> getIntegerLowerBound() {
		return null;
	}

	// wildcard without bound
	// ()Ljava/util/List<-Ljava/util/Map<**>;>;
	private List<? super Map<?, ?>> getIntegerUpperBound() {
		return null;
	}
}