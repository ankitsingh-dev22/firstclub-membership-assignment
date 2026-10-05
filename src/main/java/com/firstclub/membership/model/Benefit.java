package com.firstclub.membership.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Benefit {

    private final BenefitType type;
    private final String description;

    // Optional magnitude of the perk; for EXTRA_DISCOUNT it is the discount percentage.
    private final BigDecimal value;

    public Benefit(BenefitType type, String description, BigDecimal value) {
        this.type = Objects.requireNonNull(type, "type");
        this.description = Objects.requireNonNull(description, "description");
        if (type == BenefitType.EXTRA_DISCOUNT && !isValidPercentage(value)) {
            throw new IllegalArgumentException("EXTRA_DISCOUNT needs a percentage between 0 and 100");
        }
        this.value = value;
    }

    private static boolean isValidPercentage(BigDecimal value) {
        return value != null && value.signum() > 0 && value.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
