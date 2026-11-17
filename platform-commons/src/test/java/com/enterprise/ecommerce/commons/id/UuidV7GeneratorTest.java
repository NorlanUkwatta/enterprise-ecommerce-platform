package com.enterprise.ecommerce.commons.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UuidV7GeneratorTest {

    @Test
    @DisplayName("Should generate RFC 9562 compliant UUID with version 7 and IETF variant 2")
    void shouldGenerateValidVersion7AndVariant2() {
        UUID id = UuidV7Generator.generate();

        assertThat(id).isNotNull();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should embed and accurately extract the 48-bit Unix epoch millisecond timestamp")
    void shouldEmbedAndExtractExactTimestamp() {
        long fixedEpochMillis = 1794896000123L;
        Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(fixedEpochMillis), ZoneOffset.UTC);
        UuidV7Generator generator = new UuidV7Generator(fixedClock);

        UUID id = generator.nextUuid();

        assertThat(UuidV7Generator.extractTimestampMillis(id)).isEqualTo(fixedEpochMillis);
    }

    @Test
    @DisplayName("Should reject non-v7 UUIDs when extracting timestamp")
    void shouldRejectUuidV4WhenExtractingTimestamp() {
        UUID randomV4 = UUID.randomUUID();

        assertThatThrownBy(() -> UuidV7Generator.extractTimestampMillis(randomV4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected UUID version 7");
    }

    @Test
    @DisplayName("Should guarantee strict monotonic ordering even when thousands of IDs are generated in the same millisecond")
    void shouldMaintainMonotonicOrderWithinSameMillisecond() {
        Clock frozenClock = Clock.fixed(Instant.parse("2026-11-17T06:00:00Z"), ZoneOffset.UTC);
        UuidV7Generator generator = new UuidV7Generator(frozenClock);

        int count = 10_000; // Exceeds 4,096 per-ms sequence limit to test logical ms rollover too
        List<String> generatedStrings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            generatedStrings.add(generator.nextUuid().toString());
        }

        // Standard lexicographical string order of UUIDv7 must be strictly increasing
        for (int i = 1; i < count; i++) {
            assertThat(generatedStrings.get(i))
                    .as("UUID at index %d should be strictly greater than index %d", i, i - 1)
                    .isGreaterThan(generatedStrings.get(i - 1));
        }
    }

    @Test
    @DisplayName("Should generate 50,000 unique UUIDv7 values concurrently across Java 21 Virtual Threads without collision")
    void shouldGenerateUniqueIdsUnderHighVirtualThreadConcurrency() throws InterruptedException {
        int virtualThreadCount = 50;
        int idsPerThread = 1_000;
        int totalExpected = virtualThreadCount * idsPerThread;

        Set<UUID> uniqueIds = ConcurrentHashMap.newKeySet(totalExpected);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(virtualThreadCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < virtualThreadCount; t++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        for (int i = 0; i < idsPerThread; i++) {
                            uniqueIds.add(UuidV7Generator.generate());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean finished = doneGate.await(15, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        }

        assertThat(uniqueIds).hasSize(totalExpected);
    }
}
