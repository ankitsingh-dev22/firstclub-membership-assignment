package com.firstclub.membership.eligibility;

import com.firstclub.membership.model.MemberProfile;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Satisfied when every rule is satisfied. With no rules it is always satisfied,
 * which is how the base tier is expressed.
 */
@Getter
public final class AllOfRule implements EligibilityRule {

    private final List<EligibilityRule> rules;

    public AllOfRule(List<EligibilityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public boolean isSatisfiedBy(MemberProfile profile) {
        return rules.stream().allMatch(rule -> rule.isSatisfiedBy(profile));
    }

    @Override
    public String describe() {
        if (rules.isEmpty()) {
            return "always";
        }
        return rules.stream()
                .map(EligibilityRule::describe)
                .collect(Collectors.joining(" AND ", "(", ")"));
    }
}
