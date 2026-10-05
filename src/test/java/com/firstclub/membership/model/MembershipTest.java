package com.firstclub.membership.model;

import com.firstclub.membership.eligibility.AllOfRule;
import com.firstclub.membership.exception.ErrorCode;
import com.firstclub.membership.exception.MembershipException;
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

class MembershipTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final MembershipPlan MONTHLY = plan("MONTHLY", Period.ofMonths(1), "299");
    private static final MembershipPlan YEARLY = plan("YEARLY", Period.ofYears(1), "2499");
    private static final MembershipTier SILVER = tier("SILVER", 1);
    private static final MembershipTier GOLD = tier("GOLD", 2);

    @Test
    void expiryFollowsTheCalendarInTheBusinessTimeZone() {
        Membership monthly = new Membership("u1", MONTHLY, SILVER, at("2027-01-31T10:00:00+05:30"));
        Membership yearly = new Membership("u2", YEARLY, SILVER, at("2028-02-29T10:00:00+05:30"));

        assertThat(monthly.getExpiresAt()).isEqualTo(Instant.parse("2027-02-28T04:30:00Z"));
        assertThat(yearly.getExpiresAt()).isEqualTo(Instant.parse("2029-02-28T04:30:00Z"));
        assertThat(monthly.getPricePaid().getAmount()).isEqualByComparingTo("299");
        assertThat(monthly.getVersion()).isZero();
    }

    @Test
    void becomesExpiredExactlyAtExpiry() {
        Membership membership = new Membership("u1", MONTHLY, SILVER, at("2027-01-10T10:00:00+05:30"));

        assertThat(membership.statusAt(membership.getExpiresAt().minusSeconds(1))).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.statusAt(membership.getExpiresAt())).isEqualTo(MembershipStatus.EXPIRED);
    }

    @Test
    void tierChangeKeepsPlanExpiryAndPriceAndBumpsVersion() {
        Membership original = new Membership("u1", MONTHLY, SILVER, at("2027-01-10T10:00:00+05:30"));
        Instant later = original.getStartedAt().plusSeconds(3600);

        Membership upgraded = original.withTier(GOLD, later);

        assertThat(upgraded.getTierCode()).isEqualTo("GOLD");
        assertThat(upgraded.getId()).isEqualTo(original.getId());
        assertThat(upgraded.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(upgraded.getPricePaid()).isSameAs(original.getPricePaid());
        assertThat(upgraded.getVersion()).isEqualTo(1);
        assertThat(original.getTierCode()).isEqualTo("SILVER");
        assertThat(upgraded.withTier(GOLD, later)).isSameAs(upgraded);
    }

    @Test
    void cancelledMembershipStaysCancelledAfterExpiryAndCannotChange() {
        Membership membership = new Membership("u1", MONTHLY, SILVER, at("2027-01-10T10:00:00+05:30"));
        Instant cancelTime = membership.getStartedAt().plusSeconds(60);

        Membership cancelled = membership.cancel(cancelTime);

        assertThat(cancelled.getCancelledAt()).isEqualTo(cancelTime);
        assertThat(cancelled.statusAt(cancelled.getExpiresAt().plusSeconds(1))).isEqualTo(MembershipStatus.CANCELLED);
        assertThatThrownBy(() -> cancelled.withTier(GOLD, cancelTime))
                .isInstanceOf(MembershipException.class)
                .extracting(ex -> ((MembershipException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEMBERSHIP_NOT_ACTIVE);
    }

    @Test
    void expiredMembershipCannotBeCancelled() {
        Membership membership = new Membership("u1", MONTHLY, SILVER, at("2027-01-10T10:00:00+05:30"));

        assertThatThrownBy(() -> membership.cancel(membership.getExpiresAt()))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("EXPIRED");
    }

    private static ZonedDateTime at(String isoDateTime) {
        return ZonedDateTime.parse(isoDateTime).withZoneSameInstant(IST);
    }

    private static MembershipPlan plan(String code, Period duration, String price) {
        return new MembershipPlan(code, code, duration, new Money(new BigDecimal(price), Currency.getInstance("INR")));
    }

    private static MembershipTier tier(String code, int rank) {
        return new MembershipTier(code, code, rank, List.of(), new AllOfRule(List.of()));
    }
}
