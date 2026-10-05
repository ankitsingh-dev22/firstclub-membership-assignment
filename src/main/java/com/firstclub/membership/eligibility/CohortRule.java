package com.firstclub.membership.eligibility;

import lombok.Getter;

@Getter
public final class CohortRule implements EligibilityRule {

    private final String cohort;

    public CohortRule(String cohort) {
        if (cohort == null || cohort.isBlank()) {
            throw new IllegalArgumentException("cohort must not be blank");
        }
        this.cohort = cohort;
    }

    @Override
    public boolean isSatisfiedBy(MemberProfile profile) {
        return profile.getCohorts().contains(cohort);
    }

    @Override
    public String describe() {
        return "cohort = " + cohort;
    }
}
