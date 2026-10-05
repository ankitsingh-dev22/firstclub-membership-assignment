package com.firstclub.membership.dto;

import com.firstclub.membership.model.Benefit;
import com.firstclub.membership.model.Membership;
import com.firstclub.membership.model.MembershipPlan;
import com.firstclub.membership.model.MembershipStatus;
import com.firstclub.membership.model.MembershipTier;
import com.firstclub.membership.model.Money;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class MembershipResponse {

    private final UUID id;
    private final String userId;
    private final MembershipStatus status;
    private final String planCode;
    private final String planName;
    private final String tierCode;
    private final String tierName;
    private final Money pricePaid;
    private final Instant startedAt;
    private final Instant expiresAt;
    private final Instant cancelledAt;
    private final List<Benefit> benefits;
    private final long version;

    public MembershipResponse(Membership membership, MembershipPlan plan, MembershipTier tier, MembershipStatus status) {
        this.id = membership.getId();
        this.userId = membership.getUserId();
        this.status = status;
        this.planCode = plan.getCode();
        this.planName = plan.getName();
        this.tierCode = tier.getCode();
        this.tierName = tier.getName();
        this.pricePaid = membership.getPricePaid();
        this.startedAt = membership.getStartedAt();
        this.expiresAt = membership.getExpiresAt();
        this.cancelledAt = membership.getCancelledAt();
        // Benefits only apply while the membership is active, so checkout can use this list as-is.
        this.benefits = status == MembershipStatus.ACTIVE ? tier.getBenefits() : List.of();
        this.version = membership.getVersion();
    }
}
