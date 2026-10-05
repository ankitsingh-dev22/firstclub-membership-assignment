package com.firstclub.membership.model;

import com.firstclub.membership.eligibility.EligibilityRule;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@Getter
@ToString
public final class MembershipTier {

    private final String code;
    private final String name;
    private final int rank;
    private final List<Benefit> benefits;
    private final EligibilityRule eligibility;

    public MembershipTier(String code, String name, int rank, List<Benefit> benefits, EligibilityRule eligibility) {
        if (rank <= 0) {
            throw new IllegalArgumentException("Tier " + code + " needs a positive rank");
        }
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.rank = rank;
        this.benefits = List.copyOf(benefits);
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
    }
}
