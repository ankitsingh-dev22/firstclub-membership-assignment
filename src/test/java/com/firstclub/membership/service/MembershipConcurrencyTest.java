package com.firstclub.membership.service;

import com.firstclub.membership.config.Catalog;
import com.firstclub.membership.exception.MembershipException;
import com.firstclub.membership.model.Membership;
import com.firstclub.membership.repository.MembershipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MembershipConcurrencyTest {

    private static final int THREADS = 16;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private Catalog catalog;

    @Autowired
    private Clock clock;

    @Test
    void concurrentSubscriptionsCreateExactlyOneMembership() throws Exception {
        List<Callable<Membership>> attempts = Collections.nCopies(THREADS,
                () -> membershipService.subscribe("race-subscribe", "MONTHLY", "SILVER"));

        List<String> outcomes = runConcurrently(attempts);

        assertThat(outcomes).filteredOn("OK"::equals).hasSize(1);
        assertThat(outcomes).filteredOn("ACTIVE_MEMBERSHIP_EXISTS"::equals).hasSize(THREADS - 1);
        assertThat(membershipRepository.findLatestByUserId("race-subscribe"))
                .hasValueSatisfying(membership -> assertThat(membership.isActiveAt(clock.instant())).isTrue());
    }

    @Test
    void updatesBasedOnTheSameVersionCannotBothWin() throws Exception {
        Membership current = membershipService.subscribe("race-update", "MONTHLY", "SILVER");
        Instant now = clock.instant();
        Membership upgraded = current.withTier(catalog.getTier("GOLD"), now);
        Membership cancelled = current.cancel(now);

        List<String> outcomes = runConcurrently(List.of(
                () -> membershipRepository.update(upgraded, current.getVersion()),
                () -> membershipRepository.update(cancelled, current.getVersion())));

        assertThat(outcomes).containsExactlyInAnyOrder("OK", "CONCURRENT_MODIFICATION");
        Membership stored = membershipRepository.findLatestByUserId("race-update").orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(1);
        assertThat(stored).isIn(upgraded, cancelled);
    }

    @Test
    void concurrentCancellationsTakeEffectOnce() throws Exception {
        membershipService.subscribe("race-cancel", "MONTHLY", "SILVER");
        List<Callable<Membership>> attempts = Collections.nCopies(THREADS,
                () -> membershipService.cancel("race-cancel"));

        List<String> outcomes = runConcurrently(attempts);

        // Losers either read the old version and lose the version check, or read the cancelled one and see it is not active.
        assertThat(outcomes).filteredOn("OK"::equals).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.equals("OK"))
                .allMatch(outcome -> outcome.equals("CONCURRENT_MODIFICATION") || outcome.equals("MEMBERSHIP_NOT_ACTIVE"));
        assertThat(membershipRepository.findLatestByUserId("race-cancel").orElseThrow().getVersion()).isEqualTo(1);
    }

    /**
     * Starts all tasks at the same moment and returns "OK" or the error code for each.
     */
    private static List<String> runConcurrently(List<? extends Callable<?>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (Callable<?> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        task.call();
                        return "OK";
                    } catch (MembershipException ex) {
                        return ex.getErrorCode().name();
                    }
                }));
            }
            ready.await();
            start.countDown();

            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(5, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }
}
