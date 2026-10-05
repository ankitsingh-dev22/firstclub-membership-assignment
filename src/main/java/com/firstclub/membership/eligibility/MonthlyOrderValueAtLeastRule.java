package com.firstclub.membership.eligibility;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public final class MonthlyOrderValueAtLeastRule implements EligibilityRule {

    private final BigDecimal minMonthlyOrderValue;

    public MonthlyOrderValueAtLeastRule(BigDecimal minMonthlyOrderValue) {
        if (minMonthlyOrderValue == null || minMonthlyOrderValue.signum() < 0) {
            throw new IllegalArgumentException("minMonthlyOrderValue must not be negative");
        }
        this.minMonthlyOrderValue = minMonthlyOrderValue;
    }

    @Override
    public boolean isSatisfiedBy(MemberProfile profile) {
        return profile.getMonthlyOrderValue().compareTo(minMonthlyOrderValue) >= 0;
    }

    @Override
    public String describe() {
        return "monthlyOrderValue >= " + minMonthlyOrderValue.toPlainString();
    }
}
