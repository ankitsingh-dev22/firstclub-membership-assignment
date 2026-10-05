package com.firstclub.membership.eligibility;

import com.firstclub.membership.model.MemberProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EligibilityRuleTest {

    @Test
    void orderCountRuleIncludesTheThreshold() {
        EligibilityRule rule = new OrderCountAtLeastRule(5);

        assertThat(rule.isSatisfiedBy(profile(4, "0"))).isFalse();
        assertThat(rule.isSatisfiedBy(profile(5, "0"))).isTrue();
    }

    @Test
    void monthlyOrderValueRuleIncludesTheThresholdRegardlessOfScale() {
        EligibilityRule rule = new MonthlyOrderValueAtLeastRule(new BigDecimal("10000"));

        assertThat(rule.isSatisfiedBy(profile(0, "9999.99"))).isFalse();
        assertThat(rule.isSatisfiedBy(profile(0, "10000.00"))).isTrue();
    }

    @Test
    void cohortRuleMatchesMembership() {
        EligibilityRule rule = new CohortRule("VIP");

        assertThat(rule.isSatisfiedBy(profile(0, "0", "VIP", "EARLY_ADOPTER"))).isTrue();
        assertThat(rule.isSatisfiedBy(profile(0, "0", "EARLY_ADOPTER"))).isFalse();
    }

    @Test
    void allOfRequiresEveryRule() {
        EligibilityRule gold = new AllOfRule(List.of(
                new OrderCountAtLeastRule(5),
                new MonthlyOrderValueAtLeastRule(new BigDecimal("10000"))));

        assertThat(gold.isSatisfiedBy(profile(5, "10000"))).isTrue();
        assertThat(gold.isSatisfiedBy(profile(5, "9000"))).isFalse();
        assertThat(gold.isSatisfiedBy(profile(4, "10000"))).isFalse();
    }

    @Test
    void emptyAllOfIsAlwaysSatisfied() {
        EligibilityRule base = new AllOfRule(List.of());

        assertThat(base.isSatisfiedBy(MemberProfile.empty("u1"))).isTrue();
        assertThat(base.describe()).isEqualTo("always");
    }

    @Test
    void anyOfRequiresAtLeastOneRule() {
        EligibilityRule platinum = new AnyOfRule(List.of(
                new MonthlyOrderValueAtLeastRule(new BigDecimal("30000")),
                new CohortRule("VIP")));

        assertThat(platinum.isSatisfiedBy(profile(0, "0", "VIP"))).isTrue();
        assertThat(platinum.isSatisfiedBy(profile(0, "30000"))).isTrue();
        assertThat(platinum.isSatisfiedBy(profile(50, "29999"))).isFalse();
        assertThatThrownBy(() -> new AnyOfRule(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nestedRulesDescribeThemselvesReadably() {
        EligibilityRule rule = new AnyOfRule(List.of(
                new AllOfRule(List.of(new OrderCountAtLeastRule(5), new MonthlyOrderValueAtLeastRule(new BigDecimal("10000")))),
                new CohortRule("VIP")));

        assertThat(rule.describe()).isEqualTo("((orderCount >= 5 AND monthlyOrderValue >= 10000) OR cohort = VIP)");
    }

    private static MemberProfile profile(int orderCount, String monthlyOrderValue, String... cohorts) {
        return new MemberProfile("u1", orderCount, new BigDecimal(monthlyOrderValue), Set.of(cohorts));
    }
}
