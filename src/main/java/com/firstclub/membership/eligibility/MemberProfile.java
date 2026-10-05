package com.firstclub.membership.eligibility;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/**
 * Facts about a user that tier eligibility is decided on. Owned by the order and CRM systems;
 * this service only receives the latest snapshot.
 */
@Getter
@ToString
public final class MemberProfile {

    private final String userId;
    private final int orderCount;
    private final BigDecimal monthlyOrderValue;
    private final Set<String> cohorts;

    public MemberProfile(String userId, int orderCount, BigDecimal monthlyOrderValue, Set<String> cohorts) {
        if (orderCount < 0) {
            throw new IllegalArgumentException("orderCount must not be negative");
        }
        if (monthlyOrderValue == null || monthlyOrderValue.signum() < 0) {
            throw new IllegalArgumentException("monthlyOrderValue must not be negative");
        }
        this.userId = Objects.requireNonNull(userId, "userId");
        this.orderCount = orderCount;
        this.monthlyOrderValue = monthlyOrderValue;
        this.cohorts = cohorts == null ? Set.of() : Set.copyOf(cohorts);
    }

    public static MemberProfile empty(String userId) {
        return new MemberProfile(userId, 0, BigDecimal.ZERO, Set.of());
    }
}
