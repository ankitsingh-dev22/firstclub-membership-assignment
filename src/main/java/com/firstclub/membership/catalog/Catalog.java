package com.firstclub.membership.catalog;

import com.firstclub.membership.catalog.CatalogProperties.BenefitProperties;
import com.firstclub.membership.catalog.CatalogProperties.EligibilityProperties;
import com.firstclub.membership.catalog.CatalogProperties.PlanProperties;
import com.firstclub.membership.catalog.CatalogProperties.TierProperties;
import com.firstclub.membership.common.ErrorCode;
import com.firstclub.membership.common.MembershipException;
import com.firstclub.membership.eligibility.AllOfRule;
import com.firstclub.membership.eligibility.AnyOfRule;
import com.firstclub.membership.eligibility.CohortRule;
import com.firstclub.membership.eligibility.EligibilityRule;
import com.firstclub.membership.eligibility.MemberProfile;
import com.firstclub.membership.eligibility.MonthlyOrderValueAtLeastRule;
import com.firstclub.membership.eligibility.OrderCountAtLeastRule;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Plans and tiers loaded once from configuration. Immutable after construction,
 * so it is safe to read from any thread without locking.
 */
@Getter
@Component
public class Catalog {

    private final List<MembershipPlan> plans;
    private final List<MembershipTier> tiers;

    public Catalog(CatalogProperties properties) {
        Currency currency = Currency.getInstance(properties.getCurrency());
        this.plans = properties.getPlans().stream()
                .map(plan -> toPlan(plan, currency))
                .toList();
        this.tiers = properties.getTiers().stream()
                .map(Catalog::toTier)
                .sorted(Comparator.comparingInt(MembershipTier::getRank))
                .toList();
        validate();
    }

    public MembershipPlan getPlan(String code) {
        return plans.stream()
                .filter(plan -> plan.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new MembershipException(ErrorCode.PLAN_NOT_FOUND, "Unknown plan: " + code));
    }

    public MembershipTier getTier(String code) {
        return tiers.stream()
                .filter(tier -> tier.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new MembershipException(ErrorCode.TIER_NOT_FOUND, "Unknown tier: " + code));
    }

    /**
     * The highest tier the profile qualifies for. This is the user's ceiling: they may hold this tier or any lower one.
     */
    public MembershipTier highestEligibleTier(MemberProfile profile) {
        for (int i = tiers.size() - 1; i >= 0; i--) {
            MembershipTier tier = tiers.get(i);
            if (tier.getEligibility().isSatisfiedBy(profile)) {
                return tier;
            }
        }
        throw new IllegalStateException("Base tier must accept every user");
    }

    private void validate() {
        if (plans.isEmpty() || tiers.isEmpty()) {
            throw new IllegalStateException("Catalog needs at least one plan and one tier");
        }
        requireUnique(plans, MembershipPlan::getCode, "plan code");
        requireUnique(tiers, MembershipTier::getCode, "tier code");
        requireUnique(tiers, MembershipTier::getRank, "tier rank");

        // Rules only get easier to satisfy as order count, order value and cohorts grow,
        // so if a brand-new user qualifies for the lowest tier, every user does.
        MembershipTier baseTier = tiers.get(0);
        if (!baseTier.getEligibility().isSatisfiedBy(MemberProfile.empty("new-user"))) {
            throw new IllegalStateException("Lowest tier " + baseTier.getCode() + " must not have eligibility conditions");
        }
    }

    private static <T> void requireUnique(List<T> items, Function<T, ?> key, String label) {
        Set<Object> seen = new HashSet<>();
        for (T item : items) {
            if (!seen.add(key.apply(item))) {
                throw new IllegalStateException("Duplicate " + label + ": " + key.apply(item));
            }
        }
    }

    private static MembershipPlan toPlan(PlanProperties plan, Currency currency) {
        return new MembershipPlan(plan.getCode(), plan.getName(), plan.getDuration(), new Money(plan.getPrice(), currency));
    }

    private static MembershipTier toTier(TierProperties tier) {
        List<Benefit> benefits = tier.getBenefits().stream()
                .map(Catalog::toBenefit)
                .toList();
        return new MembershipTier(tier.getCode(), tier.getName(), tier.getRank(), benefits, toRule(tier.getEligibility()));
    }

    private static Benefit toBenefit(BenefitProperties benefit) {
        return new Benefit(benefit.getType(), benefit.getDescription(), benefit.getValue());
    }

    private static EligibilityRule toRule(EligibilityProperties eligibility) {
        if (eligibility == null) {
            return new AllOfRule(List.of());
        }
        List<EligibilityRule> rules = new ArrayList<>();
        if (eligibility.getMinOrderCount() != null) {
            rules.add(new OrderCountAtLeastRule(eligibility.getMinOrderCount()));
        }
        if (eligibility.getMinMonthlyOrderValue() != null) {
            rules.add(new MonthlyOrderValueAtLeastRule(eligibility.getMinMonthlyOrderValue()));
        }
        for (String cohort : eligibility.getCohorts()) {
            rules.add(new CohortRule(cohort));
        }

        if (rules.size() == 1) {
            return rules.get(0);
        }
        return eligibility.getMatch() == EligibilityProperties.Match.ANY ? new AnyOfRule(rules) : new AllOfRule(rules);
    }
}
