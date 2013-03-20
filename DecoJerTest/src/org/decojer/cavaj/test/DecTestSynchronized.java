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

	public void dynamicSync() {
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

	public void emptySync() {
		synchronized (this) {
		}
		synchronized (this) {
			synchronized (mutex) {
			}
		}
	}

	public void innerExcSync() {
		synchronized (this) {
			try {
				System.out.println("TRY");
			} catch (final RuntimeException e) {
				System.out.println("EXC");
			} finally {
				System.out.println("FIN");
			}
		}
	}

	public void nestedSync() {
		synchronized (this) {
			System.out.println("SYNC");
			synchronized (mutex) {
				System.out.println("NESTEDSYNC");
			}
			System.out.println("SYNC");
		}
	}

	public void outerExcSync() {
		try {
			synchronized (this) {
				System.out.println("TRY");
			}
		} catch (final RuntimeException e) {
			System.out.println("EXC");
		} finally {
			System.out.println("FIN");
		}
	}

}