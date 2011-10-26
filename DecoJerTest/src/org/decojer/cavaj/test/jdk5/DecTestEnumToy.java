package org.decojer.cavaj.test.jdk5;

public enum DecTestEnumToy {

	DOLL(1) {
		@Override
		public void execute() {
			System.out.println("I'm a doll.");
		}
	},

	@Deprecated
	SOLDIER("YUP", 2) {

		Class clazz = DecTestEnumToy.class;

		@Override
		public void execute() {
			System.out.println("I'm a soldier.");
		}
	};

	private DecTestEnumToy(int t) {
		// test only
	}

	private DecTestEnumToy(String t, int s) {
		// test only
	}

	// template method
	public abstract void execute();

}