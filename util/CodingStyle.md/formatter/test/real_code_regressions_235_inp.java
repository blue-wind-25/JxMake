/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG line-split-by-operator-priority=on */

import java.io.Serializable;
import java.util.function.Supplier;

public final class RealCodeRegressions235 {

    private RealCodeRegressions235() {}

    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        if (delegate instanceof NonSerializableMemoizingSupplier
            || delegate instanceof MemoizingSupplier) {
            return delegate;
        }
        return delegate instanceof Serializable
            ? new MemoizingSupplier<T>(delegate)
            : new NonSerializableMemoizingSupplier<T>(delegate);
    }

    static final class MemoizingSupplier<T> implements Supplier<T>, Serializable {
        final Supplier<T> delegate;

        MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            return delegate.get();
        }
    }

    static final class NonSerializableMemoizingSupplier<T> implements Supplier<T> {
        final Supplier<T> delegate;

        NonSerializableMemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            return delegate.get();
        }
    }
}
