package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.application.port.out.IdGenerator;
import java.lang.management.ManagementFactory;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Collision-resistant ids in the cuid (v1) format Prisma's {@code @default(cuid())} used, so new rows
 * look like the existing ones: {@code c} + timestamp(8) + counter(4) + fingerprint(4) + random(8), base 36.
 */
@Component
class CuidIdGenerator implements IdGenerator {

    private static final int BASE = 36;
    private static final int BLOCK = 4;
    private static final long BLOCK_MAX = (long) Math.pow(BASE, BLOCK);

    private final SecureRandom random = new SecureRandom();
    private final AtomicInteger counter = new AtomicInteger(random.nextInt((int) BLOCK_MAX));
    private final String fingerprint = fingerprint();

    @Override
    public String newId() {
        String timestamp = pad(Long.toString(System.currentTimeMillis(), BASE), 8);
        String count = pad(Long.toString(Math.floorMod(counter.getAndIncrement(), BLOCK_MAX), BASE), BLOCK);
        return "c" + timestamp + count + fingerprint + randomBlock() + randomBlock();
    }

    private String randomBlock() {
        return pad(Long.toString(random.nextLong(BLOCK_MAX), BASE), BLOCK);
    }

    private static String fingerprint() {
        long pid = ProcessHandle.current().pid();
        String host = ManagementFactory.getRuntimeMXBean().getName();
        int hostHash = host.chars().reduce(host.length() + BASE, Integer::sum);
        return pad(Long.toString(pid % (BASE * BASE), BASE), 2) + pad(Long.toString(hostHash % (BASE * BASE), BASE), 2);
    }

    private static String pad(String s, int size) {
        return s.length() >= size ? s.substring(s.length() - size) : "0".repeat(size - s.length()) + s;
    }
}
