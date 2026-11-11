package com.festdescans.api.application.fakes;

import com.festdescans.api.application.port.out.IdGenerator;
import java.util.concurrent.atomic.AtomicInteger;

/** Predictable ids: id-1, id-2, ... */
public final class SequentialIdGenerator implements IdGenerator {

    private final AtomicInteger next = new AtomicInteger();

    @Override
    public String newId() {
        return "id-" + next.incrementAndGet();
    }

    public void reset() {
        next.set(0);
    }
}
