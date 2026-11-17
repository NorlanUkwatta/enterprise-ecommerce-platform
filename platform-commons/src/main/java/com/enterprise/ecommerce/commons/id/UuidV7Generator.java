package com.enterprise.ecommerce.commons.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RFC 9562 Compliant, Lock-Free Time-Ordered UUIDv7 Generator.
 *
 * <p><b>Why UUIDv7 for 5M Customers (Database B+Tree Locality):</b>
 * Standard random UUIDv4 keys scatter inserts randomly across PostgreSQL B+Tree index pages,
 * causing frequent page splits, buffer-pool thrashing, and write amplification once tables grow
 * beyond 10M+ rows. UUIDv7 embeds a 48-bit Unix Epoch millisecond timestamp in the most significant
 * bits, followed by a 12-bit monotonic counter (`rand_a`) and 62 bits of cryptographic randomness (`rand_b`).
 * This guarantees strictly sequential B+Tree index appends at $O(\log N)$ cost with $O(1)$ lock-free generation.
 *
 * <p><b>Bit Layout (128 bits total):</b>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           unix_ts_ms                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          unix_ts_ms           |  ver  |       rand_a          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |var|                        rand_b                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                            rand_b                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 */
public final class UuidV7Generator {

    private static final long VERSION_7_BITS = 0x7000L;
    private static final long VARIANT_2_BITS = 0x8000_0000_0000_0000L;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long SEQUENCE_MASK = 0x0FFFL; // 12-bit monotonic sequence (0..4095 per ms)

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final UuidV7Generator DEFAULT_INSTANCE = new UuidV7Generator(Clock.systemUTC());

    /**
     * Packed 64-bit state: upper 52 bits store last timestamp (ms), lower 12 bits store monotonic sequence.
     * Updated via lock-free CAS (Compare-And-Swap) loop.
     */
    private final AtomicLong state = new AtomicLong(0L);
    private final Clock clock;

    public UuidV7Generator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    /**
     * Generates a new time-ordered UUIDv7 using the system UTC clock.
     *
     * @return a monotonically increasing RFC 9562 UUIDv7
     */
    public static UUID generate() {
        return DEFAULT_INSTANCE.nextUuid();
    }

    /**
     * Extracts the 48-bit Unix Epoch millisecond timestamp embedded inside a UUIDv7.
     *
     * @param uuid a UUIDv7 instance
     * @return Unix Epoch timestamp in milliseconds
     */
    public static long extractTimestampMillis(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID must not be null");
        if (uuid.version() != 7) {
            throw new IllegalArgumentException("Expected UUID version 7, but got version " + uuid.version());
        }
        return uuid.getMostSignificantBits() >>> 16;
    }

    /**
     * Generates the next UUIDv7 using a lock-free Compare-And-Swap (CAS) loop.
     * If more than 4,096 UUIDs are requested within the same millisecond on a single JVM,
     * the logical timestamp advances by 1ms to preserve strict monotonicity.
     *
     * @return a monotonically increasing UUIDv7
     */
    public UUID nextUuid() {
        long timestampMs;
        long sequence;

        while (true) {
            long currentState = state.get();
            long lastTimestampMs = currentState >>> 12;
            long lastSequence = currentState & SEQUENCE_MASK;
            long nowMs = clock.millis();

            if (nowMs > lastTimestampMs) {
                timestampMs = nowMs;
                // Seed initial 10 bits randomly per millisecond while keeping room for monotonic increments
                sequence = SECURE_RANDOM.nextInt(1 << 10);
            } else {
                // Same millisecond (or clock drift backwards): increment 12-bit monotonic counter
                timestampMs = lastTimestampMs;
                sequence = (lastSequence + 1) & SEQUENCE_MASK;
                if (sequence == 0L) {
                    // 12-bit counter overflowed (4096 IDs in 1ms); advance logical millisecond
                    timestampMs++;
                }
            }

            long nextState = (timestampMs << 12) | sequence;
            if (state.compareAndSet(currentState, nextState)) {
                break;
            }
        }

        long msb = (timestampMs << 16) | VERSION_7_BITS | sequence;
        long lsb = VARIANT_2_BITS | (SECURE_RANDOM.nextLong() & RAND_B_MASK);
        return new UUID(msb, lsb);
    }
}
