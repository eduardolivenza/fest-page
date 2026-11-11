package com.festdescans.api.application.port.out;

import java.util.function.Supplier;

/** Runs a unit of work atomically: either all its writes are stored or none. */
public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
