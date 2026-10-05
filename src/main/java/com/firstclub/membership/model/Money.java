package com.firstclub.membership.model;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@Getter
@ToString
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "currency");
    }
}
