Differences JDK 7 to previous

* Autoboxing with Generic Methods has Bugs
  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6355696, e.g.
	public void testParametrizedMethods() {
		parameterizedStaticClassMethod(1, 2);
	}
###
cannot find symbol
symbol  : method valueOf(int)
location: bound of type variable T
                parameterizedStaticClassMethod(1, 2);
                                               ^
Fatal Error: Unable to find method valueOf
###
