package com.festdescans.api.adapter.out.password;

import com.festdescans.api.application.port.out.PasswordVerifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

/**
 * Argon2 hashes in PHC string format ({@code $argon2id$v=19$m=65536,t=3,p=4$<salt>$<hash>}).
 *
 * <p>Compatible with the hashes the Node api stored with node-argon2. Those write the parameters as
 * {@code m,p,t}, an order Spring Security's Argon2PasswordEncoder does not accept, so the parameters are
 * parsed here in any order.
 */
public final class Argon2PasswordHasher implements PasswordVerifier {

    // node-argon2 defaults, so new hashes look like the existing ones.
    private static final int MEMORY_KIB = 65536;
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 4;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;

    private static final Base64.Encoder B64 = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODE = Base64.getDecoder();

    private final SecureRandom random = new SecureRandom();

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return false;
        }
        Phc phc;
        try {
            phc = Phc.parse(passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = derive(rawPassword, phc.type, phc.version, phc.memoryKiB, phc.iterations, phc.parallelism,
                phc.salt, phc.hash.length);
        return MessageDigest.isEqual(actual, phc.hash);
    }

    /** New argon2id hash (used to create admin users and in tests). */
    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(rawPassword, Argon2Parameters.ARGON2_id, Argon2Parameters.ARGON2_VERSION_13, MEMORY_KIB,
                ITERATIONS, PARALLELISM, salt, HASH_BYTES);
        return "$argon2id$v=19$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM + "$"
                + B64.encodeToString(salt) + "$" + B64.encodeToString(hash);
    }

    private static byte[] derive(String password, int type, int version, int memoryKiB, int iterations,
            int parallelism, byte[] salt, int length) {
        Argon2Parameters params = new Argon2Parameters.Builder(type)
                .withVersion(version)
                .withMemoryAsKB(memoryKiB)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] out = new byte[length];
        generator.generateBytes(password.getBytes(StandardCharsets.UTF_8), out);
        return out;
    }

    private record Phc(int type, int version, int memoryKiB, int iterations, int parallelism, byte[] salt,
            byte[] hash) {

        // Upper bounds so a corrupted hash can't make a login allocate gigabytes.
        private static final int MAX_MEMORY_KIB = 1 << 20;
        private static final int MAX_ITERATIONS = 32;
        private static final int MAX_PARALLELISM = 64;

        static Phc parse(String encoded) {
            // "", type, [v=..], params, salt, hash
            String[] parts = encoded.split("\\$");
            if (parts.length < 5 || parts.length > 6 || !parts[0].isEmpty()) {
                throw new IllegalArgumentException("not a PHC string");
            }
            int type = switch (parts[1]) {
                case "argon2id" -> Argon2Parameters.ARGON2_id;
                case "argon2i" -> Argon2Parameters.ARGON2_i;
                case "argon2d" -> Argon2Parameters.ARGON2_d;
                default -> throw new IllegalArgumentException("unknown algorithm");
            };
            int i = 2;
            int version = Argon2Parameters.ARGON2_VERSION_10;
            if (parts[i].startsWith("v=")) {
                version = Integer.parseInt(parts[i++].substring(2));
            }
            Map<String, Integer> params = new HashMap<>();
            for (String kv : parts[i++].split(",")) {
                String[] pair = kv.split("=", 2);
                if (pair.length != 2) {
                    throw new IllegalArgumentException("bad parameter");
                }
                params.put(pair[0], Integer.parseInt(pair[1]));
            }
            if (i + 2 != parts.length) {
                throw new IllegalArgumentException("missing salt or hash");
            }
            int m = required(params, "m", MAX_MEMORY_KIB);
            int t = required(params, "t", MAX_ITERATIONS);
            int p = required(params, "p", MAX_PARALLELISM);
            byte[] salt = B64_DECODE.decode(parts[i]);
            byte[] hash = B64_DECODE.decode(parts[i + 1]);
            if (salt.length < 8 || hash.length < 4) {
                throw new IllegalArgumentException("salt or hash too short");
            }
            return new Phc(type, version, m, t, p, salt, hash);
        }

        private static int required(Map<String, Integer> params, String key, int max) {
            Integer value = params.get(key);
            if (value == null || value < 1 || value > max) {
                throw new IllegalArgumentException("bad parameter " + key);
            }
            return value;
        }
    }
}
