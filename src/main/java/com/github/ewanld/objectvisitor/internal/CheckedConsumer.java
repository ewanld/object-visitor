package com.github.ewanld.objectvisitor.internal;

import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T> {
	public void accept(T t) throws Exception;

	public static <T> Consumer<T> uncheck(CheckedConsumer<T> checkedConsumer) {
		return t -> {
			try {
				checkedConsumer.accept(t);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
}
