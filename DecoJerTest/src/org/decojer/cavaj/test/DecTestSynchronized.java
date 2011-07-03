package org.decojer.cavaj.test;

public abstract class DecTestSynchronized {

	private static final Object MUTEX = new Object();

	public static void staticSync() {
		System.out.println("Start");
		synchronized (DecTestSynchronized.class) {
			System.out.println("Class mutex");
		}
		System.out.println("Nix");
		synchronized (MUTEX) {
			System.out.println("Static mutex");
		}
		System.out.println("End");
	}

	private final Object mutex = new Object();

	public void sync() {
		System.out.println("Start");
		synchronized (this) {
			System.out.println("This mutex");
		}
		System.out.println("Nix");
		synchronized (this.mutex) {
			System.out.println("Mutex");
		}
		System.out.println("End");
	}

}