package com.firstclub.membership.membership;

import com.firstclub.membership.catalog.Catalog;
import com.firstclub.membership.catalog.MembershipPlan;
import com.firstclub.membership.catalog.MembershipTier;
import com.firstclub.membership.common.ErrorCode;
import com.firstclub.membership.common.MembershipException;
import com.firstclub.membership.eligibility.MemberProfile;
import com.firstclub.membership.eligibility.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final Catalog catalog;
    private final MemberProfileRepository profileRepository;
    private final MembershipRepository membershipRepository;
    private final Clock clock;

    public Membership subscribe(String userId, String planCode, String tierCode) {
        MembershipPlan plan = catalog.getPlan(planCode);
        MembershipTier tier = catalog.getTier(tierCode);
        requireWithinCeiling(userId, tier);

        ZonedDateTime now = ZonedDateTime.now(clock);
        Membership membership = membershipRepository.insertIfNoActive(new Membership(userId, plan, tier, now), now.toInstant());
        log.info("Subscribed user={} plan={} tier={} expiresAt={}", userId, plan.getCode(), tier.getCode(), membership.getExpiresAt());
        return membership;
    }

    public Membership getLatest(String userId) {
        return membershipRepository.findLatestByUserId(userId)
                .orElseThrow(() -> new MembershipException(ErrorCode.MEMBERSHIP_NOT_FOUND, "User " + userId + " has no membership"));
    }

    public Membership changeTier(String userId, String tierCode) {
        MembershipTier target = catalog.getTier(tierCode);
        Membership current = getLatest(userId);

        Membership changed = current.withTier(target, clock.instant());
        if (changed == current) {
            return current;
        }
        requireWithinCeiling(userId, target);

        Membership saved = membershipRepository.update(changed, current.getVersion());
        log.info("Changed tier user={} from={} to={}", userId, current.getTierCode(), target.getCode());
        return saved;
    }

    public Membership cancel(String userId) {
        Membership current = getLatest(userId);

        Membership saved = membershipRepository.update(current.cancel(clock.instant()), current.getVersion());
        log.info("Cancelled membership user={} id={}", userId, saved.getId());
        return saved;
    }

    /**
     * Moves the user to the highest tier they currently qualify for, up or down.
     * Stands in for a job or order-event consumer that would call this in production.
     */
    public Membership evaluateTier(String userId) {
        Membership current = getLatest(userId);
        MembershipTier ceiling = ceilingFor(userId);

        Membership changed = current.withTier(ceiling, clock.instant());
        if (changed == current) {
            return current;
        }

        Membership saved = membershipRepository.update(changed, current.getVersion());
        log.info("Re-evaluated tier user={} from={} to={}", userId, current.getTierCode(), ceiling.getCode());
        return saved;
    }

    private void requireWithinCeiling(String userId, MembershipTier tier) {
        MembershipTier ceiling = ceilingFor(userId);
        if (tier.getRank() > ceiling.getRank()) {
            throw new MembershipException(ErrorCode.TIER_NOT_ELIGIBLE,
                    "Tier " + tier.getCode() + " is above the highest tier user " + userId + " qualifies for (" + ceiling.getCode() + ")");
        }
    }

    private MembershipTier ceilingFor(String userId) {
        MemberProfile profile = profileRepository.findByUserId(userId).orElseGet(() -> MemberProfile.empty(userId));
        return catalog.highestEligibleTier(profile);
    }
}
