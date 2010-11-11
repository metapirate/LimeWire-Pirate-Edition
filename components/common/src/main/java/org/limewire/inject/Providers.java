package org.limewire.inject;

import com.google.inject.Provider;

public class Providers {

    public static <T> Provider<T> of(T instance) {
        return new SimpleProvider<T>(instance);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static <T> Provider<T> nullProvider(Class<? extends T> forTypeSafety) {
        return new EmptyProvider<T>();
    }

    /** A simple provider that always returns T. */
    private static class SimpleProvider<T> implements Provider<T> {
        private final T t;

        public SimpleProvider(T t) {
            this.t = t;
        }

        public T get() {
            return t;
        }
    }

    private static class EmptyProvider<T> implements Provider<T> {
        public T get() {
            return null;
        }
    }

}
