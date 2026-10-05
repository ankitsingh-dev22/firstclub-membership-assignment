package com.firstclub.membership.eligibility;

import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public final class AnyOfRule implements EligibilityRule {

    private final List<EligibilityRule> rules;

    public AnyOfRule(List<EligibilityRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("AnyOfRule needs at least one rule");
        }
        this.rules = List.copyOf(rules);
    }

    @Override
    public boolean isSatisfiedBy(MemberProfile profile) {
        return rules.stream().anyMatch(rule -> rule.isSatisfiedBy(profile));
    }

    @Override
    public String describe() {
        return rules.stream()
                .map(EligibilityRule::describe)
                .collect(Collectors.joining(" OR ", "(", ")"));
    }
}
