package com.firstclub.membership.eligibility;

public interface EligibilityRule {

    boolean isSatisfiedBy(MemberProfile profile);

    String describe();
}
