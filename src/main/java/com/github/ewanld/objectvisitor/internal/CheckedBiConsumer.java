package com.github.ewanld.objectvisitor.internal;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface CheckedBiConsumer<T, U> {
	public void accept(T t, U u) throws Exception;

	public static <T, U> BiConsumer<T, U> uncheck(CheckedBiConsumer<T, U> checkedConsumer) {
		return (t, u) -> {
			try {
				checkedConsumer.accept(t, u);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
}
