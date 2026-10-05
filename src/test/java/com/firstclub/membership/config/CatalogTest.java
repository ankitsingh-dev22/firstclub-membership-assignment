package com.firstclub.membership.config;

import com.firstclub.membership.config.CatalogProperties.EligibilityProperties;
import com.firstclub.membership.config.CatalogProperties.PlanProperties;
import com.firstclub.membership.config.CatalogProperties.TierProperties;
import com.firstclub.membership.model.MemberProfile;
import com.firstclub.membership.model.MembershipTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Period;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTest {

    @Test
    void ordersTiersByRankRegardlessOfConfigOrder() {
        Catalog catalog = new Catalog(properties(platinum(), silver(), gold()));

        assertThat(catalog.getTiers()).extracting(MembershipTier::getCode)
                .containsExactly("SILVER", "GOLD", "PLATINUM");
    }

    @Test
    void highestEligibleTierIsTheUsersCeiling() {
        Catalog catalog = new Catalog(properties(silver(), gold(), platinum()));

        assertThat(catalog.highestEligibleTier(MemberProfile.empty("new")).getCode()).isEqualTo("SILVER");
        assertThat(catalog.highestEligibleTier(profile(5, "10000")).getCode()).isEqualTo("GOLD");
        assertThat(catalog.highestEligibleTier(profile(5, "9999")).getCode()).isEqualTo("SILVER");
    }

    @Test
    void vipQualifiesForPlatinumWithoutMeetingGoldConditions() {
        Catalog catalog = new Catalog(properties(silver(), gold(), platinum()));

        assertThat(catalog.highestEligibleTier(profile(0, "0", "VIP")).getCode()).isEqualTo("PLATINUM");
    }

    @Test
    void rejectsCatalogWhoseLowestTierHasConditions() {
        assertThatThrownBy(() -> new Catalog(properties(gold(), platinum())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lowest tier GOLD");
    }

    @Test
    void rejectsDuplicateTierRanks() {
        TierProperties anotherRankTwo = tier("SILVER_PLUS", 2, null);

        assertThatThrownBy(() -> new Catalog(properties(silver(), gold(), anotherRankTwo)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate tier rank");
    }

    private static CatalogProperties properties(TierProperties... tiers) {
        PlanProperties monthly = new PlanProperties();
        monthly.setCode("MONTHLY");
        monthly.setName("Monthly");
        monthly.setDuration(Period.ofMonths(1));
        monthly.setPrice(new BigDecimal("299"));

        CatalogProperties properties = new CatalogProperties();
        properties.setCurrency("INR");
        properties.setPlans(List.of(monthly));
        properties.setTiers(List.of(tiers));
        return properties;
    }

    private static TierProperties silver() {
        return tier("SILVER", 1, null);
    }

    private static TierProperties gold() {
        EligibilityProperties eligibility = new EligibilityProperties();
        eligibility.setMinOrderCount(5);
        eligibility.setMinMonthlyOrderValue(new BigDecimal("10000"));
        return tier("GOLD", 2, eligibility);
    }

    private static TierProperties platinum() {
        EligibilityProperties eligibility = new EligibilityProperties();
        eligibility.setMatch(EligibilityProperties.Match.ANY);
        eligibility.setMinMonthlyOrderValue(new BigDecimal("30000"));
        eligibility.setCohorts(List.of("VIP"));
        return tier("PLATINUM", 3, eligibility);
    }

    private static TierProperties tier(String code, int rank, EligibilityProperties eligibility) {
        TierProperties tier = new TierProperties();
        tier.setCode(code);
        tier.setName(code);
        tier.setRank(rank);
        tier.setEligibility(eligibility);
        return tier;
    }

    private static MemberProfile profile(int orderCount, String monthlyOrderValue, String... cohorts) {
        return new MemberProfile("u1", orderCount, new BigDecimal(monthlyOrderValue), Set.of(cohorts));
    }
}
