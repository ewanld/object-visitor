package com.github.ewanld.objectvisitor.internal;

public class ObjectVisitorUtil {
	/**
	 * Run an expression block, wrapping all exceptions into a RuntimeException.
	 */
	public static void quietly(CheckedRunnable checkedRunnable) {
		try {
			checkedRunnable.run();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a value from a Supplier, wrapping all exceptions into a RuntimeException.
	 */
	public static <U> U getQuietly(CheckedSupplier<U> checkedSupplier) {
		try {
			return checkedSupplier.get();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
