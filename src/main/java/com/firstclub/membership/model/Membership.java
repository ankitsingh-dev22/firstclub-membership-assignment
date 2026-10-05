package com.firstclub.membership.model;

import com.firstclub.membership.exception.ErrorCode;
import com.firstclub.membership.exception.MembershipException;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * A user's subscription to a plan at a tier. Immutable: every change returns a new instance
 * with a bumped version, which the repository uses for optimistic concurrency.
 * Status is never stored; it is derived from the timestamps.
 */
@Getter
@ToString
public final class Membership {

    private final UUID id;
    private final String userId;
    private final String planCode;
    private final String tierCode;
    private final Money pricePaid;
    private final Instant startedAt;
    private final Instant expiresAt;
    private final Instant cancelledAt;
    private final long version;

    /**
     * Starts a new membership. {@code now} must be in the business time zone because
     * the plan duration is calendar based (one month from 31 Jan is 28/29 Feb).
     */
    public Membership(String userId, MembershipPlan plan, MembershipTier tier, ZonedDateTime now) {
        this(UUID.randomUUID(), userId, plan.getCode(), tier.getCode(), plan.getPrice(),
                now.toInstant(), now.plus(plan.getDuration()).toInstant(), null, 0);
    }

    private Membership(UUID id, String userId, String planCode, String tierCode, Money pricePaid,
                       Instant startedAt, Instant expiresAt, Instant cancelledAt, long version) {
        this.id = id;
        this.userId = userId;
        this.planCode = planCode;
        this.tierCode = tierCode;
        this.pricePaid = pricePaid;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.cancelledAt = cancelledAt;
        this.version = version;
    }

    public MembershipStatus statusAt(Instant now) {
        if (cancelledAt != null) {
            return MembershipStatus.CANCELLED;
        }
        return now.isBefore(expiresAt) ? MembershipStatus.ACTIVE : MembershipStatus.EXPIRED;
    }

    public boolean isActiveAt(Instant now) {
        return statusAt(now) == MembershipStatus.ACTIVE;
    }

    public Membership withTier(MembershipTier tier, Instant now) {
        requireActive(now);
        if (tierCode.equals(tier.getCode())) {
            return this;
        }
        return new Membership(id, userId, planCode, tier.getCode(), pricePaid,
                startedAt, expiresAt, null, version + 1);
    }

    public Membership cancel(Instant now) {
        requireActive(now);
        return new Membership(id, userId, planCode, tierCode, pricePaid,
                startedAt, expiresAt, now, version + 1);
    }

    private void requireActive(Instant now) {
        MembershipStatus status = statusAt(now);
        if (status != MembershipStatus.ACTIVE) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_NOT_ACTIVE, "Membership is " + status);
        }
    }
}
