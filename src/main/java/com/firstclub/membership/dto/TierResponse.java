package com.firstclub.membership.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.firstclub.membership.model.Benefit;
import com.firstclub.membership.model.MembershipTier;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TierResponse {

    private final String code;
    private final String name;
    private final int rank;
    private final List<Benefit> benefits;
    private final String eligibility;

    // Only present when the caller asks about a specific user.
    private final Boolean eligible;

    public TierResponse(MembershipTier tier, Boolean eligible) {
        this.code = tier.getCode();
        this.name = tier.getName();
        this.rank = tier.getRank();
        this.benefits = tier.getBenefits();
        this.eligibility = tier.getEligibility().describe();
        this.eligible = eligible;
    }
}
