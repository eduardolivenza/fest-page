package com.festdescans.api.application.fakes;

import com.festdescans.api.application.port.out.TransactionRunner;
import java.util.function.Supplier;

/** Runs the work directly; counts calls so tests can assert a use case is transactional. */
public final class DirectTransactionRunner implements TransactionRunner {

    private int transactions;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        transactions++;
        return work.get();
    }

    public int transactions() {
        return transactions;
    }
}
