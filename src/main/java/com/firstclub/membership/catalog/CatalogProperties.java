package com.firstclub.membership.catalog;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw binding of the {@code membership.catalog} section of application.yml.
 * {@link Catalog} turns it into validated domain objects.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "membership.catalog")
public class CatalogProperties {

    private String currency;
    private List<PlanProperties> plans = new ArrayList<>();
    private List<TierProperties> tiers = new ArrayList<>();

    @Getter
    @Setter
    public static class PlanProperties {
        private String code;
        private String name;
        private Period duration;
        private BigDecimal price;
    }

    @Getter
    @Setter
    public static class TierProperties {
        private String code;
        private String name;
        private int rank;
        private EligibilityProperties eligibility;
        private List<BenefitProperties> benefits = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class EligibilityProperties {
        private Match match = Match.ALL;
        private Integer minOrderCount;
        private BigDecimal minMonthlyOrderValue;
        private List<String> cohorts = new ArrayList<>();

        public enum Match { ALL, ANY }
    }

    @Getter
    @Setter
    public static class BenefitProperties {
        private BenefitType type;
        private String description;
        private BigDecimal value;
    }
}
