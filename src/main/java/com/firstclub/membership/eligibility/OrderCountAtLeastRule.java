package com.firstclub.membership.eligibility;

import com.firstclub.membership.model.MemberProfile;
import lombok.Getter;

@Getter
public final class OrderCountAtLeastRule implements EligibilityRule {

    private final int minOrderCount;

    public OrderCountAtLeastRule(int minOrderCount) {
        if (minOrderCount < 0) {
            throw new IllegalArgumentException("minOrderCount must not be negative");
        }
        this.minOrderCount = minOrderCount;
    }

    @Override
    public boolean isSatisfiedBy(MemberProfile profile) {
        return profile.getOrderCount() >= minOrderCount;
    }

    @Override
    public String describe() {
        return "orderCount >= " + minOrderCount;
    }
}
