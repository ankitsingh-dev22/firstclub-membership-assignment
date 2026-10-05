package com.firstclub.membership.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Period;
import java.util.Objects;

@Getter
@ToString
public final class MembershipPlan {

    private final String code;
    private final String name;
    private final Period duration;
    private final Money price;

    public MembershipPlan(String code, String name, Period duration, Money price) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Plan " + code + " needs a positive duration");
        }
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.duration = duration;
        this.price = Objects.requireNonNull(price, "price");
    }
}
