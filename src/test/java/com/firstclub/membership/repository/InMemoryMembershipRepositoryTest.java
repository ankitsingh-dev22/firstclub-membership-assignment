package com.firstclub.membership.repository;

import com.firstclub.membership.eligibility.AllOfRule;
import com.firstclub.membership.exception.ErrorCode;
import com.firstclub.membership.exception.MembershipException;
import com.firstclub.membership.model.Membership;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryMembershipRepositoryTest {

    private static final MembershipPlan MONTHLY = new MembershipPlan("MONTHLY", "Monthly", Period.ofMonths(1),
            new Money(new BigDecimal("299"), Currency.getInstance("INR")));
    private static final MembershipTier SILVER = tier("SILVER", 1);
    private static final MembershipTier GOLD = tier("GOLD", 2);
    private static final ZonedDateTime START = ZonedDateTime.of(2027, 1, 10, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata"));

    private final InMemoryMembershipRepository repository = new InMemoryMembershipRepository();

    @Test
    void rejectsSecondMembershipWhileFirstIsActive() {
        Membership first = repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, START), START.toInstant());

        assertThatThrownBy(() -> repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, START), START.toInstant()))
                .satisfies(ex -> assertErrorCode(ex, ErrorCode.ACTIVE_MEMBERSHIP_EXISTS));
        assertThat(repository.findLatestByUserId("u1")).containsSame(first);
    }

    @Test
    void allowsNewMembershipAfterExpiryOrCancellation() {
        Membership expired = repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, START), START.toInstant());
        ZonedDateTime afterExpiry = START.plusMonths(1);
        Membership renewed = repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, afterExpiry), afterExpiry.toInstant());

        assertThat(renewed.getId()).isNotEqualTo(expired.getId());

        Instant cancelTime = afterExpiry.toInstant().plusSeconds(60);
        repository.update(renewed.cancel(cancelTime), renewed.getVersion());
        Membership afterCancel = repository.insertIfNoActive(new Membership("u1", MONTHLY, GOLD, afterExpiry), cancelTime);

        assertThat(repository.findLatestByUserId("u1")).containsSame(afterCancel);
    }

    @Test
    void rejectsUpdateBasedOnStaleVersion() {
        Membership stored = repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, START), START.toInstant());
        Instant now = START.toInstant().plusSeconds(60);

        repository.update(stored.withTier(GOLD, now), stored.getVersion());

        assertThatThrownBy(() -> repository.update(stored.cancel(now), stored.getVersion()))
                .satisfies(ex -> assertErrorCode(ex, ErrorCode.CONCURRENT_MODIFICATION));
        assertThat(repository.findLatestByUserId("u1").orElseThrow().getTierCode()).isEqualTo("GOLD");
    }

    @Test
    void rejectsUpdateToMembershipThatWasReplaced() {
        Membership old = repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, START), START.toInstant());
        ZonedDateTime afterExpiry = START.plusMonths(1);
        repository.insertIfNoActive(new Membership("u1", MONTHLY, SILVER, afterExpiry), afterExpiry.toInstant());

        Membership staleChange = old.withTier(GOLD, START.toInstant());

        assertThatThrownBy(() -> repository.update(staleChange, old.getVersion()))
                .satisfies(ex -> assertErrorCode(ex, ErrorCode.CONCURRENT_MODIFICATION));
    }

    private static void assertErrorCode(Throwable ex, ErrorCode expected) {
        assertThat(ex).isInstanceOf(MembershipException.class);
        assertThat(((MembershipException) ex).getErrorCode()).isEqualTo(expected);
    }

    private static MembershipTier tier(String code, int rank) {
        return new MembershipTier(code, code, rank, List.of(), new AllOfRule(List.of()));
    }
}
