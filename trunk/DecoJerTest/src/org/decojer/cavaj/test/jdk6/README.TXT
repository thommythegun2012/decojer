Differences JDK 6 to previous

* Autoboxing with Generic Methods has Bugs, e.g.
	private static <T extends Integer> void parameterizedStaticClassMethod(
			final T a, final T b) {
		System.out.println(a + b);
	}
###
cannot find symbol
symbol  : method intValue()
location: bound of type variable T
                System.out.println(a + b);
                                   ^
###


* new constants:
  + Double.MIN_NORMAL, Float.MIN_NORMAL
  + Double.MAX_EXPONENT, Float.MAX_EXPONENT
  + Double.MIN_EXPONENT; Float.MIX_EXPONENT