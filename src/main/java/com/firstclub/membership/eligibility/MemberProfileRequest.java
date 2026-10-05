package com.firstclub.membership.eligibility;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
public class MemberProfileRequest {

    @NotNull
    @Min(0)
    private Integer orderCount;

    @NotNull
    @DecimalMin("0")
    private BigDecimal monthlyOrderValue;

    private Set<@NotBlank String> cohorts;
}
